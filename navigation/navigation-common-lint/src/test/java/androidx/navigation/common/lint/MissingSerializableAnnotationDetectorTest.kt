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

import androidx.navigation.lint.common.K_SERIALIZER
import androidx.navigation.lint.common.NAVIGATION_STUBS
import androidx.navigation.lint.common.NAV_DEEP_LINK
import androidx.navigation.lint.common.SERIALIZABLE_ANNOTATION
import androidx.navigation.lint.common.SERIALIZABLE_TEST_CLASS
import androidx.navigation.lint.common.TEST_CLASS
import androidx.navigation.lint.common.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MissingSerializableAnnotationDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = TypeSafeDestinationMissingAnnotationDetector()

    override fun getIssues(): List<Issue> =
        listOf(TypeSafeDestinationMissingAnnotationDetector.MissingSerializableAnnotationIssue)

    @Test
    fun testNavDestinationBuilderConstructor_noError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.testSerializable.*

                fun navigation() {
                    NavDestinationBuilder<NavGraph>(route = TestClass::class)
                    NavDestinationBuilder<NavGraph>(route = TestObject::class)
                    NavDestinationBuilder<NavGraph>(route = TestDataObject::class)
                    NavDestinationBuilder<NavGraph>(route = Outer::class)
                    NavDestinationBuilder<NavGraph>(route = Outer.InnerObject::class)
                    NavDestinationBuilder<NavGraph>(route = Outer.InnerClass::class)
                    NavDestinationBuilder<NavGraph>(route = TestInterface::class)
                    NavDestinationBuilder<NavGraph>(route = InterfaceChildClass::class)
                    NavDestinationBuilder<NavGraph>(route = InterfaceChildObject::class)
                    NavDestinationBuilder<NavGraph>(route = TestAbstract::class)
                    NavDestinationBuilder<NavGraph>(route = AbstractChildClass::class)
                    NavDestinationBuilder<NavGraph>(route = AbstractChildObject::class)
                    NavDestinationBuilder<NavGraph>(route = SealedClass::class)
                    NavDestinationBuilder<NavGraph>(route = SealedClass.SealedSubClass::class)
                }
                """
                    )
                    .indented(),
                *STUBS,
                SERIALIZABLE_TEST_CLASS.kotlin
            )
            .run()
            .expectClean()
    }

    @Test
    fun testNavDestinationBuilderConstructor_hasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.test.*

                fun navigation() {
                    NavDestinationBuilder<NavGraph>(route = TestClass::class)
                    NavDestinationBuilder<NavGraph>(route = TestObject::class)
                    NavDestinationBuilder<NavGraph>(route = TestDataObject::class)
                    NavDestinationBuilder<NavGraph>(route = Outer::class)
                    NavDestinationBuilder<NavGraph>(route = Outer.InnerObject::class)
                    NavDestinationBuilder<NavGraph>(route = Outer.InnerClass::class)
                    NavDestinationBuilder<NavGraph>(route = TestInterface::class)
                    NavDestinationBuilder<NavGraph>(route = InterfaceChildClass::class)
                    NavDestinationBuilder<NavGraph>(route = InterfaceChildObject::class)
                    NavDestinationBuilder<NavGraph>(route = TestAbstract::class)
                    NavDestinationBuilder<NavGraph>(route = AbstractChildClass::class)
                    NavDestinationBuilder<NavGraph>(route = AbstractChildObject::class)
                    NavDestinationBuilder<NavGraph>(route = SealedClass::class)
                    NavDestinationBuilder<NavGraph>(route = SealedClass.SealedSubClass::class)
                }
                """
                    )
                    .indented(),
                *STUBS,
                TEST_CLASS.kotlin
            )
            .run()
            .expect(
                """
src/androidx/test/Test.kt:11: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object TestObject
       ~~~~~~~~~~
src/androidx/test/Test.kt:13: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
data object TestDataObject
            ~~~~~~~~~~~~~~
src/androidx/test/Test.kt:15: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class TestClass
      ~~~~~~~~~
src/androidx/test/Test.kt:19: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object Outer {
       ~~~~~
src/androidx/test/Test.kt:20: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
    data object InnerObject
                ~~~~~~~~~~~
src/androidx/test/Test.kt:22: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
    data class InnerClass (
               ~~~~~~~~~~
src/androidx/test/Test.kt:29: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class InterfaceChildClass(val arg: Boolean): TestInterface
      ~~~~~~~~~~~~~~~~~~~
src/androidx/test/Test.kt:30: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object InterfaceChildObject: TestInterface
       ~~~~~~~~~~~~~~~~~~~~
src/androidx/test/Test.kt:32: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
abstract class TestAbstract
               ~~~~~~~~~~~~
src/androidx/test/Test.kt:33: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class AbstractChildClass(val arg: Boolean): TestAbstract()
      ~~~~~~~~~~~~~~~~~~
src/androidx/test/Test.kt:34: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object AbstractChildObject: TestAbstract()
       ~~~~~~~~~~~~~~~~~~~
src/androidx/test/Test.kt:36: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
sealed class SealedClass {
             ~~~~~~~~~~~
src/androidx/test/Test.kt:37: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
    class SealedSubClass : SealedClass()
          ~~~~~~~~~~~~~~
13 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testNavGraphBuilderConstructor_noError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.testSerializable.*

                fun navigation() {
                    NavGraphBuilder(route = TestClass::class)
                    NavGraphBuilder(route = TestObject::class)
                    NavGraphBuilder(route = TestDataObject::class)
                    NavGraphBuilder(route = Outer::class)
                    NavGraphBuilder(route = Outer.InnerObject::class)
                    NavGraphBuilder(route = Outer.InnerClass::class)
                    NavGraphBuilder(route = TestInterface::class)
                    NavGraphBuilder(route = InterfaceChildClass::class)
                    NavGraphBuilder(route = InterfaceChildObject::class)
                    NavGraphBuilder(route = TestAbstract::class)
                    NavGraphBuilder(route = AbstractChildClass::class)
                    NavGraphBuilder(route = AbstractChildObject::class)
                    NavGraphBuilder(route = SealedClass::class)
                    NavGraphBuilder(route = SealedClass.SealedSubClass::class)
                }
                """
                    )
                    .indented(),
                *STUBS,
                SERIALIZABLE_TEST_CLASS.kotlin
            )
            .run()
            .expectClean()
    }

    @Test
    fun testNavGraphBuilderConstructor_hasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.test.*

                fun navigation() {
                    NavGraphBuilder(route = TestClass::class)
                    NavGraphBuilder(route = TestObject::class)
                    NavGraphBuilder(route = TestDataObject::class)
                    NavGraphBuilder(route = Outer::class)
                    NavGraphBuilder(route = Outer.InnerObject::class)
                    NavGraphBuilder(route = Outer.InnerClass::class)
                    NavGraphBuilder(route = TestInterface::class)
                    NavGraphBuilder(route = InterfaceChildClass::class)
                    NavGraphBuilder(route = InterfaceChildObject::class)
                    NavGraphBuilder(route = TestAbstract::class)
                    NavGraphBuilder(route = AbstractChildClass::class)
                    NavGraphBuilder(route = AbstractChildObject::class)
                    NavGraphBuilder(route = SealedClass::class)
                    NavGraphBuilder(route = SealedClass.SealedSubClass::class)
                }
                """
                    )
                    .indented(),
                *STUBS,
                TEST_CLASS.kotlin
            )
            .run()
            .expect(
                """
