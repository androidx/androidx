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

import androidx.navigation.lint.common.NAVIGATION_STUBS
import androidx.navigation.lint.common.SERIALIZABLE_ANNOTATION
import androidx.navigation.lint.common.SERIALIZABLE_TEST_CLASS
import androidx.navigation.lint.common.TEST_CLASS
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

class MissingSerializableAnnotationDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = TypeSafeDestinationMissingAnnotationDetector()

    override fun getIssues(): List<Issue> =
        listOf(TypeSafeDestinationMissingAnnotationDetector.MissingSerializableAnnotationIssue)

    @Test
    fun testComposeNavigatorDestinationBuilderConstructor_noError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.navigation.compose.*
                import androidx.testSerializable.*

                fun navigation() {
                    ComposeNavigatorDestinationBuilder(route = TestClass::class)
                    ComposeNavigatorDestinationBuilder(route = TestObject::class)
                    ComposeNavigatorDestinationBuilder(route = TestDataObject::class)
                    ComposeNavigatorDestinationBuilder(route = Outer::class)
                    ComposeNavigatorDestinationBuilder(route = Outer.InnerObject::class)
                    ComposeNavigatorDestinationBuilder(route = Outer.InnerClass::class)
                    ComposeNavigatorDestinationBuilder(route = TestInterface::class)
                    ComposeNavigatorDestinationBuilder(route = InterfaceChildClass::class)
                    ComposeNavigatorDestinationBuilder(route = InterfaceChildObject::class)
                    ComposeNavigatorDestinationBuilder(route = TestAbstract::class)
                    ComposeNavigatorDestinationBuilder(route = AbstractChildClass::class)
                    ComposeNavigatorDestinationBuilder(route = AbstractChildObject::class)
                    ComposeNavigatorDestinationBuilder(route = SealedClass::class)
                    ComposeNavigatorDestinationBuilder(route = SealedClass.SealedSubClass::class)
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
    fun testComposeNavigatorDestinationBuilderConstructor_hasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.navigation.compose.*
                import androidx.test.*

                fun navigation() {
                    ComposeNavigatorDestinationBuilder(route = TestClass::class)
                    ComposeNavigatorDestinationBuilder(route = TestObject::class)
                    ComposeNavigatorDestinationBuilder(route = TestDataObject::class)
                    ComposeNavigatorDestinationBuilder(route = Outer::class)
                    ComposeNavigatorDestinationBuilder(route = Outer.InnerObject::class)
                    ComposeNavigatorDestinationBuilder(route = Outer.InnerClass::class)
                    ComposeNavigatorDestinationBuilder(route = TestInterface::class)
                    ComposeNavigatorDestinationBuilder(route = InterfaceChildClass::class)
                    ComposeNavigatorDestinationBuilder(route = InterfaceChildObject::class)
                    ComposeNavigatorDestinationBuilder(route = TestAbstract::class)
                    ComposeNavigatorDestinationBuilder(route = AbstractChildClass::class)
                    ComposeNavigatorDestinationBuilder(route = AbstractChildObject::class)
                    ComposeNavigatorDestinationBuilder(route = SealedClass::class)
                    ComposeNavigatorDestinationBuilder(route = SealedClass.SealedSubClass::class)
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
    fun testComposable_noError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.navigation.compose.*
                import androidx.testSerializable.*

                @Serializable class RouteClass

                fun navigation() {
                    val builder = NavGraphBuilder(route = RouteClass::class)

                    builder.composable<TestClass>()
                    builder.composable<TestObject>()
                    builder.composable<TestDataObject>()
                    builder.composable<Outer>()
                    builder.composable<Outer.InnerObject>()
                    builder.composable<Outer.InnerClass>()
                    builder.composable<TestInterface>()
                    builder.composable<InterfaceChildClass>()
                    builder.composable<InterfaceChildObject>()
                    builder.composable<TestAbstract>()
                    builder.composable<AbstractChildClass>()
                    builder.composable<AbstractChildObject>()
                    builder.composable<SealedClass>()
                    builder.composable<SealedClass.SealedSubClass>()
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
    fun testComposable_hasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.navigation.compose.*
                import androidx.test.*

                @Serializable class RouteClass

                fun navigation() {
                    val builder = NavGraphBuilder(route = RouteClass::class)

                    builder.composable<TestClass>()
                    builder.composable<TestObject>()
                    builder.composable<TestDataObject>()
                    builder.composable<Outer>()
                    builder.composable<Outer.InnerObject>()
                    builder.composable<Outer.InnerClass>()
                    builder.composable<TestInterface>()
                    builder.composable<InterfaceChildClass>()
                    builder.composable<InterfaceChildObject>()
                    builder.composable<TestAbstract>()
                    builder.composable<AbstractChildClass>()
                    builder.composable<AbstractChildObject>()
                    builder.composable<SealedClass>()
                    builder.composable<SealedClass.SealedSubClass>()
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
    fun testNavigation_noError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.navigation.compose.*
                import androidx.testSerializable.*

                @Serializable class RouteClass

                fun navigation() {
                    val builder = NavGraphBuilder(route = RouteClass::class)

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
    fun testNavigation_hasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.navigation.compose.*
                import androidx.test.*

                @Serializable class RouteClass

                fun navigation() {
                    val builder = NavGraphBuilder(route = RouteClass::class)

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
    fun testDialogNavigatorDestinationBuilderConstructor_noError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.navigation.compose.*
                import androidx.testSerializable.*

                fun navigation() {
                    DialogNavigatorDestinationBuilder(route = TestClass::class)
                    DialogNavigatorDestinationBuilder(route = TestObject::class)
                    DialogNavigatorDestinationBuilder(route = TestDataObject::class)
                    DialogNavigatorDestinationBuilder(route = Outer::class)
                    DialogNavigatorDestinationBuilder(route = Outer.InnerObject::class)
                    DialogNavigatorDestinationBuilder(route = Outer.InnerClass::class)
                    DialogNavigatorDestinationBuilder(route = TestInterface::class)
                    DialogNavigatorDestinationBuilder(route = InterfaceChildClass::class)
                    DialogNavigatorDestinationBuilder(route = InterfaceChildObject::class)
                    DialogNavigatorDestinationBuilder(route = TestAbstract::class)
                    DialogNavigatorDestinationBuilder(route = AbstractChildClass::class)
                    DialogNavigatorDestinationBuilder(route = AbstractChildObject::class)
                    DialogNavigatorDestinationBuilder(route = SealedClass::class)
                    DialogNavigatorDestinationBuilder(route = SealedClass.SealedSubClass::class)
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
    fun testDialogNavigatorDestinationBuilderConstructor_hasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.navigation.compose.*
                import androidx.test.*

                fun navigation() {
                    DialogNavigatorDestinationBuilder(route = TestClass::class)
                    DialogNavigatorDestinationBuilder(route = TestObject::class)
                    DialogNavigatorDestinationBuilder(route = TestDataObject::class)
                    DialogNavigatorDestinationBuilder(route = Outer::class)
                    DialogNavigatorDestinationBuilder(route = Outer.InnerObject::class)
                    DialogNavigatorDestinationBuilder(route = Outer.InnerClass::class)
                    DialogNavigatorDestinationBuilder(route = TestInterface::class)
                    DialogNavigatorDestinationBuilder(route = InterfaceChildClass::class)
                    DialogNavigatorDestinationBuilder(route = InterfaceChildObject::class)
                    DialogNavigatorDestinationBuilder(route = TestAbstract::class)
                    DialogNavigatorDestinationBuilder(route = AbstractChildClass::class)
                    DialogNavigatorDestinationBuilder(route = AbstractChildObject::class)
                    DialogNavigatorDestinationBuilder(route = SealedClass::class)
                    DialogNavigatorDestinationBuilder(route = SealedClass.SealedSubClass::class)
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
    fun testDialog_noError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.navigation.compose.*
                import androidx.testSerializable.*

                @Serializable class RouteClass

                fun navigation() {
                    val builder = NavGraphBuilder(route = RouteClass::class)

                    builder.dialog<TestClass>()
                    builder.dialog<TestObject>()
                    builder.dialog<TestDataObject>()
                    builder.dialog<Outer>()
                    builder.dialog<Outer.InnerObject>()
                    builder.dialog<Outer.InnerClass>()
                    builder.dialog<TestInterface>()
                    builder.dialog<InterfaceChildClass>()
                    builder.dialog<InterfaceChildObject>()
                    builder.dialog<TestAbstract>()
                    builder.dialog<AbstractChildClass>()
                    builder.dialog<AbstractChildObject>()
                    builder.dialog<SealedClass>()
                    builder.dialog<SealedClass.SealedSubClass>()
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
    fun testDialog_hasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.navigation.compose.*
                import androidx.test.*

                @Serializable class RouteClass

                fun navigation() {
                    val builder = NavGraphBuilder(route = RouteClass::class)

                    builder.dialog<TestClass>()
                    builder.dialog<TestObject>()
                    builder.dialog<TestDataObject>()
                    builder.dialog<Outer>()
                    builder.dialog<Outer.InnerObject>()
                    builder.dialog<Outer.InnerClass>()
                    builder.dialog<TestInterface>()
                    builder.dialog<InterfaceChildClass>()
                    builder.dialog<InterfaceChildObject>()
                    builder.dialog<TestAbstract>()
                    builder.dialog<AbstractChildClass>()
                    builder.dialog<AbstractChildObject>()
                    builder.dialog<SealedClass>()
                    builder.dialog<SealedClass.SealedSubClass>()
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

    val STUBS =
        arrayOf(
            *NAVIGATION_STUBS,
            COMPOSE_NAVIGATOR_DESTINATION_BUILDER,
            DIALOG_NAVIGATOR_DESTINATION_BUILDER,
            SERIALIZABLE_ANNOTATION
        )
}
