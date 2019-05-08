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

package androidx.room.gradle

import com.google.common.truth.Expect
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.nio.file.Files
import java.util.Properties

@RunWith(Parameterized::class)
class RoomIncrementalAnnotationProcessingTest(private val withIncrementalRoom: Boolean) {

    companion object {

        @Parameterized.Parameters(name = "incrementalRoom={0}")
        @JvmStatic
        fun parameters() = listOf(true, false)

        private const val SRC_DIR = "src/main/java"
        private const val GEN_SRC_DIR = "build/generated/ap_generated_sources/debug/out/"
        private const val GEN_RES_DIR = "build/generated/resources"
        private const val CLASS_DIR = "build/intermediates/javac/debug/classes"

        private const val CLEAN_TASK = ":clean"
        private const val COMPILE_TASK = ":compileDebugJavaWithJavac"
    }

    @get:Rule
    val testProjectDir = TemporaryFolder()

    @get:Rule
    val expect: Expect = Expect.create()

    // Properties to set up test project
    private lateinit var prebuiltsRepo: String
    private lateinit var agpVersion: String
    private lateinit var localSupportRepo: String
    private lateinit var compileSdkVersion: String
    private lateinit var buildToolsVersion: String
    private lateinit var minSdkVersion: String
    private lateinit var debugKeystore: String

    // Original source files
    private lateinit var srcDatabase1: File
    private lateinit var srcDao1: File
    private lateinit var srcEntity1: File

    // Generated source files
    private lateinit var genDatabase1: File
    private lateinit var genDao1: File
    private lateinit var genDatabase2: File
    private lateinit var genDao2: File

    // Generated resource files
    private lateinit var genSchema1: File
    private lateinit var genSchema2: File

    // Compiled classes
    private lateinit var classSrcDatabase1: File
    private lateinit var classSrcDao1: File
    private lateinit var classSrcEntity1: File
    private lateinit var classSrcDatabase2: File
    private lateinit var classSrcDao2: File
    private lateinit var classSrcEntity2: File
    private lateinit var classGenDatabase1: File
    private lateinit var classGenDao1: File
    private lateinit var classGenDatabase2: File
    private lateinit var classGenDao2: File

    // Timestamps of files
    private lateinit var fileToTimestampMap: Map<File, Long>

    // Sets of files that have changed/not changed/deleted
    private lateinit var changedFiles: Set<File>
    private lateinit var unchangedFiles: Set<File>
    private lateinit var deletedFiles: Set<File>

    @Before
    fun setup() {
        val projectRoot = testProjectDir.root

        // copy local.properties
        File("../../../local.properties")
            .copyTo(File(projectRoot, "local.properties"), overwrite = true)

        // copy sdk.prop (created by module's build.gradle)
        RoomIncrementalAnnotationProcessingTest::class.java.classLoader
            .getResourceAsStream("sdk.prop")
            .use { input ->
                val properties = Properties().apply { load(input) }
                prebuiltsRepo = properties.getProperty("prebuiltsRepo")
                localSupportRepo = properties.getProperty("localSupportRepo")
                agpVersion = properties.getProperty("agpVersion")
                compileSdkVersion = properties.getProperty("compileSdkVersion")
                buildToolsVersion = properties.getProperty("buildToolsVersion")
                minSdkVersion = properties.getProperty("minSdkVersion")
                debugKeystore = properties.getProperty("debugKeystore")
            }

        // copy test project
        File("src/test/data/simple-project").copyRecursively(projectRoot)

        // set up build file
        File(projectRoot, "build.gradle").writeText(
            """
            buildscript {
                repositories {
                    maven { url "$prebuiltsRepo/androidx/external" }
                    maven { url "$prebuiltsRepo/androidx/internal" }
                }
                dependencies {
                    classpath 'com.android.tools.build:gradle:$agpVersion'
                }
            }

            apply plugin: 'com.android.application'

            repositories {
                maven { url "$prebuiltsRepo/androidx/external" }
                maven { url "$localSupportRepo" }
                maven {
                    url "$prebuiltsRepo/androidx/internal"
                    content {
                        excludeModule("androidx.room", "room-compiler")
                    }
                }
            }

            android {
                compileSdkVersion $compileSdkVersion
                buildToolsVersion "$buildToolsVersion"

                defaultConfig {
                    minSdkVersion $minSdkVersion
                }

                signingConfigs {
                    debug {
                        storeFile file("$debugKeystore")
                    }
                }
            }

            dependencies {
                // Uses latest Room built from tip of tree
                implementation "androidx.room:room-runtime:+"
                annotationProcessor "androidx.room:room-compiler:+"
            }

            class SchemaLocationArgumentProvider implements CommandLineArgumentProvider {

                @OutputDirectory
                File schemaDir

                SchemaLocationArgumentProvider(File schemaDir) {
                    this.schemaDir = schemaDir
                }

                @Override
                Iterable<String> asArguments() {
                    ["-Aroom.schemaLocation=" + schemaDir.path ]
                }
            }

            android {
                defaultConfig {
                    javaCompileOptions {
                        annotationProcessorOptions {
                            argument 'room.incremental', '$withIncrementalRoom'
                compilerArgumentProvider new SchemaLocationArgumentProvider(file('$GEN_RES_DIR'))
                        }
                    }
                }
            }
        """.trimIndent()
        )

        // Compute file paths
        srcDatabase1 = File(projectRoot, "$SRC_DIR/room/testapp/Database1.java")
        srcDao1 = File(projectRoot, "$SRC_DIR/room/testapp/Dao1.java")
        srcEntity1 = File(projectRoot, "$SRC_DIR/room/testapp/Entity1.java")

        genDatabase1 = File(projectRoot, "$GEN_SRC_DIR/room/testapp/Database1_Impl.java")
        genDao1 = File(projectRoot, "$GEN_SRC_DIR/room/testapp/Dao1_Impl.java")
        genDatabase2 = File(projectRoot, "$GEN_SRC_DIR/room/testapp/Database2_Impl.java")
        genDao2 = File(projectRoot, "$GEN_SRC_DIR/room/testapp/Dao2_Impl.java")

        genSchema1 = File(projectRoot, "$GEN_RES_DIR/room.testapp.Database1/1.json")
        genSchema2 = File(projectRoot, "$GEN_RES_DIR/room.testapp.Database2/1.json")

        classSrcDatabase1 = File(projectRoot, "$CLASS_DIR/room/testapp/Database1.class")
        classSrcDao1 = File(projectRoot, "$CLASS_DIR/room/testapp/Dao1.class")
        classSrcEntity1 = File(projectRoot, "$CLASS_DIR/room/testapp/Entity1.class")
        classSrcDatabase2 = File(projectRoot, "$CLASS_DIR/room/testapp/Database2.class")
        classSrcDao2 = File(projectRoot, "$CLASS_DIR/room/testapp/Dao2.class")
        classSrcEntity2 = File(projectRoot, "$CLASS_DIR/room/testapp/Entity2.class")
        classGenDatabase1 = File(projectRoot, "$CLASS_DIR/room/testapp/Database1_Impl.class")
        classGenDao1 = File(projectRoot, "$CLASS_DIR/room/testapp/Dao1_Impl.class")
        classGenDatabase2 = File(projectRoot, "$CLASS_DIR/room/testapp/Database2_Impl.class")
        classGenDao2 = File(projectRoot, "$CLASS_DIR/room/testapp/Dao2_Impl.class")
    }

