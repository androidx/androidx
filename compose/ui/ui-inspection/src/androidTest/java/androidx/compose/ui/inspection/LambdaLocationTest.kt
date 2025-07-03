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

package androidx.compose.ui.inspection

import androidx.compose.ui.inspection.LambdaLocation.Companion.findLambdaSelector
import androidx.compose.ui.inspection.rules.JvmtiRule
import androidx.compose.ui.inspection.testdata.TestLambdas
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.android.tools.deploy.liveedit.SourceLocationAware
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class LambdaLocationTest {

    @get:Rule val rule = JvmtiRule()

    @Test
    fun test() {
        assertThat(LambdaLocation.resolve(TestLambdas.short))
            .isEqualTo(
                LambdaLocation(
                    "androidx.compose.ui.inspection.testdata.TestLambdas\$short\$1",
                    "TestLambdas.kt",
                    22,
                    22,
                )
            )
        assertThat(LambdaLocation.resolve(TestLambdas.long))
            .isEqualTo(
                LambdaLocation(
                    "androidx.compose.ui.inspection.testdata.TestLambdas\$long\$1",
                    "TestLambdas.kt",
                    24,
                    26,
                )
            )
        assertThat(LambdaLocation.resolve(TestLambdas.inlined))
            .isEqualTo(
                LambdaLocation(
                    "androidx.compose.ui.inspection.testdata.TestLambdas\$inlined\$1",
                    "TestLambdas.kt",
                    29,
                    30,
                )
            )
        assertThat(LambdaLocation.resolve(TestLambdas.inlinedParameter))
            .isEqualTo(
                LambdaLocation(
                    "androidx.compose.ui.inspection.testdata.TestLambdas\$inlinedParameter\$1",
                    "TestLambdas.kt",
                    32,
                    32,
                )
            )
        assertThat(LambdaLocation.resolve(TestLambdas.unnamed))
            .isEqualTo(
                LambdaLocation(
                    "androidx.compose.ui.inspection.testdata.TestLambdas\$unnamed\$1",
                    "TestLambdas.kt",
                    33,
                    33,
                )
            )
    }

    @Test
    fun testLambdaSelector() {
        assertThat(findLambdaSelector("com.example.Compose\$MainActivityKt\$lambda-10$1$2$2$1"))
            .isEqualTo("lambda-10\$1\$2\$2\$1")
        assertThat(findLambdaSelector("com.example.Class\$f1\$3\$2")).isEqualTo("3$2")
    }

    @Test
    fun testLiveEditLambda() {
        @Suppress("ObjectLiteralToLambda")
        val lambda =
            object : SourceLocationAware {
                override fun getSourceLocationInfo(): Map<String, Any> =
                    mapOf(
                        "lambda" to "com.example.Fct$1$2",
                        "file" to "MainActivity.kt",
                        "startLine" to 34,
                        "endLine" to 78,
                    )
            }
        val location = LambdaLocation.resolve(lambda) ?: error("Location didn't resolve")
        assertThat(location.packageName).isEqualTo("com.example")
        assertThat(location.lambdaName).isEqualTo("1$2")
        assertThat(location.fileName).isEqualTo("MainActivity.kt")
        assertThat(location.startLine).isEqualTo(34)
        assertThat(location.endLine).isEqualTo(78)
    }

    interface Foo
}
