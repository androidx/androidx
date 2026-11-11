/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.lint

import androidx.credentials.lint.RestoreCredentialConstants.ANDROID_URI
import androidx.credentials.lint.RestoreCredentialConstants.ATTR_ALLOW_BACKUP
import androidx.credentials.lint.RestoreCredentialConstants.ATTR_BACKUP_AGENT
import androidx.credentials.lint.RestoreCredentialConstants.ATTR_FULL_BACKUP_ONLY
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_BACKUP_AGENT
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_CREATE_RESTORE_CREDENTIAL_REQUEST
import androidx.credentials.lint.RestoreCredentialConstants.CLASS_GET_RESTORE_CREDENTIAL_OPTION
import androidx.credentials.lint.RestoreCredentialConstants.RESTORE_CREDENTIALS_GUIDE_URL
import androidx.credentials.lint.RestoreCredentialConstants.TAG_APPLICATION
import androidx.credentials.lint.RestoreCredentialConstants.TAG_MANIFEST
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.PartialResult
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.XmlContext
import com.android.tools.lint.detector.api.XmlScanner
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.w3c.dom.Element

/**
 * Validates `AndroidManifest.xml` `<application>` backup configuration for apps integrating Restore
 * Credentials (`RestoreCredentialManifestBackupConfig`):
 * - Sub-case A: `<application>` declares `android:backupAgent` without
 *   `android:fullBackupOnly="true"`, which disables Android Auto Backup and skips
 *   `BackupAgent.onRestoreFinished()`.
 * - Sub-case B: `<application>` declares `android:backupAgent` while `android:allowBackup="false"`,
 *   which prevents the OS from ever invoking the `BackupAgent`.
 * - Sub-case C: App uses Restore Credentials with `android:allowBackup="true"` (or default) without
 *   a `BackupAgent` overriding `onRestoreFinished()`, missing Tier 1 background session restoration
 *   during device setup.
 */
class RestoreCredentialManifestBackupDetector : Detector(), SourceCodeScanner, XmlScanner {

    override fun getApplicableElements(): Collection<String> = listOf(TAG_APPLICATION)

    override fun visitElement(context: XmlContext, element: Element) {
        if (element.ownerDocument?.documentElement?.tagName != TAG_MANIFEST) return

        val map = context.getPartialResults(ISSUE).map()
        if (context.driver.isSuppressed(context, ISSUE, element)) {
            map.put(KEY_MANIFEST_SUPPRESSED, true)
            return
        }

        val backupAgent = element.getAndroidAttribute(ATTR_BACKUP_AGENT)
        val allowBackup = element.getAndroidAttribute(ATTR_ALLOW_BACKUP)
        val fullBackupOnly = element.getAndroidAttribute(ATTR_FULL_BACKUP_ONLY)

        map.put(KEY_MANIFEST_SEEN, true)
        if (backupAgent.isNotEmpty() || map.getString(KEY_BACKUP_AGENT, null) == null) {
            map.put(KEY_BACKUP_AGENT, backupAgent)
        }
        if (allowBackup.isNotEmpty() || map.getString(KEY_ALLOW_BACKUP, null) == null) {
            map.put(KEY_ALLOW_BACKUP, allowBackup)
        }
        if (fullBackupOnly.isNotEmpty() || map.getString(KEY_FULL_BACKUP_ONLY, null) == null) {
            map.put(KEY_FULL_BACKUP_ONLY, fullBackupOnly)
        }
        if (map.getLocation(KEY_APP_NAME_LOCATION) == null || backupAgent.isNotEmpty()) {
            map.put(KEY_APP_NAME_LOCATION, context.getNameLocation(element))
            map.put(KEY_APP_ELEMENT_LOCATION, context.getElementLocation(element))
        }

        val allowBackupAttrNode =
            element.getAttributeNodeNS(ANDROID_URI, ATTR_ALLOW_BACKUP)
                ?: element.getAttributeNode("android:$ATTR_ALLOW_BACKUP")
        if (allowBackupAttrNode != null) {
            map.put(KEY_ALLOW_BACKUP_LOCATION, context.getLocation(allowBackupAttrNode))
        }
    }

    override fun getApplicableConstructorTypes(): List<String> =
        listOf(CLASS_CREATE_RESTORE_CREDENTIAL_REQUEST, CLASS_GET_RESTORE_CREDENTIAL_OPTION)

    override fun visitConstructor(
        context: JavaContext,
        node: UCallExpression,
        constructor: PsiMethod,
    ) {
        context.getPartialResults(ISSUE).map().put(KEY_USES_RESTORE_CREDENTIALS, true)
    }

    override fun applicableSuperClasses(): List<String> = listOf(CLASS_BACKUP_AGENT)

