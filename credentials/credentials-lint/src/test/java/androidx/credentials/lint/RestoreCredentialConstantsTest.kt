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
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RestoreCredentialConstantsTest : LintDetectorTest() {

    override fun getDetector(): Detector = OptionExtractionProbeDetector()

    override fun getIssues(): List<Issue> = listOf(PROBE_ISSUE)

    @Test
    fun localBuilderFluentChain_extractsSingleOptionWithoutDuplicates() {
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

                suspend fun fetchRestore(context: Context, json: String) {
                    val manager = CredentialManager.create(context)
                    val builder = GetCredentialRequest.Builder()
                    val req = builder.addCredentialOption(GetRestoreCredentialOption(json)).build()
                    manager.getCredential(context, req)
                }
                """
                    )
                    .indented(),
                java(
                        """
                package com.example;

                import android.content.Context;
                import android.os.CancellationSignal;
                import androidx.credentials.CredentialManager;
                import androidx.credentials.CredentialManagerCallback;
                import androidx.credentials.GetCredentialRequest;
                import androidx.credentials.GetCredentialResponse;
                import androidx.credentials.GetRestoreCredentialOption;
                import androidx.credentials.exceptions.GetCredentialException;
                import java.util.concurrent.Executor;

                public final class JavaLocalBuilder {
                    public void fetchRestore(
                            Context context,
                            String json,
                            Executor executor,
                            CredentialManagerCallback<GetCredentialResponse, GetCredentialException> cb) {
                        CredentialManager manager = CredentialManager.create(context);
                        GetCredentialRequest.Builder builder = new GetCredentialRequest.Builder();
                        GetCredentialRequest req =
                                builder.addCredentialOption(new GetRestoreCredentialOption(json)).build();
                        manager.getCredentialAsync(context, req, new CancellationSignal(), executor, cb);
                    }
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/JavaLocalBuilder.java:23: Warning: Extracted 1 option(s): androidx.credentials.GetRestoreCredentialOption [OptionExtractionProbe]
                        manager.getCredentialAsync(context, req, new CancellationSignal(), executor, cb);
                                ~~~~~~~~~~~~~~~~~~
                src/com/example/test.kt:12: Warning: Extracted 1 option(s): androidx.credentials.GetRestoreCredentialOption [OptionExtractionProbe]
                    manager.getCredential(context, req)
                            ~~~~~~~~~~~~~
                0 errors, 2 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun localBuilderMixedImperativeAndFluentCalls_preservesOrderWithoutDuplicates() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.content.Context
                import androidx.credentials.CredentialManager
                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetPasswordOption
                import androidx.credentials.GetPublicKeyCredentialOption
                import androidx.credentials.GetRestoreCredentialOption

                suspend fun fetchMixed(context: Context, json: String) {
                    val manager = CredentialManager.create(context)
                    val builder =
                        GetCredentialRequest.Builder().addCredentialOption(GetRestoreCredentialOption(json))
                    builder.addCredentialOption(GetPasswordOption())
                    val req = builder.addCredentialOption(GetPublicKeyCredentialOption(json)).build()
                    manager.getCredential(context, req)
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:16: Warning: Extracted 3 option(s): androidx.credentials.GetRestoreCredentialOption, androidx.credentials.GetPasswordOption, androidx.credentials.GetPublicKeyCredentialOption [OptionExtractionProbe]
                    manager.getCredential(context, req)
                            ~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun localBuilderSetCredentialOptions_clearsPreviouslyAddedOptionsInSourceOrder() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.content.Context
                import androidx.credentials.CredentialManager
                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetPasswordOption
                import androidx.credentials.GetRestoreCredentialOption

                suspend fun fetchReplaced(context: Context, json: String) {
                    val manager = CredentialManager.create(context)
                    val builder = GetCredentialRequest.Builder()
                    builder.addCredentialOption(GetPasswordOption())
                    val restoreReq =
                        builder.setCredentialOptions(listOf(GetRestoreCredentialOption(json))).build()
                    manager.getCredential(context, restoreReq)

                    val fallbackReq =
                        builder.setCredentialOptions(listOf(GetPasswordOption())).build()
                    manager.getCredential(context, fallbackReq)
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:15: Warning: Extracted 1 option(s): androidx.credentials.GetRestoreCredentialOption [OptionExtractionProbe]
                    manager.getCredential(context, restoreReq)
                            ~~~~~~~~~~~~~
                src/com/example/test.kt:19: Warning: Extracted 1 option(s): androidx.credentials.GetPasswordOption [OptionExtractionProbe]
                    manager.getCredential(context, fallbackReq)
                            ~~~~~~~~~~~~~
                0 errors, 2 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun mutableListClearPlusAssignCreateFromAndReorderedNamedArgs_extractsCorrectOptions() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import android.content.Context
                import androidx.credentials.CredentialManager
                import androidx.credentials.CredentialOption
                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetPasswordOption
                import androidx.credentials.GetRestoreCredentialOption

                suspend fun testListMutationsAndNamedArgs(context: Context, json: String) {
                    val manager = CredentialManager.create(context)
                    val options = mutableListOf<CredentialOption>()
                    options += GetRestoreCredentialOption(json)
                    val firstReq =
                        GetCredentialRequest(
                            origin = "com.example",
                            credentialOptions = options.toList(),
                        )
                    manager.getCredential(request = firstReq, context = context)

                    options.clear()
                    options.add(0, GetPasswordOption())
                    val secondReq = GetCredentialRequest.createFrom(options)
                    manager.getCredential(context, secondReq)
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:19: Warning: Extracted 1 option(s): androidx.credentials.GetRestoreCredentialOption [OptionExtractionProbe]
                    manager.getCredential(request = firstReq, context = context)
                            ~~~~~~~~~~~~~
                src/com/example/test.kt:24: Warning: Extracted 1 option(s): androidx.credentials.GetPasswordOption [OptionExtractionProbe]
                    manager.getCredential(context, secondReq)
                            ~~~~~~~~~~~~~
                0 errors, 2 warnings
                """
                    .trimIndent()
            )
    }

    class OptionExtractionProbeDetector : Detector(), SourceCodeScanner {
        override fun getApplicableMethodNames(): List<String> =
            listOf("getCredential", "getCredentialAsync")

        override fun visitMethodCall(
            context: JavaContext,
            node: UCallExpression,
            method: PsiMethod,
        ) {
            if (
                !context.evaluator.isMemberInClass(
                    method,
                    RestoreCredentialConstants.CLASS_CREDENTIAL_MANAGER,
                )
            ) {
                return
            }
            val requestArg =
                RestoreCredentialConstants.findArgument(context, node, method, "request", 1)
                    ?: return
            val optionTypes =
                RestoreCredentialConstants.extractCredentialOptionExpressions(context, requestArg)
                    .map { expr ->
                        RestoreCredentialConstants.resolveExpressionClass(context, expr)
                            ?.qualifiedName ?: "<unresolved>"
                    }
            context.report(
                PROBE_ISSUE,
                node,
                context.getNameLocation(node),
                "Extracted ${optionTypes.size} option(s): ${optionTypes.joinToString()}",
            )
        }
    }

    companion object {
        private val PROBE_ISSUE =
            Issue.create(
                id = "OptionExtractionProbe",
                briefDescription = "Test probe for RestoreCredentialConstants option extraction",
                explanation = "Reports the CredentialOption types extracted from a request.",
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.WARNING,
                implementation =
                    Implementation(
                        OptionExtractionProbeDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                    ),
            )
    }
}
