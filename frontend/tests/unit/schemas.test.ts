import { describe, expect, it } from "vitest";
import { commentSchema, ticketFormSchema } from "@/lib/validation/schemas";

const valid = { title: "Printer jam", description: "Paper is stuck.", priority: "HIGH", assigneeId: "2" };

function messages(values: Record<string, string>) {
  const r = ticketFormSchema.safeParse(values);
  return r.success ? [] : r.error.issues.map((i) => `${i.path[0]}: ${i.message}`);
}

describe("ticketFormSchema", () => {
  it("accepts valid input and trims text", () => {
    const r = ticketFormSchema.parse({ ...valid, title: "  Printer jam  " });
    expect(r.title).toBe("Printer jam");
  });

  it.each([
    ["title", "ab", false],
    ["title", "abc", true],
    ["title", "x".repeat(150), true],
    ["title", "x".repeat(151), false],
    ["title", "   ", false],
    ["description", "x".repeat(9), false],
    ["description", "x".repeat(10), true],
    ["description", "x".repeat(5000), true],
    ["description", "x".repeat(5001), false],
    ["priority", "URGENT", false],
    ["priority", "", false],
    ["assigneeId", "", false],
    ["assigneeId", "0", false],
  ])("%s=%j valid=%s", (field, value, ok) => {
    expect(ticketFormSchema.safeParse({ ...valid, [field]: value }).success).toBe(ok);
  });

  it("uses the same messages as the backend", () => {
    expect(messages({ title: "", description: "", priority: "", assigneeId: "" })).toEqual([
      "title: Title is required.",
      "description: Description is required.",
      "priority: Please choose a priority.",
      "assigneeId: Please choose an assignee.",
    ]);
    expect(messages({ ...valid, title: "ab" })).toEqual(["title: Title must be between 3 and 150 characters."]);
  });
});

describe("commentSchema", () => {
  it.each([
    ["", false],
    ["   ", false],
    ["x", true],
    ["x".repeat(2000), true],
    ["x".repeat(2001), false],
  ])("%j valid=%s", (body, ok) => {
    expect(commentSchema.safeParse({ body }).success).toBe(ok);
  });
});
