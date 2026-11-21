/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.remote.core.input;

import androidx.annotation.RestrictTo;

/**
 * Packs and unpacks metadata bitfields for pointer input events.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MetadataUtil {
    // Number of bits allocated
    private static final int PASS_BITS = 2;
    private static final int SUBTYPE_BITS = 5;
    private static final int CONSUMED_BITS = 1;

    // Bit shift offsets
    private static final int PASS_SHIFT = 0;
    private static final int SUBTYPE_SHIFT = PASS_SHIFT + PASS_BITS; // 2
    private static final int CONSUMED_SHIFT = SUBTYPE_SHIFT + SUBTYPE_BITS; // 7

    // Bit masks for extracting values
    private static final int PASS_MASK = (1 << PASS_BITS) - 1;       // 0x03 (3)
    private static final int SUBTYPE_MASK = (1 << SUBTYPE_BITS) - 1; // 0x1F (31)
    private static final int CONSUMED_MASK = (1 << CONSUMED_BITS) - 1; // 0x01 (1)

    private MetadataUtil() {}

    /**
     * Packs pass, subtype, and consumed status into a single integer.
     *
     * @param pass pointer event pass (bits 0-1)
     * @param subtype pointer event subtype (bits 2-6)
     * @param consumed consumed status flag (bit 7)
     * @return packed integer metadata
     */
    public static int pack(int pass, int subtype, int consumed) {
        // Bounds checking
        if (pass < 0 || pass > PASS_MASK) {
            throw new IllegalArgumentException("Pass out of bounds(0-" + PASS_MASK + ")");
        }
        if (subtype < 0 || subtype > SUBTYPE_MASK) {
            throw new IllegalArgumentException("Subtype out of bounds(0-" + SUBTYPE_MASK + ")");
        }
        if (consumed < 0 || consumed > CONSUMED_MASK) {
            throw new IllegalArgumentException("Consumed out of bounds(0-" + CONSUMED_MASK + ")");
        }

        return (pass & PASS_MASK)
                | ((subtype & SUBTYPE_MASK) << SUBTYPE_SHIFT)
                | ((consumed & CONSUMED_MASK) << CONSUMED_SHIFT);
    }

    /**
     * Extracts the pass (bits 0-1) from packed metadata.
     *
     * @param packed packed integer metadata
     * @return extracted pass value
     */
    public static int unpackPass(int packed) {
        return (packed >>> PASS_SHIFT) & PASS_MASK;
    }

    /**
     * Extracts the subtype (bits 2-6) from packed metadata.
     *
     * @param packed packed integer metadata
     * @return extracted subtype value
     */
    public static int unpackSubtype(int packed) {
        return (packed >>> SUBTYPE_SHIFT) & SUBTYPE_MASK;
    }

    /**
     * Extracts the consumed status (bit 7) from packed metadata.
     *
     * @param packed packed integer metadata
     * @return extracted consumed flag
     */
    public static int unpackConsumed(int packed) {
        return (packed >>> CONSUMED_SHIFT) & CONSUMED_MASK;
    }

    /**
     * Updates the pass (bits 0-1) in packed metadata.
     *
     * @param packed packed integer metadata
     * @param pass new pass value
     * @return updated packed metadata
     */
    public static int updatePass(int packed, int pass) {
        if (pass < 0 || pass > PASS_MASK) {
            throw new IllegalArgumentException("Pass out of bounds(0-" + PASS_MASK + ")");
        }
        return (packed & ~(PASS_MASK << PASS_SHIFT)) | ((pass & PASS_MASK) << PASS_SHIFT);
    }

    /**
     * Updates the subtype (bits 2-6) in packed metadata.
     *
     * @param packed packed integer metadata
     * @param subtype new subtype value
     * @return updated packed metadata
     */
    public static int updateSubtype(int packed, int subtype) {
        if (subtype < 0 || subtype > SUBTYPE_MASK) {
            throw new IllegalArgumentException("Subtype out of bounds(0-" + SUBTYPE_MASK + ")");
        }
        return (packed & ~(SUBTYPE_MASK << SUBTYPE_SHIFT)) | ((subtype & SUBTYPE_MASK)
                << SUBTYPE_SHIFT);
    }

    /**
     * Updates the consumed status (bit 7) in packed metadata.
     *
     * @param packed packed integer metadata
     * @param consumed new consumed flag
     * @return updated packed metadata
     */
    public static int updateConsumed(int packed, int consumed) {
        if (consumed < 0 || consumed > CONSUMED_MASK) {
            throw new IllegalArgumentException("Consumed out of bounds(0-" + CONSUMED_MASK + ")");
        }
        return (packed & ~(CONSUMED_MASK << CONSUMED_SHIFT)) | ((consumed & CONSUMED_MASK)
                << CONSUMED_SHIFT);
    }
}
