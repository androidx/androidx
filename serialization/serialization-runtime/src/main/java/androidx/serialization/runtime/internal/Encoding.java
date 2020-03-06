/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.serialization.runtime.internal;

/**
 * Encoding constants for integer and long scalar encodings.
 *
 * @see androidx.serialization.ProtoEncoding
 */
public final class Encoding {
    private Encoding() {
    }

    /**
     * Signed variable length encoding.
     */
    @IntEncoding
    public static final int SIGNED_VARINT = 0;

    /**
     * Unsigned variable length encoding.
     */
    @IntEncoding
    public static final int UNSIGNED_VARINT = 1;

    /**
     * Signed variable length encoding with compact negative value representations.
     */
    @IntEncoding
    public static final int ZIG_ZAG_VARINT = 2;

    /**
     * Signed fixed width encoding.
     */
    @IntEncoding
    public static final int SIGNED_FIXED = 3;

    /**
     * Unsigned fixed width encoding.
     */
    @IntEncoding
    public static final int UNSIGNED_FIXED = 4;
}
