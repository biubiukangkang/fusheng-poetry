// 前端图片压缩：长边 1600、JPEG q85，控制上传体积
export async function compressImage(file: File): Promise<Blob> {
  const bitmap = await createImageBitmap(file);
  const scale = Math.min(1, 1600 / Math.max(bitmap.width, bitmap.height));
  const w = Math.round(bitmap.width * scale);
  const h = Math.round(bitmap.height * scale);
  const canvas = document.createElement("canvas");
  canvas.width = w;
  canvas.height = h;
  const ctx = canvas.getContext("2d");
  if (!ctx) throw new Error("canvas 不可用");
  ctx.drawImage(bitmap, 0, 0, w, h);
  bitmap.close();
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => (blob ? resolve(blob) : reject(new Error("压缩失败"))),
      "image/jpeg",
      0.85,
    );
  });
}

export async function isLandscape(file: File | Blob): Promise<boolean> {
  const bitmap = await createImageBitmap(file);
  const landscape = bitmap.width > bitmap.height;
  bitmap.close();
  return landscape;
}
