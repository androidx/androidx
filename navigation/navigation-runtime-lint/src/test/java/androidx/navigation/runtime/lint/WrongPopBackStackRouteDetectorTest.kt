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
class WrongPopBackStackRouteDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = WrongPopBackStackRouteDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(WrongPopBackStackRouteDetector.WrongPopBackStackRouteType)

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
                    navController.popBackStack(route = TestClass(), false)
                    navController.popBackStack(route = TestClassComp(), false)
                }
                """
                    )
                    .indented(),
                *STUBS
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
                    navController.popBackStack(route = TestClassWithArg(10), false)
                    navController.popBackStack(route = TestObject, false)
                    navController.popBackStack(route = classInstanceRef, false)
                    navController.popBackStack(route = classInstanceWithArgRef, false)
                    navController.popBackStack(route = Outer, false)
                    navController.popBackStack(route = Outer.InnerObject, false)
                    navController.popBackStack(route = Outer.InnerClass(123), false)
                    navController.popBackStack(route = 123, false)
                    navController.popBackStack(route = "www.test.com/{arg}", false)
                    navController.popBackStack(route = InterfaceChildClass(true), false)
                    navController.popBackStack(route = InterfaceChildObject, false)
                    navController.popBackStack(route = AbstractChildClass(true), false)
                    navController.popBackStack(route = AbstractChildObject, false)
                    //classes with companion object to simulate marked with @Serializable
                    navController.popBackStack(route = TestClassWithArgComp(15), false)
                    navController.popBackStack(route = OuterComp.InnerClassComp(15), false)
                    navController.popBackStack(route = InterfaceChildClassComp(true), false)
                    navController.popBackStack(route = AbstractChildClassComp(true), false)
                }
                """
                    )
                    .indented(),
                *STUBS
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
                    navController.popBackStack(route = TestClass, false)
                    navController.popBackStack(route = TestClassWithArg, false)
                    navController.popBackStack(route = TestInterface, false)
                    navController.popBackStack(route = InterfaceChildClass, false)
                    navController.popBackStack(route = TestAbstract, false)
                    navController.popBackStack(route = AbstractChildClass, false)
                    navController.popBackStack(route = Outer.InnerClass, false)

                    //classes with companion object to simulate marked with @Serializable
                    navController.popBackStack(route = TestClassComp, false)
                    navController.popBackStack(route = TestClassWithArgComp, false)
                    navController.popBackStack(route = OuterComp.InnerClassComp, false)
                    navController.popBackStack(route = InterfaceChildClassComp, false)
                    navController.popBackStack(route = AbstractChildClassComp, false)
                    navController.popBackStack(route = TestAbstractComp, false)
                }
                """
                    )
                    .indented(),
                *STUBS
            )
            .skipTestModes(TestMode.FULLY_QUALIFIED)
            .run()
            .expect(
                """
src/com/example/test.kt:17: Error: Use popBackStack with reified class instead. [WrongPopBackStackRouteType]
    navController.popBackStack(route = TestClassComp, false)
                                       ~~~~~~~~~~~~~
src/com/example/test.kt:18: Error: Use popBackStack with reified class instead. [WrongPopBackStackRouteType]
    navController.popBackStack(route = TestClassWithArgComp, false)
                                       ~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:19: Error: Use popBackStack with reified class instead. [WrongPopBackStackRouteType]
    navController.popBackStack(route = OuterComp.InnerClassComp, false)
                                       ~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:20: Error: Use popBackStack with reified class instead. [WrongPopBackStackRouteType]
    navController.popBackStack(route = InterfaceChildClassComp, false)
                                       ~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:21: Error: Use popBackStack with reified class instead. [WrongPopBackStackRouteType]
    navController.popBackStack(route = AbstractChildClassComp, false)
                                       ~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:22: Error: Use popBackStack with reified class instead. [WrongPopBackStackRouteType]
    navController.popBackStack(route = TestAbstractComp, false)
                                       ~~~~~~~~~~~~~~~~
6 errors, 0 warnings
                """
            )
            .expectFixDiffs(
                """
Autofix for src/com/example/test.kt line 17: Use popBackStack with reified class instead.:
@@ -17 +17
-     navController.popBackStack(route = TestClassComp, false)
+     navController.popBackStack<TestClassComp>(false)
Autofix for src/com/example/test.kt line 18: Use popBackStack with reified class instead.:
@@ -18 +18
-     navController.popBackStack(route = TestClassWithArgComp, false)
+     navController.popBackStack<TestClassWithArgComp>(false)
Autofix for src/com/example/test.kt line 19: Use popBackStack with reified class instead.:
@@ -19 +19
-     navController.popBackStack(route = OuterComp.InnerClassComp, false)
+     navController.popBackStack<OuterComp.InnerClassComp>(false)
Autofix for src/com/example/test.kt line 20: Use popBackStack with reified class instead.:
@@ -20 +20
-     navController.popBackStack(route = InterfaceChildClassComp, false)
+     navController.popBackStack<InterfaceChildClassComp>(false)
Autofix for src/com/example/test.kt line 21: Use popBackStack with reified class instead.:
@@ -21 +21
-     navController.popBackStack(route = AbstractChildClassComp, false)
+     navController.popBackStack<AbstractChildClassComp>(false)
Autofix for src/com/example/test.kt line 22: Use popBackStack with reified class instead.:
@@ -22 +22
-     navController.popBackStack(route = TestAbstractComp, false)
+     navController.popBackStack<TestAbstractComp>(false)
                """
                    .trimIndent()
            )
    }
}
