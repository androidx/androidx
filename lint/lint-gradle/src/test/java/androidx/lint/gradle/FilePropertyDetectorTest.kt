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

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FilePropertyDetectorTest :
    GradleLintDetectorTest(
        detector = FilePropertyDetector(),
        issues = listOf(FilePropertyDetector.FILE_PROPERTY_ISSUE),
    ) {

    @Test
    fun `Test detect PropertyFile usage in property`() {
        val input =
            kotlin(
                """
            import org.gradle.api.provider.Property
            import java.io.File

            class Example {
                val fileProperty: Property<File>? = null
            }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/Example.kt:5: Error: Property<File> is discouraged. Use RegularFileProperty or DirectoryProperty. [FilePropertyDetector]
                val fileProperty: Property<File>? = null
                    ~~~~~~~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Test detect PropertyFile usage in method return type`() {
        val input =
            kotlin(
                """
            import org.gradle.api.provider.Property
            import java.io.File

            class Example {
                fun provideFileProperty(): Property<File>? {
                    return null
                }
            }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/Example.kt:5: Error: Property<File> is discouraged. Use RegularFileProperty or DirectoryProperty. [FilePropertyDetector]
                fun provideFileProperty(): Property<File>? {
                    ~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Test detect PropertyFile usage with nullable File`() {
        val input =
            kotlin(
                """
            import org.gradle.api.provider.Property
            import java.io.File

            class Example {
                val nullableFileProperty: Property<File?>? = null
            }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/Example.kt:5: Error: Property<File> is discouraged. Use RegularFileProperty or DirectoryProperty. [FilePropertyDetector]
                val nullableFileProperty: Property<File?>? = null
                    ~~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Test detect PropertyFile with fully qualified names`() {
        val input =
            kotlin(
                """
            class Example {
                val fileProperty: org.gradle.api.provider.Property<java.io.File>? = null
            }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/Example.kt:2: Error: Property<File> is discouraged. Use RegularFileProperty or DirectoryProperty. [FilePropertyDetector]
                val fileProperty: org.gradle.api.provider.Property<java.io.File>? = null
                    ~~~~~~~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Test detect PropertyFile in method parameters`() {
        val input =
            kotlin(
                """
            import org.gradle.api.provider.Property
            import java.io.File

            class Example {
                fun setFileProperty(fileProperty: Property<File>) {}
            }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/Example.kt:5: Error: Property<File> is discouraged. Use RegularFileProperty or DirectoryProperty. [FilePropertyDetector]
                fun setFileProperty(fileProperty: Property<File>) {}
                                    ~~~~~~~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Test no issue when not using PropertyFile`() {
        val input =
            kotlin(
                """
            import org.gradle.api.provider.Property

            class Example {
                val stringProperty: Property<String>? = null
            }
            """
                    .trimIndent()
            )

        check(input).expectClean()
    }

    @Test
    fun `Test detect subclasses of PropertyFile`() {
        val input =
            kotlin(
                """
            import org.gradle.api.provider.Property
            import java.io.File

            interface CustomFileProperty : Property<File>

            class Example {
                val customFileProperty: CustomFileProperty? = null
            }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/CustomFileProperty.kt:7: Error: Property<File> is discouraged. Use RegularFileProperty or DirectoryProperty. [FilePropertyDetector]
                val customFileProperty: CustomFileProperty? = null
                    ~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Test detect nested PropertyFile usage`() {
        val input =
            kotlin(
                """
        import org.gradle.api.provider.Property
        import java.io.File

        class Example {

            val nestedProperty: Property<Property<File>>? = null
        }
        """
                    .trimIndent()
            )

        val expected =
            """
            src/Example.kt:6: Error: Property<File> is discouraged. Use RegularFileProperty or DirectoryProperty. [FilePropertyDetector]
                val nestedProperty: Property<Property<File>>? = null
                    ~~~~~~~~~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Test detect PropertyFile usage in type parameter bound`() {
        val input =
            kotlin(
                """
            import org.gradle.api.provider.Property
            import java.io.File

            class Example {
                fun <T : Property<File>> processFile(fileProperty: T) {
                }
            }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/Example.kt:5: Error: Property<File> is discouraged. Use RegularFileProperty or DirectoryProperty. [FilePropertyDetector]
                fun <T : Property<File>> processFile(fileProperty: T) {
                                                     ~~~~~~~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()

        check(input).expect(expected)
    }
}
