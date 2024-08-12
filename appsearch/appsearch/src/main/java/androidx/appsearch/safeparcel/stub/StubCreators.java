/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.appsearch.safeparcel.stub;

import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchBlobHandle;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.EmbeddingVector;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.InternalSetSchemaResponse;
import androidx.appsearch.app.InternalVisibilityConfig;
import androidx.appsearch.app.JoinSpec;
import androidx.appsearch.app.RemoveByDocumentIdRequest;
import androidx.appsearch.app.ReportUsageRequest;
import androidx.appsearch.app.SchemaVisibilityConfig;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResult.MatchInfo;
import androidx.appsearch.app.SearchResultPage;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SearchSuggestionResult;
import androidx.appsearch.app.SearchSuggestionSpec;
import androidx.appsearch.app.SetSchemaResponse;
import androidx.appsearch.app.SetSchemaResponse.MigrationFailure;
import androidx.appsearch.app.StorageInfo;
import androidx.appsearch.app.VisibilityPermissionConfig;
import androidx.appsearch.observer.ObserverSpec;
import androidx.appsearch.safeparcel.PropertyConfigParcel;
import androidx.appsearch.safeparcel.PropertyConfigParcel.DocumentIndexingConfigParcel;
import androidx.appsearch.safeparcel.PropertyConfigParcel.EmbeddingIndexingConfigParcel;
import androidx.appsearch.safeparcel.PropertyConfigParcel.IntegerIndexingConfigParcel;
import androidx.appsearch.safeparcel.PropertyConfigParcel.JoinableConfigParcel;
import androidx.appsearch.safeparcel.PropertyConfigParcel.StringIndexingConfigParcel;
import androidx.appsearch.stats.SchemaMigrationStats;

/**
 * Stub creators for any classes extending
 * {@link androidx.appsearch.safeparcel.SafeParcelable}.
 *
 * <p>We don't have SafeParcelProcessor in Jetpack, so for each
 * {@link androidx.appsearch.safeparcel.SafeParcelable}, a stub creator class needs to
 * be provided for code sync purpose.
 */
