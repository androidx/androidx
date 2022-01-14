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
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test

class KeyEventsTestCaseWithWindowDecor : BaseKeyEventsTestCase<WindowDecorAppCompatActivity>(
    WindowDecorAppCompatActivity::class.java
) {
    @Test
    @LargeTest
    @Throws(Throwable::class)
    fun testUnhandledKeys() {
        val container = mActivityTestRule.activity!!.findViewById<ViewGroup>(R.id.test_content)
        val listener = MockUnhandledKeyListener()
        val mockView1: View = HandlerView(mActivityTestRule.activity)
        val mockView2 = HandlerView(mActivityTestRule.activity)
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        // Validity check: should work before any unhandled stuff is used. This just needs to run
        // without causing a crash
        mActivityTestRule.runOnUiThread {
            container.addView(mockView2)
            mockView2.isFocusableInTouchMode = true
            mockView2.requestFocus()
        }
        instrumentation.waitForIdleSync()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_B)
        // check that we're fine if a view consumes a down but not an up.
        mockView2.respondToDown = true
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_B)
        Assert.assertTrue(mockView2.gotDown)
        mockView2.reset()
        ViewCompat.addOnUnhandledKeyEventListener(mockView1, listener)

        // Before the view is attached, it shouldn't respond to anything
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_B)
        Assert.assertFalse(listener.fired())

        // Once attached, it should start receiving fallback events
        mActivityTestRule.runOnUiThread { container.addView(mockView1) }
        instrumentation.waitForIdleSync()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_B)
        Assert.assertTrue(listener.fired())
        listener.reset()

        // Views should still take precedence
        mockView2.respondToDown = true
        mockView2.respondToUp = true
        mActivityTestRule.runOnUiThread { mockView2.requestFocus() }
        instrumentation.waitForIdleSync()
        Assert.assertTrue(mockView2.isFocused)
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_B)
        Assert.assertTrue(mockView2.gotDown)
        Assert.assertFalse(listener.fired())

        // Still receives fallback with focused view
        mockView2.reset()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_B)
        Assert.assertTrue(listener.fired())
        listener.reset()

        // Receives events before Window.Callback (eg BACK)
        listener.mReturnVal = true
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        Assert.assertTrue(listener.fired())
        Assert.assertFalse(mActivityTestRule.activity!!.wasOnBackPressedCalled())
        listener.mReturnVal = false
        listener.reset()

        // If removed, it should not receive fallbacks anymore
        mActivityTestRule.runOnUiThread {
            ViewCompat.removeOnUnhandledKeyEventListener(
                mockView1,
                listener
            )
        }
        instrumentation.waitForIdleSync()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_B)
        Assert.assertFalse(listener.fired())
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
