/*
 * Copyright 2017 The Android Open Source Project
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

package com.android.tools.build.jetifier.processor.transform.bytecode

import com.android.tools.build.jetifier.core.config.Config
import com.android.tools.build.jetifier.core.rule.RewriteRule
import com.android.tools.build.jetifier.core.rule.RewriteRulesMap
import com.android.tools.build.jetifier.core.type.JavaType
import com.android.tools.build.jetifier.core.type.TypesMap
import com.android.tools.build.jetifier.processor.FileMapping
import com.android.tools.build.jetifier.processor.Processor
import com.android.tools.build.jetifier.processor.archive.Archive
import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.android.tools.build.jetifier.processor.archive.ArchiveItemVisitor
import com.google.common.truth.Truth
import org.junit.Test
import java.io.File

/**
 * Tests that individual files were moved properly due to their owner types rewrites.
 */
class ClassFilesMoveTest {

    companion object {
        private val TEST_CONFIG = Config.fromOptional(
            restrictToPackagePrefixes = setOf("android/support"),
            reversedRestrictToPackagesPrefixes = setOf("androidx"),
            rulesMap = RewriteRulesMap(
                RewriteRule("android/support/annotation/(.*)", "ignore"),
                RewriteRule("android/support/v7/preference/R(.*)", "ignore"),
                RewriteRule("android/support/v4/(.*)", "ignore")
            ),
            slRules = listOf(
                RewriteRule("android/support/annotation/(.*)", "ignore"),
                RewriteRule("android/support/v7/preference/R(.*)", "ignore"),
                RewriteRule("android/support/v4/(.*)", "ignore")
            ),
            typesMap = TypesMap(mapOf(
                "android/support/v7/preference/Preference"
                        to "androidx/support/preference/Preference",
                "android/support/v7/preference/TwoStatePreference"
                        to "androidx/support/preference/TwoStatePreference",
                "android/support/v7/preference/PreferenceGroup"
                        to "androidx/support/preference/PreferenceGroup",
                "android/support/v7/preference/PreferenceViewHolder"
                        to "androidx/support/preference/PreferenceViewHolder",
                "android/support/v7/preference/PreferenceManager"
                        to "androidx/support/preference/PreferenceManager",
                "android/support/v14/preference/SwitchPreference"
                        to "androidx/support/preference/SwitchPreference",
                "android/support/v7/preference/PreferenceDataStore"
                        to "androidx/support/preference/PreferenceDataStore"
            ).map { JavaType(it.key) to JavaType(it.value) }.toMap())
        )
    }

    /**
     * Tests that after rewrite of a input archive the internal classes are properly moved to new
     * locations (based on the rewrite rules) which is compared with the expected archive.
     *
     * Note: The expected archive does not contain rewritten classes - they were only manually
     * moved. Which is fine because this test validates only files locations.
     */
    @Test fun fileMove_forwardRewrite_shouldMoveFilesProperly() {
        val inputZipPath = "/fileRenameTest/inputTestLib.zip"
        val expectedZipPath = "/fileRenameTest/expectedTestLib.zip"

        val processor = Processor.createProcessor(TEST_CONFIG)
        val inputFile = File(javaClass.getResource(inputZipPath).file)

        val tempDir = createTempDir()
        val expectedFile = File(createTempDir(), "test.zip")

        val resultFiles = processor.transform(setOf(FileMapping(inputFile, expectedFile)))

        Truth.assertThat(resultFiles).hasSize(1)
        testArchivesAreSame(resultFiles.first(), File(javaClass.getResource(expectedZipPath).file))

        tempDir.delete()
    }

    /**
     * Does exactly the same as [fileMove_forwardRewrite_nestedArchive_shouldMoveFilesProperly] but
     * the files are in a nested archive e.g. archive.zip/classes.jar/some files.
     */
    @Test fun fileMove_forwardRewrite_nestedArchive_shouldMoveFilesProperly() {
        val inputZipPath = "/fileRenameTest/inputTestLibNested.zip"
        val expectedZipPath = "/fileRenameTest/expectedTestLibNested.zip"

        val processor = Processor.createProcessor(TEST_CONFIG)
        val inputFile = File(javaClass.getResource(inputZipPath).file)

        val tempDir = createTempDir()
        val expectedFile = File(createTempDir(), "test.zip")

        val resultFiles = processor.transform(setOf(FileMapping(inputFile, expectedFile)))

        Truth.assertThat(resultFiles).hasSize(1)
        testArchivesAreSame(resultFiles.first(), File(javaClass.getResource(expectedZipPath).file))

        tempDir.delete()
    }

    /**
     * Rewrites the input archive and then applies reversed mode to rewrite it back. The final
     * produced archive has to have the same directory structure as the input one.
     */
    @Test fun fileMove_forwardRewrite_backwardsRewrite_shouldKeepFilesProperly() {
        val inputZipPath = "/fileRenameTest/inputTestLib.zip"

        // Transform forward
        val processor = Processor.createProcessor(TEST_CONFIG)
        val inputFile = File(javaClass.getResource(inputZipPath).file)

        val tempDir = createTempDir()
        val expectedFile = File(createTempDir(), "test.zip")

        val resultFiles = processor.transform(setOf(FileMapping(inputFile, expectedFile)))

        // Take previous result & reverse it
        val processor2 = Processor.createProcessor(
            TEST_CONFIG,
            rewritingSupportLib = true,
            reversedMode = true)
        val expectedFile2 = File(createTempDir(), "test2.zip")
        val resultFiles2 = processor2.transform(setOf(
            FileMapping(resultFiles.first(), expectedFile2)))

        testArchivesAreSame(resultFiles2.first(), File(javaClass.getResource(inputZipPath).file))

        tempDir.delete()
    }

    fun testArchivesAreSame(givenZip: File, expectedZip: File) {
        testArchivesAreSame(Archive.Builder.extract(givenZip), Archive.Builder.extract(expectedZip))
    }

    fun testArchivesAreSame(givenZip: Archive, expectedZip: Archive) {
        val givenFiles = ArchiveBrowser.grabAllPathsIn(givenZip)
        val expectedFiles = ArchiveBrowser.grabAllPathsIn(expectedZip)
        Truth.assertThat(givenFiles).containsExactlyElementsIn(expectedFiles)
    }

    /**
     * Just a helper utility to get all file paths in the archive.
     */
    class ArchiveBrowser : ArchiveItemVisitor {

        companion object {
            fun grabAllPathsIn(archive: Archive): MutableSet<String> {
                val grabber = ArchiveBrowser()
                archive.accept(grabber)
                return grabber.allPaths
            }
        }

        val allPaths = mutableSetOf<String>()

        override fun visit(archiveFile: ArchiveFile) {
            allPaths.add(archiveFile.relativePath.toString())
            println("Visited ${archiveFile.relativePath}")
        }

        override fun visit(archive: Archive) {
            archive.files.forEach { it.accept(this) }
        }
    }
}
