import type { Chart } from "chart.js";

export async function saveToClipboard(chart: Chart): Promise<void> {
  const image = chart.toBase64Image('image/png', 1);
  return fetch(image)
    .then(response => response.blob())
    .then((blob: Blob) => {
      const type = blob.type;
      return new ClipboardItem({
        [type]: blob
      });
    }).then(item => navigator.clipboard.write([item]));
}
