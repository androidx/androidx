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

import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import android.os.Build;

import androidx.annotation.RequiresFeature;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.EnterpriseGlobalSearchSession;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.testutil.AppSearchTestUtils;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;
import androidx.test.filters.SdkSuppress;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
public abstract class EnterpriseGlobalSearchSessionCtsTestBase {
    @Rule
    public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();

    @RequiresFeature(
            enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
            name = Features.ENTERPRISE_GLOBAL_SEARCH_SESSION)
    protected abstract ListenableFuture<EnterpriseGlobalSearchSession>
            createEnterpriseGlobalSearchSessionAsync() throws Exception;

    @NonNull
    protected abstract Features getFeatures();

    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_ENTERPRISE_EMPTY_BATCH_RESULT_FIX)
    @Test
    public void testGetByDocumentId_returnsNotFoundResults() throws Exception {
        assumeTrue(getFeatures().isFeatureSupported(Features.ENTERPRISE_GLOBAL_SEARCH_SESSION));
        EnterpriseGlobalSearchSession enterpriseGlobalSearchSession =
                createEnterpriseGlobalSearchSessionAsync().get();

        // The batch result may be empty instead of containing NOT_FOUND errors if the enterprise
        // user is missing. If that's the case, we correct the batch result before returning to the
        // caller
        GetByDocumentIdRequest request = new GetByDocumentIdRequest.Builder("namespace").addIds(
                "123", "456", "789").build();
        AppSearchBatchResult<String, GenericDocument> result =
                enterpriseGlobalSearchSession.getByDocumentIdAsync("foo", "bar", request).get();
        assertThat(result.getFailures()).containsExactly("123",
                AppSearchResult.newFailedResult(RESULT_NOT_FOUND,
                        "Document (namespace, 123) not found."), "456",
                AppSearchResult.newFailedResult(RESULT_NOT_FOUND,
                        "Document (namespace, 456) not found."), "789",
                AppSearchResult.newFailedResult(RESULT_NOT_FOUND,
                        "Document (namespace, 789) not found."));
    }

    @Test
    public void testEnterpriseGlobalSearchNotAvailable_throwsException() {
        assumeFalse(getFeatures().isFeatureSupported(Features.ENTERPRISE_GLOBAL_SEARCH_SESSION));
        assertThrows(
                UnsupportedOperationException.class,
                () -> createEnterpriseGlobalSearchSessionAsync().get());
    }
}
