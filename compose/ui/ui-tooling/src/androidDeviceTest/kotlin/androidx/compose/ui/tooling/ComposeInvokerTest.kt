/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.tooling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

@Suppress("DEPRECATION")
@OptIn(ExperimentalComposeUiApi::class)
class ComposeInvokerTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun workingComposable() {
        rule.setContent {
            ComposableInvoker.invokeComposable(
                "androidx.compose.ui.tooling.MyTestComposables",
                "MyWorkingComposable",
                currentComposer,
            )
        }
    }

    @Test
    fun composableWithBooleanPreviewParams() {
        rule.setContent {
            ComposableInvoker.invokeComposable(
                "androidx.compose.ui.tooling.MyTestComposableWithBooleanPreviewParams",
                "TestContent",
                currentComposer,
            )
        }
    }

    @Test
    fun composableWithIntPreviewParams() {
        rule.setContent {
            ComposableInvoker.invokeComposable(
                "androidx.compose.ui.tooling.MyTestComposableWithIntPreviewParams",
                "TestContent",
                currentComposer,
            )
        }
    }

    @Test
    fun composableWithMoreThan21DefaultParameters() {
        rule.setContent {
            ComposableInvoker.invokeComposable(
                "androidx.compose.ui.tooling.MyTestComposableWithManyDefaults",
                "ManyDefaults",
                currentComposer,
            )
        }
        assertEquals(25, MyTestComposableWithManyDefaults.lastInvokedParam25)
    }

    @Test
    fun composableClassNotFound() {
        try {
            rule.setContent {
                ComposableInvoker.invokeComposable(
                    "androidx.compose.ui.tooling.ClassDoesntExist",
                    "MyWorkingComposable",
                    currentComposer,
                )
            }
            fail("ClassNotFoundException expected to be thrown")
        } catch (e: ClassNotFoundException) {
            // ClassNotFoundException expected to be thrown when Composable class is not found.
        }
    }

    @Test
    fun composableMethodNotFound() {
        try {
            rule.setContent {
                ComposableInvoker.invokeComposable(
                    "androidx.compose.ui.tooling.MyTestComposables",
                    "MethodDoesntExist",
                    currentComposer,
                )
            }
            fail("NoSuchMethodException expected to be thrown")
        } catch (e: NoSuchMethodException) {
            // NoSuchMethodException expected to be thrown when Composable method is not found.
        }
    }

    @Test
    fun composableMethodThrowsException() {
        try {
            rule.setContent {
                ComposableInvoker.invokeComposable(
                    "androidx.compose.ui.tooling.MyTestComposables",
                    "MyThrowExceptionComposable",
                    currentComposer,
                )
            }
            fail("InvocationTargetException expected to be thrown")
        } catch (e: InvocationTargetException) {
            // InvocationTargetException expected to be thrown when Composable throws an exception.
        }
    }
}

class MyTestComposables {

    @Composable fun MyWorkingComposable() {}

    @Composable
    fun MyThrowExceptionComposable() {
        throw Exception("An Exception")
    }
}

class MyTestComposableWithBooleanPreviewParams {

    @Composable fun TestContent() {}

    @Preview
    @Composable
    private fun TestContent(
        @PreviewParameter(TestContentParameterProviderBoolean::class)
        @Suppress("UNUSED_PARAMETER")
        valueParameter: Boolean
    ) {}

    private class TestContentParameterProviderBoolean : PreviewParameterProvider<Boolean> {
        override val values = sequenceOf(true, false)
    }
}

class MyTestComposableWithIntPreviewParams {

    @Composable fun TestContent() {}

    @Preview
    @Composable
    private fun TestContent(
        @PreviewParameter(TestContentParameterProviderInt::class)
        @Suppress("UNUSED_PARAMETER")
        valueParameter: Int
    ) {}

    private class TestContentParameterProviderInt : PreviewParameterProvider<Int> {
        override val values = sequenceOf(42, 45, 92)
    }
}

class MyTestComposableWithClassTypePreviewParams {
    @Preview(showBackground = true)
    @Composable
    private fun TestRadius(
        @PreviewParameter(CornerRadiusParamProvider::class)
        @Suppress("UNUSED_PARAMETER")
        radius: CornerRadius
    ) {}

    class CornerRadiusParamProvider : PreviewParameterProvider<CornerRadius> {
        override val values: Sequence<CornerRadius>
            get() = sequenceOf(CornerRadius(42f), CornerRadius.Zero, CornerRadius(0f, 34f))
    }
}

class MyTestComposableWithManyDefaults {
    companion object {
        var lastInvokedParam25: Int = 0
    }

    @Composable
    fun ManyDefaults(
        @Suppress("UNUSED_PARAMETER") p0: Int = 0,
        @Suppress("UNUSED_PARAMETER") p1: Int = 1,
        @Suppress("UNUSED_PARAMETER") p2: Int = 2,
        @Suppress("UNUSED_PARAMETER") p3: Int = 3,
        @Suppress("UNUSED_PARAMETER") p4: Int = 4,
        @Suppress("UNUSED_PARAMETER") p5: Int = 5,
        @Suppress("UNUSED_PARAMETER") p6: Int = 6,
        @Suppress("UNUSED_PARAMETER") p7: Int = 7,
        @Suppress("UNUSED_PARAMETER") p8: Int = 8,
        @Suppress("UNUSED_PARAMETER") p9: Int = 9,
        @Suppress("UNUSED_PARAMETER") p10: Int = 10,
        @Suppress("UNUSED_PARAMETER") p11: Int = 11,
        @Suppress("UNUSED_PARAMETER") p12: Int = 12,
        @Suppress("UNUSED_PARAMETER") p13: Int = 13,
        @Suppress("UNUSED_PARAMETER") p14: Int = 14,
        @Suppress("UNUSED_PARAMETER") p15: Int = 15,
        @Suppress("UNUSED_PARAMETER") p16: Int = 16,
        @Suppress("UNUSED_PARAMETER") p17: Int = 17,
        @Suppress("UNUSED_PARAMETER") p18: Int = 18,
        @Suppress("UNUSED_PARAMETER") p19: Int = 19,
        @Suppress("UNUSED_PARAMETER") p20: Int = 20,
        @Suppress("UNUSED_PARAMETER") p21: Int = 21,
        @Suppress("UNUSED_PARAMETER") p22: Int = 22,
        @Suppress("UNUSED_PARAMETER") p23: Int = 23,
        @Suppress("UNUSED_PARAMETER") p24: Int = 24,
        p25: Int = 25,
    ) {
        lastInvokedParam25 = p25
    }
}
