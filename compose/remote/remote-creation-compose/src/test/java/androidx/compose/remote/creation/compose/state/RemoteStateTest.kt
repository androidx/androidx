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
import org.robolectric.annotation.Config

@RunWith(JUnit4::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteStateTest {
    @Test
    fun domainToString() {
        assertEquals("USER", RemoteState.Domain.User.toString())
        assertEquals("SYSTEM", RemoteState.Domain.System.toString())
    }

    @Test
    fun domainEquals() {
        assertEquals(RemoteState.Domain.User, RemoteState.Domain("USER"))
        assertNotEquals(RemoteState.Domain.User, RemoteState.Domain.System)
    }

    @Test
    fun domainHashcode() {
        assertEquals(RemoteState.Domain.User.hashCode(), RemoteState.Domain("USER").hashCode())
    }
}
