import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useRef, useState } from "react";
import { AppShell } from "@/components/app-shell";
import { compressImage } from "@/lib/image";
import {
  createExhibit,
  listPickedPoems,
  pickPoem,
  type PoemRow,
} from "@/services/store";

export const Route = createFileRoute("/compose")({
  validateSearch: (search: Record<string, unknown>) => ({
    poem: typeof search.poem === "string" ? search.poem : undefined,
  }),
  component: Compose,
});

type Mode = "pick" | "write" | "ai";

function Compose() {
  const { poem: presetPoemId } = Route.useSearch();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [note, setNote] = useState("");
  const [error, setError] = useState<string | null>(null);

  const poemsQ = useQuery({
    queryKey: ["poems"],
    queryFn: listPickedPoems,
  });
  const presetPoem =
    presetPoemId && poemsQ.data
      ? poemsQ.data.find((p) => p.id === presetPoemId)
      : undefined;
  const [selectedPoem, setSelectedPoem] = useState<PoemRow | null>(null);
  const effectivePoem = selectedPoem ?? presetPoem ?? null;

  const [mode, setMode] = useState<Mode>(presetPoem ? "pick" : "pick");
  // 自己填的诗
  const [writeTitle, setWriteTitle] = useState("");
  const [writeAuthor, setWriteAuthor] = useState("");
  const [writeLines, setWriteLines] = useState("");

  const submitMut = useMutation({
    mutationFn: async () => {
      if (!file) throw new Error("请先选择照片");
      // 1) 确定诗：自己填的先拾入诗集
      let poem = effectivePoem;
      if (mode === "write") {
        if (!writeTitle.trim() || !writeLines.trim())
          throw new Error("请填写诗题和诗句");
        poem = await pickPoem({
          poemKey: "",
          title: writeTitle.trim(),
          author: writeAuthor.trim() || "佚名",
          dynasty: "",
          content: writeLines.trim(),
          focusLine: writeLines.trim().split("\n")[0],
          tags: "",
        });
      }
      if (!poem) throw new Error("请先选一首诗");
      // 2) 压缩上传 + 入馆
      const blob = await compressImage(file);
      return createExhibit({
        photoBlob: blob,
        note: note.trim(),
        poemId: poem.id,
        focusLine: poem.focusLine,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["exhibits"] });
      queryClient.invalidateQueries({ queryKey: ["poems"] });
      void navigate({ to: "/" });
    },
    onError: (e) => setError(e instanceof Error ? e.message : "入馆失败，请重试"),
  });

  return (
    <AppShell>
      <div className="px-5 pt-4 pb-8">
        {/* 一、照片 */}
        <p className="px-1 pt-1 pb-2.5 text-[13px] text-foreground">一张生活照</p>
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        className="hidden"
        onChange={(e) => {
          const f = e.target.files?.[0];
          if (f) {
            setFile(f);
            setPreviewUrl(URL.createObjectURL(f));
          }
          e.target.value = "";
        }}
      />
      {previewUrl ? (
        <div className="relative overflow-hidden rounded-[14px]">
          <img src={previewUrl} alt="预览" className="w-full object-cover" />
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            className="absolute right-3 bottom-3 rounded-full bg-background/90 px-4 py-1.5 text-xs text-foreground"
          >
            重选
          </button>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          className="flex aspect-[4/3] w-full flex-col items-center justify-center gap-3 rounded-[14px] border-[1.5px] border-dashed border-border bg-card text-xs tracking-[0.1em] text-muted-foreground"
        >
          <svg
            width="30"
            height="30"
            viewBox="0 0 24 24"
            fill="none"
            stroke="#C2C8C4"
            strokeWidth="1.4"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <rect x="3" y="5" width="18" height="14" rx="2" />
            <circle cx="9" cy="10" r="1.6" />
            <path d="M3.5 17l5-5 4 4 3.5-3.5 4.5 4.5" />
          </svg>
          拍照 或 从相册选择
        </button>
      )}

      {/* 二、配什么诗 */}
      <p className="px-1 pt-6 pb-2.5 text-[13px] text-foreground">配什么诗</p>
      <div className="flex gap-2">
        {(
          [
            ["pick", "从诗集选"],
            ["write", "自己填"],
            ["ai", "AI 觅句"],
          ] as const
        ).map(([m, label]) => (
          <button
            key={m}
            type="button"
            disabled={m === "ai"}
            onClick={() => setMode(m)}
            className={`rounded-full px-4 py-1.5 text-xs tracking-[0.08em] ${
              mode === m
                ? "bg-foreground text-background"
                : "border border-border text-muted-foreground disabled:opacity-40"
            }`}
          >
            {m === "ai" ? "AI 觅句（即将）" : label}
          </button>
        ))}
      </div>

      {mode === "pick" && (
        <div className="pt-3">
          {presetPoem && !selectedPoem && (
            <div className="mb-2.5 rounded-xl border border-border bg-card px-4 py-3">
              <p className="font-serif text-[15px] text-foreground">
                {presetPoem.focusLine}
              </p>
              <p className="mt-1 text-[11px] text-muted-foreground">
                {presetPoem.author} · {presetPoem.title} · 已选定
              </p>
            </div>
          )}
          {selectedPoem && (
            <div className="mb-2.5 rounded-xl border border-border bg-card px-4 py-3">
              <p className="font-serif text-[15px] text-foreground">
                {selectedPoem.focusLine}
              </p>
              <p className="mt-1 text-[11px] text-muted-foreground">
                {selectedPoem.author} · {selectedPoem.title} · 已选定
              </p>
            </div>
          )}
          {(poemsQ.data ?? []).filter((p) => p.id !== effectivePoem?.id).length >
          0 ? (
            (poemsQ.data ?? [])
              .filter((p) => p.id !== effectivePoem?.id)
              .map((p) => (
                <button
                  key={p.id}
                  type="button"
                  onClick={() => setSelectedPoem(p)}
                  className="mb-2 flex w-full items-center justify-between gap-3 rounded-xl bg-card px-4 py-3 text-left shadow-[0_1px_2px_rgba(32,42,48,0.03)]"
                >
                  <span className="min-w-0">
                    <span className="block truncate font-serif text-[15px] text-foreground">
                      {p.focusLine}
                    </span>
                    <span className="mt-1 block text-[11px] text-muted-foreground">
                      {p.author} · {p.title}
                    </span>
                  </span>
                  <span className="flex-none text-sm text-muted-foreground/50">
                    选
                  </span>
                </button>
              ))
          ) : (
            !effectivePoem && (
              <p className="pt-2 text-[11px] leading-loose text-muted-foreground">
                诗集里还没有诗——去「觅诗」页拾一首，或切到「自己填」
              </p>
            )
          )}
        </div>
      )}

      {mode === "write" && (
        <div className="mt-3 space-y-2.5">
          <input
            value={writeTitle}
            onChange={(e) => setWriteTitle(e.target.value)}
            placeholder="诗题，如：山行"
            className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm text-foreground placeholder:text-muted-foreground/60 focus:outline-none"
          />
          <input
            value={writeAuthor}
            onChange={(e) => setWriteAuthor(e.target.value)}
            placeholder="作者（可留空）"
            className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm text-foreground placeholder:text-muted-foreground/60 focus:outline-none"
          />
          <textarea
            value={writeLines}
            onChange={(e) => setWriteLines(e.target.value)}
            rows={4}
            placeholder={"诗句，一行一句\n如：远上寒山石径斜\n白云生处有人家"}
            className="w-full resize-none rounded-xl border border-border bg-card px-4 py-3 font-serif text-sm leading-loose text-foreground placeholder:text-muted-foreground/60 focus:outline-none"
          />
        </div>
      )}

      {mode === "ai" && <div className="pt-3" />}

      {/* 三、注记 */}
      <p className="px-1 pt-6 pb-2.5 text-[13px] text-foreground">
        写一句注记（可留空）
      </p>
      <textarea
        value={note}
        onChange={(e) => setNote(e.target.value)}
        rows={3}
        maxLength={100}
        placeholder="比如：加班晚归，楼下的桂花开了…"
        className="w-full resize-none rounded-xl border border-border bg-card px-4 py-3.5 text-sm leading-relaxed text-foreground placeholder:text-muted-foreground/60 focus:outline-none"
      />

      {error && <p className="pt-3 text-center text-xs text-destructive">{error}</p>}

      <button
        type="button"
        disabled={
          !file || submitMut.isPending || (mode === "pick" && !effectivePoem)
        }
        onClick={() => submitMut.mutate()}
        className="mt-7 w-full rounded-full bg-foreground py-4 text-[15px] tracking-[0.35em] text-background shadow-lg shadow-foreground/20 disabled:opacity-40"
      >
        {submitMut.isPending ? "入 馆 中" : "入 馆"}
      </button>
      </div>
    </AppShell>
  );
}
