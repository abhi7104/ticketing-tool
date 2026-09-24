import { NextResponse, type NextRequest } from "next/server";

/**
 * Sends visitors without a session cookie to the sign-in page. This is a convenience only; the
 * backend validates the session on every API call.
 */
export function middleware(request: NextRequest) {
  const { pathname, search } = request.nextUrl;
  if (pathname.startsWith("/login")) return NextResponse.next();
  if (request.cookies.get("TMS_SESSION")?.value) return NextResponse.next();

  const url = request.nextUrl.clone();
  url.pathname = "/login";
  url.search = `?next=${encodeURIComponent(pathname + search)}`;
  return NextResponse.redirect(url);
}

export const config = {
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico|robots.txt).*)"],
};