    override fun visitClass(context: JavaContext, declaration: UClass) {
        val map = context.getPartialResults(ISSUE).map()
        val overridesOnRestoreFinished =
            declaration.javaPsi.findMethodsByName("onRestoreFinished", true).any { method ->
                method.parameterList.parametersCount == 0 &&
                    method.containingClass?.qualifiedName != CLASS_BACKUP_AGENT
            }
        val classKey = declaration.javaPsi.qualifiedName ?: declaration.name ?: return
        if (overridesOnRestoreFinished) {
            map.put(KEY_HAS_ON_RESTORE_FINISHED, true)
            appendClassToken(map, KEY_AGENTS_WITH_RESTORE_FINISHED, classKey)
        } else {
            appendClassToken(map, KEY_AGENTS_WITHOUT_RESTORE_FINISHED, classKey)
        }
    }

    override fun checkPartialResults(context: Context, partialResults: PartialResult) {
        if (context.project.isLibrary) return

        val appMap = partialResults.mapFor(context.project)
        if (appMap.getBoolean(KEY_MANIFEST_SUPPRESSED) == true) return
        if (appMap.getBoolean(KEY_MANIFEST_SEEN) != true) return

        val usesRestoreCredentials =
            partialResults.maps().any { it.getBoolean(KEY_USES_RESTORE_CREDENTIALS) == true }
        if (!usesRestoreCredentials) return

        val agentsWithRestoreFinished =
            partialResults
                .maps()
                .flatMap {
                    it.getString(KEY_AGENTS_WITH_RESTORE_FINISHED, "")
                        .orEmpty()
                        .split(',')
                        .filter(String::isNotEmpty)
                }
                .toSet()
        val agentsWithoutRestoreFinished =
            partialResults
                .maps()
                .flatMap {
                    it.getString(KEY_AGENTS_WITHOUT_RESTORE_FINISHED, "")
                        .orEmpty()
                        .split(',')
                        .filter(String::isNotEmpty)
                }
                .toSet()

        val mergedAppElement = findMergedApplicationElement(context)
        val backupAgent =
            appMap.getString(KEY_BACKUP_AGENT, "").orEmpty().ifEmpty {
                mergedAppElement?.getAndroidAttribute(ATTR_BACKUP_AGENT).orEmpty()
            }
        val hasBackupAgent = backupAgent.isNotEmpty()
        val allowBackup =
            appMap.getString(KEY_ALLOW_BACKUP, "").orEmpty().ifEmpty {
                mergedAppElement?.getAndroidAttribute(ATTR_ALLOW_BACKUP).orEmpty()
            }
        val fullBackupOnly =
            appMap.getString(KEY_FULL_BACKUP_ONLY, "").orEmpty().ifEmpty {
                mergedAppElement?.getAndroidAttribute(ATTR_FULL_BACKUP_ONLY).orEmpty()
            }

        val appNameLocation = appMap.getLocation(KEY_APP_NAME_LOCATION) ?: return
        val appElementLocation = appMap.getLocation(KEY_APP_ELEMENT_LOCATION) ?: appNameLocation
        val allowBackupLocation = appMap.getLocation(KEY_ALLOW_BACKUP_LOCATION) ?: appNameLocation

        // Sub-case B: android:backupAgent declared alongside android:allowBackup="false"
        if (hasBackupAgent && allowBackup.equals("false", ignoreCase = true)) {
            val fix =
                LintFix.create()
                    .name("Set android:allowBackup=\"true\"")
                    .set(ANDROID_URI, ATTR_ALLOW_BACKUP, "true")
                    .range(appElementLocation)
                    .build()
            context.report(
                Incident(
                    ISSUE,
                    allowBackupLocation,
                    "Declaring `android:backupAgent` while `android:allowBackup=\"false\"` prevents " +
                        "Android from invoking `$backupAgent` to restore credentials; set " +
                        "`android:allowBackup=\"true\"`",
                    fix,
                )
            )
            return
        }

        // Sub-case A: android:backupAgent declared without android:fullBackupOnly="true"
        val isDynamicFullBackupOnly =
            fullBackupOnly.startsWith("@") || fullBackupOnly.startsWith("\${")
        if (
            hasBackupAgent &&
                !isDynamicFullBackupOnly &&
                !fullBackupOnly.equals("true", ignoreCase = true)
        ) {
            val fix =
                LintFix.create()
                    .name("Set android:fullBackupOnly=\"true\"")
                    .set(ANDROID_URI, ATTR_FULL_BACKUP_ONLY, "true")
                    .range(appElementLocation)
                    .build()
            context.report(
                Incident(
                    ISSUE,
                    appNameLocation,
                    "Declaring `android:backupAgent` without `android:fullBackupOnly=\"true\"` disables " +
                        "Android Auto Backup and falls back to legacy Key-Value backup, skipping " +
                        "`onRestoreFinished()`; add `android:fullBackupOnly=\"true\"`",
                    fix,
                )
            )
            return
        }

        // Sub-case C: Restore Credentials used with allowBackup="true" (or default) without Tier 1
        // BackupAgent overriding onRestoreFinished().
        val isDynamicAllowBackup = allowBackup.startsWith("@") || allowBackup.startsWith("\${")
        if (!allowBackup.equals("false", ignoreCase = true) && !isDynamicAllowBackup) {
            if (!hasBackupAgent) {
                val allowBackupClause =
                    if (allowBackup.equals("true", ignoreCase = true)) {
                        "with `android:allowBackup=\"true\"` "
                    } else {
                        ""
                    }
                context.report(
                    Incident(
                        ISSUE,
                        appNameLocation,
                        "App uses Restore Credentials ${allowBackupClause}without declaring " +
                            "`android:backupAgent` overriding `BackupAgent.onRestoreFinished()`, missing Tier 1 " +
                            "background sign-in restoration during device setup",
                    )
                )
            } else if (
                !matchesAnyAgent(backupAgent, agentsWithRestoreFinished) &&
                    matchesAnyAgent(backupAgent, agentsWithoutRestoreFinished)
            ) {
                context.report(
                    Incident(
                        ISSUE,
                        appNameLocation,
                        "`android:backupAgent` (`$backupAgent`) does not override " +
                            "`BackupAgent.onRestoreFinished()`, missing Tier 1 background sign-in restoration " +
                            "during device setup",
                    )
                )
            }
        }
    }

