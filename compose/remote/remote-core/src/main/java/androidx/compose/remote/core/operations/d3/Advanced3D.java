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
package androidx.compose.remote.core.operations.d3;

import org.jspecify.annotations.NonNull;

/**
 * The advanced bit: bit 30 of a field ({@code DrawMesh3D} mode, {@code DefineMesh3D} id, {@code
 * SetTexture3D} bitmap id) marks an advanced form of the operation, defined by a later 3D format
 * version. This version defines none, so a reader that meets the bit refuses the document: it
 * can't know the operation's layout, and must not render a guess.
 */
final class Advanced3D {
    private Advanced3D() {}

    /** The bit of a field that marks the advanced form of an operation. */
    static final int ADVANCED_BIT = 1 << 30;

    /**
     * Refuses a field with {@link #ADVANCED_BIT} set.
     *
     * @param field the field as read from the document
     * @param op the operation name, for the error message
     * @throws RuntimeException if the bit is set
     */
    static void check(int field, @NonNull String op) {
        if ((field & ADVANCED_BIT) != 0) {
            throw new RuntimeException(
                    op + ": advanced form (bit 30) is not supported by this player");
        }
    }
}
