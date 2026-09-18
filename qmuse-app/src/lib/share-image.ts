// 分享导出图：展品 → 竖版卡片（照片 + 诗句 + 出处 + 注记 + 馆名印章）
// 画布 1080×1440（3:4），米白底，照片居中裁切 1080×1000，下区文字
export interface ShareCardInput {
  photoUrl: string;
  focusLine: string;
  meta: string; // 作者 · 题目
  note: string;
}

const BG = "#FAF9F6";
const INK = "#202A30";
const SUB = "#657176";

const SERIF = "'Noto Serif SC', 'Songti SC', 'STSong', 'SimSun', serif";
const SANS = "'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif";

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = "anonymous";
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error("照片加载失败"));
    img.src = src;
  });
}

export async function drawShareCard(
  input: ShareCardInput,
): Promise<Blob> {
  const W = 1080;
  const H = 1440;
  const PHOTO_H = 1000;

  const canvas = document.createElement("canvas");
  canvas.width = W;
  canvas.height = H;
  const ctx = canvas.getContext("2d");
  if (!ctx) throw new Error("画布不可用");

  // 底色
  ctx.fillStyle = BG;
  ctx.fillRect(0, 0, W, H);

  // 照片：cover 居中裁切
  const img = await loadImage(input.photoUrl);
  const scale = Math.max(W / img.width, PHOTO_H / img.height);
  const dw = img.width * scale;
  const dh = img.height * scale;
  ctx.drawImage(img, (W - dw) / 2, (PHOTO_H - dh) / 2, dw, dh);

  // 诗句：自动换行，最多两行
  const verse = input.focusLine;
  const maxLine = 14; // 每行最多字数
  const lines: string[] = [];
  if (verse.length <= maxLine) {
    lines.push(verse);
  } else {
    // 按标点或长度切两行
    const cut = verse.slice(0, maxLine);
    const punct = Math.max(cut.lastIndexOf("，"), cut.lastIndexOf("。"), cut.lastIndexOf("；"));
    const at = punct > maxLine * 0.5 ? punct + 1 : maxLine;
    lines.push(verse.slice(0, at), verse.slice(at));
  }
  ctx.fillStyle = INK;
  ctx.font = `54px ${SERIF}`;
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  const verseTop = PHOTO_H + 105;
  lines.forEach((line, i) => {
    ctx.fillText(line, W / 2, verseTop + i * 88, W - 120);
  });

  // 出处
  ctx.fillStyle = SUB;
  ctx.font = `30px ${SANS}`;
  ctx.fillText(input.meta, W / 2, verseTop + lines.length * 88 + 52, W - 160);

  // 注记（如有）
  if (input.note) {
    ctx.fillStyle = "rgba(101,113,118,0.85)";
    ctx.font = `28px ${SERIF}`;
    ctx.fillText(`— ${input.note}`, W / 2, verseTop + lines.length * 88 + 108, W - 160);
  }

  // 底部：馆名 + 印章
  ctx.textAlign = "left";
  ctx.textBaseline = "middle";
  ctx.fillStyle = SUB;
  ctx.font = `28px ${SANS}`;
  ctx.fillText("浮生诗集 · 用生活表达诗意", 72, H - 92);
  const seal = 84;
  const sx = W - 72 - seal;
  const sy = H - 92 - seal / 2;
  ctx.fillStyle = INK;
  const r = 14;
  ctx.beginPath();
  ctx.roundRect(sx, sy, seal, seal, r);
  ctx.fill();
  ctx.fillStyle = BG;
  ctx.font = `46px ${SERIF}`;
  ctx.textAlign = "center";
  ctx.fillText("浮", sx + seal / 2 + 2, sy + seal / 2 + 4);

  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => (blob ? resolve(blob) : reject(new Error("生成失败"))),
      "image/jpeg",
      0.92,
    );
  });
}
