/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.navigation.safe.args.generator

import androidx.navigation.safe.args.generator.java.JavaCodeFile
import androidx.navigation.safe.args.generator.kotlin.KotlinCodeFile
import com.google.common.truth.Truth
import com.google.testing.compile.JavaSourcesSubject
import java.io.File
import java.lang.IllegalStateException
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class NavGeneratorTest(private val generateKotlin: Boolean) {

    @Suppress("MemberVisibilityCanBePrivate")
    @get:Rule
    val workingDir = TemporaryFolder()

    val fileNameExt = if (generateKotlin) "kt" else "java"

    private fun generateSafeArgs(
        rFilePackage: String,
        applicationId: String,
        navigationXml: File,
        outputDir: File
    ) = SafeArgsGenerator(
        rFilePackage = rFilePackage,
        applicationId = applicationId,
        navigationXml = navigationXml,
        outputDir = outputDir,
        useAndroidX = true,
        generateKotlin = generateKotlin
    ).generate()

    private fun CodeFile.assertParsesAs(fullClassName: String, folder: String) {
        when (this) {
            is JavaCodeFile -> {
                JavaSourcesSubject.assertThat(this.toJavaFileObject())
                    .parsesAs(fullClassName, "expected/nav_generator_test/java/$folder")
            }
            is KotlinCodeFile -> {
                Truth.assertThat(this.wrapped.toString())
                    .isEqualTo(
                        loadSourceString(
                            fullClassName,
                            "expected/nav_generator_test/kotlin/$folder",
                            "kt"
                        )
                    )
            }
            else -> throw IllegalStateException("Unknown CodeFile type.")
        }
    }

    @Test
    fun naive_test() {
        val output = generateSafeArgs(
            "foo", "foo.flavor",
            testData("naive_test.xml"), workingDir.root
        )
        val fileNames = output.fileNames
        val expectedSet = setOf(
            "androidx.navigation.testapp.MainFragmentDirections",
            "foo.flavor.NextFragmentDirections",
            "androidx.navigation.testapp.MainFragmentArgs",
            "foo.flavor.NextFragmentArgs"
        )
        assertThat(output.errors.isEmpty(), `is`(true))
        assertThat(fileNames.toSet(), `is`(expectedSet))
        fileNames.forEach { name ->
            val file =
                File(workingDir.root, "${name.replace('.', File.separatorChar)}.$fileNameExt")
            assertThat(file.exists(), `is`(true))
        }
    }

    @Test
    fun nested_test() {
        val output = generateSafeArgs(
            "foo", "foo.flavor",
            testData("nested_login_test.xml"), workingDir.root
        )
        val fileNames = output.fileNames
        val expectedSet = setOf(
            "foo.flavor.MainFragmentDirections",
            "foo.LoginDirections",
            "foo.flavor.account.LoginFragmentDirections",
            "foo.flavor.account.RegisterFragmentDirections"
        )
        assertThat(output.errors.isEmpty(), `is`(true))
        assertThat(fileNames.toSet(), `is`(expectedSet))
        fileNames.forEach { name ->
            val file =
                File(workingDir.root, "${name.replace('.', File.separatorChar)}.$fileNameExt")
            assertThat(file.exists(), `is`(true))
        }

        val codeFiles = fileNames
            .mapIndexed { index, name -> name to (output.files[index]) }
            .associate { it }
        codeFiles.forEach { (name, file) ->
            file.assertParsesAs(name, "nested")
        }
    }

    @Test
    fun nested_same_action_test() {
        val output = generateSafeArgs(
            "foo", "foo.flavor",
            testData("nested_same_action_test.xml"), workingDir.root
        )
        val fileNames = output.fileNames
        val expectedSet = setOf(
            "foo.flavor.MainFragmentDirections",
            "foo.SettingsDirections",
            "foo.flavor.SettingsFragmentDirections"
        )
        assertThat(output.errors.isEmpty(), `is`(true))
        assertThat(fileNames.toSet(), `is`(expectedSet))
        fileNames.forEach { name ->
            val file =
                File(workingDir.root, "${name.replace('.', File.separatorChar)}.$fileNameExt")
            assertThat(file.exists(), `is`(true))
        }

        val codeFiles = fileNames
            .mapIndexed { index, name -> name to (output.files[index]) }
            .associate { it }
        codeFiles.forEach { (name, file) ->
            file.assertParsesAs(name, "nested_same_action")
        }
    }

    @Test
    fun nested_overridden_action_test() {
        val output = generateSafeArgs(
            "foo", "foo.flavor",
            testData("nested_overridden_action_test.xml"), workingDir.root
        )
        val fileNames = output.fileNames
        val expectedSet = setOf(
            "foo.flavor.MainFragmentDirections",
            "foo.SettingsDirections",
            "foo.flavor.SettingsFragmentDirections",
            "foo.InnerSettingsDirections",
            "foo.flavor.InnerSettingsFragmentDirections"
        )
        assertThat(output.errors.isEmpty(), `is`(true))
        assertThat(fileNames.toSet(), `is`(expectedSet))
        fileNames.forEach { name ->
            val file =
                File(workingDir.root, "${name.replace('.', File.separatorChar)}.$fileNameExt")
            assertThat(file.exists(), `is`(true))
        }

        val codeFiles = fileNames
            .mapIndexed { index, name -> name to (output.files[index]) }
            .associate { it }
        codeFiles.forEach { (name, file) ->
            file.assertParsesAs(name, "nested_overridden_action")
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "generateKotlin={0}")
        fun data() = listOf(false, true)
    }
}
