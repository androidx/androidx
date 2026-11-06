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
class CredentialManagerMutableContextDetectorTest : LintDetectorTest() {

    override fun getDetector() = CredentialManagerMutableContextDetector()

    override fun getIssues() = listOf(CredentialManagerMutableContextDetector.ISSUE)

    private val credentialManagerStub =
        kotlin(
            """
        package androidx.credentials
        import android.content.Context

        class CredentialManager {
            fun getCredential(context: Context, request: Any) {}
        }
        """
        )

    @Test
    fun testRawActivityContextThrowsError() {
        lint()
            .files(
                credentialManagerStub,
                kotlin(
                        """
                package com.example
                import androidx.credentials.CredentialManager
                import android.app.Activity

                fun doLogin(activity: Activity) {
                    val manager = CredentialManager()
                    manager.getCredential(activity, Any())
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:7: Warning: Use a MutableContextWrapper instead of a raw Activity for credential operations to properly handle activity configuration changes. [CredManMutableContext]
                    manager.getCredential(activity, Any())
                                          ~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testMutableContextWrapperPasses() {
        lint()
            .files(
                credentialManagerStub,
                kotlin(
                        """
                package com.example
                import androidx.credentials.CredentialManager
                import android.app.Activity
                import android.content.MutableContextWrapper

                fun doLogin(activity: Activity) {
                    val manager = CredentialManager()
                    val wrapper = MutableContextWrapper(activity)
                    manager.getCredential(wrapper, Any())
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun testMutableContextWrapperAsGenericContextPasses() {
        lint()
            .files(
                credentialManagerStub,
                kotlin(
                        """
                package com.example
                import androidx.credentials.CredentialManager
                import android.app.Activity
                import android.content.Context
                import android.content.MutableContextWrapper

                fun doLogin(activity: Activity) {
                    val manager = CredentialManager()
                    val context: Context = MutableContextWrapper(activity)
                    manager.getCredential(context, Any())
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun testRawActivityContextInJavaAsyncThrowsWarning() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                java(
                        """
                package com.example;

                import android.app.Activity;
                import android.os.CancellationSignal;
                import androidx.credentials.CredentialManager;
                import androidx.credentials.CredentialManagerCallback;
                import androidx.credentials.GetCredentialRequest;
                import androidx.credentials.GetCredentialResponse;
                import androidx.credentials.GetPasswordOption;
                import androidx.credentials.exceptions.GetCredentialException;
                import java.util.Collections;
                import java.util.concurrent.Executor;

                public final class JavaAuthHelper {
                    public void signIn(
                            Activity activity,
                            Executor executor,
                            CredentialManagerCallback<GetCredentialResponse, GetCredentialException> cb) {
                        CredentialManager manager = CredentialManager.create(activity);
                        GetCredentialRequest request =
                                new GetCredentialRequest(Collections.singletonList(new GetPasswordOption()));
                        manager.getCredentialAsync(activity, request, new CancellationSignal(), executor, cb);
                    }
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/JavaAuthHelper.java:22: Warning: Use a MutableContextWrapper instead of a raw Activity for credential operations to properly handle activity configuration changes. [CredManMutableContext]
                        manager.getCredentialAsync(activity, request, new CancellationSignal(), executor, cb);
                                                   ~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testBackupAgentContextIsExempt() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.app.backup.BackupAgent
                import android.app.backup.BackupDataInput
                import android.app.backup.BackupDataOutput
                import android.os.ParcelFileDescriptor
                import androidx.credentials.CredentialManager
                import androidx.credentials.GetCredentialRequest

                class MyBackupAgent : BackupAgent() {
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

                    suspend fun restoreFromOpaqueRequest(request: GetCredentialRequest) {
                        val manager = CredentialManager.create(this)
                        manager.getCredential(this, request)
                    }
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun testNonUiRestoreCredentialCallsWithRawContextAreExempt() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.app.Activity
                import android.content.Context
                import androidx.credentials.CreateRestoreCredentialRequest
                import androidx.credentials.CredentialManager
                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetRestoreCredentialOption

                suspend fun saveAndRestore(activity: Activity, appContext: Context, json: String) {
                    val manager = CredentialManager.create(appContext)
                    manager.createCredential(activity, CreateRestoreCredentialRequest(json))

                    val createReq = CreateRestoreCredentialRequest(json, isCloudBackupEnabled = false)
                    manager.createCredential(request = createReq, context = appContext)

                    val getReq = GetCredentialRequest(listOf(GetRestoreCredentialOption(json)))
                    manager.getCredential(activity, getReq)

                    val builtReq =
                        GetCredentialRequest.Builder()
                            .addCredentialOption(GetRestoreCredentialOption(json))
                            .build()
                    manager.getCredential(appContext, builtReq)
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun testLocalBuilderVariableChainedRestoreOptionWithRawActivityIsExempt() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.app.Activity
                import androidx.credentials.CredentialManager
                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetRestoreCredentialOption

                suspend fun restoreWithLocalBuilder(activity: Activity, json: String) {
                    val manager = CredentialManager.create(activity)
                    val builder = GetCredentialRequest.Builder()
                    val req = builder.addCredentialOption(GetRestoreCredentialOption(json)).build()
                    manager.getCredential(activity, req)
                }
                """
                    )
                    .indented(),
                java(
                        """
                package com.example;

                import android.app.Activity;
                import android.os.CancellationSignal;
                import androidx.credentials.CredentialManager;
                import androidx.credentials.CredentialManagerCallback;
                import androidx.credentials.GetCredentialRequest;
                import androidx.credentials.GetCredentialResponse;
                import androidx.credentials.GetRestoreCredentialOption;
                import androidx.credentials.exceptions.GetCredentialException;
                import java.util.concurrent.Executor;

                public final class JavaLocalBuilderRestore {
                    public void restore(
                            Activity activity,
                            String json,
                            Executor executor,
                            CredentialManagerCallback<GetCredentialResponse, GetCredentialException> cb) {
                        CredentialManager manager = CredentialManager.create(activity);
                        GetCredentialRequest.Builder builder = new GetCredentialRequest.Builder();
                        GetCredentialRequest req =
                                builder.addCredentialOption(new GetRestoreCredentialOption(json)).build();
                        manager.getCredentialAsync(activity, req, new CancellationSignal(), executor, cb);
                    }
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun testLocalBuilderSetCredentialOptionsOverridesPriorOption() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.app.Activity
                import androidx.credentials.CredentialManager
                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetPasswordOption
                import androidx.credentials.GetRestoreCredentialOption

                suspend fun restoreAndFallback(activity: Activity, json: String) {
                    val manager = CredentialManager.create(activity)
                    val builder = GetCredentialRequest.Builder()
                    builder.addCredentialOption(GetPasswordOption())
                    val restoreOnlyReq =
                        builder.setCredentialOptions(listOf(GetRestoreCredentialOption(json))).build()
                    manager.getCredential(activity, restoreOnlyReq)

                    val interactiveFallbackReq =
                        builder.setCredentialOptions(listOf(GetPasswordOption())).build()
                    manager.getCredential(activity, interactiveFallbackReq)
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:19: Warning: Use a MutableContextWrapper instead of a raw Activity for credential operations to properly handle activity configuration changes. [CredManMutableContext]
                    manager.getCredential(activity, interactiveFallbackReq)
                                          ~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testMixedRestoreAndInteractiveOptionsWithRawActivityStillWarns() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.app.Activity
                import androidx.credentials.CredentialManager
                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetPasswordOption
                import androidx.credentials.GetRestoreCredentialOption

                suspend fun mixedCall(activity: Activity, json: String) {
                    val manager = CredentialManager.create(activity)
                    val request =
                        GetCredentialRequest(
                            listOf(GetRestoreCredentialOption(json), GetPasswordOption())
                        )
                    manager.getCredential(activity, request)
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:15: Warning: Use a MutableContextWrapper instead of a raw Activity for credential operations to properly handle activity configuration changes. [CredManMutableContext]
                    manager.getCredential(activity, request)
                                          ~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
    }
}
