package androidx.compose.foundation.text2

import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardHelper
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.AssumptionViolatedException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class TextFieldFocusTest {
    @get:Rule
    val rule = createComposeRule()

    private val testKeyboardController = TestSoftwareKeyboardController(rule)

    @Composable
    private fun TextFieldApp(dataList: List<FocusTestData>) {
        for (data in dataList) {
            val state = remember { TextFieldState() }
            BasicTextField2(
                state = state,
                modifier = Modifier
                    .focusRequester(data.focusRequester)
                    .onFocusChanged { data.focused = it.isFocused }
                    .requiredWidth(10.dp)
            )
        }
    }

    data class FocusTestData(val focusRequester: FocusRequester, var focused: Boolean = false)

    @Test
    fun requestFocus() {
        lateinit var testDataList: List<FocusTestData>

        rule.setContent {
            testDataList = listOf(
                FocusTestData(FocusRequester()),
                FocusTestData(FocusRequester()),
                FocusTestData(FocusRequester())
            )

            TextFieldApp(testDataList)
        }

        rule.runOnIdle { testDataList[0].focusRequester.requestFocus() }

        rule.runOnIdle {
            assertThat(testDataList[0].focused).isTrue()
            assertThat(testDataList[1].focused).isFalse()
            assertThat(testDataList[2].focused).isFalse()
        }

        rule.runOnIdle { testDataList[1].focusRequester.requestFocus() }
        rule.runOnIdle {
            assertThat(testDataList[0].focused).isFalse()
            assertThat(testDataList[1].focused).isTrue()
            assertThat(testDataList[2].focused).isFalse()
        }

        rule.runOnIdle { testDataList[2].focusRequester.requestFocus() }
        rule.runOnIdle {
            assertThat(testDataList[0].focused).isFalse()
            assertThat(testDataList[1].focused).isFalse()
            assertThat(testDataList[2].focused).isTrue()
        }
    }

    @Test
    fun noCrashWhenSwitchingBetweenEnabledFocusedAndDisabledTextField() {
        val enabled = mutableStateOf(true)
        var focused = false
        val tag = "textField"
        rule.setContent {
            val state = remember { TextFieldState() }
            BasicTextField2(
                state = state,
                enabled = enabled.value,
                modifier = Modifier
                    .testTag(tag)
                    .onFocusChanged {
                        focused = it.isFocused
                    }
                    .requiredWidth(10.dp)
            )
        }
        // bring enabled text field into focus
        rule.onNodeWithTag(tag).performClick()
        rule.runOnIdle {
            assertThat(focused).isTrue()
        }

        // make text field disabled
        enabled.value = false
        rule.runOnIdle {
            assertThat(focused).isFalse()
        }

        // make text field enabled again, it must not crash
        enabled.value = true
        rule.runOnIdle {
            assertThat(focused).isFalse()
        }
    }

    @SdkSuppress(minSdkVersion = 22) // b/266742195
    @Test
    fun keyboardIsShown_forFieldInActivity_whenFocusRequestedImmediately_fromLaunchedEffect() {
        keyboardIsShown_whenFocusRequestedImmediately_fromEffect(
            runEffect = {
                LaunchedEffect(Unit) {
                    it()
                }
            }
        )
    }

    @SdkSuppress(minSdkVersion = 22) // b/266742195
    @Test
    fun keyboardIsShown_forFieldInActivity_whenFocusRequestedImmediately_fromDisposableEffect() {
        keyboardIsShown_whenFocusRequestedImmediately_fromEffect(
            runEffect = {
                DisposableEffect(Unit) {
                    it()
                    onDispose {}
                }
            }
        )
    }

    // TODO(b/229378542) We can't accurately detect IME visibility from dialogs before API 30 so
    //  this test can't assert.
    @SdkSuppress(minSdkVersion = 30)
    @Test
    fun keyboardIsShown_forFieldInDialog_whenFocusRequestedImmediately_fromLaunchedEffect() {
        keyboardIsShown_whenFocusRequestedImmediately_fromEffect(
            runEffect = {
                LaunchedEffect(Unit) {
                    it()
                }
            },
            wrapContent = {
                Dialog(onDismissRequest = {}, content = it)
            }
        )
    }

    // TODO(b/229378542) We can't accurately detect IME visibility from dialogs before API 30 so
    //  this test can't assert.
    @SdkSuppress(minSdkVersion = 30)
    @Test
    fun keyboardIsShown_forFieldInDialog_whenFocusRequestedImmediately_fromDisposableEffect() {
        keyboardIsShown_whenFocusRequestedImmediately_fromEffect(
            runEffect = {
                DisposableEffect(Unit) {
                    it()
                    onDispose {}
                }
            },
            wrapContent = {
                Dialog(onDismissRequest = {}, content = it)
            }
        )
    }

    private fun keyboardIsShown_whenFocusRequestedImmediately_fromEffect(
        runEffect: @Composable (body: () -> Unit) -> Unit,
        wrapContent: @Composable (@Composable () -> Unit) -> Unit = { it() }
    ) {
        val focusRequester = FocusRequester()
        val keyboardHelper = KeyboardHelper(rule)
        val state = TextFieldState()

        rule.setContent {
            wrapContent {
                keyboardHelper.initialize()

                runEffect {
                    assertThat(keyboardHelper.isSoftwareKeyboardShown()).isFalse()
                    focusRequester.requestFocus()
                }

                BasicTextField2(
                    state = state,
                    modifier = Modifier.focusRequester(focusRequester)
                )
            }
        }

        keyboardHelper.waitForKeyboardVisibility(visible = true)

        // Ensure the keyboard doesn't leak in to the next test. Can't do this at the start of the
        // test since the KeyboardHelper won't be initialized until composition runs, and this test
        // is checking behavior that all happens on the first frame.
        keyboardHelper.hideKeyboard()
        keyboardHelper.waitForKeyboardVisibility(visible = false)
    }

    @SdkSuppress(minSdkVersion = 22) // b/266742195
    @Test
    fun basicTextField_checkFocusNavigation_onDPadLeft() {
        setupAndEnableBasicTextField()
        inputSingleLineTextInBasicTextField()

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)
        rule.waitForIdle()

        // Move focus to the focusable element on left
        keyPressOnDpadInputDevice(rule, NativeKeyEvent.KEYCODE_DPAD_LEFT)

        // Check if the element to the left of text field gains focus
        rule.onNodeWithTag("test-button-left").assertIsFocused()
    }

    @SdkSuppress(minSdkVersion = 22) // b/266742195
    @Test
    fun basicTextField_checkFocusNavigation_onDPadRight() {
        setupAndEnableBasicTextField()
        inputSingleLineTextInBasicTextField()

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)
        rule.waitForIdle()

        // Move focus to the focusable element on right
        keyPressOnDpadInputDevice(rule, NativeKeyEvent.KEYCODE_DPAD_RIGHT)

        // Check if the element to the right of text field gains focus
        rule.onNodeWithTag("test-button-right").assertIsFocused()
    }

    @SdkSuppress(minSdkVersion = 22) // b/266742195
    @Test
    fun basicTextField_checkFocusNavigation_onDPadUp() {
        setupAndEnableBasicTextField()
        inputMultilineTextInBasicTextField()

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)
        rule.waitForIdle()

        // Move focus to the focusable element on top
        keyPressOnDpadInputDevice(rule, NativeKeyEvent.KEYCODE_DPAD_UP)

        // Check if the element on the top of text field gains focus
        rule.onNodeWithTag("test-button-top").assertIsFocused()
    }

    @SdkSuppress(minSdkVersion = 22) // b/266742195
    @Test
    fun basicTextField_checkFocusNavigation_onDPadDown() {
        setupAndEnableBasicTextField()
        inputMultilineTextInBasicTextField()

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)
        rule.waitForIdle()

        // Move focus to the focusable element on bottom
        keyPressOnDpadInputDevice(rule, NativeKeyEvent.KEYCODE_DPAD_DOWN)

        // Check if the element to the bottom of text field gains focus
        rule.onNodeWithTag("test-button-bottom").assertIsFocused()
    }

    @Test
    fun basicTextField_checkKeyboardShown_onDPadCenter() {
        setupAndEnableBasicTextField()
        inputSingleLineTextInBasicTextField()

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)
        testKeyboardController.assertHidden()

        // Check if keyboard is enabled on Dpad center key press
        keyPressOnDpadInputDevice(rule, NativeKeyEvent.KEYCODE_DPAD_CENTER)
        testKeyboardController.assertShown()
    }

    @Test
    fun basicTextField_handlesInvalidDevice() {
        setupAndEnableBasicTextField()
        inputSingleLineTextInBasicTextField()

        // -2 shouldn't be a valid device – we verify this below by asserting the device in the
        // event is actually null.
        val invalidDeviceId = -2
        val keyCode = NativeKeyEvent.KEYCODE_DPAD_CENTER
        val keyEventDown = KeyEvent(
            SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
            KeyEvent.ACTION_DOWN, keyCode, 0, 0, invalidDeviceId, 0
        )
        assertThat(keyEventDown.device).isNull()
        rule.onRoot().performKeyPress(androidx.compose.ui.input.key.KeyEvent(keyEventDown))
        rule.waitForIdle()
        val keyEventUp = KeyEvent(
            SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
            KeyEvent.ACTION_UP, keyCode, 0, 0, invalidDeviceId, 0
        )
        rule.onRoot().performKeyPress(androidx.compose.ui.input.key.KeyEvent(keyEventUp))
        rule.waitForIdle()
    }

    private fun setupAndEnableBasicTextField() {
        setupContent()

        rule.onNodeWithTag("test-text-field-1").assertIsFocused()
    }

    private fun inputSingleLineTextInBasicTextField() {
        // Input "abc"
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_A)
        rule.waitForIdle()
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_B)
        rule.waitForIdle()
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_C)
        rule.waitForIdle()
    }

    private fun inputMultilineTextInBasicTextField() {
        // Input "a\nb\nc"
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_A)
        rule.waitForIdle()
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_ENTER)
        rule.waitForIdle()
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_B)
        rule.waitForIdle()
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_ENTER)
        rule.waitForIdle()
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_C)
        rule.waitForIdle()
    }

    private fun setupContent() {
        rule.setContent {
            CompositionLocalProvider(
                LocalSoftwareKeyboardController provides testKeyboardController
            ) {
                Column {
                    Row(horizontalArrangement = Arrangement.Center) {
                        TestFocusableElement(id = "top")
                    }
                    Row {
                        TestFocusableElement(id = "left")
                        TestBasicTextField2(id = "1", requestFocus = true)
                        TestFocusableElement(id = "right")
                    }
                    Row(horizontalArrangement = Arrangement.Center) {
                        TestFocusableElement(id = "bottom")
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    @Composable
    private fun TestFocusableElement(id: String) {
        var isFocused by remember {
            mutableStateOf(false)
        }
        BasicText(
            text = "test-button-$id",
            modifier = Modifier
                .testTag("test-button-$id")
                .padding(10.dp)
                .onFocusChanged {
                    isFocused = it.hasFocus
                }
                .focusable()
                .border(2.dp, if (isFocused) Color.Green else Color.Cyan)
        )
    }

    @Composable
    private fun TestBasicTextField2(
        id: String,
        requestFocus: Boolean = false
    ) {
        val state = rememberTextFieldState()
        var isFocused by remember {
            mutableStateOf(false)
        }
        val focusRequester = remember {
            FocusRequester()
        }
        val modifier = if (requestFocus) Modifier.focusRequester(focusRequester) else Modifier

        BasicTextField2(
            state = state,
            modifier = modifier
                .testTag("test-text-field-$id")
                .padding(10.dp)
                .onFocusChanged {
                    isFocused = it.isFocused || it.hasFocus
                }
                .border(2.dp, if (isFocused) Color.Red else Color.Transparent)
        )

        LaunchedEffect(requestFocus, focusRequester) {
            if (requestFocus) focusRequester.requestFocus()
        }
    }

    /** Triggers a key press on the root node from a non-virtual dpad device (if supported). */
    private fun keyPressOnDpadInputDevice(
        rule: ComposeContentTestRule,
        keyCode: Int,
        count: Int = 1
    ) = keyPressOnPhysicalDevice(rule, keyCode, InputDevice.SOURCE_DPAD, count)

    private fun keyPressOnPhysicalDevice(
        rule: ComposeContentTestRule,
        keyCode: Int,
        source: Int,
        count: Int = 1,
        metaState: Int = 0,
    ) {
        // Since we can't create our own InputDevices, we have to use one that is connected to the
        // system. This functionality only supports specific types of input devices, so we have to
        // filter and if we can't find a valid one, the test can't run. In that case, we throw
        // an AssumptionViolatedException which will abort the test and cause JUnit to report it
        // as ignored.
        val deviceId = InputDevice.getDeviceIds().firstOrNull { id ->
            InputDevice.getDevice(id)?.isVirtual?.not() ?: false &&
                InputDevice.getDevice(id)?.supportsSource(source) ?: false
        } ?: throw AssumptionViolatedException(
            "Could not find physical input device to send DPAD event from"
        )

        val keyEventDown = KeyEvent(
            SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
            KeyEvent.ACTION_DOWN, keyCode, 0, metaState,
            deviceId, 0, 0, InputDevice.SOURCE_DPAD
        )
        val keyEventUp = KeyEvent(
            SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
            KeyEvent.ACTION_UP, keyCode, 0, metaState,
            deviceId, 0, 0, InputDevice.SOURCE_DPAD
        )

        repeat(count) {
            rule.onRoot().performKeyPress(androidx.compose.ui.input.key.KeyEvent(keyEventDown))
            rule.waitForIdle()
            rule.onRoot().performKeyPress(androidx.compose.ui.input.key.KeyEvent(keyEventUp))
        }
    }

    /** Triggers a key press on the virtual keyboard. */
    private fun keyPressOnVirtualKeyboard(keyCode: Int, count: Int = 1) {
        repeat(count) {
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(keyCode)
        }
    }
}
