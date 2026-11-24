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

package androidx.compose.remote.document

import androidx.compose.remote.core.operations.layout.managers.Custom
import kotlin.test.Test
import kotlin.test.assertEquals

/** Parity tests for [RemoteCustomProperty] type constants against `remote-core`. */
class RemoteCustomPropertyTest {

    @Test
    fun testRemoteCustomPropertyConstantsParity() {
        assertEquals(Custom.CustomProperty.INT_PROP, RemoteCustomProperty.INT_PROP)
        assertEquals(Custom.CustomProperty.FLOAT_PROP, RemoteCustomProperty.FLOAT_PROP)
        assertEquals(Custom.CustomProperty.STRING_PROP, RemoteCustomProperty.STRING_PROP)
        assertEquals(Custom.CustomProperty.FLOAT_RETURN, RemoteCustomProperty.FLOAT_RETURN)
        assertEquals(Custom.CustomProperty.TEXT_RETURN, RemoteCustomProperty.TEXT_RETURN)
        assertEquals(Custom.CustomProperty.INT_RETURN, RemoteCustomProperty.INT_RETURN)
        assertEquals(Custom.CustomProperty.COLOR_RETURN, RemoteCustomProperty.COLOR_RETURN)
        assertEquals(Custom.CustomProperty.COLOR_ID_PROP, RemoteCustomProperty.COLOR_ID_PROP)
        assertEquals(Custom.CustomProperty.COLOR_PROP, RemoteCustomProperty.COLOR_PROP)
        assertEquals(Custom.CustomProperty.INT_ID_PROP, RemoteCustomProperty.INT_ID_PROP)
    }
}
