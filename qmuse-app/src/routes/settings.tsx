import { AppShell } from "@/components/app-shell";
import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { getAiSettings, saveAiSettings } from "@/lib/settings";

export const Route = createFileRoute("/settings")({
  component: Settings,
});

function Settings() {
  const [form, setForm] = useState(getAiSettings);
  const [saved, setSaved] = useState(false);

  const set = (key: keyof typeof form) => (
    e: React.ChangeEvent<HTMLInputElement>,
  ) => {
    setForm((f) => ({ ...f, [key]: e.target.value }));
    setSaved(false);
  };

  return (
    <AppShell>
      <div className="px-4 pt-4 pb-6">
      <p className="px-1 pt-2 pb-2.5 text-xs tracking-[0.18em] text-muted-foreground">
        AI 觅诗
      </p>
      <div className="overflow-hidden rounded-xl bg-card shadow-[0_1px_2px_rgba(32,42,48,0.03),0_4px_14px_rgba(32,42,48,0.03)]">
        <label className="flex items-center justify-between gap-4 border-b border-border px-4 py-3.5">
          <span className="flex-none text-[13px] text-foreground">接口地址</span>
          <input
            value={form.baseUrl}
            onChange={set("baseUrl")}
            placeholder="https://…/v1"
            className="min-w-0 flex-1 text-right text-[13px] text-foreground placeholder:text-muted-foreground/50 focus:outline-none"
          />
        </label>
        <label className="flex items-center justify-between gap-4 border-b border-border px-4 py-3.5">
          <span className="flex-none text-[13px] text-foreground">API Key</span>
          <input
            value={form.apiKey}
            onChange={set("apiKey")}
            type="password"
            placeholder="sk-…"
            className="min-w-0 flex-1 text-right text-[13px] text-foreground placeholder:text-muted-foreground/50 focus:outline-none"
          />
        </label>
        <label className="flex items-center justify-between gap-4 px-4 py-3.5">
          <span className="flex-none text-[13px] text-foreground">模型名</span>
          <input
            value={form.model}
            onChange={set("model")}
            placeholder="glm-4v-flash"
            className="min-w-0 flex-1 text-right text-[13px] text-foreground placeholder:text-muted-foreground/50 focus:outline-none"
          />
        </label>
      </div>
      <p className="px-1 pt-3 text-[11px] leading-[1.9] text-muted-foreground">
        OpenAI 兼容格式，通吃智谱 / 通义 / Kimi / OpenAI
        等主流服务商。配置仅保存在本机；未配置时，拾句、配照、展览照常可用。
      </p>

      <button
        type="button"
        onClick={() => {
          saveAiSettings(form);
          setSaved(true);
        }}
        className="mt-6 w-full rounded-full bg-foreground py-3.5 text-sm tracking-[0.3em] text-background shadow-lg shadow-foreground/20"
      >
        {saved ? "已 保 存" : "保 存"}
      </button>
    </div>
    </AppShell>
  );
}
