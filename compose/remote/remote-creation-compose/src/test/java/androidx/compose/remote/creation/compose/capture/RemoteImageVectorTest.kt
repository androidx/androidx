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

package androidx.compose.remote.creation.compose.capture

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteImageVectorTest {

    @Test
    fun toRemoteImageVector_convertsProperties() {
        val imageVector =
            ImageVector.Builder(
                    name = "test_icon",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                )
                .path(fill = SolidColor(Color.Red)) {
                    moveTo(0f, 0f)
                    lineTo(24f, 24f)
                }
                .build()

        val remoteImageVector = imageVector.toRemoteImageVector()

        assertThat(remoteImageVector.name).isEqualTo("test_icon")
        assertThat(remoteImageVector.viewportWidth.constantValueOrNull).isEqualTo(24f)
        assertThat(remoteImageVector.viewportHeight.constantValueOrNull).isEqualTo(24f)
    }
}
