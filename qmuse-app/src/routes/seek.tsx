import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AppShell } from "@/components/app-shell";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { corpus } from "@/data/corpus";
import { listPickedPoems, pickPoem } from "@/services/store";

export const Route = createFileRoute("/seek")({
  component: Seek,
});

function dayOfYear(d: Date): number {
  return Math.floor(
    (d.getTime() - new Date(d.getFullYear(), 0, 0).getTime()) / 86400000,
  );
}

function todayLine(): string {
  const now = new Date();
  const week = ["日", "一", "二", "三", "四", "五", "六"][now.getDay()];
  return `${now.getMonth() + 1} 月 ${now.getDate()} 日 · 星期${week}`;
}

function Seek() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const poemsQ = useQuery({
    queryKey: ["poems"],
    queryFn: listPickedPoems,
  });

  const pickMut = useMutation({
    mutationFn: (poemId: string) => {
      const poem = corpus.find((p) => p.id === poemId);
      if (!poem) throw new Error("词库中没有这首");
      return pickPoem({
        poemKey: poem.id,
        title: poem.title,
        author: poem.author,
        dynasty: poem.dynasty,
        content: poem.lines.join("\n"),
        focusLine: poem.focusLine,
        tags: poem.tags.join(","),
      });
    },
    onSuccess: () => {
      navigator.vibrate?.(15);
      queryClient.invalidateQueries({ queryKey: ["poems"] });
    },
  });

  const picked = new Set((poemsQ.data ?? []).map((p) => p.poemKey));
  const daily = corpus[dayOfYear(new Date()) % corpus.length];

  return (
    <AppShell>
      <div className="px-4 pb-6 pt-3">
      {/* 每日一句 */}
      <section className="rounded-[14px] bg-card px-6 pt-6 pb-5 text-center shadow-[0_1px_2px_rgba(32,42,48,0.04),0_10px_30px_rgba(32,42,48,0.06)]">
        <p className="text-[11px] tracking-[0.2em] text-muted-foreground">
          {todayLine()}
        </p>
        <p className="mt-4 font-serif text-[21px] leading-[1.8] tracking-[0.04em] text-foreground">
          {daily.focusLine.includes("，") && daily.focusLine.length > 8 ? (
            <>
              {daily.focusLine.split("，")[0]}
              <br />
              {daily.focusLine.split("，").slice(1).join("，")}
            </>
          ) : (
            daily.focusLine
          )}
        </p>
        <p className="mt-3 text-[11px] tracking-[0.14em] text-muted-foreground">
          {daily.author} · {daily.title}
        </p>
        <button
          type="button"
          disabled={picked.has(daily.id) || pickMut.isPending}
          onClick={() => pickMut.mutate(daily.id)}
          className="mt-4 rounded-full border border-border px-6 py-2 text-xs tracking-[0.2em] text-foreground disabled:opacity-50"
        >
          {picked.has(daily.id) ? "已 拾" : "拾入诗集"}
        </button>
      </section>

      {/* 名句流 */}
      <div className="pt-5" />
      {corpus.map((poem) => (
        <div
          key={poem.id}
          className="mb-2.5 flex items-center justify-between gap-3 rounded-xl bg-card px-4 py-4 shadow-[0_1px_2px_rgba(32,42,48,0.03),0_4px_14px_rgba(32,42,48,0.03)]"
        >
          <div className="min-w-0">
            <p className="font-serif text-[15px] leading-[1.6] text-foreground">
              {poem.focusLine}
            </p>
            <p className="mt-1.5 text-[11px] tracking-[0.08em] text-muted-foreground">
              {poem.author} · {poem.title}
            </p>
          </div>
          <button
            key={picked.has(poem.id) ? "picked" : "pick"}
            type="button"
            aria-label={picked.has(poem.id) ? "已拾入诗集" : "拾入诗集"}
            disabled={picked.has(poem.id) || pickMut.isPending}
            onClick={() => pickMut.mutate(poem.id)}
            className="flex h-8.5 w-8.5 flex-none animate-in fade-in zoom-in-95 items-center justify-center rounded-full border border-border text-xs text-foreground disabled:opacity-40"
          >
            {picked.has(poem.id) ? "✓" : "拾"}
          </button>
        </div>
      ))}
      {poemsQ.error && (
        <p className="pt-3 text-center text-[11px] text-muted-foreground/70">
          本地存储暂不可用，拾句暂不可用
        </p>
      )}
      </div>
    </AppShell>
  );
}
