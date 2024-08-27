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

package androidx.navigation.runtime.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class WrongStartDestinationTypeDetectorTest : LintDetectorTest() {

    @Test
    fun testEmptyConstructorNoError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.NavController
                import androidx.navigation.TestNavHost
                import androidx.navigation.createGraph
                import androidx.test.*

                fun createGraph() {
                    val navController = NavController()
                    navController.createGraph(startDestination = TestClass())
                    navController.createGraph(startDestination = TestClassComp())

                    val navHost = TestNavHost()
                    navHost.createGraph(startDestination = TestClass())
                    navHost.createGraph(startDestination = TestClassComp())
                }
                """
                    )
                    .indented(),
                *NAVIGATION_STUBS,
                TEST_CODE
            )
            .skipTestModes(TestMode.FULLY_QUALIFIED)
            .run()
            .expectClean()
    }

    @Test
    fun testNavController_classNoErrors() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.NavController
                import androidx.navigation.createGraph
                import androidx.test.*

                fun createGraph() {
                    val navController = NavController()
                    navController.createGraph(startDestination = TestClassWithArg(15)) {}
                    navController.createGraph(startDestination = classInstanceRef) {}
                    navController.createGraph(startDestination = classInstanceWithArgRef) {}
                    navController.createGraph(startDestination = TestClass::class) {}
                    navController.createGraph(startDestination = Outer.InnerClass::class) {}
                    navController.createGraph(startDestination = Outer.InnerClass(15)) {}
                    navController.createGraph(startDestination = InterfaceChildClass(true)) {}
                    navController.createGraph(startDestination = AbstractChildClass(true)) {}
                    navController.createGraph(startDestination = TestClassWithArgComp(15))
                    navController.createGraph(startDestination = TestClassComp::class)
                    navController.createGraph(startDestination = OuterComp.InnerClassComp::class)
                    navController.createGraph(startDestination = OuterComp.InnerClassComp(15))
                    navController.createGraph(startDestination = InterfaceChildClassComp(true))
                    navController.createGraph(startDestination = AbstractChildClassComp(true))
                }
                """
                    )
                    .indented(),
                *NAVIGATION_STUBS,
                TEST_CODE
            )
            .run()
            .expectClean()
    }

    @Test
    fun testNavController_objectNoError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.NavController
                import androidx.navigation.createGraph
                import androidx.test.*

                fun createGraph() {
                    val navController = NavController()
                    navController.createGraph(startDestination = Outer) {}
                    navController.createGraph(startDestination = Outer.InnerObject) {}
                    navController.createGraph(startDestination = TestObject) {}
                    navController.createGraph(startDestination = TestObject::class) {}
                    navController.createGraph(startDestination = Outer.InnerObject::class) {}
                    navController.createGraph(startDestination = InterfaceChildObject) {}
                    navController.createGraph(startDestination = AbstractChildObject) {}
                    navController.createGraph(startDestination = OuterComp.InnerObject::class)
                    navController.createGraph(startDestination = AbstractChildObjectComp)
                }
                """
                    )
                    .indented(),
                *NAVIGATION_STUBS,
                TEST_CODE
            )
            .run()
            .expectClean()
    }

    @Test
    fun testNavController_classHasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.NavController
                import androidx.navigation.createGraph
                import androidx.test.*

                fun createGraph() {
                    val navController = NavController()
                    navController.createGraph(startDestination = TestClass) {}
                    navController.createGraph(startDestination = TestClassWithArg) {}
                    navController.createGraph(startDestination = Outer.InnerClass) {}
                    navController.createGraph(startDestination = InterfaceChildClass) {}
                    navController.createGraph(startDestination = AbstractChildClass) {}
                    navController.createGraph(startDestination = TestInterface)
                    navController.createGraph(startDestination = TestAbstract)
                    //classes with companion object to simulate marked with @Serializable
                    navController.createGraph(startDestination = TestClassComp)
                    navController.createGraph(startDestination = TestClassWithArgComp)
                    navController.createGraph(startDestination = OuterComp.InnerClassComp)
                    navController.createGraph(startDestination = InterfaceChildClassComp)
                    navController.createGraph(startDestination = AbstractChildClassComp)
                    navController.createGraph(startDestination = TestAbstractComp)
                }
                """
                    )
                    .indented(),
                *NAVIGATION_STUBS,
                TEST_CODE
            )
            .run()
            .expect(
                """
src/com/example/test.kt:9: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestClass(...)?
If the class TestClass does not contain arguments,
you can also pass in its KClass reference TestClass::class [WrongStartDestinationType]
    navController.createGraph(startDestination = TestClass) {}
                                                 ~~~~~~~~~
src/com/example/test.kt:10: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestClassWithArg(...)?
If the class TestClassWithArg does not contain arguments,
you can also pass in its KClass reference TestClassWithArg::class [WrongStartDestinationType]
    navController.createGraph(startDestination = TestClassWithArg) {}
                                                 ~~~~~~~~~~~~~~~~
src/com/example/test.kt:11: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor InnerClass(...)?
If the class InnerClass does not contain arguments,
you can also pass in its KClass reference InnerClass::class [WrongStartDestinationType]
    navController.createGraph(startDestination = Outer.InnerClass) {}
                                                 ~~~~~~~~~~~~~~~~
src/com/example/test.kt:12: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor InterfaceChildClass(...)?
If the class InterfaceChildClass does not contain arguments,
you can also pass in its KClass reference InterfaceChildClass::class [WrongStartDestinationType]
    navController.createGraph(startDestination = InterfaceChildClass) {}
                                                 ~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:13: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor AbstractChildClass(...)?
If the class AbstractChildClass does not contain arguments,
you can also pass in its KClass reference AbstractChildClass::class [WrongStartDestinationType]
    navController.createGraph(startDestination = AbstractChildClass) {}
                                                 ~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:14: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestInterface(...)?
If the class TestInterface does not contain arguments,
you can also pass in its KClass reference TestInterface::class [WrongStartDestinationType]
    navController.createGraph(startDestination = TestInterface)
                                                 ~~~~~~~~~~~~~
src/com/example/test.kt:15: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestAbstract(...)?
If the class TestAbstract does not contain arguments,
you can also pass in its KClass reference TestAbstract::class [WrongStartDestinationType]
    navController.createGraph(startDestination = TestAbstract)
                                                 ~~~~~~~~~~~~
src/com/example/test.kt:17: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    navController.createGraph(startDestination = TestClassComp)
                                                 ~~~~~~~~~~~~~
src/com/example/test.kt:18: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    navController.createGraph(startDestination = TestClassWithArgComp)
                                                 ~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:19: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    navController.createGraph(startDestination = OuterComp.InnerClassComp)
                                                 ~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:20: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    navController.createGraph(startDestination = InterfaceChildClassComp)
                                                 ~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:21: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    navController.createGraph(startDestination = AbstractChildClassComp)
                                                 ~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:22: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    navController.createGraph(startDestination = TestAbstractComp)
                                                 ~~~~~~~~~~~~~~~~
13 errors, 0 warnings
            """
            )
    }

    @Test
    fun testHost_classNoErrors() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.TestNavHost
                import androidx.navigation.createGraph
                import androidx.test.*

                fun createGraph() {
                    val navHost = TestNavHost()
                    navHost.createGraph(startDestination = TestClassWithArg(15)) {}
                    navHost.createGraph(startDestination = classInstanceRef) {}
                    navHost.createGraph(startDestination = classInstanceWithArgRef) {}
                    navHost.createGraph(startDestination = TestClass::class) {}
                    navHost.createGraph(startDestination = Outer.InnerClass::class) {}
                    navHost.createGraph(startDestination = Outer.InnerClass(15)) {}
                    navHost.createGraph(startDestination = InterfaceChildClass(true)) {}
                    navHost.createGraph(startDestination = AbstractChildClass(true)) {}
                    navHost.createGraph(startDestination = TestClassWithArgComp(15)) {}
                    navHost.createGraph(startDestination = TestClassComp::class) {}
                    navHost.createGraph(startDestination = OuterComp.InnerClassComp::class) {}
                    navHost.createGraph(startDestination = OuterComp.InnerClassComp(15)) {}
                    navHost.createGraph(startDestination = InterfaceChildClassComp(true)) {}
                    navHost.createGraph(startDestination = AbstractChildClassComp(true)) {}
                }
                """
                    )
                    .indented(),
                *NAVIGATION_STUBS,
                TEST_CODE
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

                import androidx.navigation.TestNavHost
                import androidx.navigation.createGraph
                import androidx.test.*

                fun createGraph() {
                    val navHost = TestNavHost()
                    navHost.createGraph(startDestination = Outer) {}
                    navHost.createGraph(startDestination = Outer.InnerObject) {}
                    navHost.createGraph(startDestination = TestObject) {}
                    navHost.createGraph(startDestination = TestObject::class) {}
                    navHost.createGraph(startDestination = Outer.InnerObject::class) {}
                    navHost.createGraph(startDestination = InterfaceChildObject) {}
                    navHost.createGraph(startDestination = AbstractChildObject) {}
                    navHost.createGraph(startDestination = OuterComp) {}
                    navHost.createGraph(startDestination = OuterComp.InnerObject) {}
                    navHost.createGraph(startDestination = OuterComp.InnerObject::class) {}
                }
                """
                    )
                    .indented(),
                *NAVIGATION_STUBS,
                TEST_CODE
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

                import androidx.navigation.TestNavHost
                import androidx.navigation.createGraph
                import androidx.test.*

                fun createGraph() {
                    val navHost = TestNavHost()
                    navHost.createGraph(startDestination = TestClass) {}
                    navHost.createGraph(startDestination = TestClassWithArg) {}
                    navHost.createGraph(startDestination = Outer.InnerClass) {}
                    navHost.createGraph(startDestination = InterfaceChildClass) {}
                    navHost.createGraph(startDestination = AbstractChildClass) {}
                    navHost.createGraph(startDestination = TestInterface)
                    navHost.createGraph(startDestination = TestAbstract)
                    navHost.createGraph(startDestination = TestClassComp) {}
                    navHost.createGraph(startDestination = TestClassWithArgComp) {}
                    navHost.createGraph(startDestination = OuterComp.InnerClassComp) {}
                    navHost.createGraph(startDestination = InterfaceChildClassComp) {}
                    navHost.createGraph(startDestination = AbstractChildClassComp) {}
                    navHost.createGraph(startDestination = TestAbstractComp)
                }
                """
                    )
                    .indented(),
                *NAVIGATION_STUBS,
                TEST_CODE
            )
            .run()
            .expect(
                """
