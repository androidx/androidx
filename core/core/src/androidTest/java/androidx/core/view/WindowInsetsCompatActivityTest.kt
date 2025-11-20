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

import android.content.Intent
import android.os.Build
import android.view.View
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
import androidx.core.graphics.Insets
import androidx.core.test.R
import androidx.core.view.WindowInsetsCompat.Type
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.espresso.matcher.ViewMatchers.hasFocus
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.withActivity
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress("DEPRECATION") // Testing deprecated methods
@LargeTest
@RunWith(Parameterized::class)
public class WindowInsetsCompatActivityTest(private val softInputMode: Int) {
    private lateinit var scenario: ActivityScenario<WindowInsetsCompatActivity>

    @Before
    public fun setup() {
        scenario = ActivityScenario.launch(WindowInsetsCompatActivity::class.java)

        scenario.withActivity {
            // Update the soft input mode based on the test parameter
            window.setSoftInputMode(softInputMode)
        }
        onIdle()

        // Close the IME if it's open, so we start from a known scenario
        onView(withId(R.id.edittext)).perform(closeSoftKeyboard())
    }

    @After
    public fun cleanup() {
        scenario.close()
    }

    /** IME visibility is only reliable on API 23+, where we have access to the root WindowInsets */
    @Test
    @Ignore("IME tests are inherently flaky, but still useful for local testing.")
    public fun ime_viewInsets() {
        // Insets are only dispatched to views with adjustResize
        assumeSoftInputMode(SOFT_INPUT_ADJUST_RESIZE)
        // Test do not currently work on Cuttlefish
        assumeNotCuttlefish()

        val container: View = scenario.withActivity { findViewById(R.id.container) }

        // Tell the window that our view will fit system windows
        container
            .doAndAwaitNextInsets {
                scenario.onActivity { activity ->
                    WindowCompat.setDecorFitsSystemWindows(activity.window, false)
                }
            }
            .let { insets ->
                // Assert that the IME visibility is false and the insets are empty
                assertThat(insets.isVisible(Type.ime()), `is`(false))
                assertEquals(Insets.NONE, insets.getInsets(Type.ime()))
            }

        // Now open click on the EditText in the layout and ensure it has focus. The IME should
        // now be open
        container
            .doAndAwaitNextInsets {
                onView(withId(R.id.edittext)).perform(click()).check(matches(hasFocus()))
            }
            .let { insets ->
                // Assert that the IME visibility is true and the insets are not empty
                assertThat(insets.isVisible(Type.ime()), `is`(true))
                assertNotEquals(Insets.NONE, insets.getInsets(Type.ime()))
            }

        // Finally dismiss the IME
        container
            .doAndAwaitNextInsets { onView(withId(R.id.edittext)).perform(closeSoftKeyboard()) }
            .let { insets ->
                // Assert that the IME visibility is false and the insets are empty
                assertThat(insets.isVisible(Type.ime()), `is`(false))
                assertEquals(Insets.NONE, insets.getInsets(Type.ime()))
            }
    }

    /** IME visibility is only reliable on API 23+, where we have access to the root WindowInsets */
    @Test
    @Ignore("IME tests are inherently flaky, but still useful for local testing.")
    public fun ime_rootInsets() {
        // Test do not currently work on Cuttlefish
        assumeNotCuttlefish()

        val container: View = scenario.withActivity { findViewById(R.id.container) }

        // Tell the window that our view will fit system windows
        scenario.onActivity { activity ->
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        }
        ViewCompat.getRootWindowInsets(container).let { insets ->
            checkNotNull(insets)
            // Assert that the IME visibility is false and the insets are empty
            assertThat(insets.isVisible(Type.ime()), `is`(false))
            assertEquals(Insets.NONE, insets.getInsets(Type.ime()))
        }

        // Now open click on the EditText in the layout and ensure it has focus. The IME should
        // now be open
        onView(withId(R.id.edittext)).perform(click()).check(matches(hasFocus()))

        ViewCompat.getRootWindowInsets(container).let { insets ->
            checkNotNull(insets)
            // Assert that the IME visibility is true and the insets are not empty
            assertThat(insets.isVisible(Type.ime()), `is`(true))
            assertNotEquals(Insets.NONE, insets.getInsets(Type.ime()))
        }

        // Finally dismiss the IME
        onView(withId(R.id.edittext)).perform(closeSoftKeyboard())

        ViewCompat.getRootWindowInsets(container).let { insets ->
            checkNotNull(insets)
            // Assert that the IME visibility is false and the insets are empty
            assertThat(insets.isVisible(Type.ime()), `is`(false))
            assertEquals(Insets.NONE, insets.getInsets(Type.ime()))
        }
    }

