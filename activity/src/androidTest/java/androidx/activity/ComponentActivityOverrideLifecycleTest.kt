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

package androidx.activity

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ComponentActivityOverrideLifecycleTest {

    @get:Rule
    val activityRule = ActivityTestRule(LazyOverrideLifecycleComponentActivity::class.java)

    @UiThreadTest
    @Test
    fun testEagerOverride() {
        try {
            EagerOverrideLifecycleComponentActivity()
            fail("Constructor for ComponentActivity using a field initializer should throw")
        } catch (e: IllegalStateException) {
            // expected
        }
    }

    @UiThreadTest
    @Test
    fun testOverrideLifecycle() {
        val activity = activityRule.activity

        assertThat(activity.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
    }
}

class EagerOverrideLifecycleComponentActivity : ComponentActivity() {

    val overrideLifecycle = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle {
        return overrideLifecycle
    }
}

class LazyOverrideLifecycleComponentActivity : ComponentActivity() {
    private var overrideLifecycle: LifecycleRegistry? = null

    override fun getLifecycle(): Lifecycle {
        return overrideLifecycle ?: LifecycleRegistry(this).also {
            overrideLifecycle = it
        }
    }
}
