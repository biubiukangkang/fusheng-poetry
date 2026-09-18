import { defineConfig } from '@qmuse/vite-config';

// @qmuse/vite-config already includes the following — do NOT add or register
// them again, or the app may break because of duplicate plugins:
//   - TanStack Router
//   - React
//   - Tailwind CSS
//   - TypeScript path aliases
//
// Add only genuinely additional configuration through defineConfig({ ... }).
export default defineConfig({
  build: {
    rollupOptions: {
      output: {
        entryFileNames: 'assets/chunk.[hash].js',
        chunkFileNames: 'assets/chunk.[hash].js',
        assetFileNames: 'assets/chunk.[hash][extname]',
        // 平台 iframe 内动态 import 路由 chunk 加载不稳，全部合入单 chunk
        manualChunks: () => 'app',
      },
    },
  },
});
