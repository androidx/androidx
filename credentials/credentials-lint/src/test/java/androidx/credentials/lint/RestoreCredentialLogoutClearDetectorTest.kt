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

import androidx.credentials.lint.stubs.CREDENTIALS_STUBS
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RestoreCredentialLogoutClearDetectorTest : LintDetectorTest() {

    override fun getDetector() = RestoreCredentialLogoutClearDetector()

    override fun getIssues() = listOf(RestoreCredentialLogoutClearDetector.ISSUE)

    @Test
    fun defaultClearWithoutRestoreClearInRestoreEnabledApp_warnsAndAppendsRestoreClear() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        "src/com/example/SignInManager.kt",
                        """
                package com.example

                import android.content.Context
                import androidx.credentials.CreateRestoreCredentialRequest
                import androidx.credentials.CredentialManager

                suspend fun persistRestoreKey(context: Context, json: String) {
                    CredentialManager.create(context)
                        .createCredential(
                            context,
                            CreateRestoreCredentialRequest(json, isCloudBackupEnabled = false),
                        )
                }
                """,
                    )
                    .indented(),
                kotlin(
                        "src/com/example/SignOutManager.kt",
                        """
                package com.example

                import androidx.credentials.ClearCredentialStateRequest
                import androidx.credentials.CredentialManager

                suspend fun onUserSignOut(credentialManager: CredentialManager) {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                }
                """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/SignOutManager.kt:7: Warning: Default ClearCredentialStateRequest() does not delete the stored RestoreCredential key; also call clearCredentialState(ClearCredentialStateRequest(TYPE_CLEAR_RESTORE_CREDENTIAL)) on sign-out to prevent post-logout auto-login [RestoreCredentialMissingLogoutClear]
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/com/example/SignOutManager.kt line 7: Also clear RestoreCredential state (TYPE_CLEAR_RESTORE_CREDENTIAL):
                @@ -7,0 +8 @@
                +    credentialManager.clearCredentialState(ClearCredentialStateRequest(ClearCredentialStateRequest.TYPE_CLEAR_RESTORE_CREDENTIAL))
                """
                    .trimIndent()
            )
    }

    @Test
    fun clearingBothTypes_isClean() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.content.Context
                import androidx.credentials.ClearCredentialStateRequest
                import androidx.credentials.ClearCredentialStateRequest.Companion.TYPE_CLEAR_RESTORE_CREDENTIAL
                import androidx.credentials.CreateRestoreCredentialRequest
                import androidx.credentials.CredentialManager

                suspend fun onSignIn(context: Context, json: String) {
                    CredentialManager.create(context)
                        .createCredential(
                            context,
                            CreateRestoreCredentialRequest(json, isCloudBackupEnabled = false),
                        )
                }

                suspend fun onSignOut(credentialManager: CredentialManager) {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    credentialManager.clearCredentialState(
                        ClearCredentialStateRequest(TYPE_CLEAR_RESTORE_CREDENTIAL)
                    )
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun passkeyOnlyAppWithoutRestoreCredentials_isClean() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import androidx.credentials.ClearCredentialStateRequest
                import androidx.credentials.CredentialManager

                suspend fun onPasskeySignOut(credentialManager: CredentialManager) {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun createRestoreCredentialWithoutAnyClearCredentialState_warnsAtCreationSite() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        "src/com/example/SignInManager.kt",
                        """
                package com.example

                import android.content.Context
                import androidx.credentials.CreateRestoreCredentialRequest
                import androidx.credentials.CredentialManager

                suspend fun persistRestoreKey(context: Context, json: String) {
                    CredentialManager.create(context)
                        .createCredential(
                            context,
                            CreateRestoreCredentialRequest(json, isCloudBackupEnabled = false),
                        )
                }
                """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/SignInManager.kt:11: Warning: App creates a RestoreCredential but never calls CredentialManager.clearCredentialState(ClearCredentialStateRequest(TYPE_CLEAR_RESTORE_CREDENTIAL)) on sign-out, which leaves the restore key active after logout [RestoreCredentialMissingLogoutClear]
                            CreateRestoreCredentialRequest(json, isCloudBackupEnabled = false),
                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
    }
}
