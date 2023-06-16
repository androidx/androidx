import type { ChartDataset } from "chart.js";
import type { Data } from "./transforms.js";

export function chartData(container: Array<Data>) {
  let xmax = 0;
  let datasets = [];
  for (let i = 0; i < container.length; i += 1) {
    // update xmax
    xmax = Math.max(xmax, container[i].data.length);
    datasets.push(chartDataset(container[i]));
  }
  return {
    labels: xrange(xmax),
    datasets: datasets
  };
}

function xrange(xmax: number): Array<number> {
  let labels = [];
  for (let i = 1; i <= xmax; i += 1) {
    labels.push(i);
  }
  return labels;
}

function chartDataset(data: Data): ChartDataset<'line'> {
  return {
    label: data.name,
    data: data.data,
    tension: 0.3
  };
}
