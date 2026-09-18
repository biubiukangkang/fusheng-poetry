// 本地数据底座：IndexedDB 存诗集、展品与照片（第一轮无云端，数据只在设备本地）
const DB_NAME = "fusheng-db";
const VERSION = 1;
const STORES = ["poems", "exhibits", "photos"] as const;
type StoreName = (typeof STORES)[number];

let dbPromise: Promise<IDBDatabase> | null = null;

function open(): Promise<IDBDatabase> {
  if (!dbPromise) {
    dbPromise = new Promise((resolve, reject) => {
      const req = indexedDB.open(DB_NAME, VERSION);
      req.onupgradeneeded = () => {
        const db = req.result;
        for (const name of STORES) {
          if (!db.objectStoreNames.contains(name)) db.createObjectStore(name);
        }
      };
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error ?? new Error("IndexedDB 打开失败"));
    });
  }
  return dbPromise;
}

async function tx<T>(
  store: StoreName,
  mode: IDBTransactionMode,
  run: (s: IDBObjectStore) => IDBRequest<T>,
): Promise<T> {
  const db = await open();
  return new Promise<T>((resolve, reject) => {
    const t = db.transaction(store, mode);
    const req = run(t.objectStore(store));
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error ?? new Error("IndexedDB 操作失败"));
  });
}

export function dbPut<T>(store: StoreName, key: string, value: T): Promise<IDBValidKey> {
  return tx(store, "readwrite", (s) => s.put(value, key));
}

export function dbGet<T>(store: StoreName, key: string): Promise<T | undefined> {
  return tx(store, "readonly", (s) => s.get(key) as IDBRequest<T | undefined>);
}

export function dbGetAll<T>(store: StoreName): Promise<T[]> {
  return tx(store, "readonly", (s) => s.getAll() as IDBRequest<T[]>);
}

export function dbDelete(store: StoreName, key: string): Promise<undefined> {
  return tx(store, "readwrite", (s) => s.delete(key));
}
