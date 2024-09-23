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

package androidx.navigation.common.lint

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
class WrongStartDestinationTypeDetectorTest : LintDetectorTest() {

    @Test
    fun testEmptyConstructorNoError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.test.*

                fun createGraph() {
                    val provider = NavigatorProvider()
                    val builder = NavGraphBuilder(provider, TestGraph, null)
                    val navGraph = NavGraph()

                    builder.navigation<TestGraph>(startDestination = TestClass())
                    provider.navigation(startDestination = TestClass()) {}
                    navGraph.setStartDestination(startDestRoute = TestClass())
                    builder.navigation<TestGraph>(startDestination = TestClassComp())
                    provider.navigation(startDestination = TestClassComp()) {}
                    navGraph.setStartDestination(startDestRoute = TestClassComp())
                }
                """
                    )
                    .indented(),
                *STUBS,
            )
            .skipTestModes(TestMode.FULLY_QUALIFIED)
            .run()
            .expectClean()
    }

    @Test
    fun testNavGraphBuilder_classNoErrors() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.test.*

                fun createGraph() {
                    val provider = NavigatorProvider()
                    val builder = NavGraphBuilder(provider, TestGraph, null)
                    builder.navigation<TestGraph>(startDestination = TestClassWithArg(15))
                    builder.navigation<TestGraph>(startDestination = classInstanceRef)
                    builder.navigation<TestGraph>(startDestination = classInstanceWithArgRef)
                    builder.navigation<TestGraph>(startDestination = TestClass::class)
                    builder.navigation<TestGraph>(startDestination = Outer.InnerClass::class)
                    builder.navigation<TestGraph>(startDestination = Outer.InnerClass(15))
                    builder.navigation<TestGraph>(startDestination = InterfaceChildClass(true))
                    builder.navigation<TestGraph>(startDestination = AbstractChildClass(true))
                    builder.navigation<TestGraph>(startDestination = TestClassWithArgComp(15))
                    builder.navigation<TestGraph>(startDestination = TestClassComp::class)
                    builder.navigation<TestGraph>(startDestination = OuterComp.InnerClassComp::class)
                    builder.navigation<TestGraph>(startDestination = OuterComp.InnerClassComp(15))
                    builder.navigation<TestGraph>(startDestination = InterfaceChildClassComp(true))
                    builder.navigation<TestGraph>(startDestination = AbstractChildClassComp(true))
                }
                """
                    )
                    .indented(),
                *STUBS,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testNavGraphBuilder_objectNoError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.test.*

                fun createGraph() {
                    val provider = NavigatorProvider()
                    val builder = NavGraphBuilder(provider, TestGraph, null)
                    builder.navigation<TestGraph>(startDestination = Outer)
                    builder.navigation<TestGraph>(startDestination = Outer.InnerObject)
                    builder.navigation<TestGraph>(startDestination = TestObject)
                    builder.navigation<TestGraph>(startDestination = TestObject::class)
                    builder.navigation<TestGraph>(startDestination = Outer.InnerObject::class)
                    builder.navigation<TestGraph>(startDestination = InterfaceChildObject)
                    builder.navigation<TestGraph>(startDestination = AbstractChildObject)
                    builder.navigation<TestGraph>(startDestination = OuterComp.InnerObject::class)
                    builder.navigation<TestGraph>(startDestination = AbstractChildObjectComp)
                }
                """
                    )
                    .indented(),
                *STUBS,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testNavGraphBuilder_classHasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.test.*

                fun createGraph() {
                    val provider = NavigatorProvider()
                    val builder = NavGraphBuilder(provider, TestGraph, null)
                    builder.navigation<TestGraph>(startDestination = TestClass)
                    builder.navigation<TestGraph>(startDestination = TestClassWithArg)
                    builder.navigation<TestGraph>(startDestination = Outer.InnerClass)
                    builder.navigation<TestGraph>(startDestination = InterfaceChildClass)
                    builder.navigation<TestGraph>(startDestination = AbstractChildClass)
                    builder.navigation<TestGraph>(startDestination = TestInterface)
                    builder.navigation<TestGraph>(startDestination = TestAbstract)
                    // classes with companion object to simulate marked with @Serializable
                    builder.navigation<TestGraph>(startDestination = TestClassComp)
                    builder.navigation<TestGraph>(startDestination = TestClassWithArgComp)
                    builder.navigation<TestGraph>(startDestination = OuterComp.InnerClassComp)
                    builder.navigation<TestGraph>(startDestination = InterfaceChildClassComp)
                    builder.navigation<TestGraph>(startDestination = AbstractChildClassComp)
                    builder.navigation<TestGraph>(startDestination = TestAbstractComp)
                }
                """
                    )
                    .indented(),
                *STUBS,
            )
            .run()
            .expect(
                """
src/com/example/test.kt:9: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestClass(...)?
If the class TestClass does not contain arguments,
you can also pass in its KClass reference TestClass::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = TestClass)
                                                     ~~~~~~~~~
src/com/example/test.kt:10: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestClassWithArg(...)?
If the class TestClassWithArg does not contain arguments,
you can also pass in its KClass reference TestClassWithArg::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = TestClassWithArg)
                                                     ~~~~~~~~~~~~~~~~
src/com/example/test.kt:11: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor InnerClass(...)?
If the class InnerClass does not contain arguments,
you can also pass in its KClass reference InnerClass::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = Outer.InnerClass)
                                                     ~~~~~~~~~~~~~~~~
src/com/example/test.kt:12: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor InterfaceChildClass(...)?
If the class InterfaceChildClass does not contain arguments,
you can also pass in its KClass reference InterfaceChildClass::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = InterfaceChildClass)
                                                     ~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:13: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor AbstractChildClass(...)?
If the class AbstractChildClass does not contain arguments,
you can also pass in its KClass reference AbstractChildClass::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = AbstractChildClass)
                                                     ~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:14: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestInterface(...)?
If the class TestInterface does not contain arguments,
you can also pass in its KClass reference TestInterface::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = TestInterface)
                                                     ~~~~~~~~~~~~~
src/com/example/test.kt:15: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestAbstract(...)?
If the class TestAbstract does not contain arguments,
you can also pass in its KClass reference TestAbstract::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = TestAbstract)
                                                     ~~~~~~~~~~~~
src/com/example/test.kt:17: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = TestClassComp)
                                                     ~~~~~~~~~~~~~
src/com/example/test.kt:18: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = TestClassWithArgComp)
                                                     ~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:19: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = OuterComp.InnerClassComp)
                                                     ~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:20: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = InterfaceChildClassComp)
                                                     ~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:21: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = AbstractChildClassComp)
                                                     ~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:22: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = TestAbstractComp)
                                                     ~~~~~~~~~~~~~~~~
13 errors, 0 warnings
            """
            )
    }

    @Test
    fun testNavProvider_classNoErrors() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.test.*

                fun createGraph() {
                    val provider = NavigatorProvider()
                    provider.navigation(startDestination = TestClassWithArg(15)) {}
                    provider.navigation(startDestination = classInstanceRef) {}
                    provider.navigation(startDestination = classInstanceWithArgRef) {}
                    provider.navigation(startDestination = TestClass::class) {}
                    provider.navigation(startDestination = Outer.InnerClass::class) {}
                    provider.navigation(startDestination = Outer.InnerClass(15)) {}
                    provider.navigation(startDestination = InterfaceChildClass(true)) {}
                    provider.navigation(startDestination = AbstractChildClass(true)) {}
                    provider.navigation(startDestination = TestClassWithArgComp(15)) {}
                    provider.navigation(startDestination = TestClassComp::class) {}
                    provider.navigation(startDestination = OuterComp.InnerClassComp::class) {}
                    provider.navigation(startDestination = OuterComp.InnerClassComp(15)) {}
                    provider.navigation(startDestination = InterfaceChildClassComp(true)) {}
                    provider.navigation(startDestination = AbstractChildClassComp(true)) {}
                }
                """
                    )
                    .indented(),
                *STUBS,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testNavProvider_objectNoError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.test.*

                fun createGraph() {
                    val provider = NavigatorProvider()
                    provider.navigation(startDestination = Outer) {}
                    provider.navigation(startDestination = Outer.InnerObject) {}
                    provider.navigation(startDestination = TestObject) {}
                    provider.navigation(startDestination = TestObject::class) {}
                    provider.navigation(startDestination = Outer.InnerObject::class) {}
                    provider.navigation(startDestination = InterfaceChildObject) {}
                    provider.navigation(startDestination = AbstractChildObject) {}
                    provider.navigation(startDestination = OuterComp) {}
                    provider.navigation(startDestination = OuterComp.InnerObject) {}
                    provider.navigation(startDestination = OuterComp.InnerObject::class) {}
                }
                """
                    )
                    .indented(),
                *STUBS,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testNavProvider_classHasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.test.*

                fun createGraph() {
                    val provider = NavigatorProvider()
                    provider.navigation(startDestination = TestClass) {}
                    provider.navigation(startDestination = TestClassWithArg) {}
                    provider.navigation(startDestination = Outer.InnerClass) {}
                    provider.navigation(startDestination = InterfaceChildClass) {}
                    provider.navigation(startDestination = AbstractChildClass) {}
                    provider.navigation(startDestination = TestInterface)
                    provider.navigation(startDestination = TestAbstract)
                    provider.navigation(startDestination = TestClassComp) {}
                    provider.navigation(startDestination = TestClassWithArgComp) {}
                    provider.navigation(startDestination = OuterComp.InnerClassComp) {}
                    provider.navigation(startDestination = InterfaceChildClassComp) {}
                    provider.navigation(startDestination = AbstractChildClassComp) {}
                    provider.navigation(startDestination = TestAbstractComp)
                }
                """
                    )
                    .indented(),
                *STUBS,
            )
            .run()
            .expect(
                """
