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

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.SavedStateRegistry
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ComponentActivitySavedStateTest {

    @get:Rule
    val activityRule = ActivityTestRule<SavedStateActivity>(SavedStateActivity::class.java)

    @After
    fun clear() {
        SavedStateActivity.checkEnabledInOnCreate = false
    }

    @Throws(Throwable::class)
    private fun initializeSavedState(): SavedStateActivity {
        val activity = activityRule.activity
        activityRule.runOnUiThread {
            assertThat(activity.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)).isTrue()
            val registry = activity.bundleSavedStateRegistry
            val savedState = registry.consumeRestoredStateForKey(CALLBACK_KEY)
            assertThat(savedState).isNull()
            registry.registerSavedStateProvider(CALLBACK_KEY, DefaultProvider())
        }
        return activity
    }

    @Test
    @Throws(Throwable::class)
    fun savedState() {
        initializeSavedState()
        val recreated = recreateActivity(activityRule)
        activityRule.runOnUiThread {
            assertThat(recreated.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)).isTrue()
            checkDefaultSavedState(recreated.bundleSavedStateRegistry)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun savedStateLateInit() {
        initializeSavedState()
        val recreated = recreateActivity(activityRule)
        activityRule.runOnUiThread {
            recreated.lifecycle.addObserver(object : LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
                fun onResume() {
                    checkDefaultSavedState(recreated.bundleSavedStateRegistry)
                }
            })
        }
    }

    @Test
    @Throws(Throwable::class)
    fun savedStateEarlyRegister() {
        initializeSavedState()
        SavedStateActivity.checkEnabledInOnCreate = true
        recreateActivity(activityRule)
    }
}

private class DefaultProvider : SavedStateRegistry.SavedStateProvider<Bundle> {
    override fun saveState() = Bundle().apply { putString(KEY, VALUE) }
}

private const val KEY = "key"
private const val VALUE = "value"
private const val CALLBACK_KEY = "foo"

private fun checkDefaultSavedState(store: SavedStateRegistry<Bundle>) {
    val savedState = store.consumeRestoredStateForKey(CALLBACK_KEY)
    assertThat(savedState).isNotNull()
    assertThat(savedState!!.getString(KEY)).isEqualTo(VALUE)
}

class SavedStateActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkEnabledInOnCreate) {
            checkDefaultSavedState(bundleSavedStateRegistry)
            checkEnabledInOnCreate = false
        }
    }

    companion object {
        internal var checkEnabledInOnCreate = false
    }
}
