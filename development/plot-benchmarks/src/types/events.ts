import type { FileMetadata } from "./files.js";

export interface FileMetadataEvent {
  entries: FileMetadata[];
}

export interface Selection {
  name: string;
  enabled: boolean;
}
export interface SelectionEvent {
  selections: Selection[];
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
