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

package androidx.test.uiautomator.testapp

import android.widget.Button
import androidx.kruth.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.ElementNotFoundException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.isClass
import androidx.test.uiautomator.onElement
import androidx.test.uiautomator.onElementOrNull
import androidx.test.uiautomator.scrollToElement
import androidx.test.uiautomator.simpleViewResourceName
import androidx.test.uiautomator.textAsString
import androidx.test.uiautomator.uiAutomator
import androidx.test.uiautomator.waitForStable
import androidx.test.uiautomator.watcher.ScopedUiWatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiAutomatorTestScopeTest {

    companion object {
        private const val APP_PACKAGE_NAME = "androidx.test.uiautomator.testapp"
    }

    @Before
    fun setup() = uiAutomator {
        device.wakeUp()
        device.pressMenu()
        device.pressHome()
        device.setOrientationNatural()
    }

    @Test
    @LargeTest
    fun mainActivity() = uiAutomator {
        startActivity(MainActivity::class.java)

        onElement { simpleViewResourceName() == "button" }.click()
        onElement { textAsString() == "Accessible button" }.click()

        with(onElement { simpleViewResourceName() == "nested_elements" }) {
            onElement { textAsString() == "First Level" }
            onElement { textAsString() == "Second Level" }
            onElement { textAsString() == "Third Level" }
        }
    }

    @Test
    @LargeTest
    fun waitForStable() = uiAutomator {
        startActivity(MainActivity::class.java)
        waitForStableInActiveWindow(stableTimeoutMs = 10000, stableIntervalMs = 5000)

        // The timeout of onElement is set to `0`: this should fail if the previous transition
        // hasn't completed.
        onElement(0) { simpleViewResourceName() == "button" }.click()
    }

    @Test
    @LargeTest
    fun nodeWaitForStable() = uiAutomator {
        startActivity(MainActivity::class.java)

        onElement { packageName == APP_PACKAGE_NAME }.waitForStable()

        // The timeout of onElement is set to `0`: this should fail if the previous transition
        // hasn't completed.
        onElement(0) { simpleViewResourceName() == "button" }.click()
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    @LargeTest
    fun bySelectorTestActivity() = uiAutomator {
        startActivity(BySelectorTestActivity::class.java)

        with(onElement { simpleViewResourceName() == "selected" }) {
            assertThat(isSelected)
            click()
            assertThat(!isSelected)
        }

        onElement {
            packageName == APP_PACKAGE_NAME &&
                simpleViewResourceName() == "clazz" &&
                isClass(Button::class.java)
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    @LargeTest
    fun hintTestActivity() = uiAutomator {
        startActivity(HintTestActivity::class.java)
        onElement { hintText == "sample_hint" }
    }

    @Test(expected = ElementNotFoundException::class)
    @LargeTest
    fun bySelectorTestActivityFail() = uiAutomator {
        startActivity(BySelectorTestActivity::class.java)

        onElement { simpleViewResourceName() == "clazz" && className == "android.widget.TextView" }
    }

    @Test
    @LargeTest
    fun composeTest() = uiAutomator {
        startActivity(ComposeTestActivity::class.java)

        onElement { simpleViewResourceName() == "top-text" }
        val button =
            onElement { isScrollable }
                .scrollToElement(Direction.DOWN) { className == Button::class.java.name }
        val textView = onElement { textAsString() == "Initial" }
        button.click()
        assertThat(textView.text).isEqualTo("Updated")
    }

    @Test
    @LargeTest
    fun pressYesOnDialogActivityTest() = uiAutomator {
        startActivity(DialogActivity::class.java)
        watchFor(MyDialog()) { clickYes() }
        onElement { textAsString() == "Show Dialog" }.click()
        onElement { textAsString() == "Dialog Result: Pressed Yes" }
    }

    @Test
    @LargeTest
    fun pressNoOnDialogActivityTest() = uiAutomator {
        startActivity(DialogActivity::class.java)
        watchFor(MyDialog()) { clickNo() }
        onElement { textAsString() == "Show Dialog" }.click()
        onElement { textAsString() == "Dialog Result: Pressed No" }
    }
}

// Define a dialog
class MyDialog : ScopedUiWatcher<MyDialog.Scope> {

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    override fun isVisible(): Boolean =
        uiDevice.onElementOrNull(0) { textAsString() == "Confirmation" } != null

    override fun scope() = Scope()

    inner class Scope {
        fun clickYes() = uiDevice.onElement { textAsString() == "Yes" }.click()

        fun clickNo() = uiDevice.onElement { textAsString() == "No" }.click()
    }
}
