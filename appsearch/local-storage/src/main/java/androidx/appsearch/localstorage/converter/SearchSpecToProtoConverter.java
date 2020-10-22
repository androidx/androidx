/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.appsearch.localstorage.converter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.SearchSpec;
import androidx.core.util.Preconditions;

import com.google.android.icing.proto.ResultSpecProto;
import com.google.android.icing.proto.ScoringSpecProto;
import com.google.android.icing.proto.SearchSpecProto;
import com.google.android.icing.proto.TermMatchType;

/**
 * Translates a {@link SearchSpec} into icing search protos.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SearchSpecToProtoConverter {
    private SearchSpecToProtoConverter() {}

    /** Extracts {@link SearchSpecProto} information from a {@link SearchSpec}. */
    @NonNull
    public static SearchSpecProto toSearchSpecProto(@NonNull SearchSpec spec) {
        Preconditions.checkNotNull(spec);
        Bundle bundle = spec.getBundle();
        SearchSpecProto.Builder protoBuilder = SearchSpecProto.newBuilder()
                .addAllSchemaTypeFilters(bundle.getStringArrayList(SearchSpec.SCHEMA_TYPE_FIELD))
                .addAllNamespaceFilters(bundle.getStringArrayList(SearchSpec.NAMESPACE_FIELD));

        @SearchSpec.TermMatch int termMatchCode =
                bundle.getInt(SearchSpec.TERM_MATCH_TYPE_FIELD);
        TermMatchType.Code termMatchCodeProto = TermMatchType.Code.forNumber(termMatchCode);
        if (termMatchCodeProto == null || termMatchCodeProto.equals(TermMatchType.Code.UNKNOWN)) {
            throw new IllegalArgumentException("Invalid term match type: " + termMatchCode);
        }
        protoBuilder.setTermMatchType(termMatchCodeProto);

        return protoBuilder.build();
    }

    /** Extracts {@link ResultSpecProto} information from a {@link SearchSpec}. */
    @NonNull
    public static ResultSpecProto toResultSpecProto(@NonNull SearchSpec spec) {
        Preconditions.checkNotNull(spec);
        Bundle bundle = spec.getBundle();
        return ResultSpecProto.newBuilder()
                .setNumPerPage(bundle.getInt(
                        SearchSpec.NUM_PER_PAGE_FIELD, SearchSpec.DEFAULT_NUM_PER_PAGE))
                .setSnippetSpec(ResultSpecProto.SnippetSpecProto.newBuilder()
                        .setNumToSnippet(bundle.getInt(SearchSpec.SNIPPET_COUNT_FIELD))
                        .setNumMatchesPerProperty(
                                bundle.getInt(SearchSpec.SNIPPET_COUNT_PER_PROPERTY_FIELD))
                        .setMaxWindowBytes(bundle.getInt(SearchSpec.MAX_SNIPPET_FIELD)))
                .build();

    }

    /** Extracts {@link ScoringSpecProto} information from a {@link SearchSpec}. */
    @NonNull
    public static ScoringSpecProto toScoringSpecProto(@NonNull SearchSpec spec) {
        Preconditions.checkNotNull(spec);
        Bundle bundle = spec.getBundle();
        ScoringSpecProto.Builder protoBuilder = ScoringSpecProto.newBuilder();

        @SearchSpec.Order int orderCode = bundle.getInt(SearchSpec.ORDER_FIELD);
        ScoringSpecProto.Order.Code orderCodeProto =
                ScoringSpecProto.Order.Code.forNumber(orderCode);
        if (orderCodeProto == null) {
            throw new IllegalArgumentException("Invalid result ranking order: " + orderCode);
        }
        protoBuilder.setOrderBy(orderCodeProto);

        @SearchSpec.RankingStrategy int rankingStrategyCode =
                bundle.getInt(SearchSpec.RANKING_STRATEGY_FIELD);
        ScoringSpecProto.RankingStrategy.Code rankingStrategyCodeProto =
                ScoringSpecProto.RankingStrategy.Code.forNumber(rankingStrategyCode);
        if (rankingStrategyCodeProto == null) {
            throw new IllegalArgumentException("Invalid result ranking strategy: "
                    + rankingStrategyCode);
        }
        protoBuilder.setRankBy(rankingStrategyCodeProto);

        return protoBuilder.build();
    }
}