    @SdkSuppress(maxSdkVersion = 29)
    @Test
    @Ignore("IME tests are inherently flaky, but still useful for local testing.")
    public fun ime_insets_cleared_on_back() {
        // Test do not currently work on Cuttlefish
        assumeNotCuttlefish()
        assumeSoftInputMode(SOFT_INPUT_ADJUST_RESIZE)

        val expectedListenerPasses = 2
        val latch = CountDownLatch(expectedListenerPasses)
        val received = AtomicReference<WindowInsetsCompat>()
        val container: View = scenario.withActivity { findViewById(R.id.container) }

        // Tell the window that our view will fit system windows
        scenario.onActivity { activity ->
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        }

        onView(withId(R.id.edittext)).perform(click()).check(matches(hasFocus()))

        // Set a listener to catch WindowInsets
        ViewCompat.setOnApplyWindowInsetsListener(container.rootView) {
            _,
            insets: WindowInsetsCompat ->
            received.set(insets)
            latch.countDown()
            WindowInsetsCompat.CONSUMED
        }

        scenario.onActivity { activity ->
            activity.startActivity(Intent(activity, activity::class.java))
        }

        Espresso.pressBackUnconditionally()
        onView(withId(R.id.edittext)).check(matches(isDisplayed()))
        assertThat(
            "OnApplyWindowListener should have been called $expectedListenerPasses times but was " +
                "called ${expectedListenerPasses - latch.count} times",
            latch.await(2, TimeUnit.SECONDS),
            `is`(true),
        )

        // Check that the IME insets is equal to 0
        val insets = received.get()
        assertEquals(0, insets.getInsets(Type.ime()).bottom)
    }

    @Test
    @Ignore("IME tests are inherently flaky, but still useful for local testing.")
    public fun systemBars_viewInsets() {
        // Insets are only dispatched to views with adjustResize
        assumeSoftInputMode(SOFT_INPUT_ADJUST_RESIZE)
        // Test do not currently work on Cuttlefish
        assumeNotCuttlefish()

        scenario.onActivity { activity ->
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        }
        val container: View = scenario.withActivity { findViewById(R.id.container) }

        // Get the current insets and check that the system bars insets are not empty
        val initialSystemBars = container.requestAndAwaitInsets().getInsets(Type.systemBars())
        assertNotEquals(Insets.NONE, initialSystemBars)

        // Now open the IME...
        container
            .doAndAwaitNextInsets {
                onView(withId(R.id.edittext)).perform(click()).check(matches(hasFocus()))
            }
            .let { insets ->
                // Assert that the systemBars() insets are not affected by the IME visibility
                // (unlike the old system window insets)
                assertEquals(initialSystemBars, insets.getInsets(Type.systemBars()))
            }
    }

    @SdkSuppress(minSdkVersion = 20)
    @Test
    public fun systemBars_hidden() {
        scenario.onActivity { activity ->
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            WindowInsetsControllerCompat(activity.window, activity.window.decorView)
                .hide(Type.systemBars())
        }
        val container: View = scenario.withActivity { findViewById(R.id.container) }

        // System bar insets should be empty and invisible because we just hide them
        val insets = container.requestAndAwaitInsets()
        assertEquals(Insets.NONE, insets.getInsets(Type.systemBars()))
        assertFalse(insets.isVisible(Type.systemBars()))
    }

    @Ignore("IME tests are inherently flaky, but still useful for local testing.")
    @Test
    public fun systemBars_rootInsets() {
        // Test do not currently work on Cuttlefish
        assumeNotCuttlefish()

        scenario.onActivity { activity ->
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        }
        val container: View = scenario.withActivity { findViewById(R.id.container) }

        // Get the current insets and check that the system bars insets are not empty
        val initialSystemBars =
            ViewCompat.getRootWindowInsets(container)?.getInsets(Type.systemBars())
        assertNotEquals(Insets.NONE, initialSystemBars)

        // Now open the IME...
        onView(withId(R.id.edittext)).perform(click()).check(matches(hasFocus()))

        ViewCompat.getRootWindowInsets(container).let { insets ->
            checkNotNull(insets)
            // Assert that the systemBars() insets are not affected by the IME visibility
            // (unlike the old system window insets)
            assertEquals(initialSystemBars, insets.getInsets(Type.systemBars()))
        }
    }

