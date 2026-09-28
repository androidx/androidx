# RemoteCompose Document Compression

A RemoteCompose document can compress everything that follows its header.
Layouts repeat the same operations, modifiers, strings and floats many times,
so they shrink a lot. That matters wherever documents travel or are stored:
widget updates over IPC, Wear surfaces over Bluetooth, server-driven UI over
the network, and documents cached on disk.

Compression is:
- **Opt-in**: documents are compressed only when their creator asks for it.
- **Transparent**: players decompress while loading. The in-memory document
  is the uncompressed original, byte for byte.
- **Peekable**: the header is never compressed, so tools can read a
  document's version, size and profiles without inflating anything.
- **Dependency free**: it's DEFLATE in a zlib wrapper (RFC 1950), done with
  `java.util.zip.Deflater` and `Inflater`, which every JVM and Android release
  ships.
- **Deterministic**: with a given zlib version, the same document always
  compresses to the same bytes, so deduplication and caching keep working.

## Wire format

```
+------------------------------------------------------+
| HEADER (opcode 0), never compressed                  |
|   version (major | magic, minor, patch)              |
|   property count                                     |
|   properties ...                                     |
|   COMPRESS = 1    (00 1F | 00 04 | 00 00 00 01)      |
+------------------------------------------------------+
| zlib stream (RFC 1950) holding the operations        |
|   78 9C ... DEFLATE data ... Adler-32                |
+------------------------------------------------------+
```

`Header.COMPRESS` is header property **31**. It has the INT type, so its tag is
`0x001F` and its value is 4 bytes long.

| Value | Constant | Meaning |
| :--- | :--- | :--- |
| absent or `0` | `Header.COMPRESSION_NONE` | The operations are stored as is. |
| `1` | `Header.COMPRESSION_DEFLATE` | Everything after the header is one zlib stream. |
| anything else | - | Reserved. Players reject the document. |

Rules:
1. Only a properties header can carry `COMPRESS`. Properties headers start
   at API level 7; the legacy header has no properties. Writers only compress
   API level 8+ documents.
2. Writers append `COMPRESS` after the other properties. Readers accept it
   anywhere in the header.
3. The zlib stream starts right after the header and ends exactly at the end
   of the document. Players reject a document whose stream is truncated or
   corrupted, or is followed by extra bytes.
4. The stream inflates to the operations. Players parse them exactly as they
   would in an uncompressed document with the same header.
5. Players reject documents whose operations inflate to more than
   `Limits.MAX_DECOMPRESSED_SIZE` bytes (32 MB), which guards them against
   decompression bombs.

## Creating compressed documents

Writers compress a document when their `COMPRESS` header tag asks for it. The
document is compressed when it is serialized, by
`RemoteComposeWriter.encodeToByteArray()`, which the Kotlin DSL, JSON parsing
and `RemoteComposeContext.buffer()` all call.

### With a header tag

```java
RemoteComposeWriter writer = new RemoteComposeWriter(
        RcPlatformProfiles.ANDROIDX,
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 300),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 300),
        RemoteComposeWriter.hTag(Header.COMPRESS, Header.COMPRESSION_DEFLATE));
// ... write the document ...
byte[] bytes = writer.encodeToByteArray(); // compressed
```

The DSL accepts the same tag: `createRcBuffer(profile, *tags) { ... }`.

### From JSON

```json
{
  "header": { "apiLevel": 8, "compress": true },
  "root": { "type": "column", "children": [ ... ] }
}
```

`"compress"` accepts `true` or `false`, or a `COMPRESS` value such as `1`. JSON
documents default to API level 7, so compression needs `"apiLevel": 8`.

### Converting existing documents

```java
byte[] compressed = Header.compressDocument(bytes, bytes.length);
byte[] original = Header.decompressDocument(compressed, compressed.length);
```

`decompressDocument` returns the exact bytes that were compressed, and a copy
of documents that aren't compressed. `compressDocument` returns a copy of
documents that are already compressed. It only needs a properties header
(API level 7+): it can't know whether the document's players support
compression.

Compose capture doesn't take header tags, so compress the documents it
returns this way:

```kotlin
val document = captureSingleRemoteDocument(context) { RemoteText("Hello".rs) }
val compressed = Header.compressDocument(document.bytes, document.bytes.size)
```

### Things to know

- On a writer, only `encodeToByteArray()` compresses.
  `RemoteComposeWriter.buffer()` and `bufferSize()` always expose the raw
  document, without the `COMPRESS` property.
  `RemoteComposeContext.bufferSize()` returns the size of what `buffer()`
  returns, so it compresses the document when compressing.
- `RemoteComposeWriter.getCompression()` returns how the writer compresses.
  `reset()` turns compression off, because it restarts the document with a
  legacy header.
- Writers throw `IllegalArgumentException` for unknown compressions, and for
  compression below API level 8.

## Playing compressed documents

