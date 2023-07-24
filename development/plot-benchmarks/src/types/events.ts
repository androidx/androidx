import type { FileMetadata } from "./files.js";

export interface FileMetadataEvent {
  entries: Array<FileMetadata>;
}

export interface Selection {
  name: string;
  enabled: boolean;
}
export interface SelectionEvent {
  selections: Array<Selection>;
}
