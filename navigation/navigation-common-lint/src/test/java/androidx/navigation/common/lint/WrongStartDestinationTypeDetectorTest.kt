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

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.compiled
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class WrongStartDestinationTypeDetectorTest(private val testFile: TestFile) : LintDetectorTest() {

    private companion object {
        @JvmStatic @Parameterized.Parameters public fun data() = listOf(SOURCECODE, BYTECODE)
    }

    @Test
    fun testEmptyConstructorNoError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*

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
                testFile,
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
                testFile,
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
                testFile,
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
                testFile,
            )
            .run()
            .expect(
                """
src/com/example/test.kt:8: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestClass(...)?
If the class TestClass does not contain arguments,
you can also pass in its KClass reference TestClass::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = TestClass)
                                                     ~~~~~~~~~
src/com/example/test.kt:9: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestClassWithArg(...)?
If the class TestClassWithArg does not contain arguments,
you can also pass in its KClass reference TestClassWithArg::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = TestClassWithArg)
                                                     ~~~~~~~~~~~~~~~~
src/com/example/test.kt:10: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor InnerClass(...)?
If the class InnerClass does not contain arguments,
you can also pass in its KClass reference InnerClass::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = Outer.InnerClass)
                                                     ~~~~~~~~~~~~~~~~
src/com/example/test.kt:11: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor InterfaceChildClass(...)?
If the class InterfaceChildClass does not contain arguments,
you can also pass in its KClass reference InterfaceChildClass::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = InterfaceChildClass)
                                                     ~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:12: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor AbstractChildClass(...)?
If the class AbstractChildClass does not contain arguments,
you can also pass in its KClass reference AbstractChildClass::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = AbstractChildClass)
                                                     ~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:13: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestInterface(...)?
If the class TestInterface does not contain arguments,
you can also pass in its KClass reference TestInterface::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = TestInterface)
                                                     ~~~~~~~~~~~~~
src/com/example/test.kt:14: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestAbstract(...)?
If the class TestAbstract does not contain arguments,
you can also pass in its KClass reference TestAbstract::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = TestAbstract)
                                                     ~~~~~~~~~~~~
src/com/example/test.kt:16: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = TestClassComp)
                                                     ~~~~~~~~~~~~~
src/com/example/test.kt:17: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = TestClassWithArgComp)
                                                     ~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:18: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = OuterComp.InnerClassComp)
                                                     ~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:19: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = InterfaceChildClassComp)
                                                     ~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:20: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    builder.navigation<TestGraph>(startDestination = AbstractChildClassComp)
                                                     ~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:21: Error: StartDestination should not be a simple class name reference.
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
                testFile,
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
                testFile,
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
                testFile,
            )
            .run()
            .expect(
                """
src/com/example/test.kt:7: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestClass(...)?
If the class TestClass does not contain arguments,
you can also pass in its KClass reference TestClass::class [WrongStartDestinationType]
    provider.navigation(startDestination = TestClass) {}
                                           ~~~~~~~~~
src/com/example/test.kt:8: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestClassWithArg(...)?
If the class TestClassWithArg does not contain arguments,
you can also pass in its KClass reference TestClassWithArg::class [WrongStartDestinationType]
    provider.navigation(startDestination = TestClassWithArg) {}
                                           ~~~~~~~~~~~~~~~~
src/com/example/test.kt:9: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor InnerClass(...)?
If the class InnerClass does not contain arguments,
you can also pass in its KClass reference InnerClass::class [WrongStartDestinationType]
    provider.navigation(startDestination = Outer.InnerClass) {}
                                           ~~~~~~~~~~~~~~~~
src/com/example/test.kt:10: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor InterfaceChildClass(...)?
If the class InterfaceChildClass does not contain arguments,
you can also pass in its KClass reference InterfaceChildClass::class [WrongStartDestinationType]
    provider.navigation(startDestination = InterfaceChildClass) {}
                                           ~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:11: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor AbstractChildClass(...)?
If the class AbstractChildClass does not contain arguments,
you can also pass in its KClass reference AbstractChildClass::class [WrongStartDestinationType]
    provider.navigation(startDestination = AbstractChildClass) {}
                                           ~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:12: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestInterface(...)?
If the class TestInterface does not contain arguments,
you can also pass in its KClass reference TestInterface::class [WrongStartDestinationType]
    provider.navigation(startDestination = TestInterface)
                                           ~~~~~~~~~~~~~
src/com/example/test.kt:13: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestAbstract(...)?
If the class TestAbstract does not contain arguments,
you can also pass in its KClass reference TestAbstract::class [WrongStartDestinationType]
    provider.navigation(startDestination = TestAbstract)
                                           ~~~~~~~~~~~~
src/com/example/test.kt:14: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    provider.navigation(startDestination = TestClassComp) {}
                                           ~~~~~~~~~~~~~
src/com/example/test.kt:15: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    provider.navigation(startDestination = TestClassWithArgComp) {}
                                           ~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:16: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    provider.navigation(startDestination = OuterComp.InnerClassComp) {}
                                           ~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:17: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    provider.navigation(startDestination = InterfaceChildClassComp) {}
                                           ~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:18: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    provider.navigation(startDestination = AbstractChildClassComp) {}
                                           ~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:19: Error: StartDestination should not be a simple class name reference.
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
                testFile,
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
                testFile,
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
                testFile,
            )
            .run()
            .expect(
                """
src/com/example/test.kt:7: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestClass(...)?
If the class TestClass does not contain arguments,
you can also pass in its KClass reference TestClass::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = TestClass)
                                                  ~~~~~~~~~
src/com/example/test.kt:8: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestClassWithArg(...)?
If the class TestClassWithArg does not contain arguments,
you can also pass in its KClass reference TestClassWithArg::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = TestClassWithArg)
                                                  ~~~~~~~~~~~~~~~~
src/com/example/test.kt:9: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor InnerClass(...)?
If the class InnerClass does not contain arguments,
you can also pass in its KClass reference InnerClass::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = Outer.InnerClass)
                                                  ~~~~~~~~~~~~~~~~
src/com/example/test.kt:10: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor InterfaceChildClass(...)?
If the class InterfaceChildClass does not contain arguments,
you can also pass in its KClass reference InterfaceChildClass::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = InterfaceChildClass)
                                                  ~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:11: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor AbstractChildClass(...)?
If the class AbstractChildClass does not contain arguments,
you can also pass in its KClass reference AbstractChildClass::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = AbstractChildClass)
                                                  ~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:12: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestInterface(...)?
If the class TestInterface does not contain arguments,
you can also pass in its KClass reference TestInterface::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = TestInterface)
                                                  ~~~~~~~~~~~~~
src/com/example/test.kt:13: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor TestAbstract(...)?
If the class TestAbstract does not contain arguments,
you can also pass in its KClass reference TestAbstract::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = TestAbstract)
                                                  ~~~~~~~~~~~~
src/com/example/test.kt:15: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = TestClassComp)
                                                  ~~~~~~~~~~~~~
src/com/example/test.kt:16: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = TestClassWithArgComp)
                                                  ~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:17: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = OuterComp.InnerClassComp)
                                                  ~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:18: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = InterfaceChildClassComp)
                                                  ~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:19: Error: StartDestination should not be a simple class name reference.
Did you mean to call its constructor Companion(...)?
If the class Companion does not contain arguments,
you can also pass in its KClass reference Companion::class [WrongStartDestinationType]
    navGraph.setStartDestination(startDestRoute = AbstractChildClassComp)
                                                  ~~~~~~~~~~~~~~~~~~~~~~
src/com/example/test.kt:20: Error: StartDestination should not be a simple class name reference.
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

private val SOURCECODE =
    kotlin(
            """
package androidx.navigation

import kotlin.reflect.KClass
import kotlin.reflect.KType

// NavGraphBuilder
public inline fun <reified T : Any> NavGraphBuilder.navigation(
    startDestination: Any,
) { }

// NavGraph
public open class NavGraph: NavDestination() {
    public fun <T : Any> setStartDestination(startDestRoute: T) {}
}

// NavDestinationBuilder
public open class NavDestinationBuilder<out D : NavDestination>
public open class NavDestination

// NavigatorProvider
public open class NavigatorProvider

public open class NavGraphBuilder : NavDestinationBuilder<NavGraph> {
    public constructor(
        provider: NavigatorProvider,
        startDestination: Any,
        route: KClass<*>?,
    )
}

public inline fun NavigatorProvider.navigation(
    startDestination: Any,
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
): NavGraph = NavGraph()

// source code for classes
val classInstanceRef = TestClass()

val classInstanceWithArgRef = TestClassWithArg(15)

val innerClassInstanceRef = Outer.InnerClass(15)

object TestGraph

object TestObject

class TestClass

class TestClassWithArg(val arg: Int)

object Outer {
    data object InnerObject

    data class InnerClass (
        val innerArg: Int,
    )
}

interface TestInterface
class InterfaceChildClass(val arg: Boolean): TestInterface
object InterfaceChildObject: TestInterface

abstract class TestAbstract
class AbstractChildClass(val arg: Boolean): TestAbstract()
object AbstractChildObject: TestAbstract()

// classes with companion object to simulate classes marked with @Serializable
class TestClassComp { companion object }

class TestClassWithArgComp(val arg: Int) { companion object }

object OuterComp {
    data object InnerObject

    data class InnerClassComp (
        val innerArg: Int,
    ) { companion object }
}

class InterfaceChildClassComp(val arg: Boolean): TestInterface { companion object }

abstract class TestAbstractComp { companion object }
class AbstractChildClassComp(val arg: Boolean): TestAbstractComp() { companion object }
object AbstractChildObjectComp: TestAbstractComp()
"""
        )
        .indented()

// Stub
private val BYTECODE =
    compiled(
        "libs/StartDestinationLint.jar",
        SOURCECODE,
        0xbfec49d0,
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgUuQSTsxLKcrPTKnQy0ssy0xPLMnM
                zxPi8ksscy9KLMjwLuES5eJOzs/VS61IzC3ISRViC0ktLvEuUWLQYgAAL/WN
                klIAAAA=
                """,
        """
                androidx/navigation/AbstractChildClass.class:
                H4sIAAAAAAAA/41Qz28SQRT+ZnZZYAFZ8BelamttDeUgtPGmaaQkGhLsoTYc
                4DSwG5gAu2ZnID3yt3j2YqIx8WCIR/8o45ulGg8cPMx773vz5XvvfT9/ffsO
                4DmOGJ6K0I8j6V83QrGUY6FlFDZaQ6VjMdLtiZz57ZlQKg3GsL+NexUo/Yef
                hsXgvJSh1GcMdq1/3GOwase9PFJIu7CRISziMQPr5+EilwVHnqh6IhVDrft/
                27ygKeNAt4wQyfcZSt1ppGcybLwNtPCFFkTh86VFZzITsiaAxk6pfy0NalLl
                nzCcr1dll1d48tYrl3s5l2esynp1ypvsvFB2PF7lTevHB4d79mXpL8oQu2pn
                Up5jlE4ZDrau/69BtBUtkbsQyzexeD95NtV0ezvyA4ZiV4bBxWI+DOIrMZxR
                p9yNRmLWE7E0+KbpvosW8Sh4LQ3YuVyEWs6DnlSSflthGOlkqMIJGWsnJ5eN
                z1RxqlNwKO4TOiPMKbv1r8jWd7+g8CnhPKZoOEALBxTvbVi4haJxkCqjRobD
                o7fRahhjKafqn1H4uFUmvyHcyHA8SeIeDim/SpZM4fYAVgd3OrjbobH3qUSl
                gx1UB2AKu3gwQFqhqPBQwVV4pOAoeAql3yqWmbDQAgAA
                """,
        """
                androidx/navigation/AbstractChildClassComp＄Companion.class:
                H4sIAAAAAAAA/51Sy24TQRCsmXX82BhwEh4274eRkkhk4yjiEoQUjECWnCCF
                yJcc0Hh3iMfenY1mx1aOPvEh/EFOSByQlSMfhehZG7gCl57uqq7unZr9/uPr
                NwC7eMqwK3RkUhWdB1pM1KmwKtXBfj+zRoS2PVBx1I5FlrXT5KzpgtDUUAJj
                qA3FRASx0KfBu/5QhrYEj6H4QmllXzJ46xu9KpZQ9FFAiaFgBypjeN79n4V7
                DK317ii1sdLBcJIESltptIiD1/KjGMe2nWqaMA5tag6EGUmzt9Hzwd3itWb4
                h/yQ5CzD1r9NY1j5JTiQVkTCCsJ4MvHISOZCxQUwsBHh58pV25RFLYbmbOr7
                vM59XqNsNi1ffvLqs+kO32avSmV++bnIa9z17jA3YfPvHSrhDkPlt00My4di
                8taIs8HWyJLn7TSSDNe6SsvDcdKX5lj0Y0JWu2ko4p4wytULsNrRWpp8tqSX
                8t+nYxPKN8pxjaOxtiqRPZUpat7XOrX5h2Vokc0Fd3c6uXtwusIDqgJnBp1L
                m19QvsjphxSLOXiMRxSr8wZU4AM1RtnyQvyMTr4QVy9yY53g5hycC/LsCq4S
                5+ExVX4uuot7aOBJvvA+mvmPTh5Qb+0EXgcrHax2sIbrlOJGh2beOgHLUEeD
                +Ax+htsZij8BRr5n4CUDAAA=
                """,
        """
                androidx/navigation/AbstractChildClassComp.class:
                H4sIAAAAAAAA/5VSS08TURT+7vQ1HYqUiljABwpiqcgUQlwIIcEaSZPSBZIm
                wuq2Hcul0ztk7m3Dkt/i2g1RQ6KJIS79UcZzpxUSZYGLOeeeM9/5zvPnr6/f
                AaxhlaHIZSsMROvElbwv2lyLQLpbDaVD3tTlQ+G3yj5Xqhx0j1NgDPPX4fc8
                pS9jImSMIbkhpNCbDPHC/mKdIVZYrGeQQMpBHDbZPGwzsP0MHIykYSFDUH0o
                FMNS9eZVrVOmtqe3DBml2GewN5r+MPXazXnmjeCSACncZlgpVDuBJh73qN91
                hdReKLnvvvbe855PTUri6DV1EO7wsOOF64Pe7jiYwCRD+pKM4cV/NHNVxHoG
                eUyZsUwzzFWDsO0eeboRciGVy6UMdMSj3Fqgaz3fpzGM/6l4x9O8xTUnn9Xt
                x2jVzIi0EaCRd8h/IoxVoldrhWH74jTnWHkr+i5OHSs74lh2PH9xOptatUrs
                JUu9Gs0ls9a0VYr9+JC0svHd8UvLppDpuJ3IJg0dHdXCtS3/fSVUHlUzUuP9
                7ZAfHy53NMPMbk9q0fUqsi+UaPje1lWrdB7loOUxjFWF9Gq9bsML9zhhGHLV
                oMn9Og+FsYfOTEVKL4xm61Gw8zbohU3vjTD/poZ56v9kwQrNPE6zsTBlVkAl
                PiMrSfoe6Zy5VtIxshORd4msTUJbpJ3iOdLFmS8YPYsYng8jgT0sk5wcoHAL
                Y2YX9DJstDpk6RtwuWZFpBPFzxj9eC1NZgAY0thUVGoYnEe0ZGS+YeIdO8fd
                T5g5izwxIjYJGV2oFTVWiriL1DBQJv99YnxwgFgFDyuYreARHtMTcxXM48kB
                mMICnh7AVhhTKCg4CosKSYWswrhC/jfYorpPVwQAAA==
                """,
        """
                androidx/navigation/AbstractChildObject.class:
                H4sIAAAAAAAA/41Sy27TQBQ9M0kcxwm0lEcTCrS0SFAWuK3YUSGlESBLxkg0
                ioS6Gj/UTOPYyDOJusyKD+EPKhaVQEIR7PgoxB0THhJd4JHvnXPnzLkzx/72
                /eNnAI9xj+G+yOIil/Gpm4mpPBZa5pnbDZUuRKR7Q5nGr8KTJNJ1MIaNi8j9
                ROlfG+qoMFj7MpP6KUPlwfaghRosB1XUGap6KBXDtv+fPZ8w2PtRWqo54EbC
                9oLDfjfoPWvhEpwGFS8zbPl5ceyeJDoshMyUK7Is16WqcoNcB5M0Jakr/ijX
                JOa+TLSIhRZU4+NphZxgJjRMAAMbUf1UGrRDs3iXGsxnjsPbvHznM/vrO96e
                z/b4Djuo2/zLe4svc0PdY9i88HJ/e0Rtm4GYvijE2+GjkWZYez3JtBwnXjaV
                SoZp0v1zfjKtl8cJw5IvsySYjMOk6AviMKz4eSTSgSikwYuic5hPiih5Lg3o
                LIQH/8hil5yrltftGCMp3yFkUV6mzGnUSrROyDWmUK49PId9Vi5vLMjAAe5S
                bP0koEFSgI3m782rxDZP8xP4m3O0PmDprCxwbJbxNrbKf5E+EAmsHKHi4aqH
                ax6u4wZNseqhjc4RmMJNrNG6gqNwS8H6AX2Z6w7IAgAA
                """,
        """
                androidx/navigation/AbstractChildObjectComp.class:
                H4sIAAAAAAAA/5VSW2sTQRT+ZnLbbKKt9dLEem8RL+i2xTeLEIPKwrqCjQHp
                02x2aabZzJSdSehjnvwh/oPiQ0FBgr75o8Sza6igfXGXPbf5znc43+yPn5+/
                AniCDYaHQsWZlvGRp8RU7gsrtfI6kbGZGNjuUKbxm+ggoVCPD2tgDBtnNfQS
                Y0+bCmSJobojlbTPGEr37vebqKDqoowaQ9kOpWF4FPzH7KcMzs4gLRhd8JzG
                8cPdXifsvmjiHNw6Fc8zrAc62/cOEhtlQirjCaW0LZiNF2obTtKUqC4EI22J
                zHudWBELK6jGx9MSqcJyU88NGNiI6kcyzzYpirdowHzmurzFi28+c75/4K35
                bJtvsuc1h3/7WOXLPIduM9w9c8G/taLRjVBMX2XicPh4ZBnW3k6UlePEV1Np
                ZJQmnT87kHhdHScMS4FUSTgZR0nWE4RhWAn0QKR9kck8XxTdXT3JBslLmSft
                BXH/H1pskXrlYuV2Lib5G5RVyS+T5/RWiuwmZV4uDPnKgxM4x8XxrQUYeIfb
                ZJu/AagTFeCgcdq8Suj8aXwBf3+C5icsHRcFjjuFvY714t+kSyKClT2UfFz0
                ccnHZVyhEKs+WmjvgRlcxRqdG7gG1wyqvwCOLhDu2AIAAA==
                """,
        """
                androidx/navigation/InterfaceChildClass.class:
                H4sIAAAAAAAA/41Qz28SQRT+ZhZYWKgsVCul/qpVWzi4tPGmNrZNNCRYk9pw
                gNPArjBl2TU7A+mRv8WzFxONiQdDPPpHGd9Q0sSEg4d5733vffO9H7///PgJ
                4Bl2GXZF5Cex9C+9SEzlQGgZR14z0kHyQfSDk6EM/ZNQKGWDMbgXYiq8UEQD
                713vIuhrGxbD9iqJ80DpaxkbaYbMCxlJfciQ2uvU2gzWXq1dgI2cgxQcwiIZ
                MLBOAQWs5cBxg6h6KBVDrfWfUz6nNoNAHxkl0u8wlFqjWIcy8t4GWvhCC6Lw
                8dSi/ZkxOWNAfUeUv5QGNSjy9xmO57Oywyt88eYzh7t5h2etynx2wBvseK2c
                cXmVN6xfnzLcTZ2VrlGW2NVUNu1mjNIBw87K+f85EY1FU+RPxfRNIj4On440
                bX8S+wFDsSWj4HQy7gXJueiFlCm34r4I2yKRBi+Tzvt4kvSD19KAzbNJpOU4
                aEslqXoURbFedFXYp9OmqFeGXtncmlbmFNvIkn1I6JAwJ+/UvyNf3/qG4pcF
                Z4es+QW8xCOyG1csuCiZI1Jk1OjmpLu+1PLMbcmn619R/LxSpnBFWMpwPF7Y
                bTwh/4pqN6l2qwuriY0mbjdRwSaFqDaxhTtdMIW7uNeFrVBSuK9QUHigkFUo
                K6z/BWao/b3sAgAA
                """,
        """
                androidx/navigation/InterfaceChildClassComp＄Companion.class:
                H4sIAAAAAAAA/51Sy27TQBQ9M07zcAO4LY+E9yNILYi6qUAsipAgCGQpbcVD
                2XSBJva0mcQeV/Yk6jIrPoQ/6AqJBYq65KMQd5wA67K5c+8599zrOeOfv77/
                APAUDxmeCR1lqYpOfC0m6kgYlWo/0EZmhyKUnYGKo04s8ryTJsctG4SmjgoY
                gzcUE+HHQh/5+/2hDE0FDkP5hdLKvGRw1jd6dSyh7KKECkPJDFTO8Lz7Xxt3
                GNrr3VFqYqX94STxlVVoEftv5KEYx6aT6txk49Ck2a7IRjLb2ei54HbzWiv8
                R35OCpZh83zTGFb+CHalEZEwgjCeTByyktlQswEMbET4ibLVFmVRm6E1m7ou
                b3CXe5TNptWzL05jNt3mW+x1pcrPvpa5x23vNrMTHp/DogpuMNT++sSwvCcm
                7zJxPNgcGXK9k0aS4VJXabk3Tvoy+yT6MSGr3TQUcU9kytYLsB5oLbNitqS3
                cj+m4yyUb5Xlmh/G2qhE9lSuqPmV1qkpvixHm3wu2cvTye2T0x3uUOVbN+hc
                evQN1dOCvkuxXIDvcY9ifd6AGlzAY5QtL8RP6OQLcf20cNYKrs7BuaDILuAi
                cQ7uU+UWopu4hSYeFAtvo1X86+QB9XoHcAKsBFgNsIbLlOJKQDOvHYDlaKBJ
                fA43x/Uc5d8GoYhiKAMAAA==
                """,
        """
                androidx/navigation/InterfaceChildClassComp.class:
                H4sIAAAAAAAA/5VS308TQRD+9lp67VGkLYql+AMEtRThADEmQkiwRtKk1Iik
                ifC0bZey7XWP3G0bHvlbfPaFqCHRxBAf/aOMs6XCgyaGh5vZmZv55psfP399
                /Q5gFasM81w1Al82jl3Fe7LJtfSVW1JaBAe8LoqH0msUPR6GRb9zZIMxpFq8
                x12Pq6b7ptYSdW0jwjD9L5hdEepLKBtDDLF1qaTeYIjm9+aqDJH8XDUJGwkH
                UThk86DJwPaSSGIkAQs3KFQfypBhoXwNpmtUqin0pkGjGnsM8fW6N6j97BpA
                s0ZwRRE2bjEs58ttXxOQ2+p1XGlyFPfcV+KAdz1d9FWog25d+8E2D9oiWLvo
                7raDcWQZEpdgDM+v084Vi7Ukcpg0k7nDMFP2g6bbEroWcKlClyvl6z5Q6FZ8
                Xel6Hg0i/YfyttC8wTUnn9XpRegCmBEJI0BTb5P/WBpriV6NZYat85OMY2Wt
                /nd+4lipYceKR7PnJ1P2irXEXjD75UgmlrJy1lLkx4eYlYrupC+tOKXkovGh
                VMzArRi+/70S4kZUhiu8txXwo8PFtmaY3OkqLTuipHoylDVPbF71SedR9BuC
                YbQslah0OzUR7HKKYciU/Tr3qjyQxh44kyWlRNAfrKBk553fDeritTT/JgZ1
                qn9VwTINPErEYqQnzAbovUCDipG+Rzpj7pV0hGwbcZKLZG1QtEXaKZxhuDD5
                BaOnZFlwB5nAWyyRHL+IQgppswp6GTTaHOGODbBcsyHSQ4XPGP34T5jkRcAA
                Jo6bSAySs+jvGMlvGH/PzjDxCXdP+54ItWYKsj6JHDW30sd+gqeki+S/T4hT
                +4iUMF3CgxJmMEtPPCzhER7vg4XIY24f8RDpEIUQyRDzoTEzIcZC5H4Dj/nZ
                kW0EAAA=
                """,
        """
                androidx/navigation/InterfaceChildObject.class:
                H4sIAAAAAAAA/41STW/TQBB9u0kTxwk0LV8JBUopoNIDbituVJVKBMhSMBKN
                IqGeNvaSbOKskb2JesyJH8I/qDhUAglFcONHIWZNKAeQwNbOzHs783Zn7G/f
                P34G8Aj3GbaEjtJERSeeFlPVF0Yl2vO1kekbEcrWQMXRy95QhqYMxlAfiqnw
                YqH73i+2wLDxN42OzMy5ThlLDKV9pZU5YChsPejWUIbjoogKQ9EMVMaw3f7f
                uzxmcPbDOJdzwa2G4wdHncOg9bSGZdQqRNYZNttJ2veG0vRSoXTmCa0Tk8tm
                XpCYYBLHJLXSHiWGxLwX0ohIGEEcH08LNCJmTcUaMLAR8SfKoh2Kol06YD5z
                Xd7g+ZrPnK/veGM+2+M77EnZ4V/el3id29Q9e5d/TonOrQZi+jwVbwcPR4Zh
                7dVEGzWWvp6qTPViefi7ARpbK4kkw3JbaRlMxj2ZdgTlMKy2k1DEXZEqixek
                e5RM0lA+UxY0F8LdP2SxS6MrUrclWk07S/K3qWWLV8lzeunTEdog5Nm5kF/a
                PoN7mm/fWSQDB9gkW/uZgCpFoMIL58XXKNs+1U/gr89w8QNWTnOC425u13Ev
                /08ZLpHA5WMUfFzxcdWn0gaFaPq4jrVjsAw3cJP2M9Qy3Mrg/ACDO+xh5AIA
                AA==
                """,
        """
                androidx/navigation/NavDestination.class:
                H4sIAAAAAAAA/4VRy07CQBQ9M0CBivJQEXwkalyoC4vEncbERzQkiIkaNq4G
                2sBAmZp2aFjyLf6BKxMXhrj0o4y3lb2bk/O4d+bOne+fj08AJ9hi2BXK9j1p
                TywlQtkTWnrKaonw2gm0VLFMgzEUBiIUlitUz7rvDJyuTiPBYJxJJfU5Q2L/
                oJ1DCoaJJNIMSd2XAcNe8//jTxmKzaGnXamsO0cLW2hBHh+FCRqSRZCNAAxs
                SP5ERqpGzD5m2J5NTZNXuMkLxGbTTLkym9Z5jV2mvl4NXuBRXZ1F3Qt0760v
                XvpHQ00TXnm2w5BvSuW0xqOO4z+JjktOqel1hdsWvoz03DQfvbHfdW5kJKoP
                Y6XlyGnLQFJ6oZSn46cE2AGnBcynjfZBWCFlxRpIHb4j80aEo0poxGYJ64S5
                vwJkYcb5Roxr2Iy/isanLPeMRAOLDSw1kEeBKIoN6l9+BguwglXKA5gBygGM
                X6tGzkfnAQAA
                """,
        """
                androidx/navigation/NavDestinationBuilder.class:
                H4sIAAAAAAAA/41QTW/TQBSct86nE4hToKR8tZV6oEHCoeoprSpRIlAkUySK
                cslpE1vpNs4aeddRj/kt/ANOSBxQxJEfhXg2OXGhl3kzs7P79r1fv7//AHCM
                fcKh1GGaqPDG13KpZtKqRPsXcjmIjFW6kOeZisMorYII/dNBP/j/lZOz4Fou
                pR9LPfM/TK6jqT0heP96VZQIlVOllT0jOM8PR01UUHVRRo1QslfKEF7cot/m
                i9yjHcwTGyvtv4+sDKWV7InF0uF5KYd6DiDQnP0blases/AV4Xi98lzREe56
                VRThMVmvatud9arr1viU9qgreuJI9Jzz8s8vFeGV8rtH/NyAcHCbxVDevMHm
                u1R+vno5tzznmySMCK1A6egiW0yi9JOcxOxsBclUxiOZqlxvzPqlmvFbWcrc
                vUyydBq9VfnBzsdMW7WIRsooTr7WOrFFT4N9CF7pZvB8w4yPWfmFBsrdb6h/
                ZSLwhLFSmG08ZWz+DcBFg6uDZ0XKwW5RH2GPa58zTc7cGcMZ4u4QrSE8tJli
                a4h7uD8GGTzA9hhlg4bBQ4OOwY5B9Q/QTbpQiwIAAA==
                """,
        """
                androidx/navigation/NavGraph.class:
                H4sIAAAAAAAA/31SW28SQRT+ZinLQi/Q1tYW26pttbTVLjY+SdPES6oYRC2E
                lz4NMKEDy6zZHUgf+S3+A580Phjioz/KeGahiql1kzmX79y+2TM/fn79BuAx
                DhjWuGoGvmxeuIr3ZYtr6Su3zPsvA/7hPAHGsHlNxgsRaqkiN4EYg30kldTH
                DLHcbm0GcdgpTCHBMKXPZciwUfrfqALDYih0RfNAT3RmWMqV2rzPXY+rlvu2
                3hYNXditEfGj6pOrkeNctRqFt0p+0HLbQtcDLlXocqV8HbUM3bKvyz3Po5Fz
                4eW8U7+nhYM08ez42pPKbfe7rlRaBIp7blHpgNrIRpjAPJFqnItGZ9znHQ94
                V1Aiw84/yE4gFdOkVTC/ZxE3UljAEsPC1RKG+dKYxRuheZNrTpjV7cdobcyI
                pBFgYB3CL6Tx8mQ1HzG8Hw6yKWvFGh2HTsZKDQekjHAsZ3llODi08uxZ/PtH
                m4Kv1zOxrJWf2nSc4SAT37Py9qGdSWStV6MExzQ+ZNi+boET+yKahlWVYfpy
                swcdTW/gud8UDOmSVKLc69ZFUOV1T5jb+w3u1XggjT8GkxXZon69gOzt057S
                siuKqi9DSeHfv/vpn5UypCp+L2iIE2nqV8c1tVHFRCLuwqJXaT6LiNIjJblD
                nmtok47vfYbzKQrnSNoROIPdSEYJSCJFeh7ThMSi4gJlW6QT+wuZL1j+u9zG
                bFS+PEoZlxsrjZsU34uyZ7FvMENiLgIeRPI+HpI+IXSFJq+eIVZEtohbRaxh
                nUxsFHEbd87AzNU2z5AMkQqxFcIOMR1iO8S9SKZDzPwCFg2MhPgDAAA=
                """,
        """
                androidx/navigation/NavGraphBuilder.class:
                H4sIAAAAAAAA/61TW08TQRT+Zre0S6nSLgKlIoKAXGUL8lZCIhhNQ0ViTRPD
                g5m2a5my3SUz04ZHfov/wCeND4b46I8ynl2WS6IlPPiw5zbf+c6ZM2d//f7+
                A8AmNhlmud+UgWieOj7viRbXIvCdfd57LfnJ0U5XeE1XpsAY9ip9kC9dpYUf
                uTF+qx80Ii1tlxiW7kyWQoIhuSV8obcZqov9uEMzkAcy6AnKKlXavMcdj/st
                52297TZ0qXIcaE/4jnQ/eeQ7e7seV6q0VGOo/W/WreXtiHi2EsiW03Z1XXLh
                K4f7fqAjcuXsB3q/63k0jLnbUAThdc8lmHUSt2FhiGEqLtzudRzha1f63HPK
                vpbEIBoqhXsMo40jt3EcFzrgkndcAjIsLP7jIteRakjSogtkMIxsGveRY8gq
                zaW+8T4WRhjMxRCVxGgaAxhjSOgjoRjmb92A+GXpSgt3nDuD/XfHDAMy6GqX
                YbzP2zLkLk/euJo3ueYUMzo9k9afhWIwFGBgxxQ/FaFXJKu5zvDx/Gw6beSN
                q+/87EplSVy51lj+/GzDshO2UTSKbGfeGrOTWbNgFBOkB0gn7Uw2VbBswzLz
                rGj9/Jw0soNhmQ2Glbv/V9T71O2/FgvvMnTprh1repHdoEkjGq4I393vduqu
                fB/uUzjRoMG9Gpci9OPgYFW0qGZXkj33rutr0XHLfk8oQcdXG/TiekEZ0tWg
                KxvuKxHmT8Q5tYuMG0DMwKAdicdNK5OCiWfkfaB4gnRuxc58g71qPyC5/BXj
                XyhoYI1kkhIymIRD9vIFGHlMRGQ5DKFA56E1goeUEVqTeET0xYjBwjrpEZNA
                g1H1C5kmeQkxsRHpVTwnXabTKer18SHMMqbLmCnjCWbJxFwZ83h6CKawgMVD
                pBUmFJYUlhWGFAoKIworCpMKjxRSfwCslzbYagUAAA==
                """,
        """
                androidx/navigation/NavGraphKt.class:
                H4sIAAAAAAAA/71W31MTVxT+bn6QzRrIEoyQICASY0QxMeJPkBZRNBWQCmKt
                be2SLGEh7Dp7Nxn70mH60JdO/4C+9KF/gdYHnTLTYehb/6i25y4bEhIgzHTG
                h9x77rnnO+c75557N3//88efAEZRZOhXjYJl6oXXaUOt6EXV1k0jPadWHljq
                q9VHdgCMQVlTK2q6pBrF9OPlNS1PWi+DXAMw3E7NHOXoblkvFTRrbKbR09iF
                JYb58cXbzTsT/8dl27i9qvMJCRKluG7aJd1Ir1U20rpha5ahltI5w7Z0g+t5
                HoDMEM2vavn1OdOeK5dK86qlbmhkyHA+1ey/TrMgnBQpYgghtMs4gQ4qF7dV
                y76ncVs3HM4SFIZwQk+sJOprxnJkmxA896nPHStthkgzM4afDi2aEE1r3jIr
                +sFla05rpq5wK2UjL1zx9LQrXRm7cCRTYrP9EdmMjxyvXVwvTw2dWqx1CkMz
                plVMr2n2sqVSv6RVwzBtdTe22y5klTjKikzU5ZJGZoHlXRYSehlOHxU5gD7R
                xDqxnGDwpkSHDeCMjH4MUlces6wMfsss29r+ZnHryXCm1QETruY/UdBW1HLJ
                Zvj1YzZZ7oAL3urQehtv1ctydnSPfldRs6dKKuc5g26qkdeeaCsMg6mD3S7S
                PXasya+Sb4INtACFcAkjQXhwmSHeGPiZbq9OWkXHUapVfNeYaHTnD3OSPJ6L
                ELK4KkiNMvQQqZxhaFZzSQ6j9JhaykrUQEQpqh/s4mBCTQ5CuIlbgtBtBmk8
                X3L7vu/IbAK4I2NC3IfEcbIO4FMGXyq3e5XuypjE1CHQRn4B3JcxLcw7q+06
                q9lqQbVVyt2zUfHS55SJISgG0Nu+LgQPbb7WhZQhqXCF4d/tzbvy9qbs6fHI
                HsnbPCseZ1txbaqmrqh01Gnig0oo7sm0n5Wl7U2lo4cNezJh+inZNqWTNsIP
                d36U4nccowjpu6r6rCx5lJNxXw/LRLPdyql4V8QXIZwztme6d35r80g9Av3X
                B7a9ebaT2BCOkV6i+HEfwb2k9ZHSX1O2KQFSSqQM1pSycmLnB09A9ks7v2Qz
                TJQhy5wKLdKXudVFjlTLXf8m9VeV91/bGn3CTaO6u/jdK/HMxg490wCe0x+X
                2sEynKhGu7xOT0Pvk7Jh6xtazqjoXKdHe7L2kFPzTJkFekvDM7qhzZU3ljVr
                UTzsgqaZV0tLqqWLtasMLuhF+v6XLZITjX73/mLsC9C+YKv59Vn1lesiVGOq
                0ba8YJatvDati72Y63KpiSiu0EXyUdN5ERevDxX7a1q10RyjOS4uf5OO7l+D
                LqDE4KeVB9/QapZm0cjh4UjwA8IXI500eu+8FV2Oly6sA0F8S/IZMu2gdQRd
                tEsgnERUXAySFJyiHdXBBSBCdBNZEcIkjZ/mvvoQI5HTItB13xb6n7/H2Tf7
                AkYx5AQcJWAUkhNQZN5HAYecgH1uQCElcM6h04deJIniLomefVkv0y/mcxfV
                MV6VlSDOI0Wy4PszgdtoTkZ9/u9/gZ/NtiLuRd4ZWdDJIOJwHaAyDFLhBujb
                Xsumvy6bJC642ST3sknuZZN0s4lhGBfd0zrt2AC+35F+45CvVkzY1+cbQ8bp
                lwbUtUZUqgF1HTeaUWONqOF9KAnj1Bq7xZtyugkY2sIEleeTd0hvYfK5En6P
                e+9wbQvTjvzgHcbe7jntcEBJyETnFDn3okBrmXan8SVeUBDNOdKvsEJzhfQP
                qZS5F/Dm8FkOj3KYwWwOc3icwzw+fwHG8QQLBOS4xDHCMcxxkSPLcZUjQzeJ
                4ybHLY7rHDc4/ByLHE+dMcrpjz6WOIY4Eo6ml+MZxxf/AV3xdy9eDQAA
                """,
        """
                androidx/navigation/NavigatorProvider.class:
                H4sIAAAAAAAA/41Ry0rDQBQ9M2nTGqt9+Gp9gC7Ex8LU4k4RVFAC9YFKN66m
                Tahj0xlJpqXLfot/4EpwIcWlHyXeRD/AzeE8Zrhn7nx9v38AOMAaw6ZQfqSl
                P3KVGMquMFIr9+qX6ugm0kPpB1EOjKH0JIbCDYXqutftp6BjcrAY7COppDlm
                sLZ3WgVkYTvIIMeQMY8yZthq/mvCIUO52dMmlMq9DIzwhRHk8f7QoqosgakE
                wMB65I9kourE/H2G9cnYcXiVO7xEbDLOL1Yn4wavs9Ps54vNSzw512DJ7Wka
                fRGJ58e9nqGSZ9oPGIpNqYKrQb8dRPeiHZJTaeqOCFsikon+M507PYg6wblM
                RO12oIzsBy0ZS0pPlNImfVyMDXDawV/bZCWEVVJuqoHs7hvyr0Q4aoR2ai5g
                mbDwewBTcNJ8JcUlrKYfRvUpKzzA8jDjYdZDESWiKHuoYO4BLMY8FiiP4cRY
                jGH/AKZPGqDtAQAA
                """,
        """
                androidx/navigation/Outer＄InnerClass.class:
                H4sIAAAAAAAA/41U32/bVBT+rp0fjpu2Tttt/RE2oKEk6Ta3ZWNl7QZtoatL
                mo4WVYzycpOYxG1qB9upxgvq0/6ESfCChBBPfRgStIhJqGxv/E0IcW7sJSMd
                VSX7nnOPz/nOd88513/98/sfAG5gkyHD7YrrWJWHus33rSr3LcfW15u+6WYM
                2zbdpTr3vDgYg7bD97le53ZVXy/tmGU/DpkhNm/Zln+XIZI1clsMcja3lUQU
                cRURKAyKJVAW3CoDM5JQ0ZOAhCT5+zXLY5gonIfAHENP1fSNNhalMRjUsrPX
                cGzT9qcJsOw0vmbIEY/zYo4XHLeq75h+yeWW7encth2/5e3pRccvNuv1OXGY
                mEqcLzIkRYpMxfySN+s+Qyl7vkSGUeiu3dw5OSYxiCGRfZRK6TubvmvZdPyh
                bO4lyMBK57nUbVtsWvWK6cZxWcUV0Y6hDnb2RWfuKHiDGskbDdOuMFzLnoY+
                nS1EJoLjyAjwtxjSovRnOb4tHLPCcelsx7xwnEwijdeEdo0OX+NebcmpmAyp
                TqRh+2ZVnG8qGECaMB0zKqbxDp3I/KrJ6zRjF7KvqP/nNPtntZ96z0t1k6oa
                dfya6TIMnEYhMoVdx69btr5m+rzCfU42aW9fpvvFxJIQC2j4d8n+0BI74ipV
                aGB/PDm4rErDkippJwcqPZKmqJISI9lDUibZpzx7pAyfHMxIU2yxdyCmSaPS
                lPzsh5ikRVYTWlzsVp4/klcHNYV0clQUKXAiMyNzgnR1RtF6RiPDbIqtPH8s
                U2Ay8HjMSO8lvU/oG6k2vEJ0RiNKVIsJrjNMnGDkfwc2jmW6i53Josta5Pv3
                XN6oXd+lexIJ+tZfsGyz2Nwrme6norSiok6Z17e4a4l9aBzbaNq+tWca9r7l
                WWRa6LSFoXfT5+XdNd4IvTPd3ve5y/dMYvWfsGSHnUlbddNpumVz2RIQIyHE
                1ql0NEcS/cbE6QfEr4s0hXT6IdC6Srtl+i6RVPPHSOTHfkXvz7ST8DGtfRDN
                vkXxs0iQLNDuYuBN3/rFWJAmUGmKoNEbYOpiWkhG87+g97ANF2sZZ1swycAh
                hEkRuRfB493B7JUB9FMhWBEwTSwFp8RTSA/GjnHpSTsoIJtok02EZNdCNhcA
                LYFhjIS5J8JipdKRb76FIhjM58eOMBZAFmmVwQQCXesw/W2Sglr6Ka48OMbr
                A28eYUJEHiGn5Y5w9QjXn3QdIx0yeokHrXq7BhNhDVoMfsON7jIoYTzDTbwb
                8viCpGhXJj/5E6KRw8k/IX2HqHw4eQJpTQBdpfd7YYkEPSm22ifHlb+RitO+
                U7FMu2IZ6td7lGed9LggdatVg/utULpZuIcVKt8nLUADGyQ/I/tt6tTcNmQD
                8wbuGLiL90nFBwYWsLgN5mEJH26j3xPPRx7U1hrzoHlIeRjwMOjhZss460H3
                kCb9X5rxH871BwAA
                """,
        """
                androidx/navigation/Outer＄InnerObject.class:
                H4sIAAAAAAAA/41US08TURT+7p0+plMe5SFPxQcgBZQpCCuICRIfQ0oxlmCU
                1W070oEygzO3DUtW7ty6cOnCFQuJCxJNDErc+KOI5w6jIERj0t77nXPPOd+Z
                79yZH8efvgCYxgzDsHArvudUdkxXNJx1IR3PNZfr0vaHLNe1/eXShl2WSTCG
                zIZoCLMm3HXzl1djSMw5riPvMmjZ0dUmxJEwEEOSISarTsAwkv8vhlkGXXpF
                6TvuOkNndjR/ynbipYjBvOevmxu2LPnCcQNTuK4nw4KBWfBkoV6rUVT6TFkd
                LVS4KoLqglexwyYt7fv08Wtq3H5ZFzXq8FI2f/7JZkefMwz9i42oRKlmE13c
                k1XbZ2i/WIWo58q1UB8DXImiW4Xiynxh4X4T+mCkyNnP0Jbf9CSFmUu2FBUh
                BSXyrYZGM2JqSakFDGyT/DuOsnKEKpMMLw53Bwzeww2eOdw1uK5AOtp1Q7ky
                LfrRK6PncHeK59i9pM6/vUvwDF/syGh9PBeb0jPxvlgPy7FHR2+0xVQmQd4k
                YUZYJ5xSWLFNMdVD71+nmcQoaV8QjYe+2K5ObEqG/id1VzpbtuU2nMAhueZP
                JaQLcjKS1rzj2oX6Vsn2V5SkSkmvLGqrwneUHTmbi1KUN5fEdmQPna/9WPhi
                y6ZG/iBpCi/DQk0EgU2mUfTqftl+4KgSvVGJ1QvNYZImEwtF71WDov0WWQna
                m2mP02k8tG6TZarRKO/YAfR9AhwTUTAwQ8dA00kAUlRKFU2Th4fJ16Nkrb31
                Q3h0Gq5F4WeZ6S1EW8R7mtq+95dUhg50RkwW7Zz27rHx94jH9sa/gr9FXNsb
                PwR/GtsLG8/RGgNP6mGxrpOEqJhCXfRnpA7UZaZXh4COnt9SdIcJQPoz+LMD
                9H7E5f3QoWGKVqUjxxhaSNU7Id84fYVUawxXSJ6BNWgWrlq4ZtHT3SCIQQtD
                GF4DC3ATI2swAvXLBkgE6AhBV4BMCNK0/gRLa5P62wQAAA==
                """,
        """
                androidx/navigation/Outer.class:
                H4sIAAAAAAAA/4VSy27TQBQ9M87DcQxNw6MJoeXRFJoi4bZiVSqkEgGyFFyJ
                VpFQVpPESqdxxsh2oi6z4kP4g4pFJZBQBDs+CnHHDWSBKmz5nrmPc+7MHf/8
                9eUbgGd4wlAVqh+Fsn/mKDGRA5HIUDmH48SP8mAMpVMxEU4g1MA57J76vSQP
                gyG3L5VMXjAYm422jSxyFjLIM2SSExkz1FpXqj5nMPd7Qcq3wDXJdL2j4wOv
                +crGNVgFCl5nWG+F0cA59ZNuJKSKHaFUmKQ6seOFiTcOApJabg3DhMSct34i
                +iIRFOOjiUGnY9oUtAEDG1L8TGpvm1b9HYbGbGpbvMItXppNLW4a5o+PvDKb
                7vJttseNzMu8yb9/yvES14RdpmUsVyk/agYipkMWU+dyKgz1K09cX5DyuMew
                8Z/KP3N+QC08MXkTiQ8nT4fUovZurBI58l01kbHsBv7BYiY0+mbY9xmWWlL5
                3njU9aNjQTUM5VbYE0FbRFL786C92JRPZOsoHEc9/7XUueq8T/ufLtihy8mk
                E63quyKsk5cjLBFyerOpt0Geo+dOmN26gHmeph/Ni/W/95isfVmAAkkBJop/
                yStUrZ/iV/D3F7A/Y+k8DRjYJFum9H36VmkfDwnXCBtpi3VsEe6RzDIJlzsw
                XNxwcdPFLdymJVZcVFDtgMW4g1oH2RhWjLsxcjFWY6z9BuhHh3wdAwAA
                """,
        """
                androidx/navigation/OuterComp＄InnerClassComp＄Companion.class:
                H4sIAAAAAAAA/51Ty27TUBA9Y6dx4oaStjwSoJRHgBRB3VQIIRUhQRAoUppK
                gLLpAt0kpr2JfV3Z11GXWfEh/EFXSCxQ1CUfhZjrBCo2SGUzc2bOnBl7xv7x
                89t3AE9QJzwVahBHcnDsKTGWB0LLSHl7qfbjZhQe1VpKMQpEkmShMUJxiQMi
                lIdiLLxAqANvrzf0+9qBTcg/l0rqFwS7vtEtYQF5Fzk4hJw+lAnhWfv/Ru4Q
                GvX2KNKBVN5wHHpSsUSJwHvtfxJpoJuRSnSc9nUU74p45Mc7G10Xlhm9Wuuf
                kR/DjCVsnq8bYfm3YNfXYiC04JwVjm1eJhlTNAYEGnH+WJpoi9GgQahNJ65r
                VSzXKjOaTgqnn+3KdLJtbdErp2CdfslbZcvUbpPp8Og8O3JwnbD2T4WDNcLS
                3zJC8c9yCYsdMX4bi6PDzZHmWzWjgU+42JbK76Rhz48/iF7AmZV21BdBV8TS
                xPNk6ayxzxd230dp3PffSMNV36VKy9DvykRy8UulIp09XIIGHydnNsbeMh8K
                v/gdjjyzQvYLD7+icJLRd9nms2QHNbalWQGKcIEyMVqcix+zt+bi0kl2DiO4
                MkvOBBm6gCXmbNzjaIXZG7iJdVQzdIv9/WzwbTzIfhXeBWvK+7BbWG5hpYVV
                XGKIyy3ufXUflKCCKvMJ3ATXEuR/Ae6K/YtnAwAA
                """,
        """
                androidx/navigation/OuterComp＄InnerClassComp.class:
                H4sIAAAAAAAA/5VUW08bVxD+zvq2XgysIRcCpEmLm5pLWKBJSsFpuKSEpTak
                kNIQ2oeDvTULZpfurlH6UuUpPyFS+1KpD33iIVFbqIpU0eStv6mqOmd3MWAi
                JEv2OTOzM998Z2bO+ee/P/8CcAtfMwxwq+TYZumpZvEds8w907a0hapnONP2
                1nZGtyySKtx1hZoAY1A3+A7XKtwqawtrG0bRSyDCEM+Zlul9whDN6r3LDJFs
                73IKMSQURCEzyKZAmnTKDExPQUFTEhJS5O+tmy7DYL4RIuMMTWXD02uYlE5n
                UIr0zbYMyxsm4KK9/R3DMPFpFLsnbztlbcPw1hxuWq7GLcv2/ChXm7e9+Wql
                Mi4OF1foDJcYUiJVpmR8w6sVj8HJNpZQ1/P1NR1vkHMK7bgg2HRSqT17yXNM
                i8pyIdt7Ajqw0vku19umqmalZDgJvKPgmmhXx2n87FH37sp4l5rNt7cNq8Rw
                M3sW/mzGEJ1I9iAjErzP0C3acp7jB8IxKxynz3fsE479KXTjqpBuUgHWubs+
                bZcMhvRxpG55RlmccSgYUppCDSMKhvEhncj4tsorNIcXs2/pxROGzHkjQfPA
                1yoGVTZme+uGw9B2FoV45YqV8JbcaaS7GbFwi1wSGBcTnd+0PULSNna2NJOO
                5Vi8ot0Pxm+aGHlOtejZToE7m1Sk4CLeVZADZU7WwBhGGxqyYxpU9wlMigs8
                RSU+YlMwPF7iHieK0tZOhF4YJpakWEDXfpPsT02hUQekEl3R3cNn1xWpQ1Ik
                9fCZQj9JlRVJjtPeRHuE9hYyy6+fyx3k2joiDbEx1jrV3BZXpU5pKPL657ik
                RueSakJos2+eR+baVZnkw2cjsiwFTmRmZE6SrIzIalNntIMNsdk3LyIUmAo8
                XjCSm0luEfJiugYvU/7OqBxT44LzCBMnuXpu1RJ4yNByunT0XM3znQcO314f
                3KQXomuxannmlqFbO6Zr0uhMHo8TTWcwu6150zLmq1trhvNIjJeYKrvIK8vc
                MYUeGpuXPF7cLPDtUM/UYz/kDt8yiN2pJKljhgapypJddYrGjCkgroQQy2fI
                0W2R6EEHrVfEDFA1HpEWp/0i7W3iYRc9Jz3mW78gbYa8JdqVvn0k+7p+R/Mr
                H2GZ1haIgcgTZoGi8viStEuBN31rFaNDkkClSYNK/wBTExNFe6zvNzTv1uDi
                vrHgw6QChxAmTeSOgnvqg9lbA+hJJVgRMEwsBafkAaSVrn1cflkLCsgma2ST
                IdkTZVGT6KByBblvhAVMd0e//wGyYJDr69pDVwD5mNYImECgBy1MP0a7oNZ9
                gGsr+7je9t4ebojIPfSqvXsY2MPgy7pjdIeMTraHUdnSNR5BDXwGf+BWfRnk
                MJ7hNu6EPL6iXbQr09f/C2LR3f6/If2IWGS3/xBSQQAN0P8nYYkGPXnsty+S
                kP9FOkH6ccUytYplMIqPKc8KyQlB6iM//RgSIdUOPykRO0Buhe3j3q+YfuVb
                InjiT52Yr8+xSEXOkTRB+6qffokog2SG+9TXT1cR0TGj44GOWegkYk7HZ8iT
                g0tDM78K1UWriwUXir/GXWFJu2hz0e7itm8cdaG56Pblif8BqzOQX0wJAAA=
                """,
        """
                androidx/navigation/OuterComp＄InnerObject.class:
                H4sIAAAAAAAA/41US08TURT+7p0+plMe5SFPxQdVKVWmoK4gJkh8DKnFCMEo
                q0s70oF2BmduG5as/AkuXLrQDQuJCxJNTJWdP8p47nQUhEhM2nu/c+455zvz
                nTvz4+fnrwBu4w5DTrgV33MqO6Yrms6GkI7nmksNafsLXn07a7mu7S+tb9pl
                mQRjyGyKpjBrwt0wf3s1hsSc4zryLoM2kVvtQBwJAzEkGWKy6gQM+eJ/s8wy
                6NJblr7jbjD0T+SKR4xtL0WMFz1/w9y05bovHDcwhet6MiwamCVPlhq1GkWl
                j5XV0UWFqyKoLngVO2zU0twPhTlq3n7VEDXq8txE8eTTzeZeMGTPYiMqsV6z
                iS7uyartM/SerkLUc+VaqJEBroTRrdLyynxp4X4HRmCkyDnK0FPc8iSFmY9t
                KSpCCkrk9aZGs2JqSakFDGyL/DuOsgqEKtMML1u7YwYf4gbPtHYNriuQjnbd
                UK5Ml3742hhq7c7wAruX1Pn3dwme4Yt9GW2EF2IzeiY+EhtiBfbo8I22mMok
                yJskzAjrhFMKK7YZpnq4cOZEk8iR/iXRfOiL7erUlmQYfdpwpVO3LbfpBA5J
                Nn8kI12U9li6i45rlxr1ddtfUbIqNb2yqK0K31F25OxclqK89VhsR3b2ZO0n
                whd1m5r5i6QjvBALNREENpnGstfwy/YDR5UYjkqsnmoO0zSdWCj8sBoW7TfI
                StDeSXucTuOhdZMsU41HeScPoO8T4JiKgoFFOgY62gFIUSlVNE0eHiZfjpK1
                3u6P4dFRuBaFH2emtxE9Ee9Rau/eP1IZ+tAfMVm0c9oHJ/PvEY/t5b+Bv0Vc
                28u3wJ/F9sLGC7TGwJN6WGygnRAVU2iA/ozUgbrQ9PoQ0DH0R4rBMAFIfwF/
                foDhTzi/Hzo0zNCqdOSYRBepeivky9MXSbVGl4vkGVuDZuGihUsWPd0Vghi3
                kMXVNbAA13B9DUagfhMBEgH6QjAQIBOCNK2/ALLTKT3nBAAA
                """,
        """
                androidx/navigation/OuterComp.class:
                H4sIAAAAAAAA/41RW2sTQRT+ZjaXzWZt03hpYm2rtmpTxW2LT7UINagsxC3Y
                EpA8TZIlnWQzW3Y2oY958of4D4oPBQUJ+uaPEs9uo0WE4g57vjm378w558fP
                z18BPMNjhmWhulEou6eOEmPZE7EMlXMwiv2oHg5P8mAMpb4YCycQqucctPt+
                J87DYMjtSSXjFwzGRq1pI4uchQzyDJn4WGqG1caVzM8ZzL1OkHJY4Emi6XqH
                R/te/ZWNa7AKZJxjWGuEUc/p+3E7ElJpRygVximXdrww9kZBQFQLjUEYE5nz
                1o9FV8SCbHw4NqhLlohCIsDABmQ/lYm2RbfuNkNtOrEtXuEWL00nFjcN8/sH
                XplOdvgW2+VG5mXe5N8+5niJJwk7LKGZc5WiNgKhddILQzE1XEyH4cmVna//
                nZzHKj3iPzJ+z/4elfPE+E0kTo6fDqjc0ruRiuXQd9VYatkO/P3LGdE66mHX
                Z5hvSOV7o2Hbj44ExTCUG2FHBE0RyUSfGe3Lx/mUbB2Go6jjv5aJrzqr0/yn
                CrZpWZl0wtVkd4TrpOUIS4ScTjbVHpDmJHsgzG6ewzxL3Q9nwYCLRyTtiwAU
                iAowUfyTvEjRyVf8Av7+HPYnzJ+lBgMbJMvkvkv/Mr3jPuEKYS0tsYZNwl2i
                WSDicguGi+subri4iVt0xaKLCqotMI3bWGohq2Fp3NHIaSxrrPwCZxVYTTUD
                AAA=
                """,
        """
                androidx/navigation/TestAbstract.class:
                H4sIAAAAAAAA/4VRy04CMRQ9LTDKiAo+wUeiLoy6cJS40xjRREOCmChhw6ow
                Ey2PjpmWiUu+xT9wZeLCEJd+lPF2ZO/m5Dxum3Pb75+PTwAn2GTYEsqPQum/
                eErE8lEYGSqvEWhTaWsTiY6ZAmPId0UsvL5Qj95duxtYN8XgnEklzTlDam+/
                mUMGjos0phjS5klqhp3af5efMhRqvdD0pfJuAyN8YQR5fBCnqCCzkLUABtYj
                /0VadUTMP6bu45Hr8iJ3eZ7YeDS9WxyPyvyIXWa+Xh2e53auzOzpmbqIbyLx
                /HTYM9TvKvQDhvmaVEF9OGgHUUO0++Qs1MKO6DdFJK2emO5DOIw6wbW0onQ/
                VEYOgqbUktKKUqFJ9tLpbXBaf9LWvgZhkZSXaCBz8I7pNyIcJUInMS+wRpj7
                G0AWbpKvJ7iKjeSbqD5luRZSVcxWMVfFPPJEUahiAYstMI0lLFOu4WqsaDi/
                +liLRuMBAAA=
                """,
        """
                androidx/navigation/TestAbstractComp＄Companion.class:
                H4sIAAAAAAAA/5VSy24TMRQ99qR5TAOkLY+EV3kEqUWi01TsipBKEChSWiRa
                ZdMFciamdTLjqWwn6jIrPoQ/6AqJBYq65KMQ15MAW7q5r3PPvfaxf/76/gPA
                Szxj2BJ6YDI1OI+0mKgT4VSmoyNp3V7fOiNi187Ss6Y3QhNUAmOoDcVERInQ
                J9GH/lDGroSAofhKaeVeMwQbm70qllAMUUCJoeBOlWXY7l5t1S5Da6M7ylyi
                dDScpJHSThotkuit/CzGCbVr4o1jl5l9YUbS7G72QnC/cq0Z/wM/pTlKd73a
                NIaVP4R96cRAOEE1nk4CEo95U/EGDGxE9XPls22KBi2G5mwahrzOQ16jaDYt
                X34J6rPpDt9mb0plfvm1yGvc9+4wP6H5P9qUcI+h8lcghuUDMXlvxNnp1siR
                zu1sIBludJWWB+O0L82R6CdUWe1msUh6wiifL4rVjtbStBNhraTXCQ+zsYnl
                O+WxxsexdiqVPWUVNe9pnbn8SBYtErjgb02e+0emw69TFnkZyC89/4byRQ4/
                IlvMi4d4TLY6b0AFIVBjFC0vyC/I8wW5epFL6gm358U5IY+u4TphAZ5QFuak
                +3iABp7mCx+imX9r0oB6a8cIOljpYLWDNdykELc6NPPOMZhFHQ3CLUKLuxbF
                3++scPYTAwAA
                """,
        """
                androidx/navigation/TestAbstractComp.class:
                H4sIAAAAAAAA/41RW08TQRg9s9vrspUWb0W8gFQEHlggJiZCTBCjaVJKIoTE
                8DRtxzLtdpbMzDY89rf4D4gPJJqYxkd/lPHbpcKDL335znyXc77L/P7z/SeA
                V1hnqHHV0ZHsXASKD2WXWxmp4FgYu9cyVvO23Y8G53kwhnKPD3kQctUNDls9
                0bZ5uAy5Xamkfcvgrq6d+Mgi5yGDPEPGnknDsNKYpsEOQ2G3HU6kNqah1BLD
                FaXy8Bm2Vhv9yJJC0BsOAqms0IqHwXvxhcchERQx47aN9AHXfaF3roe946GE
                WYbijRjD5lQT37bf8VHBXBEO7jIsNyLdDXrCtjSXygRcqcimCiZoRrYZhyHt
                Wvk364GwvMMtp5gzGLr0KSwxxcSAgfUpfiETb5NenS2653jke07V8ZzyeOQ5
                BaewUh2PFt1tZ5O9Ye677K+vOafsJNXbLNGYafLhR83Pzzb6lmHhU6ysHIi6
                GkojW6HYux2Q/mw/6giG2YZUohkPWkIfc6phmGtEbR6ecC0TfxL060oJvR9y
                YwSRvaMo1m3xQSa5+Umfk/+6ZJboUpl0vfnkcITL5OUI7xM6hNnUq5EXJEcg
                zK5foXCZpl9MioEjrJD1rwtQhEdYwMwNuYr0jPB/oPSZXaH8Dfcu04iLl2Q9
                qiuRYoUGWU21n2ON8DXFH5Diw1O4dVTrmK/jERboicd1PMHTUzCDZ1g8RcbA
                M1gyyBlU/gL0PzRmVwMAAA==
                """,
        """
                androidx/navigation/TestClass.class:
                H4sIAAAAAAAA/31Ru07DMBQ9dmhKQ4GUZ3mvwEDaig2EBEigSAEkQF2Y3CYC
                09RBsVsx9lv4AyYkBlQx8lGI68DMcnQe19a59tf3+weAfWwwbAgV55mMnwMl
                hvJeGJmp4DbR5jQVWpfBGPxHMRRBKtR9cNV5TLqmDIfBPZRKmiMGZ3unXUUJ
                rocJlBkmzIPUDFvRvzcfMNSiXmZSqYKLxIhYGEEe7w8dqsYsVCyAgfXIf5ZW
                NYjFTYbN8cjzeJ173Cc2HtXHoxZvsJPS54vLfW6nWsyenboUw/NcPD3s9QxV
                O83ihGE2kiq5HPQ7SX4rOik5c1HWFWlb5NLqP9O7yQZ5NzmTVqxcD5SR/aQt
                taT0WKnMFCtpNMFp87+u9iEI66SCQgOl3TdMvhLhWCF0C7OBVcLq7wAq8Ip8
                rcBlrBffQ/Upq97BCTEdYibELHyiqIWYw/wdmMYCFinX8DSWNNwfvyv/UtsB
                AAA=
                """,
        """
                androidx/navigation/TestClassComp＄Companion.class:
                H4sIAAAAAAAA/5VSy24TMRQ99qR5TAOkLY+Ed9sgtaB2mopdERKEhyKlrQRV
                Nl0gJzGtkxlPZTtRl1nxIfxBV0gsUNQlH4W4ngRYom6u7z3nnns9x/Pz1/cf
                AJ7jCcMzofsmVf3zSIuxOhFOpTo6ktY1Y2FtM03O6j4ITXgBjKEyEGMRxUKf
                RIfdgey5AgKG/AullXvJEGxsdspYQD5EDgWGnDtVlmGrfYU9ewyNjfYwdbHS
                0WCcREo7abSIozfysxjFrplq68yo51KzL8xQmr3NTgju963Ue//IT0nGMmxf
                bRrD0h/BvnSiL5wgjCfjgGxjPpR8AAMbEn6ufLVDWb/BUJ9OwpBXecgrlE0n
                xcsvQXU62eU77HWhyC+/5nmF+95d5ies/teYAu4xlP66w7B4IMbvjTg73R46
                criZ9iXDjbbS8mCUdKU5Et2YkOV22hNxRxjl6zlYbmktTTZb0ruEH9OR6cl3
                ynO1DyPtVCI7yipqfqV16rL7WDTI3Zz/ZDq5f166+SOqIu8BnQtPv6F4kdGP
                KeYz8C1WKZZnDSghBCqMssW5eItOPheXLzI/veD2DJwJsuwarhMXYI2qMBPd
                xwPUsJ4tfIh69jeTB9RbOUbQwlILyy2s4CaluNWimXeOwSyqqBFvEVrctcj/
                Bj3L7PkKAwAA
                """,
        """
                androidx/navigation/TestClassComp.class:
                H4sIAAAAAAAA/4VRXU8TQRQ9s9vPZZEWv4r4AYJaMLqUmJgIMdH6kU1KTZQ0
                MX2atiNMu50lM9MNj/wW/wHxgUQTQ3z0RxnvLhUefODlnrlnzj33zp3ff77/
                BPAM6wzLXA10LAeHgeKJ3ONWxirYFcY2I25MMx4fFMEYKkOe8CDiai/40BuK
                vi3CZShsSyXtSwa3vtbxkUfBQw5Fhpzdl4ZhpXWp+xZDabsfTX0eX6pfTQNX
                xBfhMzTqrVFsqTwYJuNAKiu04lHwRnzhk8g2Y2WsnvRtrHe4Hgm9dTbmFQ+z
                mGMon5sxPLl81oveWz6qmC/DwdX0lbHeC4bC9jSXygRcqdhm5SZox7Y9iSJ6
                ZfXfoDvC8gG3nDhnnLj0ESwN5TSAgY2IP5RptkGnQYNh9fTI95ya4zmV0yPP
                KTm106Mld9PZYC+Y+zr/62vBqTipdpOlDjNtnrzX/GD/6cgyLH6cKCvHIlSJ
                NLIXiVcX49FHNeOBYJhrSSXak3FP6F1OGob5VtznUYdrmeZT0g+VEjrbh6Bi
                71M80X3xTqZ3C9M+nf+6oEF7ymWPW0jXRrhCWYHwOqFDmM+yVcqCdAWE+fUT
                lI6z6wdTMfAWDyn6ZwKU4RGWMHNeXEO2RPg/MPuZnaDyDdeOM8bFI4oe6WbJ
                sUqD1DPv+1gjfE78DXK82YUbohZiIcQtLNIRt0Pcwd0umME9LHWRM/AMlg0K
                BtW/kzCVQUkDAAA=
                """,
        """
                androidx/navigation/TestClassWithArg.class:
                H4sIAAAAAAAA/41QTY/SUBQ977WU0gEp+MUw6vgxMTMsLEPcaSYiiaYJjsk4
                wQWrBzTlDdCavgeZJb/FtRsTjYkLQ1z6o4z3lYkrFybtuffcnpzbe379/v4D
                wFMcMByIZJKlcnIZJGIlY6FlmgTnkdK9uVDqvdTTbhYXwRj8C7ESwVwkcfB2
                dBGNdREWg/NcJlKfMNiH4dGAwTo8GpRRQNGDDZe4yGIGFpbhYacEjjJJ9VQq
                hsf9/9n9jHbEke4aGzIPGWr9WarnMgneRFpMhBYk4YuVRScxAyUDoKUzml9K
                w9rUTY4Zept13eMN7nF/s/bo4b7rcddqbNYd3mYvK3XH503etn5+dLhvn9X+
                MpfUTdst+I6x6jCzYOdUrF5n4sP0yUzTVb10EjFU+zKJTpeLUZSdi9GcJvV+
                Ohbzgcik4VdD7126zMbRK2nI7tky0XIRDaSS9LWbJKnO01A4psjs/Jy6SZA6
                Tn0BDuE+sRPinKrX+oZSa+8rKp9zzX1CowE6eEB4a6vCNVRNOtQZNwoTPr1b
                r8CERrXQ+oLKp3/alLeCKxuOhznewyOqL/KfLOD6EFaIGyFuhrT2NrVohNhF
                cwimsIc7QxQVqgp3FbwcHQVfofYHzU1tNpgCAAA=
                """,
        """
                androidx/navigation/TestClassWithArgComp＄Companion.class:
                H4sIAAAAAAAA/5VSTW8TMRB93k3zsQ2QtnwkfBeClCLRbaLeipBKEFWktEi0
                CoceKicxiZNdb2U7UY858UP4Bz0hcUBRj/woxHgT4Eov45n35s2sn/fnr+8/
                AOziBUODq75OZP8iVHwqB9zKRIUnwthmxI35JO1wXw+aSXxedYEronNgDKUR
                n/Iw4moQfuiORM/m4DNkX0sl7RsGv7bVKWIF2QAZ5BgydigNw277+uv2GOq1
                9jixkVThaBqHUlmhFY/Cd+Izn0S2mShj9aRnE33I9Vjova1OAM+t3aj2/pFn
                ccoybF9vGsPaH8GhsLzPLSfMi6c+mchcKLgABjYm/EK6aoeyfp2hOp8FgVf2
                Aq9E2XyWv/ril+ezhrfD3uby3tXXrFfyXG+DuQm1//UnhwcMhb8mMawe8emB
                5ufD7bElv5tJXzDcaksljiZxV+gT3o0IWW8nPR51uJauXoLFllJCpysEvVJw
                nEx0T7yXjqt8nCgrY9GRRlLzvlKJTT/LoE4mZ9zN6fTcY9MFnlAVOivoXHn5
                DfnLlH5KMZuCB9ikWFw0oIAAKDHKVpfiV3R6S3HxMrXVCe4uwIUgzW7gJnE+
                nlEVpKKHeIQKnqcLH6Oa/uLkAfWWTuG3sNbCegsbuE0p7rRo5r1TMIMyKsQb
                BAb3DbK/Ae9ImxYfAwAA
                """,
        """
                androidx/navigation/TestClassWithArgComp.class:
                H4sIAAAAAAAA/41SW08TQRT+ZnvZ7VJkqYgFvKCglqpsITwJIcEadZNSEyQ1
                hqdpO5Zpt7Nkd9rwyG/x2ReihkQTQ3z0RxnPbis86APJ7jlzzpzzfecyv35/
                +wFgA2sMJa7aYSDbx67iQ9nhWgbK3ReRrvo8it5JfbgTdqpB/8gEY3C6fMhd
                n6uO+6bZFS1tIsWQ3ZJK6m2GdMlbaTCkSiuNPDIwbaRhkc3DDgPz8rAxkYOB
                PIXqQxkxlGtX5d8kno7QOzEUEXgM1lbLHxOvXxVlORZc0bWJ6wxrpVov0ITi
                dod9VyotQsV994X4wAe+rgYq0uGgpYNwl4c9EW6O+rphYwazDLkLMIaNKzdy
                WcJmHkXMxQOZZ1iqBWHH7QrdDLlUkcuVCnSCErn1QNcHvk8jmP5b767QvM01
                J5/RH6ZonSwWuViAht0j/7GMrQqd2rTp1+cnBdsoGrbhnJ/Y9BmOZRtWunh+
                smiuGxX2jJnPJwtZx5g3KqmfH7OGk96bvrAsSplPWxknG+Ots5hlos6Hr0J+
                dLja0wwLewOlZV94aigj2fTFzmULtPBq0BYMUzWpRH3Qb4pwn1MMQ6EWtLjf
                4KGM7bEz7yklwmR0gpLtt8EgbImXMr6bG/M0/mHBGs0yTT0bmItHSyWWycqS
                vkW6EL8/0imyM4n3MVnbFG2QtstnyJUXvmLyNEF4Ms4EXuEpydlRFK5hKp4x
                nWI0Wgkc+kdYbjx60pnyF0x++i9MfhQwhrGoKHOcXESyPOS/Y+Y9O8PNz1g4
                TTwprCaEjN6dkTTmJtgrqJCukv82Id45QMrDXQ+LHu7hPh2x5GEZDw7AIjzE
                owNYEaYilCLYicxGcCJMRyj+AVa+iyYTBAAA
                """,
        """
                androidx/navigation/TestGraph.class:
                H4sIAAAAAAAA/32S32oTQRTGv5kkm80m2rT+aWKtVduLKuK2xTuLUIvKwrqC
                DQHp1SQ7pJNsZmV3svQyVz6Ib1C8KChI0DsfSjyzBr0Q3IVzzvfNmR97Dvvj
                5+evAJ5gh2FT6DhLVXzua1GokTAq1X5P5uZVJt6f1cEY2mNRCD8ReuS/GYzl
                0NRRYXAOlVbmGUNl90G/hRocD1XUGarmTOUMW+F/yU8Z3MNhUjI8cHvRDaKT
                3lF0/KKFK/AaZF5l2A7TbOSPpRlkQuncF1qnpmTlfpSaaJYkhFoNJ6khmP9a
                GhELI8jj06JCUzIbGjaAgU3IP1dW7VEV7zPsLOaexzvc422qFnP3+wfeWcwP
                +B57Xnf5t48Ob3Pbe8AsoRmJohzg8cQwbLydaaOmMtCFytUgkUd/P482cZzG
                kmElVFpGs+lAZj1BPQxrYToUSV9kyuql6Z2ks2woXyoruktw/x8s9mkx1XKa
                rt0T5TukHMptypzeWqm2SPl2Zsq1h5dwL8rju8tm4BHuUWz9bkCDUICL5p/L
                69Rtn+YX8HeXaH3CykVpcNwv4ya2y9+I9k+AtVNUAlwLcD3ADdykEusBOuie
                guW4hQ06z+HluJ3D+QXkdVzrgwIAAA==
                """,
        """
                androidx/navigation/TestInterface.class:
                H4sIAAAAAAAA/4WOz07CQBDGv9kqheKfopLg0Xi3QLx58qJpgpqo8dLT0C64
                tGxNd2k48lweDGcfyrjFB3Am+eabmeQ38/3z+QXgGn3CBeusKlW2jjTXas5W
                lTp6lcbG2spqxqn0QYRwwTVHBet59DRdyNT68Ai9SV7aQunoQVrO2PINQSxr
                z8GpkU4jIFDu5mvVdEPnshGhv920AzEQgQidmw22m7EYUrMcEy4n/37lLjlw
                95Hr+4o/3q9ySwheylWVyjtVSML580pbtZRvyqhpIW+1Lu2OY1ruCvbwFwKn
                Oz3Bmasjx9x32UrgxfBjtGN0EDiLbowDHCYggyMcJxAGoUHvFzaSAvhMAQAA
                """,
        """
                androidx/navigation/TestObject.class:
                H4sIAAAAAAAA/32Sz27TQBDGv90kjuMEGsqfJhRKoT0Ah7qtuFEhlQqQJWMk
                GkWqetrEq3QTZ43sjdVjTjwIb1BxqAQSiuDGQyFmTYADErY0M9+3sz95Rv7+
                49MXAE+wzbAhdJylKj73tSjUSBiVar8nc/NmMJZDUwdjaI9FIfxE6JH/260w
                OAdKK/OMofLwUb+FGhwPVdQZquZM5Qyb4f/RTxncg2FSQjxwe9MNouPeYXT0
                ooUr8BpkXmXYCtNs5I+lGWRC6dwXWqemhOV+lJpoliSEuhZOUkMw/7U0IhZG
                kMenRYXmZDY0bAADm5B/rqzapSreY9hezD2Pd7jH21Qt5u6397yzmO/zXfa8
                7vKvHxze5rZ3n1lCMxLFq0y8O9uZGIb1tzNt1FQGulC5GiTy8O/n0SqO0lgy
                rIRKy2g2HcisJ6iHYTVMhyLpi0xZvTS943SWDeVLZUV3Ce7/g8UeLaZaTtO1
                e6K8Qcqh3KbM6a2V6h4p385Mufb4Eu5Feby5bAZ2cJ9i61cDGoQCXDT/XF6j
                bvs0P4OfXKL1ESsXpcHxoIx3sVX+SLR/AqyeohLgeoAbAW7iFpVYC9BB9xQs
                x22s03kOL8edHM5P0Fqg1oUCAAA=
                """
    )
