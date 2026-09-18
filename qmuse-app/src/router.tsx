import { QueryClient } from "@tanstack/react-query";
import { createHashHistory, createRouter } from "@tanstack/react-router";
import { routeTree } from "./routeTree.gen";

export const getRouter = () => {
  // 云查询快速失败：1 次重试后转入降级态，避免「载入中」长驻
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: 1, retryDelay: 1500 } },
  });
  const hashHistory = createHashHistory();

  const router = createRouter({
    routeTree,
    history: hashHistory,
    context: { queryClient },
    scrollRestoration: true,
    defaultPreloadStaleTime: 0,
  });

  return router;
};
