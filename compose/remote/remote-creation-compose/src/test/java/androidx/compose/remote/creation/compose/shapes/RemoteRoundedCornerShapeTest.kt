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

package androidx.compose.remote.creation.compose.shapes

import androidx.compose.remote.creation.compose.state.rdp
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@SdkSuppress(minSdkVersion = 29)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteRoundedCornerShapeTest {
    @Test
    fun copy_preservesValuesIfNotSpecified() {
        val shape = RemoteRoundedCornerShape(1.rdp, 2.rdp, 3.rdp, 4.rdp)
        val copied = shape.copy()

        assertTrue(haveSameInstances(shape, copied))
    }

    @Test
    fun copy_updatesSpecifiedValues() {
        val topEnd = 2.rdp
        val bottomEnd = 3.rdp
        val bottomStart = 4.rdp
        val shape = RemoteRoundedCornerShape(1.rdp, topEnd, bottomEnd, bottomStart)
        val topStartOverride = 10.rdp
        val copied = shape.copy(topStart = RemoteCornerSize(topStartOverride))

        assertEquals(topStartOverride, (copied.topStart as? RemoteDpCornerSize)?.size)

        assertEquals(topEnd, (copied.topEnd as? RemoteDpCornerSize)?.size)
        assertEquals(bottomEnd, (copied.bottomEnd as? RemoteDpCornerSize)?.size)
        assertEquals(bottomStart, (copied.bottomStart as? RemoteDpCornerSize)?.size)
    }

    private fun haveSameInstances(
        shape1: RemoteCornerBasedShape,
        shape2: RemoteCornerBasedShape,
    ): Boolean {

        return shape1.topStart === shape2.topStart &&
            shape1.topEnd === shape2.topEnd &&
            shape1.bottomEnd === shape2.bottomEnd &&
            shape1.bottomStart === shape2.bottomStart
    }
}
