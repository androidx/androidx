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

import com.android.utils.XmlUtils
import java.io.StringReader
import java.util.Locale
import java.util.Properties
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource

/**
 * Diagnoses backup rule configurations that omit app data during failed tests.
 *
 * Reads the resolved manifest and XML backup rules passed in the AGP test suite input properties
 * (`backup_rules.xml` and `data_extraction_rules.xml`).
 */
internal object BackupRulesChecker {
    private val BACKUP_RULE_INPUT_KEYS =
        listOf(
            BackupReportKeys.INPUT_ALLOW_BACKUP,
            BackupReportKeys.INPUT_FULL_BACKUP_CONTENT,
            BackupReportKeys.INPUT_FULL_BACKUP_CONTENT_FILE,
            BackupReportKeys.INPUT_FULL_BACKUP_CONTENT_XML,
            BackupReportKeys.INPUT_DATA_EXTRACTION_RULES,
            BackupReportKeys.INPUT_DATA_EXTRACTION_RULES_FILE,
            BackupReportKeys.INPUT_DATA_EXTRACTION_RULES_XML,
        )

    private enum class StorageKind {
        PREFS,
        DATASTORE_PREFS,
        DATABASE,
        FILE,
    }

    internal data class TargetCoordinate(val domain: String, val path: String)

    internal data class XmlRule(
        val domain: String,
        val rawPath: String,
        val normalizedPath: String,
        val requireFlags: Set<String> = emptySet(),
    )

    internal data class RuleSection(
        val fileName: String,
        val sectionTag: String,
        val disableIfNoEncryption: Boolean = false,
        val includes: List<XmlRule> = emptyList(),
        val excludes: List<XmlRule> = emptyList(),
    )

    internal data class ParsedBackupConfig(
        val allowBackup: Boolean = true,
        val targetSdkVersion: Int? = null,
        val fullBackupContentDisabled: Boolean = false,
        val dataExtractionRulesFile: String? = null,
        val dataExtractionRulesDoc: Document? = null,
        val fullBackupContentFile: String? = null,
        val fullBackupContentDoc: Document? = null,
    )

    /**
     * Diagnoses whether backup rules in [suiteProperties] explain a failure under [transportMode].
     *
     * Returns a human-readable cause string, or null when no backup configuration rule matches.
     */
    fun diagnoseLikelyCause(
        suiteProperties: Properties?,
        transportMode: BackupTransportMode?,
        storages: List<StorageDomain>,
        actionTargets: List<TargetCoordinate>,
        failureMessage: String?,
    ): String? {
        val config = loadFromSuiteProperties(suiteProperties) ?: return null
        return diagnoseFromConfig(
            config = config,
            transportMode = transportMode,
            storages = storages,
            actionTargets = actionTargets,
            failureMessage = failureMessage,
        )
    }

