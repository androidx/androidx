/*
 * Copyright 2022 The Android Open Source Project
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

import android.graphics.PixelFormat
import android.view.KeyEvent
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.view.WindowCallbackWrapper
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.TestCase.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@MediumTest
@RunWith(AndroidJUnit4::class)
class AppCompatWindowCallbackWrapperTest {

    /**
     * Regression test for b/173628052 where window callbacks are not dispatched to custom wrappers.
     */
    @Test
    fun testDispatchKeyEventToWrapper() {
        ActivityScenario.launch(AppCompatInflaterDefaultActivity::class.java).let { scenario ->
            val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MENU)
            var callback: Window.Callback? = null
            scenario.onActivity { activity ->
                callback = spy(WindowCallbackWrapper(activity.window.callback))
                activity.window.callback = callback
            }

            // Inject a key event.
            InstrumentationRegistry.getInstrumentation().sendKeySync(keyEvent)

            // Make sure we got the expected callbacks.
            assertNotNull(callback)
            verify(callback, times(2))!!.dispatchKeyEvent(argThat(SimpleKeyEventMatcher(keyEvent)))
        }
    }

    /**
     * Regression test for b/173628052 where window callbacks are not dispatched to custom wrappers.
     */
    @Test
    fun testOnContentChangedToWrapper() {
        ActivityScenario.launch(AppCompatInflaterDefaultActivity::class.java).let { scenario ->
            var callback: Window.Callback? = null
            scenario.onActivity { activity ->
                callback = spy(WindowCallbackWrapper(activity.window.callback))
                activity.window.callback = callback

                // Force a content change event.
                activity.setContentView(android.R.layout.two_line_list_item)
            }

            // Make sure we got the expected callback.
            assertNotNull(callback)
            verify(callback, times(1))!!.onContentChanged()
        }
    }

    /**
     * Regression test for b/173628052 where window callbacks are not dispatched to custom wrappers.
     */
    @Test
    fun testOnPanelClosedWrapper() {
        ActivityScenario.launch(AppCompatInflaterDefaultActivity::class.java).let { scenario ->
            var callback: Window.Callback? = null
            scenario.onActivity { activity ->
                callback = spy(WindowCallbackWrapper(activity.window.callback))
                activity.window.callback = callback

                // Set up a fake application sub-panel, then close it. This is messy, but it's
                // likely more reliable than displaying a real panel.
                val v = FrameLayout(activity)
                val st =
                    AppCompatDelegateImpl.PanelFeatureState(Window.FEATURE_CONTEXT_MENU).apply {
                        isOpen = true
                        decorView = v
                    }
                activity.windowManager.addView(
                    v,
                    WindowManager.LayoutParams(
                        0,
                        0,
                        WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL,
                        0,
                        PixelFormat.TRANSLUCENT,
                    ),
                )
                (activity.delegate as? AppCompatDelegateImpl)?.closePanel(st, true)
            }

            // Make sure we got the expected callback.
            assertNotNull(callback)
            verify(callback, times(1))!!.onPanelClosed(anyInt(), any())
        }
    }
}

class SimpleKeyEventMatcher(val keyEvent: KeyEvent) : ArgumentMatcher<KeyEvent> {
    override fun matches(argument: KeyEvent?): Boolean {
        return argument != null &&
            argument.action == keyEvent.action &&
            argument.keyCode == keyEvent.keyCode
    }
}