    override fun afterCheckRootProject(context: Context) {
        if (context.isGlobalAnalysis() && !context.driver.isIsolated()) {
            checkPartialResults(context, context.getPartialResults(ISSUE))
        }
    }

    private fun appendClassToken(
        map: com.android.tools.lint.detector.api.LintMap,
        key: String,
        value: String,
    ) {
        val existing = map.getString(key, "").orEmpty()
        val updated = if (existing.isEmpty()) value else "$existing,$value"
        map.put(key, updated)
    }

    private fun matchesAnyAgent(manifestAttr: String, candidates: Set<String>): Boolean {
        val normalizedAttr = manifestAttr.removePrefix(".")
        val attrSimple = normalizedAttr.substringAfterLast('.').substringAfterLast('$')
        return candidates.any { candidate ->
            val candidateSimple = candidate.substringAfterLast('.').substringAfterLast('$')
            candidate == normalizedAttr ||
                candidate.endsWith(".$normalizedAttr") ||
                candidateSimple == attrSimple
        }
    }

    private fun findMergedApplicationElement(context: Context): Element? {
        val root = context.project.mergedManifest?.documentElement ?: return null
        val children = root.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node is Element && node.tagName == TAG_APPLICATION) {
                return node
            }
        }
        return null
    }

    private fun Element.getAndroidAttribute(localName: String): String {
        val nsVal = getAttributeNS(ANDROID_URI, localName)
        if (nsVal.isNotEmpty()) return nsVal.trim()
        return getAttribute("android:$localName").trim()
    }

    companion object {
        private const val KEY_MANIFEST_SEEN = "MANIFEST_SEEN"
        private const val KEY_MANIFEST_SUPPRESSED = "MANIFEST_SUPPRESSED"
        private const val KEY_BACKUP_AGENT = "BACKUP_AGENT"
        private const val KEY_ALLOW_BACKUP = "ALLOW_BACKUP"
        private const val KEY_FULL_BACKUP_ONLY = "FULL_BACKUP_ONLY"
        private const val KEY_APP_NAME_LOCATION = "APP_NAME_LOCATION"
        private const val KEY_APP_ELEMENT_LOCATION = "APP_ELEMENT_LOCATION"
        private const val KEY_ALLOW_BACKUP_LOCATION = "ALLOW_BACKUP_LOCATION"
        private const val KEY_USES_RESTORE_CREDENTIALS = "USES_RESTORE_CREDENTIALS"
        private const val KEY_HAS_ON_RESTORE_FINISHED = "HAS_ON_RESTORE_FINISHED"
        private const val KEY_AGENTS_WITH_RESTORE_FINISHED = "AGENTS_WITH_RESTORE_FINISHED"
        private const val KEY_AGENTS_WITHOUT_RESTORE_FINISHED = "AGENTS_WITHOUT_RESTORE_FINISHED"

        private val IMPLEMENTATION =
            Implementation(
                RestoreCredentialManifestBackupDetector::class.java,
                EnumSet.of(Scope.MANIFEST, Scope.JAVA_FILE),
            )

        val ISSUE: Issue =
            Issue.create(
                id = "RestoreCredentialManifestBackupConfig",
                briefDescription =
                    "Misconfigured `AndroidManifest.xml` backup settings for Restore Credentials",
                explanation =
                    """
                Apps using Restore Credentials rely on Android Auto Backup (`android:allowBackup="true"` \
                and `android:fullBackupOnly="true"`) to invoke `BackupAgent.onRestoreFinished()` silently \
                during device setup (Tier 1 background restoration):

                1. Declaring `android:backupAgent` without `android:fullBackupOnly="true"` disables \
                full-data Auto Backup and falls back to legacy Key-Value backup.
                2. Declaring `android:backupAgent` alongside `android:allowBackup="false"` prevents the \
                OS from ever invoking the `BackupAgent`.
                3. Omitting a `BackupAgent` that overrides `onRestoreFinished()` misses Tier 1 \
                background restoration before the user first opens the app.
            """,
                category = Category.CORRECTNESS,
                priority = 7,
                severity = Severity.WARNING,
                implementation = IMPLEMENTATION,
                moreInfo = RESTORE_CREDENTIALS_GUIDE_URL,
                androidSpecific = true,
            )
    }
}
