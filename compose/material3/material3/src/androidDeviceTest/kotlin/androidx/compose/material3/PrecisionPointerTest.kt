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

package androidx.compose.material3

import android.os.Build
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.LocalUiMediaScope
import androidx.compose.ui.UiMediaScope
import androidx.compose.ui.UiMediaScope.KeyboardKind
import androidx.compose.ui.UiMediaScope.PointerPrecision
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMediaQueryApi::class,
)
@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.P) // Needed for inline mocking
class PrecisionPointerTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Before
    fun setup() {
        shouldUsePrecisionPointerComponentSizing.value = false
    }

    @Test
    fun precisionPointerUiDisabled_withPhysicalKeyboardAndMouse_noDenseUi() {
        ComposeUiFlags.isMediaQueryIntegrationEnabled = false

        val uiMediaScope = MockUiMediaScope()

        rule.setContent(uiMediaScope)
        uiMediaScope.updateKeyboardKind(KeyboardKind.Physical)
        uiMediaScope.updatePointerPrecision(PointerPrecision.Fine)
        rule.runOnIdle { assertThat(shouldUsePrecisionPointerComponentSizing.value).isFalse() }
    }

    @Test
    fun precisionPointerUiEnabled_noDevicesConnected_noDenseUi() {
        ComposeUiFlags.isMediaQueryIntegrationEnabled = true

        val uiMediaScope = MockUiMediaScope()

        rule.setContent(uiMediaScope)

        rule.runOnIdle { assertThat(shouldUsePrecisionPointerComponentSizing.value).isFalse() }
    }

    @Test
    fun precisionPointerUiEnabled_withPhysicalKeyboardAndMouse_usesDenseUi() {
        ComposeUiFlags.isMediaQueryIntegrationEnabled = true

        val uiMediaScope = MockUiMediaScope()

        rule.setContent(uiMediaScope)
        uiMediaScope.updateKeyboardKind(KeyboardKind.Physical)
        uiMediaScope.updatePointerPrecision(PointerPrecision.Fine)
        rule.runOnIdle { assertThat(shouldUsePrecisionPointerComponentSizing.value).isTrue() }
    }

    @Test
    fun precisionPointerUiEnabled_withMouse_physicalKeyboardAddedLater_updatesToUseDenseUi() {
        ComposeUiFlags.isMediaQueryIntegrationEnabled = true

        val uiMediaScope = MockUiMediaScope()

        rule.setContent(uiMediaScope)

        // Add just mouse, not enough to trigger dense UI
        uiMediaScope.updatePointerPrecision(PointerPrecision.Fine)
        rule.runOnIdle { assertThat(shouldUsePrecisionPointerComponentSizing.value).isFalse() }

        // Add keyboard as well, now we have kb+mouse, so we can trigger dense UI
        uiMediaScope.updateKeyboardKind(KeyboardKind.Physical)
        rule.runOnIdle { assertThat(shouldUsePrecisionPointerComponentSizing.value).isTrue() }
    }

    @Test
    fun precisionPointerUiEnabled_updateDeviceToMouse_updatesToUseDenseUi() {
        ComposeUiFlags.isMediaQueryIntegrationEnabled = true

        val uiMediaScope = MockUiMediaScope()

        rule.setContent(uiMediaScope)

        uiMediaScope.updateKeyboardKind(KeyboardKind.Physical)
        uiMediaScope.updatePointerPrecision(PointerPrecision.Coarse)
        rule.runOnIdle { assertThat(shouldUsePrecisionPointerComponentSizing.value).isFalse() }

        uiMediaScope.updatePointerPrecision(PointerPrecision.Fine)
        rule.runOnIdle { assertThat(shouldUsePrecisionPointerComponentSizing.value).isTrue() }
    }

    @Test
    fun precisionPointerUiEnabled_removePhysicalKeyboard_updatesToRemoveDenseUi() {
        ComposeUiFlags.isMediaQueryIntegrationEnabled = true

        val uiMediaScope = MockUiMediaScope()

        rule.setContent(uiMediaScope)

        uiMediaScope.updateKeyboardKind(KeyboardKind.Physical)
        uiMediaScope.updatePointerPrecision(PointerPrecision.Fine)
        rule.runOnIdle { assertThat(shouldUsePrecisionPointerComponentSizing.value).isTrue() }

        uiMediaScope.updateKeyboardKind(KeyboardKind.None)
        rule.runOnIdle { assertThat(shouldUsePrecisionPointerComponentSizing.value).isFalse() }
    }
}

@OptIn(ExperimentalMediaQueryApi::class)
private fun ComposeContentTestRule.setContent(uiMediaScope: MockUiMediaScope) {
    setContent {
        CompositionLocalProvider(LocalUiMediaScope provides uiMediaScope) { MaterialTheme {} }
    }
}

@OptIn(ExperimentalMediaQueryApi::class)
internal class MockUiMediaScope : UiMediaScope {
    override var windowWidth: Dp by mutableStateOf(0.dp)
    override var windowHeight: Dp by mutableStateOf(0.dp)
    override var windowPosture: UiMediaScope.Posture by mutableStateOf(UiMediaScope.Posture.Flat)
    override var pointerPrecision: PointerPrecision by mutableStateOf(PointerPrecision.None)
    override var keyboardKind: KeyboardKind by mutableStateOf(KeyboardKind.None)
    override var hasMicrophone: Boolean by mutableStateOf(false)
    override var hasCamera: Boolean by mutableStateOf(false)
    override var viewingDistance: UiMediaScope.ViewingDistance by
        mutableStateOf(UiMediaScope.ViewingDistance.Near)

    constructor(
        windowWidth: Dp = 0.dp,
        windowHeight: Dp = 0.dp,
        windowPosture: UiMediaScope.Posture = UiMediaScope.Posture.Flat,
        pointerPrecision: PointerPrecision = PointerPrecision.None,
        keyboardKind: KeyboardKind = KeyboardKind.None,
        hasMicrophone: Boolean = false,
        hasCamera: Boolean = false,
        viewingDistance: UiMediaScope.ViewingDistance = UiMediaScope.ViewingDistance.Near,
    ) {
        this.windowWidth = windowWidth
        this.windowHeight = windowHeight
        this.windowPosture = windowPosture
        this.pointerPrecision = pointerPrecision
        this.keyboardKind = keyboardKind
        this.hasMicrophone = hasMicrophone
        this.hasCamera = hasCamera
        this.viewingDistance = viewingDistance
    }

    fun updatePointerPrecision(pointerPrecision: PointerPrecision) {
        this.pointerPrecision = pointerPrecision
    }

    fun updateKeyboardKind(keyboardKind: KeyboardKind) {
        this.keyboardKind = keyboardKind
    }
}