    @Test
    @Ignore("IME tests are inherently flaky, but still useful for local testing.")
    public fun rootInsets_no_ime() {
        scenario.onActivity { activity ->
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        }
        val container: View = scenario.withActivity { findViewById(R.id.container) }
        scenario.onActivity { activity ->
            WindowCompat.getInsetsController(activity.window, container).show(Type.systemBars())
        }

        // Get the current insets and check that the system bars insets are not empty
        val navigationBar =
            ViewCompat.getRootWindowInsets(container)?.getInsets(Type.navigationBars())!!
        assertNotEquals(
            "The root window insets for NavigationBars not be empty",
            Insets.NONE,
            navigationBar,
        )

        val statusBar = ViewCompat.getRootWindowInsets(container)?.getInsets(Type.statusBars())!!
        assertNotEquals("The root window insets for StatusBar not be empty", Insets.NONE, statusBar)

        // Check the same thing but for when insets are dispatched
        val insets = container.requestAndAwaitInsets()
        assertNotEquals(
            "The dispatched insets for NavigationBars insets should not be empty",
            Insets.NONE,
            insets.getInsets(Type.navigationBars()),
        )
        assertNotEquals(
            "The dispatched insets for StatusBar insets should not be empty",
            Insets.NONE,
            insets.getInsets(Type.statusBars()),
        )
    }

    private fun assumeNotCuttlefish() {
        // TODO: remove this if b/159103848 is resolved
        assumeFalse(
            "Unable to test: Cuttlefish devices default to the virtual keyboard being disabled.",
            Build.MODEL.contains("Cuttlefish", ignoreCase = true),
        )
    }

    private fun assumeSoftInputMode(mode: Int) {
        scenario.withActivity { assumeThat(window.attributes.softInputMode, `is`(mode)) }
    }

    @Test
    @SdkSuppress(minSdkVersion = 20)
    public fun equality_when_converted() {
        // Insets are only dispatched to views with adjustResize
        assumeSoftInputMode(SOFT_INPUT_ADJUST_RESIZE)
        val container: View = scenario.withActivity { findViewById(R.id.container) }
        val originalInsets: WindowInsetsCompat =
            container.doAndAwaitNextInsets {
                scenario.onActivity { activity ->
                    WindowCompat.setDecorFitsSystemWindows(activity.window, false)
                }
                onView(withId(R.id.edittext)).perform(click()).check(matches(hasFocus()))
            }
        val platformInsets = originalInsets.toWindowInsets()!!
        val convertedInsets = WindowInsetsCompat.toWindowInsetsCompat(platformInsets, container)
        assertEquals(originalInsets.getInsets(Type.ime()), convertedInsets.getInsets(Type.ime()))
        assertEquals(originalInsets, convertedInsets)
    }

    @Test
    public fun root_insets_not_null() {
        val container: View = scenario.withActivity { findViewById(R.id.container) }
        val rootWindowInsets = ViewCompat.getRootWindowInsets(container)
        assertNotNull(rootWindowInsets)
        assertNotEquals(WindowInsetsCompat.CONSUMED, rootWindowInsets)
    }

    public companion object {
        @JvmStatic
        @Parameterized.Parameters
        public fun data(): List<Array<Int>> =
            listOf(arrayOf(SOFT_INPUT_ADJUST_PAN), arrayOf(SOFT_INPUT_ADJUST_RESIZE))
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
        latch.await(5, TimeUnit.SECONDS)
    } finally {
        ViewCompat.setOnApplyWindowInsetsListener(this, null)
    }

    return received.get()
}

private fun View.requestAndAwaitInsets(): WindowInsetsCompat {
    val latch = CountDownLatch(1)
    val received = AtomicReference<WindowInsetsCompat>()

    // Set a listener to catch WindowInsets
    ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets: WindowInsetsCompat ->
        received.set(insets)
        latch.countDown()

        WindowInsetsCompat.CONSUMED
    }

    post { requestApplyInsets() }

    try {
        // Await an inset pass
        latch.await(5, TimeUnit.SECONDS)
    } finally {
        ViewCompat.setOnApplyWindowInsetsListener(this, null)
    }

    return received.get()
}
