// 浮生诗集 API：手机 App 的局域网归档主库（全本地，不上云）
import { serve } from "@hono/node-server";
import { Hono } from "hono";
import { corpus } from "./corpus.js";
import syncRoutes from "./routes/sync.js";
import photoRoutes from "./routes/photos.js";

const app = new Hono();

// 健康检查免鉴权（测连通用）
app.get("/api/health", (c) => c.json({ ok: true, time: new Date().toISOString() }));

// Bearer Token 鉴权
app.use("/api/*", async (c, next) => {
  const token = (c.req.header("Authorization") ?? "").replace(/^Bearer\s+/i, "");
  if (!token || token !== process.env.FUSHENG_TOKEN) {
    return c.text("未授权", 401);
  }
  await next();
});

app.get("/api/corpus", (c) => c.json(corpus));
app.route("/api/sync", syncRoutes);
app.route("/api/photos", photoRoutes);

const port = Number(process.env.PORT ?? 8787);
serve({ fetch: app.fetch, port, hostname: "0.0.0.0" }, (info) => {
  console.log(`浮生诗集 API 已启动 http://localhost:${info.port}`);
});
