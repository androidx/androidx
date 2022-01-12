/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.viewpager2.widget

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.adapter.FragmentStateAdapter.FragmentTransactionCallback
import androidx.viewpager2.adapter.FragmentStateAdapter.FragmentTransactionCallback.OnPostEventListener
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

@RunWith(AndroidJUnit4::class)
@LargeTest
/**
 * Integration test of [FragmentTransactionCallback] mechanism.
 *
 * The test executes various operations on a [ViewPager2] instance and inspects resulting lifecycle
 * events. The test relies on [FragmentLifecycleCallbacks] to assure the events are reflecting
 * what is happening to adapter's [Fragment]s.
 */
class FragmentTransactionCallbackTest : BaseTest() {
    @Test // TODO: split into individual tests
    fun test_populateCollection_changePage_removePage_recreateActivity_unregister() {
        setUpTest(ORIENTATION_HORIZONTAL).apply {
            // given
            val items = mutableListOf<String>()
            val adapterProvider = fragmentAdapterProviderValueId.provider(items)
            setAdapterSync(adapterProvider)
            runOnUiThreadSync { viewPager.offscreenPageLimit = 1 }

            var adapter = viewPager.adapter as FragmentStateAdapter
            var fragmentManager = activity.supportFragmentManager

            val log = RecordingLogger()
            var adapterCallback = createRecordingFragmentTransactionCallback(log)
            var lifecycleCallback = createRecordingFragmentLifecycleCallback(log)
            adapter.registerFragmentTransactionCallback(adapterCallback)
            fragmentManager.registerFragmentLifecycleCallbacks(lifecycleCallback, true)

            // when 1: the underlying collection gets populated
            val latch1 = adapter.registerFragmentAddedLatch { f -> f.name == "f1" }
            runOnUiThreadSync {
                items.addAll(stringSequence(3))
                adapter.notifyDataSetChanged()
            }
            latch1.awaitStrict(5)

            // then 1
            assertThat(
                log.consume(),
                equalTo(
                    listOf(
                        "Adapter:onFragmentPreAdded(<no-tag>)",
                        "Lifecycle:onFragmentPreAttached(f0)",
                        "Lifecycle:onFragmentAttached(f0)",
                        "Lifecycle:onFragmentPreCreated(f0)",
                        "Lifecycle:onFragmentCreated(f0)",
                        "Lifecycle:onFragmentViewCreated(f0)",
                        "Lifecycle:onFragmentActivityCreated(f0)",
                        "Lifecycle:onFragmentStarted(f0)",
                        "Adapter:onFragmentMaxLifecyclePreUpdated(f0 at RESUMED)",
                        "Lifecycle:onFragmentResumed(f0)",
                        "Adapter:onFragmentMaxLifecycleUpdated(f0 at RESUMED)",
                        "Adapter:onFragmentAdded(f0)",
                        "Adapter:onFragmentPreAdded(<no-tag>)",
                        "Lifecycle:onFragmentPreAttached(f1)",
                        "Lifecycle:onFragmentAttached(f1)",
                        "Lifecycle:onFragmentPreCreated(f1)",
                        "Lifecycle:onFragmentCreated(f1)",
                        "Lifecycle:onFragmentViewCreated(f1)",
                        "Lifecycle:onFragmentActivityCreated(f1)",
                        "Lifecycle:onFragmentStarted(f1)",
                        "Adapter:onFragmentAdded(f1)"
                    )
                )
            )

            // when 2: current item changed to next page
            val latch2 = adapter.registerMaxLifecycleUpdatedLatch { fragment, maxLifecycleState ->
                fragment.name == "f0" && maxLifecycleState == Lifecycle.State.STARTED
            }
            viewPager.setCurrentItemSync(1, true, 5, SECONDS)
            latch2.awaitStrict(5)

            // then 2
            assertThat(
                log.consume(),
                equalTo(
                    listOf(
                        "Adapter:onFragmentPreAdded(<no-tag>)",
                        "Lifecycle:onFragmentPreAttached(f2)",
                        "Lifecycle:onFragmentAttached(f2)",
                        "Lifecycle:onFragmentPreCreated(f2)",
                        "Lifecycle:onFragmentCreated(f2)",
                        "Lifecycle:onFragmentViewCreated(f2)",
                        "Lifecycle:onFragmentActivityCreated(f2)",
                        "Lifecycle:onFragmentStarted(f2)",
                        "Adapter:onFragmentAdded(f2)",
                        "Adapter:onFragmentMaxLifecyclePreUpdated(f0 at STARTED)",
                        "Adapter:onFragmentMaxLifecyclePreUpdated(f2 at STARTED)",
                        "Adapter:onFragmentMaxLifecyclePreUpdated(f1 at RESUMED)",
                        "Lifecycle:onFragmentPaused(f0)",
                        "Lifecycle:onFragmentResumed(f1)",
                        "Adapter:onFragmentMaxLifecycleUpdated(f1 at RESUMED)",
                        "Adapter:onFragmentMaxLifecycleUpdated(f2 at STARTED)",
                        "Adapter:onFragmentMaxLifecycleUpdated(f0 at STARTED)"
                    )
                )
            )

            // when 3: the last page is removed from the collection
            val latch3 = adapter.registerFragmentRemovedLatch()
            runOnUiThreadSync {
                val ix = items.size - 1
                items.removeAt(ix)
                adapter.notifyItemRemoved(ix)
            }
            latch3.awaitStrict(5)

            // then 3
            assertThat(
                log.consume(),
                equalTo(
                    listOf(
                        "Adapter:onFragmentMaxLifecyclePreUpdated(f0 at STARTED)",
                        "Adapter:onFragmentMaxLifecyclePreUpdated(f2 at STARTED)",
                        "Adapter:onFragmentMaxLifecyclePreUpdated(f1 at RESUMED)",
                        "Adapter:onFragmentMaxLifecycleUpdated(f1 at RESUMED)",
                        "Adapter:onFragmentMaxLifecycleUpdated(f2 at STARTED)",
                        "Adapter:onFragmentMaxLifecycleUpdated(f0 at STARTED)",
                        "Adapter:onFragmentPreRemoved(f2)",
                        "Lifecycle:onFragmentStopped(f2)",
                        "Lifecycle:onFragmentViewDestroyed(f2)",
                        "Lifecycle:onFragmentDestroyed(f2)",
                        "Lifecycle:onFragmentDetached(f2)",
                        "Adapter:onFragmentRemoved(<no-tag>)"
                    )
                )
            )

            // when 4: recreate activity
            recreateActivity(adapterProvider) {
                // re-register listeners after activity recreation
                adapter = (it.adapter as FragmentStateAdapter)
                fragmentManager = (it.context as FragmentActivity).supportFragmentManager
                adapterCallback = createRecordingFragmentTransactionCallback(log)
                lifecycleCallback = createRecordingFragmentLifecycleCallback(log)
                adapter.registerFragmentTransactionCallback(adapterCallback)
                fragmentManager.registerFragmentLifecycleCallbacks(lifecycleCallback, true)
            }

            // then 4
            assertThat(
                log.consume().filter { !it.contains("onFragmentSaveInstanceState") },
                equalTo(
                    listOf(
                        "Lifecycle:onFragmentPaused(f1)",
                        "Lifecycle:onFragmentStopped(f0)",
                        "Lifecycle:onFragmentStopped(f1)",
                        // "Lifecycle:onFragmentSaveInstanceState(f0)", # unstable ordering
                        // "Lifecycle:onFragmentSaveInstanceState(f1)", # unstable ordering
                        "Lifecycle:onFragmentViewDestroyed(f0)",
                        "Lifecycle:onFragmentDestroyed(f0)",
                        "Lifecycle:onFragmentDetached(f0)",
                        "Lifecycle:onFragmentViewDestroyed(f1)",
                        "Lifecycle:onFragmentDestroyed(f1)",
                        "Lifecycle:onFragmentDetached(f1)",
                        "Lifecycle:onFragmentViewCreated(f0)",
                        "Lifecycle:onFragmentActivityCreated(f0)",
                        "Lifecycle:onFragmentViewCreated(f1)",
                        "Lifecycle:onFragmentActivityCreated(f1)",
                        "Lifecycle:onFragmentStarted(f0)",
                        "Lifecycle:onFragmentStarted(f1)",
                        "Adapter:onFragmentMaxLifecyclePreUpdated(f0 at STARTED)",
                        "Adapter:onFragmentMaxLifecyclePreUpdated(f1 at RESUMED)",
                        "Adapter:onFragmentMaxLifecycleUpdated(f1 at RESUMED)",
                        "Adapter:onFragmentMaxLifecycleUpdated(f0 at STARTED)",
                        "Lifecycle:onFragmentResumed(f1)"
                    )
                )
            )

            // when 5: unregister listeners, remove all pages
            adapter.unregisterFragmentTransactionCallback(adapterCallback)
            fragmentManager.unregisterFragmentLifecycleCallbacks(lifecycleCallback)

            val latch5 = adapter.registerFragmentRemovedLatch()
            runOnUiThreadSync {
                val ix = viewPager.currentItem
                items.removeAt(ix)
                adapter.notifyItemRemoved(ix)
            }
            latch5.awaitStrict(5)

            // then 5
            assertThat(log.consume(), equalTo(emptyList()))
        }
    }

