import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AppShell } from "@/components/app-shell";
import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { corpus } from "@/data/corpus";
import { demoExhibits } from "@/data/exhibits";
import { drawShareCard } from "@/lib/share-image";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  listExhibits,
  listPickedPoems,
  removeExhibit,
  type PoemRow,
} from "@/services/store";

export const Route = createFileRoute("/")({
  component: Hall,
});

interface FeedItem {
  key: string;
  photo: string;
  /** 初始猜测；真实方向在图片加载后按 naturalWidth 修正 */
  landscape?: boolean;
  focusLine: string;
  meta: string;
  note: string;
  /** 用户展品的行 id（可移除）；预置展品为 null */
  rowId: string | null;
  createdAt: Date;
}

function Hall() {
  const queryClient = useQueryClient();
  const exhibitsQ = useQuery({
    queryKey: ["exhibits"],
    queryFn: listExhibits,
  });
  const poemsQ = useQuery({
    queryKey: ["poems"],
    queryFn: listPickedPoems,
  });

  const removeMut = useMutation({
    mutationFn: (rowId: string) => removeExhibit(rowId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["exhibits"] }),
  });

  // 分享图：生成后页内预览，移动端长按保存
  const [shareUrl, setShareUrl] = useState<string | null>(null);
  const shareMut = useMutation({
    mutationFn: async (item: FeedItem) =>
      drawShareCard({
        photoUrl: item.photo,
        focusLine: item.focusLine,
        meta: item.meta,
        note: item.note,
      }),
    onSuccess: (blob) => setShareUrl(URL.createObjectURL(blob)),
  });

  const items: FeedItem[] = [];
  // 用户展品（本地 IndexedDB）
  if (exhibitsQ.data && poemsQ.data) {
    const poemMap = new Map<string, PoemRow>(
      poemsQ.data.map((p) => [p.id, p]),
    );
    for (const row of exhibitsQ.data) {
      const poem = poemMap.get(row.poemId);
      items.push({
        key: row.id,
        rowId: row.id,
        photo: row.photoUrl,
        focusLine: row.focusLine || poem?.focusLine || "",
        meta: poem ? `${poem.author} · ${poem.title}` : "",
        note: row.note || "",
        createdAt: new Date(row.createdAt),
      });
    }
  }
  // 预置展品
  for (const d of demoExhibits) {
    const poem = corpus.find((p) => p.id === d.poemId);
    items.push({
      key: d.id,
      rowId: null,
      photo: d.photo,
      landscape: d.landscape,
      focusLine: d.focusLine,
      meta: poem ? `${poem.author} · ${poem.title}` : "",
      note: d.note,
      createdAt: new Date(d.createdAt),
    });
  }
  items.sort((a, b) => b.createdAt.getTime() - a.createdAt.getTime());

  // 按月分组
  const groups: { month: string; items: FeedItem[] }[] = [];
  for (const item of items) {
    const month = `${item.createdAt.getFullYear()} 年 ${item.createdAt.getMonth() + 1} 月`;
    const last = groups.at(-1);
    if (last && last.month === month) last.items.push(item);
    else groups.push({ month, items: [item] });
  }

  return (
    <AppShell>
      <div className="px-4 pb-6 pt-1">
      {groups.map((g) => (
        <section key={g.month}>
          <div className="flex items-center gap-3 px-1 pb-3 pt-4">
            <span className="text-[13px] tracking-[0.25em] text-muted-foreground">
              {g.month}
            </span>
            <span className="h-px flex-1 bg-border" />
          </div>
          {g.items.map((item) => (
            <ExhibitCard
              key={item.key}
              item={item}
              onRemove={
                item.rowId
                  ? () => removeMut.mutate(item.rowId!)
                  : undefined
              }
              onShare={() => void shareMut.mutate(item)}
            />
          ))}
        </section>
      ))}
      <p className="pb-2 pt-4 text-center text-[11px] tracking-[0.2em] text-muted-foreground/60">
        · 虚位以待 ·
      </p>
      {(exhibitsQ.error || poemsQ.error) && (
        <p className="pt-2 text-center text-[11px] text-muted-foreground/70">
          本地数据暂不可用，正在展示内置展品
        </p>
      )}

      <Dialog open={shareUrl !== null} onOpenChange={(o) => !o && setShareUrl(null)}>
        <DialogContent className="max-w-[330px] rounded-2xl p-4">
          <DialogHeader className="sr-only">
            <DialogTitle>分享图</DialogTitle>
          </DialogHeader>
          {shareUrl && (
            <>
              <img src={shareUrl} alt="分享图" className="w-full rounded-lg" />
              <p className="pt-1 text-center text-[11px] leading-relaxed text-muted-foreground">
                长按图片保存 · 发给朋友
              </p>
            </>
          )}
        </DialogContent>
      </Dialog>
    </div>
    </AppShell>
  );
}

function ExhibitCard({
  item,
  onRemove,
  onShare,
}: {
  item: FeedItem;
  onRemove?: () => void;
  onShare: () => void;
}) {
  const [landscape, setLandscape] = useState(item.landscape ?? false);
  const [loaded, setLoaded] = useState(false);
  const date = `${item.createdAt.getMonth() + 1}.${String(item.createdAt.getDate()).padStart(2, "0")}`;
  return (
    <article className="mb-4 overflow-hidden rounded-[14px] bg-card shadow-[0_1px_2px_rgba(32,42,48,0.04),0_10px_30px_rgba(32,42,48,0.06)]">
      <div className="relative">
        <img
          src={item.photo}
          alt={item.focusLine}
          loading="lazy"
          onLoad={(e) => {
            const img = e.currentTarget;
            setLandscape(img.naturalWidth >= img.naturalHeight);
            setLoaded(true);
          }}
          className={`w-full object-cover transition-[opacity,filter] duration-500 ${loaded ? "opacity-100 blur-0" : "opacity-0 blur-md"} ${landscape ? "aspect-[16/10]" : "aspect-[4/5]"}`}
        />
        <span className="absolute top-3 left-3 rounded-full bg-background/90 px-3 py-1 text-[10px] tracking-[0.12em] text-foreground">
          {date}
        </span>
        <div className="absolute top-2.5 right-2.5">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button
                type="button"
                aria-label="展品操作"
                className="flex h-7 w-7 items-center justify-center rounded-full bg-background/90 text-sm leading-none text-foreground"
              >
                ⋯
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onSelect={() => onShare()}>
                生成分享图
              </DropdownMenuItem>
              {onRemove && (
                <DropdownMenuItem
                  className="text-destructive"
                  onSelect={() => onRemove()}
                >
                  移出展馆
                </DropdownMenuItem>
              )}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
      <div className="px-4 pt-4 pb-5 text-center">
        <p className="font-serif text-xl leading-[1.7] tracking-[0.04em] text-foreground">
          {item.focusLine}
        </p>
        <p className="mt-2.5 text-[11px] tracking-[0.14em] text-muted-foreground">
          {item.meta}
        </p>
        {item.note && (
          <p className="mt-2 text-xs text-muted-foreground/80">
            <span className="text-muted-foreground/50">— </span>
            {item.note}
          </p>
        )}
      </div>
    </article>
  );
}
