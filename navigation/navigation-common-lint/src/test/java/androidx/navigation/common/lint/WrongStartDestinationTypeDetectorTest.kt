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
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

class WrongStartDestinationTypeDetectorTest : LintDetectorTest() {

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
                }
                """
                    )
                    .indented(),
                byteCode,
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
                }
                """
                    )
                    .indented(),
                byteCode,
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
                }
                """
                    )
                    .indented(),
                byteCode,
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
                }
                """
                    )
                    .indented(),
                byteCode,
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
5 errors, 0 warnings
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
                }
                """
                    )
                    .indented(),
                byteCode,
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
                }
                """
                    )
                    .indented(),
                byteCode,
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
                }
                """
                    )
                    .indented(),
                byteCode,
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
5 errors, 0 warnings
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
                }
                """
                    )
                    .indented(),
                byteCode,
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
                }
                """
                    )
                    .indented(),
                byteCode,
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
                }
                """
                    )
                    .indented(),
                byteCode,
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
5 errors, 0 warnings
            """
            )
    }

    private val sourceCode =
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

"""

    // Stub
    private val byteCode =
        compiled(
            "libs/StartDestinationLint.jar",
            kotlin(sourceCode).indented(),
            0x8f4dea5e,
            """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgUuQSTsxLKcrPTKnQy0ssy0xPLMnM
                zxPi8ksscy9KLMjwLuES5eJOzs/VS61IzC3ISRViC0ktLvEuUWLQYgAAL/WN
                klIAAAA=
                """,
            """
                androidx/navigation/AbstractChildClass.class:
                H4sIAAAAAAAA/41Qz28SQRh9M7sssIAs+ItStbW2hnIQ2njTNFISDQn2UBsO
                cBrYDUyAXbMzkB75Wzx7MdGYeDDEo3+U8ZulGg8cPMyb7828vO/73s9f374D
                eI4jhqci9ONI+teNUCzlWGgZhY3WUOlYjHR7Imd+eyaUSoMx7G/TXgVK/9Gn
                YTE4L2Uo9RmDXesf9xis2nEvjxTSLmxkiIt4zMD6ebjIZcGRJ6meSMVQ6/7f
                NC+oyzjQLWNE9n2GUnca6ZkMG28DLXyhBUn4fGnRmsxA1gCo7ZTer6VhTar8
                E4bz9ars8gpPznrlci/n8oxVWa9OeZOdF8qOx6u8af344HDPviz9ZRlSV+1M
                ynOM0ynDwdbx/w2IpqIhchdi+SYW7yfPppp2b0d+wFDsyjC4WMyHQXwlhjN6
                KXejkZj1RCwNv3l030WLeBS8lobsXC5CLedBTypJv60wjHTSVOGEgrWTlcsm
                Z6o41Sk4hPvEzohzut36V2Tru19Q+JRoHhMaDfAKB4T3NircQtEkSJVxo8Dh
                0dl4NUywdKfqn1H4uNUmvxHc2HA8SXAPh4nCDJnC7QGsDu50cLdDbe9TiUoH
                O6gOwBR28WCAtEJR4aGCq/BIwVHwFEq/ASCFXj3QAgAA
                """,
            """
                androidx/navigation/AbstractChildObject.class:
                H4sIAAAAAAAA/41Sy27TQBQ9M0kcxwn0waMJ5VFapFIWuK3YUSGFCJAlYyQa
                RUJdjR9qpnFs5JlEXWbFh/AHFYtKIKEIdnwU4o4JD4ku8Mj3zrlz5tyZY3/7
                /vEzgEe4x7AtsrjIZXzqZmIqj4WWeeZ2Q6ULEeneUKbxq/AkiXQdjGHjInI/
                UfrXhjoqDNaBzKR+wlC5vzNooQbLQRV1hqoeSsWw4/9nz8cM9kGUlmoOuJGw
                veCw3w16z1q4BKdBxcsMW35eHLsniQ4LITPliizLdamq3CDXwSRNSWrFH+Wa
                xNyXiRax0IJqfDytkBPMhIYJYGAjqp9Kg3ZpFu9Rg/nMcXibl+98Zn99x9vz
                2T7fZU/rNv/y3uLL3FD3GTYvvNzfHlHbZiCmLwrxdvhwpBnWX08yLceJl02l
                kmGadP+cn0zr5XHCsOTLLAkm4zAp+oI4DKt+Hol0IApp8KLoHOaTIkqeSwM6
                C+HBP7LYI+eq5XU7xkjKtwlZlJcpcxq1Et0h5BpTKNcenMM+K5c3FmSgi7sU
                Wz8JaJAUYKP5e/Masc3T/AT+5hytD1g6Kwscm2W8ha3yX6QPRAKrR6h4uOLh
                qodruE5TrHloo3MEpnAD67Su4CjcVLB+AMKW4JLIAgAA
                """,
            """
                androidx/navigation/InterfaceChildClass.class:
                H4sIAAAAAAAA/41Qz28SQRT+ZhZYWKgsVCtt/VWrtnBwaePNprEl0ZBgTWrD
                AU4Du8KUZdfsDKRH/hbPXkw0Jh4M8egfZXxDSRMTDh7mvfe99833fvz+8+Mn
                gBfYY9gTkZ/E0r/yIjGVA6FlHHnNSAfJB9EPGkMZ+o1QKGWDMbiXYiq8UEQD
                713vMuhrGxbDziqJi0DpGxkbaYbMkYykPmZI7XeqbQZrv9ouwEbOQQoOYZEM
                GFingALWcuC4RVQ9lIqh2vrPKV9Sm0GgT4wS6XcYSq1RrEMZeW8DLXyhBVH4
                eGrR/syYnDGgviPKX0mD6hT5Bwyn81nZ4RW+ePOZw928w7NWZT475HV2ulbO
                uHyL161fnzLcTZ2XblCW2FupbNrNGKVDht2V8/9zIhqLpsifiembRHwcPh9p
                2r4R+wFDsSWj4Gwy7gXJheiFlCm34r4I2yKRBi+Tzvt4kvSD19KAzfNJpOU4
                aEslqXoSRbFedFU4oNOmqFeGXtncmlbmFNvIkn1M6JgwJ+/UviNf2/6G4pcF
                Z5es+QUc4QnZjWsWXJTMESkyanRz0l1fannmtuTTta8ofl4pU7gmLGU4ni7s
                Dp6Rf0W121S704XVxEYTd5uoYJNCbDWxjXtdMIX7eNCFrVBSeKhQUHikkFUo
                K6z/BWy7OjDsAgAA
                """,
            """
                androidx/navigation/InterfaceChildObject.class:
                H4sIAAAAAAAA/41STW/TQBB9u0kTxwk0LV8J5asUUOkBtxU3ClKJAFkKRmqj
                SKinjb0kmzhrZG+iHnPih/APKg6VQEIR3PhRiFkTygEksLUz897OvN0Z+9v3
                j58BPMQ9hk2hozRR0bGnxVT1hVGJ9nxtZPpGhLI1UHH0qjeUoSmDMdSHYiq8
                WOi+94stMKz/TaMjM3OmU8YSQ2lPaWWeMBQ273drKMNxUUSFoWgGKmPYav/v
                XR4xOHthnMu54FbD8YPDzn7QelbDMmoVIusMG+0k7XtDaXqpUDrzhNaJyWUz
                L0hMMIljklppjxJDYt5LaUQkjCCOj6cFGhGzpmINGNiI+GNl0TZF0Q4dMJ+5
                Lm/wfM1nztd3vDGf7fJt9rTs8C/vS7zObequvcs/p0TnVgMxfZGKt4MHI8Ow
                djDRRo2lr6cqU71Y7v9ugMbWSiLJsNxWWgaTcU+mHUE5DKvtJBRxV6TK4gXp
                HiaTNJTPlQXNhXD3D1ns0OiK1G2JVtPOkvwtatniVfKcXvp0hNYJeXYu5Je2
                TuGe5Nu3F8nAY2yQrf1MQJUiUOG5s+IrlG2f6ifw16c4/wErJznBcSe3N3E3
                /08ZLpDAxSMUfFzycdmn0gaFaPq4irUjsAzXcJ32M9Qy3Mjg/AA8NOf95AIA
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
                H4sIAAAAAAAA/71W31MTVxT+bn6QzRrIEoxAEBCJMaCYGPEnSIsomgpIBbHW
                tnZJlrAQdp29m4x96TB96Eunf4AvPvQv0PqgU2Y6DH3rH9X23GVDQgKEmc74
                kHvPPfd853zn3HPv5u9//vgTwCgKDH2qkbdMPf8qZahlvaDaummk5tTyfUt9
                ufrQDoAxKGtqWU0VVaOQerS8puVI62WQqwCGW8mZoxzdKenFvGaNzdR7Ghta
                YpgfX7zVuDPxf1y2jNurOp+QIFGK66Zd1I3UWnkjpRu2ZhlqMZU1bEs3uJ7j
                AcgM0dyqllufM+25UrE4r1rqhkaGDOeTjf5rNAvCSYEihhBCq4wTaKNycVu1
                7Lsat3XD4SxBYQjH9fhKvLZmLEu2ccFzn/rcsdJmiDQyY/jl0KIJ0bTmLbOs
                H1y2xrRmagq3UjJywhVPTbvS5bGhI5kSm+1PyGZ85Hjt4np5YujUYs1TGJwx
                rUJqTbOXLZX6JaUahmmru7HddiGr+FFWZKIuFzUyCyzvspDQw3D6qMgB9Iom
                1onlBIM3KTqsH2dk9GGAuvKYZWXwW2bJ1vY3i1tPhjPNDphwVf/xvLailoo2
                w5tP2WTZAy54s0Prqb9VL0qZ0T36HQXNniqqnGcNuqlGTnusrTAMJA92u0j3
                2LEmv0quAdbfBBTCRYwE4cElhlh94Ke6vTppFRxHyWbxXWOi0Zk7zEnieC5C
                yOCKIDXK0EWksoahWY0lOYzSI2opK14FEaWofrCLgwk1OAjhBm4KQrcYpPFc
                0e373iOzCeC2jAlxH+LHyTqAzxl8yezuVbojYxJTh0Dr+QVwT8a0MG+vtOus
                Zqt51VYpd89G2UufUyaGoBhAb/u6EDy0+UoXUpqk/GWGf7c378jbm7KnyyN7
                JG/jrHicbcW1qZi6otJWo4kNKKGYJ916Vpa2N5W2LjbsSYfpp2RalHbaCD/Y
                +VmK3XaMIqTvqOgzsuRRTsZ8XSwdzXQqp2IdEV+EcM7Ymu7c+a3FI3UJ9F8f
                2fbm2XZiQzhGeonix3wE95LWR0p/VdmiBEgpkTJYVcrKiZ2fPAHZL+28zqSZ
                KEOGORVapC9zs4scqZS79k3qqyjvvbI1+oSbRmV38YeX4pntPvRMA3hGf1yq
                B8twohLt0jo9DT2PS4atb2hZo6xznR7tyepDTs0zZebpLQ3P6IY2V9pY1qxF
                8bALmmZOLS6pli7WrjK4oBfo+1+ySI7X+937i7EvQOuCrebWZ9WXrotQlalG
                2/KCWbJy2rQu9rpdl0sNRHGZLpKPms6LmHh9qNjf0qqF5m6aY+LyN+jo/tXp
                Ako3/LTy4DtazdIsGjk8HAl+RPhCpJ1G7+13osvxwoW1IYjvST5Dpm20jqCD
                dgmEk4iKi0GSglO0ozq4AESITiIrQpik8dPcWxtiJHJaBLrm20Lfsw84+3Zf
                wCgGnYCjBIxCcgKKzHsp4KATsNcNKKQ4zjl0etGDBFHcJdG1L+tl+nX73EVl
                jFVkJYjzSJIs+P5K4BaaE1Gf/8fX8LPZZsS9yDkjCzoZRByu/VSGASpcP33b
                q9n01WSTwJCbTWIvm8ReNgk3m24M44J7WqcdG8D3O1JvHfKVioFQtfl2I+30
                Sx3qaj3qfB3qGq43osbqUUP7UBLGqTV2izfldBMwuIUJKs9n75HawuQzJfwB
                d9/j6hamHfn+e4y923Pa5oDOQSY6p8i5F3lay7Q7ja/xnIJozpF+gxWay6R/
                QKXMPoc3iy+yeJjFDGazmMOjLObx5XMwjsdYICDHRY4RjmGOCxwZjiscabpJ
                HDc4bnJc47jO4edY5HjijFFOf/SxxDHIEXc0PRxPOb76Dzb5xaFeDQAA
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
                H4sIAAAAAAAA/41U32/bVBT+rp0fjpu2Tttt/RE2oKEk6Ta3ZaNj7QZtoatL
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
                fpfiZ5EgWaDdxcCbvvWLsSBNoNIUQaM3wNTFtJCM5n9B72EbLtYyzrZgkoFD
                CJMici+Cx7uD2SsD6KdCsCJgmlgKTomnkB6MHePSk3ZQQDbRJpsIya6FbC4A
                WgLDGAlzT4TFSqUj33wLRTCYz48dYSyALNIqgwkEutZh+tskBbX0U1x5cIzX
                B948woSIPEJOyx3h6hGuP+k6Rjpk9BIPWvV2DSbCGrQY/IYb3WVQwniGm9SW
                gMcXJEW7MvnJnxCNHE7+Cek7ROXDyRNIawLoKr3fC0sk6Emx1T45rvyNVJz2
                nYpl2hXL4BbeozzrpMcFqdlWDe63Qulm4R5WqHyftAANbJD8jOy3qVNz25AN
                zBu4Y+Au3icVHxhYwOI2mIclfLiNfk88H3lQW2vMg+Yh5WHAw6CHmy3jLQ+6
                hzTp/wKll0bx9QcAAA==
                """,
            """
                androidx/navigation/Outer＄InnerObject.class:
                H4sIAAAAAAAA/41TS08TURT+7p0+plMe5SFPxQcgLShTEFcQEyQ+hpRiLMEo
                q0s70oEygzO3DUtW7ty6cOnCFQuJCxJNDErc+KOI5w6jIARj0t7znTPnnO/M
                d+78PPr8FcAU7jIMC7fie05l23RFw1kT0vFcc7EubX/Icl3bX1xdt8syCcaQ
                WRcNYdaEu2b+jmoMiRnHdeQ9Bi2bW25CHAkDMSQZYrLqBAwjhf9imGbQpVeS
                vuOuMXRmc4UTtuMoZQwWPH/NXLflqi8cNzCF63oybBiYRU8W67UaZaVPtdXR
                Qo2rIqjOeRU7HNLSfkwdvaHB7Vd1UaMJL2ULZ99sOveCYehfbEQlVms20cU9
                WbV9hvbzXYh6plwL9THAlSi6VSwtzRbnHjShD0aKgv0MbYUNT1KauWBLURFS
                UCHfbGi0I6aOlDrAwDYovu0oL0+oMsHw8mBnwOA93OCZgx2D6wqkI6sbKpRp
                0Q9fGz0HO5M8z+4ndf79fYJn+HxHRuvj+diknon3xXpYnj0+fKvNpzIJiiYJ
                M8I64ZTCim2SqRl6L9xmEjnSvigaj3yxVR3fkAz9T+uudDZty204gUNyzZ5I
                SBfkeCWtBce1i/XNVdtfUpIqJb2yqC0L31F+FGwuSVHeWBBbkT90tvcT4YtN
                mwb5i6QpvAxzNREENrlGyav7Zfuho1r0Ri2Wzw2HCdpMLBS9Vy2K7C3yEmSb
                ycbpaTz0bpNnqtWo6Og+9D0CHONRsvrITDqbjhOQolaqaZoiPCy+HhVr7a0f
                w0cn6VqUfpqZvkK0Rbwnpe27F5QydKAzYrLIcrLdo2MfEI/tjn0Df4e4tjt2
                AP4sthsOnqczBp7Uw2ZdxwVRM4W66M9IHajLTJ8OAR09f6ToDguA9Bfw5/vo
                /YTLe2FAwySdSkeOUbSQqndCvjESSI3GcIXkGViBZuGqhWsWvd0Nghi0MITh
                FbAANzGyAiNQv2yARICOEHQFyIQgTecvRvQ+5dsEAAA=
                """,
            """
                androidx/navigation/Outer.class:
                H4sIAAAAAAAA/4VRy27TQBQ9M87DcQxNw6MJoeXRFJoi4bawKhVSiQBZSlOJ
                VpFQVpNklE7ijJHtRF1mxYfwBxWLSiChCHZ8FOLaDWSBKjzyPXMf59yZOz9/
                ffkG4DmeMJSF7gW+6p05WkxUX0TK187ROJJBFoyhMBAT4XhC952jzkB2oywM
                hsy+0ip6yWBs1lo20shYSCHLkIpOVchQaVyp+oLB3O96Cd8Cj0mm2zw+OWjW
                X9u4BitHwesM6w0/6DsDGXUCoXToCK39KNEJnaYfNceeR1LLjaEfkZhzKCPR
                E5GgGB9NDLodi00uNmBgQ4qfqdjbpl1vh6E2m9oWL3GLF2ZTi5uG+eMjL82m
                u3yb7XEj9Spr8u+fMrzAY8Iui2UsV2sZ1D0R0iXziXM5FYbqlTeuLkhZ3GPY
                +E/lnzk/oBZNMXkbiA+nT4fUovJurCM1kq6eqFB1PHmwmAmNvu73JMNSQ2nZ
                HI86MjgRVMNQbPhd4bVEoGJ/HrQXh5JEto79cdCVb1ScK8/7tP7pgh16nFQy
                0XL8VoRV8jKEBUJOK514G+Q58dwJ01sXMM+T9KN5MfAMj8nalwXIkRRgIv+X
                vELV8Zf/Cv7+AvZnLJ0nAQObZIuUvk//Kp3jIeEaYS1psY4twj2SWSbhYhuG
                ixsubrq4hdu0xYqLEsptsBB3UGkjHcIKcTdEJsRqiLXfdJuyTB0DAAA=
                """,
            """
                androidx/navigation/TestAbstract.class:
                H4sIAAAAAAAA/4VRy04CMRQ9LTDKiAo+wUeiLoy6cJS402jQREOCmChhw6ow
                Ey2PjpmWiUu+xT9wZeLCEJd+lPF2ZO/m5Dxum3Pb75+PTwAn2GTYEsqPQum/
                eErE8lEYGSqvEWhTaWsTiY6ZAmPId0UsvL5Qj95duxtYN8XgnEklzTlDam+/
                mUMGjos0phjS5klqhp3af5efMhRqvdD0pfJuAyN8YQR5fBCnqCCzkLUABtYj
                /0VadUTMP6bu45Hr8iJ3eZ7YeDS9WxyPyvyIXWa+Xh2e53auzOzpmbqIbyLx
                /HTYM9TvKvQDhvmaVEF9OGgHUUO0++Qs1MKO6DdFJK2emO5DOIw6wbW0onQ/
                VEYOgqbUktKKUqFJ9tLpbXBaf9LWvgZhkZSXaCBz8I7pNyIcJUInMS+wRpj7
                G0AWbpKvJ7iKjeSbqD5luRZSVcxWMVfFPPJEUahiAYstMI0lLFOu4WqsaDi/
                +rnGr+MBAAA=
                """,
            """
                androidx/navigation/TestClass.class:
                H4sIAAAAAAAA/31Ru07DMBQ9dmgKoUBaXuW9AgMBxAZCAiRQpAASoC5MbmMV
                t6mDYrfq2G/hD5iQGFDFyEchbgIzy9F5XFvn2l/f7x8AjrDBsCF0nKUqHgZa
                DFRbWJXq4EEae5EIY8pgDH5HDESQCN0Obpsd2bJlOAzuidLKnjI42zuNCkpw
                PUygzDBhn5Rh2Ir+vfmYoRp1U5soHVxLK2JhBXm8N3CoGsthKgcwsC75Q5Wr
                fWLxAcPmeOR5vM497hMbj+rj0SHfZ+elzxeX+zyfOmT52ekbMbjKxPPTXtdS
                tYs0lgxzkdLypt9ryuxBNBNyalHaEklDZCrXf6Z3n/azlrxUuVi562urerKh
                jKL0TOvUFisZHIDT5n9d84cgrJMKCg2Udt8w+UqEY4XQLcwAq4SV3wFMwSvy
                tQKXsV58D9WnrPIIJ8RMiNkQc/CJohqihvlHMIMFLFJu4BksGbg/2IItFdsB
                AAA=
                """,
            """
                androidx/navigation/TestClassWithArg.class:
                H4sIAAAAAAAA/41QTW/TQBScXSeO4ybECV9pChRoVbU54LTiBqoIkUCWQpFK
                FQ45bWIr2caxkXcT9ZjfwpkLEgiJA4o48qMQb52KEwcke96b59E8v/n1+/sP
                AE+xz7AvkjBLZXjlJ2IpJ0LLNPEvIqV7sVDqvdTTbjYpgTF4l2Ip/FgkE//t
                6DIa6xIsBvu5TKQ+ZSgcBkcDBuvwaFBBESUXBTjERTZhYEEFLrbK4KiQVE+l
                Yjjo/8/uZ7RjEumusSHzgKHen6U6lon/JtIiFFqQhM+XFp3EDJQNgJbOaH4l
                DetQFx4z9Narhsub3OXeeuXSwz3H5Y7VXK9OeIe9rDZsj7d4x/r50eZe4bz+
                lzmkbhWcomcbqxNmFmydieXrTHyYPplpuqqXhhFDrS+T6GwxH0XZhRjFNGn0
                07GIByKThl8P3XfpIhtHr6Qh2+eLRMt5NJBK0tdukqQ6T0PhmCIr5Oc0TILU
                ceqLsAl3iZ0S51Td9jeU2ztfUf2cax4SGg3I4RHhnY0KN1Az6VBn3ChMePRu
                vHwTGtVi+wuqn/5pU9kIrm04Huf4AHtUX+Q/WcTNIawAtwLcDmjtXWrRDLCN
                1hBMYQf3higp1BTuK7g52gqeQv0Pkn5Ve5gCAAA=
                """,
            """
                androidx/navigation/TestGraph.class:
                H4sIAAAAAAAA/32S32oTQRTGv5kkm80m2rT+aWKtVduLquC2xTuLUIvKwrqC
                DQHp1SQ7pJNsZmV3svQyVz6Ib1C8KChI0DsfSjyzBr0Q3IVzzvfNmR97Dvvj
                5+evAJ5gh2FT6DhLVXzua1GokTAq1X5P5uZVJt6f1cEY2mNRCD8ReuS/GYzl
                0NRRYXAOlVbmGUNl90G/hRocD1XUGarmTOUMW+F/yU8Z3MNhUjI8cHvRDaKT
                3lF0/KKFK/AaZF5l2A7TbOSPpRlkQuncF1qnpmTlfpSaaJYkhFoNJ6khmP9a
                GhELI8jj06JCUzIbGjaAgU3IP1dW7VEV7zPsLOaexzvc422qFnP3+wfeWcwP
                +B57Xnf5t48Ob3Pbe8AsoRmJohzg8cQwbLydaaOmMtCFytUgkUd/P482cZzG
                kmElVFpGs+lAZj1BPQxrYToUSV9kyuql6Z2ks2woXyoruktw/x8s9mkx1XKa
                rt0T5TukHMptypzeWqm2SPl2Zsq1h5dwL8rju8tm4BHuUWz9bkCDUICL5p/L
                69Rtn+YX8HeXaH3CykVpcNwv4ya2y9+I9k+AtVNUAlwLcD3ADdykEusBOuie
                guW4hQ06z+HluJ3D+QVOlj18gwIAAA==
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
                H4sIAAAAAAAA/32Sz27TQBDGv90kjuMEGsqfJhTaQnsAJHBbcaNCKhUgS8ZI
                NIqEetrEq3QTZ43sjdVjTjwIb1BxqAQSiuDGQyFmTYADErY0M9+3sz95Rv7+
                49MXAI+xw7AhdJylKj7ztSjUSBiVar8nc/N6MJZDUwdjaI9FIfxE6JH/260w
                OAdKK/OUoXLvfr+FGhwPVdQZquZU5Qxb4f/RTxjcg2FSQjxwe9MNouPeYXT0
                vIVL8BpkXmbYDtNs5I+lGWRC6dwXWqemhOV+lJpoliSEuhJOUkMw/5U0IhZG
                kMenRYXmZDY0bAADm5B/pqzapSreY9hZzD2Pd7jH21Qt5u6397yzmO/zXfas
                7vKvHxze5rZ3n1lCMxLFy0y8O300MQzrb2baqKkMdKFyNUjk4d/Po1UcpbFk
                WAmVltFsOpBZT1APw2qYDkXSF5myeml6x+ksG8oXyoruEtz/B4s9Wky1nKZr
                90R5g5RDuU2Z01sr1SYp385MufbgAu55eby1bAYe4g7F1q8GNAgFuGj+ubxG
                3fZpfgZ/e4HWR6yclwbH3TLexnb5I9H+CbB6gkqAqwGuBbiOG1RiLUAH3ROw
                HDexTuc5vBy3cjg/AW9Vq0qFAgAA
                """
        )

    override fun getDetector(): Detector = WrongStartDestinationTypeDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(WrongStartDestinationTypeDetector.WrongStartDestinationType)
}
