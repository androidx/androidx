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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TypesTest {

    @Test
    fun planeOrientation_toString() {
        assertThat(PlaneOrientation.ANY.toString()).isEqualTo("ANY")
        assertThat(PlaneOrientation.HORIZONTAL.toString()).isEqualTo("HORIZONTAL")
        assertThat(PlaneOrientation.VERTICAL.toString()).isEqualTo("VERTICAL")
    }

    @Test
    fun planeSemanticType_toString() {
        assertThat(PlaneSemanticType.ANY.toString()).isEqualTo("ANY")
        assertThat(PlaneSemanticType.WALL.toString()).isEqualTo("WALL")
        assertThat(PlaneSemanticType.FLOOR.toString()).isEqualTo("FLOOR")
        assertThat(PlaneSemanticType.CEILING.toString()).isEqualTo("CEILING")
        assertThat(PlaneSemanticType.TABLE.toString()).isEqualTo("TABLE")
    }
}
