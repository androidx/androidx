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

import androidx.credentials.lint.stubs.COROUTINES_STUB
import androidx.credentials.lint.stubs.CREDENTIALS_STUBS
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.ProjectDescription
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RestoreCredentialManifestBackupDetectorTest : LintDetectorTest() {

    override fun getDetector() = RestoreCredentialManifestBackupDetector()

    override fun getIssues() = listOf(RestoreCredentialManifestBackupDetector.ISSUE)

    private val restoreBackupAgentSource =
        kotlin(
                """
            package com.example

            import android.app.backup.BackupAgent
            import android.app.backup.BackupDataInput
            import android.app.backup.BackupDataOutput
            import android.os.ParcelFileDescriptor
            import androidx.credentials.CredentialManager
            import androidx.credentials.GetCredentialRequest
            import androidx.credentials.GetRestoreCredentialOption
            import kotlinx.coroutines.runBlocking

            class AppBackupAgent : BackupAgent() {
                override fun onBackup(
                    oldState: ParcelFileDescriptor?,
                    data: BackupDataOutput?,
                    newState: ParcelFileDescriptor?,
                ) {}

                override fun onRestore(
                    data: BackupDataInput?,
                    appVersionCode: Int,
                    newState: ParcelFileDescriptor?,
                ) {}

                override fun onRestoreFinished() {
                    val manager = CredentialManager.create(this)
                    val request = GetCredentialRequest(listOf(GetRestoreCredentialOption("{}")))
                    runBlocking { manager.getCredential(this@AppBackupAgent, request) }
                }
            }
            """
            )
            .indented()

    @Test
    fun backupAgentWithoutFullBackupOnly_reportsSubCaseAWithSetAttributeFix() {
        lint()
            .files(
                manifest(
                        """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="com.example">
                    <application
                        android:allowBackup="true"
                        android:backupAgent=".AppBackupAgent" />
                </manifest>
                """
                    )
                    .indented(),
                *CREDENTIALS_STUBS,
                COROUTINES_STUB,
                restoreBackupAgentSource,
            )
            .run()
            .expect(
                """
                AndroidManifest.xml:3: Warning: Declaring android:backupAgent without android:fullBackupOnly="true" disables Android Auto Backup and falls back to legacy Key-Value backup, skipping onRestoreFinished(); add android:fullBackupOnly="true" [RestoreCredentialManifestBackupConfig]
                    <application
                     ~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for AndroidManifest.xml line 3: Set android:fullBackupOnly="true":
                @@ -7 +7,2 @@
                -        android:backupAgent=".AppBackupAgent" />
                +        android:backupAgent=".AppBackupAgent"
                +        android:fullBackupOnly="true" />
                """
                    .trimIndent()
            )
    }

    @Test
    fun backupAgentWithAllowBackupFalse_reportsSubCaseBWithSetAttributeFix() {
        lint()
            .files(
                manifest(
                        """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="com.example">
                    <application
                        android:allowBackup="false"
                        android:fullBackupOnly="true"
                        android:backupAgent=".AppBackupAgent" />
                </manifest>
                """
                    )
                    .indented(),
                *CREDENTIALS_STUBS,
                COROUTINES_STUB,
                restoreBackupAgentSource,
            )
            .run()
            .expect(
                """
                AndroidManifest.xml:4: Warning: Declaring android:backupAgent while android:allowBackup="false" prevents Android from invoking .AppBackupAgent to restore credentials; set android:allowBackup="true" [RestoreCredentialManifestBackupConfig]
                        android:allowBackup="false"
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for AndroidManifest.xml line 4: Set android:allowBackup="true":
                @@ -6 +6 @@
                -        android:allowBackup="false"
                +        android:allowBackup="true"
                """
                    .trimIndent()
            )
    }

    @Test
    fun restoreCredentialsWithoutBackupAgent_reportsSubCaseCMissingTier1Restore() {
        lint()
            .files(
                manifest(
                        """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="com.example">
                    <application android:allowBackup="true" />
                </manifest>
                """
                    )
                    .indented(),
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.content.Context
                import androidx.credentials.CreateRestoreCredentialRequest
                import androidx.credentials.CredentialManager

                suspend fun registerRestoreKey(context: Context, json: String) {
                    CredentialManager.create(context)
                        .createCredential(
                            context,
                            CreateRestoreCredentialRequest(json, isCloudBackupEnabled = false),
                        )
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                AndroidManifest.xml:3: Warning: App uses Restore Credentials with android:allowBackup="true" without declaring android:backupAgent overriding BackupAgent.onRestoreFinished(), missing Tier 1 background sign-in restoration during device setup [RestoreCredentialManifestBackupConfig]
                    <application android:allowBackup="true" />
                     ~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun compliantManifest_reportsNoFindings() {
        lint()
            .files(
                manifest(
                        """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="com.example">
                    <application
                        android:allowBackup="true"
                        android:fullBackupOnly="true"
                        android:backupAgent=".AppBackupAgent" />
                </manifest>
                """
                    )
                    .indented(),
                *CREDENTIALS_STUBS,
                COROUTINES_STUB,
                restoreBackupAgentSource,
            )
            .run()
            .expectClean()
    }

    @Test
    fun libraryModule_reportsNoFindings() {
        val libProject =
            ProjectDescription(
                    manifest(
                            """
                    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                        package="com.example.lib">
                        <application />
                    </manifest>
                    """
                        )
                        .indented(),
                    *CREDENTIALS_STUBS,
                    kotlin(
                            """
                    package com.example.lib

                    import androidx.credentials.GetRestoreCredentialOption

                    fun createOption(json: String) = GetRestoreCredentialOption(json)
                    """
                        )
                        .indented(),
                )
                .type(ProjectDescription.Type.LIBRARY)

        lint().projects(libProject).run().expectClean()
    }

    @Test
    fun nonRestoreAppWithBackupAgentOverridingOnRestoreFinished_reportsNoFindings() {
        lint()
            .files(
                manifest(
                        """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="com.example">
                    <application
                        android:allowBackup="true"
                        android:backupAgent=".PasskeyAppBackupAgent" />
                </manifest>
                """
                    )
                    .indented(),
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.app.backup.BackupAgent
                import android.app.backup.BackupDataInput
                import android.app.backup.BackupDataOutput
                import android.content.MutableContextWrapper
                import android.os.ParcelFileDescriptor
                import androidx.credentials.CredentialManager
                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetPublicKeyCredentialOption

                class PasskeyAppBackupAgent : BackupAgent() {
                    override fun onBackup(
                        oldState: ParcelFileDescriptor?,
                        data: BackupDataOutput?,
                        newState: ParcelFileDescriptor?,
                    ) {}

                    override fun onRestore(
                        data: BackupDataInput?,
                        appVersionCode: Int,
                        newState: ParcelFileDescriptor?,
                    ) {}

                    override fun onRestoreFinished() {}
                }

                suspend fun signInWithPasskeyOnly(context: MutableContextWrapper, json: String) {
                    val manager = CredentialManager.create(context)
                    manager.getCredential(
                        context,
                        GetCredentialRequest(listOf(GetPublicKeyCredentialOption(json))),
                    )
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }
}
