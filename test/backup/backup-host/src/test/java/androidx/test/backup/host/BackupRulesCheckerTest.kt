/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup.host

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BackupRulesCheckerTest {

    @get:Rule val tempFolder = TemporaryFolder()

    @Test
    fun mapsStorageDomainsAndActionArgsToBackupRuleCoordinates() {
        assertEquals(
            BackupRulesChecker.TargetCoordinate("sharedpref", "user_prefs.xml"),
            BackupRulesChecker.targetCoordinateOf(StorageDomain.Preference("user_prefs", "k", "v")),
        )
        assertEquals(
            BackupRulesChecker.TargetCoordinate("device_sharedpref", "de_prefs.xml"),
            BackupRulesChecker.targetCoordinateOf(
                StorageDomain.Preference("de_prefs", "k", "v", isDeviceProtectedStorage = true)
            ),
        )
        assertEquals(
            BackupRulesChecker.TargetCoordinate("file", "datastore/settings.preferences_pb"),
            BackupRulesChecker.targetCoordinateOf(
                StorageDomain.DataStorePreference("settings", "k", "v")
            ),
        )
        assertEquals(
            BackupRulesChecker.TargetCoordinate("database", "app.db"),
            BackupRulesChecker.targetCoordinateOf(
                StorageDomain.Database("app.db", "t", "id", 1, mapOf("id" to 1))
            ),
        )
        assertEquals(
            BackupRulesChecker.TargetCoordinate("file", "notes/todo.txt"),
            BackupRulesChecker.targetCoordinateOf(StorageDomain.TextFile("notes/todo.txt", "hi")),
        )
        assertNull(
            BackupRulesChecker.targetCoordinateOf(
                StorageDomain.ExcludedData(StorageDomain.Preference("skipped", "k", "v"))
            )
        )
        assertEquals(
            BackupRulesChecker.TargetCoordinate("sharedpref", "custom.xml"),
            BackupRulesChecker.targetCoordinateFromArgs(
                mapOf("storage_type" to "prefs", "pref_name" to "custom")
            ),
        )
        assertEquals(
            BackupRulesChecker.TargetCoordinate("sharedpref", "default_prefs.xml"),
            BackupRulesChecker.targetCoordinateFromArgs(mapOf("pref_key" to "only_key")),
        )
        assertNull(
            BackupRulesChecker.targetCoordinateFromArgs(
                mapOf("storage_type" to "files", "path" to "/data/local/tmp/outside.txt")
            )
        )
    }

    @Test
    fun loadsSuiteEnvironmentAndParsesFullBackupAndDataExtractionSections() {
        val resultsDir = tempFolder.newFolder("results")
        writeSuiteEnvironmentJson(
            resultsDir = resultsDir,
            allowBackup = true,
            targetSdkVersion = 34,
            fullBackupContentRef = "@xml/backup_rules",
            fullBackupContentXml =
                """
                <full-backup-content>
                    <include domain="sharedpref" path="vault.xml" requireFlags="clientSideEncryption" />
                    <exclude domain="database" path="cache.db" />
                </full-backup-content>
                """
                    .trimIndent(),
            dataExtractionRulesRef = "@xml/data_extraction_rules",
            dataExtractionRulesXml =
                """
                <data-extraction-rules>
                    <cloud-backup disableIfNoEncryptionCapabilities="true">
                        <exclude domain="sharedpref" path="session.xml" />
                    </cloud-backup>
                </data-extraction-rules>
                """
                    .trimIndent(),
        )
        val config = BackupRulesChecker.loadFromSuiteEnvironment(resultsDir.absolutePath)
        assertNotNull(config)
        assertTrue(config.allowBackup)
        assertEquals(34, config.targetSdkVersion)
        assertEquals("backup_rules.xml", config.fullBackupContentFile)
        assertEquals("data_extraction_rules.xml", config.dataExtractionRulesFile)

        val fullSection =
            BackupRulesChecker.parseFullBackupSection(
                assertNotNull(config.fullBackupContentDoc),
                "backup_rules.xml",
            )
        assertNotNull(fullSection)
        assertEquals(1, fullSection.includes.size)
        assertEquals(setOf("clientSideEncryption"), fullSection.includes.single().requireFlags)
        assertEquals(1, fullSection.excludes.size)

        val cloudSection =
            BackupRulesChecker.parseDataExtractionSection(
                assertNotNull(config.dataExtractionRulesDoc),
                "data_extraction_rules.xml",
                "cloud-backup",
            )
        assertNotNull(cloudSection)
        assertTrue(cloudSection.disableIfNoEncryption)
        assertEquals(1, cloudSection.excludes.size)
    }

    @Test
    fun rejectsUnsafeXmlFileReferences() {
        assertEquals(
            "data_extraction_rules.xml",
            BackupRulesChecker.xmlFileNameFromRef("@xml/data_extraction_rules"),
        )
        for (maliciousRef in
            listOf(
                "@xml/../outside",
                "../outside.xml",
                "@xml/sub/outside",
                "@xml/sub\\outside",
                "sub/outside.xml",
            )) {
            assertNull(BackupRulesChecker.xmlFileNameFromRef(maliciousRef))
        }
    }
}

/** Writes a `suite-environment.json` file into [resultsDir] for tests. */
internal fun writeSuiteEnvironmentJson(
    resultsDir: File,
    allowBackup: Boolean = true,
    targetSdkVersion: Int? = null,
    fullBackupContentRef: String? = null,
    fullBackupContentXml: String? = null,
    dataExtractionRulesRef: String? = null,
    dataExtractionRulesXml: String? = null,
) {
    val json = buildJsonObject {
        put("schemaVersion", 1)
        put("targetSdkVersion", targetSdkVersion)
        putJsonObject("backupRules") {
            put("allowBackup", allowBackup)
            put("fullBackupContent", fullBackupContentRef)
            put(
                "fullBackupContentFile",
                fullBackupContentRef?.let(BackupRulesChecker::xmlFileNameFromRef),
            )
            put("fullBackupContentXml", fullBackupContentXml)
            put("dataExtractionRules", dataExtractionRulesRef)
            put(
                "dataExtractionRulesFile",
                dataExtractionRulesRef?.let(BackupRulesChecker::xmlFileNameFromRef),
            )
            put("dataExtractionRulesXml", dataExtractionRulesXml)
        }
    }
    File(resultsDir, "suite-environment.json").writeText(json.toString())
}