    private fun runGradleTasks(vararg args: String): BuildResult {
        return GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments(*args)
            .build()
    }

    private fun runFullBuild(): BuildResult {
        val result = runGradleTasks(CLEAN_TASK, COMPILE_TASK)
        recordTimestamps()
        return result
    }

    private fun runIncrementalBuild(): BuildResult {
        val result = runGradleTasks(COMPILE_TASK)
        recordFileChanges()
        return result
    }

    private fun recordTimestamps() {
        val files = listOf(
            genDatabase1,
            genDao1,
            genDatabase2,
            genDao2,
            genSchema1,
            genSchema2,
            classSrcDatabase1,
            classSrcDao1,
            classSrcEntity1,
            classSrcDatabase2,
            classSrcDao2,
            classSrcEntity2,
            classGenDatabase1,
            classGenDao1,
            classGenDatabase2,
            classGenDao2
        )

        val map = mutableMapOf<File, Long>()
        for (file in files) {
            map[file] = file.lastModified()
        }
        fileToTimestampMap = map.toMap()
    }

    private fun recordFileChanges() {
        changedFiles = fileToTimestampMap.filter { (file, previousTimestamp) ->
            file.exists() && file.lastModified() != previousTimestamp
        }.keys

        unchangedFiles = fileToTimestampMap.filter { (file, previousTimestamp) ->
            file.exists() && file.lastModified() == previousTimestamp
        }.keys

        deletedFiles = fileToTimestampMap.filter { (file, _) -> !file.exists() }.keys
    }

    private fun assertFilesExist(vararg files: File) {
        expect.that(files.filter { it.exists() }).named("Existing files")
            .containsExactlyElementsIn(files)
    }

    private fun assertChangedFiles(vararg files: File) {
        expect.that(changedFiles).named("Changed files").containsAtLeastElementsIn(files)
    }

    private fun assertUnchangedFiles(vararg files: File) {
        expect.that(unchangedFiles).named("Unchanged files").containsAtLeastElementsIn(files)
    }

    private fun assertDeletedFiles(vararg files: File) {
        expect.that(deletedFiles).named("Deleted files").containsAtLeastElementsIn(files)
    }

    private fun searchAndReplace(file: File, search: String, replace: String) {
        file.writeText(file.readText().replace(search, replace))
    }

