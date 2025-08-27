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
import androidx.compose.remote.player.view.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// TODO(b/438410969): Move all these tests to test/ folder.
@RunWith(AndroidJUnit4::class)
class PaddingModifierTest {
    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }

    val creationState =
        RemoteComposeCreationState(AndroidxPlatformServices(), density = 1f, Size(1f, 1f))

    @get:Rule val composeTestRule = createComposeRule()

    /** Tests that negative start padding is not allowed. */
    @Test(expected = IllegalArgumentException::class)
    fun negativeStartPaddingDp_throws() {
        composeTestRule.setContent { RemoteModifier.padding(left = (-1f).dp) }
    }

    /** Tests that negative top padding is not allowed. */
    @Test(expected = IllegalArgumentException::class)
    fun negativeTopPaddingDp_throws() {
        composeTestRule.setContent { RemoteModifier.padding(top = (-1f).dp) }
    }

    /** Tests that negative end padding is not allowed. */
    @Test(expected = IllegalArgumentException::class)
    fun negativeEndPaddingDp_throws() {
        composeTestRule.setContent { RemoteModifier.padding(right = (-1f).dp) }
    }

    /** Tests that negative bottom padding is not allowed. */
    @Test(expected = IllegalArgumentException::class)
    fun negativeBottomPaddingDp_throws() {
        composeTestRule.setContent { RemoteModifier.padding(bottom = (-1f).dp) }
    }

    /** Tests that negative all padding is not allowed. */
    @Test(expected = IllegalArgumentException::class)
    fun negativeAllPaddingDp_throws() {
        composeTestRule.setContent { RemoteModifier.padding(all = (-1f).dp) }
    }

    /** Tests that negative horizontal padding is not allowed. */
    @Test(expected = IllegalArgumentException::class)
    fun negativeHorizontalPaddingDp_throws() {
        composeTestRule.setContent { RemoteModifier.padding(horizontal = (-1f).dp) }
    }

    /** Tests that negative vertical padding is not allowed. */
    @Test(expected = IllegalArgumentException::class)
    fun negativeVerticalPaddingDp_throws() {
        composeTestRule.setContent { RemoteModifier.padding(vertical = (-1f).dp) }
    }

    /** Tests that the [padding]-all and [padding] factories return equivalent modifiers. */
    @Test
    fun allDpEqualToAbsoluteWithExplicitSides() {
        composeTestRule.setContent {
            assertTrue(
                haveSameValues(
                    RemoteModifier.padding(10.dp, 10.dp, 10.dp, 10.dp),
                    RemoteModifier.padding(10.dp),
                )
            )
        }
    }

    /** Tests that the symmetrical-[padding] and [padding] factories return equivalent modifiers. */
    @Test
    fun symmetricDpEqualToAbsoluteWithExplicitSides() {
        composeTestRule.setContent {
            assertTrue(
                haveSameValues(
                    RemoteModifier.padding(10.dp, 20.dp, 10.dp, 20.dp),
                    RemoteModifier.padding(10.dp, 20.dp),
                )
            )
        }
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