// @exportToFramework:skipFile()
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class StubCreators {
    /** Stub creator for {@link androidx.appsearch.app.StorageInfo}. */
    public static class StorageInfoCreator extends AbstractCreator<StorageInfo> {
    }

    /** Stub creator for {@link PropertyConfigParcel}. */
    public static class PropertyConfigParcelCreator extends AbstractCreator<PropertyConfigParcel> {
    }

    /**
     * Stub creator for
     * {@link PropertyConfigParcel.JoinableConfigParcel}.
     */
    public static class JoinableConfigParcelCreator extends AbstractCreator<JoinableConfigParcel> {
    }

    /**
     * Stub creator for
     * {@link PropertyConfigParcel.StringIndexingConfigParcel}.
     */
    public static class StringIndexingConfigParcelCreator extends
            AbstractCreator<StringIndexingConfigParcel> {
    }

    /**
     * Stub creator for
     * {@link PropertyConfigParcel.IntegerIndexingConfigParcel}.
     */
    public static class IntegerIndexingConfigParcelCreator extends
            AbstractCreator<IntegerIndexingConfigParcel> {
    }

    /**
     * Stub creator for
     * {@link PropertyConfigParcel.DocumentIndexingConfigParcel}.
     */
    public static class DocumentIndexingConfigParcelCreator extends
            AbstractCreator<DocumentIndexingConfigParcel> {
    }

    /** Stub creator for {@link SchemaVisibilityConfig}. */
    public static class VisibilityConfigCreator extends AbstractCreator<SchemaVisibilityConfig> {
    }

    /**
     * Stub creator for {@link EmbeddingIndexingConfigParcel}.
     */
    public static class EmbeddingIndexingConfigParcelCreator extends
            AbstractCreator<EmbeddingIndexingConfigParcel> {
    }

    /** Stub creator for {@link InternalVisibilityConfig}. */
    public static class InternalVisibilityConfigCreator
            extends AbstractCreator<InternalVisibilityConfig> {
    }

    /** Stub creator for {@link VisibilityPermissionConfig}. */
    public static class VisibilityPermissionConfigCreator extends
            AbstractCreator<VisibilityPermissionConfig> {
    }

    /** Stub creator for {@link androidx.appsearch.stats.SchemaMigrationStats}. */
    public static class SchemaMigrationStatsCreator extends AbstractCreator<SchemaMigrationStats> {
    }

    /** Stub creator for {@link androidx.appsearch.app.SearchSuggestionResult}. */
    public static class SearchSuggestionResultCreator extends
            AbstractCreator<SearchSuggestionResult> {
    }

    /** Stub creator for {@link androidx.appsearch.app.SearchSuggestionSpec}. */
    public static class SearchSuggestionSpecCreator extends AbstractCreator<SearchSuggestionSpec> {
    }

    /** Stub creator for {@link androidx.appsearch.observer.ObserverSpec}. */
    public static class ObserverSpecCreator extends AbstractCreator<ObserverSpec> {
    }

    /** Stub creator for {@link androidx.appsearch.app.SetSchemaResponse}. */
    public static class SetSchemaResponseCreator extends
            AbstractCreator<SetSchemaResponse> {
    }

    /** Stub creator for {@link androidx.appsearch.app.SetSchemaResponse.MigrationFailure}. */
    public static class MigrationFailureCreator extends
            AbstractCreator<MigrationFailure> {
    }

    /** Stub creator for {@link androidx.appsearch.app.InternalSetSchemaResponse}. */
    public static class InternalSetSchemaResponseCreator extends
            AbstractCreator<InternalSetSchemaResponse> {
    }

    /** Stub creator for {@link androidx.appsearch.app.SearchSpec}. */
    public static class SearchSpecCreator extends AbstractCreator<SearchSpec> {
    }

    /** Stub creator for {@link androidx.appsearch.app.JoinSpec}. */
    public static class JoinSpecCreator extends AbstractCreator<JoinSpec> {
    }

    /** Stub creator for {@link androidx.appsearch.app.GetSchemaResponse}. */
    public static class GetSchemaResponseCreator extends AbstractCreator<GetSchemaResponse> {
    }

    /** Stub creator for {@link androidx.appsearch.app.AppSearchSchema}. */
    public static class AppSearchSchemaCreator extends
            AbstractCreator<AppSearchSchema> {
    }

    /** Stub creator for {@link androidx.appsearch.app.SearchResult}. */
    public static class SearchResultCreator extends AbstractCreator<SearchResult> {
    }

    /** Stub creator for {@link androidx.appsearch.app.MatchInfo}. */
    public static class MatchInfoCreator extends AbstractCreator<MatchInfo> {
    }

    /** Stub creator for {@link androidx.appsearch.app.SearchResultPage}. */
    public static class SearchResultPageCreator extends AbstractCreator<SearchResultPage> {
    }

    /** Stub creator for {@link androidx.appsearch.app.RemoveByDocumentIdRequest}. */
    public static class RemoveByDocumentIdRequestCreator extends
            AbstractCreator<RemoveByDocumentIdRequest> {
    }

    /** Stub creator for {@link androidx.appsearch.app.ReportUsageRequest}. */
    public static class ReportUsageRequestCreator extends AbstractCreator<ReportUsageRequest> {
    }

    /** Stub creator for {@link androidx.appsearch.app.GetByDocumentIdRequest}. */
    public static class GetByDocumentIdRequestCreator extends
            AbstractCreator<GetByDocumentIdRequest> {
    }

    /** Stub creator for {@link EmbeddingVector}. */
    public static class EmbeddingVectorCreator extends
            AbstractCreator<EmbeddingVector> {
    }

    /** Stub creator for {@link androidx.appsearch.app.AppSearchBlobHandle}. */
    public static class AppSearchBlobHandleCreator extends
            AbstractCreator<AppSearchBlobHandle> {
    }
}
