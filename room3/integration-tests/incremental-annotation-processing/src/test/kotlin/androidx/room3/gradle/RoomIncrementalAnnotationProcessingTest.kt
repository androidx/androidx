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

package androidx.room3.gradle

import androidx.testutils.gradle.ProjectSetupRule
import com.google.common.truth.Expect
import java.io.File
import java.nio.file.Files
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RoomIncrementalAnnotationProcessingTest() {

    companion object {

        private const val SRC_DIR = "src/main/java"
        private const val GEN_RES_DIR = "schemas"
        private const val JAVA_CLASS_DIR =
            "build/intermediates/javac/debug/compileDebugJavaWithJavac/classes"
        private const val KOTLIN_CLASS_DIR = "build/tmp/kotlin-classes/debug"

        private const val CLEAN_TASK = ":clean"
        private const val COMPILE_TASK = ":compileDebugJavaWithJavac"
    }

    @get:Rule val projectSetup = ProjectSetupRule()

    @get:Rule val expect: Expect = Expect.create()

    private val genSrcDir = "build/generated/ksp/debug/kotlin"

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

    /**
     * Find the Room version from local repo. Using + instead of an explicit version might cause
     * gradle to find a newer version from prebuilts (SNAPSHOT).
     */
    private val roomVersion by lazy {
        projectSetup.getLibraryLatestVersionInLocalRepo("androidx/room3/room3-compiler")
    }

    @Before
    fun setup() {
        val projectRoot = projectSetup.rootDir
        val repositoriesBlock = buildString {
            appendLine("repositories {")
            projectSetup.allRepositoryPaths.forEach {
                appendLine(
                    """
                    maven {
                        url "$it"
                    }
                    """
                        .trimIndent()
                )
            }
            appendLine("}")
        }

        // copy test project
        File("src/test/test-data/simple-project").copyRecursively(projectRoot)

        // add a kotlin file to trigger kotlin compilation
        projectRoot.resolve("src/main/java/placeholer.kt").writeText("")

        // set up build file
        File(projectRoot, "build.gradle")
            .writeText(
                """
            plugins {
                id('com.android.application')
                id('kotlin-android')
                id('com.google.devtools.ksp')
            }

            $repositoriesBlock

            %s

            dependencies {
                // Uses latest Room built from tip of tree
                implementation "androidx.room3:room3-runtime:$roomVersion"
                ksp "androidx.room3:room3-compiler:$roomVersion"
            }

            tasks.withType(
                org.jetbrains.kotlin.gradle.tasks.KotlinCompile
            ).configureEach {
                kotlinOptions {
                    jvmTarget = "11"
                }
            }

            android {
                namespace = "room.testapp"

                compileOptions {
                  sourceCompatibility = JavaVersion.VERSION_11
                  targetCompatibility = JavaVersion.VERSION_11
                }
            }

            ksp {
                arg('room.schemaLocation', '${projectRoot.resolve(GEN_RES_DIR).canonicalPath}')
            }
        """
                    .trimIndent()
                    // doing format instead of "$projectSetup.androidProject" on purpose,
                    // because otherwise trimIndent will mess with formatting
                    .format(projectSetup.androidProject)
            )

        // Compute file paths
        srcDatabase1 = File(projectRoot, "$SRC_DIR/room/testapp/Database1.java")
        srcDao1 = File(projectRoot, "$SRC_DIR/room/testapp/Dao1.java")
        srcEntity1 = File(projectRoot, "$SRC_DIR/room/testapp/Entity1.java")

        genDatabase1 = File(projectRoot, "$genSrcDir/room/testapp/Database1_Impl.kt")
        genDao1 = File(projectRoot, "$genSrcDir/room/testapp/Dao1_Impl.kt")
        genDatabase2 = File(projectRoot, "$genSrcDir/room/testapp/Database2_Impl.kt")
        genDao2 = File(projectRoot, "$genSrcDir/room/testapp/Dao2_Impl.kt")

        genSchema1 = File(projectRoot, "$GEN_RES_DIR/room.testapp.Database1/1.json")
        genSchema2 = File(projectRoot, "$GEN_RES_DIR/room.testapp.Database2/1.json")

        classSrcDatabase1 = File(projectRoot, "$JAVA_CLASS_DIR/room/testapp/Database1.class")
        classSrcDao1 = File(projectRoot, "$JAVA_CLASS_DIR/room/testapp/Dao1.class")
        classSrcEntity1 = File(projectRoot, "$JAVA_CLASS_DIR/room/testapp/Entity1.class")
        classSrcDatabase2 = File(projectRoot, "$JAVA_CLASS_DIR/room/testapp/Database2.class")
        classSrcDao2 = File(projectRoot, "$JAVA_CLASS_DIR/room/testapp/Dao2.class")
        classSrcEntity2 = File(projectRoot, "$JAVA_CLASS_DIR/room/testapp/Entity2.class")
        classGenDatabase1 = File(projectRoot, "$KOTLIN_CLASS_DIR/room/testapp/Database1_Impl.class")
        classGenDao1 = File(projectRoot, "$KOTLIN_CLASS_DIR/room/testapp/Dao1_Impl.class")
        classGenDatabase2 = File(projectRoot, "$KOTLIN_CLASS_DIR/room/testapp/Database2_Impl.class")
        classGenDao2 = File(projectRoot, "$KOTLIN_CLASS_DIR/room/testapp/Dao2_Impl.class")

        projectRoot
            .resolve("gradle.properties")
            .writeText(
                """
                ksp.incremental=true
                ksp.incremental.log=true
                android.useAndroidX=true
                android.builtInKotlin=false
                """
                    .trimIndent()
            )
    }

    private fun runGradleTasks(vararg args: String): BuildResult {
        return GradleRunner.create()
            .withProjectDir(projectSetup.rootDir)
            .withPluginClasspath()
            // workaround for b/231154556
            .withArguments("-Dorg.gradle.jvmargs=-Xmx1g -XX:MaxMetaspaceSize=512m", *args)
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
        val files =
            listOf(
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
                classGenDao2,
            )

        val map = mutableMapOf<File, Long>()
        for (file in files) {
            map[file] = file.lastModified()
        }
        fileToTimestampMap = map.toMap()
    }

    private fun recordFileChanges() {
        changedFiles =
            fileToTimestampMap
                .filter { (file, previousTimestamp) ->
                    file.exists() && file.lastModified() != previousTimestamp
                }
                .keys

        unchangedFiles =
            fileToTimestampMap
                .filter { (file, previousTimestamp) ->
                    file.exists() && file.lastModified() == previousTimestamp
                }
                .keys

        deletedFiles = fileToTimestampMap.filter { (file, _) -> !file.exists() }.keys
    }

    private fun assertFilesExist(vararg files: File) {
        expect
            .withMessage("Existing files")
            .that(files.filter { it.exists() })
            .containsExactlyElementsIn(files)
    }

    private fun assertChangedFiles(vararg files: File) {
        expect.withMessage("Changed files").that(changedFiles).containsAtLeastElementsIn(files)
    }

    private fun assertUnchangedFiles(vararg files: File) {
        expect.withMessage("Unchanged files").that(unchangedFiles).containsAtLeastElementsIn(files)
    }

    private fun assertDeletedFiles(vararg files: File) {
        expect.withMessage("Deleted files").that(deletedFiles).containsAtLeastElementsIn(files)
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
        assertFilesExist(genDatabase1, genDao1, genDatabase2, genDao2, genSchema1, genSchema2)

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
            classGenDao2,
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
            @androidx.room3.ColumnInfo(name = "name")
            private String mName = "";

            public String getName() {
                return mName;
            }

            public void setName(String name) {
                mName = name;
            }
            """
                .trimIndent(),
        )

        val result = runIncrementalBuild()
        expect.that(result.task(COMPILE_TASK)!!.outcome).isEqualTo(TaskOutcome.SUCCESS)

        // Check annotation processing outputs:
        //   - Relevant files should be re-generated
        //   - Irrelevant files should not be re-generated (if Room is incremental)
        assertChangedFiles(genDatabase1, genDao1, genSchema1)
        assertUnchangedFiles(genDatabase2, genDao2, genSchema2)

        // Check compilation outputs:
        //   - Relevant files should be recompiled
        //   - Irrelevant files should not be recompiled (if Room is incremental)
        assertChangedFiles(
            classSrcDao1,
            classSrcDatabase1,
            classSrcEntity1,
            classGenDatabase1,
            classGenDao1,
        )
        assertUnchangedFiles(
            classSrcDatabase2,
            classSrcDao2,
            classSrcEntity2,
            classGenDatabase2,
            classGenDao2,
        )
    }

    @Test
    fun `delete group of source files`() {
        runFullBuild()

        // Delete the first group of source files
        Files.delete(srcDatabase1.toPath())
        Files.delete(srcDao1.toPath())
        Files.delete(srcEntity1.toPath())

        val result = runIncrementalBuild()
        // Gradle detects that nothing needs to be recompiled so it reports as UP-TO-DATE
        expect.that(result.task(COMPILE_TASK)!!.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)

        // Check annotation processing outputs:
        //   - Relevant files should be re-generated (or deleted)
        //   - Irrelevant files should not be re-generated (if Room is incremental)
        assertDeletedFiles(genDatabase1, genDao1)
        assertUnchangedFiles(
            // EXPECTATION-NOT-MET: Schema file 1 should be deleted but is not
            // (https://issuetracker.google.com/134472065).
            genSchema1,
            genDatabase2,
            genDao2,
            genSchema2,
        )

        // Check compilation outputs:
        //   - Relevant files should be recompiled (or deleted)
        //   - Irrelevant files should not be recompiled (if Room is incremental)
        assertDeletedFiles(
            classSrcDatabase1,
            classSrcDao1,
            classSrcEntity1,
            classGenDatabase1,
            classGenDao1,
        )
        assertUnchangedFiles(
            classSrcDatabase2,
            classSrcDao2,
            classSrcEntity2,
            // EXPECTATION-NOT-MET: Generated classes shouldn't need to be recompiled
            // classGenDatabase2,
            // classGenDao2,
        )
    }
}
