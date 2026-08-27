/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.build

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProjectParserTest {
    @Test
    fun parsePublishedLibrary() {
        val parsed =
            ProjectParser.parseProject(
                """
                plugins {
                    id("AndroidXPlugin")
                }
                androidx {
                    name = "Activity"
                    type = SoftwareType.PUBLISHED_LIBRARY
                    mavenVersion = LibraryVersions.ACTIVITY
                    inceptionYear = "2018"
                }
                """
                    .trimIndent()
            )
        assertThat(parsed.softwareType).isEqualTo(SoftwareType.PUBLISHED_LIBRARY)
        assertThat(parsed.specifiesVersion).isTrue()
        assertThat(parsed.shouldPublish()).isTrue()
        assertThat(parsed.shouldRelease()).isTrue()
    }

    @Test
    fun parseSamplesLibraryWithoutVersion() {
        val parsed =
            ProjectParser.parseProject(
                """
                androidx {
                    name = "Samples"
                    type = SoftwareType.SAMPLES
                }
                """
                    .trimIndent()
            )
        assertThat(parsed.softwareType).isEqualTo(SoftwareType.SAMPLES)
        assertThat(parsed.specifiesVersion).isFalse()
        assertThat(parsed.shouldPublish()).isTrue()
        assertThat(parsed.shouldRelease()).isTrue()
    }

    @Test
    fun parseIgnoresComments() {
        val parsed =
            ProjectParser.parseProject(
                """
                androidx {
                    // type = SoftwareType.SAMPLES
                    /* type = SoftwareType.BENCHMARK */
                    type = SoftwareType.PUBLISHED_LIBRARY
                    // mavenVersion = LibraryVersions.COMPOSE
                }
                """
                    .trimIndent()
            )
        assertThat(parsed.softwareType).isEqualTo(SoftwareType.PUBLISHED_LIBRARY)
        assertThat(parsed.specifiesVersion).isFalse()
    }

    @Test
    fun parseNoAndroidxBlock() {
        val parsed =
            ProjectParser.parseProject(
                """
                plugins {
                    id("kotlin")
                }
                dependencies {
                    implementation("org.jetbrains.kotlin:kotlin-stdlib")
                }
                """
                    .trimIndent()
            )
        assertThat(parsed.softwareType).isEqualTo(SoftwareType.UNSET)
        assertThat(parsed.specifiesVersion).isFalse()
        assertThat(parsed.shouldPublish()).isFalse()
        assertThat(parsed.shouldRelease()).isFalse()
    }

    @Test
    fun parseFormattingVariations() {
        val parsed =
            ProjectParser.parseProject(
                """
                androidx {
                    type=SoftwareType.INTERNAL_TEST_LIBRARY
                    mavenVersion =
                        LibraryVersions.ROOM
                }
                """
                    .trimIndent()
            )
        assertThat(parsed.softwareType).isEqualTo(SoftwareType.INTERNAL_TEST_LIBRARY)
        assertThat(parsed.specifiesVersion).isTrue()
    }

    @Test
    fun parseMultiplatformModule() {
        val parsed =
            ProjectParser.parseProject(
                """
                plugins {
                    id("AndroidXPlugin")
                    id("AndroidXComposePlugin")
                }
                androidXMultiplatform {
                    androidLibrary {
                        namespace = "androidx.compose.ui.graphics"
                    }
                    sourceSets {
                        commonMain.dependencies {
                            implementation("androidx.collection:collection:1.4.2")
                        }
                    }
                }
                androidx {
                    name = "Compose Graphics"
                    type = SoftwareType.PUBLISHED_LIBRARY_ONLY_USED_BY_KOTLIN_CONSUMERS
                    mavenVersion = LibraryVersions.COMPOSE
                }
                """
                    .trimIndent()
            )
        assertThat(parsed.softwareType)
            .isEqualTo(SoftwareType.PUBLISHED_LIBRARY_ONLY_USED_BY_KOTLIN_CONSUMERS)
        assertThat(parsed.specifiesVersion).isTrue()
        assertThat(parsed.shouldPublish()).isTrue()
        assertThat(parsed.shouldRelease()).isTrue()
    }

    @Test
    fun parseChainedAssignment() {
        val parsed =
            ProjectParser.parseProject(
                """
                androidx {
                    type = type = SoftwareType.INTERNAL_TEST_LIBRARY
                }
                """
                    .trimIndent()
            )
        assertThat(parsed.softwareType).isEqualTo(SoftwareType.INTERNAL_TEST_LIBRARY)
        assertThat(parsed.specifiesVersion).isFalse()
    }

    @Test
    fun parseDetectsSingleQuotesInDependencies() {
        val parsed =
            ProjectParser.parseProject(
                """
                dependencies {
                    implementation('androidx.annotation:annotation:1.8.0')
                    api(project(':core:core'))
                }
                """
                    .trimIndent()
            )
        assertThat(parsed.singleQuoteViolations)
            .containsExactly("line 2:20", "line 2:57", "line 3:17", "line 3:28")
    }

    @Test
    fun parseDetectsSingleQuotesInPluginsAndProperties() {
        val parsed =
            ProjectParser.parseProject(
                """
                plugins {
                    id('com.android.library')
                }
                androidx {
                    name = 'My Library'
                    inceptionYear = '2024'
                }
                """
                    .trimIndent()
            )
        assertThat(parsed.singleQuoteViolations).hasSize(6)
    }

    @Test
    fun parseIgnoresSingleQuotesInComments() {
        val parsed =
            ProjectParser.parseProject(
                """
                // Don't remove this dependency, it's required
                /* Here's a multi-line comment with 'single quotes' */
                dependencies {
                    implementation("androidx.annotation:annotation:1.8.0")
                }
                """
                    .trimIndent()
            )
        assertThat(parsed.singleQuoteViolations).isEmpty()
    }

    @Test
    fun parseDoubleQuotesHasNoViolations() {
        val parsed =
            ProjectParser.parseProject(
                """
                plugins {
                    id("AndroidXPlugin")
                    id("com.android.library")
                }
                dependencies {
                    implementation("androidx.annotation:annotation:1.8.0")
                    api(project(":core:core"))
                }
                androidx {
                    name = "Activity"
                    type = SoftwareType.PUBLISHED_LIBRARY
                    mavenVersion = LibraryVersions.ACTIVITY
                    inceptionYear = "2018"
                }
                """
                    .trimIndent()
            )
        assertThat(parsed.singleQuoteViolations).isEmpty()
    }

    @Test
    fun parseDoubleQuotesWithNestedSingleQuotesHasNoViolations() {
        val parsed =
            ProjectParser.parseProject(
                """
                androidx {
                    name = "Lifecycle ViewModel Testing"
                    type = SoftwareType.PUBLISHED_TEST_LIBRARY
                    inceptionYear = "2024"
                    description = "Testing utilities for 'lifecycle-viewmodel' artifact"
                }
                """
                    .trimIndent()
            )
        assertThat(parsed.singleQuoteViolations).isEmpty()
    }

    @Test
    fun parseDoubleQuotesWithEscapedQuotesHasNoViolations() {
        val parsed =
            ProjectParser.parseProject(
                """
                androidx {
                    description = "Here's an escaped quote: \"nested 'quote'\" in string"
                }
                """
                    .trimIndent()
            )
        assertThat(parsed.singleQuoteViolations).isEmpty()
    }

    @Test
    fun parseDetectsSingleQuotesInMultilineStrings() {
        val parsed =
            ProjectParser.parseProject(
                """
                androidx {
                    description = '''Testing utilities for 'lifecycle-viewmodel' artifact'''
                }
                """
                    .trimIndent()
            )
        assertThat(parsed.singleQuoteViolations).isNotEmpty()
    }
}
