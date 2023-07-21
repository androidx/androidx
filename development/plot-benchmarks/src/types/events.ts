import type { FileMetadata } from "./files.js";

export interface FileMetadataEvent {
  entries: Array<FileMetadata>;
}