    internal fun diagnoseFromConfig(
        config: ParsedBackupConfig,
        transportMode: BackupTransportMode?,
        storages: List<StorageDomain>,
        actionTargets: List<TargetCoordinate>,
        failureMessage: String?,
    ): String? {
        val d2dOnApi31PlusTarget =
            transportMode == BackupTransportMode.DEVICE_TO_DEVICE &&
                (config.targetSdkVersion != null && config.targetSdkVersion >= 31)
        if (!config.allowBackup) {
            if (!d2dOnApi31PlusTarget) {
                return "AndroidManifest.xml sets android:allowBackup=\"false\", which disables " +
                    "app data backup and restore."
            }
            if (
                failureMessage != null &&
                    (failureMessage.contains("contains no app data", ignoreCase = true) ||
                        failureMessage.endsWith("restored no app data", ignoreCase = true) ||
                        failureMessage.contains("allowBackup=\"false\"", ignoreCase = true) ||
                        failureMessage.contains("allowBackup property is false", ignoreCase = true))
            ) {
                return "AndroidManifest.xml sets android:allowBackup=\"false\". " +
                    "Device-to-device test backups require android:allowBackup=\"true\"."
            }
        }

        val targets = (storages.mapNotNull { targetCoordinateOf(it) } + actionTargets).distinct()

        // Android 12+ uses data_extraction_rules when the destination section (<cloud-backup> or
        // <device-transfer>) is present, and falls back to fullBackupContent otherwise.
        val dataExtractionDoc = config.dataExtractionRulesDoc
        if (dataExtractionDoc != null) {
            val fileName = config.dataExtractionRulesFile ?: "data_extraction_rules.xml"
            val sectionTags =
                when (transportMode) {
                    BackupTransportMode.DEVICE_TO_DEVICE -> listOf("device-transfer")
                    BackupTransportMode.CLOUD_ENCRYPTED,
                    BackupTransportMode.CLOUD_UNENCRYPTED,
                    BackupTransportMode.LOCAL -> listOf("cloud-backup")
                    else -> listOf("cloud-backup", "device-transfer")
                }
            val api31PlusTarget = config.targetSdkVersion != null && config.targetSdkVersion >= 31
            var canFallbackToFullBackup = false
            for (tag in sectionTags) {
                val section = parseDataExtractionSection(dataExtractionDoc, fileName, tag)
                if (section == null) {
                    if (!(tag == "device-transfer" && api31PlusTarget)) {
                        canFallbackToFullBackup = true
                    }
                    continue
                }
                diagnoseSection(
                        section = section,
                        transportMode = transportMode,
                        targets = targets,
                        failureMessage = failureMessage,
                        isDataExtraction = true,
                    )
                    ?.let {
                        return it
                    }
            }
            if (!canFallbackToFullBackup) {
                return null
            }
        }

        // Android 12+ (targetSdkVersion >= 31) ignores fullBackupContent for device-to-device.
        if (d2dOnApi31PlusTarget) {
            return null
        }

        if (config.fullBackupContentDisabled) {
            return "AndroidManifest.xml sets android:fullBackupContent=\"false\", which disables " +
                "full-data backup."
        }

        val fullBackupDoc = config.fullBackupContentDoc
        if (fullBackupDoc != null) {
            val fileName = config.fullBackupContentFile ?: "backup_rules.xml"
            val section = parseFullBackupSection(fullBackupDoc, fileName)
            if (section != null) {
                return diagnoseSection(
                    section = section,
                    transportMode = transportMode,
                    targets = targets,
                    failureMessage = failureMessage,
                    isDataExtraction = false,
                )
            }
        }
        return null
    }

    internal fun targetCoordinateOf(domain: StorageDomain): TargetCoordinate? =
        when (domain) {
            is StorageDomain.Preference ->
                coordinateFor(StorageKind.PREFS, domain.prefName, domain.isDeviceProtectedStorage)
            is StorageDomain.DataStorePreference ->
                coordinateFor(
                    StorageKind.DATASTORE_PREFS,
                    domain.dataStoreName,
                    domain.isDeviceProtectedStorage,
                )
            is StorageDomain.Database ->
                coordinateFor(StorageKind.DATABASE, domain.dbName, domain.isDeviceProtectedStorage)
            is StorageDomain.TextFile ->
                coordinateFor(StorageKind.FILE, domain.path, domain.isDeviceProtectedStorage)
            is StorageDomain.BinaryFile ->
                coordinateFor(StorageKind.FILE, domain.path, domain.isDeviceProtectedStorage)
            // ExcludedData is expected to be omitted by backup rules.
            is StorageDomain.ExcludedData,
            is StorageDomain.CustomData -> null
            else -> null
        }

    internal fun targetCoordinateFromArgs(args: Map<String, String>): TargetCoordinate? {
        val deviceProtected = args[BackupActionInputKeys.IS_DEVICE_PROTECTED]?.toBoolean() == true
        val rawStorageType =
            args[BackupActionInputKeys.STORAGE_TYPE]
                ?: if (BackupActionInputKeys.PREF_KEY in args) {
                    BackupActionValues.STORAGE_TYPE_PREFS
                } else {
                    return null
                }
        return when (rawStorageType.uppercase(Locale.ROOT)) { // STORAGE_TYPE_* values are uppercase
            BackupActionValues.STORAGE_TYPE_PREFS ->
                coordinateFor(
                    StorageKind.PREFS,
                    args[BackupActionInputKeys.PREF_NAME] ?: "default_prefs",
                    deviceProtected,
                )
            BackupActionValues.STORAGE_TYPE_DATASTORE_PREFS ->
                // DataStorePreference actions pass dataStoreName via PREF_NAME in preferenceArgs.
                coordinateFor(
                    StorageKind.DATASTORE_PREFS,
                    args[BackupActionInputKeys.PREF_NAME].orEmpty(),
                    deviceProtected,
                )
            BackupActionValues.STORAGE_TYPE_DATABASE ->
                coordinateFor(
                    StorageKind.DATABASE,
                    args[BackupActionInputKeys.DB_NAME].orEmpty(),
                    deviceProtected,
                )
            BackupActionValues.STORAGE_TYPE_FILES ->
                coordinateFor(
                    StorageKind.FILE,
                    args[BackupActionInputKeys.PATH].orEmpty(),
                    deviceProtected,
                )
            else -> null
        }
    }