src/com/example/test.kt:9: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestClass(...)?
If the class TestClass does not contain arguments,
you can also pass in its KClass reference TestClass::class [WrongStartDestinationType]
    navHost.createGraph(startDestination = TestClass) {}
                                           ~~~~~~~~~
src/com/example/test.kt:10: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestClassWithArg(...)?
If the class TestClassWithArg does not contain arguments,
you can also pass in its KClass reference TestClassWithArg::class [WrongStartDestinationType]
    navHost.createGraph(startDestination = TestClassWithArg) {}
                                           ~~~~~~~~~~~~~~~~
src/com/example/test.kt:11: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor InnerClass(...)?
If the class InnerClass does not contain arguments,
you can also pass in its KClass reference InnerClass::class [WrongStartDestinationType]
    navHost.createGraph(startDestination = Outer.InnerClass) {}
                                           ~~~~~~~~~~~~~~~~
src/com/example/test.kt:12: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor InterfaceChildClass(...)?
If the class InterfaceChildClass does not contain arguments,
you can also pass in its KClass reference InterfaceChildClass::class [WrongStartDestinationType]
    navHost.createGraph(startDestination = InterfaceChildClass) {}
                                           ~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:13: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor AbstractChildClass(...)?
If the class AbstractChildClass does not contain arguments,
you can also pass in its KClass reference AbstractChildClass::class [WrongStartDestinationType]
    navHost.createGraph(startDestination = AbstractChildClass) {}
                                           ~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:14: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestInterface(...)?