src/androidx/test/Test.kt:11: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object TestObject
       ~~~~~~~~~~
src/androidx/test/Test.kt:13: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
data object TestDataObject
            ~~~~~~~~~~~~~~
src/androidx/test/Test.kt:15: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class TestClass
      ~~~~~~~~~
src/androidx/test/Test.kt:19: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object Outer {
       ~~~~~
src/androidx/test/Test.kt:20: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
    data object InnerObject
                ~~~~~~~~~~~
src/androidx/test/Test.kt:22: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
    data class InnerClass (
               ~~~~~~~~~~
src/androidx/test/Test.kt:29: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class InterfaceChildClass(val arg: Boolean): TestInterface
      ~~~~~~~~~~~~~~~~~~~
src/androidx/test/Test.kt:30: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object InterfaceChildObject: TestInterface
       ~~~~~~~~~~~~~~~~~~~~
src/androidx/test/Test.kt:32: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
abstract class TestAbstract
               ~~~~~~~~~~~~
src/androidx/test/Test.kt:33: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class AbstractChildClass(val arg: Boolean): TestAbstract()
      ~~~~~~~~~~~~~~~~~~
src/androidx/test/Test.kt:34: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object AbstractChildObject: TestAbstract()
       ~~~~~~~~~~~~~~~~~~~
src/androidx/test/Test.kt:36: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
sealed class SealedClass {
             ~~~~~~~~~~~
src/androidx/test/Test.kt:37: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
    class SealedSubClass : SealedClass()
          ~~~~~~~~~~~~~~
13 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testNavGraphBuilderNavigation_noError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import kotlinx.serialization.*
                import androidx.testSerializable.*

                fun navigation() {
                    val builder = NavGraphBuilder(provider, TestGraph, null)

                    builder.navigation<TestClass>()
                    builder.navigation<TestObject>()
                    builder.navigation<TestDataObject>()
                    builder.navigation<Outer>()
                    builder.navigation<Outer.InnerObject>()
                    builder.navigation<Outer.InnerClass>()
                    builder.navigation<TestInterface>()
                    builder.navigation<InterfaceChildClass>()
                    builder.navigation<InterfaceChildObject>()
                    builder.navigation<TestAbstract>()
                    builder.navigation<AbstractChildClass>()
                    builder.navigation<AbstractChildObject>()
                    builder.navigation<SealedClass>()
                    builder.navigation<SealedClass.SealedSubClass>()

                }
                """
                    )
                    .indented(),
                *STUBS,
                SERIALIZABLE_TEST_CLASS.kotlin
            )
            .run()
            .expectClean()
    }

    @Test
    fun testNavGraphBuilderNavigation_hasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import kotlinx.serialization.*
                import androidx.test.*

                fun navigation() {
                    val builder = NavGraphBuilder(provider, TestGraph, null)

                    builder.navigation<TestClass>()
                    builder.navigation<TestObject>()
                    builder.navigation<TestDataObject>()
                    builder.navigation<Outer>()
                    builder.navigation<Outer.InnerObject>()
                    builder.navigation<Outer.InnerClass>()
                    builder.navigation<TestInterface>()
                    builder.navigation<InterfaceChildClass>()
                    builder.navigation<InterfaceChildObject>()
                    builder.navigation<TestAbstract>()
                    builder.navigation<AbstractChildClass>()
                    builder.navigation<AbstractChildObject>()
                    builder.navigation<SealedClass>()
                    builder.navigation<SealedClass.SealedSubClass>()

                }
                """
                    )
                    .indented(),
                *STUBS,
                TEST_CLASS.kotlin
            )
            .run()
            .expect(
                """
src/androidx/test/Test.kt:11: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object TestObject
       ~~~~~~~~~~
src/androidx/test/Test.kt:13: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
data object TestDataObject
            ~~~~~~~~~~~~~~
src/androidx/test/Test.kt:15: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class TestClass
      ~~~~~~~~~
src/androidx/test/Test.kt:19: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object Outer {
       ~~~~~
src/androidx/test/Test.kt:20: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
    data object InnerObject
                ~~~~~~~~~~~
src/androidx/test/Test.kt:22: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
    data class InnerClass (
               ~~~~~~~~~~
src/androidx/test/Test.kt:29: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class InterfaceChildClass(val arg: Boolean): TestInterface
      ~~~~~~~~~~~~~~~~~~~
src/androidx/test/Test.kt:30: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object InterfaceChildObject: TestInterface
       ~~~~~~~~~~~~~~~~~~~~
src/androidx/test/Test.kt:32: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
abstract class TestAbstract
               ~~~~~~~~~~~~
src/androidx/test/Test.kt:33: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class AbstractChildClass(val arg: Boolean): TestAbstract()
      ~~~~~~~~~~~~~~~~~~
src/androidx/test/Test.kt:34: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object AbstractChildObject: TestAbstract()
       ~~~~~~~~~~~~~~~~~~~
src/androidx/test/Test.kt:36: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
sealed class SealedClass {
             ~~~~~~~~~~~
src/androidx/test/Test.kt:37: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
    class SealedSubClass : SealedClass()
          ~~~~~~~~~~~~~~
13 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testNavProviderNavigation_noError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import kotlinx.serialization.*
                import androidx.testSerializable.*

                fun navigation() {
                    val provider = NavigatorProvider()

                    provider.navigation(route = TestClass::class)
                    provider.navigation(route = TestObject::class)
                    provider.navigation(route = TestDataObject::class)
                    provider.navigation(route = Outer::class)
                    provider.navigation(route = Outer.InnerObject::class)
                    provider.navigation(route = Outer.InnerClass::class)
                    provider.navigation(route = TestInterface::class)
                    provider.navigation(route = InterfaceChildClass::class)
                    provider.navigation(route = InterfaceChildObject::class)
                    provider.navigation(route = TestAbstract::class)
                    provider.navigation(route = AbstractChildClass::class)
                    provider.navigation(route = AbstractChildObject::class)
                    provider.navigation(route = SealedClass::class)
                    provider.navigation(route = SealedClass.SealedSubClass::class)
                }
                """
                    )
                    .indented(),
                *STUBS,
                SERIALIZABLE_TEST_CLASS.kotlin
            )
            .run()
            .expectClean()
    }

    @Test
    fun testNavProviderNavigation_hasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import kotlinx.serialization.*
                import androidx.test.*

                fun navigation() {
                    val provider = NavigatorProvider()

                    provider.navigation(route = TestClass::class)
                    provider.navigation(route = TestObject::class)
                    provider.navigation(route = TestDataObject::class)
                    provider.navigation(route = Outer::class)
                    provider.navigation(route = Outer.InnerObject::class)
                    provider.navigation(route = Outer.InnerClass::class)
                    provider.navigation(route = TestInterface::class)
                    provider.navigation(route = InterfaceChildClass::class)
                    provider.navigation(route = InterfaceChildObject::class)
                    provider.navigation(route = TestAbstract::class)
                    provider.navigation(route = AbstractChildClass::class)
                    provider.navigation(route = AbstractChildObject::class)
                    provider.navigation(route = SealedClass::class)
                    provider.navigation(route = SealedClass.SealedSubClass::class)
                }
                """
                    )
                    .indented(),
                *STUBS,
                TEST_CLASS.kotlin
            )
            .run()
            .expect(
                """
