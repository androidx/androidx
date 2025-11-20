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

package androidx.lint.gradle

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InternalApiUsageDetectorTest :
    GradleLintDetectorTest(
        detector = InternalApiUsageDetector(),
        issues =
            listOf(
                InternalApiUsageDetector.INTERNAL_GRADLE_ISSUE,
                InternalApiUsageDetector.INTERNAL_AGP_ISSUE,
                InternalApiUsageDetector.INTERNAL_KGP_ISSUE,
            ),
    ) {
    @Test
    fun `Test usage of internal Gradle API`() {
        val input =
            kotlin(
                """
                import org.gradle.api.component.SoftwareComponent
                import org.gradle.api.internal.component.SoftwareComponentInternal

                fun getSoftwareComponent() : SoftwareComponent {
                    return object : SoftwareComponentInternal {
                        override fun getUsages(): Set<out UsageContext> {
                            TODO()
                        }
                    }
                }
            """
                    .trimIndent()
            )

        // Adding import aliases adds new warnings and that is working as intended.
        check(input, skipTestModes = arrayOf(TestMode.IMPORT_ALIAS))
            .expect(
                """
                    src/test.kt:2: Error: Avoid using internal Gradle APIs [InternalGradleApiUsage]
                    import org.gradle.api.internal.component.SoftwareComponentInternal
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    1 errors, 0 warnings
                """
                    .trimIndent()
            )

        lint()
            .files(*STUBS, input)
            // Adding import aliases adds new warnings and that is working as intended.
            .testModes(TestMode.IMPORT_ALIAS)
            .run()
            .expect(
                """
                    src/test.kt:2: Error: Avoid using internal Gradle APIs [InternalGradleApiUsage]
                    import org.gradle.api.internal.component.SoftwareComponentInternal
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    src/test.kt:4: Error: Avoid using internal Gradle APIs [InternalGradleApiUsage]
                    import org.gradle.api.internal.component.SoftwareComponentInternal as IMPORT_ALIAS_2_SOFTWARECOMPONENTINTERNAL
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    2 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun `Test usage of internal Android Gradle API`() {
        val input =
            kotlin(
                """
                import com.android.build.gradle.internal.lint.VariantInputs
            """
                    .trimIndent()
            )

        // Import aliases mode is covered by other tests
        check(input, skipTestModes = arrayOf(TestMode.IMPORT_ALIAS))
            .expect(
                """
                src/test.kt:1: Error: Avoid using internal Android Gradle Plugin APIs [InternalAgpApiUsage]
                import com.android.build.gradle.internal.lint.VariantInputs
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun `Test usage of internal Kotlin Gradle API`() {
        val input =
            kotlin(
                """
                import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
            """
                    .trimIndent()
            )

        // Import aliases mode is covered by other tests
        check(input, skipTestModes = arrayOf(TestMode.IMPORT_ALIAS))
            .expect(
                """
                src/test.kt:1: Error: Avoid using internal Kotlin Gradle Plugin APIs [InternalKgpApiUsage]
                import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun `Test usage of Internal annotation`() {
        val input =
            kotlin(
                """
                import java.io.File
                import org.gradle.api.Task
                import org.gradle.api.tasks.Internal

                class MyTask : Task {
                    @get:Internal
                    val notInput: File
                }
            """
                    .trimIndent()
            )
        check(input).expectClean()
    }

    @Test
    fun `Test usage of internal gradle API methods`() {
        // Custom stubs that don't make sense to reuse for other tests.
        val stubs =
            arrayOf(
                kotlin(
                    """
                        package org.gradle.api
                        interface BasePublicInterface {
                            fun basePublicMethodOverriddenByInternal()
                            fun basePublicMethodOverriddenByBoth()
                        }
                    """
                ),
                kotlin(
                    """
                        package org.gradle.api.internal
                        import org.gradle.api.BasePublicInterface
                        abstract class InternalClass : BasePublicInterface {
                            open fun internalMethodOverridden() = Unit
                            open fun internalMethodNotOverridden() = Unit
                            override fun basePublicMethodOverriddenByInternal() = Unit
                            override fun basePublicMethodOverriddenByBoth() = Unit
                        }
                    """
                ),
                kotlin(
                    """
                        @file:Suppress("InternalGradleApiUsage")
                        package org.gradle.api
                        import org.gradle.api.internal.InternalClass
                        class PublicClass : InternalClass() {
                            override fun internalMethodOverridden() = Unit
                            override fun basePublicMethodOverriddenByBoth() = Unit
                        }
                    """
                ),
            )
        val input =
            kotlin(
                """
                    package test.pkg
                    import org.gradle.api.PublicClass
                    fun callMethods(publicClass: PublicClass) {
                        publicClass.basePublicMethodOverriddenByInternal()
                        publicClass.basePublicMethodOverriddenByBoth()
                        publicClass.internalMethodOverridden()
                        publicClass.internalMethodNotOverridden()
                    }
                """
            )

        check(*stubs, input)
            .expect(
                """
                src/test/pkg/test.kt:8: Error: Avoid using internal Gradle APIs (method internalMethodNotOverridden from org.gradle.api.internal.InternalClass) [InternalGradleApiUsage]
                                        publicClass.internalMethodNotOverridden()
                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun `Test usage of internal gradle API properties`() {
        // Custom stubs that don't make sense to reuse for other tests.
        val stubs =
            arrayOf(
                kotlin(
                    """
                        package org.gradle.api
                        interface BasePublicInterface {
                            val basePublicPropertyOverriddenByInternal: Boolean
                            val basePublicPropertyOverriddenByBoth: Boolean
                        }
                    """
                ),
                kotlin(
                    """
                        package org.gradle.api.internal
                        import org.gradle.api.BasePublicInterface
                        abstract class InternalClass : BasePublicInterface {
                            open val internalPropertyOverridden = false
                            open val internalPropertyNotOverridden = false
                            override val basePublicPropertyOverriddenByInternal = false
                            override val basePublicPropertyOverriddenByBoth = false
                        }
                    """
                ),
                kotlin(
                    """
                        @file:Suppress("InternalGradleApiUsage")
                        package org.gradle.api
                        import org.gradle.api.internal.InternalClass
                        class PublicClass : InternalClass() {
                            override val internalPropertyOverridden = false
                            override val basePublicPropertyOverriddenByBoth = false
                        }
                    """
                ),
            )
        val input =
            kotlin(
                """
                    package test.pkg
                    import org.gradle.api.PublicClass
                    fun getProperties(publicClass: PublicClass) {
                        publicClass.basePublicPropertyOverriddenByInternal
                        publicClass.basePublicPropertyOverriddenByBoth
                        publicClass.internalPropertyOverridden
                        publicClass.internalPropertyNotOverridden
                    }
                """
            )

        check(*stubs, input)
            .expect(
                """
                src/test/pkg/test.kt:8: Error: Avoid using internal Gradle APIs (method getInternalPropertyNotOverridden from org.gradle.api.internal.InternalClass) [InternalGradleApiUsage]
                                        publicClass.internalPropertyNotOverridden
                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun `Test usage of internal gradle API fields`() {
        // Custom stubs that don't make sense to reuse for other tests.
        val stubs =
            arrayOf(
                java(
                    """
                        package org.gradle.api;
                        public interface BasePublicInterface {
                            boolean basePublicField = false;
                        }
                    """
                ),
                java(
                    """
                        package org.gradle.api.internal;
                        import org.gradle.api.BasePublicInterface;
                        public abstract class InternalClass implements BasePublicInterface {
                            boolean internalField = false;
                        }
                    """
                ),
                java(
                    """
                        package org.gradle.api;
                        public class PublicClass extends org.gradle.api.internal.InternalClass {
                            boolean publicField = false;
                        }
                    """
                ),
            )
        val input =
            kotlin(
                """
                    package test.pkg
                    import org.gradle.api.PublicClass
                    fun getFields(publicClass: PublicClass) {
                        publicClass.basePublicField
                        publicClass.internalField
                        publicClass.publicField
                    }
                """
            )

        check(*stubs, input)
            .expect(
                """
                src/org/gradle/api/PublicClass.java:3: Error: Avoid using internal Gradle APIs [InternalGradleApiUsage]
                                        public class PublicClass extends org.gradle.api.internal.InternalClass {
                                                                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/test.kt:6: Error: Avoid using internal Gradle APIs (field internalField from org.gradle.api.internal.InternalClass) [InternalGradleApiUsage]
                                        publicClass.internalField
                                        ~~~~~~~~~~~~~~~~~~~~~~~~~
                2 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun `Check qualified usage of internal class as supertype`() {
        val qualifiedInput =
            kotlin(
                """
                    import org.gradle.api.component.SoftwareComponent
                    fun getSoftwareComponent() : SoftwareComponent {
                        return object : org.gradle.api.internal.component.SoftwareComponentInternal {
                            override fun getUsages(): Set<out UsageContext> {
                                TODO()
                            }
                        }
                    }
                """
                    .trimIndent()
            )
        val importInput =
            kotlin(
                """
                    import org.gradle.api.component.SoftwareComponent
                    import org.gradle.api.internal.component.SoftwareComponentInternal
                    fun getSoftwareComponent() : SoftwareComponent {
                        return object : SoftwareComponentInternal {
                            override fun getUsages(): Set<out UsageContext> {
                                TODO()
                            }
                        }
                    }
                """
                    .trimIndent()
            )

        // b/406739378: TestMode.SUPPRESSIBLE doesn't know how to handle the object return
        // Import aliases mode is covered by other tests
        check(
                qualifiedInput,
                importInput,
                skipTestModes = arrayOf(TestMode.SUPPRESSIBLE, TestMode.IMPORT_ALIAS),
            )
            .expect(
                """
                src/test.kt:3: Error: Avoid using internal Gradle APIs [InternalGradleApiUsage]
                    return object : org.gradle.api.internal.component.SoftwareComponentInternal {
                                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test2.kt:2: Error: Avoid using internal Gradle APIs [InternalGradleApiUsage]
                import org.gradle.api.internal.component.SoftwareComponentInternal
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                2 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun `Check qualified usage of internal class literal`() {
        val qualifiedInput =
            kotlin(
                """
                    fun classReference() {
                        org.gradle.api.internal.component.SoftwareComponentInternal::class.java
                    }
                """
                    .trimIndent()
            )
        val importInput =
            kotlin(
                """
                    import org.gradle.api.internal.component.SoftwareComponentInternal
                    fun classReference() {
                        SoftwareComponentInternal::class.java
                    }
                """
                    .trimIndent()
            )

        // Import aliases mode is covered by other tests
        check(qualifiedInput, importInput, skipTestModes = arrayOf(TestMode.IMPORT_ALIAS))
            .expect(
                """
                src/test.kt:2: Error: Avoid using internal Gradle APIs [InternalGradleApiUsage]
                    org.gradle.api.internal.component.SoftwareComponentInternal::class.java
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test2.kt:1: Error: Avoid using internal Gradle APIs [InternalGradleApiUsage]
                import org.gradle.api.internal.component.SoftwareComponentInternal
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                2 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun `Check qualified usage of internal class in cast`() {
        val qualifiedInput =
            kotlin(
                """
                    import org.gradle.api.component.SoftwareComponent
                    fun castSoftwareComponent(sc: SoftwareComponent) {
                        sc as org.gradle.api.internal.component.SoftwareComponentInternal
                    }
                """
                    .trimIndent()
            )
        val importedInput =
            kotlin(
                """
                    import org.gradle.api.component.SoftwareComponent
                    import org.gradle.api.internal.component.SoftwareComponentInternal
                    fun castSoftwareComponent(sc: SoftwareComponent) {
                        sc as SoftwareComponentInternal
                    }
                """
                    .trimIndent()
            )

        // Import aliases mode is covered by other tests
        check(qualifiedInput, importedInput, skipTestModes = arrayOf(TestMode.IMPORT_ALIAS))
            .expect(
                """
                src/test.kt:3: Error: Avoid using internal Gradle APIs [InternalGradleApiUsage]
                    sc as org.gradle.api.internal.component.SoftwareComponentInternal
                          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test2.kt:2: Error: Avoid using internal Gradle APIs [InternalGradleApiUsage]
                import org.gradle.api.internal.component.SoftwareComponentInternal
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                2 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun `Check qualified usage of internal class as caught exception`() {
        val qualifiedInput =
            kotlin(
                """
                    import org.gradle.testkit.runner.GradleRunner
                    fun catchException() {
                        try {
                            GradleRunner.create()
                        } catch(e: org.gradle.process.internal.ExecException) {
                            TODO()
                        }
                    }
                """
                    .trimIndent()
            )
        val importInput =
            kotlin(
                """
                    import org.gradle.testkit.runner.GradleRunner
                    import org.gradle.process.internal.ExecException
                    fun catchException() {
                        try {
                            GradleRunner.create()
                        } catch(e: ExecException) {
                            TODO()
                        }
                    }
                """
                    .trimIndent()
            )

        // Import aliases mode is covered by other tests
        check(qualifiedInput, importInput, skipTestModes = arrayOf(TestMode.IMPORT_ALIAS))
            .expect(
                """
                src/test.kt:5: Error: Avoid using internal Gradle APIs [InternalGradleApiUsage]
                    } catch(e: org.gradle.process.internal.ExecException) {
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test2.kt:2: Error: Avoid using internal Gradle APIs [InternalGradleApiUsage]
                import org.gradle.process.internal.ExecException
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                2 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun `Check qualified usage of internal class as annotation`() {
        val qualifiedInput =
            kotlin(
                """
                    @com.android.build.gradle.internal.tasks.BuildAnalyzer
                    class AnnotatedQualified
                """
                    .trimIndent()
            )
        val importInput =
            kotlin(
                """
                    import com.android.build.gradle.internal.tasks.BuildAnalyzer
                    @BuildAnalyzer
                    class AnnotatedImport
                """
                    .trimIndent()
            )

        // Import aliases mode is covered by other tests
        check(qualifiedInput, importInput, skipTestModes = arrayOf(TestMode.IMPORT_ALIAS))
            .expect(
                """
                src/AnnotatedImport.kt:1: Error: Avoid using internal Android Gradle Plugin APIs [InternalAgpApiUsage]
                import com.android.build.gradle.internal.tasks.BuildAnalyzer
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/AnnotatedQualified.kt:1: Error: Avoid using internal Android Gradle Plugin APIs [InternalAgpApiUsage]
                @com.android.build.gradle.internal.tasks.BuildAnalyzer
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                2 errors, 0 warnings
                """
                    .trimIndent()
            )
    }
}
