/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.activity

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SmallTest
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertWithMessage
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class OnBackPressedHandlerTest {

    private var fallbackCount = 0
    lateinit var dispatcher: OnBackPressedDispatcher

    @get:Rule
    val rule = DetectLeaksAfterTestSuccess()

    @Before
    fun setup() {
        fallbackCount = 0
        dispatcher = OnBackPressedDispatcher {
            fallbackCount++
        }
    }

    @UiThreadTest
    @Test
    fun testFallbackRunnable() {
        assertWithMessage("Dispatcher should have no enabled callbacks by default")
            .that(dispatcher.hasEnabledCallbacks())
            .isFalse()
        dispatcher.onBackPressed()
        assertWithMessage("Fallback count should be incremented when there's no enabled callbacks")
            .that(fallbackCount)
            .isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testAddCallback() {
        val onBackPressedCallback = CountingOnBackPressedCallback()

        dispatcher.addCallback(onBackPressedCallback)
        assertWithMessage("Handler should return true once a callback is added")
            .that(dispatcher.hasEnabledCallbacks())
            .isTrue()
        dispatcher.onBackPressed()
        assertWithMessage("Count should be incremented after onBackPressed")
            .that(onBackPressedCallback.count)
            .isEqualTo(1)
        assertWithMessage("Fallback count should not be incremented")
            .that(fallbackCount)
            .isEqualTo(0)
    }

    @UiThreadTest
    @Test
    fun testIsEnabledWithinCallback() {
        var count = 0
        val callback = dispatcher.addCallback {
            count++
            isEnabled = false
        }
        assertWithMessage("Callback should be enabled by default")
            .that(callback.isEnabled)
            .isTrue()
        assertWithMessage("Dispatcher should have an enabled callback")
            .that(dispatcher.hasEnabledCallbacks())
            .isTrue()

        dispatcher.onBackPressed()

        assertWithMessage("Count should be incremented after onBackPressed")
            .that(count)
            .isEqualTo(1)
        assertWithMessage("Callback should be disabled after onBackPressed()")
            .that(callback.isEnabled)
            .isFalse()
        assertWithMessage("Dispatcher should have no enabled callbacks")
            .that(dispatcher.hasEnabledCallbacks())
            .isFalse()
    }

    @UiThreadTest
    @Test
    fun testRemove() {
        val onBackPressedCallback = CountingOnBackPressedCallback()

        dispatcher.addCallback(onBackPressedCallback)
        assertWithMessage("Handler should return true once a callback is added")
            .that(dispatcher.hasEnabledCallbacks())
            .isTrue()
        dispatcher.onBackPressed()
        assertWithMessage("Count should be incremented after onBackPressed")
            .that(onBackPressedCallback.count)
            .isEqualTo(1)
        assertWithMessage("Fallback count should not be incremented")
            .that(fallbackCount)
            .isEqualTo(0)

        onBackPressedCallback.remove()
        assertWithMessage(
            "Handler should return false when no OnBackPressedCallbacks " +
                "are registered"
        )
            .that(dispatcher.hasEnabledCallbacks())
            .isFalse()
        dispatcher.onBackPressed()
        // Check that the count still equals 1
        assertWithMessage("Count shouldn't be incremented after removal")
            .that(onBackPressedCallback.count)
            .isEqualTo(1)
        assertWithMessage("Fallback count should be incremented")
            .that(fallbackCount)
            .isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testRemoveInCallback() {
        val onBackPressedCallback = object : CountingOnBackPressedCallback() {
            override fun handleOnBackPressed() {
                super.handleOnBackPressed()
                remove()
            }
        }

        dispatcher.addCallback(onBackPressedCallback)
        assertWithMessage("Handler should return true once a callback is added")
            .that(dispatcher.hasEnabledCallbacks())
            .isTrue()
        dispatcher.onBackPressed()
        assertWithMessage("Count should be incremented after onBackPressed")
            .that(onBackPressedCallback.count)
            .isEqualTo(1)

        assertWithMessage(
            "Handler should return false when no OnBackPressedCallbacks " +
                "are registered"
        )
            .that(dispatcher.hasEnabledCallbacks())
            .isFalse()
        dispatcher.onBackPressed()
        // Check that the count still equals 1
        assertWithMessage("Count shouldn't be incremented after removal")
            .that(onBackPressedCallback.count)
            .isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testRemoveWithinCallback() {
        var count = 0
        dispatcher.addCallback {
            count++
            remove()
        }

        dispatcher.onBackPressed()

        assertWithMessage("Count should be incremented after onBackPressed")
            .that(count)
            .isEqualTo(1)
        assertWithMessage("Dispatcher should have no enabled callbacks after remove")
            .that(dispatcher.hasEnabledCallbacks())
            .isFalse()
    }

    @UiThreadTest
    @Test
    fun testMultipleCalls() {
        val onBackPressedCallback = CountingOnBackPressedCallback()

        dispatcher.addCallback(onBackPressedCallback)
        assertWithMessage("Handler should return true once a callback is added")
            .that(dispatcher.hasEnabledCallbacks())
            .isTrue()
        dispatcher.onBackPressed()
        dispatcher.onBackPressed()
        assertWithMessage("Count should be incremented after each onBackPressed")
            .that(onBackPressedCallback.count)
            .isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testMostRecentGetsPriority() {
        val onBackPressedCallback = CountingOnBackPressedCallback()
        val mostRecentOnBackPressedCallback = CountingOnBackPressedCallback()

        dispatcher.addCallback(onBackPressedCallback)
        dispatcher.addCallback(mostRecentOnBackPressedCallback)
        dispatcher.onBackPressed()
        assertWithMessage("Most recent callback should be incremented")
            .that(mostRecentOnBackPressedCallback.count)
            .isEqualTo(1)
        assertWithMessage("Only the most recent callback should be incremented")
            .that(onBackPressedCallback.count)
            .isEqualTo(0)
        assertWithMessage("Fallback count should not be incremented")
            .that(fallbackCount)
            .isEqualTo(0)
    }

    @UiThreadTest
    @Test
    fun testDisabledListener() {
        val onBackPressedCallback = CountingOnBackPressedCallback()
        val disabledOnBackPressedCallback = CountingOnBackPressedCallback(enabled = false)

        dispatcher.addCallback(onBackPressedCallback)
        dispatcher.addCallback(disabledOnBackPressedCallback)
        dispatcher.onBackPressed()
        assertWithMessage("Disabled callbacks should not be incremented")
            .that(disabledOnBackPressedCallback.count)
            .isEqualTo(0)
        assertWithMessage(
            "Previous callbacks should be incremented if more recent callbacks " +
                "were disabled"
        )
            .that(onBackPressedCallback.count)
            .isEqualTo(1)
        assertWithMessage("Fallback count should not be incremented")
            .that(fallbackCount)
            .isEqualTo(0)
    }

    @UiThreadTest
    @Test
    fun testPassthroughListener() {
        val onBackPressedCallback = CountingOnBackPressedCallback()
        val passThroughOnBackPressedCallback = object : CountingOnBackPressedCallback() {
            override fun handleOnBackPressed() {
                super.handleOnBackPressed()
                // Trigger the next listener
                isEnabled = false
                dispatcher.onBackPressed()
            }
        }

        dispatcher.addCallback(onBackPressedCallback)
        dispatcher.addCallback(passThroughOnBackPressedCallback)
        dispatcher.onBackPressed()
        assertWithMessage("Most recent callback should be incremented")
            .that(passThroughOnBackPressedCallback.count)
            .isEqualTo(1)
        assertWithMessage(
            "Previous callbacks should be incremented if more recent callbacks " +
                "disabled itself and called onBackPressed()"
        )
            .that(onBackPressedCallback.count)
            .isEqualTo(1)
        assertWithMessage("Fallback count should not be incremented")
            .that(fallbackCount)
            .isEqualTo(0)
    }

    @UiThreadTest
    @Test
    fun testLifecycleCallback() {
        val onBackPressedCallback = CountingOnBackPressedCallback()
        val lifecycleOnBackPressedCallback = CountingOnBackPressedCallback()
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)

        dispatcher.addCallback(onBackPressedCallback)
        dispatcher.addCallback(lifecycleOwner, lifecycleOnBackPressedCallback)
        dispatcher.onBackPressed()
        assertWithMessage("Non-started callbacks shouldn't have their count incremented")
            .that(lifecycleOnBackPressedCallback.count)
            .isEqualTo(0)
        assertWithMessage(
            "Previous callbacks should be incremented if more recent callbacks " +
                "aren't started"
        )
            .that(onBackPressedCallback.count)
            .isEqualTo(1)

        // Now start the Lifecycle
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        dispatcher.onBackPressed()
        assertWithMessage("Once the callbacks is started, the count should increment")
            .that(lifecycleOnBackPressedCallback.count)
            .isEqualTo(1)
        assertWithMessage("Only the most recent callback should be incremented")
            .that(onBackPressedCallback.count)
            .isEqualTo(1)

        // Now stop the Lifecycle
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        dispatcher.onBackPressed()
        assertWithMessage("Non-started callbacks shouldn't have their count incremented")
            .that(lifecycleOnBackPressedCallback.count)
            .isEqualTo(1)
        assertWithMessage(
            "Previous callbacks should be incremented if more recent callbacks " +
                "aren't started"
        )
            .that(onBackPressedCallback.count)
            .isEqualTo(2)

        // Now destroy the Lifecycle
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        dispatcher.onBackPressed()
        assertWithMessage("Non-started callbacks shouldn't have their count incremented")
            .that(lifecycleOnBackPressedCallback.count)
            .isEqualTo(1)
        assertWithMessage(
            "Previous callbacks should be incremented if more recent callbacks " +
                "aren't started"
        )
            .that(onBackPressedCallback.count)
            .isEqualTo(3)
    }

    @UiThreadTest
    @Test
    fun testLifecycleRemoveInCallback() {
        val onBackPressedCallback = object : CountingOnBackPressedCallback() {
            override fun handleOnBackPressed() {
                super.handleOnBackPressed()
                remove()
            }
        }
        val lifecycleOwner = TestLifecycleOwner()

        dispatcher.addCallback(lifecycleOwner, onBackPressedCallback)
        assertWithMessage("Handler should return true once a callback is added")
            .that(dispatcher.hasEnabledCallbacks())
            .isTrue()
        dispatcher.onBackPressed()
        assertWithMessage("Count should be incremented after onBackPressed")
            .that(onBackPressedCallback.count)
            .isEqualTo(1)

        assertWithMessage(
            "Handler should return false when no OnBackPressedCallbacks " +
                "are registered"
        )
            .that(dispatcher.hasEnabledCallbacks())
            .isFalse()
        dispatcher.onBackPressed()
        // Check that the count still equals 1
        assertWithMessage("Count shouldn't be incremented after removal")
            .that(onBackPressedCallback.count)
            .isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testLifecycleCallbackDestroyed() {
        val onBackPressedCallback = CountingOnBackPressedCallback()
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.CREATED)

        dispatcher.addCallback(lifecycleOwner, onBackPressedCallback)
        assertWithMessage("Non-started callbacks shouldn't appear as an enabled dispatcher")
            .that(dispatcher.hasEnabledCallbacks())
            .isFalse()

        // Now destroy the Lifecycle
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertWithMessage("Destroyed callbacks shouldn't appear as an enabled dispatcher")
            .that(dispatcher.hasEnabledCallbacks())
            .isFalse()

        // Now start the Lifecycle - this wouldn't happen in a real Lifecycle since DESTROYED
        // is terminal but serves as a good test to make sure the Observer is cleaned up
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertWithMessage(
            "Previously destroyed callbacks shouldn't appear as an enabled " +
                "dispatcher"
        )
            .that(dispatcher.hasEnabledCallbacks())
            .isFalse()
    }

    @UiThreadTest
    @Test
    fun testLifecycleCallback_whenDestroyed() {
        val lifecycleOnBackPressedCallback = CountingOnBackPressedCallback()

        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
        lifecycleOwner.lifecycle.currentState = Lifecycle.State.DESTROYED

        dispatcher.addCallback(lifecycleOwner, lifecycleOnBackPressedCallback)

        assertWithMessage(
            "Handler should return false when no OnBackPressedCallbacks " +
                "are registered"
        )
            .that(dispatcher.hasEnabledCallbacks())
            .isFalse()

        // Now start the Lifecycle - this wouldn't happen in a real Lifecycle since DESTROYED
        // is terminal but serves as a good test to make sure no lingering Observer exists
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertWithMessage(
            "Previously destroyed callbacks shouldn't appear as an enabled " +
                "dispatcher"
        )
            .that(dispatcher.hasEnabledCallbacks())
            .isFalse()
    }

    /**
     * Test to ensure that manually calling [ComponentActivity.onBackPressed] after
     * [ComponentActivity.onSaveInstanceState] does not cause an exception.
     */
    @LargeTest
    @Test
    fun testCallOnBackPressedWhenStopped() {
       withUse(ActivityScenario.launch(ContentViewActivity::class.java)) {
            val realDispatcher = withActivity { onBackPressedDispatcher }
            moveToState(Lifecycle.State.CREATED)
            withActivity { realDispatcher.onBackPressed() }
        }
    }

    @Test
    fun testOnHasEnabledCallbacks() {
        var reportedHasEnabledCallbacks = false
        var reportCount = 0
        val dispatcher = OnBackPressedDispatcher(
            fallbackOnBackPressed = null,
            onHasEnabledCallbacksChanged = {
                reportedHasEnabledCallbacks = it
                reportCount++
            }
        )

        assertWithMessage("initial reportCount")
            .that(reportCount)
            .isEqualTo(0)
        assertWithMessage("initial reportedHasEnabledCallbacks")
            .that(reportedHasEnabledCallbacks)
            .isFalse()

        val callbackA = dispatcher.addCallback(enabled = false) {}

        assertWithMessage("reportCount")
            .that(reportCount)
            .isEqualTo(0)
        assertWithMessage("reportedHasEnabledCallbacks")
            .that(reportedHasEnabledCallbacks)
            .isFalse()

        callbackA.isEnabled = true

        assertWithMessage("reportCount")
            .that(reportCount)
            .isEqualTo(1)
        assertWithMessage("reportedHasEnabledCallbacks")
            .that(reportedHasEnabledCallbacks)
            .isTrue()

        val callbackB = dispatcher.addCallback {}

        assertWithMessage("reportCount")
            .that(reportCount)
            .isEqualTo(1)
        assertWithMessage("reportedHasEnabledCallbacks")
            .that(reportedHasEnabledCallbacks)
            .isTrue()

        callbackA.remove()

        assertWithMessage("reportCount")
            .that(reportCount)
            .isEqualTo(1)
        assertWithMessage("reportedHasEnabledCallbacks")
            .that(reportedHasEnabledCallbacks)
            .isTrue()

        callbackB.remove()

        assertWithMessage("reportCount")
            .that(reportCount)
            .isEqualTo(2)
        assertWithMessage("reportedHasEnabledCallbacks")
            .that(reportedHasEnabledCallbacks)
            .isFalse()

        dispatcher.addCallback {}

        assertWithMessage("reportCount")
            .that(reportCount)
            .isEqualTo(3)
        assertWithMessage("reportedHasEnabledCallbacks")
            .that(reportedHasEnabledCallbacks)
            .isTrue()
    }
}

open class CountingOnBackPressedCallback(
    enabled: Boolean = true
) : OnBackPressedCallback(enabled) {
    var count = 0

    override fun handleOnBackPressed() {
        count++
    }
}
