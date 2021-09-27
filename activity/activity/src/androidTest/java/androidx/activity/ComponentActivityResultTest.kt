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
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ComponentActivityResultTest {
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

            scenario.withActivity { }

            scenario.withActivity {
                assertThat(firstLaunchCount).isEqualTo(0)
                assertThat(secondLaunchCount).isEqualTo(1)
            }
        }
    }

    @Test
    fun registerInInitTest() {
        ActivityScenario.launch(RegisterInInitActivity::class.java).use { scenario ->
            scenario.withActivity {
                recreate()
                launcher.launch(Intent(this, FinishActivity::class.java))
            }

            scenario.withActivity {
                assertThat(launchCount).isEqualTo(1)
            }
        }
    }

    @Test
    fun noActivityAvailableTest() {
        ActivityScenario.launch(RegisterInInitActivity::class.java).use { scenario ->
            var exceptionThrown = false
            scenario.withActivity {
                try {
                    launcher.launch(Intent("no action"))
                } catch (e: ActivityNotFoundException) {
                    exceptionThrown = true
                }
            }

            scenario.withActivity {
                assertThat(exceptionThrown).isTrue()
                assertThat(launchCount).isEqualTo(0)
            }
        }
    }
}

class PassThroughActivity : ComponentActivity() {
    private val launcher = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            finish()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcher.launch(intent.getParcelableExtra("destinationIntent"))
    }
}

class ResultComponentActivity : ComponentActivity() {
    var registryLaunchCount = 0

    val registry = object : ActivityResultRegistry() {

        override fun <I : Any?, O : Any?> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?
        ) {
            registryLaunchCount++
        }
    }

    val launcher = registerForActivityResult(StartActivityForResult(), registry) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        launcher.launch(Intent())
    }
}

class RegisterBeforeOnCreateActivity : ComponentActivity() {
    lateinit var launcher: ActivityResultLauncher<Intent>
    var firstLaunchCount = 0
    var secondLaunchCount = 0
    var recreated = false

    init {
        addOnContextAvailableListener {
            launcher = if (!recreated) {
                registerForActivityResult(StartActivityForResult()) {
                    firstLaunchCount++
                }
            } else {
                registerForActivityResult(StartActivityForResult()) {
                    secondLaunchCount++
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
    var launchCount = 0

    init {
        launcher = registerForActivityResult(StartActivityForResult()) {
            launchCount++
        }
    }
}

class FinishActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
