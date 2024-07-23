/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.lifecycle.viewmodel.compose.lint.lint

import androidx.compose.lint.test.Stubs
import androidx.lifecycle.lint.ViewModelConstructorInComposableDetector
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ViewModelConstructorInComposableDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ViewModelConstructorInComposableDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ViewModelConstructorInComposableDetector.ISSUE)

    @Test
    fun constructInComposableShouldFail() {
        lint()
            .files(
                kotlin(
                    """
                    package com.example

                    import androidx.compose.runtime.*
                    import androidx.lifecycle.ViewModel

                    class MyViewModel: ViewModel()
                    @Composable
                    fun Test() {
                        val viewModel = MyViewModel()
                        val composableLambda = @Composable {
                            val vm = MyViewModel()
                        }
                    }
                """
                ),
                Stubs.Composable,
                VIEWMODEL
            )
            .run()
            .expect(
                """
src/com/example/MyViewModel.kt:10: Error: Constructing a view model in a composable [ViewModelConstructorInComposable]
                        val viewModel = MyViewModel()
                                        ~~~~~~~~~~~
src/com/example/MyViewModel.kt:12: Error: Constructing a view model in a composable [ViewModelConstructorInComposable]
                            val vm = MyViewModel()
                                     ~~~~~~~~~~~
2 errors, 0 warnings
            """
                    .trimIndent()
            )
    }

    @Test
    fun useExtensionMethodShouldPass() {
        lint()
            .files(
                kotlin(
                    """
                    package com.example

                    import androidx.compose.runtime.*
                    import androidx.lifecycle.ViewModel
                    import androidx.lifecycle.viewmodel.compose.viewModel

                    class MyViewModel: ViewModel()
                    @Composable
                    fun Test() {
                        val viewModel = viewModel<MyViewModel>()
                        val viewModel2 = viewModel { MyViewModel() }
                    }

                    fun Test2(viewModel: MyViewModel = viewModel<MyViewModel>()) {

                    }
                """
                ),
                Stubs.Composable,
                VIEWMODEL,
                VIEWMODEL_COMPOSE
            )
            .run()
            .expectClean()
    }

    @Test
    fun constructedOutsideComposableShouldPass() {
        lint()
            .files(
                kotlin(
                    """
                    package com.example

                    import androidx.compose.runtime.*
                    import androidx.lifecycle.ViewModel

                    class MyViewModel: ViewModel()
                    fun Test() {
                        val myViewModel = MyViewModel()
                        val viewModel = ViewModel()
                    }
                """
                ),
                Stubs.Composable,
                VIEWMODEL,
            )
            .run()
            .expectClean()
    }
}