    private fun coordinateFor(
        kind: StorageKind,
        rawNameOrPath: String,
        isDeviceProtected: Boolean,
    ): TargetCoordinate? {
        if (rawNameOrPath.isEmpty()) return null
        if (kind == StorageKind.FILE && rawNameOrPath.trimStart().startsWith("/")) return null
        val prefix = if (isDeviceProtected) "device_" else ""
        return when (kind) {
            StorageKind.PREFS ->
                TargetCoordinate("${prefix}sharedpref", normalizePath("$rawNameOrPath.xml"))
            StorageKind.DATASTORE_PREFS ->
                TargetCoordinate(
                    "${prefix}file",
                    normalizePath("datastore/$rawNameOrPath.preferences_pb"),
                )
            StorageKind.DATABASE ->
                TargetCoordinate("${prefix}database", normalizePath(rawNameOrPath))
            StorageKind.FILE -> TargetCoordinate("${prefix}file", normalizePath(rawNameOrPath))
        }
    }

    private fun diagnoseSection(
        section: RuleSection,
        transportMode: BackupTransportMode?,
        targets: List<TargetCoordinate>,
        failureMessage: String?,
        isDataExtraction: Boolean,
    ): String? {
        if (
            isDataExtraction &&
                section.sectionTag == "cloud-backup" &&
                section.disableIfNoEncryption &&
                (transportMode == BackupTransportMode.CLOUD_UNENCRYPTED ||
                    transportMode == BackupTransportMode.LOCAL)
        ) {
            return "${section.fileName} sets disableIfNoEncryptionCapabilities=\"true\" on " +
                "<cloud-backup>, which skips backup under $transportMode."
        }

        val scopeSuffix =
            if (isDataExtraction) "for <${section.sectionTag}>." else "in <full-backup-content>."

        if (targets.isNotEmpty()) {
            for (target in targets) {
                val matchingExclude = section.excludes.firstOrNull { ruleMatches(it, target) }
                if (matchingExclude != null) {
                    return "${section.fileName} excludes domain=\"${matchingExclude.domain}\" " +
                        "path=\"${matchingExclude.rawPath}\" $scopeSuffix"
                }
            }
            if (section.includes.isNotEmpty()) {
                for (target in targets) {
                    val activeIncludes =
                        section.includes.filter {
                            flagsSatisfied(it.requireFlags, transportMode, isDataExtraction)
                        }
                    if (activeIncludes.none { ruleMatches(it, target) }) {
                        val flagBlocked =
                            if (!isDataExtraction) {
                                section.includes.firstOrNull {
                                    ruleMatches(it, target) &&
                                        !flagsSatisfied(
                                            it.requireFlags,
                                            transportMode,
                                            isDataExtraction = false,
                                        )
                                }
                            } else {
                                null
                            }
                        if (flagBlocked != null) {
                            val flagsText = flagBlocked.requireFlags.joinToString("|")
                            return "${section.fileName} requires requireFlags=\"$flagsText\" on " +
                                "domain=\"${flagBlocked.domain}\" path=\"${flagBlocked.rawPath}\", " +
                                "which excludes it under ${transportMode ?: "this transport"}."
                        }
                        return "${section.fileName} uses <include> rules " +
                            (if (isDataExtraction) "for <${section.sectionTag}> "
                            else "in <full-backup-content> ") +
                            "that omit domain=\"${target.domain}\" path=\"${target.path}\"."
                    }
                }
            }
            return null
        }

        if (section.excludes.isNotEmpty() && !failureMessage.isNullOrEmpty()) {
            val hinted = findHintedExclude(section.excludes, failureMessage)
            if (hinted != null) {
                return "${section.fileName} excludes domain=\"${hinted.domain}\" " +
                    "path=\"${hinted.rawPath}\" $scopeSuffix"
            }
        }

        if (!isDataExtraction && section.includes.isNotEmpty()) {
            val flagBlocked =
                section.includes.firstOrNull {
                    !flagsSatisfied(it.requireFlags, transportMode, isDataExtraction = false)
                }
            if (
                flagBlocked != null &&
                    section.includes.none {
                        flagsSatisfied(it.requireFlags, transportMode, isDataExtraction = false)
                    }
            ) {
                val flagsText = flagBlocked.requireFlags.joinToString("|")
                return "${section.fileName} requires requireFlags=\"$flagsText\" on " +
                    "domain=\"${flagBlocked.domain}\" path=\"${flagBlocked.rawPath}\", " +
                    "which excludes it under ${transportMode ?: "this transport"}."
            }
        }

        return null
    }

