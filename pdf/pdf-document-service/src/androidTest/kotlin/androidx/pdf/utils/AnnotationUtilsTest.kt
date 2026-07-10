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

package androidx.pdf.utils

import android.graphics.Path
import android.os.Build
import androidx.pdf.annotation.models.PathPdfObject.PathInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM, codeName = "VanillaIceCream")
class AnnotationUtilsTest {

    @Test
    fun getPathFromPathInputs_emptyList_returnsEmptyPath() {
        val pathInputs = emptyList<PathInput>()
        val path = pathInputs.getPathFromPathInputs()
        assert(path.isEmpty)
    }

    @Test
    fun getPathInputsFromPath_multipleContours_identifiesMoveToAndLineTo() {
        val path = Path()
        path.moveTo(0f, 0f)
        path.lineTo(5f, 5f)
        path.moveTo(10f, 10f)
        path.lineTo(15f, 15f)

        val pathInputs = path.getPathInputsFromPath()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            assertThat(pathInputs).isEmpty()
            return
        }

        // Assert MOVE_TO commands: Identifying the start of each contour
        val moveTos = pathInputs.filter { it.command == PathInput.MOVE_TO }
        assertThat(moveTos).hasSize(2)
        assertThat(moveTos[0].x).isEqualTo(0f)
        assertThat(moveTos[0].y).isEqualTo(0f)
        assertThat(moveTos[1].x).isEqualTo(10f)
        assertThat(moveTos[1].y).isEqualTo(10f)

        // Assert LINE_TO commands: Verifying the connections within contours
        val lineTos = pathInputs.filter { it.command == PathInput.LINE_TO }
        // Each segment in this test is a simple straight line, so we expect exactly two LINE_TOs
        assertThat(lineTos).hasSize(2)
        assertThat(lineTos[0].x).isEqualTo(5f)
        assertThat(lineTos[0].y).isEqualTo(5f)
        assertThat(lineTos[1].x).isEqualTo(15f)
        assertThat(lineTos[1].y).isEqualTo(15f)
    }
}
