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

package androidx.navigation.compose.lint

import androidx.compose.lint.test.Stubs
import androidx.navigation.lint.common.NAVIGATION_STUBS
import androidx.navigation.lint.common.TEST_CLASS
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class WrongStartDestinationTypeDetectorTest() : LintDetectorTest() {

    @Test
    fun testEmptyConstructorNoError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.compose.runtime.Composable
                import androidx.test.*

                @Composable
                fun createGraph() {
                    val controller = NavHostController()
                    NavHost(navController = controller, startDestination = TestClass())
                    NavHost(navController = controller, startDestination = TestClassComp())
                }
                """
                    )
                    .indented(),
                *STUBS,
                Stubs.Composable
            )
            .skipTestModes(TestMode.FULLY_QUALIFIED)
            .run()
            .expectClean()
    }

    @Test
    fun testNavHost_classNoErrors() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.compose.runtime.Composable
                import androidx.test.*

                @Composable
                fun createGraph() {
                    val controller = NavHostController()
                    NavHost(navController = controller, startDestination = TestClassWithArg(15)) {}
                    NavHost(navController = controller, startDestination = classInstanceRef) {}
                    NavHost(navController = controller, startDestination = classInstanceWithArgRef) {}
                    NavHost(navController = controller, startDestination = TestClass::class) {}
                    NavHost(navController = controller, startDestination = Outer.InnerClass::class) {}
                    NavHost(navController = controller, startDestination = Outer.InnerClass(15)) {}
                    NavHost(navController = controller, startDestination = InterfaceChildClass(true)) {}
                    NavHost(navController = controller, startDestination = AbstractChildClass(true)) {}
                    NavHost(navController = controller, startDestination = TestClassWithArgComp(15)) {}
                    NavHost(navController = controller, startDestination = TestClassComp::class) {}
                    NavHost(navController = controller, startDestination = OuterComp.InnerClassComp::class) {}
                    NavHost(navController = controller, startDestination = OuterComp.InnerClassComp(15)) {}
                    NavHost(navController = controller, startDestination = InterfaceChildClassComp(true)) {}
                    NavHost(navController = controller, startDestination = AbstractChildClassComp(true)) {}
                }
                """
                    )
                    .indented(),
                *STUBS,
                Stubs.Composable
            )
            .run()
            .expectClean()
    }

    @Test
    fun testNavHost_objectNoError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.compose.runtime.Composable
                import androidx.test.*

                @Composable
                fun createGraph() {
                    val controller = NavHostController()
                    NavHost(navController = controller, startDestination = Outer) {}
                    NavHost(navController = controller, startDestination = Outer.InnerObject) {}
                    NavHost(navController = controller, startDestination = TestObject) {}
                    NavHost(navController = controller, startDestination = TestObject::class) {}
                    NavHost(navController = controller, startDestination = Outer.InnerObject::class) {}
                    NavHost(navController = controller, startDestination = InterfaceChildObject) {}
                    NavHost(navController = controller, startDestination = AbstractChildObject) {}
                    NavHost(navController = controller, startDestination = OuterComp.InnerObject::class) {}
                    NavHost(navController = controller, startDestination = AbstractChildObjectComp) {}
                }
                """
                    )
                    .indented(),
                *STUBS,
                Stubs.Composable
            )
            .run()
            .expectClean()
    }

    @Test
    fun testNavHost_classHasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.compose.runtime.Composable
                import androidx.test.*

                @Composable
                fun createGraph() {
                    val controller = NavHostController()
                    NavHost(navController = controller, startDestination = TestClass) {}
                    NavHost(navController = controller, startDestination = TestClassWithArg) {}
                    NavHost(navController = controller, startDestination = Outer.InnerClass) {}
                    NavHost(navController = controller, startDestination = InterfaceChildClass) {}
                    NavHost(navController = controller, startDestination = AbstractChildClass) {}
                    NavHost(navController = controller, startDestination = TestInterface)
                    NavHost(navController = controller, startDestination = TestAbstract)
                    // classes with companion object to simulate marked with @Serializable
                    NavHost(navController = controller, startDestination = TestClassComp)
                    NavHost(navController = controller, startDestination = TestClassWithArgComp)
                    NavHost(navController = controller, startDestination = OuterComp.InnerClassComp)
                    NavHost(navController = controller, startDestination = InterfaceChildClassComp)
                    NavHost(navController = controller, startDestination = AbstractChildClassComp)
                    NavHost(navController = controller, startDestination = TestAbstractComp)
                }
                """
                    )
                    .indented(),
                *STUBS,
                Stubs.Composable
            )
            .run()
            .expect(
                """
src/com/example/test.kt:10: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestClass(...)?
If the class TestClass does not contain arguments,
you can also pass in its KClass reference TestClass::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = TestClass) {}
                                                           ~~~~~~~~~
src/com/example/test.kt:11: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestClassWithArg(...)?
If the class TestClassWithArg does not contain arguments,
you can also pass in its KClass reference TestClassWithArg::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = TestClassWithArg) {}
                                                           ~~~~~~~~~~~~~~~~
src/com/example/test.kt:12: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor InnerClass(...)?
If the class InnerClass does not contain arguments,
you can also pass in its KClass reference InnerClass::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = Outer.InnerClass) {}
                                                           ~~~~~~~~~~~~~~~~
src/com/example/test.kt:13: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor InterfaceChildClass(...)?
If the class InterfaceChildClass does not contain arguments,
you can also pass in its KClass reference InterfaceChildClass::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = InterfaceChildClass) {}
                                                           ~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:14: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor AbstractChildClass(...)?
If the class AbstractChildClass does not contain arguments,
you can also pass in its KClass reference AbstractChildClass::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = AbstractChildClass) {}
                                                           ~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:15: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestInterface(...)?
If the class TestInterface does not contain arguments,
you can also pass in its KClass reference TestInterface::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = TestInterface)
                                                           ~~~~~~~~~~~~~
src/com/example/test.kt:16: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestAbstract(...)?
If the class TestAbstract does not contain arguments,
you can also pass in its KClass reference TestAbstract::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = TestAbstract)
                                                           ~~~~~~~~~~~~
src/com/example/test.kt:18: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = TestClassComp)
                                                           ~~~~~~~~~~~~~
src/com/example/test.kt:19: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = TestClassWithArgComp)
                                                           ~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:20: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = OuterComp.InnerClassComp)
                                                           ~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:21: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = InterfaceChildClassComp)
                                                           ~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:22: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = AbstractChildClassComp)
                                                           ~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:23: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    NavHost(navController = controller, startDestination = TestAbstractComp)
                                                           ~~~~~~~~~~~~~~~~
13 errors, 0 warnings
            """
            )
    }

    override fun getDetector(): Detector = WrongStartDestinationTypeDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(WrongStartDestinationTypeDetector.WrongStartDestinationType)
}

private val STUBS =
    arrayOf(TEST_CLASS, COMPOSABLE_NAV_HOST, *NAVIGATION_STUBS)
        .map { it.toTestBytecodeStub() }
        .toTypedArray()