    @Test
    fun `verify first full build`() {
        // This test verifies the results of the first full (non-incremental) build. The other tests
        // verify the results of the second incremental build based on different change scenarios.
        val result = runFullBuild()
        expect.that(result.task(COMPILE_TASK)!!.outcome).isEqualTo(TaskOutcome.SUCCESS)

        // Check annotation processing outputs
        assertFilesExist(
            genDatabase1,
            genDao1,
            genDatabase2,
            genDao2,
            genSchema1,
            genSchema2
        )

        // Check compilation outputs
        assertFilesExist(
            classSrcDatabase1,
            classSrcDao1,
            classSrcEntity1,
            classSrcDatabase2,
            classSrcDao2,
            classSrcEntity2,
            classGenDatabase1,
            classGenDao1,
            classGenDatabase2,
            classGenDao2
        )
    }

    @Test
    fun `change source file`() {
        runFullBuild()

        // Change a source file
        searchAndReplace(
            srcEntity1,
            "// Insert a change here",
            """
            @androidx.room.ColumnInfo(name = "name")
            private String mName = "";

            public String getName() {
                return mName;
            }

            public void setName(String name) {
                mName = name;
            }
            """.trimIndent()
        )

        val result = runIncrementalBuild()
        expect.that(result.task(COMPILE_TASK)!!.outcome).isEqualTo(TaskOutcome.SUCCESS)

        // Check annotation processing outputs:
        //   - Relevant files should be re-generated
        //   - Irrelevant files should not be re-generated (if Room is incremental)
        if (withIncrementalRoom) {
            assertChangedFiles(
                genDatabase1,
                genDao1,
                genSchema1
            )
            assertUnchangedFiles(
                genDatabase2,
                genDao2,
                genSchema2
            )
        } else {
            assertChangedFiles(
                genDatabase1,
                genDao1,
                genSchema1,
                genDatabase2,
                genDao2
            )
            // Room is able to avoid re-generating schema file 2 as its contents have not changed
            assertUnchangedFiles(genSchema2)
        }

        // Check compilation outputs:
        //   - Relevant files should be recompiled
        //   - Irrelevant files should not be recompiled (if Room is incremental)
        if (withIncrementalRoom) {
            assertChangedFiles(
                classSrcDatabase1,
                classSrcEntity1,
                classGenDatabase1,
                classGenDao1
            )
            assertUnchangedFiles(
                classSrcDao1, // Gradle detects that this file is not relevant to the change
                classSrcDatabase2,
                classSrcDao2,
                classSrcEntity2,
                classGenDatabase2,
                classGenDao2
            )
        } else {
            assertChangedFiles(
                classSrcDatabase1,
                classSrcDao1,
                classSrcEntity1,
                classGenDatabase1,
                classGenDao1,
                classSrcDatabase2,
                classSrcDao2,
                classSrcEntity2,
                classGenDatabase2,
                classGenDao2
            )
        }
    }

    @Test
    fun `delete group of source files`() {
        runFullBuild()

        // Delete the first group of source files
        Files.delete(srcDatabase1.toPath())
        Files.delete(srcDao1.toPath())
        Files.delete(srcEntity1.toPath())

        val result = runIncrementalBuild()
        if (withIncrementalRoom) {
            // Gradle detects that nothing needs to be recompiled so it reports as UP-TO-DATE
            expect.that(result.task(COMPILE_TASK)!!.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
        } else {
            expect.that(result.task(COMPILE_TASK)!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

        // Check annotation processing outputs:
        //   - Relevant files should be re-generated (or deleted)
        //   - Irrelevant files should not be re-generated (if Room is incremental)
        if (withIncrementalRoom) {
            assertDeletedFiles(
                genDatabase1,
                genDao1
            )
            assertUnchangedFiles(
                // EXPECTATION-NOT-MET: Schema file 1 should be deleted but is not
                // (https://issuetracker.google.com/134472065).
                genSchema1,
                genDatabase2,
                genDao2,
                genSchema2
            )
        } else {
            assertDeletedFiles(
                genDatabase1,
                genDao1
            )
            // EXPECTATION-NOT-MET: Schema file 1 should be deleted but is not
            // (https://github.com/gradle/gradle/issues/9401).
            assertUnchangedFiles(genSchema1)
            assertChangedFiles(
                genDatabase2,
                genDao2
            )
            // Room is able to avoid re-generating schema file 2 as its contents have not changed
            assertUnchangedFiles(genSchema2)
        }

        // Check compilation outputs:
        //   - Relevant files should be recompiled (or deleted)
        //   - Irrelevant files should not be recompiled (if Room is incremental)
        if (withIncrementalRoom) {
            assertDeletedFiles(
                classSrcDatabase1,
                classSrcDao1,
                classSrcEntity1,
                classGenDatabase1,
                classGenDao1
            )
            assertUnchangedFiles(
                classSrcDatabase2,
                classSrcDao2,
                classSrcEntity2,
                classGenDatabase2,
                classGenDao2
            )
        } else {
            assertDeletedFiles(
                classSrcDatabase1,
                classSrcDao1,
                classSrcEntity1,
                classGenDatabase1,
                classGenDao1
            )
            assertChangedFiles(
                classSrcDatabase2,
                classSrcDao2,
                classSrcEntity2,
                classGenDatabase2,
                classGenDao2
            )
        }
    }
}