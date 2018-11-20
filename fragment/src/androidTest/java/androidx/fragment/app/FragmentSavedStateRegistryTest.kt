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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.SavedStateRegistry
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import androidx.testutils.FragmentActivityUtils.recreateActivity
import androidx.testutils.RecreatedActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class FragmentSavedStateRegistryTest {

    @get:Rule
    var activityRule = ActivityTestRule(FragmentSavedStateActivity::class.java)

    private fun initializeSavedState(testFragment: Fragment = Fragment()) {
        activityRule.runOnUiThread {
            val fragmentManager = activityRule.activity.supportFragmentManager
            fragmentManager.beginTransaction().add(testFragment, FRAGMENT_TAG).commitNow()
            assertThat(fragmentManager.findFragmentByTag(FRAGMENT_TAG)).isNotNull()
            assertThat(testFragment.lifecycle.currentState.isAtLeast(CREATED)).isTrue()
            val registry = testFragment.bundleSavedStateRegistry
            val savedState = registry.consumeRestoredStateForKey(CALLBACK_KEY)
            assertThat(savedState).isNull()
            registry.registerSavedStateProvider(CALLBACK_KEY, DefaultProvider())
        }
    }

    @Test
    fun savedState() {
        initializeSavedState()
        val recreated = recreateActivity(activityRule, activityRule.activity)
        activityRule.runOnUiThread {
            assertThat(recreated.fragment().lifecycle.currentState.isAtLeast(CREATED)).isTrue()
            checkDefaultSavedState(recreated.fragment().bundleSavedStateRegistry)
        }
    }

    @Test
    fun savedStateLateInit() {
        initializeSavedState()
        val recreated = recreateActivity(activityRule, activityRule.activity)
        activityRule.runOnUiThread {
            recreated.fragment().lifecycle.addObserver(object : LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
                fun onResume() {
                    checkDefaultSavedState(recreated.fragment().bundleSavedStateRegistry)
                }
            })
        }
    }

    @Test
    fun savedStateEarlyRegister() {
        initializeSavedState(OnCreateCheckingFragment())
        recreateActivity(activityRule, activityRule.activity)
    }
}

private fun checkDefaultSavedState(store: SavedStateRegistry<Bundle>) {
    val savedState = store.consumeRestoredStateForKey(CALLBACK_KEY)
    assertThat(savedState).isNotNull()
    assertThat(savedState!!.getString(KEY)).isEqualTo(VALUE)
}

class FragmentSavedStateActivity : RecreatedActivity() {
    fun fragment() = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG)
        ?: throw IllegalStateException("Fragment under test wasn't found")
}

class OnCreateCheckingFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            checkDefaultSavedState(bundleSavedStateRegistry)
        }
    }
}

private class DefaultProvider : SavedStateRegistry.SavedStateProvider<Bundle> {
    override fun saveState() = Bundle().apply { putString(KEY, VALUE) }
}

private const val FRAGMENT_TAG = "TAAAG"
private const val KEY = "key"
private const val VALUE = "value"
private const val CALLBACK_KEY = "foo"
