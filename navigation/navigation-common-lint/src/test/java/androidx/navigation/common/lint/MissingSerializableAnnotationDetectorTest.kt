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
import androidx.navigation.lint.common.TEST_CODE_SOURCE
import androidx.navigation.lint.common.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

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
                TEST_SERIALIZABLE_CLASS
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
                TEST_CLASS
            )
            .run()
            .expect(
                """
src/androidx/test/TestGraph.kt:11: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object TestObject
       ~~~~~~~~~~
src/androidx/test/TestGraph.kt:13: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
data object TestDataObject
            ~~~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:15: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class TestClass
      ~~~~~~~~~
src/androidx/test/TestGraph.kt:19: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object Outer {
       ~~~~~
src/androidx/test/TestGraph.kt:20: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
    data object InnerObject
                ~~~~~~~~~~~
src/androidx/test/TestGraph.kt:22: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
    data class InnerClass (
               ~~~~~~~~~~
src/androidx/test/TestGraph.kt:29: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class InterfaceChildClass(val arg: Boolean): TestInterface
      ~~~~~~~~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:30: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object InterfaceChildObject: TestInterface
       ~~~~~~~~~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:32: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
abstract class TestAbstract
               ~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:33: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class AbstractChildClass(val arg: Boolean): TestAbstract()
      ~~~~~~~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:34: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object AbstractChildObject: TestAbstract()
       ~~~~~~~~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:36: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
sealed class SealedClass {
             ~~~~~~~~~~~
src/androidx/test/TestGraph.kt:37: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
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
                TEST_SERIALIZABLE_CLASS
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
                TEST_CLASS
            )
            .run()
            .expect(
                """
src/androidx/test/TestGraph.kt:11: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object TestObject
       ~~~~~~~~~~
src/androidx/test/TestGraph.kt:13: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
data object TestDataObject
            ~~~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:15: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class TestClass
      ~~~~~~~~~
src/androidx/test/TestGraph.kt:19: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object Outer {
       ~~~~~
src/androidx/test/TestGraph.kt:20: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
    data object InnerObject
                ~~~~~~~~~~~
src/androidx/test/TestGraph.kt:22: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
    data class InnerClass (
               ~~~~~~~~~~
src/androidx/test/TestGraph.kt:29: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class InterfaceChildClass(val arg: Boolean): TestInterface
      ~~~~~~~~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:30: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object InterfaceChildObject: TestInterface
       ~~~~~~~~~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:32: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
abstract class TestAbstract
               ~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:33: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class AbstractChildClass(val arg: Boolean): TestAbstract()
      ~~~~~~~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:34: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object AbstractChildObject: TestAbstract()
       ~~~~~~~~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:36: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
sealed class SealedClass {
             ~~~~~~~~~~~
src/androidx/test/TestGraph.kt:37: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
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
                TEST_SERIALIZABLE_CLASS
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
                TEST_CLASS
            )
            .run()
            .expect(
                """
src/androidx/test/TestGraph.kt:11: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object TestObject
       ~~~~~~~~~~
src/androidx/test/TestGraph.kt:13: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
data object TestDataObject
            ~~~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:15: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class TestClass
      ~~~~~~~~~
src/androidx/test/TestGraph.kt:19: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object Outer {
       ~~~~~
src/androidx/test/TestGraph.kt:20: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
    data object InnerObject
                ~~~~~~~~~~~
src/androidx/test/TestGraph.kt:22: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
    data class InnerClass (
               ~~~~~~~~~~
src/androidx/test/TestGraph.kt:29: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class InterfaceChildClass(val arg: Boolean): TestInterface
      ~~~~~~~~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:30: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object InterfaceChildObject: TestInterface
       ~~~~~~~~~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:32: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
abstract class TestAbstract
               ~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:33: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class AbstractChildClass(val arg: Boolean): TestAbstract()
      ~~~~~~~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:34: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object AbstractChildObject: TestAbstract()
       ~~~~~~~~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:36: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
sealed class SealedClass {
             ~~~~~~~~~~~
src/androidx/test/TestGraph.kt:37: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
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
                TEST_SERIALIZABLE_CLASS
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
                TEST_CLASS
            )
            .run()
            .expect(
                """
src/androidx/test/TestGraph.kt:11: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object TestObject
       ~~~~~~~~~~
src/androidx/test/TestGraph.kt:13: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
data object TestDataObject
            ~~~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:15: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class TestClass
      ~~~~~~~~~
src/androidx/test/TestGraph.kt:19: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object Outer {
       ~~~~~
src/androidx/test/TestGraph.kt:20: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
    data object InnerObject
                ~~~~~~~~~~~
src/androidx/test/TestGraph.kt:22: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
    data class InnerClass (
               ~~~~~~~~~~
src/androidx/test/TestGraph.kt:29: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class InterfaceChildClass(val arg: Boolean): TestInterface
      ~~~~~~~~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:30: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object InterfaceChildObject: TestInterface
       ~~~~~~~~~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:32: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
abstract class TestAbstract
               ~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:33: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
class AbstractChildClass(val arg: Boolean): TestAbstract()
      ~~~~~~~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:34: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
object AbstractChildObject: TestAbstract()
       ~~~~~~~~~~~~~~~~~~~
src/androidx/test/TestGraph.kt:36: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
sealed class SealedClass {
             ~~~~~~~~~~~
src/androidx/test/TestGraph.kt:37: Error: To use this class or object as a type-safe destination, annotate it with @Serializable [MissingSerializableAnnotation]
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

    val SERIALIZABLE_TEST_CODE_SOURCE =
        """
package androidx.testSerializable

import kotlinx.serialization.Serializable

@Serializable class TestClass
@Serializable object TestObject
@Serializable data object TestDataObject
@Serializable object Outer {
    @Serializable data object InnerObject
    @Serializable class InnerClass
    class InnerClassNotUsed
}

// interface should not require @Serializable
interface TestInterface

@Serializable class InterfaceChildClass: TestInterface
@Serializable object InterfaceChildObject: TestInterface

@Serializable abstract class TestAbstract
@Serializable class AbstractChildClass(): TestAbstract()
@Serializable object AbstractChildObject: TestAbstract()

@Serializable sealed class SealedClass {
    @Serializable class SealedSubClass : SealedClass()
}
        """
            .trimIndent()

    internal val K_SERIALIZER =
        bytecodeStub(
            "KSerializer.kt",
            "kotlinx/serialization",
            0xdfbaa177,
            """
package kotlinx.serialization

public interface KSerializer<T>
        """,
            """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgsuUSTsxLKcrPTKnQy0ssy0xPLMnM
                zxPicsyrLMnIzEv3LhHi90ssc87PKynKz8lJLQIKcAIFPPKLS7xLuES5uJPz
                c/VSKxJzC3JShdhCUkHCSgxaDABdSlZNbgAAAA==
                """,
            """
                kotlinx/serialization/KSerializer.class:
                H4sIAAAAAAAA/4VQO0/DMBi8zy19hFfKs0wIsSAGUiomQEgsSBFFSLRi6eS2
                pnKbOlLsVhVTfhcDysyPQnxpGRAMeLj77nzy2f74fHsHcIE64Wgcu0ibeWBV
                omWkX6XTsQnu299SJWUQ4eS6c9kayZkMImmGwWNvpPru6uavRfB/e2UUCbXW
                sih4UE4OpJOcFJNZge9BOVRzAIHG7M91rho8Dc4Jh1nqeaIuPGbhZ2nlpZ6l
                p8VKlvrUFA2Rx5qE49a/T+FO6lBes/HDPRs7QrWth0a6aaIIXjueJn11pyMW
                B09T4/REPWure5G6NSZ2i4NtiYuxguUqYJdRMO8teAf7iy8mlDhT7qIQohKi
                GsLDKo9YC7GOjS7IYhM+71vULLYstr8A6rZa9Z8BAAA=
                """
        )

    internal val SERIALIZABLE_ANNOTATION =
        bytecodeStub(
            "Serializable.kt",
            "kotlinx/serialization",
            0xcce046a,
            """
package kotlinx.serialization

import kotlin.reflect.KClass

@MustBeDocumented
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS, AnnotationTarget.TYPE)
public annotation class Serializable(
    val with: KClass<out KSerializer<*>> = KSerializer::class // Default value indicates that auto-generated serializer is used
)
        """,
            """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgMuQSTsxLKcrPTKnQy0ssy0xPLMnM
                zxPi90ssc87PKynKz8lJLfIuEeIECnjkF5d4l3BJcXEn5+fqpVYk5hbkpApx
                h6QWlzjnJBYXe5coMWgxAADHc03gZwAAAA==
                """,
            """
                kotlinx/serialization/Serializable.class:
                H4sIAAAAAAAA/4VSW08TQRT+ZntbVoW2VC5F5GrlorYS3yAk3EwaWmjaYoJ9
                MEM74MKwazqzBfSlb/4Pf4YPpuHRH2U8K7bFsJGHOXMu35n5zuXnr+8/ALzB
                S4bZM1dL27nMKtG0ubQ/c227TrbStY6kiIExxE95i2cld06y+0enoq5jCDFM
                9b3ccVx9k7zRU2OIMIQvbP2RYXhhsdCHb0mu1CrD2l3v2nIhmNRul5Vori2t
                r65T+tz9UELN/0XdJln0lN4U227dOxeOFg2CpQNgVd48EZqCg1xK90I0bhwq
                +NF+5b08s1TeL+2Uq4cMka3CRqVCDakelnYYZgqB3fuH0nQwpiw0IUgjSKTF
                pScYMvdAS66061eUECsf7FXzRWIwGZzS4z4bHN+RwidYvfok/AL9aj4cVOi9
                RLclRaF5g2tOYeO8FaJlY74Y8AUY2Bn5L23fypHWeM3wvtNOWcaYYRnxCavT
                vlHpdNrm9VdjrNNeMXJscySZMo24kR5MWiZLRpOGGcqFc6HyxF3f9bdoNB0m
                dMT/YaU/r/+tOvEleqPdMpriWNKqZ3e72zp0G/zqTDMMVOwTh2uvSRNI9Me/
                LY65JyluVVyvWRdvbUmA8bJHozgX72xl0wN9uMoQSYTp76jfoDANCSZ5Fsky
                MIAlukN1WGQs/3Et4AXdXwj+gO6HlPiohpDAIIZ8EfdFAkmKDVMsJfAYIxj1
                1RoMgTEkfDGONCKYoMw8nuQxmcdTTJGK6TxmMFsDU5jDfA1RhWcKGYWYwnMF
                S8H8Da/SzgtFBAAA
                """
        )

    val TEST_CLASS = kotlin(TEST_CODE_SOURCE)

    val TEST_SERIALIZABLE_CLASS = kotlin(SERIALIZABLE_TEST_CODE_SOURCE)
    val STUBS = arrayOf(*NAVIGATION_STUBS, SERIALIZABLE_ANNOTATION, K_SERIALIZER)
}
