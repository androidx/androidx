/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.testutils.gradle.ProjectSetupRule
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProjectCreatorTaskTest {
    @get:Rule val projectSetup = ProjectSetupRule()
    @get:Rule val tempFolder = TemporaryFolder()

    @Test
    fun testIsGroupIdAtomic() {
        val file = tempFolder.newFile()
        file.writeText(
            """
            [versions]
            BAR = "1.0.0-alpha01"
            FOO = "1.0.0-alpha01"

            [groups]
            BAR = { group = "androidx.bar" }
            FOO = { group = "androidx.foo", atomicGroupVersion = "versions.FOO" }
        """
                .trimIndent()
        )

        val projectSpecBar =
            ProjectSpec(
                "androidx.bar",
                "bar-foo",
                ProjectType.ANDROID_LIBRARY,
                "",
                projectSetup.rootDir,
            )
        assertFalse(VersionCatalogEditor(file, projectSpecBar).isGroupIdAtomic())

        val projectSpecFoo =
            ProjectSpec(
                "androidx.foo",
                "foo-abc",
                ProjectType.ANDROID_LIBRARY,
                "",
                projectSetup.rootDir,
            )
        assertTrue(VersionCatalogEditor(file, projectSpecFoo).isGroupIdAtomic())
    }

    @Test
    fun testUpdateLibraryVersionsToml() {
        val file = tempFolder.newFile()
        file.writeText(
            """
            [versions]
            FOO = "1.0.0-alpha01"

            [groups]
            FOO = { group = "androidx.foo", atomicGroupVersion = "versions.FOO" }
        """
                .trimIndent()
        )

        val projectSpec =
            ProjectSpec(
                "androidx.bar",
                "bar-foo",
                ProjectType.ANDROID_LIBRARY,
                "",
                projectSetup.rootDir,
            )
        val catalogEditor = VersionCatalogEditor(file, projectSpec)
        catalogEditor.updateLibraryVersionsToml()
        assertThat(file.readText())
            .isEqualTo(
                """
            [versions]
            BAR = "1.0.0-alpha01"
            FOO = "1.0.0-alpha01"

            [groups]
            BAR = { group = "androidx.bar", atomicGroupVersion = "versions.BAR" }
            FOO = { group = "androidx.foo", atomicGroupVersion = "versions.FOO" }

        """
                    .trimIndent()
            )
    }

    @Test
    fun testUpdateSettingsGradleFileAndroidProject() {
        val file = tempFolder.newFile()
        file.writeText(
            """
                // Stuff before includeProject section
                class MyClass {
                }
                // End stuff before includeProject section

                includeProject(":activity:activity", [BuildType.MAIN, BuildType.FLAN, BuildType.COMPOSE])
            """
                .trimIndent()
        )
        val gradleSettingsEditor = GradleSettingsEditor(file)
        val projectSpec =
            ProjectSpec(
                "androidx.foo",
                "foo-bar",
                ProjectType.ANDROID_LIBRARY,
                "",
                projectSetup.rootDir,
            )
        gradleSettingsEditor.updateSettingsGradle(projectSpec)
        assertThat(file.readText())
            .isEqualTo(
                """
            // Stuff before includeProject section
            class MyClass {
            }
            // End stuff before includeProject section

            includeProject(":activity:activity", [BuildType.MAIN, BuildType.FLAN, BuildType.COMPOSE])
            includeProject(":foo:foo-bar", [BuildType.MAIN])

        """
                    .trimIndent()
            )
    }

    @Test
    fun testUpdateSettingsGradleFileKMPProject() {
        val file = tempFolder.newFile()
        file.writeText(
            """
                // Stuff before includeProject section
                class MyClass {
                }
                // End stuff before includeProject section

                includeProject(":activity:activity", [BuildType.MAIN, BuildType.FLAN, BuildType.COMPOSE])
            """
                .trimIndent()
        )
        val gradleSettingsEditor = GradleSettingsEditor(file)
        val projectSpec =
            ProjectSpec("androidx.foo", "foo-bar", ProjectType.KMP, "", projectSetup.rootDir)
        gradleSettingsEditor.updateSettingsGradle(projectSpec)
        assertThat(file.readText())
            .isEqualTo(
                """
            // Stuff before includeProject section
            class MyClass {
            }
            // End stuff before includeProject section

            includeProject(":activity:activity", [BuildType.MAIN, BuildType.FLAN, BuildType.COMPOSE])
            includeProject(":foo:foo-bar", [BuildType.KMP])

        """
                    .trimIndent()
            )
    }

    @Test
    fun validateName_valid() {
        assertTrue(isGroupIdValid("androidx.foo"))
        assertTrue(isArtifactIdValid("androidx.foo", "foo-bar"))
    }

    @Test
    fun validateName_invalidGroupId() {
        assertFalse(isGroupIdValid("foo"))
        assertFalse(isGroupIdValid("com.example"))
        assertFalse(isGroupIdValid("androidx.compose"))
    }

    @Test
    fun validateName_invalidArtifactId() {
        assertFalse(isArtifactIdValid("androidx.foo", "bar"))
    }

    @Test
    fun testGeneratePackageName() {
        assertThat(generatePackageName("androidx.foo", "foo-bar")).isEqualTo("androidx.foo.bar")
        assertThat(generatePackageName("androidx.foo.bar", "bar-qux"))
            .isEqualTo("androidx.foo.bar.qux")
        assertThat(generatePackageName("androidx.foo", "foo")).isEqualTo("androidx.foo")
    }

    @Test
    fun testGetGroupIdVersionMacro() {
        assertThat(getGroupIdVersionMacro("androidx.foo")).isEqualTo("FOO")
        assertThat(getGroupIdVersionMacro("androidx.foo.bar")).isEqualTo("FOO_BAR")
        assertThat(getGroupIdVersionMacro("androidx.compose.ui")).isEqualTo("COMPOSE_UI")
        assertThat(getGroupIdVersionMacro("androidx.compose")).isEqualTo("COMPOSE")
    }

    @Test
    fun getGradleProjectCoordinates() {
        assertThat(getGradleProjectCoordinates("androidx.foo", "foo-bar")).isEqualTo(":foo:foo-bar")
        assertThat(getGradleProjectCoordinates("androidx.foo.bar", "bar-qux"))
            .isEqualTo(":foo:bar:bar-qux")
    }

    @Test
    fun getLibraryType() {
        assertThat(getLibraryType("foo-sample")).isEqualTo("SAMPLES")
        assertThat(getLibraryType("foo-compiler")).isEqualTo("ANNOTATION_PROCESSOR")
        assertThat(getLibraryType("foo-lint")).isEqualTo("LINT")
        assertThat(getLibraryType("foo-inspection")).isEqualTo("IDE_PLUGIN")
        assertThat(getLibraryType("foo-bar")).isEqualTo("PUBLISHED_LIBRARY")
    }
}
