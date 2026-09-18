import chunri from "@/assets/exhibits/demo-chunri-v.jpg";
import chumen from "@/assets/exhibits/demo-chumen.jpg";
import qinghuan from "@/assets/exhibits/demo-qinghuan-v.jpg";
import yipiao from "@/assets/exhibits/demo-yipiao.jpg";
import zhenshu from "@/assets/exhibits/demo-zhenshu-v.jpg";

// 诗照：一首古诗词 × 一张生活照片 × 一句生活注记
export interface Exhibit {
  id: string;
  poemId: string;
  focusLine: string;
  photo: string;
  note: string;
  createdAt: string;
  /** photo 为横构图时 true（竖构图 false） */
  landscape: boolean;
}

// 预置示范展品（静态数据，随包发布，不可移除），按时间倒序
export const demoExhibits: Exhibit[] = [
  {
    id: "exhibit-demo-yipiao",
    poemId: "p005",
    focusLine: "我有一瓢酒，可以慰风尘",
    photo: yipiao,
    note: "夜班高铁上的一罐酒，敬风尘",
    createdAt: "2026-09-10",
    landscape: true,
  },
  {
    id: "exhibit-demo-chumen",
    poemId: "p003",
    focusLine: "一笑出门去，千里落花风",
    photo: chumen,
    note: "出差半月的行李箱落地，楼下早桂开了",
    createdAt: "2026-08-30",
    landscape: true,
  },
  {
    id: "exhibit-demo-zhenshu",
    poemId: "p002",
    focusLine: "枕上诗书闲处好，门前风景雨来佳",
    photo: zhenshu,
    note: "连着一周的雨，索性窝着把书读完",
    createdAt: "2026-07-02",
    landscape: false,
  },
  {
    id: "exhibit-demo-qinghuan",
    poemId: "p001",
    focusLine: "人间有味是清欢",
    photo: qinghuan,
    note: "春盘上桌，蓼茸蒿笋是外婆园里现摘的",
    createdAt: "2026-04-05",
    landscape: false,
  },
  {
    id: "exhibit-demo-chunri",
    poemId: "p004",
    focusLine: "春日游，杏花吹满头",
    photo: chunri,
    note: "杏花开的那周，特意走路去上班",
    createdAt: "2026-03-18",
    landscape: false,
  },
];
