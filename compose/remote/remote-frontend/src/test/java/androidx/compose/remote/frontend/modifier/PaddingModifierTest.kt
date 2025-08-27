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

package androidx.compose.remote.frontend.modifier

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.platform.AndroidxPlatformServices
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState
import androidx.compose.remote.frontend.state.rf
import androidx.compose.remote.player.view.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@SdkSuppress(minSdkVersion = 26)
@RunWith(RobolectricTestRunner::class)
class PaddingModifierTest {
    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }

    val creationState =
        RemoteComposeCreationState(AndroidxPlatformServices(), density = 1f, Size(1f, 1f))

    /** Tests that negative start padding is not allowed. */
    @Test(expected = IllegalArgumentException::class)
    fun negativeLeftPadding_throws() {
        RemoteModifier.padding(left = (-1f).rf)
    }

    /** Tests that negative top padding is not allowed. */
    @Test(expected = IllegalArgumentException::class)
    fun negativeTopPadding_throws() {
        RemoteModifier.padding(top = (-1f).rf)
    }

    /** Tests that negative end padding is not allowed. */
    @Test(expected = IllegalArgumentException::class)
    fun negativeRightPadding_throws() {
        RemoteModifier.padding(right = (-1f).rf)
    }

    /** Tests that negative bottom padding is not allowed. */
    @Test(expected = IllegalArgumentException::class)
    fun negativeBottomPadding_throws() {
        RemoteModifier.padding(bottom = (-1f).rf)
    }

    /** Tests that negative all padding is not allowed. */
    @Test(expected = IllegalArgumentException::class)
    fun negativeAllPadding_throws() {
        RemoteModifier.padding(all = (-1f).rf)
    }

    /** Tests that negative horizontal padding is not allowed. */
    @Test(expected = IllegalArgumentException::class)
    fun negativeHorizontalPadding_throws() {
        RemoteModifier.padding(horizontal = (-1f).rf)
    }

    /** Tests that negative vertical padding is not allowed. */
    @Test(expected = IllegalArgumentException::class)
    fun negativeVerticalPadding_throws() {
        RemoteModifier.padding(vertical = (-1f).rf)
    }

    /** Tests that the [padding]-all and [padding] factories return equivalent modifiers. */
    @Test
    fun allEqualToAbsoluteWithExplicitSides() {
        assertTrue(
            haveSameValues(
                RemoteModifier.padding(10f.rf, 10f.rf, 10f.rf, 10f.rf),
                RemoteModifier.padding(10f.rf),
            )
        )
    }

    /** Tests that the symmetrical-[padding] and [padding] factories return equivalent modifiers. */
    @Test
    fun symmetricEqualToAbsoluteWithExplicitSides() {
        assertTrue(
            haveSameValues(
                RemoteModifier.padding(10f.rf, 20f.rf, 10f.rf, 20f.rf),
                RemoteModifier.padding(10f.rf, 20f.rf),
            )
        )
    }

    private fun haveSameValues(modifier1: RemoteModifier, modifier2: RemoteModifier): Boolean {
        require(modifier1 is PaddingModifier && modifier2 is PaddingModifier) {
            "This function only compares PaddingModifier"
        }

        val modifier1LeftId = modifier1.left.getIdForCreationState(creationState)
        val modifier1TopId = modifier1.top.getIdForCreationState(creationState)
        val modifier1RightId = modifier1.right.getIdForCreationState(creationState)
        val modifier1BottomId = modifier1.bottom.getIdForCreationState(creationState)

        val modifier2LeftId = modifier2.left.getIdForCreationState(creationState)
        val modifier2TopId = modifier2.top.getIdForCreationState(creationState)
        val modifier2RightId = modifier2.right.getIdForCreationState(creationState)
        val modifier2BottomId = modifier2.bottom.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        fun areEquals(floatId1: Int, floatId2: Int) =
            context.getFloat(floatId1) == context.getFloat(floatId2)

        return areEquals(modifier1LeftId, modifier2LeftId) &&
            areEquals(modifier1TopId, modifier2TopId) &&
            areEquals(modifier1RightId, modifier2RightId) &&
            areEquals(modifier1BottomId, modifier2BottomId)
    }

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }
}
