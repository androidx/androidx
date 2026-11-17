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

    @Test
    fun namedRequestArgAndUnprotectedHelperCall_reportWarningsWhileRetryHelperIsClean() {
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

                fun buildCloudRequest(json: String): CreateRestoreCredentialRequest =
                    CreateRestoreCredentialRequest(json)

                suspend fun retryLocalD2d(manager: CredentialManager, context: Context, json: String) {
                    manager.createCredential(
                        context,
                        CreateRestoreCredentialRequest(json, isCloudBackupEnabled = false),
                    )
                }

                suspend fun compliantWithHelperRetry(context: Context, json: String) {
                    val manager = CredentialManager.create(context)
                    val req = CreateRestoreCredentialRequest(json)
                    try {
                        manager.createCredential(request = req, context = context)
                    } catch (e: E2eeUnavailableException) {
                        retryLocalD2d(manager, context, json)
                    }
                }

                suspend fun missingFallbackWithNamedArg(context: Context, json: String) {
                    val manager = CredentialManager.create(context)
                    val req = CreateRestoreCredentialRequest(json)
                    manager.createCredential(request = req, context = context)
                }

                suspend fun missingFallbackWithHelperRequest(context: Context, json: String) {
                    val manager = CredentialManager.create(context)
                    manager.createCredential(context, buildCloudRequest(json))
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:30: Warning: CreateRestoreCredentialRequest with cloud backup enabled throws E2eeUnavailableException when the device lacks a screen lock or cloud E2EE; catch E2eeUnavailableException and retry with isCloudBackupEnabled = false so local Device-to-Device restore still works [RestoreCredentialMissingE2eeFallback]
                    val req = CreateRestoreCredentialRequest(json)
                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/com/example/test.kt:36: Warning: CreateRestoreCredentialRequest with cloud backup enabled throws E2eeUnavailableException when the device lacks a screen lock or cloud E2EE; catch E2eeUnavailableException and retry with isCloudBackupEnabled = false so local Device-to-Device restore still works [RestoreCredentialMissingE2eeFallback]
                    manager.createCredential(context, buildCloudRequest(json))
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 2 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun runCatchingGenericExceptionNamedCallbackAndDefaultCloudParam_areClean() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.content.Context
                import android.os.CancellationSignal
                import androidx.credentials.CreateCredentialResponse
                import androidx.credentials.CreateRestoreCredentialRequest
                import androidx.credentials.CredentialManager
                import androidx.credentials.CredentialManagerCallback
                import androidx.credentials.exceptions.CreateCredentialException
                import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException
                import java.util.concurrent.Executor

                suspend fun fallbackWithRunCatching(context: Context, json: String) {
                    val manager = CredentialManager.create(context)
                    runCatching {
                        manager.createCredential(context, CreateRestoreCredentialRequest(json))
                    }.onFailure {
                        manager.createCredential(
                            context,
                            CreateRestoreCredentialRequest(json, isCloudBackupEnabled = false),
                        )
                    }
                }

                suspend fun fallbackWithGenericExceptionCatch(context: Context, json: String) {
                    val manager = CredentialManager.create(context)
                    try {
                        manager.createCredential(context, CreateRestoreCredentialRequest(json))
                    } catch (e: Exception) {
                        manager.createCredential(
                            context,
                            CreateRestoreCredentialRequest(json, isCloudBackupEnabled = false),
                        )
                    }
                }

                class NamedRestoreCallback(
                    private val manager: CredentialManager,
                    private val context: Context,
                    private val json: String,
                    private val executor: Executor,
                ) : CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException> {
                    override fun onResult(result: CreateCredentialResponse) {}

                    override fun onError(e: CreateCredentialException) {
                        manager.createCredentialAsync(
                            context,
                            CreateRestoreCredentialRequest(json, isCloudBackupEnabled = false),
                            CancellationSignal(),
                            executor,
                            this,
                        )
                    }
                }

                fun fallbackWithNamedCallback(context: Context, json: String, executor: Executor) {
                    val manager = CredentialManager.create(context)
                    val callback = NamedRestoreCallback(manager, context, json, executor)
                    manager.createCredentialAsync(
                        context,
                        CreateRestoreCredentialRequest(json),
                        CancellationSignal(),
                        executor,
                        callback,
                    )
                }

                private fun makeRestoreRequest(
                    json: String,
                    cloud: Boolean = true,
                ): CreateRestoreCredentialRequest =
                    CreateRestoreCredentialRequest(json, isCloudBackupEnabled = cloud)

                suspend fun fallbackWithDefaultCloudParamHelper(context: Context, json: String) {
                    val manager = CredentialManager.create(context)
                    try {
                        manager.createCredential(context, makeRestoreRequest(json))
                    } catch (e: E2eeUnavailableException) {
                        manager.createCredential(context, makeRestoreRequest(json, cloud = false))
                    }
                }

                suspend fun saveRestoreCredentialWithDefaultParam(
                    manager: CredentialManager,
                    context: Context,
                    json: String,
                    cloud: Boolean = true,
                ) {
                    try {
                        manager.createCredential(
                            context,
                            CreateRestoreCredentialRequest(json, isCloudBackupEnabled = cloud),
                        )
                    } catch (e: E2eeUnavailableException) {
                        saveRestoreCredentialWithDefaultParam(manager, context, json, cloud = false)
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
    fun runCatchingGenericExceptionNamedCallbackAndDefaultCloudParamWithoutFallback_reportWarnings() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.content.Context
                import android.os.CancellationSignal
                import androidx.credentials.CreateCredentialResponse
                import androidx.credentials.CreateRestoreCredentialRequest
                import androidx.credentials.CredentialManager
                import androidx.credentials.CredentialManagerCallback
                import androidx.credentials.exceptions.CreateCredentialException
                import java.util.concurrent.Executor

                suspend fun runCatchingWithoutRetryDespiteLocalElsewhere(
                    context: Context,
                    json: String,
                    localOnly: Boolean,
                ) {
                    val cm = CredentialManager.create(context)
                    if (localOnly) {
                        cm.createCredential(
                            context,
                            CreateRestoreCredentialRequest(json, isCloudBackupEnabled = false),
                        )
                    } else {
                        runCatching {
                            cm.createCredential(context, CreateRestoreCredentialRequest(json))
                        }.onFailure { println(it) }
                    }
                }

                suspend fun genericCatchWithoutRetry(context: Context, json: String) {
                    val cm = CredentialManager.create(context)
                    try {
                        cm.createCredential(context, CreateRestoreCredentialRequest(json))
                    } catch (e: Exception) {
                        println(e)
                    }
                }

                class NoRetryNamedCallback :
                    CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException> {
                    override fun onResult(result: CreateCredentialResponse) {}

                    override fun onError(e: CreateCredentialException) {
                        println(e)
                    }
                }

                fun namedCallbackWithoutRetry(context: Context, json: String, executor: Executor) {
                    val cm = CredentialManager.create(context)
                    cm.createCredentialAsync(
                        context,
                        CreateRestoreCredentialRequest(json),
                        CancellationSignal(),
                        executor,
                        NoRetryNamedCallback(),
                    )
                }

                private fun makeRestoreRequest(
                    json: String,
                    cloud: Boolean = true,
                ): CreateRestoreCredentialRequest =
                    CreateRestoreCredentialRequest(json, isCloudBackupEnabled = cloud)

                suspend fun defaultCloudParamHelperWithoutFallback(context: Context, json: String) {
                    val cm = CredentialManager.create(context)
                    cm.createCredential(context, makeRestoreRequest(json))
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/NoRetryNamedCallback.kt:25: Warning: CreateRestoreCredentialRequest with cloud backup enabled throws E2eeUnavailableException when the device lacks a screen lock or cloud E2EE; catch E2eeUnavailableException and retry with isCloudBackupEnabled = false so local Device-to-Device restore still works [RestoreCredentialMissingE2eeFallback]
                            cm.createCredential(context, CreateRestoreCredentialRequest(json))
                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/com/example/NoRetryNamedCallback.kt:33: Warning: CreateRestoreCredentialRequest with cloud backup enabled throws E2eeUnavailableException when the device lacks a screen lock or cloud E2EE; catch E2eeUnavailableException and retry with isCloudBackupEnabled = false so local Device-to-Device restore still works [RestoreCredentialMissingE2eeFallback]
                        cm.createCredential(context, CreateRestoreCredentialRequest(json))
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/com/example/NoRetryNamedCallback.kt:50: Warning: CreateRestoreCredentialRequest with cloud backup enabled throws E2eeUnavailableException when the device lacks a screen lock or cloud E2EE; catch E2eeUnavailableException and retry with isCloudBackupEnabled = false so local Device-to-Device restore still works [RestoreCredentialMissingE2eeFallback]
                    cm.createCredentialAsync(
                    ^
                src/com/example/NoRetryNamedCallback.kt:67: Warning: CreateRestoreCredentialRequest with cloud backup enabled throws E2eeUnavailableException when the device lacks a screen lock or cloud E2EE; catch E2eeUnavailableException and retry with isCloudBackupEnabled = false so local Device-to-Device restore still works [RestoreCredentialMissingE2eeFallback]
                    cm.createCredential(context, makeRestoreRequest(json))
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 4 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun helperUnrelatedFalseParamAndResultBranchPolarity_areCheckedAccurately() {
        // 1. Clean: cloud = false disables cloud backup even with another boolean param, and
        // runCatching failure retries in the actual failure branch (else of isSuccess, then of
        // !isSuccess / isFailure / exceptionOrNull() is E2eeUnavailableException) are recognized.
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

                private fun makeRestoreRequest(
                    json: String,
                    logMetrics: Boolean = true,
                    cloud: Boolean = true,
                ): CreateRestoreCredentialRequest {
                    if (logMetrics) println("metrics")
                    return CreateRestoreCredentialRequest(json, isCloudBackupEnabled = cloud)
                }

                suspend fun cleanHelperAndResultBranches(context: Context, json: String) {
                    val cm = CredentialManager.create(context)
                    cm.createCredential(
                        context,
                        makeRestoreRequest(json, logMetrics = true, cloud = false),
                    )
                    try {
                        cm.createCredential(context, makeRestoreRequest(json, logMetrics = false))
                    } catch (e: E2eeUnavailableException) {
                        cm.createCredential(context, makeRestoreRequest(json, cloud = false))
                    }

                    val res1 = runCatching {
                        cm.createCredential(context, CreateRestoreCredentialRequest(json))
                    }
                    if (res1.isSuccess) {
                        println("ok")
                    } else {
                        cm.createCredential(
                            context,
                            CreateRestoreCredentialRequest(json, isCloudBackupEnabled = false),
                        )
                    }

                    val res2 = runCatching {
                        cm.createCredential(context, CreateRestoreCredentialRequest(json))
                    }
                    if (!res2.isSuccess) {
                        cm.createCredential(
                            context,
                            CreateRestoreCredentialRequest(json, isCloudBackupEnabled = false),
                        )
                    }

                    val res3 = runCatching {
                        cm.createCredential(context, CreateRestoreCredentialRequest(json))
                    }
                    if (res3.isFailure) {
                        cm.createCredential(
                            context,
                            CreateRestoreCredentialRequest(json, isCloudBackupEnabled = false),
                        )
                    }

                    val res4 = runCatching {
                        cm.createCredential(context, CreateRestoreCredentialRequest(json))
                    }
                    if (res4.exceptionOrNull() is E2eeUnavailableException) {
                        cm.createCredential(
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

        // 2. Flagged: passing false to an unrelated parameter (not isCloudBackupEnabled) does not
        // suppress the warning or satisfy a catch retry, and placing the local retry in the
        // success branch of `if (res.isSuccess)` or `else` of `if (res.isFailure)` is flagged.
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

                private fun makeRestoreRequest(
                    json: String,
                    logMetrics: Boolean = true,
                    cloud: Boolean = true,
                ): CreateRestoreCredentialRequest {
                    if (logMetrics) println("metrics")
                    return CreateRestoreCredentialRequest(json, isCloudBackupEnabled = cloud)
                }

                suspend fun helperWithUnrelatedFalseArgWithoutFallback(
                    context: Context,
                    json: String,
                ) {
                    val cm = CredentialManager.create(context)
                    cm.createCredential(context, makeRestoreRequest(json, logMetrics = false))
                }

                suspend fun helperCatchWithUnrelatedFalseArg(context: Context, json: String) {
                    val cm = CredentialManager.create(context)
                    try {
                        cm.createCredential(context, makeRestoreRequest(json))
                    } catch (e: E2eeUnavailableException) {
                        cm.createCredential(context, makeRestoreRequest(json, logMetrics = false))
                    }
                }

                suspend fun saveWithExtraFlagWrongCatchRetry(
                    cm: CredentialManager,
                    context: Context,
                    json: String,
                    logMetrics: Boolean = true,
                    cloud: Boolean = true,
                ) {
                    if (logMetrics) println("metrics")
                    try {
                        cm.createCredential(
                            context,
                            CreateRestoreCredentialRequest(json, isCloudBackupEnabled = cloud),
                        )
                    } catch (e: E2eeUnavailableException) {
                        saveWithExtraFlagWrongCatchRetry(
                            cm,
                            context,
                            json,
                            logMetrics = false,
                        )
                    }
                }

                suspend fun runCatchingRetryInIsSuccessThenBranch(
                    context: Context,
                    json: String,
                ) {
                    val cm = CredentialManager.create(context)
                    val res = runCatching {
                        cm.createCredential(context, CreateRestoreCredentialRequest(json))
                    }
                    if (res.isSuccess) {
                        cm.createCredential(
                            context,
                            CreateRestoreCredentialRequest(json, isCloudBackupEnabled = false),
                        )
                    }
                }

                suspend fun runCatchingRetryInIsFailureElseBranch(
                    context: Context,
                    json: String,
                ) {
                    val cm = CredentialManager.create(context)
                    val res = runCatching {
                        cm.createCredential(context, CreateRestoreCredentialRequest(json))
                    }
                    if (res.isFailure) {
                        println("error")
                    } else {
                        cm.createCredential(
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
            .expect(
                """
                src/com/example/test.kt:22: Warning: CreateRestoreCredentialRequest with cloud backup enabled throws E2eeUnavailableException when the device lacks a screen lock or cloud E2EE; catch E2eeUnavailableException and retry with isCloudBackupEnabled = false so local Device-to-Device restore still works [RestoreCredentialMissingE2eeFallback]
                    cm.createCredential(context, makeRestoreRequest(json, logMetrics = false))
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/com/example/test.kt:28: Warning: CreateRestoreCredentialRequest with cloud backup enabled throws E2eeUnavailableException when the device lacks a screen lock or cloud E2EE; catch E2eeUnavailableException and retry with isCloudBackupEnabled = false so local Device-to-Device restore still works [RestoreCredentialMissingE2eeFallback]
                        cm.createCredential(context, makeRestoreRequest(json))
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/com/example/test.kt:30: Warning: CreateRestoreCredentialRequest with cloud backup enabled throws E2eeUnavailableException when the device lacks a screen lock or cloud E2EE; catch E2eeUnavailableException and retry with isCloudBackupEnabled = false so local Device-to-Device restore still works [RestoreCredentialMissingE2eeFallback]
                        cm.createCredential(context, makeRestoreRequest(json, logMetrics = false))
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/com/example/test.kt:43: Warning: CreateRestoreCredentialRequest with cloud backup enabled throws E2eeUnavailableException when the device lacks a screen lock or cloud E2EE; catch E2eeUnavailableException and retry with isCloudBackupEnabled = false so local Device-to-Device restore still works [RestoreCredentialMissingE2eeFallback]
                        cm.createCredential(
                        ^
                src/com/example/test.kt:63: Warning: CreateRestoreCredentialRequest with cloud backup enabled throws E2eeUnavailableException when the device lacks a screen lock or cloud E2EE; catch E2eeUnavailableException and retry with isCloudBackupEnabled = false so local Device-to-Device restore still works [RestoreCredentialMissingE2eeFallback]
                        cm.createCredential(context, CreateRestoreCredentialRequest(json))
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/com/example/test.kt:79: Warning: CreateRestoreCredentialRequest with cloud backup enabled throws E2eeUnavailableException when the device lacks a screen lock or cloud E2EE; catch E2eeUnavailableException and retry with isCloudBackupEnabled = false so local Device-to-Device restore still works [RestoreCredentialMissingE2eeFallback]
                        cm.createCredential(context, CreateRestoreCredentialRequest(json))
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 6 warnings
                """
                    .trimIndent()
            )
    }
}
