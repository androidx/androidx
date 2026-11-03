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
import java.io.File
import java.io.StringReader
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource

/**
 * Diagnoses backup rule configurations that omit app data during failed tests.
 *
 * Reads the resolved manifest and XML backup rules written to `suite-environment.json`
 * (`backup_rules.xml` and `data_extraction_rules.xml`).
 */
internal object BackupRulesChecker {
    private const val SUITE_ENVIRONMENT_FILE = "suite-environment.json"

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

    internal fun loadFromSuiteEnvironment(resultsDirPath: String?): ParsedBackupConfig? {
        if (resultsDirPath.isNullOrEmpty()) return null
        val envFile = File(resultsDirPath, SUITE_ENVIRONMENT_FILE)
        if (!envFile.isFile) return null
        return try {
            val root = Json.parseToJsonElement(envFile.readText(Charsets.UTF_8)).jsonObject
            val targetSdkVersion = root["targetSdkVersion"].asStringOrNull()?.toIntOrNull()
            val rules = root["backupRules"]?.takeIf { it !is JsonNull }?.jsonObject ?: return null
            val allowBackup = (rules["allowBackup"] as? JsonPrimitive)?.booleanOrNull ?: true
            val fullBackupRef = rules["fullBackupContent"].asStringOrNull()
            val fullBackupDisabled = fullBackupRef.equals("false", ignoreCase = true)
            val fullBackupFile =
                rules["fullBackupContentFile"].asStringOrNull()?.takeIf { isSafeFileName(it) }
                    ?: xmlFileNameFromRef(fullBackupRef)
            val fullBackupXml = rules["fullBackupContentXml"].asStringOrNull()
            val dataExtractionRef = rules["dataExtractionRules"].asStringOrNull()
            val dataExtractionFile =
                rules["dataExtractionRulesFile"].asStringOrNull()?.takeIf { isSafeFileName(it) }
                    ?: xmlFileNameFromRef(dataExtractionRef)
            val dataExtractionXml = rules["dataExtractionRulesXml"].asStringOrNull()
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

    internal fun parseDataExtractionSection(
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

    internal fun parseFullBackupSection(doc: Document, fileName: String): RuleSection? {
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

    private fun JsonElement?.asStringOrNull(): String? = (this as? JsonPrimitive)?.contentOrNull
}
