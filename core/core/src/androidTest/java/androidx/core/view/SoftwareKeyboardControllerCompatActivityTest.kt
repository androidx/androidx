/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.view

import android.app.Dialog
import android.os.Build
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.withActivity
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 23)
@LargeTest
@RunWith(AndroidJUnit4::class)
public class SoftwareKeyboardControllerCompatActivityTest {

    private lateinit var container: View
    private lateinit var softwareKeyboardControllerCompat: SoftwareKeyboardControllerCompat
    private lateinit var scenario: ActivityScenario<SoftwareKeyboardControllerCompatActivity>

    @Before
    public fun setup() {
        scenario = ActivityScenario.launch(SoftwareKeyboardControllerCompatActivity::class.java)

        container = scenario.withActivity { findViewById(R.id.container) }
        scenario.withActivity {
            softwareKeyboardControllerCompat =
                SoftwareKeyboardControllerCompat(container)
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        // Close the IME if it's open, so we start from a known scenario
        Espresso.onView(ViewMatchers.withId(R.id.edittext)).perform(ViewActions.closeSoftKeyboard())
    }

    @Test
    public fun toggleIME() {
        // Test do not currently work on Cuttlefish
        assumeNotCuttlefish()
        val container: View = scenario.withActivity { findViewById(R.id.container) }
        scenario.withActivity { findViewById<View>(R.id.edittext).requestFocus() }

        val softwareKeyboardControllerCompat = scenario.withActivity {
            SoftwareKeyboardControllerCompat(container)
        }
        container.doAndAwaitNextInsets(
            insetsPredicate = { !it.isVisible(WindowInsetsCompat.Type.ime()) }
        ) {
            scenario.onActivity { softwareKeyboardControllerCompat.hide() }
        }

        container.doAndAwaitNextInsets(
            insetsPredicate = { it.isVisible(WindowInsetsCompat.Type.ime()) }
        ) {
            scenario.onActivity { softwareKeyboardControllerCompat.show() }
        }

        container.doAndAwaitNextInsets(
            insetsPredicate = { !it.isVisible(WindowInsetsCompat.Type.ime()) }
        ) {
            scenario.onActivity { softwareKeyboardControllerCompat.hide() }
        }
    }

    @Test
    public fun do_not_show_IME_if_TextView_not_focused() {
        val editText = scenario.withActivity {
            findViewById<EditText>(R.id.edittext)
        }

        // We hide the edit text to ensure it won't be automatically focused
        scenario.onActivity {
            editText.visibility = View.GONE
            ViewMatchers.assertThat(editText.isFocused, Matchers.`is`(false))
        }

        container.doAndAwaitNextInsets(
            insetsPredicate = {
                !it.isVisible(WindowInsetsCompat.Type.ime())
            }
        ) {
            scenario.onActivity { softwareKeyboardControllerCompat.show() }
        }
    }

    @Test
    fun show_IME_fromEditText() {
        // Test do not currently work on Cuttlefish
        assumeNotCuttlefish()
        val editText = scenario.withActivity { findViewById(R.id.edittext) }
        val controller = scenario.withActivity {
            SoftwareKeyboardControllerCompat(editText)
        }

        scenario.onActivity {
            editText.requestFocus()
            controller.show()
        }

        container.doAndAwaitNextInsets(
            insetsPredicate = {
                it.isVisible(WindowInsetsCompat.Type.ime())
            }
        ) {
            scenario.onActivity {
                editText.requestFocus()
                controller.show()
            }
        }

        ViewMatchers.assertThat(editText.isFocused, Matchers.`is`(true))
    }

    @Test
    public fun do_not_show_IME_if_TextView_in_dialog_not_focused() {
        val dialog = scenario.withActivity {
            object : Dialog(this) {
                override fun onAttachedToWindow() {
                    super.onAttachedToWindow()
                    WindowCompat.setDecorFitsSystemWindows(window!!, false)
                }
            }.apply {
                setContentView(R.layout.insets_compat_activity)
            }
        }

        val editText = dialog.findViewById<TextView>(R.id.edittext)

        // We hide the edit text to ensure it won't be automatically focused
        scenario.onActivity {
            dialog.show()
            editText.visibility = View.GONE
            ViewMatchers.assertThat(editText.isFocused, Matchers.`is`(false))
        }

        container.doAndAwaitNextInsets(
            insetsPredicate = {
                !it.isVisible(WindowInsetsCompat.Type.ime())
            }
        ) {
            scenario.onActivity {
                SoftwareKeyboardControllerCompat(editText).show()
            }
        }
    }

    @Test
    fun show_IME_fromEditText_in_dialog() {
        val dialog = scenario.withActivity {
            object : Dialog(this) {
                override fun onAttachedToWindow() {
                    super.onAttachedToWindow()
                    WindowCompat.setDecorFitsSystemWindows(window!!, false)
                }
            }.apply {
                setContentView(R.layout.insets_compat_activity)
            }
        }

        val editText = dialog.findViewById<TextView>(R.id.edittext)

        scenario.onActivity { dialog.show() }

        val controller =
            SoftwareKeyboardControllerCompat(editText)

        container.doAndAwaitNextInsets(
            insetsPredicate = {
                it.isVisible(WindowInsetsCompat.Type.ime())
            }
        ) {
            scenario.onActivity { controller.show() }
        }
    }

    @Test
    public fun hide_IME() {
        // Test do not currently work on Cuttlefish
        assumeNotCuttlefish()

        container.doAndAwaitNextInsets(
            insetsPredicate = {
                it.isVisible(WindowInsetsCompat.Type.ime())
            }
        ) {
            Espresso.onView(ViewMatchers.withId(R.id.edittext)).perform(ViewActions.click())
        }
        container.doAndAwaitNextInsets(
            insetsPredicate = {
                !it.isVisible(WindowInsetsCompat.Type.ime())
            }
        ) {
            scenario.onActivity { softwareKeyboardControllerCompat.hide() }
        }
    }

    private fun assumeNotCuttlefish() {
        // TODO: remove this if b/159103848 is resolved
        Assume.assumeFalse(
            "Unable to test: Cuttlefish devices default to the virtual keyboard being disabled.",
            Build.MODEL.contains("Cuttlefish", ignoreCase = true)
        )
    }

    @After
    fun cleanup() {
        scenario.close()
    }

    private fun View.doAndAwaitNextInsets(
        insetsPredicate: (WindowInsetsCompat) -> Boolean = { true },
        action: (View) -> Unit,
    ): WindowInsetsCompat {
        val latch = CountDownLatch(1)
        val received = AtomicReference<WindowInsetsCompat>()

        // Set a listener to catch WindowInsets
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets: WindowInsetsCompat ->
            if (insetsPredicate(insets)) {
                received.set(insets)
                latch.countDown()
            }

            WindowInsetsCompat.CONSUMED
        }

        scenario.onActivity { ViewCompat.requestApplyInsets(this) }

        try {
            // Perform the action
            action(this)
            // Await an inset pass
            if (!latch.await(5, TimeUnit.SECONDS)) {
                Assert.fail("OnApplyWindowInsetsListener was not called")
            }
        } finally {
            ViewCompat.setOnApplyWindowInsetsListener(this, null)
        }
        return received.get()
    }
}
