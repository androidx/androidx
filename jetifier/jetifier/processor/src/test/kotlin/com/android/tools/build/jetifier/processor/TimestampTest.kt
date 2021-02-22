/*
 * Copyright 2020 The Android Open Source Project
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

package com.android.tools.build.jetifier.processor

import com.android.tools.build.jetifier.core.config.Config
import com.android.tools.build.jetifier.core.rule.RewriteRulesMap
import com.android.tools.build.jetifier.core.type.JavaType
import com.android.tools.build.jetifier.core.type.TypesMap
import com.android.tools.build.jetifier.processor.archive.Archive
import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.util.zip.ZipInputStream

/**
 * Tests that transformed artifacts are properly marked as changed / unchanged base on whether there
 * was something to rewrite or not.
 */
class TimestampTest {

    private val prefRewriteConfig = Config.fromOptional(
        restrictToPackagePrefixes = setOf("android/support/v7/preference"),
        rulesMap = RewriteRulesMap(),
        slRules = listOf(),
        pomRewriteRules = setOf(),
        typesMap = TypesMap(
            JavaType("android/support/v7/preference/Preference")
                to JavaType("android/test/pref/Preference")
        )
    )

    @Test
    fun expectNowTimestamps() {
        val times = rewriteArchiveAndRetrieveTimestamps(
            prefRewriteConfig,
            TimestampsPolicy.NOW
        )

        assertThat(times).isNotEmpty()
        val nowSinceEpochInMs = Instant.now().toEpochMilli()
        times.forEach {
            val diff = nowSinceEpochInMs - it.toMillis()
            assertThat(diff).isAtMost(10_000) // 10 sec diff
        }
    }

    @Test
    fun expectEpochTimestamps() {
        val times = rewriteArchiveAndRetrieveTimestamps(
            prefRewriteConfig,
            TimestampsPolicy.EPOCH
        )

        assertThat(times).isNotEmpty()
        times.forEach {
            assertThat(it.toMillis()).isEqualTo(Instant.EPOCH.toEpochMilli())
        }
    }

    @Test
    fun keepPreviousTimestamps() {
        val fakePreviousTime = Instant.ofEpochSecond(1_000_000)

        val times = rewriteArchiveAndRetrieveTimestamps(
            prefRewriteConfig,
            TimestampsPolicy.KEEP_PREVIOUS, FileTime.from(fakePreviousTime)
        )

        assertThat(times).isNotEmpty()
        times.forEach {
            assertThat(it.toMillis()).isEqualTo(fakePreviousTime.toEpochMilli())
        }
    }

    @Test
    fun determinismTest_keepPreviousTimestamps() {
        val sourceArchive = createSourceArchive()

        val firstArchive = processArchive(
            prefRewriteConfig, sourceArchive,
            TimestampsPolicy.KEEP_PREVIOUS
        )

        // We need a delay to have a time diff > 1 sec in-between the two archives
        Thread.sleep(1000)

        val secondArchive = processArchive(
            prefRewriteConfig, sourceArchive,
            TimestampsPolicy.KEEP_PREVIOUS
        )

        assertThat(firstArchive.toMd5()).isEqualTo(secondArchive.toMd5())
    }

    @Test
    fun determinismTest_epochTimestamps() {
        val sourceArchive = createSourceArchive()

        val firstArchive = processArchive(
            prefRewriteConfig, sourceArchive,
            TimestampsPolicy.EPOCH
        )

        // We need a delay to have a time diff > 1 sec in-between the two archives
        Thread.sleep(1000)

        val secondArchive = processArchive(
            prefRewriteConfig, sourceArchive,
            TimestampsPolicy.EPOCH
        )

        assertThat(firstArchive.toMd5()).isEqualTo(secondArchive.toMd5())
    }

    @Test
    fun determinismTest_nowTimestamps_shouldNotMatch() {
        val sourceArchive = createSourceArchive()

        val firstArchive = processArchive(
            prefRewriteConfig, sourceArchive,
            TimestampsPolicy.NOW
        )

        // We need a delay to have a time diff > 1 sec in-between the two archives
        Thread.sleep(1000)

        val secondArchive = processArchive(
            prefRewriteConfig, sourceArchive,
            TimestampsPolicy.NOW
        )

        assertThat(firstArchive.toMd5()).isNotEqualTo(secondArchive.toMd5())
    }

    private fun rewriteArchiveAndRetrieveTimestamps(
        config: Config,
        timestampsPolicy: TimestampsPolicy,
        customTimeToPreset: FileTime? = null
    ): List<FileTime> {
        val sourceArchive = createSourceArchive(customTimeToPreset)
        val expectedFile = processArchive(config, sourceArchive, timestampsPolicy)
        return collectModifiedTimesForAllFiles(expectedFile)
    }

    private fun createSourceArchive(
        customTimeToPreset: FileTime? = null
    ): File {
        val fileName = "test.xml"
        val fileContent = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<android.support.v7.preference.Preference/>"

        val files = listOf(
            ArchiveFile(Paths.get("/", fileName), fileContent.toByteArray(), customTimeToPreset),
            ArchiveFile(Paths.get("/hello/empty.xml"), "".toByteArray(), customTimeToPreset)
        )

        val archive = Archive(Paths.get("some/path"), files)
        return archive.writeSelfToFile(
            Files.createTempFile("test", ".zip"),
            TimestampsPolicy.KEEP_PREVIOUS
        )
    }

    private fun processArchive(
        config: Config,
        sourceArchive: File,
        timestampsPolicy: TimestampsPolicy
    ): File {
        val expectedFile = Files.createTempFile("testRefactored", ".zip")
        val processor = Processor.createProcessor4(
            config = config,
            timestampsPolicy = timestampsPolicy
        )
        processor.transform2(
            input = setOf(FileMapping(sourceArchive, expectedFile.toFile())),
            copyUnmodifiedLibsAlso = false,
            skipLibsWithAndroidXReferences = false
        )

        return expectedFile.toFile()
    }

    private fun collectModifiedTimesForAllFiles(file: File): List<FileTime> {
        val timestamps = mutableListOf<FileTime>()
        FileInputStream(file).use {
            val zipIn = ZipInputStream(it)
            var entry = zipIn.nextEntry
            while (entry != null) {
                timestamps.add(entry.lastModifiedTime)
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
        return timestamps
    }
}