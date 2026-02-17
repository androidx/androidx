/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.stableaidl.tasks

import androidx.testutils.gradle.ProjectSetupRule
import java.io.File
import java.util.zip.ZipFile
import kotlin.test.assertContains
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StableAidlPackagedListTest {
    @get:Rule
    val projectSetup = ProjectSetupRule()
    private lateinit var gradleRunner: GradleRunner

    @Before
    fun setUp() {
        gradleRunner =
            GradleRunner.create().withProjectDir(projectSetup.rootDir).withPluginClasspath()
    }

    @Test
    fun api_36() {
        projectSetup.writeDefaultBuildGradle(
            prefix =
                """
                plugins {
                    id('com.android.library')
                    id('androidx.stableaidl')
                }
            """
                    .trimIndent(),
            suffix =
                """
            android {
                compileSdk 36
                namespace 'androidx.stableaidl.testapp'
                buildFeatures {
                  aidl = true
                }
                buildTypes.all {
                  stableAidl {
                    version 1
                  }
                }
                aidlPackagedList += 'android/os/IMyService.aidl'
            }
            """
                    .trimIndent()
        )

        val myServiceAidlFile =
            createFile("src/main/stableAidl/android/os/IMyService.aidl", projectSetup.rootDir)
        myServiceAidlFile.writeText(
            """
            package android.os;
            
            import android.os.Bundle;
            
            interface IMyService {
                Bundle getBundle() = 0;
            }
        """
                .trimIndent()
        )

        gradleRunner
            .withArguments(
                "assembleRelease",
                "--stacktrace",
                "-Pstableaidl.compilesdk=36",
            )
            .build()

        val aarFile = File(projectSetup.rootDir, "build/outputs/aar").listFiles().single()
        ZipFile(aarFile).use { zip ->
            val zipEntryNames = zip.getEntryNames()
            assertContains(zipEntryNames, "aidl/android/os/IMyService.aidl")
        }
    }

    @Test
    fun pre_api_36_with_shadowFrameworkDir() {
        projectSetup.writeDefaultBuildGradle(
            prefix =
                """
                plugins {
                    id('com.android.library')
                    id('androidx.stableaidl')
                }
            """
                    .trimIndent(),
            suffix =
                """
            android {
                compileSdk 35
                namespace 'androidx.stableaidl.testapp'
                buildFeatures {
                  aidl = true
                }
                buildTypes.all {
                  stableAidl {
                    version 1
                  }
                }
                aidlPackagedList += 'android/os/IMyService.aidl'
            }
            stableAidl {
                shadowFrameworkDir = file("${
                    File(
                        projectSetup.props.rootProjectPath,
                        "../../buildSrc/stableAidlImports"
                    ).invariantSeparatorsPath
                }")
            }
            """
                    .trimIndent()
        )

        val myServiceAidlFile =
            createFile("src/main/stableAidl/android/os/IMyService.aidl", projectSetup.rootDir)
        myServiceAidlFile.writeText(
            """
            package android.os;
            
            import android.os.Bundle;
            
            interface IMyService {
                Bundle getBundle() = 0;
            }
        """
                .trimIndent()
        )

        gradleRunner
            .withArguments(
                "assembleRelease",
                "--stacktrace",
                "-Pstableaidl.compilesdk=35",
            )
            .build()

        val aarFile = File(projectSetup.rootDir, "build/outputs/aar").listFiles().single()
        ZipFile(aarFile).use { zip ->
            val zipEntryNames = zip.getEntryNames()
            assertContains(zipEntryNames, "aidl/android/os/IMyService.aidl")
        }
    }

    @Test
    fun pre_api_36_without_shadowFrameworkDir_with_framework_classes() {
        projectSetup.writeDefaultBuildGradle(
            prefix =
                """
                plugins {
                    id('com.android.library')
                    id('androidx.stableaidl')
                }
            """
                    .trimIndent(),
            suffix =
                """
            android {
                compileSdk 35
                namespace 'androidx.stableaidl.testapp'
                buildFeatures {
                  aidl = true
                }
                buildTypes.all {
                  stableAidl {
                    version 1
                  }
                }
                aidlPackagedList += 'android/os/IMyService.aidl'
            }
            """
                    .trimIndent()
        )

        val myServiceAidlFile =
            createFile("src/main/stableAidl/android/os/IMyService.aidl", projectSetup.rootDir)
        myServiceAidlFile.writeText(
            """
            package android.os;
            
            import android.os.Bundle;
            
            interface IMyService {
                Bundle getBundle() = 0;
            }
        """
                .trimIndent()
        )

        val output = gradleRunner
            .withArguments(
                "assembleRelease",
                "--stacktrace",
                "-Pstableaidl.compilesdk=35",
            )
            .buildAndFail()
        assertTrue { output.output.contains("Couldn't find import for class android.os.Bundle.") }
    }

    @Test
    fun pre_api_36_without_shadowFrameworkDir_without_framework_classes() {
        projectSetup.writeDefaultBuildGradle(
            prefix =
                """
                plugins {
                    id('com.android.library')
                    id('androidx.stableaidl')
                }
            """
                    .trimIndent(),
            suffix =
                """
            android {
                compileSdk 35
                namespace 'androidx.stableaidl.testapp'
                buildFeatures {
                  aidl = true
                }
                buildTypes.all {
                  stableAidl {
                    version 1
                  }
                }
                aidlPackagedList += 'android/os/IMyService.aidl'
            }
            """
                    .trimIndent()
        )

        val myServiceAidlFile =
            createFile("src/main/stableAidl/android/os/IMyService.aidl", projectSetup.rootDir)
        myServiceAidlFile.writeText(
            """
            package android.os;
            
            interface IMyService {
            }
        """
                .trimIndent()
        )

        gradleRunner
            .withArguments(
                "assembleRelease",
                "--stacktrace",
                "-Pstableaidl.compilesdk=35",
            )
            .build()
    }
}
