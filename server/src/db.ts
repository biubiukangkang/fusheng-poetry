// 数据底座：node:sqlite（Node 24 内置）+ 照片文件目录
import { DatabaseSync } from "node:sqlite";
import { mkdirSync } from "node:fs";
import path from "node:path";

const dataDir = path.join(import.meta.dirname, "..", "data");
export const uploadsDir = path.join(dataDir, "uploads");
mkdirSync(uploadsDir, { recursive: true });

export const db = new DatabaseSync(path.join(dataDir, "fusheng.db"));

db.exec(`
  CREATE TABLE IF NOT EXISTS poems (
    id TEXT PRIMARY KEY,
    poemKey TEXT NOT NULL,
    title TEXT NOT NULL,
    author TEXT NOT NULL,
    dynasty TEXT NOT NULL,
    content TEXT NOT NULL,
    focusLine TEXT NOT NULL,
    tags TEXT NOT NULL,
    createdAt TEXT NOT NULL,
    deletedAt TEXT
  );
  CREATE TABLE IF NOT EXISTS exhibits (
    id TEXT PRIMARY KEY,
    photoId TEXT NOT NULL,
    note TEXT NOT NULL,
    poemId TEXT NOT NULL,
    focusLine TEXT NOT NULL,
    source TEXT NOT NULL,
    createdAt TEXT NOT NULL,
    deletedAt TEXT
  );
  CREATE TABLE IF NOT EXISTS photos (
    id TEXT PRIMARY KEY,
    file TEXT NOT NULL,
    bytes INTEGER NOT NULL,
    mime TEXT NOT NULL,
    createdAt TEXT NOT NULL
  );
`);

// 同步行类型（与 qmuse-app/src/services/store.ts 的 PoemRow/ExhibitRow 对齐，加墓碑字段）
export interface PoemRow {
  id: string;
  poemKey: string;
  title: string;
  author: string;
  dynasty: string;
  content: string;
  focusLine: string;
  tags: string;
  createdAt: string;
  deletedAt?: string | null;
}

export interface ExhibitRow {
  id: string;
  photoId: string;
  note: string;
  poemId: string;
  focusLine: string;
  source: string;
  createdAt: string;
  deletedAt?: string | null;
}
