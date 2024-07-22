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

package androidx.appsearch.playservicesstorage.converter;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.JoinSpec;
import androidx.core.util.Preconditions;

/**
 * Translates between Gms and Jetpack versions of {@link JoinSpec}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class JoinSpecToGmsConverter {
    private JoinSpecToGmsConverter() {
    }

    /**
     * Translates a Jetpack {@link JoinSpec} into a gms
     * {@link com.google.android.gms.appsearch.JoinSpec}.
     */
    @SuppressLint("WrongConstant")
    @NonNull
    public static com.google.android.gms.appsearch.JoinSpec toGmsJoinSpec(
            @NonNull JoinSpec jetpackSpec) {
        Preconditions.checkNotNull(jetpackSpec);
        return new com.google.android.gms.appsearch.JoinSpec
                .Builder(jetpackSpec.getChildPropertyExpression())
                .setNestedSearch(
                        jetpackSpec.getNestedQuery(),
                        SearchSpecToGmsConverter.toGmsSearchSpec(
                                jetpackSpec.getNestedSearchSpec()))
                .setMaxJoinedResultCount(jetpackSpec.getMaxJoinedResultCount())
                .setAggregationScoringStrategy(jetpackSpec.getAggregationScoringStrategy())
                .build();
    }
}
