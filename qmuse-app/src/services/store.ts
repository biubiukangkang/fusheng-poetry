// 本地数据服务：诗集、展品、照片全部存设备 IndexedDB（第一轮无云端依赖）
import { dbDelete, dbGet, dbGetAll, dbPut } from "@/lib/localdb";

// ===== 诗集（用户拾的诗） =====

export interface PoemRow {
  id: string;
  poemKey: string; // 内置词库 id（如 p001）
  title: string;
  author: string;
  dynasty: string;
  content: string; // 全文，句间以 \n 分隔
  focusLine: string;
  tags: string;
  createdAt: string; // ISO 时间
}

export async function listPickedPoems(): Promise<PoemRow[]> {
  const rows = await dbGetAll<PoemRow>("poems");
  return rows.sort((a, b) => b.createdAt.localeCompare(a.createdAt));
}

export async function pickPoem(input: {
  poemKey: string;
  title: string;
  author: string;
  dynasty: string;
  content: string;
  focusLine: string;
  tags: string;
}): Promise<PoemRow> {
  const row: PoemRow = {
    id: crypto.randomUUID(),
    createdAt: new Date().toISOString(),
    ...input,
  };
  await dbPut("poems", row.id, row);
  return row;
}

export async function unpickPoem(rowId: string): Promise<void> {
  await dbDelete("poems", rowId);
}

// ===== 展品 =====

export interface ExhibitRow {
  id: string;
  photoId: string;
  note: string;
  poemId: string; // 关联 PoemRow.id
  focusLine: string;
  source: string; // manual | ai
  createdAt: string; // ISO 时间
}

/** 供渲染的展品：带照片 blob URL */
export interface ExhibitWithPhoto extends ExhibitRow {
  photoUrl: string;
}

const objectUrls = new Map<string, string>();

export async function listExhibits(): Promise<ExhibitWithPhoto[]> {
  const rows = await dbGetAll<ExhibitRow>("exhibits");
  rows.sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  return Promise.all(
    rows.map(async (row) => {
      let url = objectUrls.get(row.photoId);
      if (!url) {
        const blob = await dbGet<Blob>("photos", row.photoId);
        url = URL.createObjectURL(blob ?? new Blob());
        objectUrls.set(row.photoId, url);
      }
      return { ...row, photoUrl: url };
    }),
  );
}

export async function removeExhibit(rowId: string): Promise<void> {
  const row = await dbGet<ExhibitRow>("exhibits", rowId);
  await dbDelete("exhibits", rowId);
  if (row) {
    await dbDelete("photos", row.photoId);
    const url = objectUrls.get(row.photoId);
    if (url) {
      URL.revokeObjectURL(url);
      objectUrls.delete(row.photoId);
    }
  }
}

export async function createExhibit(input: {
  photoBlob: Blob;
  note: string;
  poemId: string;
  focusLine: string;
}): Promise<ExhibitRow> {
  const photoId = crypto.randomUUID();
  await dbPut("photos", photoId, input.photoBlob);
  const row: ExhibitRow = {
    id: crypto.randomUUID(),
    photoId,
    note: input.note,
    poemId: input.poemId,
    focusLine: input.focusLine,
    source: "manual",
    createdAt: new Date().toISOString(),
  };
  await dbPut("exhibits", row.id, row);
  return row;
}