    @Test
    fun test_unregisterInsideCallback() {
        setUpTest(ORIENTATION_HORIZONTAL).apply {
            setAdapterSync(fragmentAdapterProviderValueId.provider(stringSequence(2)))
            val adapter = viewPager.adapter as FragmentStateAdapter
            val latch = CountDownLatch(1)
            adapter.registerFragmentTransactionCallback(object : FragmentTransactionCallback() {})
            adapter.registerFragmentTransactionCallback(
                object : FragmentTransactionCallback() {
                    override fun onFragmentMaxLifecyclePreUpdated(
                        fragment: Fragment,
                        maxLifecycleState: Lifecycle.State
                    ): OnPostEventListener {
                        adapter.unregisterFragmentTransactionCallback(this)
                        latch.countDown()
                        return super.onFragmentMaxLifecyclePreUpdated(fragment, maxLifecycleState)
                    }
                }
            )
            adapter.registerFragmentTransactionCallback(object : FragmentTransactionCallback() {})

            viewPager.setCurrentItemSync(1, true, 5, SECONDS)
            latch.awaitStrict(5)

            // if no crash here, then it means that the callback can unregister itself inline
        }
    }

    private fun createRecordingFragmentLifecycleCallback(
        log: RecordingLogger
    ): FragmentLifecycleCallbacks {
        return object : FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(
                fm: FragmentManager,
                f: Fragment,
                v: View,
                savedInstanceState: Bundle?
            ) {
                log.append("Lifecycle:onFragmentViewCreated(${f.name})")
            }

            override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
                log.append("Lifecycle:onFragmentStopped(${f.name})")
            }

