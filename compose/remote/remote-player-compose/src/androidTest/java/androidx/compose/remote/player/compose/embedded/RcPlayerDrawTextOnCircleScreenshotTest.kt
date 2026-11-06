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

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemotePaintingStyle
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.RemoteEmbeddedScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Embedded player ([RcPlayer]) screenshot of `DRAW_TEXT_ON_CIRCLE` whose geometry is RemoteFloat
 * expressions over the canvas size, as `RemoteTimeText` draws it.
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RcPlayerDrawTextOnCircleScreenshotTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val remoteComposeTestRule =
        RemoteEmbeddedScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = context,
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    @Test
    fun expressionGeometry() {
        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo = textOnCircleDisplayInfo(context),
            profile = TEXT_ON_CIRCLE_PROFILE,
            playComposableWrapper = ComposableWrappers.blackBackground,
        ) {
            TextOnCircleDemo()
        }
    }
}

/** A profile that adds `DRAW_TEXT_ON_CIRCLE` (an Android native operation) to AndroidX. */
internal val TEXT_ON_CIRCLE_PROFILE =
    Profile(
        CoreDocument.DOCUMENT_API_LEVEL,
        RcProfiles.PROFILE_ANDROID_NATIVE,
        AndroidxRcPlatformServices(),
        {
            Operations.getOperations(CoreDocument.DOCUMENT_API_LEVEL, RcProfiles.PROFILE_ANDROIDX)
                ?.keySet()
                .orEmpty() + setOf(Operations.DRAW_TEXT_ON_CIRCLE)
        },
    ) { creationDisplayInfo, profile, callback ->
        RemoteComposeWriterAndroid(creationDisplayInfo, null, profile, callback)
    }

/** A 200dp square document. */
internal fun textOnCircleDisplayInfo(context: Context): RemoteCreationDisplayInfo {
    val metrics = context.resources.displayMetrics
    val sizePx = (200 * metrics.density).roundToInt()
    return RemoteCreationDisplayInfo(
        sizePx,
        sizePx,
        metrics.densityDpi,
        context.resources.configuration.fontScale,
    )
}

/**
 * Text laid along a circle centred in the canvas, outside the top and inside the bottom, with a
 * guide circle. Every geometry value is an expression over the canvas size.
 */
@Composable
@RemoteComposable
internal fun TextOnCircleDemo() {
    RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
        val textSize = width / 10f.rf
        val radius = width / 2f.rf - textSize * 1.5f.rf
        drawCircle(
            paint =
                RemotePaint {
                    color = RemoteColor(Color.Gray)
                    style = RemotePaintingStyle.Stroke
                    strokeWidth = 2f.rf
                },
            radius = radius,
        )
        drawCircleText("OUTSIDE TOP", textSize, radius, startAngle = 270f, outside = true)
        drawCircleText("INSIDE BOTTOM", textSize, radius, startAngle = 90f, outside = false)
    }
}

private fun RemoteDrawScope.drawCircleText(
    text: String,
    textSize: RemoteFloat,
    radius: RemoteFloat,
    startAngle: Float,
    outside: Boolean,
) {
    drawTextOnCircle(
        text = RemoteString(text),
        centerX = width / 2f.rf,
        centerY = height / 2f.rf,
        radius = radius,
        startAngle = startAngle.rf,
        warpRadiusOffset = 0f.rf,
        paint =
            RemotePaint {
                color = RemoteColor(Color.White)
                this.textSize = textSize
            },
        alignment = RemoteDrawScope.CircularAlignment.Center,
        placement =
            if (outside) RemoteDrawScope.CircularPlacement.Outside
            else RemoteDrawScope.CircularPlacement.Inside,
    )
}
