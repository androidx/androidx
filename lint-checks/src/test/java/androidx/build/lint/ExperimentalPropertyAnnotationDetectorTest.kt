/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build.lint

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ExperimentalPropertyAnnotationDetectorTest :
    AbstractLintDetectorTest(
        useDetector = ExperimentalPropertyAnnotationDetector(),
        useIssues = listOf(ExperimentalPropertyAnnotationDetector.ISSUE),
        stubs =
            arrayOf(
                kotlin(
                    """
        package java.androidx

        @RequiresOptIn(level = RequiresOptIn.Level.ERROR)
        @Retention(AnnotationRetention.BINARY)
        annotation class ExperimentalKotlinAnnotation
    """
                )
            ),
    ) {
    @Test
    fun `Test var properties annotated with all targets`() {
        val input =
            arrayOf(
                kotlin(
                        """
                        package java.androidx

                        class AnnotatedProperty {
                            @get:ExperimentalKotlinAnnotation
                            @set:ExperimentalKotlinAnnotation
                            @ExperimentalKotlinAnnotation
                            var annotatedWithDefault: Int = 3

                            @get:ExperimentalKotlinAnnotation
                            @set:ExperimentalKotlinAnnotation
                            @property:ExperimentalKotlinAnnotation
                            var annotatedWithProperty: Int = 3
                        }
                    """
                    )
                    .indented()
            )

        // Getters are annotated, so no errors
        check(*input).expectClean()
    }

    @Test
    fun `Test var property annotated with two targets`() {
        val input =
            arrayOf(
                kotlin(
                        """
                        package java.androidx

                        class AnnotatedProperty {
                            @get:ExperimentalKotlinAnnotation
                            @ExperimentalKotlinAnnotation
                            var annotatedWithGetAndDefault = 3

                            @ExperimentalKotlinAnnotation
                            @set:ExperimentalKotlinAnnotation
                            var annotatedWithSetAndDefault = 3

                            @get:ExperimentalKotlinAnnotation
                            @set:ExperimentalKotlinAnnotation
                            var annotatedWithGetAndSet = 3

                            @property:ExperimentalKotlinAnnotation
                            @get:ExperimentalKotlinAnnotation
                            var annotatedWithGetAndProperty = 3

                            @set:ExperimentalKotlinAnnotation
                            @property:ExperimentalKotlinAnnotation
                            var annotatedWithSetAndProperty = 3
                        }
                    """
                    )
                    .indented()
            )

        // errors for properties annotated on the property and set targets, but not get
        val expected =
            """
            src/java/androidx/AnnotatedProperty.kt:8: Error: Experimental property will not appear experimental in Java [ExperimentalPropertyAnnotation]
                @ExperimentalKotlinAnnotation
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            src/java/androidx/AnnotatedProperty.kt:21: Error: Experimental property will not appear experimental in Java [ExperimentalPropertyAnnotation]
                @property:ExperimentalKotlinAnnotation
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            2 errors
            """

        check(*input).expect(expected)
    }

    @Test
    fun `Test var properties annotated with one target`() {
        val input =
            arrayOf(
                kotlin(
                        """
                        package java.androidx

                        class AnnotatedProperty {
                            // Technically an experimental annotation on a getter is not allowed
                            @get:ExperimentalKotlinAnnotation
                            var annotatedWithGet = 3

                            @set:ExperimentalKotlinAnnotation
                            var annotatedWithSet = 3

                            @property:ExperimentalKotlinAnnotation
                            var annotatedWithProperty = 3

                            @ExperimentalKotlinAnnotation
                            var annotatedWithDefault = 3
                        }
                    """
                    )
                    .indented()
            )

        val expected =
            """
            src/java/androidx/AnnotatedProperty.kt:11: Error: Experimental property will not appear experimental in Java [ExperimentalPropertyAnnotation]
                @property:ExperimentalKotlinAnnotation
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            src/java/androidx/AnnotatedProperty.kt:14: Error: Experimental property will not appear experimental in Java [ExperimentalPropertyAnnotation]
                @ExperimentalKotlinAnnotation
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            2 errors
            """

        check(*input).expect(expected)
    }

    @Test
    fun `Test val properties annotated with one target`() {
        val input =
            arrayOf(
                kotlin(
                        """
                        package java.androidx

                        class AnnotatedProperty {
                            // Technically an experimental annotation on a getter is not allowed
                            @get:ExperimentalKotlinAnnotation
                            val annotatedWithGet = 3

                            @property:ExperimentalKotlinAnnotation
                            val annotatedWithProperty = 3

                            @ExperimentalKotlinAnnotation
                            val annotatedWithDefault = 3
                        }
                    """
                    )
                    .indented()
            )

        val expected =
            """
            src/java/androidx/AnnotatedProperty.kt:8: Error: Experimental property will not appear experimental in Java [ExperimentalPropertyAnnotation]
                @property:ExperimentalKotlinAnnotation
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            src/java/androidx/AnnotatedProperty.kt:11: Error: Experimental property will not appear experimental in Java [ExperimentalPropertyAnnotation]
                @ExperimentalKotlinAnnotation
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            2 errors
            """

        check(*input).expect(expected)
    }

    @Test
    fun `Test property annotated with non-experimental annotation`() {
        val input =
            arrayOf(
                kotlin(
                    """
                package java.androidx

                class AnnotatedProperty {
                    @NonExperimentalAnnotation
                    var correctlyAnnotated: Int = 3
                }
            """
                ),
                kotlin(
                    """
                package java.androidx

                annotation class NonExperimentalAnnotation
            """
                ),
            )

        check(*input).expectClean()
    }

    @Test
    fun `Test property using Java defined annotation`() {
        val input =
            arrayOf(
                kotlin(
                        """
                        package java.androidx

                        import java.androidx.ExperimentalJavaAnnotation

                        class AnnotatedProperty {
                            @ExperimentalJavaAnnotation
                            var experimentalProperty = 3
                        }
                    """
                    )
                    .indented(),
                java(
                        """
                        package java.androidx;

                        import static androidx.annotation.RequiresOptIn.Level.ERROR;

                        import androidx.annotation.RequiresOptIn;

                        @RequiresOptIn(level = ERROR)
                        public @interface ExperimentalJavaAnnotation {}
                    """
                    )
                    .indented(),
                kotlin(
                    """
                        package androidx.annotation

                        import kotlin.annotation.Retention
                        import kotlin.annotation.Target

                        @Retention(AnnotationRetention.BINARY)
                        @Target(AnnotationTarget.ANNOTATION_CLASS)
                        annotation class RequiresOptIn(
                            val level: Level = Level.ERROR
                        ) {
                            enum class Level {
                                WARNING,
                                ERROR
                            }
                        }
                    """
                ),
            )

        val expected =
            """
            src/java/androidx/AnnotatedProperty.kt:6: Error: Experimental property will not appear experimental in Java [ExperimentalPropertyAnnotation]
                @ExperimentalJavaAnnotation
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~
            1 error
            """

        check(*input).expect(expected)
    }

    @Test
    fun `Test property defined at top-level`() {
        val input =
            arrayOf(
                kotlin(
                        """
                        package java.androidx

                        @ExperimentalKotlinAnnotation
                        var topLevelExperimentalProperty = 3
                    """
                    )
                    .indented()
            )

        val expected =
            """
            src/java/androidx/test.kt:3: Error: Experimental property will not appear experimental in Java [ExperimentalPropertyAnnotation]
            @ExperimentalKotlinAnnotation
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            1 error
            """

        check(*input).expect(expected)
    }

    @Test
    fun `Test property defined in companion object`() {
        val input =
            arrayOf(
                kotlin(
                        """
                        package java.androidx

                        class AnnotatedProperty {
                            companion object {
                                @ExperimentalKotlinAnnotation
                                var propertyInCompanion = 3
                            }
                        }
                    """
                    )
                    .indented()
            )

        val expected =
            """
            src/java/androidx/AnnotatedProperty.kt:5: Error: Experimental property will not appear experimental in Java [ExperimentalPropertyAnnotation]
                    @ExperimentalKotlinAnnotation
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            1 error
            """

        check(*input).expect(expected)
    }

    @Test
    fun `Test property defined in interface`() {
        val input =
            arrayOf(
                kotlin(
                        """
                        package java.androidx

                        interface AnnotatedProperty {
                            @ExperimentalKotlinAnnotation
                            val propertyInInterface: Int = 0
                        }
                    """
                    )
                    .indented()
            )

        val expected =
            """
            src/java/androidx/AnnotatedProperty.kt:4: Error: Experimental property will not appear experimental in Java [ExperimentalPropertyAnnotation]
                @ExperimentalKotlinAnnotation
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            1 error
            """

        check(*input).expect(expected)
    }

    @Test
    fun `Test experimental annotations on non-properties don't trigger lint`() {
        val input =
            arrayOf(
                kotlin(
                    """
                package java.androidx

                @file:ExperimentalKotlinAnnotation

                @ExperimentalKotlinAnnotation
                class ExperimentalClass {
                    @ExperimentalKotlinAnnotation
                    fun experimentalFunction() {}
                }
            """
                )
            )

        check(*input).expectClean()
    }

    @Test
    fun `Test property annotated with JvmField doesn't trigger lint`() {
        val input =
            arrayOf(
                kotlin(
                    """
                package java.androidx

                class AnnotatedWithJvmField {
                    @JvmField
                    @ExperimentalKotlinAnnotation
                    var experimentalProperty = 3
                }
            """
                )
            )

        check(*input).expectClean()
    }

    @Test
    fun `Test const property doesn't trigger lint`() {
        val input =
            arrayOf(
                kotlin(
                    """
                package java.androidx

                @ExperimentalKotlinAnnotation
                const val EXPERIMENTAL_CONST = 3
            """
                )
            )

        check(*input).expectClean()
    }

    @Test
    fun `Test property with delegate doesn't trigger lint`() {
        val input =
            arrayOf(
                kotlin(
                    """
                package java.androidx

                @ExperimentalKotlinAnnotation
                var experimentalProperty by mutableStateOf(0L)
                """
                )
            )

        check(*input).expectClean()
    }

    @Test
    fun `Test property within function doesn't trigger lint`() {
        val input =
            arrayOf(
                kotlin(
                    """
                package java.androidx

                fun functionWithProperty() {
                    @ExperimentalKotlinAnnotation
                    val experimentalProperty = 3
                }
                """
                        .trimIndent()
                )
            )

        check(*input).expectClean()
    }

    @Test
    fun `Test private property doesn't trigger lint but other non-public properties do`() {
        val input =
            arrayOf(
                kotlin(
                        """
                    package java.androidx

                    class AnnotatedProperty {
                        @ExperimentalKotlinAnnotation
                        private var privateProperty = 3

                        @ExperimentalKotlinAnnotation
                        protected var protectedProperty = 3

                        @ExperimentalKotlinAnnotation
                        internal var internalProperty = 3
                    }
                    """
                    )
                    .indented()
            )

        val expected =
            """
            src/java/androidx/AnnotatedProperty.kt:7: Error: Experimental property will not appear experimental in Java [ExperimentalPropertyAnnotation]
                @ExperimentalKotlinAnnotation
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            src/java/androidx/AnnotatedProperty.kt:10: Error: Experimental property will not appear experimental in Java [ExperimentalPropertyAnnotation]
                @ExperimentalKotlinAnnotation
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            2 errors
            """

        check(*input).expect(expected)
    }

    @Test
    fun `Test property in private class doesn't trigger lint`() {
        val input =
            arrayOf(
                kotlin(
                    """
                package java.androidx

                private class AnnotatedProperty {
                    @ExperimentalKotlinAnnotation
                    var experimentalProperty = 3
                }
                """
                        .trimIndent()
                )
            )

        check(*input).expectClean()
    }

    @Test
    fun `Test property with private setter`() {
        val input =
            arrayOf(
                kotlin(
                    """
                    package java.androidx

                    @ExperimentalKotlinAnnotation
                    var experimentalProperty = 3
                        private set
                    """
                )
            )

        val expected =
            """
            src/java/androidx/test.kt:4: Error: Experimental property will not appear experimental in Java [ExperimentalPropertyAnnotation]
                                @ExperimentalKotlinAnnotation
                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            1 error
            """

        check(*input).expect(expected)
    }

    @Test
    fun `Test constructor var property-parameter partially annotated`() {
        val input =
            arrayOf(
                kotlin(
                        """
                    package java.androidx
                    class Foo(
                        // Technically an experimental annotation on a getter is not allowed
                        @get:ExperimentalKotlinAnnotation
                        var annotatedWithGet: Int,

                        @set:ExperimentalKotlinAnnotation
                        var annotatedWithSet: Int,

                        @property:ExperimentalKotlinAnnotation
                        var annotatedWithProperty: Int,

                        // Technically parameters should never be annotated with experimental
                        // annotations on the param target
                        @param:ExperimentalKotlinAnnotation
                        var annotatedWithParam: Int,

                        @ExperimentalKotlinAnnotation
                        var annotatedWithDefault: Int,
                    )
                    """
                    )
                    .indented()
            )

        val expected =
            """
            src/java/androidx/Foo.kt:10: Error: Experimental property will not appear experimental in Java [ExperimentalPropertyAnnotation]
                @property:ExperimentalKotlinAnnotation
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            src/java/androidx/Foo.kt:18: Error: Experimental property will not appear experimental in Java [ExperimentalPropertyAnnotation]
                @ExperimentalKotlinAnnotation
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            2 errors
            """

        check(*input).expect(expected)
    }

    @Test
    fun `Test constructor val property-parameter`() {
        val input =
            arrayOf(
                kotlin(
                        """
                    package java.androidx
                    class Foo(
                        @property:ExperimentalKotlinAnnotation
                        val experimentalProperty: Int
                    )
                    """
                    )
                    .indented()
            )

        val expected =
            """
            src/java/androidx/Foo.kt:3: Error: Experimental property will not appear experimental in Java [ExperimentalPropertyAnnotation]
                @property:ExperimentalKotlinAnnotation
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            1 error
            """

        check(*input).expect(expected)
    }

    @Test
    fun `Test constructor property-parameter private`() {
        val input =
            arrayOf(
                kotlin(
                        """
                    package java.androidx
                    class Foo(
                        @property:ExperimentalKotlinAnnotation
                        private val experimentalProperty: Int
                    )
                    """
                    )
                    .indented()
            )

        check(*input).expectClean()
    }

    @Test
    fun `Test private constructor property-parameter`() {
        val input =
            arrayOf(
                kotlin(
                        """
                    package java.androidx
                    class Foo private constructor (
                        @property:ExperimentalKotlinAnnotation
                        val experimentalProperty: Int
                    )
                    """
                    )
                    .indented()
            )

        check(*input).expectClean()
    }

    @Test
    fun `Test constructor property-parameter of private class`() {
        val input =
            arrayOf(
                kotlin(
                        """
                    package java.androidx
                    class Foo private constructor (
                        @property:ExperimentalKotlinAnnotation
                        val experimentalProperty: Int
                    )
                    """
                    )
                    .indented()
            )

        check(*input).expectClean()
    }

    @Test
    fun `Test constructor parameter, non property`() {
        val input =
            arrayOf(
                kotlin(
                        """
                    package java.androidx
                    class Foo(
                        @ExperimentalKotlinAnnotation
                        experimentalParameter: Int
                    )
                    """
                    )
                    .indented()
            )

        check(*input).expectClean()
    }

    @Test
    fun `Test parameter of non-constructor`() {
        val input =
            arrayOf(
                kotlin(
                        """
                    package java.androidx
                    class Foo {
                        fun foo(
                            @ExperimentalKotlinAnnotation
                            experimentalParameter: Int
                        ) = Unit
                    }
                    """
                    )
                    .indented()
            )

        check(*input).expectClean()
    }

    @Test
    fun `Test property with custom accessors`() {
        val input =
            kotlin(
                    """
                    package java.androidx
                    class Foo {
                        @ExperimentalKotlinAnnotation
                        var annotatedOnDefault: Int
                            get() = 0
                            set(prop) {}

                        @property:ExperimentalKotlinAnnotation
                        @set:ExperimentalKotlinAnnotation
                        var annotatedOnPropertyAndSet: Int
                            get() = 0
                            set(prop) {}

                        @property:ExperimentalKotlinAnnotation
                        @get:ExperimentalKotlinAnnotation
                        var annotatedOnPropertyAndGet: Int
                            get() = 0
                            set(prop) {}

                        @property:ExperimentalKotlinAnnotation
                        var annotatedOnPropertyAndGetter: Int
                            @ExperimentalKotlinAnnotation
                            get() = 0
                            set(prop) {}
                    }
                """
                )
                .indented()

        // Errors on properties without annotated getters
        val expected =
            """
            src/java/androidx/Foo.kt:3: Error: Experimental property will not appear experimental in Java [ExperimentalPropertyAnnotation]
                @ExperimentalKotlinAnnotation
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            src/java/androidx/Foo.kt:8: Error: Experimental property will not appear experimental in Java [ExperimentalPropertyAnnotation]
                @property:ExperimentalKotlinAnnotation
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            2 errors
            """

        check(input).expect(expected)
    }

    @Test
    fun `Test extension properties`() {
        val input =
            kotlin(
                    """
                    package java.androidx
                    @ExperimentalKotlinAnnotation
                    var String.annotatedOnDefault: Int
                        get() = 0
                        set(prop) {}

                    @property:ExperimentalKotlinAnnotation
                    @set:ExperimentalKotlinAnnotation
                    var String.annotatedOnPropertyAndSet: Int
                        get() = 0
                        set(prop) {}

                    @property:ExperimentalKotlinAnnotation
                    @get:ExperimentalKotlinAnnotation
                    var String.annotatedOnPropertyAndGet: Int
                        get() = 0
                        set(prop) {}

                    @property:ExperimentalKotlinAnnotation
                    var String.annotatedOnPropertyAndGetter: Int
                        @ExperimentalKotlinAnnotation
                        get() = 0
                        set(prop) {}
                """
                )
                .indented()

        // Errors on properties without annotated getters
        val expected =
            """
            src/java/androidx/test.kt:2: Error: Experimental property will not appear experimental in Java [ExperimentalPropertyAnnotation]
            @ExperimentalKotlinAnnotation
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            src/java/androidx/test.kt:7: Error: Experimental property will not appear experimental in Java [ExperimentalPropertyAnnotation]
            @property:ExperimentalKotlinAnnotation
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            2 errors
            """

        check(input).expect(expected)
    }
}
