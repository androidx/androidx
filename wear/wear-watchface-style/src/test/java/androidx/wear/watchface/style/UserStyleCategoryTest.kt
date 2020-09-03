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

package androidx.wear.watchface.style

import android.graphics.drawable.Icon
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.model.FrameworkMethod
import org.robolectric.RobolectricTestRunner
import org.robolectric.internal.bytecode.InstrumentationConfiguration

// Without this we get test failures with an error:
// "failed to access class kotlin.jvm.internal.DefaultConstructorMarker".
class WatchFaceServiceTestRunner(testClass: Class<*>) : RobolectricTestRunner(testClass) {
    override fun createClassLoaderConfig(method: FrameworkMethod): InstrumentationConfiguration =
        InstrumentationConfiguration.Builder(super.createClassLoaderConfig(method))
            .doNotInstrumentPackage("androidx.wear.watchface.style")
            .build()
}

@RunWith(WatchFaceServiceTestRunner::class)
class UserStyleCategoryTest {

    private val icon1 = Icon.createWithContentUri("icon1")
    private val icon2 = Icon.createWithContentUri("icon2")
    private val icon3 = Icon.createWithContentUri("icon3")
    private val icon4 = Icon.createWithContentUri("icon4")

    @Test
    fun rangedUserStyleCategory_getOptionForId_returns_default_for_bad_input() {
        val defaultValue = 0.75
        val rangedUserStyleCategory =
            DoubleRangeUserStyleCategory(
                "example_category",
                "Example Ranged Category",
                "An example category",
                null,
                0.0,
                1.0,
                defaultValue
            )

        assertThat(rangedUserStyleCategory.getOptionForId("not a number").id)
            .isEqualTo(defaultValue.toString())

        assertThat(rangedUserStyleCategory.getOptionForId("-1").id)
            .isEqualTo(defaultValue.toString())

        assertThat(rangedUserStyleCategory.getOptionForId("10").id)
            .isEqualTo(defaultValue.toString())
    }

    @Test
    fun rangedUserStyleCategory_getOptionForId() {
        val defaultValue = 0.75
        val rangedUserStyleCategory =
            DoubleRangeUserStyleCategory(
                "example_category",
                "Example Ranged Category",
                "An example category",
                null,
                0.0,
                1.0,
                defaultValue
            )

        assertThat(rangedUserStyleCategory.getOptionForId("0").id)
            .isEqualTo("0.0")

        assertThat(rangedUserStyleCategory.getOptionForId("0.5").id)
            .isEqualTo("0.5")

        assertThat(rangedUserStyleCategory.getOptionForId("1").id)
            .isEqualTo("1.0")
    }
}