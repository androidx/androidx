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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
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
        first.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            fun onStop() {
                fm.beginTransaction().add(second, "second").commitNow()
                first.lifecycle.removeObserver(this)
            }
        })
        activity.onSaveInstanceState(Bundle())
        assertThat(first.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(second.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(activity.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
    }
}
