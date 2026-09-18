import { useQuery } from "@tanstack/react-query";
import { AppShell } from "@/components/app-shell";
import { createFileRoute, Link } from "@tanstack/react-router";
import { listExhibits, listPickedPoems } from "@/services/store";

export const Route = createFileRoute("/me")({
  component: Me,
});

function Me() {
  const poemsQ = useQuery({ queryKey: ["poems"], queryFn: listPickedPoems });
  const exhibitsQ = useQuery({
    queryKey: ["exhibits"],
    queryFn: listExhibits,
  });

  const pickedCount = poemsQ.data?.length ?? 0;
  const exhibitCount = exhibitsQ.data?.length ?? 0;

  return (
    <AppShell>
      <div className="px-4 pt-5 pb-6">
      <div className="pt-4 pb-7 text-center">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full border-[1.5px] border-border bg-card font-serif text-[26px] text-foreground shadow-[0_4px_14px_rgba(32,42,48,0.05)]">
          浮
        </div>
        <p className="mt-3.5 text-base tracking-[0.2em] text-foreground indent-[0.2em]">
          浮生客
        </p>
        <p className="mt-2 text-[11px] tracking-[0.1em] text-muted-foreground">
          入馆 {exhibitCount} 件 · 拾诗 {pickedCount} 首
        </p>
      </div>

      <p className="px-1 pt-4 pb-2.5 text-xs tracking-[0.18em] text-muted-foreground">
        通用
      </p>
      <div className="overflow-hidden rounded-xl bg-card shadow-[0_1px_2px_rgba(32,42,48,0.03),0_4px_14px_rgba(32,42,48,0.03)]">
        <Link
          to="/settings"
          className="flex items-center justify-between border-b border-border px-4 py-4 text-[13px] text-foreground"
        >
          设置
          <span className="text-sm text-muted-foreground/50">›</span>
        </Link>
        <Link
          to="/about"
          className="flex items-center justify-between px-4 py-4 text-[13px] text-foreground"
        >
          关于浮生诗集
          <span className="text-sm text-muted-foreground/50">›</span>
        </Link>
      </div>
    </div>
    </AppShell>
  );
}