Players need no changes. Every loading path goes through
`RemoteComposeBuffer.inflateFromBuffer`: `RemoteDocument` (bytes or stream),
`RemoteComposePlayer.setDocument` and `updateDocument`, `RcPlayerState`,
`CoreDocument.initFromBuffer` and `RemoteCaptureTestRule`. It reads the
header, and if `COMPRESS` is set to anything but `0`, it replaces the buffer
with the decompressed document before parsing the operations. The replacement
happens once, so `CoreDocument.reinflate()` doesn't decompress again.

Loading a compressed document briefly holds both the compressed and the
decompressed bytes.

`inflateFromBuffer` throws a `RuntimeException` wrapping one of these
`IOException`s:

| Problem | Message |
| :--- | :--- |
| `COMPRESS` isn't `0` or `1`, or isn't an INT | `Unsupported document compression <value>` |
| The stream ends early | `Truncated compressed document` |
| The stream is corrupted | `Corrupted compressed document` or `Invalid compressed document` |
| Bytes follow the stream | `Unexpected data after the compressed document` |
| The operations inflate past the limit | `Decompressed document exceeds <limit> bytes` |
| The header properties are malformed | `Malformed header properties` |

`RemoteComposePlayer.setDocument` logs load failures instead of throwing them.

## Peeking at a document

```java
Header header = Header.readDirect(new ByteArrayInputStream(bytes));
Object compression = header.get(Header.COMPRESS); // null or 0: none, 1: DEFLATE
```

`Header.readDirect` reads only the header, so it works the same on
compressed documents. Tools that dump operations decompress first:
- The `RcToString` test utility describes a compressed document by its
  operations.
- The demo `RemoteComposeConverter` does the same, and records
  `"compression": 1`, so converting the JSON back produces the compressed
  document again.

## Compatibility

Players that predate compression can't read compressed documents, but they
fail cleanly:
- They parse the header, because they keep unknown properties.
- They then read the first byte of the zlib stream as an opcode. With the
  default 32 KB window, zlib streams always start with `0x78`, and opcode 120
  is reserved (see `Operations`).
- So they fail with `Unknown operation encountered 120` instead of
  misreading the document.

Compressing doesn't change the document version, so a host must know that its
players support compression before turning it on.

### Encoders outside this library

Any zlib implementation can write compressed documents, for example Python's
`zlib.compress`, Go's `compress/zlib` or `java.util.zip.Deflater`:
1. Write the properties header as usual, with an extra `COMPRESS` property
   (`00 1F 00 04 00 00 00 01`) included in the property count.
2. Append the operations as a single zlib stream, and nothing after it.
3. Use the default 32 KB window. Smaller windows start the stream with a byte
   from `0x08` to `0x68`, which older players may parse as a real opcode.

## When to compress

Sizes of the 174 checked-in documents in
`integration-tests/player-view-demos/src/main/res/raw`, compressed with the
default level:

| Uncompressed size | Documents | Median compressed size |
| :--- | :--- | :--- |
| Under 1 KB | 93 | 61% |
| 1 KB to 10 KB | 69 | 38% |
| 10 KB to 100 KB | 11 | 24% |
| Over 100 KB | 1 | 99% |
| **All** | **174** | **50%** |

Together, the documents go from 994 KB to 577 KB (58%).

- Large, repetitive documents compress best: `color_table.rc` (79 KB)
  shrinks to 17% of its size, and `maze1.rc` (33 KB) to 13%.
- Images embedded as PNG, JPEG or WebP are already compressed, so documents
  made mostly of them barely shrink. The one document over 100 KB is 98.6%
  PNG.
- In the worst case, compression adds about 20 bytes: the `COMPRESS`
  property and the zlib framing. Only one document, of 61 bytes, came out
  larger (66 bytes).
- Creators pay for compression once per document. Players pay for
  decompression once per load: `CoreDocument.reinflate()` doesn't decompress
  again.
- If the transport already compresses, for example HTTP with gzip, document
  compression adds little.

## Where it lives

| Area | Code |
| :--- | :--- |
| Format, compression and decompression | `Header` (`COMPRESS`, `compressDocument`, `decompressDocument`) |
| Loading | `RemoteComposeBuffer.inflateFromBuffer` |
| Limit | `Limits.MAX_DECOMPRESSED_SIZE` |
| Reserved opcode | `Operations` (opcode 120) |
| Creation | `RemoteComposeWriter` (`COMPRESS` tag, `encodeToByteArray`, `getCompression`) |
| JSON | `RemoteComposeJsonParser` (`"compress"` header key) |
| Tests | `DocumentCompressionTest`, `RemoteComposeWriterTest`, `RemoteComposeContextTest`, `DslComparisonTest`, `RemoteComposeJsonParserTest`, `RemoteDocumentCompressionTest`, `RemoteComposePlayerTest`, `RcPlayerStateTest` |
