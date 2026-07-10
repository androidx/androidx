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

package androidx.appsearch.app;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

/** Tests for private APIs of {@link AppSearchBatchResult}. */
public class AppSearchBatchResultInternalTest {
    @Test
    public void testToVoidBatchResult() {
        AppSearchBatchResult<String, Integer> result =
                new AppSearchBatchResult.Builder<String, Integer>()
                        .setSuccess("keySuccess1", 1)
                        .setSuccess("keySuccess2", 2)
                        .setFailure("keyFailure1", AppSearchResult.RESULT_UNKNOWN_ERROR, "message1")
                        .setFailure(
                                "keyFailure2", AppSearchResult.RESULT_INTERNAL_ERROR, "message2")
                        .setResult("keySuccess3", AppSearchResult.newSuccessfulResult(3))
                        .setResult(
                                "keyFailure3",
                                AppSearchResult.newFailedResult(
                                        AppSearchResult.RESULT_INVALID_ARGUMENT, "message3"))
                        .build();

        AppSearchBatchResult<String, Void> resultWithVoidValue = result.toVoidBatchResult();

        assertThat(resultWithVoidValue.isSuccess()).isFalse();
        assertThat(resultWithVoidValue.getSuccesses())
                .containsExactly("keySuccess1", null, "keySuccess2", null, "keySuccess3", null);
        assertThat(resultWithVoidValue.getFailures())
                .containsExactly(
                        "keyFailure1",
                        AppSearchResult.newFailedResult(
                                AppSearchResult.RESULT_UNKNOWN_ERROR, "message1"),
                        "keyFailure2",
                        AppSearchResult.newFailedResult(
                                AppSearchResult.RESULT_INTERNAL_ERROR, "message2"),
                        "keyFailure3",
                        AppSearchResult.newFailedResult(
                                AppSearchResult.RESULT_INVALID_ARGUMENT, "message3"));
        assertThat(resultWithVoidValue.getAll())
                .containsExactly(
                        "keySuccess1",
                        AppSearchResult.newSuccessfulResult(null),
                        "keySuccess2",
                        AppSearchResult.newSuccessfulResult(null),
                        "keySuccess3",
                        AppSearchResult.newSuccessfulResult(null),
                        "keyFailure1",
                        AppSearchResult.newFailedResult(
                                AppSearchResult.RESULT_UNKNOWN_ERROR, "message1"),
                        "keyFailure2",
                        AppSearchResult.newFailedResult(
                                AppSearchResult.RESULT_INTERNAL_ERROR, "message2"),
                        "keyFailure3",
                        AppSearchResult.newFailedResult(
                                AppSearchResult.RESULT_INVALID_ARGUMENT, "message3"));
    }
}
