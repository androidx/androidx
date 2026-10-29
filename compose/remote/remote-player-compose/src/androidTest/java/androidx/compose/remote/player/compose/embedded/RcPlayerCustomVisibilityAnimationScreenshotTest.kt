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

import android.content.res.Resources
import androidx.compose.remote.core.operations.FloatFunctionDefine
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.action.valueChangeAction
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteEnterTransition
import androidx.compose.remote.creation.compose.modifier.RemoteExitTransition
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.animationSpec
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.visibility
import androidx.compose.remote.creation.compose.state.MutableRemoteEnum
import androidx.compose.remote.creation.compose.state.RemoteEasing
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.remoteTween
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.player.compose.test.utils.RemoteEmbeddedScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Screenshots of custom visibility animations (`AnimationSpec.ANIMATION.CUSTOM`) in [RcPlayer],
 * captured halfway through a linear transition for both enter (GONE -> VISIBLE) and exit (VISIBLE
 * -> GONE).
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(Parameterized::class)
class RcPlayerCustomVisibilityAnimationScreenshotTest(private val case: Case) {

    enum class Case(val label: String) {
        ScaleFade("Custom Scale+Fade"),
        OffscreenCapture("Offscreen Bmp"),
    }

    private val density = Resources.getSystem().displayMetrics.density

