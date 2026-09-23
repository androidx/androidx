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
@file:Suppress("RestrictedApiAndroidX")

package androidx.wear.compose.remote.material3

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.toRemoteImageVector
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.player.compose.RcPlayerTestRule
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import kotlin.math.max
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w200dp-h200dp-xhdpi")
class RemoteIconButtonComparisonTest {
    @get:Rule val rcPlayerTestRule = RcPlayerTestRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Ignore("b/556172440 - Enable once corner equality and embedded player icon mask fixes land")
    @Test
    fun filledIconButton_withAddIcon_matchesWearComposeM3() {
        val density = context.resources.displayMetrics.density
        val sizeDp = 52.dp
        val sizePx = 52f * density
        val displayInfo = createCreationDisplayInfo(context, Size(sizePx, sizePx))
        var showWearM3 by mutableStateOf(false)

        rcPlayerTestRule.setRemoteContent(
            remoteCreationDisplayInfo = displayInfo,
            playComposableWrapper = { remoteContent ->
                if (!showWearM3) {
                    Box(modifier = Modifier.size(sizeDp).testTag(REMOTE_M3_TAG)) {
                        remoteContent()
                    }
                } else {
                    Box(
                        modifier =
                            Modifier.size(sizeDp).background(Color.Black).testTag(WEAR_M3_TAG),
                        contentAlignment = Alignment.Center,
                    ) {
                        MaterialTheme {
                            IconButton(
                                onClick = {},
                                enabled = true,
                                colors = IconButtonDefaults.filledIconButtonColors(),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(IconButtonDefaults.DefaultIconSize),
                                )
                            }
                        }
                    }
                }
            },
        ) {
            RemoteMaterialTheme {
                RemoteBox(
                    modifier = RemoteModifier.fillMaxSize().background(Color.Black.rc),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteIconButton(
                        onClick = Action.Empty,
                        enabled = true.rb,
                        colors = RemoteIconButtonDefaults.filledIconButtonColors(),
                    ) {
                        RemoteIcon(
                            imageVector = Icons.Filled.Add.toRemoteImageVector(),
                            contentDescription = null,
                            modifier =
                                RemoteModifier.size(RemoteIconButtonDefaults.DefaultIconSize),
                        )
                    }
                }
            }
        }
        rcPlayerTestRule.waitForIdle()
        val remoteBitmap =
            rcPlayerTestRule.onNodeWithTag(REMOTE_M3_TAG).captureToImage().asAndroidBitmap()

        showWearM3 = true
        rcPlayerTestRule.waitForIdle()
        val wearBitmap =
            rcPlayerTestRule.onNodeWithTag(WEAR_M3_TAG).captureToImage().asAndroidBitmap()

        // Sanity check that the background, circle container, and plus icon were drawn
        assertThat(remoteBitmap.getPixel(0, 0)).isEqualTo(Color.Black.toArgb())
        assertThat(remoteBitmap.getPixel(15, 52)).isNotEqualTo(Color.Black.toArgb())
        assertThat(remoteBitmap.getPixel(52, 52)).isNotEqualTo(remoteBitmap.getPixel(15, 52))

        val diffSummary = compareBitmaps(wearBitmap, remoteBitmap)
        assertWithMessage(diffSummary.toString()).that(diffSummary.changedPixels).isEqualTo(0)
    }

    private data class DiffSummary(
        val changedPixels: Int,
        val maxChannelDelta: Int,
        val sampleDiffs: List<String>,
    )

    private fun compareBitmaps(expected: Bitmap, actual: Bitmap): DiffSummary {
        var changedPixels = 0
        var maxDelta = 0
        val samples = mutableListOf<String>()
        for (y in 0 until expected.height) {
            for (x in 0 until expected.width) {
                val e = expected.getPixel(x, y)
                val a = actual.getPixel(x, y)
                if (e != a) {
                    changedPixels++
                    val er = AndroidColor.red(e)
                    val eg = AndroidColor.green(e)
                    val eb = AndroidColor.blue(e)
                    val ea = AndroidColor.alpha(e)
                    val ar = AndroidColor.red(a)
                    val ag = AndroidColor.green(a)
                    val ab = AndroidColor.blue(a)
                    val aa = AndroidColor.alpha(a)
                    val delta =
                        max(
                            max(abs(er - ar), abs(eg - ag)),
                            max(abs(eb - ab), abs(ea - aa)),
                        )
                    maxDelta = max(maxDelta, delta)
                    if (samples.size < 15) {
                        samples.add(
                            "($x,$y): wear=[$er,$eg,$eb,$ea] vs remote=[$ar,$ag,$ab,$aa] (delta=$delta)"
                        )
                    }
                }
            }
        }
        return DiffSummary(changedPixels, maxDelta, samples)
    }

    private companion object {
        const val WEAR_M3_TAG = "WEAR_M3_TAG"
        const val REMOTE_M3_TAG = "REMOTE_M3_TAG"
    }
}
