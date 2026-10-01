// 同步：App 增量推送（幂等 upsert + 墓碑删除）、全量恢复
import { Hono } from "hono";
import { db, type ExhibitRow, type PoemRow } from "../db.js";

interface PushBody {
  poems?: PoemRow[];
  exhibits?: ExhibitRow[];
}

const app = new Hono();

// POST /api/sync/push
app.post("/push", async (c) => {
  const body = await c.req.json<PushBody>();
  const upsertPoem = db.prepare(
    `INSERT OR REPLACE INTO poems (id, poemKey, title, author, dynasty, content, focusLine, tags, createdAt, deletedAt)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
  );
  const upsertExhibit = db.prepare(
    `INSERT OR REPLACE INTO exhibits (id, photoId, note, poemId, focusLine, source, createdAt, deletedAt)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
  );
  let poems = 0;
  let exhibits = 0;
  for (const p of body.poems ?? []) {
    upsertPoem.run(p.id, p.poemKey, p.title, p.author, p.dynasty, p.content, p.focusLine, p.tags, p.createdAt, p.deletedAt ?? null);
    poems++;
  }
  for (const e of body.exhibits ?? []) {
    upsertExhibit.run(e.id, e.photoId, e.note, e.poemId, e.focusLine, e.source, e.createdAt, e.deletedAt ?? null);
    exhibits++;
  }
  return c.json({ ok: true, poems, exhibits });
});

// GET /api/sync/pull（全量恢复，只给活行；照片 id 列表供 App 按需拉取）
app.get("/pull", (c) => {
  const poems = db.prepare("SELECT * FROM poems WHERE deletedAt IS NULL ORDER BY createdAt DESC").all() as unknown as PoemRow[];
  const exhibits = db.prepare("SELECT * FROM exhibits WHERE deletedAt IS NULL ORDER BY createdAt DESC").all() as unknown as ExhibitRow[];
  const photos = db.prepare("SELECT id, bytes FROM photos").all() as unknown as { id: string; bytes: number }[];
  return c.json({ poems, exhibits, photos });
});

export default app;
