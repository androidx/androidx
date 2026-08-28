/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup.host

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BackupTransportModeTest {

    @Test
    fun testValueEqualityAndHashCode() {
        val d2d1 = BackupTransportMode.DEVICE_TO_DEVICE
        val d2d2 = BackupTransportMode.DEVICE_TO_DEVICE
        val cloudEnc = BackupTransportMode.CLOUD_ENCRYPTED
        val cloudUnenc = BackupTransportMode.CLOUD_UNENCRYPTED
        val local = BackupTransportMode.LOCAL

        // Reflexivity
        assertEquals(d2d1, d2d1)
        assertEquals(cloudEnc, cloudEnc)
        assertEquals(local, local)

        // Symmetry & Identity
        assertEquals(d2d1, d2d2)
        assertEquals(d2d2, d2d1)
        assertEquals(d2d1.hashCode(), d2d2.hashCode())

        // Pairwise Distinctness & Inequality
        assertNotEquals(d2d1, cloudEnc)
        assertNotEquals(d2d1, cloudUnenc)
        assertNotEquals(d2d1, local)
        assertNotEquals(cloudEnc, cloudUnenc)
        assertNotEquals(cloudEnc, local)
        assertNotEquals(cloudUnenc, local)

        // Pairwise Hash Code Distinctness
        assertNotEquals(d2d1.hashCode(), cloudEnc.hashCode())
        assertNotEquals(d2d1.hashCode(), cloudUnenc.hashCode())
        assertNotEquals(d2d1.hashCode(), local.hashCode())
        assertNotEquals(cloudEnc.hashCode(), cloudUnenc.hashCode())

        // Null safety
        assertNotEquals(null, d2d1)

        // Type safety (unrelated types must never equal a BackupTransportMode)
        assertNotEquals("DEVICE_TO_DEVICE", d2d1)
        assertNotEquals(123, d2d1)
    }

    @Test
    fun testToStringReturnsName() {
        assertEquals("DEVICE_TO_DEVICE", BackupTransportMode.DEVICE_TO_DEVICE.toString())
        assertEquals("CLOUD_ENCRYPTED", BackupTransportMode.CLOUD_ENCRYPTED.toString())
        assertEquals("CLOUD_UNENCRYPTED", BackupTransportMode.CLOUD_UNENCRYPTED.toString())
        assertEquals("LOCAL", BackupTransportMode.LOCAL.toString())
    }
}
