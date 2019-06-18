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

import android.os.Bundle
import androidx.fragment.app.test.EmptyFragmentTestActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class FragmentArchLifecycleTest {

    @get:Rule
    val activityRule = ActivityTestRule(EmptyFragmentTestActivity::class.java)

    @Test
    @UiThreadTest
    fun testFragmentAdditionDuringOnStop() {
        val activity = activityRule.activity
        val fm = activity.supportFragmentManager

        val first = Fragment()
        val second = Fragment()
        fm.beginTransaction().add(first, "first").commitNow()
        first.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                fm.beginTransaction().add(second, "second").commitNow()
                first.lifecycle.removeObserver(this)
            }
        })
        activity.onSaveInstanceState(Bundle())
        assertThat(first.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(second.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(activity.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
    }

    @Test
    @UiThreadTest
    fun testNestedFragmentLifecycle() {
        val activity = activityRule.activity
        val fm = activity.supportFragmentManager

        val parent = NestedLifecycleFragmentParent()
        fm.beginTransaction()
            .add(parent, "parent")
            .commitNow()

        assertThat(parent.collectedEvents)
            .containsExactly(
                // TODO b/127528777 Properly nest ON_CREATE callbacks
                "child" to Lifecycle.Event.ON_CREATE,
                "parent" to Lifecycle.Event.ON_CREATE,

                "parent" to Lifecycle.Event.ON_START,
                "child" to Lifecycle.Event.ON_START,

                "parent" to Lifecycle.Event.ON_RESUME,
                "child" to Lifecycle.Event.ON_RESUME)
            .inOrder()

        // Now test the downward events
        parent.collectedEvents.clear()

        fm.beginTransaction()
            .remove(parent)
            .commitNow()

        assertThat(parent.collectedEvents)
            .containsExactly(
                "child" to Lifecycle.Event.ON_PAUSE,
                "parent" to Lifecycle.Event.ON_PAUSE,

                "child" to Lifecycle.Event.ON_STOP,
                "parent" to Lifecycle.Event.ON_STOP,

                "child" to Lifecycle.Event.ON_DESTROY,
                "parent" to Lifecycle.Event.ON_DESTROY)
            .inOrder()
    }
}

class NestedLifecycleFragmentParent : StrictFragment() {
    val collectedEvents = mutableListOf<Pair<String, Lifecycle.Event>>()

    init {
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            collectedEvents.add("parent" to event)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .add(StrictFragment(), "child")
                .commitNow()
        }
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        childFragment.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            collectedEvents.add("child" to event)
        })
    }
}
