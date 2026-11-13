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
class RestoreCredentialInvalidCastDetectorTest : LintDetectorTest() {

    override fun getDetector() = RestoreCredentialInvalidCastDetector()

    override fun getIssues() = listOf(RestoreCredentialInvalidCastDetector.ISSUE)

    @Test
    fun castingRestoreResponseToPublicKeyCredential_reportsErrorAndAppliesQuickFix() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.content.Context
                import androidx.credentials.CredentialManager
                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetRestoreCredentialOption
                import androidx.credentials.PublicKeyCredential

                suspend fun restoreSession(context: Context, json: String): String {
                    val manager = CredentialManager.create(context)
                    val request = GetCredentialRequest(listOf(GetRestoreCredentialOption(json)))
                    val response = manager.getCredential(context, request)
                    val cred = response.credential as PublicKeyCredential
                    return cred.authenticationResponseJson
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:13: Error: GetRestoreCredentialOption returns RestoreCredential, not PublicKeyCredential (causes ClassCastException or failed type check at runtime). Use RestoreCredential instead. [RestoreCredentialInvalidCast]
                    val cred = response.credential as PublicKeyCredential
                                                      ~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/com/example/test.kt line 13: Replace 'PublicKeyCredential' with 'RestoreCredential':
                @@ -7 +7
                + import androidx.credentials.RestoreCredential
                @@ -13 +14
                -     val cred = response.credential as PublicKeyCredential
                +     val cred = response.credential as RestoreCredential
                """
                    .trimIndent()
            )
    }

    @Test
    fun whenBranchAndJavaInstanceofTargetingPublicKeyCredential_reportsError() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.content.Context
                import androidx.credentials.CredentialManager
                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetRestoreCredentialOption
                import androidx.credentials.PublicKeyCredential

                suspend fun handleWhen(context: Context, json: String): String? {
                    val manager = CredentialManager.create(context)
                    val response =
                        manager.getCredential(
                            context,
                            GetCredentialRequest(listOf(GetRestoreCredentialOption(json))),
                        )
                    return when (val cred = response.credential) {
                        is PublicKeyCredential -> cred.authenticationResponseJson
                        else -> null
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
                import androidx.credentials.Credential;
                import androidx.credentials.CredentialManager;
                import androidx.credentials.CredentialManagerCallback;
                import androidx.credentials.GetCredentialRequest;
                import androidx.credentials.GetCredentialResponse;
                import androidx.credentials.GetRestoreCredentialOption;
                import androidx.credentials.PublicKeyCredential;
                import androidx.credentials.exceptions.GetCredentialException;
                import java.util.Collections;
                import java.util.concurrent.Executor;

                public final class JavaRestoreCastBug {
                    public void restore(Context context, String json, Executor executor) {
                        CredentialManager manager = CredentialManager.create(context);
                        GetCredentialRequest request =
                                new GetCredentialRequest(
                                        Collections.singletonList(new GetRestoreCredentialOption(json)));
                        manager.getCredentialAsync(
                                context,
                                request,
                                new CancellationSignal(),
                                executor,
                                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                                    @Override
                                    public void onResult(GetCredentialResponse result) {
                                        Credential credential = result.getCredential();
                                        if (credential instanceof PublicKeyCredential) {
                                            PublicKeyCredential pk = (PublicKeyCredential) credential;
                                        }
                                    }

                                    @Override
                                    public void onError(GetCredentialException e) {}
                                });
                    }
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectErrorCount(3)
    }

    @Test
    fun passkeyFlowAndProperRestoreCredentialCast_areClean() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.content.MutableContextWrapper
                import androidx.credentials.CredentialManager
                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetPublicKeyCredentialOption
                import androidx.credentials.GetRestoreCredentialOption
                import androidx.credentials.PublicKeyCredential
                import androidx.credentials.RestoreCredential

                suspend fun signInWithPasskey(
                    context: MutableContextWrapper,
                    json: String,
                ): PublicKeyCredential {
                    val manager = CredentialManager.create(context)
                    val response =
                        manager.getCredential(
                            context,
                            GetCredentialRequest(listOf(GetPublicKeyCredentialOption(json))),
                        )
                    return response.credential as PublicKeyCredential
                }

                suspend fun restoreSession(
                    context: MutableContextWrapper,
                    json: String,
                ): RestoreCredential? {
                    val manager = CredentialManager.create(context)
                    val response =
                        manager.getCredential(
                            context,
                            GetCredentialRequest(listOf(GetRestoreCredentialOption(json))),
                        )
                    return response.credential as? RestoreCredential
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun sharedResponseHelperInViewModelUsingRestoreCredentials_isClean() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.content.Context
                import androidx.credentials.CredentialManager
                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetCredentialResponse
                import androidx.credentials.GetRestoreCredentialOption
                import androidx.credentials.PasswordCredential
                import androidx.credentials.PublicKeyCredential
                import androidx.credentials.RestoreCredential

                class SignInViewModel {
                    suspend fun fetchRestoreResponse(
                        context: Context,
                        json: String,
                    ): GetCredentialResponse {
                        val manager = CredentialManager.create(context)
                        return manager.getCredential(
                            context,
                            GetCredentialRequest(listOf(GetRestoreCredentialOption(json))),
                        )
                    }

                    // Shared helper (or called from UI where Passkey request was built in another class):
                    fun handleSignInResponse(response: GetCredentialResponse) {
                        when (val cred = response.credential) {
                            is PublicKeyCredential -> handlePasskey(cred)
                            is PasswordCredential -> handlePassword(cred)
                            is RestoreCredential -> handleRestore(cred)
                        }
                    }

                    fun handleExternalPasskeyResponse(
                        response: GetCredentialResponse
                    ): PublicKeyCredential? = response.credential as? PublicKeyCredential

                    suspend fun restoreOrInteractiveFallback(
                        context: Context,
                        json: String,
                        interactiveRequest: GetCredentialRequest,
                    ) {
                        val manager = CredentialManager.create(context)
                        val response =
                            try {
                                manager.getCredential(
                                    context,
                                    GetCredentialRequest(listOf(GetRestoreCredentialOption(json))),
                                )
                            } catch (e: Exception) {
                                manager.getCredential(context, interactiveRequest)
                            }
                        when (val cred = response.credential) {
                            is PublicKeyCredential -> handlePasskey(cred)
                            is RestoreCredential -> handleRestore(cred)
                        }
                    }

                    private fun handlePasskey(cred: PublicKeyCredential) {}
                    private fun handlePassword(cred: PasswordCredential) {}
                    private fun handleRestore(cred: RestoreCredential) {}
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun explicitlyTypedVariableCast_replacesBothVariableTypeAndCastType() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.content.Context
                import androidx.credentials.CredentialManager
                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetRestoreCredentialOption
                import androidx.credentials.PublicKeyCredential

                suspend fun restoreTyped(context: Context, json: String): String {
                    val manager = CredentialManager.create(context)
                    val request = GetCredentialRequest(listOf(GetRestoreCredentialOption(json)))
                    val response = manager.getCredential(context, request)
                    val cred: PublicKeyCredential = response.credential as PublicKeyCredential
                    return cred.authenticationResponseJson
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectErrorCount(1)
            .expectFixDiffs(
                """
                Fix for src/com/example/test.kt line 13: Replace 'PublicKeyCredential' with 'RestoreCredential':
                @@ -7 +7
                + import androidx.credentials.RestoreCredential
                @@ -13 +14
                -     val cred: PublicKeyCredential = response.credential as PublicKeyCredential
                +     val cred: RestoreCredential = response.credential as RestoreCredential
                """
                    .trimIndent()
            )
    }

    @Test
    fun unrelatedCastInWhenBranchAndSeparateFallbackVariable_areClean() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.content.Context
                import androidx.credentials.CredentialManager
                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetCredentialResponse
                import androidx.credentials.GetRestoreCredentialOption
                import androidx.credentials.PublicKeyCredential
                import androidx.credentials.RestoreCredential

                suspend fun handleWhenWithUnrelatedBranchCast(
                    context: Context,
                    json: String,
                    otherObj: Any?,
                    interactiveRequest: GetCredentialRequest,
                ) {
                    val manager = CredentialManager.create(context)
                    val response =
                        manager.getCredential(
                            context,
                            GetCredentialRequest(listOf(GetRestoreCredentialOption(json))),
                        )
                    when (val cred = response.credential) {
                        is RestoreCredential -> {
                            val unrelated = otherObj as? PublicKeyCredential
                        }
                    }

                    try {
                        val restoreResp =
                            manager.getCredential(
                                context,
                                GetCredentialRequest(listOf(GetRestoreCredentialOption(json))),
                            )
                        logResponse(restoreResp)
                    } catch (e: Exception) {
                        val interactiveResp = manager.getCredential(context, interactiveRequest)
                        val pk = interactiveResp.credential as? PublicKeyCredential
                    }

                    val onInteractiveResult: (GetCredentialResponse) -> Unit = { lambdaResp ->
                        val lambdaCred = lambdaResp.credential
                        if (lambdaCred is PublicKeyCredential) {
                            val jsonPayload = lambdaCred.authenticationResponseJson
                        }
                    }
                    onInteractiveResult(response)
                }

                private fun logResponse(response: GetCredentialResponse) {}
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun scopeFunctionLambdasOnGetCredentialResult_areFlagged() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.content.Context
                import androidx.credentials.CredentialManager
                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetRestoreCredentialOption
                import androidx.credentials.PublicKeyCredential

                suspend fun handleRestoreWithScopeLambdas(context: Context, json: String) {
                    val cm = CredentialManager.create(context)
                    val request = GetCredentialRequest(listOf(GetRestoreCredentialOption(json)))
                    cm.getCredential(context, request).let {
                        val cred = it.credential as PublicKeyCredential
                    }
                    cm.getCredential(context, request).also { response ->
                        if (response.credential is PublicKeyCredential) {
                            val pk = response.credential
                        }
                    }
                    runCatching { cm.getCredential(context, request) }.onSuccess { response ->
                        val cred = response.credential as PublicKeyCredential
                    }
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:13: Error: GetRestoreCredentialOption returns RestoreCredential, not PublicKeyCredential (causes ClassCastException or failed type check at runtime). Use RestoreCredential instead. [RestoreCredentialInvalidCast]
                        val cred = it.credential as PublicKeyCredential
                                                    ~~~~~~~~~~~~~~~~~~~
                src/com/example/test.kt:16: Error: GetRestoreCredentialOption returns RestoreCredential, not PublicKeyCredential (causes ClassCastException or failed type check at runtime). Use RestoreCredential instead. [RestoreCredentialInvalidCast]
                        if (response.credential is PublicKeyCredential) {
                                                   ~~~~~~~~~~~~~~~~~~~
                src/com/example/test.kt:21: Error: GetRestoreCredentialOption returns RestoreCredential, not PublicKeyCredential (causes ClassCastException or failed type check at runtime). Use RestoreCredential instead. [RestoreCredentialInvalidCast]
                        val cred = response.credential as PublicKeyCredential
                                                          ~~~~~~~~~~~~~~~~~~~
                3 errors, 0 warnings
                """
                    .trimIndent()
            )
    }
}
