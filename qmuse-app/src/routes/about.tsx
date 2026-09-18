import { AppShell } from "@/components/app-shell";
import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/about")({
  component: About,
});

function About() {
  return (
    <AppShell>
      <div className="px-6 pt-10 pb-6">
      <div className="pt-8 text-center">
        <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-foreground font-serif text-2xl text-background">
          浮
        </div>
        <p className="mt-4 text-[17px] tracking-[0.25em] text-foreground indent-[0.25em]">
          浮生诗集
        </p>
        <p className="mt-2 text-[11px] text-muted-foreground">v0.1 · QMuse</p>
      </div>
      <p className="pt-10 text-center text-xs leading-[2.3] tracking-[0.06em] text-muted-foreground">
        用诗词描述生活，用生活表达诗意
        <br />
        古人用文字为浮生作记
        <br />
        我们用照片与古诗词为生活成集
      </p>
    </div>
    </AppShell>
  );
}
