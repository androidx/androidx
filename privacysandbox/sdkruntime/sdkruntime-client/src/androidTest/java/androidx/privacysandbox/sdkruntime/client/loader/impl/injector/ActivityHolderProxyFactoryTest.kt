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

package androidx.privacysandbox.sdkruntime.client.loader.impl.injector

import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Lifecycle
import androidx.privacysandbox.sdkruntime.client.EmptyActivity
import androidx.privacysandbox.sdkruntime.client.TestActivityHolder
import androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder
import androidx.test.core.app.ActivityScenario
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class ActivityHolderProxyFactoryTest {

    private lateinit var factory: ActivityHolderProxyFactory

    @Before
    fun setUp() {
        factory = ActivityHolderProxyFactory.createFor(javaClass.classLoader!!)
    }

    @Test
    fun createProxyFor_RetrievesActivityFromOriginalActivityHolder() {
        with(ActivityScenario.launch(EmptyActivity::class.java)) {
            withActivity {
                val activityHolder = TestActivityHolder(this)
                val proxy = factory.createProxyFor(activityHolder) as ActivityHolder
                assertThat(proxy.getActivity()).isSameInstanceAs(activityHolder.getActivity())
            }
        }
    }

    @Suppress("ReplaceCallWithBinaryOperator") // Explicitly testing equals on proxy
    @Test
    fun createProxyFor_CreatesProxyWithValidEqualsAndHashCode() {
        with(ActivityScenario.launch(EmptyActivity::class.java)) {
            withActivity {
                val activityHolder = TestActivityHolder(this)
                val proxy = factory.createProxyFor(activityHolder)
                assertThat(proxy.equals(proxy)).isTrue()
                assertThat(proxy.hashCode()).isEqualTo(proxy.hashCode())
                assertThat(proxy.toString()).isEqualTo(proxy.toString())
            }
        }
    }

    @Test
    fun getOnBackPressedDispatcher_ProxyBackPressedFromSourceDispatcher() {
        with(ActivityScenario.launch(EmptyActivity::class.java)) {
            withActivity {
                val activityHolder = TestActivityHolder(this)
                val proxy = factory.createProxyFor(activityHolder) as ActivityHolder
                val callback = CountingOnBackPressedCallback()

                val sourceDispatcher = activityHolder.getOnBackPressedDispatcher()
                val proxyDispatcher = proxy.getOnBackPressedDispatcher()

                proxyDispatcher.addCallback(callback)
                sourceDispatcher.onBackPressed()
                assertThat(callback.count).isEqualTo(1)

                callback.remove()
                sourceDispatcher.onBackPressed()
                assertThat(callback.count).isEqualTo(1)
            }
        }
    }

    @Test
    fun getOnBackPressedDispatcher_EnableSourceCallbackOnlyWhenEnabledCallbackAddedToProxy() {
        with(ActivityScenario.launch(EmptyActivity::class.java)) {
            withActivity {
                val activityHolder = TestActivityHolder(this)
                val proxy = factory.createProxyFor(activityHolder) as ActivityHolder
                val callback = CountingOnBackPressedCallback()

                val sourceDispatcher = activityHolder.getOnBackPressedDispatcher()
                val proxyDispatcher = proxy.getOnBackPressedDispatcher()

                assertThat(sourceDispatcher.hasEnabledCallbacks()).isFalse()

                proxyDispatcher.addCallback(callback)
                assertThat(sourceDispatcher.hasEnabledCallbacks()).isTrue()

                callback.isEnabled = false
                assertThat(sourceDispatcher.hasEnabledCallbacks()).isFalse()

                callback.isEnabled = true
                assertThat(sourceDispatcher.hasEnabledCallbacks()).isTrue()

                callback.remove()
                assertThat(sourceDispatcher.hasEnabledCallbacks()).isFalse()
            }
        }
    }

    @Test
    fun getLifecycle_ProxyLifecycleEventsFromSourceActivityHolder() {
        with(ActivityScenario.launch(EmptyActivity::class.java)) {
            withActivity {
                val sourceActivityHolder = TestActivityHolder(this)
                val proxy = factory.createProxyFor(sourceActivityHolder) as ActivityHolder
                for (event in Lifecycle.Event.values().filter { it != Lifecycle.Event.ON_ANY }) {
                    sourceActivityHolder.lifecycleRegistry.handleLifecycleEvent(event)
                    assertThat(proxy.lifecycle.currentState).isEqualTo(event.targetState)
                }
            }
        }
    }

    private class CountingOnBackPressedCallback : OnBackPressedCallback(true) {
        var count = 0

        override fun handleOnBackPressed() {
            count++
        }
    }
}
