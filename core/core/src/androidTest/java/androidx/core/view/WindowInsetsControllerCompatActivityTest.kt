/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.core.graphics.Insets
import androidx.core.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.withActivity
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

@Suppress("DEPRECATION")
@SdkSuppress(minSdkVersion = 23)
@RequiresApi(23) // ViewCompat.getRootWindowInsets()
@LargeTest
@RunWith(AndroidJUnit4::class)
public class WindowInsetsControllerCompatActivityTest {

    private lateinit var container: View
    private lateinit var windowInsetsController: WindowInsetsControllerCompat
    private lateinit var scenario: ActivityScenario<WindowInsetsCompatActivity>

    @Before
    public fun setup() {
        scenario = ActivityScenario.launch(WindowInsetsCompatActivity::class.java)

        container = scenario.withActivity { findViewById(R.id.container) }
        windowInsetsController = ViewCompat.getWindowInsetsController(container)!!
        scenario.withActivity {
            WindowCompat.setDecorFitsSystemWindows(window, true)
        }
        // Close the IME if it's open, so we start from a known scenario
        onView(withId(R.id.edittext)).perform(closeSoftKeyboard())

        scenario.withActivity {
            ViewCompat.getWindowInsetsController(container)!!.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            // Needed on API 23 to report the nav bar insets
            this.window.addFlags(FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }
    }

    /**
     * IME visibility is only reliable on API 23+, where we have access to the root WindowInsets
     */
    @SdkSuppress(minSdkVersion = 23)
    @Test
    public fun toggleIME() {
        // Test do not currently work on Cuttlefish
        assumeNotCuttlefish()
        val container: View = scenario.withActivity { findViewById(R.id.container) }
        scenario.withActivity { findViewById<View>(R.id.edittext).requestFocus() }

        val windowInsetsController = ViewCompat.getWindowInsetsController(container)!!
        scenario.onActivity { windowInsetsController.hide(WindowInsetsCompat.Type.ime()) }
        container.assertInsetsVisibility(WindowInsetsCompat.Type.ime(), false)
        testShow(WindowInsetsCompat.Type.ime())
        testHide(WindowInsetsCompat.Type.ime())
    }

    /**
     * IME visibility is only reliable on API 23+, where we have access to the root WindowInsets
     */
    @SdkSuppress(minSdkVersion = 23)
    @Test
    public fun do_not_show_IME_if_TextView_not_focused() {
        val editText = scenario.withActivity {
            findViewById<EditText>(R.id.edittext)
        }

        // We hide the edit text to ensure it won't be automatically focused
        scenario.onActivity {
            editText.visibility = View.GONE
            assertThat(editText.isFocused, `is`(false))
        }

        val type = WindowInsetsCompat.Type.ime()
        scenario.onActivity { windowInsetsController.show(type) }
        container.assertInsetsVisibility(type, false)
    }

    /**
     * IME visibility is only reliable on API 23+, where we have access to the root WindowInsets
     */
    @SdkSuppress(minSdkVersion = 23)
    @Test
    fun show_IME_fromEditText() {
        // Test do not currently work on Cuttlefish
        assumeNotCuttlefish()
        val type = WindowInsetsCompat.Type.ime()
        val editText = scenario.withActivity { findViewById(R.id.edittext) }
        val controller = scenario.withActivity { ViewCompat.getWindowInsetsController(editText)!! }

        scenario.onActivity {
            editText.requestFocus()
        }
        assertThat(editText.isFocused, `is`(true))
        if (Build.VERSION.SDK_INT == 30) {
            // Dirty hack until we figure out why the IME is not showing if we don't wait before
            Thread.sleep(100)
        }
        controller.show(type)
        container.assertInsetsVisibility(type, true)
    }

    /**
     * IME visibility is only reliable on API 23+, where we have access to the root WindowInsets
     */
    @SdkSuppress(minSdkVersion = 23)
    @Test
    public fun hide_IME() {
        // Test do not currently work on Cuttlefish
        assumeNotCuttlefish()
        onView(withId(R.id.edittext)).perform(click())
        container.assertInsetsVisibility(WindowInsetsCompat.Type.ime(), true)
        testHide(WindowInsetsCompat.Type.ime())
    }

    @Test
    public fun toggle_StatusBar() {
        container.assertInsetsVisibility(WindowInsetsCompat.Type.statusBars(), true)
        testHide(WindowInsetsCompat.Type.statusBars())
        testShow(WindowInsetsCompat.Type.statusBars())
    }

    @Test
    public fun toggle_NavBar() {
        testHide(WindowInsetsCompat.Type.navigationBars())
        testShow(WindowInsetsCompat.Type.navigationBars())
    }

    private fun testHide(type: Int) {
        scenario.onActivity { windowInsetsController.hide(type) }
        container.assertInsetsVisibility(type, false)
    }

    private fun testShow(type: Int) {
        // Now open the IME using the InsetsController The IME should
        // now be open
        scenario.onActivity { windowInsetsController.show(type) }
        container.assertInsetsVisibility(type, true)
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    public fun systemBar_light() {
        scenario.onActivity {
            windowInsetsController.setAppearanceLightStatusBars(true)
        }
        if (Build.VERSION.SDK_INT < 30) {
            // The view's systemUiVisibility flags are not changed on API 30+
            val systemUiVisibility = scenario.withActivity { window.decorView }.systemUiVisibility
            assertThat(
                systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR,
                equalTo(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            )
        }
        assertThat(windowInsetsController.isAppearanceLightStatusBars(), `is`(true))
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public fun navigationBar_light() {
        scenario.onActivity {
            windowInsetsController.setAppearanceLightNavigationBars(true)
        }
        val systemUiVisibility = scenario.withActivity { window.decorView }.systemUiVisibility
        if (Build.VERSION.SDK_INT < 30) {
            // The view's systemUiVisibility flags are not changed on API 30+
            assertThat(
                systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR,
                equalTo(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
            )
        }
        assertThat(
            windowInsetsController.isAppearanceLightNavigationBars(), `is`(true)
        )
    }

    /**
     * IME visibility is only reliable on API 23+, where we have access to the root WindowInsets
     */
    @SdkSuppress(minSdkVersion = 23)
    @Ignore("The listener isn't called when changing the visibility")
    @Test
    public fun ime_toggle_check_with_listener() {
        // Test do not currently work on Cuttlefish
        assumeNotCuttlefish()
        val type = WindowInsetsCompat.Type.ime()

        val container: View = scenario.withActivity { findViewById(R.id.container) }

        // Tell the window that our view will fit system windows
        container.doAndAwaitNextInsets {
            scenario.onActivity { activity ->
                WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            }
        }.let { insets ->
            // Assert that the IME visibility is false and the insets are empty
            assertThat(insets.isVisible(type), `is`(false))
            assertEquals(Insets.NONE, insets.getInsets(type))
        }

        val windowInsetsController = ViewCompat.getWindowInsetsController(container)!!

        // Now open the IME using the InsetsController The IME should
        // now be open
        // container.doAndAwaitNextInsets {
        scenario.onActivity {
            windowInsetsController.show(type)
        }

        // Finally dismiss the IME
        container.doAndAwaitNextInsets {
            scenario.onActivity {
                windowInsetsController.hide(type)
            }
        }.let { insets ->
            // Assert that the IME visibility is false and the insets are empty
            assertThat(insets.isVisible(type), `is`(false))
            assertEquals(Insets.NONE, insets.getInsets(type))
        }
    }

    @Test
    // minSdkVersion = 21 due to b/189492236
    @SdkSuppress(minSdkVersion = 21, maxSdkVersion = 29) // Flag deprecated in 30+
    public fun systemBarsBehavior_swipe() {
        scenario.onActivity {
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
        }
        val decorView = scenario.withActivity { window.decorView }
        val sysUiVis = decorView.systemUiVisibility
        assertEquals(
            View.SYSTEM_UI_FLAG_IMMERSIVE,
            sysUiVis and View.SYSTEM_UI_FLAG_IMMERSIVE
        )
        assertEquals(0, sysUiVis and View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    @Test
    // minSdkVersion = 21 due to b/189492236
    @SdkSuppress(minSdkVersion = 21, maxSdkVersion = 29) // Flag deprecated in 30+
    public fun systemBarsBehavior_transient() {
        scenario.onActivity {
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        val decorView = scenario.withActivity { window.decorView }
        val sysUiVis = decorView.systemUiVisibility
        assertEquals(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY,
            sysUiVis and View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        assertEquals(0, sysUiVis and View.SYSTEM_UI_FLAG_IMMERSIVE)
    }

    @Test
    public fun systemBarsBehavior_touch() {
        scenario.onActivity {
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH
        }
        val decorView = scenario.withActivity { window.decorView }
        val sysUiVis = decorView.systemUiVisibility
        assertEquals(
            0,
            sysUiVis and (
                View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        )
    }

    private fun assumeNotCuttlefish() {
        // TODO: remove this if b/159103848 is resolved
        assumeFalse(
            "Unable to test: Cuttlefish devices default to the virtual keyboard being disabled.",
            Build.MODEL.contains("Cuttlefish", ignoreCase = true)
        )
    }

    @After
    fun cleanup() {
        scenario.close()
    }

    @RequiresApi(23) //  ViewCompat.getRootWindowInsets()
    private fun View.assertInsetsVisibility(
        type: Int,
        expectedVisibility: Boolean
    ) {
        val latch = CountDownLatch(1)
        var loop = true
        var lastVisibility: Boolean? = null
        try {
            thread {
                while (loop) {
                    val rootWindowInsets = scenario.withActivity {
                        ViewCompat
                            .getRootWindowInsets(this@assertInsetsVisibility)!!
                    }
                    lastVisibility = rootWindowInsets.isVisible(type)
                    if (lastVisibility == expectedVisibility) {
                        latch.countDown()
                    }
                    Thread.sleep(300)
                }
            }

            latch.await(5, TimeUnit.SECONDS)
        } finally {
            loop = false
            assertThat(
                "isVisible() should be <$expectedVisibility> but is <$lastVisibility>",
                lastVisibility, `is`(expectedVisibility)
            )
        }
    }
}

private fun View.doAndAwaitNextInsets(action: (View) -> Unit): WindowInsetsCompat {
    val latch = CountDownLatch(1)
    val received = AtomicReference<WindowInsetsCompat>()

    // Set a listener to catch WindowInsets
    ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets: WindowInsetsCompat ->
        received.set(insets)
        latch.countDown()

        WindowInsetsCompat.CONSUMED
    }

    try {
        // Perform the action
        action(this)
        // Await an inset pass
        if (!latch.await(5, TimeUnit.SECONDS)) {
            fail("OnApplyWindowInsetsListener was not called")
        }
    } finally {
        this.post {
            ViewCompat.setOnApplyWindowInsetsListener(this, null)
        }
    }
    return received.get()
}
