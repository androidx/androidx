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
class RestoreCredentialE2eeFallbackDetectorTest : LintDetectorTest() {

    override fun getDetector() = RestoreCredentialE2eeFallbackDetector()

    override fun getIssues() = listOf(RestoreCredentialE2eeFallbackDetector.ISSUE)

    @Test
    fun defaultCloudBackupWithoutTryCatch_warnsAndWrapsInE2eeFallback() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.content.Context
                import androidx.credentials.CreateCredentialResponse
                import androidx.credentials.CreateRestoreCredentialRequest
                import androidx.credentials.CredentialManager

                suspend fun registerRestoreKey(
                    context: Context,
                    requestJson: String,
                ): CreateCredentialResponse {
                    val manager = CredentialManager.create(context)
                    return manager.createCredential(
                        context,
                        CreateRestoreCredentialRequest(requestJson),
                    )
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectWarningCount(1)
            .expectFixDiffs(
                """
                Fix for src/com/example/test.kt line 13: Wrap in try/catch (E2eeUnavailableException) with local D2D fallback:
                @@ -6,0 +7 @@
                +import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException
                @@ -13 +14,2 @@
                -    return manager.createCredential(
                +    return try {
                +    manager.createCredential(
                @@ -15,0 +18,5 @@
                +    )
                +} catch (e: E2eeUnavailableException) {
                +    manager.createCredential(
                +        context,
                +        CreateRestoreCredentialRequest(requestJson, isCloudBackupEnabled = false),
                @@ -16,0 +24 @@
                +}
                """
                    .trimIndent()
            )
    }

    @Test
    fun catchWithoutFallbackRetryAndUncheckedJavaAsync_reportWarnings() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.content.Context
                import androidx.credentials.CreateRestoreCredentialRequest
                import androidx.credentials.CredentialManager
                import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException

                suspend fun swallowsExceptionWithoutLocalFallback(context: Context, json: String) {
                    val manager = CredentialManager.create(context)
                    try {
                        manager.createCredential(
                            context,
                            CreateRestoreCredentialRequest(json, isCloudBackupEnabled = true),
                        )
                    } catch (e: E2eeUnavailableException) {
                        // Swallowed without retrying isCloudBackupEnabled = false!
                    }
                }
                """
                    )
                    .indented(),
                java(
                        """
                package com.example;

                import android.content.Context;
                import android.os.CancellationSignal;
                import androidx.credentials.CreateCredentialResponse;
                import androidx.credentials.CreateRestoreCredentialRequest;
                import androidx.credentials.CredentialManager;
                import androidx.credentials.CredentialManagerCallback;
                import androidx.credentials.exceptions.CreateCredentialException;
                import java.util.concurrent.Executor;

                public final class JavaMissingFallback {
                    public void create(Context context, String json, Executor executor) {
                        CredentialManager manager = CredentialManager.create(context);
                        manager.createCredentialAsync(
                                context,
                                new CreateRestoreCredentialRequest(json),
                                new CancellationSignal(),
                                executor,
                                new CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>() {
                                    @Override
                                    public void onResult(CreateCredentialResponse result) {}

                                    @Override
                                    public void onError(CreateCredentialException e) {}
                                });
                    }
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectWarningCount(2)
    }

    @Test
    fun tryCatchRetryLocalOnlyAndJavaOnErrorFallback_areCompliant() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.content.Context
                import androidx.credentials.CreateRestoreCredentialRequest
                import androidx.credentials.CredentialManager
                import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException

                suspend fun registerWithFallback(context: Context, json: String) {
                    val manager = CredentialManager.create(context)
                    val cloudRequest = CreateRestoreCredentialRequest(json)
                    try {
                        manager.createCredential(context, cloudRequest)
                    } catch (e: E2eeUnavailableException) {
                        manager.createCredential(
                            context,
                            CreateRestoreCredentialRequest(json, isCloudBackupEnabled = false),
                        )
                    }
                }

                suspend fun registerLocalD2dOnly(context: Context, json: String) {
                    CredentialManager.create(context)
                        .createCredential(
                            context,
                            CreateRestoreCredentialRequest(json, isCloudBackupEnabled = false),
                        )
                }
                """
                    )
                    .indented(),
                java(
                        """
                package com.example;

                import android.content.Context;
                import android.os.CancellationSignal;
                import androidx.credentials.CreateCredentialResponse;
                import androidx.credentials.CreateRestoreCredentialRequest;
                import androidx.credentials.CredentialManager;
                import androidx.credentials.CredentialManagerCallback;
                import androidx.credentials.exceptions.CreateCredentialException;
                import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException;
                import java.util.concurrent.Executor;

                public final class JavaCompliantFallback {
                    public void create(Context context, String json, Executor executor) {
                        CredentialManager manager = CredentialManager.create(context);
                        manager.createCredentialAsync(
                                context,
                                new CreateRestoreCredentialRequest(json, true),
                                new CancellationSignal(),
                                executor,
                                new CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>() {
                                    @Override
                                    public void onResult(CreateCredentialResponse result) {}

                                    @Override
                                    public void onError(CreateCredentialException e) {
                                        if (e instanceof E2eeUnavailableException) {
                                            manager.createCredentialAsync(
                                                    context,
                                                    new CreateRestoreCredentialRequest(json, false),
                                                    new CancellationSignal(),
                                                    executor,
                                                    this);
                                        }
                                    }
                                });
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
    fun requestBuiltInHelperMethodAndHandledAtCallSite_isClean() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.content.Context
                import androidx.credentials.CreateRestoreCredentialRequest
                import androidx.credentials.CredentialManager
                import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException

                fun buildCloudRestoreRequest(json: String): CreateRestoreCredentialRequest =
                    CreateRestoreCredentialRequest(json)

                suspend fun registerUsingHelper(context: Context, json: String) {
                    val manager = CredentialManager.create(context)
                    try {
                        manager.createCredential(context, buildCloudRestoreRequest(json))
                    } catch (e: E2eeUnavailableException) {
                        manager.createCredential(
                            context,
                            CreateRestoreCredentialRequest(json, isCloudBackupEnabled = false),
                        )
                    }
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }
}
