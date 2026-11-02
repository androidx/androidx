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
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BackupRulesCheckerTest {

    @get:Rule val tempFolder = TemporaryFolder()

    @Test
    fun reportsAllowBackupFalseOnCloudLocalAndApi30DeviceToDevice() {
        val resultsDir = writeSuiteEnvironment(allowBackup = false, targetSdkVersion = 34)
        val pref = StorageDomain.Preference("user_prefs", "k", "v")

        for (mode in listOf(BackupTransportMode.CLOUD_ENCRYPTED, BackupTransportMode.LOCAL)) {
            assertEquals(
                "AndroidManifest.xml sets android:allowBackup=\"false\", which disables app data " +
                    "backup and restore.",
                BackupRulesChecker.diagnoseLikelyCause(
                    resultsDirPath = resultsDir.absolutePath,
                    transportMode = mode,
                    storages = listOf(pref),
                    actionTargets = emptyList(),
                    failureMessage = "Backup contains no app data",
                ),
            )
        }

        val api30Dir = writeSuiteEnvironment(allowBackup = false, targetSdkVersion = 30)
        assertEquals(
            "AndroidManifest.xml sets android:allowBackup=\"false\", which disables app data " +
                "backup and restore.",
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = api30Dir.absolutePath,
                transportMode = BackupTransportMode.DEVICE_TO_DEVICE,
                storages = listOf(pref),
                actionTargets = emptyList(),
                failureMessage = "missing key",
            ),
        )
    }

    @Test
    fun reportsAllowBackupFalseOnD2DWhenBackupFailsWithoutAppDataOtherwiseChecksDeviceTransfer() {
        val resultsDir =
            writeSuiteEnvironment(
                allowBackup = false,
                targetSdkVersion = 34,
                dataExtractionRulesRef = "@xml/data_extraction_rules",
                dataExtractionRulesXml =
                    """
                    <data-extraction-rules>
                        <device-transfer>
                            <exclude domain="sharedpref" path="user_session.xml" />
                        </device-transfer>
                    </data-extraction-rules>
                    """
                        .trimIndent(),
            )
        val sessionPref = StorageDomain.Preference("user_session", "auth_token", "secret")

        // BackupService rejects allowBackup="false" during D2D backup with WithoutAppData.
        assertEquals(
            "AndroidManifest.xml sets android:allowBackup=\"false\". Device-to-device test " +
                "backups require android:allowBackup=\"true\".",
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = resultsDir.absolutePath,
                transportMode = BackupTransportMode.DEVICE_TO_DEVICE,
                storages = listOf(sessionPref),
                actionTargets = emptyList(),
                failureMessage =
                    "Backup (DEVICE_TO_DEVICE) of com.example.app contains no app data",
            ),
        )
        assertEquals(
            "AndroidManifest.xml sets android:allowBackup=\"false\". Device-to-device test " +
                "backups require android:allowBackup=\"true\".",
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = resultsDir.absolutePath,
                transportMode = BackupTransportMode.DEVICE_TO_DEVICE,
                storages = listOf(sessionPref),
                actionTargets = emptyList(),
                failureMessage =
                    "No data was generated in backup since allowBackup property is false",
            ),
        )
        assertEquals(
            "AndroidManifest.xml sets android:allowBackup=\"false\". Device-to-device test " +
                "backups require android:allowBackup=\"true\".",
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = resultsDir.absolutePath,
                transportMode = BackupTransportMode.DEVICE_TO_DEVICE,
                storages = listOf(sessionPref),
                actionTargets = emptyList(),
                failureMessage = "Restore of com.example.app from backup.zip restored no app data",
            ),
        )
        // Non-allowBackup GMS skip reason on restore falls through to <device-transfer>.
        assertEquals(
            "data_extraction_rules.xml excludes domain=\"sharedpref\" path=\"user_session.xml\" " +
                "for <device-transfer>.",
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = resultsDir.absolutePath,
                transportMode = BackupTransportMode.DEVICE_TO_DEVICE,
                storages = listOf(sessionPref),
                actionTargets = emptyList(),
                failureMessage =
                    "Restore of com.example.app from backup.zip restored no app data: INELIGIBLE_FIRST_PARTY",
            ),
        )

        // Post-restore failure skips allowBackup="false" on API 31+ D2D and inspects
        // <device-transfer> rules instead.
        assertEquals(
            "data_extraction_rules.xml excludes domain=\"sharedpref\" path=\"user_session.xml\" " +
                "for <device-transfer>.",
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = resultsDir.absolutePath,
                transportMode = BackupTransportMode.DEVICE_TO_DEVICE,
                storages = listOf(sessionPref),
                actionTargets = emptyList(),
                failureMessage = "expected auth_token to be restored",
            ),
        )

        val unblockedDir = writeSuiteEnvironment(allowBackup = false, targetSdkVersion = 34)
        assertNull(
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = unblockedDir.absolutePath,
                transportMode = BackupTransportMode.DEVICE_TO_DEVICE,
                storages = listOf(sessionPref),
                actionTargets = emptyList(),
                failureMessage = "missing key",
            )
        )
    }

    @Test
    fun matchesDeviceTransferSectionForD2DAndCloudBackupSectionForCloud() {
        val resultsDir =
            writeSuiteEnvironment(
                dataExtractionRulesRef = "@xml/data_extraction_rules",
                dataExtractionRulesXml =
                    """
                    <data-extraction-rules>
                        <cloud-backup>
                            <exclude domain="database" path="analytics.db" />
                        </cloud-backup>
                        <device-transfer>
                            <exclude domain="sharedpref" path="user_session.xml" />
                        </device-transfer>
                    </data-extraction-rules>
                    """
                        .trimIndent(),
            )
        val sessionPref = StorageDomain.Preference("user_session", "auth_token", "secret")

        assertEquals(
            "data_extraction_rules.xml excludes domain=\"sharedpref\" path=\"user_session.xml\" " +
                "for <device-transfer>.",
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = resultsDir.absolutePath,
                transportMode = BackupTransportMode.DEVICE_TO_DEVICE,
                storages = listOf(sessionPref),
                actionTargets = emptyList(),
                failureMessage = "expected auth_token to be restored",
            ),
        )

        assertNull(
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = resultsDir.absolutePath,
                transportMode = BackupTransportMode.CLOUD_ENCRYPTED,
                storages = listOf(sessionPref),
                actionTargets = emptyList(),
                failureMessage = "expected auth_token to be restored",
            )
        )
    }

    @Test
    fun mergesMultipleSectionsAndFallsThroughToFullBackupContentWhenSectionAbsent() {
        val resultsDir =
            writeSuiteEnvironment(
                dataExtractionRulesRef = "@xml/data_extraction_rules",
                dataExtractionRulesXml =
                    """
                    <data-extraction-rules>
                        <device-transfer>
                            <exclude domain="database" path="first.db" />
                        </device-transfer>
                        <device-transfer>
                            <exclude domain="sharedpref" path="second.xml" />
                        </device-transfer>
                    </data-extraction-rules>
                    """
                        .trimIndent(),
                fullBackupContentRef = "@xml/backup_rules",
                fullBackupContentXml =
                    """
                    <full-backup-content>
                        <exclude domain="sharedpref" path="cloud_legacy.xml" />
                    </full-backup-content>
                    """
                        .trimIndent(),
            )

        // Second <device-transfer> tag is merged rather than ignored.
        assertEquals(
            "data_extraction_rules.xml excludes domain=\"sharedpref\" path=\"second.xml\" " +
                "for <device-transfer>.",
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = resultsDir.absolutePath,
                transportMode = BackupTransportMode.DEVICE_TO_DEVICE,
                storages = listOf(StorageDomain.Preference("second", "k", "v")),
                actionTargets = emptyList(),
                failureMessage = "missing key",
            ),
        )

        // Missing <cloud-backup> section falls through to <full-backup-content>.
        assertEquals(
            "backup_rules.xml excludes domain=\"sharedpref\" path=\"cloud_legacy.xml\" " +
                "in <full-backup-content>.",
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = resultsDir.absolutePath,
                transportMode = BackupTransportMode.CLOUD_ENCRYPTED,
                storages = listOf(StorageDomain.Preference("cloud_legacy", "k", "v")),
                actionTargets = emptyList(),
                failureMessage = "missing key",
            ),
        )
    }

    @Test
    fun skipsFullBackupContentOnApi31PlusDeviceToDevice() {
        val disabledDir =
            writeSuiteEnvironment(targetSdkVersion = 34, fullBackupContentRef = "false")
        assertNull(
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = disabledDir.absolutePath,
                transportMode = BackupTransportMode.DEVICE_TO_DEVICE,
                storages = listOf(StorageDomain.Preference("prefs", "k", "v")),
                actionTargets = emptyList(),
                failureMessage = "missing key",
            )
        )
        assertContains(
            BackupRulesChecker.diagnoseLikelyCause(
                    resultsDirPath = disabledDir.absolutePath,
                    transportMode = BackupTransportMode.CLOUD_ENCRYPTED,
                    storages = listOf(StorageDomain.Preference("prefs", "k", "v")),
                    actionTargets = emptyList(),
                    failureMessage = "missing key",
                )
                .orEmpty(),
            "fullBackupContent=\"false\"",
        )

        val xmlDir =
            writeSuiteEnvironment(
                targetSdkVersion = 34,
                fullBackupContentRef = "@xml/backup_rules",
                fullBackupContentXml =
                    "<full-backup-content><exclude domain=\"sharedpref\" path=\"prefs.xml\"/></full-backup-content>",
            )
        assertNull(
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = xmlDir.absolutePath,
                transportMode = BackupTransportMode.DEVICE_TO_DEVICE,
                storages = listOf(StorageDomain.Preference("prefs", "k", "v")),
                actionTargets = emptyList(),
                failureMessage = "missing key",
            )
        )
    }

    @Test
    fun checksFullBackupContentEncryptionFlagsButIgnoresPerRuleFlagsInDataExtractionRules() {
        val fbcDir =
            writeSuiteEnvironment(
                fullBackupContentRef = "@xml/backup_rules",
                fullBackupContentXml =
                    """
                    <full-backup-content>
                        <include domain="sharedpref" path="vault.xml" requireFlags="clientSideEncryption" />
                        <exclude domain="file" path="datastore/cache.preferences_pb" />
                    </full-backup-content>
                    """
                        .trimIndent(),
            )

        assertEquals(
            "backup_rules.xml excludes domain=\"file\" " +
                "path=\"datastore/cache.preferences_pb\" in <full-backup-content>.",
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = fbcDir.absolutePath,
                transportMode = BackupTransportMode.CLOUD_ENCRYPTED,
                storages = listOf(StorageDomain.DataStorePreference("cache", "token", "123")),
                actionTargets = emptyList(),
                failureMessage = "missing key",
            ),
        )

        assertContains(
            BackupRulesChecker.diagnoseLikelyCause(
                    resultsDirPath = fbcDir.absolutePath,
                    transportMode = BackupTransportMode.CLOUD_UNENCRYPTED,
                    storages = listOf(StorageDomain.Preference("vault", "pin", "9999")),
                    actionTargets = emptyList(),
                    failureMessage = "missing key",
                )
                .orEmpty(),
            "requireFlags=\"clientSideEncryption\"",
        )

        val omittedFromIncludeCause =
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = fbcDir.absolutePath,
                transportMode = BackupTransportMode.CLOUD_ENCRYPTED,
                storages = listOf(StorageDomain.TextFile("notes/todo.txt", "hello")),
                actionTargets = emptyList(),
                failureMessage = "missing file",
            )
        assertContains(omittedFromIncludeCause.orEmpty(), "uses <include> rules")
        assertContains(omittedFromIncludeCause.orEmpty(), "notes/todo.txt")

        // Per-rule requireFlags inside <data-extraction-rules> is ignored by Android.
        val derDir =
            writeSuiteEnvironment(
                dataExtractionRulesRef = "@xml/data_extraction_rules",
                dataExtractionRulesXml =
                    """
                    <data-extraction-rules>
                        <cloud-backup>
                            <include domain="sharedpref" path="vault.xml" requireFlags="clientSideEncryption" />
                        </cloud-backup>
                    </data-extraction-rules>
                    """
                        .trimIndent(),
            )
        assertNull(
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = derDir.absolutePath,
                transportMode = BackupTransportMode.CLOUD_UNENCRYPTED,
                storages = listOf(StorageDomain.Preference("vault", "pin", "9999")),
                actionTargets = emptyList(),
                failureMessage = "missing key",
            )
        )
    }

    @Test
    fun emptyTargetsOnlyDiagnoseHintedExcludesOrAllFlagBlockedIncludes() {
        val derDir =
            writeSuiteEnvironment(
                dataExtractionRulesRef = "@xml/data_extraction_rules",
                dataExtractionRulesXml =
                    """
                    <data-extraction-rules>
                        <device-transfer>
                            <include domain="sharedpref" path="allowed.xml" />
                            <exclude domain="database" path="tmp.db" />
                            <exclude domain="sharedpref" path="user_session.xml" />
                        </device-transfer>
                    </data-extraction-rules>
                    """
                        .trimIndent(),
            )

        // Hinted failureMessage mentioning "user_session" matches the exclude rule.
        assertEquals(
            "data_extraction_rules.xml excludes domain=\"sharedpref\" path=\"user_session.xml\" " +
                "for <device-transfer>.",
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = derDir.absolutePath,
                transportMode = BackupTransportMode.DEVICE_TO_DEVICE,
                storages = emptyList(),
                actionTargets = emptyList(),
                failureMessage = "user_session token was null on target",
            ),
        )

        // Unrelated failureMessage with empty targets must NOT guess excludes.first() or <include>.
        assertNull(
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = derDir.absolutePath,
                transportMode = BackupTransportMode.DEVICE_TO_DEVICE,
                storages = emptyList(),
                actionTargets = emptyList(),
                failureMessage = "expected account state to match",
            )
        )

        // When every <include> in <full-backup-content> is blocked by requireFlags, still report.
        val allBlockedDir =
            writeSuiteEnvironment(
                fullBackupContentRef = "@xml/backup_rules",
                fullBackupContentXml =
                    """
                    <full-backup-content>
                        <include domain="sharedpref" path="vault.xml" requireFlags="clientSideEncryption" />
                    </full-backup-content>
                    """
                        .trimIndent(),
            )
        assertContains(
            BackupRulesChecker.diagnoseLikelyCause(
                    resultsDirPath = allBlockedDir.absolutePath,
                    transportMode = BackupTransportMode.LOCAL,
                    storages = emptyList(),
                    actionTargets = emptyList(),
                    failureMessage = "missing state",
                )
                .orEmpty(),
            "requireFlags=\"clientSideEncryption\"",
        )
    }

    @Test
    fun matchesSharedPrefWithoutXmlSuffixAndUnconditionalStorageSuffixes() {
        val resultsDir =
            writeSuiteEnvironment(
                dataExtractionRulesRef = "@xml/data_extraction_rules",
                dataExtractionRulesXml =
                    """
                    <data-extraction-rules>
                        <cloud-backup>
                            <exclude domain="sharedpref" path="user_session" />
                            <exclude domain="sharedpref" path="foo.xml.xml" />
                            <exclude domain="file" path="datastore/bar.preferences_pb.preferences_pb" />
                        </cloud-backup>
                    </data-extraction-rules>
                    """
                        .trimIndent(),
            )

        // Rule path="user_session" (without .xml) matches Preference("user_session") ->
        // user_session.xml.
        assertEquals(
            "data_extraction_rules.xml excludes domain=\"sharedpref\" path=\"user_session\" " +
                "for <cloud-backup>.",
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = resultsDir.absolutePath,
                transportMode = BackupTransportMode.CLOUD_ENCRYPTED,
                storages = listOf(StorageDomain.Preference("user_session", "k", "v")),
                actionTargets = emptyList(),
                failureMessage = "missing key",
            ),
        )

        // Preference("foo.xml") and actionTargets prefName="foo.xml" both append ".xml"
        // unconditionally (foo.xml.xml), matching SharedPreferencesImpl on Android.
        assertContains(
            BackupRulesChecker.diagnoseLikelyCause(
                    resultsDirPath = resultsDir.absolutePath,
                    transportMode = BackupTransportMode.CLOUD_ENCRYPTED,
                    storages = listOf(StorageDomain.Preference("foo.xml", "k", "v")),
                    actionTargets = emptyList(),
                    failureMessage = "missing key",
                )
                .orEmpty(),
            "path=\"foo.xml.xml\"",
        )
        assertContains(
            BackupRulesChecker.diagnoseLikelyCause(
                    resultsDirPath = resultsDir.absolutePath,
                    transportMode = BackupTransportMode.CLOUD_ENCRYPTED,
                    storages = emptyList(),
                    actionTargets =
                        listOfNotNull(
                            BackupRulesChecker.targetCoordinateFromArgs(
                                mapOf(
                                    BackupActionInputKeys.STORAGE_TYPE to
                                        BackupActionValues.STORAGE_TYPE_PREFS,
                                    BackupActionInputKeys.PREF_NAME to "foo.xml",
                                )
                            )
                        ),
                    failureMessage = "missing key",
                )
                .orEmpty(),
            "path=\"foo.xml.xml\"",
        )

        // DataStorePreference("bar.preferences_pb") appends ".preferences_pb" unconditionally.
        assertContains(
            BackupRulesChecker.diagnoseLikelyCause(
                    resultsDirPath = resultsDir.absolutePath,
                    transportMode = BackupTransportMode.CLOUD_ENCRYPTED,
                    storages =
                        listOf(StorageDomain.DataStorePreference("bar.preferences_pb", "k", "v")),
                    actionTargets = emptyList(),
                    failureMessage = "missing key",
                )
                .orEmpty(),
            "path=\"datastore/bar.preferences_pb.preferences_pb\"",
        )
    }

    @Test
    fun ignoresExpectedExclusionOfExcludedDataDomain() {
        val resultsDir =
            writeSuiteEnvironment(
                dataExtractionRulesRef = "@xml/data_extraction_rules",
                dataExtractionRulesXml =
                    """
                    <data-extraction-rules>
                        <cloud-backup>
                            <exclude domain="sharedpref" path="ephemeral.xml" />
                        </cloud-backup>
                    </data-extraction-rules>
                    """
                        .trimIndent(),
            )

        assertNull(
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = resultsDir.absolutePath,
                transportMode = BackupTransportMode.CLOUD_ENCRYPTED,
                storages =
                    listOf(
                        StorageDomain.Preference("kept_prefs", "k", "v"),
                        StorageDomain.ExcludedData(StorageDomain.Preference("ephemeral", "t", "1")),
                    ),
                actionTargets = emptyList(),
                failureMessage = "kept_prefs failed",
            )
        )
    }

    @Test
    fun rejectsPathTraversalReferencesInXmlFileNameAndSuiteEnvironment() {
        assertEquals(
            "data_extraction_rules.xml",
            BackupRulesChecker.xmlFileNameFromRef("@xml/data_extraction_rules"),
        )
        val resultsDir = tempFolder.newFolder("traversal-results")
        val ruleXml =
            """
            <data-extraction-rules>
                <cloud-backup>
                    <exclude domain="sharedpref" path="user_prefs.xml" />
                </cloud-backup>
            </data-extraction-rules>
            """
                .trimIndent()

        for (maliciousRef in
            listOf(
                "@xml/../outside",
                "../outside.xml",
                "@xml/sub/outside",
                "@xml/sub\\outside",
                "sub/outside.xml",
            )) {
            assertNull(
                BackupRulesChecker.xmlFileNameFromRef(maliciousRef),
                "Expected xmlFileNameFromRef('$maliciousRef') to return null",
            )
            File(resultsDir, "suite-environment.json")
                .writeText(
                    buildJsonObject {
                        put("schemaVersion", 1)
                        putJsonObject("backupRules") {
                            put("allowBackup", true)
                            put("dataExtractionRules", maliciousRef)
                            put("dataExtractionRulesFile", maliciousRef)
                            put("dataExtractionRulesXml", ruleXml)
                        }
                    }
                        .toString()
                )
            val diagnosis =
                BackupRulesChecker.diagnoseLikelyCause(
                    resultsDirPath = resultsDir.absolutePath,
                    transportMode = BackupTransportMode.CLOUD_ENCRYPTED,
                    storages = listOf(StorageDomain.Preference("user_prefs", "k", "v")),
                    actionTargets = emptyList(),
                    failureMessage = "missing key",
                )
            assertEquals(
                "data_extraction_rules.xml excludes " +
                    "domain=\"sharedpref\" path=\"user_prefs.xml\" for <cloud-backup>.",
                diagnosis,
                "Expected unsafe file name '$maliciousRef' to be rejected in diagnosis",
            )
            // Also verify unsafe fullBackupContentFile is rejected and falls back to
            // fullBackupContentRef.
            File(resultsDir, "suite-environment.json")
                .writeText(
                    buildJsonObject {
                        put("schemaVersion", 1)
                        putJsonObject("backupRules") {
                            put("allowBackup", true)
                            put("fullBackupContent", "@xml/backup_rules")
                            put("fullBackupContentFile", maliciousRef)
                            put(
                                "fullBackupContentXml",
                                "<full-backup-content><exclude domain=\"sharedpref\" path=\"user_prefs.xml\"/></full-backup-content>",
                            )
                        }
                    }
                        .toString()
                )
            assertEquals(
                "backup_rules.xml excludes domain=\"sharedpref\" path=\"user_prefs.xml\" in <full-backup-content>.",
                BackupRulesChecker.diagnoseLikelyCause(
                    resultsDirPath = resultsDir.absolutePath,
                    transportMode = BackupTransportMode.CLOUD_ENCRYPTED,
                    storages = listOf(StorageDomain.Preference("user_prefs", "k", "v")),
                    actionTargets = emptyList(),
                    failureMessage = "missing key",
                ),
            )
        }
    }

    @Test
    fun coversDisableIfNoEncryptionDeviceProtectedRootBomActionArgsAndShortStemHints() {
        // 1. <cloud-backup disableIfNoEncryptionCapabilities="true"> blocks unencrypted cloud/local
        // but not encrypted cloud.
        val encAttrDir =
            writeSuiteEnvironment(
                dataExtractionRulesRef = "@xml/data_extraction_rules",
                dataExtractionRulesXml =
                    "\uFEFF<data-extraction-rules><cloud-backup disableIfNoEncryptionCapabilities=\"true\">" +
                        "<include domain=\"sharedpref\" path=\"user_prefs.xml\"/>" +
                        "</cloud-backup></data-extraction-rules>",
            )
        for (mode in listOf(BackupTransportMode.CLOUD_UNENCRYPTED, BackupTransportMode.LOCAL)) {
            assertContains(
                BackupRulesChecker.diagnoseLikelyCause(
                        resultsDirPath = encAttrDir.absolutePath,
                        transportMode = mode,
                        storages = listOf(StorageDomain.Preference("user_prefs", "k", "v")),
                        actionTargets = emptyList(),
                        failureMessage = "missing key",
                    )
                    .orEmpty(),
                "disableIfNoEncryptionCapabilities=\"true\"",
            )
        }
        assertNull(
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = encAttrDir.absolutePath,
                transportMode = BackupTransportMode.CLOUD_ENCRYPTED,
                storages = listOf(StorageDomain.Preference("user_prefs", "k", "v")),
                actionTargets = emptyList(),
                failureMessage = "missing key",
            )
        )

        // 2. transportMode = null falls back to <full-backup-content> when <cloud-backup> is
        // absent.
        val fallbackDir =
            writeSuiteEnvironment(
                dataExtractionRulesRef = "@xml/data_extraction_rules",
                dataExtractionRulesXml =
                    "<data-extraction-rules><device-transfer><exclude domain=\"database\" path=\"d2d.db\"/></device-transfer></data-extraction-rules>",
                fullBackupContentRef = "@xml/backup_rules",
                fullBackupContentXml =
                    "<full-backup-content><exclude domain=\"sharedpref\" path=\"legacy.xml\"/></full-backup-content>",
            )
        assertEquals(
            "backup_rules.xml excludes domain=\"sharedpref\" path=\"legacy.xml\" in <full-backup-content>.",
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = fallbackDir.absolutePath,
                transportMode = null,
                storages = listOf(StorageDomain.Preference("legacy", "k", "v")),
                actionTargets = emptyList(),
                failureMessage = "missing key",
            ),
        )

        // 3. requireFlags="deviceToDeviceTransfer" in <full-backup-content> blocks CLOUD_ENCRYPTED,
        // and domain="root" / domain="device_root" match CE and DE storages.
        val rootRulesDir =
            writeSuiteEnvironment(
                fullBackupContentRef = "@xml/backup_rules",
                fullBackupContentXml =
                    """
                    <full-backup-content>
                        <include domain="sharedpref" path="d2d_only.xml" requireFlags="deviceToDeviceTransfer" />
                        <exclude domain="root" path="shared_prefs/ce_root.xml" />
                        <exclude domain="device_root" path="databases/de_root.db" />
                        <exclude domain="device_sharedpref" path="de_pref.xml" />
                        <exclude domain="device_database" path="de_db.db" />
                        <exclude domain="device_file" path="de_file.txt" />
                    </full-backup-content>
                    """
                        .trimIndent(),
            )
        assertContains(
            BackupRulesChecker.diagnoseLikelyCause(
                    resultsDirPath = rootRulesDir.absolutePath,
                    transportMode = BackupTransportMode.CLOUD_ENCRYPTED,
                    storages = listOf(StorageDomain.Preference("d2d_only", "k", "v")),
                    actionTargets = emptyList(),
                    failureMessage = "missing key",
                )
                .orEmpty(),
            "requireFlags=\"deviceToDeviceTransfer\"",
        )
        assertContains(
            BackupRulesChecker.diagnoseLikelyCause(
                    resultsDirPath = rootRulesDir.absolutePath,
                    transportMode = BackupTransportMode.CLOUD_ENCRYPTED,
                    storages = listOf(StorageDomain.Preference("ce_root", "k", "v")),
                    actionTargets = emptyList(),
                    failureMessage = "missing key",
                )
                .orEmpty(),
            "domain=\"root\" path=\"shared_prefs/ce_root.xml\"",
        )
        assertContains(
            BackupRulesChecker.diagnoseLikelyCause(
                    resultsDirPath = rootRulesDir.absolutePath,
                    transportMode = BackupTransportMode.CLOUD_ENCRYPTED,
                    storages =
                        listOf(
                            StorageDomain.Database(
                                dbName = "de_root.db",
                                table = "t",
                                primaryKeyCol = "id",
                                primaryKeyVal = 1,
                                columnValues = mapOf("id" to 1),
                                isDeviceProtectedStorage = true,
                            )
                        ),
                    actionTargets = emptyList(),
                    failureMessage = "missing table",
                )
                .orEmpty(),
            "domain=\"device_root\" path=\"databases/de_root.db\"",
        )
        assertContains(
            BackupRulesChecker.diagnoseLikelyCause(
                    resultsDirPath = rootRulesDir.absolutePath,
                    transportMode = BackupTransportMode.CLOUD_ENCRYPTED,
                    storages =
                        listOf(
                            StorageDomain.Preference(
                                "de_pref",
                                "k",
                                "v",
                                isDeviceProtectedStorage = true,
                            )
                        ),
                    actionTargets = emptyList(),
                    failureMessage = "missing key",
                )
                .orEmpty(),
            "domain=\"device_sharedpref\"",
        )
        assertContains(
            BackupRulesChecker.diagnoseLikelyCause(
                    resultsDirPath = rootRulesDir.absolutePath,
                    transportMode = BackupTransportMode.CLOUD_ENCRYPTED,
                    storages =
                        listOf(
                            StorageDomain.TextFile(
                                "de_file.txt",
                                "v",
                                isDeviceProtectedStorage = true,
                            )
                        ),
                    actionTargets = emptyList(),
                    failureMessage = "missing file",
                )
                .orEmpty(),
            "domain=\"device_file\"",
        )

        // 4. targetCoordinateFromArgs: case-insensitive storage_type, default_prefs fallback,
        // and absolute file path rejection.
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

        // 5. Empty-target exclude hint: short stem "id.db" does not false-match "invalid", while
        // dotfile ".secret" matches when named in failureMessage.
        val hintDir =
            writeSuiteEnvironment(
                dataExtractionRulesRef = "@xml/data_extraction_rules",
                dataExtractionRulesXml =
                    """
                    <data-extraction-rules>
                        <cloud-backup>
                            <exclude domain="database" path="id.db" />
                            <exclude domain="file" path=".secret" />
                        </cloud-backup>
                    </data-extraction-rules>
                    """
                        .trimIndent(),
            )
        assertNull(
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = hintDir.absolutePath,
                transportMode = BackupTransportMode.CLOUD_ENCRYPTED,
                storages = emptyList(),
                actionTargets = emptyList(),
                failureMessage = "invalid state after restore",
            )
        )
        assertContains(
            BackupRulesChecker.diagnoseLikelyCause(
                    resultsDirPath = hintDir.absolutePath,
                    transportMode = BackupTransportMode.CLOUD_ENCRYPTED,
                    storages = emptyList(),
                    actionTargets = emptyList(),
                    failureMessage = "missing .secret after restore",
                )
                .orEmpty(),
            "path=\".secret\"",
        )
    }

    @Test
    fun strictEmptyTargetExcludeHintSkipsPackageNamesCommonWordsAndExplicitReasons() {
        val resultsDir =
            writeSuiteEnvironment(
                dataExtractionRulesRef = "@xml/data_extraction_rules",
                dataExtractionRulesXml =
                    """
                    <data-extraction-rules>
                        <device-transfer>
                            <exclude domain="sharedpref" path="tmp" />
                            <exclude domain="database" path="app.db" />
                            <exclude domain="database" path="notes.db" />
                            <exclude domain="sharedpref" path="notes.xml" />
                            <exclude domain="file" path="datastore/auth_prefs.preferences_pb" />
                            <exclude domain="file" path=".secret" />
                            <exclude domain="file" path="app" />
                        </device-transfer>
                    </data-extraction-rules>
                    """
                        .trimIndent(),
            )

        // Package name ".app" and "no app data" in a restore skip failure must not match "app.db".
        assertNull(
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = resultsDir.absolutePath,
                transportMode = BackupTransportMode.DEVICE_TO_DEVICE,
                storages = emptyList(),
                actionTargets = emptyList(),
                failureMessage =
                    "Restore of com.example.app from backup.zip restored no app data: " +
                        "Google Play services skipped the app as not eligible for restore " +
                        "(INELIGIBLE_FIRST_PARTY)",
            )
        )

        // Dotted package names, hyphenated identifiers, unrelated path segments, and generic "app"
        // words must not match "app.db", "notes.db", "notes.xml", or extensionless "app".
        for (unrelatedMessage in
            listOf(
                "Verification failed in com.example.app: expected no app data loss",
                "Action com.example.notes.VerifyAction failed",
                "Check failed in notes-helper, dir/notes/cache, and other_dir/app.db",
            )) {
            assertNull(
                BackupRulesChecker.diagnoseLikelyCause(
                    resultsDirPath = resultsDir.absolutePath,
                    transportMode = BackupTransportMode.DEVICE_TO_DEVICE,
                    storages = emptyList(),
                    actionTargets = emptyList(),
                    failureMessage = unrelatedMessage,
                ),
                "Expected null diagnosis for message: $unrelatedMessage",
            )
        }

        // Dotted leaf ("app.db") takes precedence over an earlier extensionless single-word rule
        // ("tmp"), including when followed by sentence punctuation.
        assertEquals(
            "data_extraction_rules.xml excludes domain=\"database\" path=\"app.db\" " +
                "for <device-transfer>.",
            BackupRulesChecker.diagnoseLikelyCause(
                resultsDirPath = resultsDir.absolutePath,
                transportMode = BackupTransportMode.DEVICE_TO_DEVICE,
                storages = emptyList(),
                actionTargets = emptyList(),
                failureMessage = "tmp check failed: missing app.db.",
            ),
        )

        // Sandbox and absolute device paths match the rule's domain subdirectory.
        assertContains(
            BackupRulesChecker.diagnoseLikelyCause(
                    resultsDirPath = resultsDir.absolutePath,
                    transportMode = BackupTransportMode.DEVICE_TO_DEVICE,
                    storages = emptyList(),
                    actionTargets = emptyList(),
                    failureMessage =
                        "FileNotFoundException: /data/user/0/com.example.app/databases/app.db",
                )
                .orEmpty(),
            "path=\"app.db\"",
        )
        assertContains(
            BackupRulesChecker.diagnoseLikelyCause(
                    resultsDirPath = resultsDir.absolutePath,
                    transportMode = BackupTransportMode.DEVICE_TO_DEVICE,
                    storages = emptyList(),
                    actionTargets = emptyList(),
                    failureMessage = "missing /data/user/0/com.example.app/files/.secret",
                )
                .orEmpty(),
            "path=\".secret\"",
        )
        assertContains(
            BackupRulesChecker.diagnoseLikelyCause(
                    resultsDirPath = resultsDir.absolutePath,
                    transportMode = BackupTransportMode.DEVICE_TO_DEVICE,
                    storages = emptyList(),
                    actionTargets = emptyList(),
                    failureMessage =
                        "missing /data/user/0/com.example.app/files/datastore/auth_prefs.preferences_pb",
                )
                .orEmpty(),
            "path=\"datastore/auth_prefs.preferences_pb\"",
        )
    }

    private fun writeSuiteEnvironment(
        allowBackup: Boolean = true,
        targetSdkVersion: Int? = null,
        fullBackupContentRef: String? = null,
        fullBackupContentXml: String? = null,
        dataExtractionRulesRef: String? = null,
        dataExtractionRulesXml: String? = null,
    ): File {
        val resultsDir = tempFolder.newFolder("results-${System.nanoTime()}")
        writeSuiteEnvironmentJson(
            resultsDir = resultsDir,
            allowBackup = allowBackup,
            targetSdkVersion = targetSdkVersion,
            fullBackupContentRef = fullBackupContentRef,
            fullBackupContentXml = fullBackupContentXml,
            dataExtractionRulesRef = dataExtractionRulesRef,
            dataExtractionRulesXml = dataExtractionRulesXml,
        )
        return resultsDir
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
