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

package androidx.appsearch.playservicesstorage.converter;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchResult;

import org.junit.Test;

public class AppSearchResultToGmsConverterTest {
    @Test
    public void testGmsAppSearchBatchResultToJetpack() {
        com.google.android.gms.appsearch.AppSearchBatchResult<String,
                Void> gmsBatchResult =
                new com.google.android.gms.appsearch.AppSearchBatchResult.Builder<String, Void>()
                        .setFailure("123", AppSearchResult.RESULT_NOT_FOUND, /*errorMessage=*/
                                null).build();
        AppSearchBatchResult<String, Void> jetpackBatchResult =
                AppSearchResultToGmsConverter.gmsAppSearchBatchResultToJetpack(
                        gmsBatchResult,
                        gmsValue -> gmsValue);
        assertThat(jetpackBatchResult.isSuccess()).isFalse();
        assertThat(jetpackBatchResult.getFailures())
                .containsExactly("123",
                        AppSearchResult
                                .newFailedResult(AppSearchResult.RESULT_NOT_FOUND,
                                        "Document id '123' doesn't exist"));
    }
}
