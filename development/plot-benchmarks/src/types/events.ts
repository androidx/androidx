import type { FileMetadata } from "./files.js";

export interface FileMetadataEvent {
  entries: FileMetadata[];
}

export interface DatasetSelection {
  name: string;
  enabled: boolean;
}
export interface DatasetSelectionEvent {
  datasetSelections: DatasetSelection[];
}

export interface MetricSelectionEvent {
  metricSelections: MetricSelection[];
}

export interface MetricSelection {
  name: string;
  enabled: boolean;
}

export type StatType = 'p';
export interface StatInfo {
  name: string;
  type: StatType;
  enabled: boolean;
}

export interface StatEvent {
  info: StatInfo[];
}

export interface ControlsEvent {
  controls: Controls;
}

export interface Controls {
  buckets: number;
}
