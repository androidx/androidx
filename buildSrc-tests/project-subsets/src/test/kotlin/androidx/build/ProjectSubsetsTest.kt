/*
 * Copyright 2021 The Android Open Source Project
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

import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

/**
 * This class tests that each of the project subsets defined in settings.gradle can be built
 * successfully and does not involve a project that attempts to reference
 * another project defined only in a different subset, b/172277767 .
 *
 * This is implemented using the Gradle TestKit so it can be
 * run in parallel with other tasks, b/180012150 .
 */
public class ProjectSubsetsTest {
    @Test
    fun testSubsetMain() {
        validateSubset("main")
    }

    @Test
    fun testSubsetCamera() {
        validateSubset("camera")
    }

    @Test
    fun testSubsetCompose() {
        validateSubset("compose")
    }

    @Test
    fun testSubsetFlan() {
        validateSubset("flan")
    }

    @Test
    fun testSubsetMedia() {
        validateSubset("media")
    }

    @Test
    fun testSubsetWear() {
        validateSubset("wear")
    }

    @Test
    fun testSubsetGlance() {
        validateSubset("glance")
    }

    @Test
    fun testSubsetTools() {
        validateSubset("tools")
    }

    @Test
    fun testSubsetKmp() {
        validateSubset("kmp")
    }

    @Test
    fun testSubsetNative() {
        validateSubset("native")
    }

    @Test
    fun testSubsetWindow() {
        validateSubset("window")
    }
    /**
     * Validates a specific project subset
     */
    fun validateSubset(name: String) {
        val projectDir = File("../..").normalize()
        var outDir = System.getenv("OUT_DIR")
        if (outDir == null || outDir == "") {
            outDir = File(projectDir, "../../out").normalize().toString()
        }
        // --dependency-verification=off is set because we don't have to do validation of
        // dependencies during these tests, it is already handled by the main build.
        // Having it validate here breaks in androidx-studio-integration case where we
        // might get new dependencies from AGP that are missinng signatures.

        // Run buildOnServer to validate project dependencies and constraints
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(
                "-Pandroidx.projects=$name", "buildOnServer", "-m", "--dependency-verification=off"
            )
            .withTestKitDir(File(outDir, ".gradle-testkit"))
            .build(); // fails the test if the build fails
    }
}
