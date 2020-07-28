/*
 * Copyright 2020 The Android Open Source Project
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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.test.FragmentTestActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for Fragment registerForActivityResult
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class FragmentActivityResultTest {

    @Test
    fun registerActivityResultInOnAttach() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            withActivity {
                val fragment = RegisterInOnAttachFragment()

                supportFragmentManager.beginTransaction()
                    .add(androidx.fragment.test.R.id.content, fragment)
                    .commitNow()

                assertThat(fragment.launchedCounter).isEqualTo(1)
            }
        }
    }

    @Test
    fun registerActivityResultInOnCreate() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            withActivity {
                val fragment = RegisterInOnCreateFragment()

                supportFragmentManager.beginTransaction()
                    .add(androidx.fragment.test.R.id.content, fragment)
                    .commitNow()

                assertThat(fragment.launchedCounter).isEqualTo(1)
            }
        }
    }

    @Test
    fun launchActivityResultInOnCreate() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            withActivity {
                val fragment = ActivityResultFragment()

                supportFragmentManager.beginTransaction()
                    .add(androidx.fragment.test.R.id.content, fragment)
                    .commitNow()
            }
        }
    }

    @Test
    fun launchTwoActivityResult() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            withActivity {
                val fragment = DoubleActivityResultFragment()

                supportFragmentManager.beginTransaction()
                    .add(androidx.fragment.test.R.id.content, fragment)
                    .commitNow()

                assertThat(fragment.launchedCounter).isEqualTo(2)
            }
        }
    }
}

class ActivityResultFragment : Fragment() {
    private val registry = object : ActivityResultRegistry() {
        override fun <I : Any?, O : Any?> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?
        ) { }
    }

    val launcher = registerForActivityResult(
        StartActivityForResult(),
        registry
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcher.launch(Intent())
    }
}

class DoubleActivityResultFragment : Fragment() {
    private val registry = object : ActivityResultRegistry() {
        override fun <I : Any?, O : Any?> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?
        ) { dispatchResult(requestCode, Activity.RESULT_OK, Intent()) }
    }

    var launchedCounter = 0

    val launcher1 = registerForActivityResult(
        StartActivityForResult(),
        registry
    ) { launchedCounter++ }

    val launcher2 = registerForActivityResult(
        StartActivityForResult(),
        registry
    ) { launchedCounter++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcher1.launch(Intent())
        launcher2.launch(Intent())
    }
}

class RegisterInOnAttachFragment : Fragment() {
    private val registry = object : ActivityResultRegistry() {
        override fun <I : Any?, O : Any?> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?
        ) { dispatchResult(requestCode, Activity.RESULT_OK, Intent()) }
    }

    lateinit var launcher: ActivityResultLauncher<Intent>

    var launchedCounter = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        launcher = registerForActivityResult(
            StartActivityForResult(),
            registry
        ) { launchedCounter++ }
    }

    override fun onStart() {
        super.onStart()
        launcher.launch(Intent())
    }
}

class RegisterInOnCreateFragment : Fragment() {
    private val registry = object : ActivityResultRegistry() {
        override fun <I : Any?, O : Any?> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?
        ) { dispatchResult(requestCode, Activity.RESULT_OK, Intent()) }
    }

    lateinit var launcher: ActivityResultLauncher<Intent>

    var launchedCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcher = registerForActivityResult(
            StartActivityForResult(),
            registry
        ) { launchedCounter++ }
    }

    override fun onStart() {
        super.onStart()
        launcher.launch(Intent())
    }
}
