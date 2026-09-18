import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { AppShell } from "@/components/app-shell";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { listPickedPoems, unpickPoem } from "@/services/store";

export const Route = createFileRoute("/anthology")({
  component: Anthology,
});

function Anthology() {
  const queryClient = useQueryClient();
  const poemsQ = useQuery({
    queryKey: ["poems"],
    queryFn: listPickedPoems,
  });

  const removeMut = useMutation({
    mutationFn: (rowId: string) => unpickPoem(rowId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["poems"] }),
  });

  let content: React.ReactNode;
  if (poemsQ.isLoading) {
    content = (
      <p className="pt-16 text-center text-xs text-muted-foreground">载入中…</p>
    );
  } else if (poemsQ.error) {
    content = (
      <button
        type="button"
        className="w-full pt-16 text-center text-xs leading-loose text-muted-foreground"
        onClick={() => poemsQ.refetch()}
      >
        存储暂不可用
        <br />
        点击重试
      </button>
    );
  } else if ((poemsQ.data ?? []).length === 0) {
    content = (
      <div className="px-8 pt-20 text-center">
        <p className="text-xs leading-loose text-muted-foreground">
          拾进来的诗都在这里
          <br />
          去觅诗页拾一首
        </p>
        <Link
          to="/seek"
          className="mt-5 inline-block rounded-full border border-border px-6 py-2 text-xs tracking-[0.2em] text-foreground"
        >
          去名句
        </Link>
      </div>
    );
  } else {
    content = (
      <div className="px-4 pt-4 pb-6">
        {poemsQ.data!.map((poem) => (
          <div
            key={poem.id}
            className="mb-3 flex items-center justify-between gap-3 rounded-xl bg-card px-4 py-4 shadow-[0_1px_2px_rgba(32,42,48,0.03),0_4px_14px_rgba(32,42,48,0.03)]"
          >
            <Link
              to="/compose"
              search={{ poem: poem.id }}
              className="min-w-0 flex-1"
            >
              <p className="truncate font-serif text-base text-foreground">
                {poem.focusLine}
              </p>
              <p className="mt-1.5 text-[11px] tracking-[0.08em] text-muted-foreground">
                {poem.author} · {poem.title}
              </p>
            </Link>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <button
                  type="button"
                  aria-label="诗作操作"
                  className="flex h-7 w-7 flex-none items-center justify-center rounded-full text-sm leading-none text-muted-foreground"
                >
                  ⋯
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem
                  className="text-destructive"
                  onSelect={() => removeMut.mutate(poem.id)}
                >
                  移出诗集
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        ))}
        <p className="pt-3 text-center text-[11px] leading-loose text-muted-foreground">
          点开一首，为它配一张生活照
        </p>
      </div>
    );
  }

  return <AppShell>{content}</AppShell>;
}
