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

package androidx.activity.result

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.app.ActivityOptionsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ActivityResultLauncherTest {
    private val registry =
        object : ActivityResultRegistry() {
            var invokeCount = 0
            var invokeOptions: ActivityOptionsCompat? = null

            override fun <I : Any?, O : Any?> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                invokeCount++
                invokeOptions = options
            }
        }

    @get:Rule val rule = DetectLeaksAfterTestSuccess()

    @Test
    fun launchTest() {
        val launcher = registry.register("key", StartActivityForResult()) {}

        launcher.launch(Intent())

        assertWithMessage("the registry was not invoked").that(registry.invokeCount).isEqualTo(1)

        assertWithMessage("the options passed to invoke were not null")
            .that(registry.invokeOptions)
            .isNull()
    }

    @Test
    fun launchUnit() {
        val expectedResult = "result"
        val registry =
            object : ActivityResultRegistry() {
                override fun <I : Any?, O : Any?> onLaunch(
                    requestCode: Int,
                    contract: ActivityResultContract<I, O>,
                    input: I,
                    options: ActivityOptionsCompat?
                ) {
                    contract.createIntent(
                        InstrumentationRegistry.getInstrumentation().context,
                        input
                    )
                    dispatchResult(requestCode, expectedResult)
                }
            }

        val contract =
            object : ActivityResultContract<Unit, String?>() {
                override fun createIntent(context: Context, input: Unit) = Intent()

                override fun parseResult(resultCode: Int, intent: Intent?) = ""
            }

        var actualResult: String? = null

        val launcher = registry.register("key", contract) { actualResult = it }

        launcher.launch()
        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @SdkSuppress(minSdkVersion = 24) // Before API 24 getLaunchBounds returns null
    @Test
    fun launchWithOptionsTest() {
        val launcher = registry.register("key", StartActivityForResult()) {}

        val options = ActivityOptionsCompat.makeBasic()
        options.launchBounds = Rect().apply { left = 1 }

        launcher.launch(Intent(), options)

        assertWithMessage("the registry was not invoked").that(registry.invokeCount).isEqualTo(1)

        assertWithMessage("the options passed to invoke were null")
            .that(registry.invokeOptions)
            .isNotNull()

        assertWithMessage("the options passed to invoke did not match")
            .that(registry.invokeOptions?.launchBounds?.left)
            .isEqualTo(1)
    }

    @Test
    fun getContractTest() {
        val contract = StartActivityForResult()
        val launcher = registry.register("key", contract) {}

        assertThat(contract).isSameInstanceAs(launcher.contract)
    }
}
