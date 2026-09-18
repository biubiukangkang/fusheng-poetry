// AI 觅诗的用户自备接口配置（OpenAI 兼容），仅存本机
export interface AiSettings {
  baseUrl: string;
  apiKey: string;
  model: string;
}

const KEY = "fusheng-ai-settings";

export function getAiSettings(): AiSettings {
  try {
    const raw = localStorage.getItem(KEY);
    if (raw) return { baseUrl: "", apiKey: "", model: "", ...JSON.parse(raw) };
  } catch {
    // 忽略损坏的本地数据
  }
  return { baseUrl: "", apiKey: "", model: "" };
}

export function saveAiSettings(s: AiSettings): void {
  localStorage.setItem(KEY, JSON.stringify(s));
}

export function isAiConfigured(s: AiSettings): boolean {
  return Boolean(s.baseUrl.trim() && s.apiKey.trim() && s.model.trim());
}
