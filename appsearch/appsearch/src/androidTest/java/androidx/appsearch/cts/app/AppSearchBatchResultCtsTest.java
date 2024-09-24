/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appsearch.cts.app;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.flags.CheckFlagsRule;
import androidx.appsearch.flags.DeviceFlagsValueProvider;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.flags.RequiresFlagsEnabled;

import org.junit.Rule;
import org.junit.Test;

public class AppSearchBatchResultCtsTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void testIsSuccess_true() {
        AppSearchBatchResult<String, Integer> result =
                new AppSearchBatchResult.Builder<String, Integer>()
                        .setSuccess("keySuccess1", 1)
                        .setSuccess("keySuccess2", 2)
                        .setResult("keySuccess3", AppSearchResult.newSuccessfulResult(3))
                        .build();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    public void testIsSuccess_false() {
        AppSearchBatchResult<String, Integer> result1 =
                new AppSearchBatchResult.Builder<String, Integer>()
                        .setSuccess("keySuccess1", 1)
                        .setSuccess("keySuccess2", 2)
                        .setFailure(
                                "keyFailure1", AppSearchResult.RESULT_UNKNOWN_ERROR, "message1")
                        .build();

        AppSearchBatchResult<String, Integer> result2 =
                new AppSearchBatchResult.Builder<String, Integer>()
                        .setSuccess("keySuccess1", 1)
                        .setResult(
                                "keyFailure3",
                                AppSearchResult.newFailedResult(
                                        AppSearchResult.RESULT_INVALID_ARGUMENT, "message3"))
                        .build();

        assertThat(result1.isSuccess()).isFalse();
        assertThat(result2.isSuccess()).isFalse();
    }

    @Test
    public void testIsSuccess_replace() {
        AppSearchBatchResult<String, Integer> result1 =
                new AppSearchBatchResult.Builder<String, Integer>()
                        .setSuccess("key", 1)
                        .setFailure("key", AppSearchResult.RESULT_UNKNOWN_ERROR, "message1")
                        .build();

        AppSearchBatchResult<String, Integer> result2 =
                new AppSearchBatchResult.Builder<String, Integer>()
                        .setFailure("key", AppSearchResult.RESULT_UNKNOWN_ERROR, "message1")
                        .setSuccess("key", 1)
                        .build();

        assertThat(result1.isSuccess()).isFalse();
        assertThat(result2.isSuccess()).isTrue();
    }

    @Test
    public void testGetters() {
        AppSearchBatchResult<String, Integer> result =
                new AppSearchBatchResult.Builder<String, Integer>()
                        .setSuccess("keySuccess1", 1)
                        .setSuccess("keySuccess2", 2)
                        .setFailure(
                                "keyFailure1", AppSearchResult.RESULT_UNKNOWN_ERROR, "message1")
                        .setFailure(
                                "keyFailure2", AppSearchResult.RESULT_INTERNAL_ERROR, "message2")
                        .setResult("keySuccess3", AppSearchResult.newSuccessfulResult(3))
                        .setResult(
                                "keyFailure3",
                                AppSearchResult.newFailedResult(
                                        AppSearchResult.RESULT_INVALID_ARGUMENT, "message3"))
                        .build();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getSuccesses()).containsExactly(
                "keySuccess1", 1, "keySuccess2", 2, "keySuccess3", 3);
        assertThat(result.getFailures()).containsExactly(
                "keyFailure1",
                AppSearchResult.newFailedResult(AppSearchResult.RESULT_UNKNOWN_ERROR, "message1"),
                "keyFailure2",
                AppSearchResult.newFailedResult(AppSearchResult.RESULT_INTERNAL_ERROR, "message2"),
                "keyFailure3",
                AppSearchResult.newFailedResult(
                        AppSearchResult.RESULT_INVALID_ARGUMENT, "message3"));
        assertThat(result.getAll()).containsExactly(
                "keySuccess1", AppSearchResult.newSuccessfulResult(1),
                "keySuccess2", AppSearchResult.newSuccessfulResult(2),
                "keySuccess3", AppSearchResult.newSuccessfulResult(3),
                "keyFailure1",
                AppSearchResult.newFailedResult(AppSearchResult.RESULT_UNKNOWN_ERROR, "message1"),
                "keyFailure2",
                AppSearchResult.newFailedResult(AppSearchResult.RESULT_INTERNAL_ERROR, "message2"),
                "keyFailure3",
                AppSearchResult.newFailedResult(
                        AppSearchResult.RESULT_INVALID_ARGUMENT, "message3"));
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
    public void testAppSearchBatchResultBuilder_copyConstructor() {
        AppSearchBatchResult<String, Integer> result =
                new AppSearchBatchResult.Builder<String, Integer>()
                        .setSuccess("keySuccess1", 1)
                        .setSuccess("keySuccess2", 2)
                        .setFailure(
                                "keyFailure1", AppSearchResult.RESULT_UNKNOWN_ERROR, "message1")
                        .setFailure(
                                "keyFailure2", AppSearchResult.RESULT_INTERNAL_ERROR, "message2")
                        .build();
        AppSearchBatchResult<String, Integer> resultCopy = new AppSearchBatchResult.Builder<>(
                result).build();
        assertThat(resultCopy.getAll()).isEqualTo(result.getAll());
        assertThat(resultCopy.getSuccesses()).isEqualTo(result.getSuccesses());
        assertThat(resultCopy.getFailures()).isEqualTo(result.getFailures());
    }
}
