/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.util;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * UUID / byte[] two-way conversion utility for Room
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public final class UUIDUtil {

    // private constructor to prevent instantiation
    private UUIDUtil() {}

    /**
     * Converts a 16-bytes array BLOB into a UUID pojo
     *
     * @param bytes byte array stored in database as BLOB
     * @return a UUID object created based on the provided byte array
     */
    @NonNull
    public static UUID convertByteToUUID(@NonNull byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long firstLong = buffer.getLong();
        long secondLong = buffer.getLong();
        return new UUID(firstLong, secondLong);
    }

    /**
     * Converts a UUID pojo into a 16-bytes array to store into database as BLOB
     *
     * @param uuid the UUID pojo
     * @return a byte array to store into database
     */
    @NonNull
    public static byte[] convertUUIDToByte(@NonNull UUID uuid) {
        byte[] bytes = new byte[16];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }
}
