import type { ChartDataset, ChartType, Point } from "chart.js";

/**
 * The chart data container.
 *
 * Has relevant default X-Axis labels & corresponding datasets.
 */
export interface Data {
  // X-axis labels.
  labels: number[];
  // The corresponding datasets.
  datasets: ChartDataset[];
}

/**
 * A single data-series being rendered in the chart.
 *
 * Used by a Mapper for data transformations.
 */
export interface Series {
  descriptiveLabel: string;
  type: ChartType;
  data: Point[];
  // Additional series options
  // For e.g. https://www.chartjs.org/docs/latest/charts/line.html
  options: object;
}
