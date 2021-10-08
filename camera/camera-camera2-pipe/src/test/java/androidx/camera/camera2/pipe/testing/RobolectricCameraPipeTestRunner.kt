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

package androidx.camera.camera2.pipe.testing

import android.os.Build
import com.google.common.truth.Truth.assertThat
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
 * This test runner disables instrumentation for the [androidx.camera.camera2.pipe] and
 * [androidx.camera.camera2.pipe.testing] packages.
 *
 * Robolectric tries to instrument Kotlin classes, and it throws errors when it encounters
 * companion objects, constructors with default values for parameters, and data classes with
 * inline classes. We don't need shadowing of our classes because we want to use the actual
 * objects in our tests.
 */
public class RobolectricCameraPipeTestRunner(testClass: Class<*>) :
    RobolectricTestRunner(testClass) {
    override fun createClassLoaderConfig(method: FrameworkMethod?): InstrumentationConfiguration {
        val builder = InstrumentationConfiguration.Builder(super.createClassLoaderConfig(method))
        builder.doNotInstrumentPackage("androidx.camera.camera2.pipe")
        builder.doNotInstrumentPackage("androidx.camera.camera2.pipe.testing")
        return builder.build()
    }
}

@Suppress("INLINE_CLASS_DEPRECATED", "EXPERIMENTAL_FEATURE_WARNING")
public inline class TestValue(public val value: String)

public data class TestData(
    val value1: TestValue,
    val value2: String
)

@RunWith(JUnit4::class)
public class DataWithInlineClassJUnitTest {
    @Test
    public fun inlineClassesAreEqualInJUnit() {
        assertThat(TestValue("42")).isEqualTo(
            TestValue("42")
        )
    }

    @Test
    public fun dataWithInlineClassesAreEqualInJUnit() {
        assertThat(
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

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class DataWithInlineClassRobolectricTest {
    @Test
    public fun inlineClassesAreEqualInRobolectric() {
        assertThat(TestValue("42")).isEqualTo(
            TestValue("42")
        )
    }

    @Test
    public fun dataWithInlineClassesAreEqualInRobolectric() {
        assertThat(
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
