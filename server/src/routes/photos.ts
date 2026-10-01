// 照片二进制：上传（App 压缩后 JPEG）与下载
import { Hono } from "hono";
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import { db, uploadsDir } from "../db.js";

const MAX_BYTES = 50 * 1024 * 1024;

const app = new Hono();

// POST /api/photos/:id（body 为原始二进制）
app.post("/:id", async (c) => {
  const id = c.req.param("id");
  if (!/^[0-9a-f-]{36}$/i.test(id)) return c.text("非法照片 id", 400);
  const buf = Buffer.from(await c.req.arrayBuffer());
  if (buf.byteLength === 0) return c.text("空文件", 400);
  if (buf.byteLength > MAX_BYTES) return c.text("超过 50MB 上限", 413);

  const month = new Date().toISOString().slice(0, 7).replace("-", "");
  const dir = path.join(uploadsDir, month);
  mkdirSync(dir, { recursive: true });
  writeFileSync(path.join(dir, id), buf);

  db.prepare("INSERT OR REPLACE INTO photos (id, file, bytes, mime, createdAt) VALUES (?, ?, ?, ?, ?)").run(
    id,
    `${month}/${id}`,
    buf.byteLength,
    c.req.header("Content-Type") ?? "image/jpeg",
    new Date().toISOString(),
  );
  return c.json({ ok: true, id, bytes: buf.byteLength });
});

// GET /api/photos/:id
app.get("/:id", (c) => {
  const row = db.prepare("SELECT file, mime FROM photos WHERE id = ?").get(c.req.param("id")) as
    | { file: string; mime: string }
    | undefined;
  if (!row) return c.text("未找到照片", 404);
  const file = path.join(uploadsDir, row.file);
  if (!existsSync(file)) return c.text("照片文件缺失", 404);
  return c.body(readFileSync(file), 200, { "Content-Type": row.mime });
});

export default app;