src/androidx/test/Test.kt:11: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object TestObject
       ~~~~~~~~~~
src/androidx/test/Test.kt:13: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
data object TestDataObject
            ~~~~~~~~~~~~~~
src/androidx/test/Test.kt:15: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class TestClass
      ~~~~~~~~~
src/androidx/test/Test.kt:19: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object Outer {
       ~~~~~
src/androidx/test/Test.kt:20: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
    data object InnerObject
                ~~~~~~~~~~~
src/androidx/test/Test.kt:22: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
    data class InnerClass (
               ~~~~~~~~~~
src/androidx/test/Test.kt:29: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class InterfaceChildClass(val arg: Boolean): TestInterface
      ~~~~~~~~~~~~~~~~~~~
src/androidx/test/Test.kt:30: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object InterfaceChildObject: TestInterface
       ~~~~~~~~~~~~~~~~~~~~
src/androidx/test/Test.kt:32: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
abstract class TestAbstract
               ~~~~~~~~~~~~
src/androidx/test/Test.kt:33: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class AbstractChildClass(val arg: Boolean): TestAbstract()
      ~~~~~~~~~~~~~~~~~~
src/androidx/test/Test.kt:34: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object AbstractChildObject: TestAbstract()
       ~~~~~~~~~~~~~~~~~~~
src/androidx/test/Test.kt:36: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
sealed class SealedClass {
             ~~~~~~~~~~~
src/androidx/test/Test.kt:37: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
    class SealedSubClass : SealedClass()
          ~~~~~~~~~~~~~~
13 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testDeeplink_noError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import kotlinx.serialization.*

                @Serializable class TestClass
                @Serializable class DeepLink

                fun navigation() {
                    val builder = NavDestinationBuilder<NavGraph>(route = TestClass::class)
                    builder.deepLink<DeepLink>()
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
    fun testDeeplink_hasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import kotlinx.serialization.*

                @Serializable class TestClass
                class DeepLink

                fun navigation() {
                    val builder = NavDestinationBuilder<NavGraph>(route = TestClass::class)
                    builder.deepLink<DeepLink>()
                }
                """
                    )
                    .indented(),
                *STUBS,
            )
            .run()
            .expect(
                """
src/com/example/TestClass.kt:7: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class DeepLink
      ~~~~~~~~
1 errors, 0 warnings
            """
                    .trimIndent()
            )
    }

    @Test
    fun testDeeplinkBuilderSetUriPattern_noError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import kotlinx.serialization.Serializable

                @Serializable class DeepLink

                fun navigation() {
                    val builder = NavDeepLink.Builder()
                    builder.setUriPattern<DeepLink>()
                }
                """
                    )
                    .indented(),
                *STUBS,
                NAV_DEEP_LINK
            )
            .run()
            .expectClean()
    }

    @Test
    fun testDeeplinkBuilderSetUriPattern_hasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*

                class DeepLink

                fun navigation() {
                    val builder = NavDeepLink.Builder()
                    builder.setUriPattern<DeepLink>()
                }
                """
                    )
                    .indented(),
                *STUBS,
                NAV_DEEP_LINK
            )
            .run()
            .expect(
                """
src/com/example/DeepLink.kt:5: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class DeepLink
      ~~~~~~~~
1 errors, 0 warnings
            """
                    .trimIndent()
            )
    }

    @Test
    fun testNavDeepLink_noError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import kotlinx.serialization.Serializable

                @Serializable class DeepLink

                fun navigation() {
                    navDeepLink<DeepLink>()
                }
                """
                    )
                    .indented(),
                *STUBS,
                NAV_DEEP_LINK
            )
            .run()
            .expectClean()
    }

    @Test
    fun testNavDeepLink_hasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*

                class DeepLink

                fun navigation() {
                    navDeepLink<DeepLink>()

                }
                """
                    )
                    .indented(),
                *STUBS,
                NAV_DEEP_LINK
            )
            .run()
            .expect(
                """
