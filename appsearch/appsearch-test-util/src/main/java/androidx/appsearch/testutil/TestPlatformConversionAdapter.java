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

// @exportToFramework:skipFile()
package androidx.appsearch.testutil;

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.EmbeddingVector;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.platformstorage.PlatformConversionAdapter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A test implementation of {@link PlatformConversionAdapter} that captures parameters.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalAppSearchApi
public class TestPlatformConversionAdapter implements PlatformConversionAdapter {
    private String mSchemaDescription;
    private String mPropertyDescription;
    private Integer mDeletePropagationType;
    private Integer mIndexingType;
    private EmbeddingVector mCapturedEmbeddingVector;
    private Map<String, Set<String>> mSchemasWipeoutAccountPropertyPaths;
    private List<String> mSearchSpecSearchStringParameters;
    private List<String> mSearchSuggestionSpecSearchStringParameters;
    private Integer mEmbeddingQueryProbeCount;

    @Nullable
    public String getSchemaDescription() {
        return mSchemaDescription;
    }

    @Nullable
    public String getPropertyDescription() {
        return mPropertyDescription;
    }

    @Nullable
    public Integer getDeletePropagationType() {
        return mDeletePropagationType;
    }

    @Nullable
    public Integer getIndexingType() {
        return mIndexingType;
    }

    @Nullable
    public EmbeddingVector getCapturedEmbeddingVector() {
        return mCapturedEmbeddingVector;
    }

    @Nullable
    public Map<String, Set<String>> getSchemasWipeoutAccountPropertyPaths() {
        return mSchemasWipeoutAccountPropertyPaths;
    }

    @Nullable
    public List<String> getSearchSpecSearchStringParameters() {
        return mSearchSpecSearchStringParameters;
    }

    @Nullable
    public List<String> getSearchSuggestionSpecSearchStringParameters() {
        return mSearchSuggestionSpecSearchStringParameters;
    }

    @Nullable
    public Integer getEmbeddingQueryProbeCount() {
        return mEmbeddingQueryProbeCount;
    }

    @Override
    public void setSchemaDescription(
            android.app.appsearch.AppSearchSchema.@NonNull Builder builder,
            @NonNull String description) {
        mSchemaDescription = description;
    }

    @Override
    public void setPropertyDescription(
            @NonNull Object builder,
            @NonNull String description) {
        mPropertyDescription = description;
    }

    @Override
    public void setDeletePropagationType(
            android.app.appsearch.AppSearchSchema.StringPropertyConfig.@NonNull Builder builder,
            @AppSearchSchema.StringPropertyConfig.DeletePropagationType int deletePropagationType) {
        mDeletePropagationType = deletePropagationType;
    }

    @Override
    public void setAnnIndexingType(
            android.app.appsearch.AppSearchSchema.EmbeddingPropertyConfig.@NonNull Builder builder,
            @AppSearchSchema.EmbeddingPropertyConfig.IndexingType int indexingType) {
        mIndexingType = indexingType;
    }

    @Override
    public android.app.appsearch.@NonNull EmbeddingVector convertQuantizedEmbeddingVector(
            @NonNull EmbeddingVector jetpackEmbeddingVector) {
        mCapturedEmbeddingVector = jetpackEmbeddingVector;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            return ApiHelperForB.createDummyEmbeddingVector();
        }
        throw new UnsupportedOperationException("Not supported on this SDK: " + Build.VERSION.SDK_INT);
    }

    @SuppressLint("NewApi") // getValues() is incorrectly flagged as needing 34-ext16
    @Override
    public @NonNull EmbeddingVector toJetpackEmbeddingVector(
            android.app.appsearch.@NonNull EmbeddingVector platformEmbeddingVector) {
        if (mCapturedEmbeddingVector != null) {
            return mCapturedEmbeddingVector;
        }
        return new EmbeddingVector(
                platformEmbeddingVector.getValues(),
                platformEmbeddingVector.getModelSignature());
    }

    @Override
    public void setSchemasWipeoutAccountPropertyPaths(
            android.app.appsearch.SetSchemaRequest.@NonNull Builder builder,
            @NonNull Map<String, Set<String>> paths) {
        mSchemasWipeoutAccountPropertyPaths = paths;
    }

    @Override
    public void setSearchStringParameters(
            android.app.appsearch.SearchSpec.@NonNull Builder builder,
            @NonNull List<String> searchStringParameters) {
        mSearchSpecSearchStringParameters = searchStringParameters;
    }

    @Override
    public void setSearchStringParameters(
            android.app.appsearch.SearchSuggestionSpec.@NonNull Builder builder,
            @NonNull List<String> searchStringParameters) {
        mSearchSuggestionSpecSearchStringParameters = searchStringParameters;
    }

    @Override
    public void setEmbeddingQueryProbeCount(
            android.app.appsearch.SearchSpec.@NonNull Builder builder,
            int embeddingQueryProbeCount) {
        mEmbeddingQueryProbeCount = embeddingQueryProbeCount;
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private static class ApiHelperForB {
        static android.app.appsearch.EmbeddingVector createDummyEmbeddingVector() {
            return new android.app.appsearch.EmbeddingVector(new float[]{0f}, "model");
        }
    }
}
