# RemoteCompose Lossless JSON Spec Extraction Notes

This document explains how the RemoteCompose binary (.rc) to JSON conversion was derived and implemented to guarantee bit-for-bit round-trip fidelity.

## 1. Opcode Discovery
Authorized opcode mappings were discovered in `androidx.compose.remote.core.Operations.java`.
RemoteCompose uses a versioned and profile-aware mapping system:
- **API Level 6**: Uses `createMapV6()`.
- **API Level 7+**: Uses `createMapV7(profiles)` which combines baseline v7 operations with profile-specific operations (AndroidX, Widgets, Experimental, Deprecated).

The converter dynamically determines the correct map by peeking at the document `Header` using `Header.peekApiLevel(buffer)`.

## 2. Field Order and Types
Unlike typical tagged formats, RemoteCompose wire format is a sequence of raw bytes where the meaning of each byte depends on its position within an operation's record.
The exact field sequence for each operation was derived from the `read(WireBuffer, List<Operation>)` and `apply(WireBuffer, ...)` methods in each operation class (found in `androidx.compose.remote.core.operations.*`).

## 3. Implementation Strategy: Payload Capture
To ensure **full fidelity** without manually mapping hundreds of evolving opcodes, the converter uses a **Payload Capture** strategy:
1. **Parse**:
   - Dispatch to the official `CompanionOperation.read()` for the identified opcode.
   - The `read()` method consumes exactly the bytes defined by the spec for that operation.
   - The converter calculates the number of bytes consumed by comparing the `WireBuffer` index before and after the `read()` call.
   - This exact byte slice (excluding the opcode) is stored in JSON as `payloadBase64`.
2. **Reconstruct**:
   - Write the `opcode` byte.
   - Write the exact bytes from `payloadBase64` back into the `WireBuffer`.

This guarantees that even if an operation's internal object representation changes or loses precision, the wire format remains bit-identical.

## 4. Float and NaN/ID Preservation
RemoteCompose frequently encodes dynamic variable IDs into IEEE 754 `NaN` bit patterns.
- Standard JSON float serialization is lossy and can normalize NaNs.
- The converter avoids this by capturing the **raw byte stream**.
- When human-readable fields are added to the JSON (for debugging), they are for inspection only; the `payloadBase64` remains the authoritative source for reconstruction.

## 5. Containers and Nesting
Nested operations (e.g., within `LAYOUT_ROOT` or `COMPONENT_START`) are represented linearly in the `ops` array, mirroring the flat wire format. The `CONTAINER_END` opcode (214) serves as the logical marker for block termination. This preservation of sequence ensures that indices and relative offsets in the binary document remain valid.

## 6. Forward Compatibility
New or unknown opcodes encountered in a document are handled gracefully if they are registered in the `Operations` map for that version. If a completely unknown opcode is encountered, the parser fails loudly to prevent lossy conversion, as record boundaries cannot be determined without the reader spec.
