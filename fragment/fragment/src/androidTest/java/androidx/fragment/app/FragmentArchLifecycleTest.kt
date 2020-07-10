/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.fragment.app

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.test.EmptyFragmentTestActivity
import androidx.fragment.test.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class FragmentArchLifecycleTest {

    @Test
    fun testFragmentAdditionDuringOnStop() {
        with(ActivityScenario.launch(EmptyFragmentTestActivity::class.java)) {
            val fm = withActivity { supportFragmentManager }
            val activityLifecycle = withActivity { lifecycle }

            val first = Fragment()
            val second = Fragment()
            fm.beginTransaction().add(first, "first").commit()
            executePendingTransactions()
            first.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_STOP) {
                        fm.beginTransaction().add(second, "second").commitNow()
                        first.lifecycle.removeObserver(this)
                    }
                }
            })
            onActivity {
                it.onSaveInstanceState(Bundle())
            }
            assertThat(first.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
            assertThat(second.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
            assertThat(activityLifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        }
    }

    @Test
    fun testFragmentAdditionDuringOnStopViewLifecycle() {
        with(ActivityScenario.launch(EmptyFragmentTestActivity::class.java)) {
            val fm = withActivity { supportFragmentManager }
            val activityLifecycle = withActivity { lifecycle }

            val first = StrictViewFragment()
            val second = StrictFragment()
            fm.beginTransaction().add(android.R.id.content, first).commit()
            executePendingTransactions()
            first.viewLifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_STOP) {
                        fm.beginTransaction().add(second, "second").commitNow()
                        first.viewLifecycleOwner.lifecycle.removeObserver(this)
                    }
                }
            })
            onActivity {
                it.onSaveInstanceState(Bundle())
            }
            assertThat(first.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
            assertThat(first.viewLifecycleOwner.lifecycle.currentState)
                .isEqualTo(Lifecycle.State.CREATED)
            assertThat(second.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
            assertThat(activityLifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        }
    }

    @Test
    fun testNestedFragmentLifecycle() {
        with(ActivityScenario.launch(FragmentArchLifecycleActivity::class.java)) {

            val collectedEvents = withActivity { collectedEvents }

            assertThat(collectedEvents)
                .containsExactly(
                    "activity" to Lifecycle.Event.ON_CREATE,
                    // TODO b/127528777 Properly nest ON_CREATE callbacks
                    "child" to Lifecycle.Event.ON_CREATE,
                    "parent" to Lifecycle.Event.ON_CREATE,

                    "activity" to Lifecycle.Event.ON_START,
                    "parent" to Lifecycle.Event.ON_START,
                    "child" to Lifecycle.Event.ON_START,

                    "activity" to Lifecycle.Event.ON_RESUME,
                    "post_activity" to Lifecycle.Event.ON_RESUME,
                    "parent" to Lifecycle.Event.ON_RESUME,
                    "child" to Lifecycle.Event.ON_RESUME
                )
                .inOrder()

            // Now test the downward events
            collectedEvents.clear()

            moveToState(Lifecycle.State.DESTROYED)

            assertThat(collectedEvents)
                .containsExactly(
                    "child" to Lifecycle.Event.ON_PAUSE,
                    "parent" to Lifecycle.Event.ON_PAUSE,
                    "activity" to Lifecycle.Event.ON_PAUSE,

                    "child" to Lifecycle.Event.ON_STOP,
                    "parent" to Lifecycle.Event.ON_STOP,
                    "activity" to Lifecycle.Event.ON_STOP,

                    "child" to Lifecycle.Event.ON_DESTROY,
                    "parent" to Lifecycle.Event.ON_DESTROY,
                    "activity" to Lifecycle.Event.ON_DESTROY
                )
                .inOrder()
        }
    }

    @Test
    fun testNestedFragmentLifecycleOnRemove() {
        with(ActivityScenario.launch(FragmentArchLifecycleActivity::class.java)) {

            val fm = withActivity { supportFragmentManager }
            val parent = withActivity {
                fm.findFragmentById(R.id.content) as NestedLifecycleFragmentParent
            }
            val collectedEvents = withActivity { collectedEvents }

            assertThat(collectedEvents)
                .containsExactly(
                    "activity" to Lifecycle.Event.ON_CREATE,
                    // TODO b/127528777 Properly nest ON_CREATE callbacks
                    "child" to Lifecycle.Event.ON_CREATE,
                    "parent" to Lifecycle.Event.ON_CREATE,

                    "activity" to Lifecycle.Event.ON_START,
                    "parent" to Lifecycle.Event.ON_START,
                    "child" to Lifecycle.Event.ON_START,

                    "activity" to Lifecycle.Event.ON_RESUME,
                    "post_activity" to Lifecycle.Event.ON_RESUME,
                    "parent" to Lifecycle.Event.ON_RESUME,
                    "child" to Lifecycle.Event.ON_RESUME
                )
                .inOrder()

            // Now test the downward events
            collectedEvents.clear()

            fm.beginTransaction()
                .remove(parent)
                .commit()
            executePendingTransactions()

            assertThat(collectedEvents)
                .containsExactly(
                    "child" to Lifecycle.Event.ON_PAUSE,
                    "parent" to Lifecycle.Event.ON_PAUSE,

                    "child" to Lifecycle.Event.ON_STOP,
                    "parent" to Lifecycle.Event.ON_STOP,

                    "child" to Lifecycle.Event.ON_DESTROY,
                    "parent" to Lifecycle.Event.ON_DESTROY
                )
                .inOrder()
        }
    }

    @Test
    fun testOverriddenLifecycleFragment() {
        with(ActivityScenario.launch(EmptyFragmentTestActivity::class.java)) {
            val fm = withActivity { supportFragmentManager }

            val fragment = OverriddenLifecycleFragment()
            fm.beginTransaction().add(fragment, "lifecycle").commit()
            executePendingTransactions()
            // This should not crash or hang
            moveToState(Lifecycle.State.DESTROYED)
        }
    }
}