            override fun onFragmentCreated(
                fm: FragmentManager,
                f: Fragment,
                savedInstanceState: Bundle?
            ) {
                log.append("Lifecycle:onFragmentCreated(${f.name})")
            }

            override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                log.append("Lifecycle:onFragmentResumed(${f.name})")
            }

            override fun onFragmentAttached(
                fm: FragmentManager,
                f: Fragment,
                context: android.content.Context
            ) {
                log.append("Lifecycle:onFragmentAttached(${f.name})")
            }

            override fun onFragmentPreAttached(
                fm: FragmentManager,
                f: Fragment,
                context: android.content.Context
            ) {
                log.append("Lifecycle:onFragmentPreAttached(${f.name})")
            }

            override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                log.append("Lifecycle:onFragmentDestroyed(${f.name})")
            }

            override fun onFragmentSaveInstanceState(
                fm: FragmentManager,
                f: Fragment,
                outState: Bundle
            ) {
                log.append("Lifecycle:onFragmentSaveInstanceState(${f.name})")
            }

            override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
                log.append("Lifecycle:onFragmentStarted(${f.name})")
            }

            override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
                log.append("Lifecycle:onFragmentViewDestroyed(${f.name})")
            }

            override fun onFragmentPreCreated(
                fm: FragmentManager,
                f: Fragment,
                savedInstanceState: Bundle?
            ) {
                log.append("Lifecycle:onFragmentPreCreated(${f.name})")
            }

            @Deprecated("To get a callback specifically when a Fragment activity's\n" +
                " {@link android.app.Activity#onCreate(Bundle)} is called, register a\n" +
                " {@link androidx.lifecycle.LifecycleObserver} on the Activity's\n" +
                " {@link Lifecycle} in" +
                " {@link #onFragmentAttached(FragmentManager, Fragment, Context)}, removing it\n" +
                " when it receives the {@link Lifecycle.State#CREATED} callback."
            )
            override fun onFragmentActivityCreated(
                fm: FragmentManager,
                f: Fragment,
                savedInstanceState: Bundle?
            ) {
                log.append("Lifecycle:onFragmentActivityCreated(${f.name})")
            }

            override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
                log.append("Lifecycle:onFragmentPaused(${f.name})")
            }

            override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
                log.append("Lifecycle:onFragmentDetached(${f.name})")
            }
        }
    }

    private fun createRecordingFragmentTransactionCallback(log: RecordingLogger):
        FragmentTransactionCallback {
            return object : FragmentTransactionCallback() {
                override fun onFragmentPreAdded(fragment: Fragment): OnPostEventListener {
                    log.append("Adapter:onFragmentPreAdded(${fragment.name})")
                    return OnPostEventListener {
                        log.append("Adapter:onFragmentAdded(${fragment.name})")
                    }
                }

                override fun onFragmentPreRemoved(fragment: Fragment): OnPostEventListener {
                    log.append("Adapter:onFragmentPreRemoved(${fragment.name})")
                    return OnPostEventListener {
                        log.append("Adapter:onFragmentRemoved(${fragment.name})")
                    }
                }

                override fun onFragmentMaxLifecyclePreUpdated(
                    fragment: Fragment,
                    maxLifecycleState: Lifecycle.State
                ): OnPostEventListener {
                    log.append(
                        "Adapter:onFragmentMaxLifecyclePreUpdated(${fragment.name} " +
                            "at $maxLifecycleState)"
                    )
                    return OnPostEventListener {
                        log.append(
                            "Adapter:onFragmentMaxLifecycleUpdated(${fragment.name} " +
                                "at $maxLifecycleState)"
                        )
                    }
                }
            }
        }
}

