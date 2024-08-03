/*
 * Copyright 2024 The Android Open Source Project
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
 *
 */

package androidx.camera.viewfinder.compose

import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CoordinateTransformerTest {

    @Test
    fun transform_withoutChangingMatrix_shouldReturnSameOffset() {
        val transformer = MutableCoordinateTransformer()

        val offset = Offset(10f, 10f)

        with(transformer) {
            val cameraSpaceOffset = offset.transform()
            assertThat(cameraSpaceOffset).isEqualTo(offset)
        }
    }

    @Test
    fun transform_withMatrix() {
        val transformer = MutableCoordinateTransformer()

        val offset = Offset(10f, 10f)
        transformer.transformMatrix.scale(2.0f, 2.0f)

        with(transformer) {
            val cameraSpaceOffset = offset.transform()
            assertThat(cameraSpaceOffset).isEqualTo(Offset(20f, 20f))
        }
    }
}
