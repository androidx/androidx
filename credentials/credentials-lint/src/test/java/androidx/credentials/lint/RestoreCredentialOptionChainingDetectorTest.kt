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

    @Test
    fun companionCreateFromAndMutableListPlusAssignChaining_reportsError() {
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

                fun createFromCompanion(json: String): GetCredentialRequest {
                    return GetCredentialRequest.createFrom(
                        listOf(GetRestoreCredentialOption(json), GetPasswordOption())
                    )
                }

                fun createWithPlusAssign(json: String): GetCredentialRequest {
                    val options = mutableListOf<CredentialOption>()
                    options += GetRestoreCredentialOption(json)
                    options += GetPasswordOption()
                    return GetCredentialRequest(options)
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectErrorCount(2)
    }

    @Test
    fun reusedMutableListClearedBetweenRequests_doesNotReportFalsePositiveError() {
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

                fun buildSeparateRequests(json: String): Pair<GetCredentialRequest, GetCredentialRequest> {
                    val options = mutableListOf<CredentialOption>()
                    options.add(GetRestoreCredentialOption(json))
                    val restoreReq = GetCredentialRequest(options)

                    options.clear()
                    options.add(GetPasswordOption())
                    val interactiveReq = GetCredentialRequest(options)
                    return restoreReq to interactiveReq
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun arrayListCapacityAndMutuallyExclusiveBranches_doNotReportFalsePositiveError() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import androidx.credentials.CredentialOption
                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetPasswordOption
                import androidx.credentials.GetPublicKeyCredentialOption
                import androidx.credentials.GetRestoreCredentialOption
                import java.util.ArrayList

                fun buildWithCapacity(json: String): GetCredentialRequest {
                    val options = ArrayList<CredentialOption>(1)
                    options.add(GetRestoreCredentialOption(json))
                    return GetCredentialRequest(options)
                }

                fun buildWithBranches(json: String, isRestoreFlow: Boolean): GetCredentialRequest {
                    val options = mutableListOf<CredentialOption>()
                    if (isRestoreFlow) {
                        options.add(GetRestoreCredentialOption(json))
                    } else {
                        options.add(GetPublicKeyCredentialOption(json))
                        options.add(GetPasswordOption())
                    }
                    return GetCredentialRequest(options)
                }

                fun buildWithBranchedBuilder(json: String, isRestoreFlow: Boolean): GetCredentialRequest {
                    val builder = GetCredentialRequest.Builder()
                    if (isRestoreFlow) {
                        builder.addCredentialOption(GetRestoreCredentialOption(json))
                    } else {
                        builder.addCredentialOption(GetPublicKeyCredentialOption(json))
                        builder.addCredentialOption(GetPasswordOption())
                    }
                    return builder.build()
                }

                private fun buildSingleRestoreList(json: String): List<CredentialOption> =
                    listOf(GetRestoreCredentialOption(json))

                fun buildCleanApplyAlsoPlusAndHelper(
                    json: String,
                    requestLog: MutableList<String>,
                ): List<GetCredentialRequest> {
                    val applyOptions =
                        mutableListOf<CredentialOption>().apply {
                            add(GetRestoreCredentialOption(json))
                            requestLog.add("restore")
                        }
                    val alsoOptions =
                        mutableListOf<CredentialOption>().also {
                            it.add(GetRestoreCredentialOption(json))
                            requestLog.add("restore-also")
                        }
                    return listOf(
                        GetCredentialRequest(applyOptions),
                        GetCredentialRequest(alsoOptions),
                        GetCredentialRequest(buildSingleRestoreList(json)),
                        GetCredentialRequest(
                            listOf<CredentialOption>(GetRestoreCredentialOption(json)) + emptyList()
                        ),
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
    fun applyBlockListPlusAndHelperBuiltOptionList_reportChainingErrors() {
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import androidx.credentials.CredentialOption
                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetPasswordOption
                import androidx.credentials.GetPublicKeyCredentialOption
                import androidx.credentials.GetRestoreCredentialOption

                private fun buildMixedOptions(json: String): List<CredentialOption> =
                    listOf(GetRestoreCredentialOption(json), GetPasswordOption())

                fun requestWithHelperList(json: String): GetCredentialRequest =
                    GetCredentialRequest(buildMixedOptions(json))

                fun requestWithChainedApply(json: String): GetCredentialRequest =
                    GetCredentialRequest(
                        mutableListOf<CredentialOption>().apply {
                            add(GetRestoreCredentialOption(json))
                            add(GetPasswordOption())
                        }
                    )

                fun requestWithChainedAlso(json: String): GetCredentialRequest =
                    GetCredentialRequest(
                        mutableListOf<CredentialOption>().also {
                            it.add(GetRestoreCredentialOption(json))
                            it.add(GetPasswordOption())
                        }
                    )

                fun requestWithLocalApplyAndPlus(json: String): Pair<GetCredentialRequest, GetCredentialRequest> {
                    val options = mutableListOf<CredentialOption>()
                    options.apply {
                        add(GetRestoreCredentialOption(json))
                        add(GetPasswordOption())
                    }
                    val req1 = GetCredentialRequest(options)
                    val req2 =
                        GetCredentialRequest(
                            listOf<CredentialOption>(GetRestoreCredentialOption(json)) +
                                GetPasswordOption()
                        )
                    return req1 to req2
                }

                fun requestWithUnconditionalPlusBranchedAdd(
                    json: String,
                    passkeys: Boolean,
                ): GetCredentialRequest {
                    val options = mutableListOf<CredentialOption>()
                    options.add(GetRestoreCredentialOption(json))
                    options.add(GetPasswordOption())
                    if (passkeys) {
                        options.add(GetPublicKeyCredentialOption(json))
                    }
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
                    listOf(GetRestoreCredentialOption(json), GetPasswordOption())
                           ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/com/example/test.kt:18: Error: GetRestoreCredentialOption cannot be combined with other CredentialOption entries in a single GetCredentialRequest (throws IllegalArgumentException at runtime). Request GetRestoreCredentialOption alone first, then fall back to interactive sign-in options. [RestoreCredentialOptionChaining]
                            add(GetRestoreCredentialOption(json))
                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/com/example/test.kt:26: Error: GetRestoreCredentialOption cannot be combined with other CredentialOption entries in a single GetCredentialRequest (throws IllegalArgumentException at runtime). Request GetRestoreCredentialOption alone first, then fall back to interactive sign-in options. [RestoreCredentialOptionChaining]
                            it.add(GetRestoreCredentialOption(json))
                                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/com/example/test.kt:34: Error: GetRestoreCredentialOption cannot be combined with other CredentialOption entries in a single GetCredentialRequest (throws IllegalArgumentException at runtime). Request GetRestoreCredentialOption alone first, then fall back to interactive sign-in options. [RestoreCredentialOptionChaining]
                        add(GetRestoreCredentialOption(json))
                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/com/example/test.kt:40: Error: GetRestoreCredentialOption cannot be combined with other CredentialOption entries in a single GetCredentialRequest (throws IllegalArgumentException at runtime). Request GetRestoreCredentialOption alone first, then fall back to interactive sign-in options. [RestoreCredentialOptionChaining]
                            listOf<CredentialOption>(GetRestoreCredentialOption(json)) +
                                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/com/example/test.kt:51: Error: GetRestoreCredentialOption cannot be combined with other CredentialOption entries in a single GetCredentialRequest (throws IllegalArgumentException at runtime). Request GetRestoreCredentialOption alone first, then fall back to interactive sign-in options. [RestoreCredentialOptionChaining]
                    options.add(GetRestoreCredentialOption(json))
                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                6 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun nestedScopeBlocksOnOtherLists_doNotFlagOuterRestoreList() {
        // 1. Clean: nested .apply / .also / buildList on unrelated lists do not pollute the outer
        // restore-only option list.
        lint()
            .files(
                *CREDENTIALS_STUBS,
                kotlin(
                        """
                package com.example

                import androidx.credentials.CredentialOption
                import androidx.credentials.GetCredentialRequest
                import androidx.credentials.GetRestoreCredentialOption

                fun cleanNestedScopes(json: String): List<GetCredentialRequest> {
                    val fromBuildList = buildList {
                        add(GetRestoreCredentialOption(json))
                        mutableListOf<String>().apply { add("tag") }
                        mutableListOf<String>().also { it.add("tag2") }
                        buildList { add("tag3") }
                    }
                    val fromApply =
                        mutableListOf<CredentialOption>().apply {
                            add(GetRestoreCredentialOption(json))
                            mutableListOf<String>().apply { add("tag") }
                        }
                    val fromAlso =
                        mutableListOf<CredentialOption>().also {
                            it.add(GetRestoreCredentialOption(json))
                            mutableListOf<String>().also { it.add("tag") }
                        }
                    val localApply = mutableListOf<CredentialOption>()
                    localApply.apply {
                        add(GetRestoreCredentialOption(json))
                        mutableListOf<String>().apply { add("tag") }
                    }
                    val localAlso = mutableListOf<CredentialOption>()
                    localAlso.also {
                        it.add(GetRestoreCredentialOption(json))
                        mutableListOf<String>().also { it.add("tag") }
                    }
                    return listOf(
                        GetCredentialRequest(fromBuildList),
                        GetCredentialRequest(fromApply),
                        GetCredentialRequest(fromAlso),
                        GetCredentialRequest(localApply),
                        GetCredentialRequest(localAlso),
                    )
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()

        // 2. Flagged: real outer-list mutations alongside nested scope blocks still report errors.
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

                fun flaggedWithNestedScopes(json: String): List<GetCredentialRequest> {
                    val fromBuildList = buildList {
                        add(GetRestoreCredentialOption(json))
                        mutableListOf<String>().apply { add("tag") }
                        add(GetPasswordOption())
                    }
                    val fromLabeledApply =
                        mutableListOf<CredentialOption>().apply {
                            add(GetRestoreCredentialOption(json))
                            mutableListOf<String>().run {
                                add("tag")
                                this@apply.add(GetPasswordOption())
                            }
                        }
                    val fromForEachInApply =
                        mutableListOf<CredentialOption>().apply {
                            add(GetRestoreCredentialOption(json))
                            listOf(1).forEach { add(GetPasswordOption()) }
                        }
                    return listOf(
                        GetCredentialRequest(fromBuildList),
                        GetCredentialRequest(fromLabeledApply),
                        GetCredentialRequest(fromForEachInApply),
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
                        add(GetRestoreCredentialOption(json))
                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/com/example/test.kt:16: Error: GetRestoreCredentialOption cannot be combined with other CredentialOption entries in a single GetCredentialRequest (throws IllegalArgumentException at runtime). Request GetRestoreCredentialOption alone first, then fall back to interactive sign-in options. [RestoreCredentialOptionChaining]
                            add(GetRestoreCredentialOption(json))
                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/com/example/test.kt:24: Error: GetRestoreCredentialOption cannot be combined with other CredentialOption entries in a single GetCredentialRequest (throws IllegalArgumentException at runtime). Request GetRestoreCredentialOption alone first, then fall back to interactive sign-in options. [RestoreCredentialOptionChaining]
                            add(GetRestoreCredentialOption(json))
                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                3 errors, 0 warnings
                """
                    .trimIndent()
            )
    }
}
