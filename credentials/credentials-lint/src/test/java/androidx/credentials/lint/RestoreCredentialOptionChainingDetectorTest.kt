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
class RestoreCredentialOptionChainingDetectorTest : LintDetectorTest() {

    override fun getDetector() = RestoreCredentialOptionChainingDetector()

    override fun getIssues() = listOf(RestoreCredentialOptionChainingDetector.ISSUE)

    @Test
    fun constructorCombiningRestoreAndPasskeyOptions_reportsError() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetPublicKeyCredentialOption
                import androidx.credentials.GetRestoreCredentialOption

                fun buildChainedRequest(authJson: String): GetCredentialRequest {
                    return GetCredentialRequest(
                        listOf(
                            GetRestoreCredentialOption(authJson),
                            GetPublicKeyCredentialOption(authJson),
                        )
                    )
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:10: Error: GetRestoreCredentialOption cannot be combined with other CredentialOption entries in a single GetCredentialRequest (throws IllegalArgumentException at runtime). Request GetRestoreCredentialOption alone first, then fall back to interactive sign-in options. [RestoreCredentialOptionChaining]
                            GetRestoreCredentialOption(authJson),
                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun builderChainingRestoreWithPasswordInJava_reportsError() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                java(
                        """
                package com.example;

                import androidx.credentials.GetCredentialRequest;
                import androidx.credentials.GetPasswordOption;
                import androidx.credentials.GetRestoreCredentialOption;

                public final class JavaBuilderChaining {
                    public GetCredentialRequest create(String authJson) {
                        return new GetCredentialRequest.Builder()
                                .addCredentialOption(new GetRestoreCredentialOption(authJson))
                                .addCredentialOption(new GetPasswordOption())
                                .build();
                    }
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/JavaBuilderChaining.java:10: Error: GetRestoreCredentialOption cannot be combined with other CredentialOption entries in a single GetCredentialRequest (throws IllegalArgumentException at runtime). Request GetRestoreCredentialOption alone first, then fall back to interactive sign-in options. [RestoreCredentialOptionChaining]
                                .addCredentialOption(new GetRestoreCredentialOption(authJson))
                                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun localVariableListCombiningRestoreOption_reportsError() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import androidx.credentials.CredentialOption
                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetPasswordOption
                import androidx.credentials.GetRestoreCredentialOption

                fun createFromLocalList(authJson: String): GetCredentialRequest {
                    val options = mutableListOf<CredentialOption>()
                    options.add(GetRestoreCredentialOption(authJson))
                    options.add(GetPasswordOption())
                    return GetCredentialRequest(options)
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:10: Error: GetRestoreCredentialOption cannot be combined with other CredentialOption entries in a single GetCredentialRequest (throws IllegalArgumentException at runtime). Request GetRestoreCredentialOption alone first, then fall back to interactive sign-in options. [RestoreCredentialOptionChaining]
                    options.add(GetRestoreCredentialOption(authJson))
                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun dedicatedSingleRestoreOptionAndInteractiveMultiOptionRequests_areClean() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetPasswordOption
                import androidx.credentials.GetPublicKeyCredentialOption
                import androidx.credentials.GetRestoreCredentialOption

                fun buildRestoreRequest(authJson: String): GetCredentialRequest =
                    GetCredentialRequest(listOf(GetRestoreCredentialOption(authJson)))

                fun buildInteractiveRequest(authJson: String): GetCredentialRequest =
                    GetCredentialRequest(
                        listOf(GetPublicKeyCredentialOption(authJson), GetPasswordOption())
                    )
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun localBuilderWithSingleRestoreOption_doesNotReportFalsePositiveError() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetPasswordOption
                import androidx.credentials.GetRestoreCredentialOption

                fun buildWithLocalBuilder(json: String): GetCredentialRequest {
                    val builder = GetCredentialRequest.Builder()
                    val req = builder.addCredentialOption(GetRestoreCredentialOption(json)).build()
                    return req
                }

                fun buildAfterReplacingOptions(json: String): GetCredentialRequest {
                    val builder = GetCredentialRequest.Builder()
                    builder.addCredentialOption(GetPasswordOption())
                    return builder.setCredentialOptions(listOf(GetRestoreCredentialOption(json))).build()
                }
                """
                    )
                    .indented(),
                java(
                        """
                package com.example;

                import androidx.credentials.GetCredentialRequest;
                import androidx.credentials.GetRestoreCredentialOption;

                public final class JavaLocalBuilderRestoreOnly {
                    public GetCredentialRequest create(String json) {
                        GetCredentialRequest.Builder builder = new GetCredentialRequest.Builder();
                        GetCredentialRequest req =
                                builder.addCredentialOption(new GetRestoreCredentialOption(json)).build();
                        return req;
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
