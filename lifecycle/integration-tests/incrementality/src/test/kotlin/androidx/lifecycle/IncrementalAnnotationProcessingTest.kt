/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.lifecycle

import androidx.testutils.gradle.ProjectSetupRule
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class IncrementalAnnotationProcessingTest {

    companion object {
        private const val MAIN_DIR = "app/src/main"
        private const val BUILD_DIR = "app/build"
        private const val SOURCE_DIR = "$MAIN_DIR/java/androidx/lifecycle/incap"
        private const val GENERATED_SOURCE_DIR =
            BUILD_DIR + "/generated/ap_generated_sources/debug/out/androidx/lifecycle/incap"
        private const val COMPILE_TASK_NAME = "compileDebugJavaWithJavac"
        private const val CLASSES_DIR =
            "$BUILD_DIR/intermediates/javac/debug/$COMPILE_TASK_NAME/classes"
        private const val GENERATED_PROGUARD_DIR = "$CLASSES_DIR/META-INF/proguard"
        private const val APP_CLASS_DIR = "$CLASSES_DIR/androidx/lifecycle/incap"
    }

    @get:Rule val projectSetup = ProjectSetupRule()

    private lateinit var projectRoot: File
    private lateinit var fooObserver: File
    private lateinit var barObserver: File
    private lateinit var genFooAdapter: File
    private lateinit var genBarAdapter: File
    private lateinit var genFooProguard: File
    private lateinit var genBarProguard: File
    private lateinit var fooObserverClass: File
    private lateinit var barObserverClass: File
    private lateinit var genFooAdapterClass: File
    private lateinit var genBarAdapterClass: File

    @Before
    fun setup() {
        projectRoot = projectSetup.rootDir
        fooObserver = File(projectRoot, "$SOURCE_DIR/FooObserver.java")
        barObserver = File(projectRoot, "$SOURCE_DIR/BarObserver.java")
        genFooAdapter =
            File(projectRoot, GENERATED_SOURCE_DIR + "/FooObserver_LifecycleAdapter.java")
        genBarAdapter =
            File(projectRoot, GENERATED_SOURCE_DIR + "/BarObserver_LifecycleAdapter.java")
        genFooProguard =
            File(projectRoot, GENERATED_PROGUARD_DIR + "/androidx.lifecycle.incap.FooObserver.pro")
        genBarProguard =
            File(projectRoot, GENERATED_PROGUARD_DIR + "/androidx.lifecycle.incap.BarObserver.pro")
        fooObserverClass = File(projectRoot, "$APP_CLASS_DIR/FooObserver.class")
        barObserverClass = File(projectRoot, "$APP_CLASS_DIR/BarObserver.class")
        genFooAdapterClass =
            File(projectRoot, APP_CLASS_DIR + "/FooObserver_LifecycleAdapter.class")
        genBarAdapterClass =
            File(projectRoot, APP_CLASS_DIR + "/BarObserver_LifecycleAdapter.class")
        projectRoot.mkdirs()
        setupProjectBuildGradle()
        setupAppBuildGradle()
        setupSettingsGradle()
        setupAndroidManifest()
        addSource()
    }

    fun gradleRunner(): GradleRunner {
        return GradleRunner.create().withProjectDir(projectRoot)
    }

    @Test
    fun checkModifySource() {
        gradleRunner().withArguments("clean", COMPILE_TASK_NAME).build()

        val fooAdapterFirstBuild = Files.getLastModifiedTime(genFooAdapter.toPath()).toMillis()
        val barAdapterFirstBuild = Files.getLastModifiedTime(genBarAdapter.toPath()).toMillis()
        val fooProguardFirstBuild = Files.getLastModifiedTime(genFooProguard.toPath()).toMillis()
        val barProguardFirstBuild = Files.getLastModifiedTime(genBarProguard.toPath()).toMillis()
        val fooObserverClassFirstBuild =
            Files.getLastModifiedTime(fooObserverClass.toPath()).toMillis()
        val barObserverClassFirstBuild =
            Files.getLastModifiedTime(barObserverClass.toPath()).toMillis()
        val fooAdapterClassFirstBuild =
            Files.getLastModifiedTime(genFooAdapterClass.toPath()).toMillis()
        val barAdapterClassFirstBuild =
            Files.getLastModifiedTime(genBarAdapterClass.toPath()).toMillis()

        searchAndReplace(fooObserver.toPath(), "FooObserver_Log", "Modified_FooObserver_Log")

        gradleRunner().withArguments(COMPILE_TASK_NAME).build()

        val fooAdapterSecondBuild = Files.getLastModifiedTime(genFooAdapter.toPath()).toMillis()
        val barAdapterSecondBuild = Files.getLastModifiedTime(genBarAdapter.toPath()).toMillis()
        val fooProguardSecondBuild = Files.getLastModifiedTime(genFooProguard.toPath()).toMillis()
        val barProguardSecondBuild = Files.getLastModifiedTime(genBarProguard.toPath()).toMillis()
        val fooObserverClassSecondBuild =
            Files.getLastModifiedTime(fooObserverClass.toPath()).toMillis()
        val barObserverClassSecondBuild =
            Files.getLastModifiedTime(barObserverClass.toPath()).toMillis()
        val fooAdapterClassSecondBuild =
            Files.getLastModifiedTime(genFooAdapterClass.toPath()).toMillis()
        val barAdapterClassSecondBuild =
            Files.getLastModifiedTime(genBarAdapterClass.toPath()).toMillis()

        // FooObserver's adapter and its proguard file are regenerated
        // FooObserver and its regenerated adapter are recompiled
        assertThat(fooAdapterFirstBuild).isLessThan(fooAdapterSecondBuild)
        assertThat(fooProguardFirstBuild).isLessThan(fooProguardSecondBuild)
        assertThat(fooObserverClassFirstBuild).isLessThan(fooObserverClassSecondBuild)
        assertThat(fooAdapterClassFirstBuild).isLessThan(fooAdapterClassSecondBuild)
        // BarObserver's adapter and its proguard are not regenerated
        // BarObserver and its generated adapter are not recompiled
        assertThat(barAdapterFirstBuild).isEqualTo(barAdapterSecondBuild)
        assertThat(barProguardFirstBuild).isEqualTo(barProguardSecondBuild)
        assertThat(barObserverClassFirstBuild).isEqualTo(barObserverClassSecondBuild)
        assertThat(barAdapterClassFirstBuild).isEqualTo(barAdapterClassSecondBuild)
    }

    @Test
    fun checkDeleteOneSource() {
        gradleRunner().withArguments("clean", COMPILE_TASK_NAME).build()
        val barAdapterFirstBuild = Files.getLastModifiedTime(genBarAdapter.toPath()).toMillis()
        val barProguardFirstBuild = Files.getLastModifiedTime(genBarProguard.toPath()).toMillis()
        val barObserverClassFirstBuild =
            Files.getLastModifiedTime(barObserverClass.toPath()).toMillis()
        val barAdapterClassFirstBuild =
            Files.getLastModifiedTime(genBarAdapterClass.toPath()).toMillis()

        assertThat(genFooAdapter.exists()).isTrue()
        assertThat(genFooProguard.exists()).isTrue()

        fooObserver.delete()

        gradleRunner().withArguments(COMPILE_TASK_NAME).build()

        val barAdapterSecondBuild = Files.getLastModifiedTime(genBarAdapter.toPath()).toMillis()
        val barProguardSecondBuild = Files.getLastModifiedTime(genBarProguard.toPath()).toMillis()
        val barObserverClassSecondBuild =
            Files.getLastModifiedTime(barObserverClass.toPath()).toMillis()
        val barAdapterClassSecondBuild =
            Files.getLastModifiedTime(genBarAdapterClass.toPath()).toMillis()

        // FooObserver's adapter and its proguard file are deleted since FooObserver is removed
        assertThat(genFooAdapter.exists()).isFalse()
        assertThat(genFooProguard.exists()).isFalse()
        // BarObserver's adapter and its proguard are not regenerated
        // BarObserver and its generated adapter are not recompiled
        assertThat(barAdapterFirstBuild).isEqualTo(barAdapterSecondBuild)
        assertThat(barProguardFirstBuild).isEqualTo(barProguardSecondBuild)
        assertThat(barObserverClassFirstBuild).isEqualTo(barObserverClassSecondBuild)
        assertThat(barAdapterClassFirstBuild).isEqualTo(barAdapterClassSecondBuild)
    }

    private fun setupProjectBuildGradle() {
        val repositoriesBlock = buildString {
            appendLine("repositories {")
            projectSetup.allRepositoryPaths.forEach { appendLine("""maven { url "$it" }""") }
            appendLine("}")
        }
        addFileWithContent(
            "build.gradle",
            """
            buildscript {
                ${repositoriesBlock.prependIndent("    ")}
                dependencies {
                    classpath "${projectSetup.props.agpDependency}"
                }
            }

            allprojects {
                $repositoriesBlock
            }

            task clean(type: Delete) {
                delete rootProject.buildDir
            }
            """
                .trimIndent()
        )
    }

    private fun setupAppBuildGradle() {
        addFileWithContent(
            "app/build.gradle",
            """
            apply plugin: 'com.android.application'

            android {
                namespace "androidx.lifecycle.incap"
                compileSdk ${projectSetup.props.compileSdk}
                buildToolsVersion "${projectSetup.props.buildToolsVersion}"

                defaultConfig {
                    minSdkVersion ${projectSetup.props.minSdkVersion}
                }

                signingConfigs {
                    debug {
                        storeFile file("${projectSetup.props.debugKeystore}")
                    }
                }
            }

            dependencies {
                // Use the latest lifecycle-runtime to keep up with lifecycle-compiler
                implementation "androidx.lifecycle:lifecycle-runtime:+"
                // Use the latest version to test lifecycle-compiler artifact built from tip of tree
                annotationProcessor "androidx.lifecycle:lifecycle-compiler:+"
            }
            """
                .trimIndent()
        )
    }

    private fun setupSettingsGradle() {
        addFileWithContent(
            "settings.gradle",
            """
            include ':app'
            """
                .trimIndent()
        )
    }

    private fun setupAndroidManifest() {
        addFileWithContent(
            "$MAIN_DIR/AndroidManifest.xml",
            """
            <manifest/>
            """
                .trimIndent()
        )
    }

    private fun addSource() {
        addFileWithContent(
            "$SOURCE_DIR/FooObserver.java",
            """
            package androidx.lifecycle.incap;

            import android.util.Log;

            import androidx.lifecycle.Lifecycle;
            import androidx.lifecycle.LifecycleObserver;
            import androidx.lifecycle.OnLifecycleEvent;

            class FooObserver implements LifecycleObserver {
            private String mLog = "FooObserver_Log";

                @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
                public void onResume() {
                    Log.i(mLog, "onResume");
                }
            }
            """
                .trimIndent()
        )

        addFileWithContent(
            "$SOURCE_DIR/BarObserver.java",
            """
            package androidx.lifecycle.incap;

            import android.util.Log;

            import androidx.lifecycle.Lifecycle;
            import androidx.lifecycle.LifecycleObserver;
            import androidx.lifecycle.OnLifecycleEvent;

            class BarObserver implements LifecycleObserver {
                private String mLog = "BarObserver_Log";

                @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
                public void onResume() {
                    Log.i(mLog, "onResume");
                }
            }
            """
                .trimIndent()
        )
    }

    private fun addFileWithContent(relativePath: String, content: String) {
        val file = File(projectRoot, relativePath)
        file.parentFile.mkdirs()
        file.writeText(content)
    }

    private fun searchAndReplace(file: Path, search: String, replace: String) {
        val content = String(Files.readAllBytes(file))
        val newContent = content.replace(search, replace)
        Files.write(file, newContent.toByteArray())
    }
}
