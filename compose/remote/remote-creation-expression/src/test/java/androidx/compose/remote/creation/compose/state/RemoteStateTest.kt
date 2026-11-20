/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteStateTest {
    @Test
    fun domainToString() {
        assertEquals("USER", RemoteValue.Domain.User.toString())
        assertEquals("SYSTEM", RemoteValue.Domain.System.toString())
        assertEquals("", RemoteValue.Domain.None.toString())
    }

    @Test
    fun domainEquals() {
        assertEquals(RemoteValue.Domain.User, RemoteValue.Domain("USER"))
        assertNotEquals(RemoteValue.Domain.User, RemoteValue.Domain.System)
        assertEquals(RemoteValue.Domain.None, RemoteValue.Domain(null))
    }

    @Test
    fun domainHashcode() {
        assertEquals(RemoteValue.Domain.User.hashCode(), RemoteValue.Domain("USER").hashCode())
    }
}
