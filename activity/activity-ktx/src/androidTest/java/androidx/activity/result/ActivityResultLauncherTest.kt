/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ActivityResultLauncherTest {

    @Test
    fun testUnitLaunch() {
        val expectedResult = "result"
        val registry = object : ActivityResultRegistry() {
            override fun <I : Any?, O : Any?> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                contract.createIntent(InstrumentationRegistry.getInstrumentation().context, input)
                dispatchResult(requestCode, expectedResult)
            }
        }

        val contract = object : ActivityResultContract<Unit, String?>() {
            override fun createIntent(context: Context, input: Unit) = Intent()
            override fun parseResult(resultCode: Int, intent: Intent?) = ""
        }

        var actualResult: String? = null

        val launcher = registry.register("key", contract) {
            actualResult = it
        }

        launcher.launch()
        assertThat(actualResult).isEqualTo(expectedResult)
    }
}