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
package androidx.ui.core.test

import android.widget.FrameLayout
import androidx.compose.Composition
import androidx.compose.Recompose
import androidx.compose.Recomposer
import androidx.compose.onActive
import androidx.compose.onCommit
import androidx.compose.onDispose
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.LifecycleOwnerAmbient
import androidx.ui.core.setContent
import androidx.ui.framework.test.TestActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class WrapperTest {
    @get:Rule
    val activityTestRule = ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )
    private lateinit var activity: TestActivity

    @Before
    fun setup() {
        activity = activityTestRule.activity
    }

    @Test
    fun ensureComposeWrapperDoesntPropagateInvalidations() {
        val commitLatch = CountDownLatch(2)
        var composeWrapperCount = 0
        var innerCount = 0

        activityTestRule.runOnUiThread {
            activity.setContent {
                onCommit { composeWrapperCount++ }
                Recompose { recompose ->
                    onCommit {
                        innerCount++
                        commitLatch.countDown()
                    }
                    onActive { recompose() }
                }
            }
        }
        assertTrue(commitLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, composeWrapperCount)
        assertEquals(2, innerCount)
    }

    @Test
    fun lifecycleOwnerIsAvailableInComponentActivity() {
        val latch = CountDownLatch(1)
        var owner: LifecycleOwner? = null

        activityTestRule.runOnUiThread {
            activity.setContent {
                owner = LifecycleOwnerAmbient.current
                latch.countDown()
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertNotNull(owner)
    }

    @Test
    fun lifecycleOwnerIsAvailableWhenComposedIntoViewGroup() {
        val latch = CountDownLatch(1)
        var owner: LifecycleOwner? = null

        activityTestRule.runOnUiThread {
            val view = FrameLayout(activity)
            activity.setContentView(view)
            view.setContent(Recomposer.current()) {
                owner = LifecycleOwnerAmbient.current
                latch.countDown()
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertNotNull(owner)
    }

    @Test
    fun disposedWhenActivityDestroyed() {
        val composedLatch = CountDownLatch(1)
        val disposeLatch = CountDownLatch(1)

        val owner = RegistryOwner()

        activityTestRule.runOnUiThread {
            val view = FrameLayout(activity)
            activity.setContentView(view)
            ViewTreeLifecycleOwner.set(view, owner)
            view.setContent(Recomposer.current()) {
                onDispose {
                    disposeLatch.countDown()
                }
                composedLatch.countDown()
            }
        }

        assertTrue(composedLatch.await(1, TimeUnit.SECONDS))

        activityTestRule.runOnUiThread {
            assertEquals(1, disposeLatch.count)
            owner.registry.currentState = Lifecycle.State.DESTROYED
        }

        assertTrue(disposeLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun detachedFromLifecycleWhenDisposed() {
        val owner = RegistryOwner()
        var composition: Composition? = null
        val composedLatch = CountDownLatch(1)

        activityTestRule.runOnUiThread {
            val view = FrameLayout(activity)
            activity.setContentView(view)
            ViewTreeLifecycleOwner.set(view, owner)
            composition = view.setContent(Recomposer.current()) {
                composedLatch.countDown()
            }
        }

        assertTrue(composedLatch.await(1, TimeUnit.SECONDS))

        activityTestRule.runOnUiThread {
            assertEquals(1, owner.registry.observerCount)
            composition!!.dispose()
            assertEquals(0, owner.registry.observerCount)
        }
    }

    private class RegistryOwner : LifecycleOwner {
        var registry = LifecycleRegistry(this).also {
            it.currentState = Lifecycle.State.RESUMED
        }
        override fun getLifecycle() = registry
    }
}