    private fun ruleMatches(rule: XmlRule, target: TargetCoordinate): Boolean {
        val domainMatches =
            rule.domain == target.domain ||
                (rule.domain == "root" && !target.domain.startsWith("device_")) ||
                (rule.domain == "device_root" && target.domain.startsWith("device_"))
        if (!domainMatches) return false

        // Map root/device_root rules into the target's sandbox subdirectory.
        val effectiveTargetPath =
            if (rule.domain == "root" || rule.domain == "device_root") {
                sandboxRelativePath(target)
            } else {
                target.path
            }

        val rulePath = rule.normalizedPath
        if (rulePath.isEmpty() || rulePath == ".") return true
        if (effectiveTargetPath == rulePath || effectiveTargetPath.startsWith("$rulePath/")) {
            return true
        }
        if (
            (rule.domain == "sharedpref" || rule.domain == "device_sharedpref") &&
                !rulePath.endsWith(".xml") &&
                effectiveTargetPath == "$rulePath.xml"
        ) {
            return true
        }
        return false
    }

    private fun sandboxRelativePath(target: TargetCoordinate): String =
        when (target.domain) {
            "sharedpref",
            "device_sharedpref" -> "shared_prefs/${target.path}"
            "database",
            "device_database" -> "databases/${target.path}"
            "file",
            "device_file" -> "files/${target.path}"
            else -> target.path
        }

    private fun flagsSatisfied(
        flags: Set<String>,
        transportMode: BackupTransportMode?,
        isDataExtraction: Boolean,
    ): Boolean {
        if (isDataExtraction || flags.isEmpty() || transportMode == null) return true
        if (
            "deviceToDeviceTransfer" in flags &&
                transportMode != BackupTransportMode.DEVICE_TO_DEVICE
        ) {
            return false
        }
        if (
            "clientSideEncryption" in flags &&
                transportMode != BackupTransportMode.DEVICE_TO_DEVICE &&
                transportMode != BackupTransportMode.CLOUD_ENCRYPTED
        ) {
            return false
        }
        return true
    }

