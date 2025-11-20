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
import androidx.fragment.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for Fragment registerForActivityResult */
@RunWith(AndroidJUnit4::class)
@MediumTest
class FragmentActivityResultTest {

    @get:Rule val rule = DetectLeaksAfterTestSuccess()

    @Test
    fun registerActivityResultInOnAttach() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            withActivity {
                val fragment = RegisterInLifecycleCallbackFragment(Fragment.ATTACHED)

                supportFragmentManager.beginTransaction().add(R.id.content, fragment).commitNow()

                assertThat(fragment.launchedCounter).isEqualTo(1)
            }
        }
    }

    @Test
    fun registerActivityResultInOnCreate() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            withActivity {
                val fragment = RegisterInLifecycleCallbackFragment(Fragment.CREATED)

                supportFragmentManager.beginTransaction().add(R.id.content, fragment).commitNow()

                assertThat(fragment.launchedCounter).isEqualTo(1)
            }
        }
    }

    @Test
    fun registerActivityResultInOnStart() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            withActivity {
                val fragment = RegisterInLifecycleCallbackFragment(Fragment.STARTED)

                try {
                    supportFragmentManager
                        .beginTransaction()
                        .add(R.id.content, fragment)
                        .commitNow()
                    fail("Registering for activity result after onCreate() should fail")
                } catch (e: IllegalStateException) {
                    assertThat(e)
                        .hasMessageThat()
                        .contains(
                            "Fragment $fragment is attempting to " +
                                "registerForActivityResult after being created. Fragments must call " +
                                "registerForActivityResult() before they are created (i.e. " +
                                "initialization, onAttach(), or onCreate())."
                        )
                }
            }
        }
    }

    @Test
    fun launchActivityResultInOnCreate() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            withActivity {
                val fragment = ActivityResultFragment()

                supportFragmentManager.beginTransaction().add(R.id.content, fragment).commitNow()
            }
        }
    }

    @Test
    fun launchTwoActivityResult() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            withActivity {
                val fragment = DoubleActivityResultFragment()

                supportFragmentManager.beginTransaction().add(R.id.content, fragment).commitNow()

                assertThat(fragment.launchedCounter).isEqualTo(2)
            }
        }
    }

    @Test
    fun launchMultipleActivitiesFromFragment() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragment = LaunchMultipleActivitiesFragment()
            val fm = withActivity { supportFragmentManager }

            fm.beginTransaction().add(R.id.content, fragment).commit()
            executePendingTransactions()

            @Suppress("DEPRECATION")
            withActivity {
                fragment.startActivityForResult(
                    Intent(this, ResultActivity1::class.java),
                    ResultActivity1.REQUEST_CODE,
                )
                fragment.startActivityForResult(
                    Intent(this, ResultActivity2::class.java),
                    ResultActivity2.REQUEST_CODE,
                )
                fragment.startActivityForResult(
                    Intent(this, ResultActivity3::class.java),
                    ResultActivity3.REQUEST_CODE,
                )
            }

            assertThat(fragment.onActivityResultCountDownLatch.await(1000, TimeUnit.MILLISECONDS))
                .isTrue()

            assertThat(fragment.launcherInfoMap)
                .containsEntry(ResultActivity1.REQUEST_CODE, ResultActivity1.RESULT_KEY)
            assertThat(fragment.launcherInfoMap)
                .containsEntry(ResultActivity2.REQUEST_CODE, ResultActivity2.RESULT_KEY)
            assertThat(fragment.launcherInfoMap)
                .containsEntry(ResultActivity3.REQUEST_CODE, ResultActivity3.RESULT_KEY)
        }
    }
}

class ActivityResultFragment : Fragment() {
    private val registry =
        object : ActivityResultRegistry() {
            override fun <I : Any?, O : Any?> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?,
            ) {}
        }

    val launcher = registerForActivityResult(StartActivityForResult(), registry) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcher.launch(Intent())
    }
}

class DoubleActivityResultFragment : Fragment() {
    private val registry =
        object : ActivityResultRegistry() {
            override fun <I : Any?, O : Any?> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?,
            ) {
                dispatchResult(requestCode, Activity.RESULT_OK, Intent())
            }
        }

    var launchedCounter = 0

    val launcher1 =
        registerForActivityResult(StartActivityForResult(), registry) { launchedCounter++ }

    val launcher2 =
        registerForActivityResult(StartActivityForResult(), registry) { launchedCounter++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcher1.launch(Intent())
        launcher2.launch(Intent())
    }
}

class RegisterInLifecycleCallbackFragment(val state: Int) : Fragment() {
    private val registry =
        object : ActivityResultRegistry() {
            override fun <I : Any?, O : Any?> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?,
            ) {
                dispatchResult(requestCode, Activity.RESULT_OK, Intent())
            }
        }

    lateinit var launcher: ActivityResultLauncher<Intent>

    var launchedCounter = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (state == ATTACHED) {
            launcher =
                registerForActivityResult(StartActivityForResult(), registry) { launchedCounter++ }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (state == CREATED) {
            launcher =
                registerForActivityResult(StartActivityForResult(), registry) { launchedCounter++ }
        }
    }

    override fun onStart() {
        super.onStart()
        if (state == STARTED) {
            launcher =
                registerForActivityResult(StartActivityForResult(), registry) { launchedCounter++ }
        }
        launcher.launch(Intent())
    }
}

@Suppress("DEPRECATION")
class LaunchMultipleActivitiesFragment : Fragment() {
    val launcherInfoMap: MutableMap<Int, String> = mutableMapOf()
    val onActivityResultCountDownLatch = CountDownLatch(3)

    @Deprecated("Deprecated in Fragment")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val extras = data?.extras
        if (extras!!.containsKey(ResultActivity1.RESULT_KEY)) {
            launcherInfoMap[requestCode] = ResultActivity1.RESULT_KEY
        } else if (extras.containsKey(ResultActivity2.RESULT_KEY)) {
            launcherInfoMap[requestCode] = ResultActivity2.RESULT_KEY
        } else if (extras.containsKey(ResultActivity3.RESULT_KEY)) {
            launcherInfoMap[requestCode] = ResultActivity3.RESULT_KEY
        }

        onActivityResultCountDownLatch.countDown()
    }
}

class ResultActivity1 : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_OK, Intent().putExtra(RESULT_KEY, RESULT_VALUE))
        finish()
    }

    companion object {
        const val REQUEST_CODE = 1111
        const val RESULT_KEY = "ResultActivity1"
        private const val RESULT_VALUE = "ResultActivity1Value"
    }
}

class ResultActivity2 : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_OK, Intent().putExtra(RESULT_KEY, RESULT_VALUE))
        finish()
    }

    companion object {
        const val REQUEST_CODE = 2222
        const val RESULT_KEY = "ResultActivity2"
        private const val RESULT_VALUE = "ResultActivity2Value"
    }
}

class ResultActivity3 : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_OK, Intent().putExtra(RESULT_KEY, RESULT_VALUE))
        finish()
    }

    companion object {
        const val REQUEST_CODE = 3333
        const val RESULT_KEY = "ResultActivity3"
        private const val RESULT_VALUE = "ResultActivity3Value"
    }
}
