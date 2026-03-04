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

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.matrix.MatrixExpression
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteMatrix3x3Test {
    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }
    val creationState = RemoteComposeCreationState(AndroidxRcPlatformServices(), Size(1f, 1f))

    @Test
    fun remoteMatrix_cacheKey() {
        val m1 = RemoteMatrix3x3.createIdentity()
        val m2 = RemoteMatrix3x3.createIdentity()
        assertThat(m1.cacheKey).isNotNull()
        assertThat(m1.cacheKey).isEqualTo(m2.cacheKey)

        val rotate1 = RemoteMatrix3x3.createRotate(45f.rf)
        val rotate2 = RemoteMatrix3x3.createRotate(45f.rf)
        assertThat(rotate1.cacheKey).isNotNull()
        assertThat(rotate1.cacheKey).isEqualTo(rotate2.cacheKey)
        assertThat(rotate1.cacheKey).isNotEqualTo(m1.cacheKey)

        val tx1 = RemoteMatrix3x3.createTranslateX(10f.rf)
        assertThat(tx1.cacheKey).isNotNull()
        assertThat(tx1.cacheKey).isNotEqualTo(rotate1.cacheKey)

        val combined = m1 * rotate1
        assertThat(combined.cacheKey).isNotNull()
        assertThat(combined.cacheKey).isNotEqualTo(rotate1.cacheKey)
    }

    @Test
    fun matrix_concatenation() {
        val m =
            RemoteMatrix3x3.createTranslateXY(RemoteFloat(10f), RemoteFloat(20f)) *
                RemoteMatrix3x3.createScaleX(RemoteFloat(1.5f)) *
                RemoteMatrix3x3.createScaleY(RemoteFloat(2f))
        val mId = m.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()
        val evaluatedMatrix = (context.getObject(mId) as MatrixExpression).get()
        assertThat(evaluatedMatrix)
            .usingExactEquality()
            .containsExactly(
                floatArrayOf(
                    1.5f,
                    0.0f,
                    0.0f,
                    10.0f,
                    0.0f,
                    2.0f,
                    0.0f,
                    20.0f,
                    0.0f,
                    0.0f,
                    1.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    1.0f,
                )
            )
    }

    @Test
    fun matrix_concatenation2() {
        val m =
            RemoteMatrix3x3.createTranslateXY(RemoteFloat(10f), RemoteFloat(20f)) *
                RemoteMatrix3x3.createTranslateXY(RemoteFloat(10f), RemoteFloat(20f)) *
                RemoteMatrix3x3.createTranslateXY(RemoteFloat(10f), RemoteFloat(20f))
        val mId = m.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()
        val evaluatedMatrix = (context.getObject(mId) as MatrixExpression).get()
        assertThat(evaluatedMatrix)
            .usingExactEquality()
            .containsExactly(
                floatArrayOf(
                    1.0f,
                    0.0f,
                    0.0f,
                    30.0f,
                    0.0f,
                    1.0f,
                    0.0f,
                    60.0f,
                    0.0f,
                    0.0f,
                    1.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    1.0f,
                )
            )
    }

    @Test
    fun matrix_concatenation_with_identity() {
        val m =
            RemoteMatrix3x3.createTranslateXY(RemoteFloat(10f), RemoteFloat(20f)) *
                RemoteMatrix3x3.createIdentity()
        val mId = m.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()
        val evaluatedMatrix = (context.getObject(mId) as MatrixExpression).get()
        assertThat(evaluatedMatrix)
            .usingExactEquality()
            .containsExactly(
                floatArrayOf(
                    1.0f,
                    0.0f,
                    0.0f,
                    10.0f,
                    0.0f,
                    1.0f,
                    0.0f,
                    20.0f,
                    0.0f,
                    0.0f,
                    1.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    1.0f,
                )
            )
    }

    @Test
    fun matrix_concatenation_with_identity2() {
        val m =
            RemoteMatrix3x3.createIdentity() *
                RemoteMatrix3x3.createTranslateXY(RemoteFloat(10f), RemoteFloat(20f))
        val mId = m.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()
        val evaluatedMatrix = (context.getObject(mId) as MatrixExpression).get()
        assertThat(evaluatedMatrix)
            .usingExactEquality()
            .containsExactly(
                floatArrayOf(
                    1.0f,
                    0.0f,
                    0.0f,
                    10.0f,
                    0.0f,
                    1.0f,
                    0.0f,
                    20.0f,
                    0.0f,
                    0.0f,
                    1.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    1.0f,
                )
            )
    }

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }
}