src/com/example/test.kt:8: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestClass(...)?
If the class TestClass does not contain arguments,
you can also pass in its KClass reference TestClass::class [WrongStartDestinationType]
    provider.navigation(startDestination = TestClass) {}
                                           ~~~~~~~~~
src/com/example/test.kt:9: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestClassWithArg(...)?
If the class TestClassWithArg does not contain arguments,
you can also pass in its KClass reference TestClassWithArg::class [WrongStartDestinationType]
    provider.navigation(startDestination = TestClassWithArg) {}
                                           ~~~~~~~~~~~~~~~~
src/com/example/test.kt:10: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor InnerClass(...)?
If the class InnerClass does not contain arguments,
you can also pass in its KClass reference InnerClass::class [WrongStartDestinationType]
    provider.navigation(startDestination = Outer.InnerClass) {}
                                           ~~~~~~~~~~~~~~~~
src/com/example/test.kt:11: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor InterfaceChildClass(...)?
If the class InterfaceChildClass does not contain arguments,
you can also pass in its KClass reference InterfaceChildClass::class [WrongStartDestinationType]
    provider.navigation(startDestination = InterfaceChildClass) {}
                                           ~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:12: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor AbstractChildClass(...)?
If the class AbstractChildClass does not contain arguments,
you can also pass in its KClass reference AbstractChildClass::class [WrongStartDestinationType]
    provider.navigation(startDestination = AbstractChildClass) {}
                                           ~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:13: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestInterface(...)?
