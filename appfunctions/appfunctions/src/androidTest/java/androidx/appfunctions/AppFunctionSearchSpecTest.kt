/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appfunctions

import android.app.appfunctions.AppFunctionName as PlatformAppFunctionName
import androidx.appfunctions.metadata.AppFunctionName
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@SdkSuppress(minSdkVersion = 37)
class AppFunctionSearchSpecTest {

    @Test
    fun testToPlatformSearchSpec() {
        val jetpackSpec =
            AppFunctionSearchSpec(
                packageNames = setOf("com.pkg"),
                schemaCategory = "category",
                schemaName = "name",
                minSchemaVersion = 2,
                functionNames = setOf(AppFunctionName("com.pkg", "func")),
            )

        val platformSpec = jetpackSpec.toPlatformSearchSpec()

        assertThat(platformSpec.packageNames).containsExactly("com.pkg")
        assertThat(platformSpec.schemaCategory).isEqualTo("category")
        assertThat(platformSpec.schemaName).isEqualTo("name")
        assertThat(platformSpec.minSchemaVersion).isEqualTo(2L)
        assertThat(platformSpec.functionNames)
            .containsExactly(PlatformAppFunctionName("com.pkg", "func"))
    }
}
