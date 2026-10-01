# 浮生诗集

用诗词描述生活，用生活表达诗意——古诗词 × 生活照片的私人展览馆。为一张生活照觅一首古诗词，配上注记入馆展出；也可以在名句流里遇见一句喜欢的诗，拾进诗集，等一张合适的照片。

线上：https://qmuse.cn/app/5255402544676766 （QMuse 平台 · 支付宝生态）

## 功能

- **浮生馆**：展品信息流（按月分组，横竖构图按原比例）
- **觅诗**：每日一句 + 名句流 50 首，一键「拾」入诗集
- **＋**：创作入馆——传一张生活照，从诗集选诗 / 自己填 / AI 觅句（规划中），写一句注记
- **诗集**：拾藏的诗，点开即可配照
- **分享图**：任一展品一键生成竖版卡片（照片 + 诗句 + 馆名印章），长按保存转发

数据保存在设备本地（IndexedDB），无需账号。AI 觅句（第二轮开发中）由 QMuse 云函数代理 dots.ai 接口，零配置即用；「我 → 设置」的自备接口表单保留为高级选项。

## 开发

```bash
npm install        # 或 cnpm install
npm run check      # 路由生成 + lint + 类型检查
npm run build      # 构建到 dist/
```

本地预览：`npx vite preview --port 5180 --strictPort`（注意 `--port` 必须直接传给 vite）。

发布：`qmuse import . --cloud-mode QMUSE --confirm-cloud-service`（需 qmuse CLI 登录态）。

## 结构

- `src/routes/` — 五键页面 + 创作页（compose）+ 设置/关于
- `src/data/corpus.ts` — 精选词库 50 首（原文取自 chinese-poetry，MIT）
- `src/lib/localdb.ts` — IndexedDB 封装（poems / exhibits / photos）
- `src/services/store.ts` — 数据服务层
- `src/lib/share-image.ts` — canvas 分享卡生成

技术栈：React 19 · Vite · TanStack Router · Tailwind v4 · shadcn/ui