    @get:Rule
    val screenshotRule =
        RemoteEmbeddedScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            remoteCreationDisplayInfo =
                createCreationDisplayInfo(
                    context = ApplicationProvider.getApplicationContext(),
                    size = Size(WIDTH_DP * density, HEIGHT_DP * density),
                ),
        )

    @Test fun exitHalfway() = captureHalfway(initial = Shown.Visible)

    @Test fun enterHalfway() = captureHalfway(initial = Shown.Gone)

    private fun captureHalfway(initial: Shown) {
        val composeRule = screenshotRule.composeTestRule
        composeRule.mainClock.autoAdvance = false
        screenshotRule.setContent { CustomVisibilityAnimationDemo(case, initial) }
        composeRule.mainClock.advanceTimeBy(100)

        composeRule.onNodeWithContentDescription(TOGGLE).performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(DURATION_MS / 2L)

        screenshotRule.verifyScreenshot()
    }

    enum class Shown(val visibility: RemoteInt) {
        Visible(Component.Visibility.VISIBLE.ri),
        Gone(Component.Visibility.GONE.ri),
    }

    @Composable
    @RemoteComposable
    private fun CustomVisibilityAnimationDemo(case: Case, initial: Shown) {
        val shown = remember { MutableRemoteEnum(initial) }
        val direction = if (initial == Shown.Gone) "Enter" else "Exit"
        RemoteColumn(
            modifier =
                RemoteModifier.fillMaxSize().background(Color.White.rc).drawWithContent {
                    document.beginGlobal()
                    when (case) {
                        Case.ScaleFade -> {
                            defineScaleFadeFunction(document, ENTER_FN_ID, isEnter = true)
                            defineScaleFadeFunction(document, EXIT_FN_ID, isEnter = false)
                        }
                        Case.OffscreenCapture -> {
                            defineOffscreenCaptureFunction(document, ENTER_FN_ID, isEnter = true)
                            defineOffscreenCaptureFunction(document, EXIT_FN_ID, isEnter = false)
                        }
                    }
                    document.endGlobal()
                    drawContent()
                }
        ) {
            RemoteBox(
                modifier =
                    RemoteModifier.size(WIDTH_DP.rdp, 24.rdp)
                        .background(Color.DarkGray.rc)
                        .clickable(
                            valueChangeAction(
                                remoteState = shown.remoteInt,
                                updatedValue = (shown.remoteInt + 1) % 2,
                            )
                        )
                        .semantics { contentDescription = TOGGLE.rs },
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText(
                    "${case.label} $direction @ 50%".rs,
                    color = Color.White.rc,
                    fontSize = 11.rsp,
                )
            }
            RemoteBox(
                modifier = RemoteModifier.fillMaxSize().background(Color.LightGray.rc),
                contentAlignment = RemoteAlignment.Center,
            ) {
                // Yellow backdrop stripe shows through wherever alpha < 1.
                RemoteBox(
                    modifier = RemoteModifier.size(160.rdp, 20.rdp).background(Color.Yellow.rc)
                )
                // Target outline marks the resting 96x48 bounds of the animated card.
                RemoteBox(
                    modifier = RemoteModifier.size(96.rdp, 48.rdp).border(1.rdp, Color.Black.rc)
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.size(96.rdp, 48.rdp)
                            .background(Color.Blue.rc)
                            .visibility(shown.toRemoteInt { it.visibility })
                            .animationSpec(
                                motionSpec = remoteTween(0, RemoteEasing.Linear),
                                visibilitySpec = remoteTween(DURATION_MS, RemoteEasing.Linear),
                                enter = RemoteEnterTransition.Custom(ENTER_FN_ID),
                                exit = RemoteExitTransition.Custom(EXIT_FN_ID),
                            ),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteText("Card".rs, color = Color.White.rc, fontSize = 14.rsp)
                }
            }
        }
    }

    companion object {
        private const val WIDTH_DP = 200f
        private const val HEIGHT_DP = 144f
        private const val DURATION_MS = 400
        private const val TOGGLE = "Toggle"
        private const val ENTER_FN_ID = 701
        private const val EXIT_FN_ID = 702

        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases(): List<Case> = Case.entries

        private fun beginFloatFunction(doc: RemoteComposeWriter, fnId: Int): FloatArray {
            val intArgs = IntArray(6) { 500 + it }
            FloatFunctionDefine.apply(doc.buffer.buffer, fnId, intArgs)
            return FloatArray(6) { i -> Utils.asNan(intArgs[i]) }
        }

        private fun defineScaleFadeFunction(
            doc: RemoteComposeWriter,
            fnId: Int,
            isEnter: Boolean,
        ) {
            val args = beginFloatFunction(doc, fnId)
            val progress = args[0]
            val w = args[1]
            val h = args[2]
            val x = args[3]
            val y = args[4]
            val factor =
                if (isEnter) {
                    progress
                } else {
                    doc.floatExpression(1f, progress, AnimatedFloatExpression.SUB)
                }
            // Anchor Enter to the left edge and Exit to the right edge of the target frame so
            // Enter @ 50% and Exit @ 50% are visually distinct within the target outline.
            val pivotX =
                if (isEnter) {
                    x
                } else {
                    doc.floatExpression(x, w, AnimatedFloatExpression.ADD)
                }
            val cy =
                doc.floatExpression(
                    y,
                    h,
                    2f,
                    AnimatedFloatExpression.DIV,
                    AnimatedFloatExpression.ADD,
                )
            doc.save()
            doc.rcPaint.setAlpha(factor).commit()
            doc.scale(factor, factor, pivotX, cy)
            doc.drawComponentContent()
            doc.restore()
            doc.endFloatFunction()
        }

        private fun defineOffscreenCaptureFunction(
            doc: RemoteComposeWriter,
            fnId: Int,
            isEnter: Boolean,
        ) {
            val args = beginFloatFunction(doc, fnId)
            val progress = args[0]
            val w = args[1]
            val h = args[2]
            val x = args[3]
            val y = args[4]
            val componentId = Utils.idFromNan(args[5])

            val offscreenBmpId = doc.createOffscreenBitmap()
            doc.drawComponentToBitmap(componentId, offscreenBmpId)

            val factor =
                if (isEnter) {
                    progress
                } else {
                    doc.floatExpression(1f, progress, AnimatedFloatExpression.SUB)
                }
            val scaledW = doc.floatExpression(w, factor, AnimatedFloatExpression.MUL)
            val quarterH = doc.floatExpression(h, 0.25f, AnimatedFloatExpression.MUL)
            val threeQuarterH = doc.floatExpression(h, 0.75f, AnimatedFloatExpression.MUL)

            // Stamp the captured component bitmap twice inside the target frame (one opaque full
            // height stamp and one 50%-alpha half-height ghost stamp) to visually demonstrate
            // that the component and its "Card" text were captured into an offscreen bitmap.
            doc.save()
            doc.translate(x, y)
            if (isEnter) {
                doc.rcPaint.setAlpha(1f).commit()
                doc.drawBitmap(offscreenBmpId, 0f, 0f, scaledW, h, "enterFull")
                doc.rcPaint.setAlpha(0.5f).commit()
                doc.drawBitmap(offscreenBmpId, scaledW, quarterH, w, threeQuarterH, "enterGhost")
            } else {
                doc.rcPaint.setAlpha(0.5f).commit()
                doc.drawBitmap(offscreenBmpId, 0f, quarterH, scaledW, threeQuarterH, "exitGhost")
                doc.rcPaint.setAlpha(1f).commit()
                doc.drawBitmap(offscreenBmpId, scaledW, 0f, w, h, "exitFull")
            }
            doc.rcPaint.setAlpha(1f).commit()
            doc.restore()
            doc.endFloatFunction()
        }
    }
}
