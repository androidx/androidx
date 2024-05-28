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
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.test.ViewModelActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistry
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.RecreatedActivity
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertThat
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class FragmentSavedStateRegistryTest {

    @get:Rule val rule = DetectLeaksAfterTestSuccess()

    private fun ActivityScenario<FragmentSavedStateActivity>.initializeSavedState(
        testFragment: Fragment = Fragment()
    ) = withActivity {
        val fragmentManager = supportFragmentManager
        fragmentManager.beginTransaction().add(testFragment, FRAGMENT_TAG).commitNow()
        assertThat(fragmentManager.findFragmentByTag(FRAGMENT_TAG)).isNotNull()
        assertThat(testFragment.lifecycle.currentState.isAtLeast(CREATED)).isTrue()
        val registry = testFragment.savedStateRegistry
        val savedState = registry.consumeRestoredStateForKey(CALLBACK_KEY)
        assertThat(savedState).isNull()
        registry.registerSavedStateProvider(CALLBACK_KEY, DefaultProvider())
    }

    @Test
    fun savedState() {
        withUse(ActivityScenario.launch(FragmentSavedStateActivity::class.java)) {
            initializeSavedState()
            recreate()
            withActivity {
                assertThat(fragment.lifecycle.currentState.isAtLeast(CREATED)).isTrue()
                checkDefaultSavedState(fragment.savedStateRegistry)
            }
        }
    }

    @Test
    fun savedStateLateInit() {
        withUse(ActivityScenario.launch(FragmentSavedStateActivity::class.java)) {
            initializeSavedState()
            recreate()
            withActivity {
                fragment.lifecycle.addObserver(
                    LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            checkDefaultSavedState(fragment.savedStateRegistry)
                        }
                    }
                )
            }
        }
    }

    @Test
    fun savedStateOnAttachConsume() {
        withUse(ActivityScenario.launch(FragmentSavedStateActivity::class.java)) {
            initializeSavedState(OnAttachCheckingFragment())
            recreate()
            val fragment = withActivity {
                supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as OnAttachCheckingFragment
            }
            assertThat(fragment.restoredState).isEqualTo(VALUE)
        }
    }

    @Test
    fun savedStateOnCreateConsume() {
        withUse(ActivityScenario.launch(FragmentSavedStateActivity::class.java)) {
            initializeSavedState(OnCreateCheckingFragment())
            recreate()
        }
    }

    @Test
    fun savedStateOnActivityResult() {
        withUse(ActivityScenario.launch(FragmentSavedStateActivity::class.java)) {
            val registry = withActivity { registry }
            initializeSavedState(OnActivityResultCheckingFragment(registry))
            recreate()
            moveToState(Lifecycle.State.CREATED)
            withActivity { (fragment as OnActivityResultCheckingFragment).launcher.launch("") }
            moveToState(Lifecycle.State.RESUMED)
        }
    }
}

private fun checkDefaultSavedState(store: SavedStateRegistry) {
    val savedState = store.consumeRestoredStateForKey(CALLBACK_KEY)
    assertThat(savedState).isNotNull()
    assertThat(savedState!!.getString(KEY)).isEqualTo(VALUE)
}

class FragmentSavedStateActivity : RecreatedActivity() {
    val registry =
        object : ActivityResultRegistry() {
            override fun <I : Any?, O : Any?> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                if (contract is ActivityResultContracts.GetContent) {
                    dispatchResult(requestCode, null)
                }
            }
        }

    init {
        supportFragmentManager.fragmentFactory =
            object : FragmentFactory() {
                override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                    return when (loadFragmentClass(classLoader, className)) {
                        OnActivityResultCheckingFragment::class.java ->
                            OnActivityResultCheckingFragment(registry)
                        else -> super.instantiate(classLoader, className)
                    }
                }
            }
    }

    val fragment: Fragment
        get() =
            supportFragmentManager.findFragmentByTag(FRAGMENT_TAG)
                ?: throw IllegalStateException("Fragment under test wasn't found")
}

class OnAttachCheckingFragment : Fragment() {

    var restoredState: String? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val savedState = savedStateRegistry.consumeRestoredStateForKey(CALLBACK_KEY)
        restoredState = savedState?.getString(KEY)
    }
}

class OnCreateCheckingFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            checkDefaultSavedState(savedStateRegistry)
        }
    }
}

class OnActivityResultCheckingFragment(activityResultRegistry: ActivityResultRegistry) :
    Fragment() {
    val viewModel by lazy {
        ViewModelProvider(this).get(ViewModelActivity.TestSavedStateViewModel::class.java)
    }

    val launcher =
        registerForActivityResult(ActivityResultContracts.GetContent(), activityResultRegistry) {
            // Accessing the ViewModel is what triggers the startup behavior of
            // SavedStateRegistryController
            viewModel
        }
}

private class DefaultProvider : SavedStateRegistry.SavedStateProvider {
    override fun saveState() = Bundle().apply { putString(KEY, VALUE) }
}

private const val FRAGMENT_TAG = "TAAAG"
private const val KEY = "key"
private const val VALUE = "value"
private const val CALLBACK_KEY = "foo"