    private fun loadFromSuiteProperties(suiteProperties: Properties?): ParsedBackupConfig? {
        if (
            BACKUP_RULE_INPUT_KEYS.none {
                BackupReportKeys.readProperty(suiteProperties, it) != null
            }
        ) {
            return null
        }
        return try {
            val targetSdkVersion =
                BackupReportKeys.readProperty(
                        suiteProperties,
                        BackupReportKeys.INPUT_TARGET_SDK_VERSION,
                    )
                    ?.toIntOrNull()
            val allowBackup =
                !BackupReportKeys.readProperty(suiteProperties, BackupReportKeys.INPUT_ALLOW_BACKUP)
                    .equals("false", ignoreCase = true)
            val fullBackupRef =
                BackupReportKeys.readProperty(
                    suiteProperties,
                    BackupReportKeys.INPUT_FULL_BACKUP_CONTENT,
                )
            val fullBackupDisabled = fullBackupRef.equals("false", ignoreCase = true)
            val fullBackupFile =
                BackupReportKeys.readProperty(
                        suiteProperties,
                        BackupReportKeys.INPUT_FULL_BACKUP_CONTENT_FILE,
                    )
                    ?.takeIf { isSafeFileName(it) } ?: xmlFileNameFromRef(fullBackupRef)
            val fullBackupXml =
                BackupReportKeys.readProperty(
                    suiteProperties,
                    BackupReportKeys.INPUT_FULL_BACKUP_CONTENT_XML,
                )
            val dataExtractionRef =
                BackupReportKeys.readProperty(
                    suiteProperties,
                    BackupReportKeys.INPUT_DATA_EXTRACTION_RULES,
                )
            val dataExtractionFile =
                BackupReportKeys.readProperty(
                        suiteProperties,
                        BackupReportKeys.INPUT_DATA_EXTRACTION_RULES_FILE,
                    )
                    ?.takeIf { isSafeFileName(it) } ?: xmlFileNameFromRef(dataExtractionRef)
            val dataExtractionXml =
                BackupReportKeys.readProperty(
                    suiteProperties,
                    BackupReportKeys.INPUT_DATA_EXTRACTION_RULES_XML,
                )
            ParsedBackupConfig(
                allowBackup = allowBackup,
                targetSdkVersion = targetSdkVersion,
                fullBackupContentDisabled = fullBackupDisabled,
                dataExtractionRulesFile = dataExtractionFile,
                dataExtractionRulesDoc = dataExtractionXml?.let(::parseXmlString),
                fullBackupContentFile = fullBackupFile,
                fullBackupContentDoc = fullBackupXml?.let(::parseXmlString),
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseDataExtractionSection(
        doc: Document,
        fileName: String,
        sectionTag: String,
    ): RuleSection? {
        val root = doc.documentElement ?: return null
        val sections = XmlUtils.getSubTagsByName(root, sectionTag).toList()
        if (sections.isEmpty()) return null
        val disableIfNoEncryption = sections.any {
            it.getAttribute("disableIfNoEncryptionCapabilities").equals("true", ignoreCase = true)
        }
        return RuleSection(
            fileName = fileName,
            sectionTag = sectionTag,
            disableIfNoEncryption = disableIfNoEncryption,
            includes =
                sections.flatMap { readChildRules(it, "include", ignoreRequireFlags = true) },
            excludes =
                sections.flatMap { readChildRules(it, "exclude", ignoreRequireFlags = true) },
        )
    }

    private fun parseFullBackupSection(doc: Document, fileName: String): RuleSection? {
        val root = doc.documentElement ?: return null
        if (root.tagName != "full-backup-content" && root.localName != "full-backup-content") {
            return null
        }
        return RuleSection(
            fileName = fileName,
            sectionTag = "full-backup-content",
            includes = readChildRules(root, "include", ignoreRequireFlags = false),
            excludes = readChildRules(root, "exclude", ignoreRequireFlags = false),
        )
    }

    private fun readChildRules(
        parent: Element,
        tagName: String,
        ignoreRequireFlags: Boolean,
    ): List<XmlRule> =
        XmlUtils.getSubTagsByName(parent, tagName).mapNotNull { child ->
            val domain = child.getAttribute("domain").trim()
            if (domain.isEmpty()) return@mapNotNull null
            val rawPath = child.getAttribute("path").trim().ifEmpty { "." }
            val requireFlags =
                if (ignoreRequireFlags) {
                    emptySet()
                } else {
                    child
                        .getAttribute("requireFlags")
                        .split('|')
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .toSet()
                }
            XmlRule(
                domain = domain,
                rawPath = rawPath,
                normalizedPath = normalizePath(rawPath),
                requireFlags = requireFlags,
            )
        }

    internal fun xmlFileNameFromRef(ref: String?): String? {
        val trimmed = ref?.trim()?.takeIf { it.startsWith("@xml/") } ?: return null
        val stem =
            trimmed.removePrefix("@xml/").removeSuffix(".xml").takeIf {
                it.isNotEmpty() && isSafeFileName(it)
            } ?: return null
        return "$stem.xml"
    }

    private fun findHintedExclude(excludes: List<XmlRule>, failureMessage: String): XmlRule? {
        val lowerFailure = failureMessage.lowercase(Locale.ROOT)
        if (EXPLICIT_FAILURE_REASONS.any { it in lowerFailure }) return null

        // Pass 1: prefer multi-segment rule paths, domain sandbox paths (such as
        // "databases/app.db"), or dotted file leaves (such as "app.db") over bare single words.
        excludes
            .firstOrNull { rule ->
                if (rule.normalizedPath == ".") return@firstOrNull false
                val fullPath = rule.normalizedPath.lowercase(Locale.ROOT)
                val leaf = fullPath.substringAfterLast('/')
                (fullPath.contains('/') &&
                    fullPath.length >= 3 &&
                    containsPathSuffixToken(lowerFailure, fullPath)) ||
                    sandboxHintPaths(rule, fullPath).any {
                        containsPathSuffixToken(lowerFailure, it)
                    } ||
                    (leaf.length >= 3 && '.' in leaf && containsToken(lowerFailure, leaf))
            }
            ?.let {
                return it
            }

        // Pass 2: match extensionless leaves or preference stems whose Kotlin APIs omit the suffix.
        return excludes.firstOrNull { rule ->
            val leaf = rule.normalizedPath.substringAfterLast('/').lowercase(Locale.ROOT)
            if ('.' !in leaf) {
                isSpecificWordHint(leaf) && containsToken(lowerFailure, leaf)
            } else {
                val stem = leaf.substringBeforeLast('.').ifEmpty { leaf }
                stem != leaf &&
                    usesExtensionlessApiName(rule, leaf) &&
                    isSpecificWordHint(stem) &&
                    containsToken(lowerFailure, stem)
            }
        }
    }

    private fun sandboxHintPaths(rule: XmlRule, lowerNormalizedPath: String): List<String> =
        when (rule.domain) {
            "sharedpref",
            "device_sharedpref" ->
                buildList {
                    add("shared_prefs/$lowerNormalizedPath")
                    if (!lowerNormalizedPath.endsWith(".xml")) {
                        add("shared_prefs/$lowerNormalizedPath.xml")
                    }
                }
            "database",
            "device_database" -> listOf("databases/$lowerNormalizedPath")
            "file",
            "device_file" -> listOf("files/$lowerNormalizedPath")
            else -> emptyList()
        }

    private fun isSpecificWordHint(word: String): Boolean =
        word.length >= 3 && word != "." && word !in GENERIC_HINT_STOPWORDS

    private fun usesExtensionlessApiName(rule: XmlRule, leaf: String): Boolean =
        rule.domain == "sharedpref" ||
            rule.domain == "device_sharedpref" ||
            leaf.endsWith(".preferences_pb")

    private fun containsToken(text: String, token: String): Boolean =
        Regex("""(?<![a-z0-9_./-])${Regex.escape(token)}(?![a-z0-9_/-]|\.[a-z0-9_/-])""")
            .containsMatchIn(text)

    private fun containsPathSuffixToken(text: String, pathToken: String): Boolean =
        Regex("""(?<![a-z0-9_.-])${Regex.escape(pathToken)}(?![a-z0-9_/-]|\.[a-z0-9_/-])""")
            .containsMatchIn(text)

    private val EXPLICIT_FAILURE_REASONS =
        listOf(
            "not eligible for restore",
            "doesn't allow backups",
            "does not allow backups",
            "allowbackup=\"false\"",
            "allowbackup property is false",
            "contains no app data",
            "restored no app data",
            "is a local backup",
            "timed out after",
        )

    private val GENERIC_HINT_STOPWORDS =
        setOf(
            "actual",
            "app",
            "backup",
            "cache",
            "data",
            "database",
            "default",
            "device",
            "empty",
            "error",
            "expected",
            "failed",
            "false",
            "file",
            "files",
            "key",
            "main",
            "missing",
            "null",
            "pref",
            "prefs",
            "restore",
            "restored",
            "source",
            "state",
            "target",
            "temp",
            "test",
            "true",
            "user",
            "value",
        )

    private fun isSafeFileName(name: String): Boolean =
        name.isNotEmpty() && name != "." && name != ".." && '/' !in name && '\\' !in name

    private fun normalizePath(path: String): String =
        path.trim().removePrefix("./").trim('/').ifEmpty { "." }

    internal fun parseXmlString(xml: String): Document? =
        try {
            XmlUtils.createDocumentBuilderFactory()
                .apply { isNamespaceAware = true }
                .newDocumentBuilder()
                .parse(InputSource(StringReader(XmlUtils.stripBom(xml))))
        } catch (_: Exception) {
            null
        }
}