class FragmentArchLifecycleActivity : FragmentActivity(R.layout.activity_content) {
    val collectedEvents = mutableListOf<Pair<String, Lifecycle.Event>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        collectedEvents.add("activity" to Lifecycle.Event.ON_CREATE)
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.content, NestedLifecycleFragmentParent())
                .commitNow()
        }
    }

    override fun onStart() {
        collectedEvents.add("activity" to Lifecycle.Event.ON_START)
        super.onStart()
    }

    override fun onResume() {
        collectedEvents.add("activity" to Lifecycle.Event.ON_RESUME)
        super.onResume()
    }

    override fun onPostResume() {
        collectedEvents.add("post_activity" to Lifecycle.Event.ON_RESUME)
        super.onPostResume()
    }

    override fun onPause() {
        super.onPause()
        collectedEvents.add("activity" to Lifecycle.Event.ON_PAUSE)
    }

    override fun onStop() {
        super.onStop()
        collectedEvents.add("activity" to Lifecycle.Event.ON_STOP)
    }

    override fun onDestroy() {
        super.onDestroy()
        collectedEvents.add("activity" to Lifecycle.Event.ON_DESTROY)
    }
}

class NestedLifecycleFragmentParent : StrictFragment(), FragmentOnAttachListener {
    private val archLifecycleActivity by lazy {
        requireActivity() as FragmentArchLifecycleActivity
    }

    init {
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            archLifecycleActivity.collectedEvents.add("parent" to event)
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        childFragmentManager.addFragmentOnAttachListener(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .add(StrictFragment(), "child")
                .commitNow()
        }
    }

    override fun onAttachFragment(fragmentManager: FragmentManager, childFragment: Fragment) {
        childFragment.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            archLifecycleActivity.collectedEvents.add("child" to event)
        })
    }
}

class OverriddenLifecycleFragment : Fragment() {

    private val lifecycleRegistry = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onStart() {
        super.onStart()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onResume() {
        super.onResume()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onPause() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        super.onPause()
    }

    override fun onStop() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onStop()
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }
}
