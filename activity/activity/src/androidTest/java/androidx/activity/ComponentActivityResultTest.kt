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

package androidx.activity

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ComponentActivityResultTest {

    @get:Rule val rule = DetectLeaksAfterTestSuccess()

    @Test
    fun launchInOnCreate() {
        ActivityScenario.launch(ResultComponentActivity::class.java).use { scenario ->
            val launchCount = scenario.withActivity { this.registryLaunchCount }
            assertThat(launchCount).isEqualTo(1)
        }
    }

    @Test
    fun leaveProcessWithParcelableExtra() {
        ActivityScenario.launch(EmptyContentActivity::class.java).use { scenario ->
            scenario.withActivity {
                val intent = Intent(this, PassThroughActivity::class.java)
                val destinationIntent = Intent(this, EmptyContentActivity::class.java)
                destinationIntent.putExtra("parcelable", ActivityResult(1, null))
                intent.putExtra("destinationIntent", destinationIntent)
                startActivity(intent)
            }
        }
    }

    @Test
    fun registerBeforeOnCreateTest() {
        ActivityScenario.launch(RegisterBeforeOnCreateActivity::class.java).use { scenario ->
            scenario.withActivity {
                recreate()
                launcher.launch(Intent(this, FinishActivity::class.java))
            }

            scenario.withActivity {}

            val latch = scenario.withActivity { launchCountDownLatch }
            val list = scenario.withActivity { launchedList }

            assertThat(latch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
            assertThat(list).containsExactly("second")
        }
    }

    @Test
    fun registerInInitTest() {
        ActivityScenario.launch(RegisterInInitActivity::class.java).use { scenario ->
            scenario.withActivity {
                recreate()
                launcher.launch(Intent(this, FinishActivity::class.java))
            }

            val launchCountDownLatch = scenario.withActivity { launchCount }

            assertThat(launchCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        }
    }

    @Test
    fun noActivityAvailableLifecycleTest() {
        ActivityScenario.launch(RegisterInInitActivity::class.java).use { scenario ->
            var exceptionThrown = false
            scenario.withActivity {
                try {
                    launcher.launch(Intent("no action"))
                } catch (e: ActivityNotFoundException) {
                    exceptionThrown = true
                }
            }

            val launchCountDownLatch =
                scenario.withActivity {
                    assertThat(exceptionThrown).isTrue()
                    launchCount
                }

            assertThat(launchCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isFalse()
        }
    }

    @Test
    fun noActivityAvailableNoLifecycleTest() {
        ActivityScenario.launch(RegisterInInitActivity::class.java).use { scenario ->
            var exceptionThrown = false
            scenario.withActivity {
                try {
                    launcherNoLifecycle.launch(Intent("no action"))
                } catch (e: ActivityNotFoundException) {
                    exceptionThrown = true
                }
            }

            val launchCountDownLatch =
                scenario.withActivity {
                    assertThat(exceptionThrown).isTrue()
                    launchCount
                }

            assertThat(launchCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isFalse()
        }
    }
}

class PassThroughActivity : ComponentActivity() {
    private val launcher =
        registerForActivityResult(StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                finish()
            }
        }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcher.launch(intent.getParcelableExtra("destinationIntent")!!)
    }
}

class ResultComponentActivity : ComponentActivity() {
    var registryLaunchCount = 0

    val registry =
        object : ActivityResultRegistry() {

            override fun <I : Any?, O : Any?> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                registryLaunchCount++
            }
        }

    val launcher = registerForActivityResult(StartActivityForResult(), registry) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        launcher.launch(Intent())
    }
}

class RegisterBeforeOnCreateActivity : ComponentActivity() {
    lateinit var launcher: ActivityResultLauncher<Intent>
    var launchCountDownLatch = CountDownLatch(1)
    val launchedList = mutableListOf<String>()
    var recreated = false

    init {
        addOnContextAvailableListener {
            launcher =
                if (!recreated) {
                    registerForActivityResult(StartActivityForResult()) {
                        launchedList.add("first")
                        launchCountDownLatch.countDown()
                    }
                } else {
                    registerForActivityResult(StartActivityForResult()) {
                        launchedList.add("second")
                        launchCountDownLatch.countDown()
                    }
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            recreated = true
        }
        super.onCreate(savedInstanceState)
    }
}

class RegisterInInitActivity : ComponentActivity() {
    var launcher: ActivityResultLauncher<Intent>
    val launcherNoLifecycle: ActivityResultLauncher<Intent>
    var launchCount = CountDownLatch(1)

    init {
        launcher = registerForActivityResult(StartActivityForResult()) { launchCount.countDown() }
        launcherNoLifecycle =
            activityResultRegistry.register("test", StartActivityForResult()) {
                launchCount.countDown()
            }
    }
}

class FinishActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
