import { Link, useRouter, useRouterState } from "@tanstack/react-router";
import type { ReactNode } from "react";

// 页面标题与二级页判定
const TITLES: Record<string, string> = {
  "/": "浮生诗集",
  "/seek": "觅诗",
  "/compose": "入馆",
  "/anthology": "诗集",
  "/me": "我",
  "/settings": "设置",
  "/about": "关于",
};

function resolveTitle(pathname: string): { title: string; sub: boolean } {
  const t = TITLES[pathname];
  return { title: t ?? "浮生诗集", sub: ["/settings", "/about", "/compose"].includes(pathname) };
}

const TABS = [
  { to: "/", label: "浮生馆", icon: "hall" },
  { to: "/seek", label: "觅诗", icon: "quote" },
  { to: "/compose", label: "", icon: "plus" },
  { to: "/anthology", label: "诗集", icon: "book" },
  { to: "/me", label: "我", icon: "me" },
] as const;

function TabIcon({ name }: { name: string }) {
  const common = {
    width: 21,
    height: 21,
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    strokeWidth: 1.5,
    strokeLinecap: "round" as const,
    strokeLinejoin: "round" as const,
  };
  if (name === "hall")
    return (
      <svg {...common}>
        <path d="M4 21V9M9 21V9M15 21V9M20 21V9M2 9l10-6 10 6M3 21h18" />
      </svg>
    );
  if (name === "quote")
    return (
      <svg {...common}>
        <path d="M10 8H6a2 2 0 0 0-2 2v10h6zM10 8l4-4v16h-4M6 8V4h4" />
        <path d="M14 20h6V6a2 2 0 0 0-2-2h-4" />
      </svg>
    );
  if (name === "plus")
    return (
      <svg width={22} height={22} viewBox="0 0 24 24" fill="none" stroke="#FAF9F6" strokeWidth={1.8} strokeLinecap="round">
        <path d="M12 5v14M5 12h14" />
      </svg>
    );
  if (name === "book")
    return (
      <svg {...common}>
        <path d="M12 6c-1.8-1.6-4.4-2-8-2v15c3.6 0 6.2.4 8 2 1.8-1.6 4.4-2 8-2V4c-3.6 0-6.2.4-8 2z" />
        <path d="M12 6v15" />
      </svg>
    );
  return (
    <svg {...common}>
      <circle cx="12" cy="8" r="3.4" />
      <path d="M5.5 21c.8-3.6 3.3-5.4 6.5-5.4s5.7 1.8 6.5 5.4" />
    </svg>
  );
}

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = useRouterState({ select: (s) => s.location.pathname });
  const router = useRouter();
  const { title, sub } = resolveTitle(pathname);

  const isActive = (to: string) =>
    to === "/" ? pathname === "/" : pathname.startsWith(to);

  return (
    <div className="flex h-svh flex-col bg-background">
      <header
        className="flex-none bg-background"
        style={{ paddingTop: "env(safe-area-inset-top)" }}
      >
        <div className="relative flex h-11 items-center justify-center border-b border-border">
          {sub && (
            <button
              type="button"
              aria-label="返回"
              className="absolute left-3 top-1/2 -translate-y-1/2 px-1 text-2xl leading-none text-foreground"
              onClick={() => router.history.back()}
            >
              ‹
            </button>
          )}
          <h1 className="text-[17px] font-semibold tracking-[0.14em] text-foreground indent-[0.14em]">
            {title}
          </h1>
        </div>
      </header>

      <div className="min-h-0 flex-1 overflow-y-auto">{children}</div>

      <nav
        className="flex-none border-t border-border bg-card/97 backdrop-blur"
        style={{ paddingBottom: "env(safe-area-inset-bottom)" }}
      >
        <div className="flex items-start justify-around px-2 pt-2.5 pb-1.5">
          {TABS.map((tab) => {
            const active = isActive(tab.to);
            if (tab.icon === "plus") {
              return (
                <Link
                  key={tab.to}
                  to={tab.to}
                  aria-label="创作入馆"
                  className="flex w-16 flex-col items-center gap-1"
                >
                  <span className="-mt-6 flex h-13 w-13 items-center justify-center rounded-full bg-foreground shadow-lg shadow-foreground/25">
                    <TabIcon name="plus" />
                  </span>
                </Link>
              );
            }
            return (
              <Link
                key={tab.to}
                to={tab.to}
                className="flex w-16 flex-col items-center gap-1"
              >
                <TabIcon name={tab.icon} />
                <span
                  className={`text-[10px] tracking-[0.06em] ${active ? "font-semibold text-foreground" : "text-muted-foreground"}`}
                >
                  {tab.label}
                </span>
              </Link>
            );
          })}
        </div>
      </nav>
    </div>
  );
}
