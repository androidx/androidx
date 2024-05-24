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
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertThat
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ComponentActivitySavedStateTest {

    @get:Rule val rule = DetectLeaksAfterTestSuccess()

    @After
    fun clear() {
        SavedStateActivity.checkEnabledInOnCreate = false
    }

    private fun ActivityScenario<SavedStateActivity>.initializeSavedState() = withActivity {
        val isLifecycleCreated = lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)
        val registry = savedStateRegistry
        val savedState = registry.consumeRestoredStateForKey(CALLBACK_KEY)
        val savedStateIsNull = savedState == null
        registry.registerSavedStateProvider(CALLBACK_KEY, DefaultProvider())
        isLifecycleCreated && savedStateIsNull
    }

    @Test
    @Throws(Throwable::class)
    fun savedState() {
        withUse(ActivityScenario.launch(SavedStateActivity::class.java)) {
            assertThat(initializeSavedState()).isTrue()
            recreate()
            moveToState(Lifecycle.State.CREATED)
            val lifecycle = withActivity { lifecycle }
            assertThat(lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)).isTrue()

            val registry = withActivity { savedStateRegistry }
            assertThat(hasDefaultSavedState(registry)).isTrue()
        }
    }

    @Test
    @Throws(Throwable::class)
    fun savedStateLateInit() {
        withUse(ActivityScenario.launch(SavedStateActivity::class.java)) {
            assertThat(initializeSavedState()).isTrue()
            recreate()
            val registry = withActivity { savedStateRegistry }
            assertThat(hasDefaultSavedState(registry)).isTrue()
        }
    }

    @Test
    @Throws(Throwable::class)
    fun savedStateEarlyRegisterOnCreate() {
        withUse(ActivityScenario.launch(SavedStateActivity::class.java)) {
            assertThat(initializeSavedState()).isTrue()
            SavedStateActivity.checkEnabledInOnCreate = true
            recreate()
        }
    }

    @Test
    @Throws(Throwable::class)
    fun savedStateEarlyRegisterOnContextAvailable() {
        withUse(ActivityScenario.launch(SavedStateActivity::class.java)) {
            assertThat(initializeSavedState()).isTrue()
            SavedStateActivity.checkEnabledInOnContextAvailable = true
            recreate()
        }
    }

    @Test
    @Throws(Throwable::class)
    fun savedStateEarlyRegisterInitAddedLifecycleObserver() {
        withUse(ActivityScenario.launch(SavedStateActivity::class.java)) {
            assertThat(initializeSavedState()).isTrue()
            SavedStateActivity.checkEnabledInInitAddedLifecycleObserver = true
            recreate()
        }
    }
}

class DefaultProvider : SavedStateRegistry.SavedStateProvider {
    override fun saveState() = Bundle().apply { putString(KEY, VALUE) }
}

private const val KEY = "key"
private const val VALUE = "value"
const val CALLBACK_KEY = "foo"

fun hasDefaultSavedState(store: SavedStateRegistry): Boolean {
    val savedState = store.consumeRestoredStateForKey(CALLBACK_KEY)
    return savedState?.getString(KEY) == VALUE
}

class SavedStateActivity : ComponentActivity() {

    init {
        addOnContextAvailableListener {
            if (checkEnabledInOnContextAvailable && hasDefaultSavedState(savedStateRegistry)) {
                checkEnabledInOnContextAvailable = false
            }
        }
        lifecycle.addObserver(
            object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (
                        event == Lifecycle.Event.ON_CREATE &&
                            checkEnabledInInitAddedLifecycleObserver &&
                            hasDefaultSavedState(savedStateRegistry)
                    ) {
                        checkEnabledInInitAddedLifecycleObserver = false
                        lifecycle.removeObserver(this)
                    }
                }
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkEnabledInOnCreate && hasDefaultSavedState(savedStateRegistry)) {
            checkEnabledInOnCreate = false
        }
    }

    companion object {
        internal var checkEnabledInOnCreate = false
        internal var checkEnabledInInitAddedLifecycleObserver = false
        internal var checkEnabledInOnContextAvailable = false
    }
}