private class RecordingLogger {
    private val log = mutableListOf<String>()

    fun consume(): List<String> = log.toList().also { log.clear() }

    fun append(message: String) {
        log += message
    }
}

private val Fragment.name get(): String = this.tag ?: "<no-tag>"

private fun FragmentStateAdapter.registerFragmentAddedLatch(
    condition: (Fragment) -> Boolean
): CountDownLatch {
    val latch = CountDownLatch(1)
    registerFragmentTransactionCallback(object : FragmentTransactionCallback() {
        override fun onFragmentPreAdded(fragment: Fragment): OnPostEventListener {
            return OnPostEventListener {
                if (condition(fragment))
                    latch.countDown()
            }
        }
    })
    return latch
}

private fun FragmentStateAdapter.registerFragmentRemovedLatch(): CountDownLatch {
    val latch = CountDownLatch(1)
    registerFragmentTransactionCallback(object : FragmentTransactionCallback() {
        override fun onFragmentPreRemoved(fragment: Fragment): OnPostEventListener {
            return OnPostEventListener {
                latch.countDown()
            }
        }
    })
    return latch
}

private fun FragmentStateAdapter.registerMaxLifecycleUpdatedLatch(
    condition: (Fragment, Lifecycle.State) -> Boolean
): CountDownLatch {
    val latch = CountDownLatch(1)
    registerFragmentTransactionCallback(object : FragmentTransactionCallback() {
        override fun onFragmentMaxLifecyclePreUpdated(
            fragment: Fragment,
            maxLifecycleState: Lifecycle.State
        ): OnPostEventListener {
            return OnPostEventListener {
                if (condition(fragment, maxLifecycleState)) {
                    latch.countDown()
                }
            }
        }
    })
    return latch
}

private fun CountDownLatch.awaitStrict(timeoutSeconds: Long) {
    await(timeoutSeconds, SECONDS).also { success -> assertThat(success, equalTo(true)) }
}
