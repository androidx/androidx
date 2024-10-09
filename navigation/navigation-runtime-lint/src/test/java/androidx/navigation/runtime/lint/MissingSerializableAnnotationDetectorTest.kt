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
    fun testActivityNavigatorDestinationBuilderConstructor_noError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.testSerializable.*

                fun navigation() {
                    ActivityNavigatorDestinationBuilder(route = TestClass::class)
                    ActivityNavigatorDestinationBuilder(route = TestObject::class)
                    ActivityNavigatorDestinationBuilder(route = TestDataObject::class)
                    ActivityNavigatorDestinationBuilder(route = Outer::class)
                    ActivityNavigatorDestinationBuilder(route = Outer.InnerObject::class)
                    ActivityNavigatorDestinationBuilder(route = Outer.InnerClass::class)
                    ActivityNavigatorDestinationBuilder(route = TestInterface::class)
                    ActivityNavigatorDestinationBuilder(route = InterfaceChildClass::class)
                    ActivityNavigatorDestinationBuilder(route = InterfaceChildObject::class)
                    ActivityNavigatorDestinationBuilder(route = TestAbstract::class)
                    ActivityNavigatorDestinationBuilder(route = AbstractChildClass::class)
                    ActivityNavigatorDestinationBuilder(route = AbstractChildObject::class)
                    ActivityNavigatorDestinationBuilder(route = SealedClass::class)
                    ActivityNavigatorDestinationBuilder(route = SealedClass.SealedSubClass::class)
                }
                """
                    )
                    .indented(),
                *STUBS,
                SERIALIZABLE_TEST_CLASS
            )
            .run()
            .expectClean()
    }

    @Test
    fun testActivityNavigatorDestinationBuilderConstructor_hasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.test.*

                fun navigation() {
                    ActivityNavigatorDestinationBuilder(route = TestClass::class)
                    ActivityNavigatorDestinationBuilder(route = TestObject::class)
                    ActivityNavigatorDestinationBuilder(route = TestDataObject::class)
                    ActivityNavigatorDestinationBuilder(route = Outer::class)
                    ActivityNavigatorDestinationBuilder(route = Outer.InnerObject::class)
                    ActivityNavigatorDestinationBuilder(route = Outer.InnerClass::class)
                    ActivityNavigatorDestinationBuilder(route = TestInterface::class)
                    ActivityNavigatorDestinationBuilder(route = InterfaceChildClass::class)
                    ActivityNavigatorDestinationBuilder(route = InterfaceChildObject::class)
                    ActivityNavigatorDestinationBuilder(route = TestAbstract::class)
                    ActivityNavigatorDestinationBuilder(route = AbstractChildClass::class)
                    ActivityNavigatorDestinationBuilder(route = AbstractChildObject::class)
                    ActivityNavigatorDestinationBuilder(route = SealedClass::class)
                    ActivityNavigatorDestinationBuilder(route = SealedClass.SealedSubClass::class)
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
    fun testActivity_noError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.testSerializable.*

                @Serializable class RouteClass

                fun navigation() {
                    val builder = NavGraphBuilder(route = RouteClass::class)

                    builder.activity<TestClass>()
                    builder.activity<TestObject>()
                    builder.activity<TestDataObject>()
                    builder.activity<Outer>()
                    builder.activity<Outer.InnerObject>()
                    builder.activity<Outer.InnerClass>()
                    builder.activity<TestInterface>()
                    builder.activity<InterfaceChildClass>()
                    builder.activity<InterfaceChildObject>()
                    builder.activity<TestAbstract>()
                    builder.activity<AbstractChildClass>()
                    builder.activity<AbstractChildObject>()
                    builder.activity<SealedClass>()
                    builder.activity<SealedClass.SealedSubClass>()
                }
                """
                    )
                    .indented(),
                *STUBS,
                TEST_CLASS
            )
            .run()
            .expectClean()
    }

    @Test
    fun testActivity_hasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.test.*

                @Serializable class RouteClass

                fun navigation() {
                    val builder = NavGraphBuilder(route = RouteClass::class)

                    builder.activity<TestClass>()
                    builder.activity<TestObject>()
                    builder.activity<TestDataObject>()
                    builder.activity<Outer>()
                    builder.activity<Outer.InnerObject>()
                    builder.activity<Outer.InnerClass>()
                    builder.activity<TestInterface>()
                    builder.activity<InterfaceChildClass>()
                    builder.activity<InterfaceChildObject>()
                    builder.activity<TestAbstract>()
                    builder.activity<AbstractChildClass>()
                    builder.activity<AbstractChildObject>()
                    builder.activity<SealedClass>()
                    builder.activity<SealedClass.SealedSubClass>()
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
        arrayOf(*NAVIGATION_STUBS, ACTIVITY_NAVIGATION_DESTINATION_BUILDER, SERIALIZABLE_ANNOTATION)
            .map { it.toTestBytecodeStub() }
            .toTypedArray()
    val SERIALIZABLE_TEST_CLASS =
        androidx.navigation.lint.common.SERIALIZABLE_TEST_CLASS.toTestKotlinAndBytecodeStub().kotlin
    val TEST_CLASS = androidx.navigation.lint.common.TEST_CLASS.toTestKotlinAndBytecodeStub().kotlin
}
