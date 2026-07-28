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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.rememberRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow as Row
import androidx.compose.remote.creation.compose.layout.RemoteStateLayout
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier as Modifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.visibility
import androidx.compose.remote.creation.compose.modifier.wrapContentSize
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.MutableRemoteEnum
import androidx.compose.remote.creation.compose.state.RemoteEnum
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteEnum
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier as ComposeModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
class RcPlayerSwitchDemoTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @Test
    fun switchWidgetDemo() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val widthPx = with(LocalDensity.current) { 360.dp.toPx() }
            val heightPx = with(LocalDensity.current) { 240.dp.toPx() }
            val document =
                rememberRemoteDocument(
                    createCreationDisplayInfo(width = widthPx.toInt(), height = heightPx.toInt())
                ) {
                    SwitchWidgetDemo()
                }

            Box(modifier = ComposeModifier.size(360.dp, 240.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }

        rule.mainClock.advanceTimeBy(100)

        rule.onRoot().captureToImage().assertAgainstGolden(screenshotRule, "switchWidgetDemo")
    }

    @Test
    fun switchWidgetDemo_toggledStateC() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val widthPx = with(LocalDensity.current) { 360.dp.toPx() }
            val heightPx = with(LocalDensity.current) { 240.dp.toPx() }
            val document =
                rememberRemoteDocument(
                    createCreationDisplayInfo(width = widthPx.toInt(), height = heightPx.toInt())
                ) {
                    SwitchWidgetDemo()
                }

            Box(modifier = ComposeModifier.size(360.dp, 240.dp)) {
                document.value?.let { RcPlayer(document = it) }
            }
        }
        rule.mainClock.advanceTimeBy(100)

        // Toggle State C switch to Off.
        rule.onNodeWithContentDescription("State C").performClick()
        rule.waitForIdle()

        rule.mainClock.advanceTimeBy(100)

        rule
            .onRoot()
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "switchWidgetDemo_toggledStateC")
    }

    // Copy of the SwitchWidgetDemo and related functions from SwitchWidget.kt

    @Composable
    @RemoteComposable
    fun SwitchWidgetOnState(modifier: Modifier = Modifier, id: Int = 0) {
        RemoteBox(
            modifier =
                modifier
                    .clip(RemoteRoundedCornerShape(20.rdp))
                    .background(Color(63, 81, 181, 255).rc)
                    .padding(2.rdp),
            contentAlignment = RemoteAlignment.CenterEnd,
        ) {
            RemoteCanvas(modifier = Modifier.size(32.rdp)) {
                val paint = RemotePaint().apply { color = Color(255, 255, 255).rc }
                drawCircle(paint = paint, radius = 34f.rf)
            }
        }
    }

    @Composable
    @RemoteComposable
    fun SwitchWidgetOffState(modifier: Modifier = Modifier) {
        RemoteBox(
            modifier =
                modifier
                    .clip(RemoteRoundedCornerShape(20.rdp))
                    .background(Color(100, 100, 100).rc)
                    .padding(8.rdp)
                    .then(modifier),
            contentAlignment = RemoteAlignment.CenterStart,
        ) {
            RemoteCanvas(modifier = Modifier.size(20.rdp)) {
                val paint = RemotePaint().apply { color = Color(220, 220, 220).rc }
                drawCircle(paint = paint, radius = 34f.rf)
            }
        }
    }

    @Composable
    @RemoteComposable
    fun RemoteComponent(name: String, content: @Composable @RemoteComposable () -> Unit) {
        content()
    }

    @Composable
    @RemoteComposable
    fun SwitchComponent(value: MutableRemoteEnum<SwitchState>) {
        RemoteComponent("switch") { SwitchWidget(value) }
    }

    enum class SwitchState(val visibility: RemoteInt) {
        Off(Component.Visibility.GONE.ri),
        On(Component.Visibility.VISIBLE.ri),
    }

    @Composable
    @RemoteComposable
    fun SwitchWidget(value: MutableRemoteEnum<SwitchState>, description: String? = null) {
        var modifier =
            Modifier.clickable(
                valueChange(remoteState = value.remoteInt, updatedValue = (value.remoteInt + 1) % 2)
            )
        if (description != null) {
            modifier = modifier.semantics { contentDescription = description.rs }
        }

        RemoteBox(
            modifier = Modifier.padding(4.rdp),
            contentAlignment = RemoteAlignment.CenterStart,
        ) {
            val modifierSize = Modifier.size(60.rdp, 36.rdp)
            RemoteStateLayout(modifier = Modifier.wrapContentSize(), currentState = value) { state
                ->
                RemoteBox {
                    when (state) {
                        SwitchState.Off -> SwitchWidgetOffState(modifier = modifierSize)
                        SwitchState.On -> SwitchWidgetOnState(modifier = modifierSize)
                    }
                }
            }
            RemoteBox(modifier = modifierSize.clip(RemoteRoundedCornerShape(20.rdp)).then(modifier))
        }
    }

    @Composable
    @RemoteComposable
    fun RowSwitch(
        state: MutableRemoteEnum<SwitchState>,
        label: String,
        modifier: Modifier = Modifier,
    ) {
        Row(modifier = modifier, verticalAlignment = RemoteAlignment.CenterVertically) {
            RemoteText(label.rs)
            SwitchWidget(state, description = label)
            RemoteText("State value is ".rs)
            RemoteText(state.toRemoteString { it.name.rs }, color = Color.White.rc)
        }
    }

    @Composable
    @RemoteComposable
    fun StateInfo(state: RemoteEnum<SwitchState>, label: String, modifier: Modifier = Modifier) {
        Row(modifier = modifier, verticalAlignment = RemoteAlignment.CenterVertically) {
            RemoteText(label.rs)
            RemoteText(state.toRemoteString { it.name.rs }, color = Color.White.rc)
        }
    }

    @Composable
    @RemoteComposable
    fun Divider(modifier: Modifier = Modifier) {
        RemoteBox(
            modifier =
                modifier
                    .padding(start = 8.rdp, end = 8.rdp)
                    .size(2.rdp, 8.rdp)
                    .background(Color.LightGray.rc)
        )
    }

    @Composable
    @RemoteComposable
    fun SwitchWidgetDemo() {
        RemoteColumn(modifier = Modifier.padding(8.rdp).background(Color.LightGray.rc)) {
            val checkedA = rememberMutableRemoteEnum(SwitchState.Off)
            val checkedB = rememberMutableRemoteEnum(SwitchState.Off)
            val checkedC = rememberMutableRemoteEnum(SwitchState.On)

            val visibilityModifierC = Modifier.visibility(checkedC.visibility)
            RowSwitch(checkedA, "State A")
            RowSwitch(checkedB, "State B", modifier = visibilityModifierC)
            RowSwitch(checkedA, "State A", modifier = visibilityModifierC)
            RowSwitch(checkedC, "State C")
            Row(
                modifier = Modifier.padding(top = 8.rdp).fillMaxWidth(),
                horizontalArrangement = RemoteArrangement.Center,
                verticalAlignment = RemoteAlignment.CenterVertically,
            ) {
                val visibilityModifierB = Modifier.visibility(checkedB.visibility)
                StateInfo(checkedA, "A is ")
                Divider(modifier = visibilityModifierB)
                StateInfo(checkedB, "B is ", modifier = visibilityModifierB)
                Divider()
                StateInfo(checkedC, "C is ")
            }
        }
    }

    val RemoteEnum<SwitchState>.visibility: RemoteInt
        get() = toRemoteInt { it.visibility }
}
