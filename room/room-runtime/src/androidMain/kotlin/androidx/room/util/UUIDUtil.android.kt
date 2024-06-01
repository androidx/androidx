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
@file:JvmName("UUIDUtil")
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)

package androidx.room.util

import androidx.annotation.RestrictTo
import java.nio.ByteBuffer
import java.util.UUID

/** UUID / byte[] two-way conversion utility for Room */

/**
 * Converts a 16-bytes array BLOB into a UUID pojo
 *
 * @param bytes byte array stored in database as BLOB
 * @return a UUID object created based on the provided byte array
 */
fun convertByteToUUID(bytes: ByteArray): UUID {
    val buffer = ByteBuffer.wrap(bytes)
    val firstLong = buffer.long
    val secondLong = buffer.long
    return UUID(firstLong, secondLong)
}

/**
 * Converts a UUID pojo into a 16-bytes array to store into database as BLOB
 *
 * @param uuid the UUID pojo
 * @return a byte array to store into database
 */
fun convertUUIDToByte(uuid: UUID): ByteArray {
    val bytes = ByteArray(16)
    val buffer = ByteBuffer.wrap(bytes)
    buffer.putLong(uuid.mostSignificantBits)
    buffer.putLong(uuid.leastSignificantBits)
    return buffer.array()
}
