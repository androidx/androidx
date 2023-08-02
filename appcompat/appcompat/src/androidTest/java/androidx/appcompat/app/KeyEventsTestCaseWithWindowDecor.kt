/*
 * Copyright (C) 2015 The Android Open Source Project
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
package androidx.appcompat.app

import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.test.R
import androidx.core.view.ViewCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.filters.LargeTest
import androidx.testutils.PollingCheck
import androidx.testutils.withActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyEventsTestCaseWithWindowDecor : BaseKeyEventsTestCase<WindowDecorAppCompatActivity>(
    WindowDecorAppCompatActivity::class.java
) {
    @Test
    @LargeTest
    @Throws(Throwable::class)
    fun testUnhandledKeys() {
        with(ActivityScenario.launch(WindowDecorAppCompatActivity::class.java)) {
            val listener = MockUnhandledKeyListener()
            val mockView1: View = withActivity { HandlerView(this) }
            val mockView2 = withActivity { HandlerView(this) }

            // Validity check: should work before any unhandled stuff is used. This just needs to run
            // without causing a crash
            withActivity {
                val container = findViewById<ViewGroup>(R.id.test_content)
                container.addView(mockView2)
                mockView2.isFocusableInTouchMode = true
                mockView2.requestFocus()
            }

            PollingCheck.waitFor { mockView2.hasFocus() }

            onView(isRoot()).perform(pressKey(KeyEvent.KEYCODE_B))
            // check that we're fine if a view consumes a down but not an up.
            mockView2.respondToDown = true
            onView(isRoot()).perform(pressKey(KeyEvent.KEYCODE_B))
            assertTrue(mockView2.gotDown)
            mockView2.reset()

            ViewCompat.addOnUnhandledKeyEventListener(mockView1, listener)

            // Before the view is attached, it shouldn't respond to anything
            onView(isRoot()).perform(pressKey(KeyEvent.KEYCODE_B))
            assertFalse(listener.fired())

            // Once attached, it should start receiving fallback events
            withActivity {
                val container = findViewById<ViewGroup>(R.id.test_content)
                container.addView(mockView1)
            }
            onView(isRoot()).perform(pressKey(KeyEvent.KEYCODE_B))
            assertTrue(listener.fired())
            listener.reset()

            // Views should still take precedence
            mockView2.respondToDown = true
            mockView2.respondToUp = true
            withActivity { mockView2.requestFocus() }
            assertTrue(mockView2.isFocused)
            onView(isRoot()).perform(pressKey(KeyEvent.KEYCODE_B))
            assertTrue(mockView2.gotDown)
            assertFalse(listener.fired())

            // Still receives fallback with focused view
            mockView2.reset()
            onView(isRoot()).perform(pressKey(KeyEvent.KEYCODE_B))
            assertTrue(listener.fired())
            listener.reset()

            // Receives events before Window.Callback (eg BACK)
            listener.mReturnVal = true
            onView(isRoot()).perform(pressKey(KeyEvent.KEYCODE_BACK))
            assertTrue(listener.fired())
            assertFalse(withActivity { wasOnBackPressedCalled() })
            listener.mReturnVal = false
            listener.reset()

            // If removed, it should not receive fallbacks anymore
            withActivity {
                ViewCompat.removeOnUnhandledKeyEventListener(mockView1, listener)
            }
            onView(isRoot()).perform(pressKey(KeyEvent.KEYCODE_B))
            assertFalse(listener.fired())
        }
    }

    private class MockUnhandledKeyListener : ViewCompat.OnUnhandledKeyEventListenerCompat {
        var mLastView: View? = null
        var mGotUp = false
        var mReturnVal = false
        override fun onUnhandledKeyEvent(v: View, event: KeyEvent): Boolean {
            if (event.action == KeyEvent.ACTION_DOWN) {
                mLastView = v
            } else if (event.action == KeyEvent.ACTION_UP) {
                mGotUp = true
            }
            return mReturnVal
        }

        fun reset() {
            mLastView = null
            mGotUp = false
        }

        fun fired(): Boolean {
            return mLastView != null && mGotUp
        }
    }

    /**
     * A View which can be set to consume or not consume key events.
     */
    private class HandlerView(ctx: Context?) : View(ctx) {
        var respondToDown = false
        var respondToUp = false
        var gotDown = false
        fun reset() {
            gotDown = false
            respondToDown = gotDown
            respondToUp = respondToDown
        }

        override fun dispatchKeyEvent(evt: KeyEvent): Boolean {
            if (evt.action == KeyEvent.ACTION_DOWN && respondToDown) {
                gotDown = true
                return true
            } else if (evt.action == KeyEvent.ACTION_UP && respondToUp) {
                return true
            }
            return super.dispatchKeyEvent(evt)
        }
    }
}