If the class TestInterface does not contain arguments,
you can also pass in its KClass reference TestInterface::class [WrongStartDestinationType]
    navHost.createGraph(startDestination = TestInterface)
                                           ~~~~~~~~~~~~~
src/com/example/test.kt:15: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestAbstract(...)?
If the class TestAbstract does not contain arguments,
you can also pass in its KClass reference TestAbstract::class [WrongStartDestinationType]
    navHost.createGraph(startDestination = TestAbstract)
                                           ~~~~~~~~~~~~
src/com/example/test.kt:16: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    navHost.createGraph(startDestination = TestClassComp) {}
                                           ~~~~~~~~~~~~~
src/com/example/test.kt:17: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    navHost.createGraph(startDestination = TestClassWithArgComp) {}
                                           ~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:18: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    navHost.createGraph(startDestination = OuterComp.InnerClassComp) {}
                                           ~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:19: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    navHost.createGraph(startDestination = InterfaceChildClassComp) {}
                                           ~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:20: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    navHost.createGraph(startDestination = AbstractChildClassComp) {}
                                           ~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:21: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    navHost.createGraph(startDestination = TestAbstractComp)
                                           ~~~~~~~~~~~~~~~~
13 errors, 0 warnings
            """
            )
    }

    override fun getDetector(): Detector = WrongStartDestinationTypeDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(WrongStartDestinationTypeDetector.WrongStartDestinationType)
}
