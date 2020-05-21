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

package androidx.camera.camera2.pipe.testing

import android.os.Build
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.model.FrameworkMethod
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.internal.bytecode.InstrumentationConfiguration

/**
 * A [RobolectricTestRunner] for [androidx.camera.camera2.pipe] unit tests.
 *
 * It has instrumentation turned off for the [androidx.camera.camera2.pipe] package.
 *
 * Robolectric tries to instrument Kotlin classes, and it throws errors when it encounters
 * companion objects, constructors with default values for parameters, and data classes with
 * inline classes. We don't need shadowing of our classes because we want to use the actual
 * objects in our tests.
 */
class CameraPipeRobolectricTestRunner(testClass: Class<*>) : RobolectricTestRunner(testClass) {
    override fun createClassLoaderConfig(method: FrameworkMethod?): InstrumentationConfiguration {
        val builder = InstrumentationConfiguration.Builder(super.createClassLoaderConfig(method))
        builder.doNotInstrumentPackage("androidx.camera.camera2.pipe")
        return builder.build()
    }
}

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class TestValue(val value: String)

data class TestData(
    val value1: TestValue,
    val value2: String
)

@SmallTest
@RunWith(JUnit4::class)
class DataWithInlineClassJUnitTest {
    @Test
    fun inlineClassesAreEqualInJUnit() {
        Truth.assertThat(TestValue("42")).isEqualTo(
            TestValue("42")
        )
    }

    @Test
    fun dataWithInlineClassesAreEqualInJUnit() {
        Truth.assertThat(
            TestData(
                value1 = TestValue("Test value #1"),
                value2 = "Test value #2"
            )
        ).isEqualTo(
            TestData(
                value1 = TestValue("Test value #1"),
                value2 = "Test value #2"
            )
        )
    }
}

@SmallTest
@RunWith(CameraPipeRobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class DataWithInlineClassRobolectricTest {
    @Test
    fun inlineClassesAreEqualInRobolectric() {
        Truth.assertThat(TestValue("42")).isEqualTo(
            TestValue("42")
        )
    }

    @Test
    fun dataWithInlineClassesAreEqualInRobolectric() {
        Truth.assertThat(
            TestData(
                value1 = TestValue("Test value #1"),
                value2 = "Test value #2"
            )
        ).isEqualTo(
            TestData(
                value1 = TestValue("Test value #1"),
                value2 = "Test value #2"
            )
        )
    }
}
