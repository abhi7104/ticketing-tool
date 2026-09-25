#!/usr/bin/env python3
"""Record GitHub Spec Kit prompts to .specstory/history/ and docs/prompt-history.md.

Only Spec Kit prompts are kept: /speckit-* commands, prompts that mention
Spec Kit, and short choice answers (e.g. "A", "Q1: A") that reply to a
Spec Kit command. Everything else is ignored.

Two modes:
  * Hook mode (default): Claude Code runs this on UserPromptSubmit and pipes the
    hook JSON on stdin. The prompt is appended to the per-session history file
    and to docs/prompt-history.md.
  * Backfill mode (--backfill): rebuild both outputs from the Claude Code
    session transcripts (~/.claude/projects/<project>/*.jsonl).

Values from the root .env are redacted before anything is written, so secrets
pasted into a prompt never reach a tracked file.
"""
import datetime as dt
import glob
import json
import os
import re
import sys

ROOT = os.environ.get("CLAUDE_PROJECT_DIR") or os.path.dirname(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
)
HISTORY_DIR = os.path.join(ROOT, ".specstory", "history")
SUMMARY = os.path.join(ROOT, "docs", "prompt-history.md")
SUMMARY_HEADER = (
    "# Prompt History\n\n"
    "GitHub Spec Kit prompts given to the AI assistant (Claude Code) while building this project:\n"
    "`/speckit-*` commands, questions about Spec Kit, and answers to Spec Kit clarification questions.\n"
    "Entries are appended automatically by the `UserPromptSubmit` hook in\n"
    "`.claude/settings.json` (script: `.claude/hooks/log-prompt.py`).\n"
    "Per-session files live in `.specstory/history/`.\n"
    "Values from `.env` are redacted.\n"
)

COMMAND_RE = re.compile(
    r"<command-message>.*?</command-message>\s*<command-name>(?P<name>.*?)</command-name>"
    r"(?:\s*<command-args>(?P<args>.*?)</command-args>)?",
    re.S,
)
SPECKIT_RE = re.compile(r"^/speckit-|spec[\s-]?kit", re.I)
ANSWER_RE = re.compile(r"((Q\d+\s*[:.)-]\s*)?[A-Ea-e]\b[\s,;]*)+")
STATE = os.path.join(HISTORY_DIR, ".last-speckit.json")
SKIP = {"[Request interrupted by user]", "[Request interrupted by user for tool use]"}


def load_secrets():
    secrets = []
    try:
        with open(os.path.join(ROOT, ".env")) as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith("#") or "=" not in line:
                    continue
                key, value = line.split("=", 1)
                value = value.strip().strip("'\"")
                if len(value) >= 6:
                    secrets.append((key.strip(), value))
    except OSError:
        pass
    return sorted(secrets, key=lambda kv: -len(kv[1]))


def clean(prompt, secrets):
    prompt = prompt.strip()
    m = COMMAND_RE.search(prompt)
    if m:
        prompt = (m.group("name").strip() + " " + (m.group("args") or "").strip()).strip()
    for key, value in secrets:
        prompt = prompt.replace(value, f"[REDACTED:{key}]")
    return prompt


def is_speckit(text, after_speckit):
    """True for a Spec Kit prompt, or a choice answer right after one."""
    return bool(SPECKIT_RE.search(text)) or (after_speckit and bool(ANSWER_RE.fullmatch(text)))


def quote(text):
    return "\n".join("> " + line if line else ">" for line in text.splitlines())


def entry(ts, session, prompt):
    return f"\n### {ts} · session `{session[:8]}`\n\n{quote(prompt)}\n"


def session_file(session, ts):
    existing = glob.glob(os.path.join(HISTORY_DIR, f"*_{session[:8]}.md"))
    if existing:
        return existing[0], False
    stamp = ts.replace(":", "-").replace("T", "_").split(".")[0].rstrip("Z")
    return os.path.join(HISTORY_DIR, f"{stamp}Z_{session[:8]}.md"), True


def session_header(session, ts):
    return f"# Session {session}\n\nStarted {ts}. Tool: Claude Code.\n"


def append(ts, session, prompt):
    os.makedirs(HISTORY_DIR, exist_ok=True)
    os.makedirs(os.path.dirname(SUMMARY), exist_ok=True)
    path, new = session_file(session, ts)
    with open(path, "a") as f:
        if new:
            f.write(session_header(session, ts))
        f.write(entry(ts, session, prompt))
    new_summary = not os.path.exists(SUMMARY)
    with open(SUMMARY, "a") as f:
        if new_summary:
            f.write(SUMMARY_HEADER)
        f.write(entry(ts, session, prompt))


def transcript_prompts(path):
    """Yield (timestamp, session_id, text) for real user prompts in a transcript."""
    with open(path) as f:
        for line in f:
            try:
                d = json.loads(line)
            except ValueError:
                continue
            ts, sid = d.get("timestamp"), d.get("sessionId")
            # Prompts typed while Claude is mid-turn are stored as queued_command attachments.
            att = d.get("attachment") or {}
            if d.get("type") == "attachment" and att.get("type") == "queued_command":
                text = att.get("prompt")
                if isinstance(text, str) and (att.get("origin") or {}).get("kind", "human") == "human":
                    yield ts, sid, text
                continue
            if d.get("type") != "user" or d.get("isMeta") or d.get("toolUseResult"):
                continue
            content = d.get("message", {}).get("content")
            if isinstance(content, list):
                if any(c.get("type") == "tool_result" for c in content):
                    continue
                content = "\n".join(c.get("text", "") for c in content if c.get("type") == "text")
            if isinstance(content, str) and content.strip():
                yield ts, sid, content


def backfill():
    slug = re.sub(r"[^A-Za-z0-9]", "-", ROOT)
    transcripts = glob.glob(os.path.expanduser(f"~/.claude/projects/{slug}/*.jsonl"))
    secrets = load_secrets()
    rows = []
    for t in transcripts:
        for ts, sid, text in transcript_prompts(t):
            text = clean(text, secrets)
            # Background-task results reach the transcript as user turns; they are not prompts.
            if text and text not in SKIP and not text.startswith("<task-notification>") and ts and sid:
                rows.append((ts, sid, text))
    rows.sort()
    kept, last = [], {}
    for ts, sid, text in rows:
        keep = is_speckit(text, last.get(sid, False))
        # An answer keeps the flag set so "Q1: A" then "Q2: B" both count.
        last[sid] = keep
        if keep:
            kept.append((ts, sid, text))
    rows = kept
    for p in glob.glob(os.path.join(HISTORY_DIR, "*.md")):
        os.remove(p)
    if os.path.exists(SUMMARY):
        os.remove(SUMMARY)
    for ts, sid, text in rows:
        append(ts, sid, text)
    print(f"backfilled {len(rows)} prompts from {len(transcripts)} transcripts")


def hook():
    data = json.load(sys.stdin)
    prompt = clean(data.get("prompt", ""), load_secrets())
    if not prompt or prompt in SKIP:
        return
    try:
        with open(STATE) as f:
            state = json.load(f)
    except (OSError, ValueError):
        state = {}
    sid = data.get("session_id", "unknown")
    keep = is_speckit(prompt, state.get(sid, False))
    os.makedirs(HISTORY_DIR, exist_ok=True)
    with open(STATE, "w") as f:
        json.dump({sid: keep}, f)
    if not keep:
        return
    ts = dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"
    append(ts, sid, prompt)


if __name__ == "__main__":
    backfill() if "--backfill" in sys.argv else hook()
