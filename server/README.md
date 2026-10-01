# server/ · 归档 API

浮生诗集 v3 的 PC 端归档库：手机 App 是数据真源，本服务跑在 PC 上做备份与恢复点。全本地，不上云。

**状态（2026-09-27）**：App v3.1.0 已删除同步代码（SyncEngine），当前不再调用本服务；服务与端点保留可用，将来要做备份/多端时按本 README 重接即可。

## 运行

```bash
cd server
npm install
npm run dev        # 启动在 :8787（监听 0.0.0.0，供局域网访问）
```

## 鉴权

`server/.env` 里的 `FUSHENG_TOKEN`（已有现值；`.env.example` 是模板）。客户端请求须带同一 Token（v3.1.0 起 App 端已无配置入口）。`.env` 被 .gitignore 覆盖，不进 Git。

## API 端点

| 端点 | 说明 |
|---|---|
| `GET /api/health` | 健康检查（免鉴权） |
| `GET /api/corpus` | 词库 50 首 |
| `POST /api/sync/push` | App 增量推送（拾诗/展品 upsert by id，含墓碑删除，幂等） |
| `GET /api/sync/pull` | 全量恢复（只返回活行 + 照片 id 清单） |
| `POST/GET /api/photos/:id` | 照片二进制上传/下载 |

除 health 外全部要求 `Authorization: Bearer <FUSHENG_TOKEN>`。

## 数据目录（`server/data/`，不进 Git）

- `fusheng.db` — SQLite（Node 24 内置 node:sqlite），三张表：poems / exhibits / photos
- `uploads/<YYYYMM>/<photoId>` — 照片文件按月分目录

备份 = 复制整个 `server/data/` 目录。

## 冒烟验证

```bash
curl http://localhost:8787/api/health
curl -H "Authorization: Bearer <token>" http://localhost:8787/api/sync/pull
```

技术栈与实现细节见 `src/`（入口 `index.ts`，建表 `db.ts`，词库副本 `corpus.ts`）；Windows 侧的坑见根目录 `踩坑日志.md`。