If the class TestInterface does not contain arguments,
you can also pass in its KClass reference TestInterface::class [WrongStartDestinationType]
    provider.navigation(startDestination = TestInterface)
                                           ~~~~~~~~~~~~~
src/com/example/test.kt:14: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestAbstract(...)?
If the class TestAbstract does not contain arguments,
you can also pass in its KClass reference TestAbstract::class [WrongStartDestinationType]
    provider.navigation(startDestination = TestAbstract)
                                           ~~~~~~~~~~~~
src/com/example/test.kt:15: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    provider.navigation(startDestination = TestClassComp) {}
                                           ~~~~~~~~~~~~~
src/com/example/test.kt:16: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    provider.navigation(startDestination = TestClassWithArgComp) {}
                                           ~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:17: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    provider.navigation(startDestination = OuterComp.InnerClassComp) {}
                                           ~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:18: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    provider.navigation(startDestination = InterfaceChildClassComp) {}
                                           ~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:19: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    provider.navigation(startDestination = AbstractChildClassComp) {}
                                           ~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:20: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    provider.navigation(startDestination = TestAbstractComp)
                                           ~~~~~~~~~~~~~~~~
13 errors, 0 warnings
            """
            )
    }

    @Test
    fun testSetStartDestination_classNoErrors() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.test.*

                fun createGraph() {
                    val navGraph = NavGraph()
                    navGraph.setStartDestination(startDestRoute = TestClassWithArg(15))
                    navGraph.setStartDestination(startDestRoute = classInstanceRef)
                    navGraph.setStartDestination(startDestRoute = classInstanceWithArgRef)
                    navGraph.setStartDestination(startDestRoute = TestClass::class)
                    navGraph.setStartDestination(startDestRoute = Outer.InnerClass::class)
                    navGraph.setStartDestination(startDestRoute = Outer.InnerClass(15))
                    navGraph.setStartDestination(startDestRoute = InterfaceChildClass(true))
                    navGraph.setStartDestination(startDestRoute = AbstractChildClass(true))
                    navGraph.setStartDestination(startDestRoute = TestClassWithArgComp(15))
                    navGraph.setStartDestination(startDestRoute = TestClassComp::class)
                    navGraph.setStartDestination(startDestRoute = OuterComp.InnerClassComp::class)
                    navGraph.setStartDestination(startDestRoute = OuterComp.InnerClassComp(15))
                    navGraph.setStartDestination(startDestRoute = InterfaceChildClassComp(true))
                    navGraph.setStartDestination(startDestRoute = AbstractChildClassComp(true))
                }
                """
                    )
                    .indented(),
                *STUBS,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testSetStartDestination_objectNoError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.test.*

                fun createGraph() {
                    val navGraph = NavGraph()
                    navGraph.setStartDestination(startDestRoute = Outer)
                    navGraph.setStartDestination(startDestRoute = Outer.InnerObject)
                    navGraph.setStartDestination(startDestRoute = TestObject)
                    navGraph.setStartDestination(startDestRoute = TestObject::class)
                    navGraph.setStartDestination(startDestRoute = Outer.InnerObject::class)
                    navGraph.setStartDestination(startDestRoute = InterfaceChildObject)
                    navGraph.setStartDestination(startDestRoute = AbstractChildObject)
                    navGraph.setStartDestination(startDestRoute = OuterComp.InnerObject::class)
                    navGraph.setStartDestination(startDestRoute = AbstractChildObjectComp)
                }
                """
                    )
                    .indented(),
                *STUBS,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testSetStartDestination_classHasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.test.*

                fun createGraph() {
                    val navGraph = NavGraph()
                    navGraph.setStartDestination(startDestRoute = TestClass)
                    navGraph.setStartDestination(startDestRoute = TestClassWithArg)
                    navGraph.setStartDestination(startDestRoute = Outer.InnerClass)
                    navGraph.setStartDestination(startDestRoute = InterfaceChildClass)
                    navGraph.setStartDestination(startDestRoute = AbstractChildClass)
                    navGraph.setStartDestination(startDestRoute = TestInterface)
                    navGraph.setStartDestination(startDestRoute = TestAbstract)
                    // classes with companion object to simulate marked with @Serializable
                    navGraph.setStartDestination(startDestRoute = TestClassComp)
                    navGraph.setStartDestination(startDestRoute = TestClassWithArgComp)
                    navGraph.setStartDestination(startDestRoute = OuterComp.InnerClassComp)
                    navGraph.setStartDestination(startDestRoute = InterfaceChildClassComp)
                    navGraph.setStartDestination(startDestRoute = AbstractChildClassComp)
                    navGraph.setStartDestination(startDestRoute = TestAbstractComp)
                }
                """
                    )
                    .indented(),
                *STUBS,
            )
            .run()
            .expect(
                """
src/com/example/test.kt:8: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestClass(...)?
If the class TestClass does not contain arguments,
you can also pass in its KClass reference TestClass::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = TestClass)
                                                  ~~~~~~~~~
src/com/example/test.kt:9: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestClassWithArg(...)?
If the class TestClassWithArg does not contain arguments,
you can also pass in its KClass reference TestClassWithArg::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = TestClassWithArg)
                                                  ~~~~~~~~~~~~~~~~
src/com/example/test.kt:10: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor InnerClass(...)?
If the class InnerClass does not contain arguments,
you can also pass in its KClass reference InnerClass::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = Outer.InnerClass)
                                                  ~~~~~~~~~~~~~~~~
src/com/example/test.kt:11: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor InterfaceChildClass(...)?
If the class InterfaceChildClass does not contain arguments,
you can also pass in its KClass reference InterfaceChildClass::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = InterfaceChildClass)
                                                  ~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:12: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor AbstractChildClass(...)?
If the class AbstractChildClass does not contain arguments,
you can also pass in its KClass reference AbstractChildClass::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = AbstractChildClass)
                                                  ~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:13: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestInterface(...)?
If the class TestInterface does not contain arguments,
you can also pass in its KClass reference TestInterface::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = TestInterface)
                                                  ~~~~~~~~~~~~~
src/com/example/test.kt:14: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestAbstract(...)?
If the class TestAbstract does not contain arguments,
you can also pass in its KClass reference TestAbstract::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = TestAbstract)
                                                  ~~~~~~~~~~~~
src/com/example/test.kt:16: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = TestClassComp)
                                                  ~~~~~~~~~~~~~
src/com/example/test.kt:17: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = TestClassWithArgComp)
                                                  ~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:18: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = OuterComp.InnerClassComp)
                                                  ~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:19: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = InterfaceChildClassComp)
                                                  ~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:20: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = AbstractChildClassComp)
                                                  ~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:21: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = TestAbstractComp)
                                                  ~~~~~~~~~~~~~~~~
13 errors, 0 warnings
            """
            )
    }

    override fun getDetector(): Detector = WrongStartDestinationTypeDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(WrongStartDestinationTypeDetector.WrongStartDestinationType)
}

private val STUBS = arrayOf(TEST_CLASS.bytecode, *NAVIGATION_STUBS)
