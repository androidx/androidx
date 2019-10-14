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
import androidx.savedstate.SavedStateRegistry
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ComponentActivitySavedStateTest {

    @After
    fun clear() {
        SavedStateActivity.checkEnabledInOnCreate = false
    }

    private fun ActivityScenario<SavedStateActivity>.initializeSavedState() = withActivity {
        assertThat(lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)).isTrue()
        val registry = savedStateRegistry
        val savedState = registry.consumeRestoredStateForKey(CALLBACK_KEY)
        assertThat(savedState).isNull()
        registry.registerSavedStateProvider(CALLBACK_KEY, DefaultProvider())
    }

    @Test
    @Throws(Throwable::class)
    fun savedState() {
        with(ActivityScenario.launch(SavedStateActivity::class.java)) {
            initializeSavedState()
            recreate()
            moveToState(Lifecycle.State.CREATED)
            withActivity {
                assertThat(lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)).isTrue()
                checkDefaultSavedState(savedStateRegistry)
            }
        }
    }

    @Test
    @Throws(Throwable::class)
    fun savedStateLateInit() {
        with(ActivityScenario.launch(SavedStateActivity::class.java)) {
            initializeSavedState()
            recreate()
            withActivity {
                checkDefaultSavedState(savedStateRegistry)
            }
        }
    }

    @Test
    @Throws(Throwable::class)
    fun savedStateEarlyRegister() {
        with(ActivityScenario.launch(SavedStateActivity::class.java)) {
            initializeSavedState()
            SavedStateActivity.checkEnabledInOnCreate = true
            recreate()
        }
    }
}

private class DefaultProvider : SavedStateRegistry.SavedStateProvider {
    override fun saveState() = Bundle().apply { putString(KEY, VALUE) }
}

private const val KEY = "key"
private const val VALUE = "value"
private const val CALLBACK_KEY = "foo"

private fun checkDefaultSavedState(store: SavedStateRegistry) {
    val savedState = store.consumeRestoredStateForKey(CALLBACK_KEY)
    assertThat(savedState).isNotNull()
    assertThat(savedState!!.getString(KEY)).isEqualTo(VALUE)
}

class SavedStateActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkEnabledInOnCreate) {
            checkDefaultSavedState(savedStateRegistry)
            checkEnabledInOnCreate = false
        }
    }

    companion object {
        internal var checkEnabledInOnCreate = false
    }
}
