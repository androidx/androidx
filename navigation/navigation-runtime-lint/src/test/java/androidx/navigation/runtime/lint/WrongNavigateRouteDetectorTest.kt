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
class WrongNavigateRouteDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = WrongNavigateRouteDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(WrongNavigateRouteDetector.WrongNavigateRouteType)

    @Test
    fun testEmptyConstructorNoError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.NavController
                import androidx.test.*

                fun createGraph() {
                    val navController = NavController()
                    navController.navigate(route = TestClass())
                    navController.navigate(route = TestClassComp())
                }
                """
                    )
                    .indented(),
                *NAVIGATION_STUBS,
                TEST_CLASS.bytecode
            )
            .skipTestModes(TestMode.FULLY_QUALIFIED)
            .run()
            .expectClean()
    }

    @Test
    fun testNoError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.NavController
                import androidx.test.*

                fun createGraph() {
                    val navController = NavController()
                    navController.navigate(route = TestClassWithArg(10))
                    navController.navigate(route = TestObject)
                    navController.navigate(route = classInstanceRef)
                    navController.navigate(route = classInstanceWithArgRef)
                    navController.navigate(route = Outer)
                    navController.navigate(route = Outer.InnerObject)
                    navController.navigate(route = Outer.InnerClass(123))
                    navController.navigate(route = 123)
                    navController.navigate(route = "www.test.com/{arg}")
                    navController.navigate(route = InterfaceChildClass(true))
                    navController.navigate(route = InterfaceChildObject)
                    navController.navigate(route = AbstractChildClass(true))
                    navController.navigate(route = AbstractChildObject)
                    //classes with companion object to simulate marked with @Serializable
                    navController.navigate(route = TestClassWithArgComp(15))
                    navController.navigate(route = OuterComp.InnerClassComp(15))
                    navController.navigate(route = InterfaceChildClassComp(true))
                    navController.navigate(route = AbstractChildClassComp(true))
                }
                """
                    )
                    .indented(),
                *NAVIGATION_STUBS,
                TEST_CLASS.bytecode
            )
            .skipTestModes(TestMode.FULLY_QUALIFIED)
            .run()
            .expectClean()
    }

    @Test
    fun testHasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.NavController
                import androidx.test.*

                fun createGraph() {
                    val navController = NavController()
                    navController.navigate(route = TestClass)
                    navController.navigate(route = TestClass::class)
                    navController.navigate(route = TestClassWithArg)
                    navController.navigate(route = TestClassWithArg::class)
                    navController.navigate(route = TestInterface)
                    navController.navigate(route = TestInterface::class)
                    navController.navigate(route = InterfaceChildClass)
                    navController.navigate(route = InterfaceChildClass::class)
                    navController.navigate(route = TestAbstract)
                    navController.navigate(route = TestAbstract::class)
                    navController.navigate(route = AbstractChildClass)
                    navController.navigate(route = AbstractChildClass::class)
                    navController.navigate(route = InterfaceChildClass::class)
                    navController.navigate(route = Outer.InnerClass)
                    navController.navigate(route = Outer.InnerClass::class)

                    //classes with companion object to simulate marked with @Serializable
                    navController.navigate(route = TestClassComp)
                    navController.navigate(route = TestClassComp::class)
                    navController.navigate(route = TestClassWithArgComp)
                    navController.navigate(route = TestClassWithArgComp::class)
                    navController.navigate(route = OuterComp.InnerClassComp)
                    navController.navigate(route = OuterComp.InnerClassComp::class)
                    navController.navigate(route = InterfaceChildClassComp)
                    navController.navigate(route = InterfaceChildClassComp::class)
                    navController.navigate(route = AbstractChildClassComp)
                    navController.navigate(route = AbstractChildClassComp::class)
                    navController.navigate(route = TestAbstractComp)
                    navController.navigate(route = TestAbstractComp::class)
                }
                """
                    )
                    .indented(),
                *NAVIGATION_STUBS,
                TEST_CLASS.bytecode
            )
            .skipTestModes(TestMode.FULLY_QUALIFIED)
            .run()
            .expect(
                """
src/com/example/test.kt:8: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestClass)
                                   ~~~~~~~~~
src/com/example/test.kt:9: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestClass::class)
                                   ~~~~~~~~~~~~~~~~
src/com/example/test.kt:10: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestClassWithArg)
                                   ~~~~~~~~~~~~~~~~
src/com/example/test.kt:11: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestClassWithArg::class)
                                   ~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:12: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestInterface)
                                   ~~~~~~~~~~~~~
src/com/example/test.kt:13: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestInterface::class)
                                   ~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:14: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = InterfaceChildClass)
                                   ~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:15: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = InterfaceChildClass::class)
                                   ~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:16: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestAbstract)
                                   ~~~~~~~~~~~~
src/com/example/test.kt:17: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestAbstract::class)
                                   ~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:18: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = AbstractChildClass)
                                   ~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:19: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = AbstractChildClass::class)
                                   ~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:20: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = InterfaceChildClass::class)
                                   ~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:21: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = Outer.InnerClass)
                                   ~~~~~~~~~~~~~~~~
src/com/example/test.kt:22: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = Outer.InnerClass::class)
                                   ~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:25: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestClassComp)
                                   ~~~~~~~~~~~~~
src/com/example/test.kt:26: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestClassComp::class)
                                   ~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:27: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestClassWithArgComp)
                                   ~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:28: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestClassWithArgComp::class)
                                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:29: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = OuterComp.InnerClassComp)
                                   ~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:30: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = OuterComp.InnerClassComp::class)
                                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:31: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = InterfaceChildClassComp)
                                   ~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:32: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = InterfaceChildClassComp::class)
                                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:33: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = AbstractChildClassComp)
                                   ~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:34: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = AbstractChildClassComp::class)
                                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:35: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestAbstractComp)
                                   ~~~~~~~~~~~~~~~~
src/com/example/test.kt:36: Error: The route should be a destination class instance or destination object. [WrongNavigateRouteType]
    navController.navigate(route = TestAbstractComp::class)
                                   ~~~~~~~~~~~~~~~~~~~~~~~
27 errors, 0 warnings
                """
            )
    }
}