src/com/example/DeepLink.kt:5: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class DeepLink
      ~~~~~~~~
1 errors, 0 warnings
            """
                    .trimIndent()
            )
    }

    @Test
    fun testWrongPackage_noError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.NavGraphBuilder
                import com.test.navigation
                import kotlinx.serialization.*

                @Serializable object TestGraph
                object TestClass

                fun navigation() {
                   val builder = NavGraphBuilder(route = TestGraph::class)

                    builder.navigation<TestClass>()
                }
                """
                    )
                    .indented(),
                *STUBS,
                CUSTOM_NAV_GRAPH_BUILDER_EXTENSIONS
            )
            .run()
            .expectClean()
    }

    private val CUSTOM_NAV_GRAPH_BUILDER_EXTENSIONS =
        bytecodeStub(
            "NavGraphBuilderNavigation.kt",
            "com/test",
            0x8c23ef1e,
            """
package com.test

import androidx.navigation.NavGraphBuilder

// NavGraphBuilder
inline fun <reified T : Any> NavGraphBuilder.navigation() { }
        """,
            """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgsuUSTsxLKcrPTKnQy0ssy0xPLMnM
                zxPicsyrLMnIzEv3LhHi90ssc87PKynKz8lJLQIKcAIFPPKLS7xLuKS4uJPz
                c/VSKxJzC3JShbhDUotL3IsSCzKAcupcHCC5EqCQkDRQC1jcqTQzJyW1yA9u
                lXeJEoMWAwA5gn4YnAAAAA==
                """,
            """
                com/test/NavGraphBuilderNavigationKt.class:
                H4sIAAAAAAAA/41RTW/TQBB966SJYwpNU1qSUgoUl6Y94BT11IZIgARYpAGR
                KJecNrZJNrHXyN5EPfbE/+GGOKCKIz8KMWsqKEVIlbwzb94+z9d+//HlK4AD
                7DLYXhw5KkiV0+Hzlwn/MH42E6EfJBSKEVcilq9VEYyhPOFz7oRcjpw3w0ng
                EZtjsORvHUO93ubST2Lhnzh/+MuZj3b7DK1m77B9OeNR6+oJCk01FmnLhMmw
                OY1VKKQzmUeOkCpIJA8dV6pEyFR4aREWw6o3DrxpJ1adWRi+5QmPAhIy7NT/
                beMC09VJRlRxEYu4buEabjAs2cJ+b1+cnLm0IFt39Be9faVxGJbb5xMcB4r7
                XHHijGieo1di2pS0AZWZamDQ5YnQqEHI32c4ODstW2enllE2fjltqsZ6jcC6
                0WBblkkKo8r2jEaOTv7Vt4+m/vcxy9L2GDb++/6Ppooh/zz2A5q8LWTQmUXD
                IOnxYUhMpR17POzzROj4nCx1xUhyNUsIW914lnjBC6Evau9mUoko6ItUkPKp
                lLHKiqTYh4E8sjnLNSygQPEDip6QN/QO9iqlz1jKNT/pFcAmW6DeTRSxTXiN
                OJPiMpbJkhwVrJB/mKmLdHYytIU6+UPS3KQiqwPkXKy5uOWiipqLddx2sYE7
                A7AUm7g7wEKqv3sp7md25Se3iEt3PgMAAA==
                """
        )

    val STUBS = arrayOf(*NAVIGATION_STUBS, SERIALIZABLE_ANNOTATION, K_SERIALIZER)
}
