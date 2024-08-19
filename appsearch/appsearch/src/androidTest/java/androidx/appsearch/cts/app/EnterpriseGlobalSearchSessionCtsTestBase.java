/*
 * Copyright 2024 The Android Open Source Project
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

import static androidx.appsearch.app.AppSearchResult.RESULT_NOT_FOUND;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.EnterpriseGlobalSearchSession;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.flags.RequiresFlagsEnabled;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;

public abstract class EnterpriseGlobalSearchSessionCtsTestBase {
    protected EnterpriseGlobalSearchSession mEnterpriseGlobalSearchSession;

    protected abstract ListenableFuture<EnterpriseGlobalSearchSession>
            createEnterpriseGlobalSearchSessionAsync() throws Exception;

    @Before
    public void setUp() throws Exception {
        mEnterpriseGlobalSearchSession = createEnterpriseGlobalSearchSessionAsync().get();
    }

    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_ENTERPRISE_EMPTY_BATCH_RESULT_FIX)
    @Test
    public void testGetByDocumentId_returnsNotFoundResults() throws Exception {
        // The batch result may be empty instead of containing NOT_FOUND errors if the enterprise
        // user is missing. If that's the case, we correct the batch result before returning to the
        // caller
        GetByDocumentIdRequest request = new GetByDocumentIdRequest.Builder("namespace").addIds(
                "123", "456", "789").build();
        AppSearchBatchResult<String, GenericDocument> result =
                mEnterpriseGlobalSearchSession.getByDocumentIdAsync("foo", "bar", request).get();
        assertThat(result.getFailures()).containsExactly("123",
                AppSearchResult.newFailedResult(RESULT_NOT_FOUND,
                        "Document (namespace, 123) not found."), "456",
                AppSearchResult.newFailedResult(RESULT_NOT_FOUND,
                        "Document (namespace, 456) not found."), "789",
                AppSearchResult.newFailedResult(RESULT_NOT_FOUND,
                        "Document (namespace, 789) not found."));
    }
}
