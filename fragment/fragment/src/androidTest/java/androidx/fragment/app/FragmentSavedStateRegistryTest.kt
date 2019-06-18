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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.LifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.testutils.RecreatedActivity
import androidx.testutils.recreate
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class FragmentSavedStateRegistryTest {

    @get:Rule
    var activityRule = ActivityTestRule(FragmentSavedStateActivity::class.java)

    private fun initializeSavedState(testFragment: Fragment = Fragment()) {
        activityRule.runOnUiThread {
            val fragmentManager = activityRule.activity.supportFragmentManager
            fragmentManager.beginTransaction().add(testFragment, FRAGMENT_TAG).commitNow()
            assertThat(fragmentManager.findFragmentByTag(FRAGMENT_TAG)).isNotNull()
            assertThat(testFragment.lifecycle.currentState.isAtLeast(CREATED)).isTrue()
            val registry = testFragment.savedStateRegistry
            val savedState = registry.consumeRestoredStateForKey(CALLBACK_KEY)
            assertThat(savedState).isNull()
            registry.registerSavedStateProvider(CALLBACK_KEY, DefaultProvider())
        }
    }

    @Test
    fun savedState() {
        initializeSavedState()
        val recreated = activityRule.recreate()
        activityRule.runOnUiThread {
            assertThat(recreated.fragment().lifecycle.currentState.isAtLeast(CREATED)).isTrue()
            checkDefaultSavedState(recreated.fragment().savedStateRegistry)
        }
    }

    @Test
    fun savedStateLateInit() {
        initializeSavedState()
        val recreated = activityRule.recreate()
        activityRule.runOnUiThread {
            recreated.fragment().lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    checkDefaultSavedState(recreated.fragment().savedStateRegistry)
                }
            })
        }
    }

    @Test
    fun savedStateEarlyRegister() {
        initializeSavedState(OnCreateCheckingFragment())
        activityRule.recreate()
    }
}

private fun checkDefaultSavedState(store: SavedStateRegistry) {
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
            checkDefaultSavedState(savedStateRegistry)
        }
    }
}

private class DefaultProvider : SavedStateRegistry.SavedStateProvider {
    override fun saveState() = Bundle().apply { putString(KEY, VALUE) }
}

private const val FRAGMENT_TAG = "TAAAG"
private const val KEY = "key"
private const val VALUE = "value"
private const val CALLBACK_KEY = "foo"
