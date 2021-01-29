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

package androidx.appsearch.platformstorage.converter;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.SearchSpec;
import androidx.core.util.Preconditions;

/**
 * Translates between Platform and Jetpack versions of {@link SearchSpec}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.S)
public final class SearchSpecToPlatformConverter {
    private SearchSpecToPlatformConverter() {}

    /** Translates from Jetpack to Platform version of {@link SearchSpec}. */
    @NonNull
    public static android.app.appsearch.SearchSpec toPlatformSearchSpec(
            @NonNull SearchSpec jetpackSearchSpec) {
        Preconditions.checkNotNull(jetpackSearchSpec);
        android.app.appsearch.SearchSpec.Builder platformBuilder =
                new android.app.appsearch.SearchSpec.Builder();
        platformBuilder
                .setTermMatch(jetpackSearchSpec.getTermMatch())
                .addSchemaType(jetpackSearchSpec.getFilterSchemas())
                .addNamespace(jetpackSearchSpec.getFilterNamespaces())
                .setResultCountPerPage(jetpackSearchSpec.getResultCountPerPage())
                .setRankingStrategy(jetpackSearchSpec.getRankingStrategy())
                .setOrder(jetpackSearchSpec.getOrder())
                .setSnippetCount(jetpackSearchSpec.getSnippetCount())
                .setSnippetCountPerProperty(jetpackSearchSpec.getSnippetCountPerProperty())
                .setMaxSnippetSize(jetpackSearchSpec.getMaxSnippetSize());
                // TODO(b/175039682): Support projection paths
        return platformBuilder.build();
    }
}
