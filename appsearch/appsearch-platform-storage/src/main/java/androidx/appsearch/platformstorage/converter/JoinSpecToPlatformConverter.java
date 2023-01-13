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

package androidx.appsearch.platformstorage.converter;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.JoinSpec;
import androidx.core.os.BuildCompat;
import androidx.core.util.Preconditions;

/**
 * Translates between Platform and Jetpack versions of {@link JoinSpec}.
 */
// TODO(b/265311462): Remove BuildCompat.PrereleaseSdkCheck annotation once
//  SearchSpecToPlatformConverter.toPlatformSearchSpec() removes it. Also, replace literal '34' with
//  Build.VERSION_CODES.UPSIDE_DOWN_CAKE once the SDK_INT is finalized.
@BuildCompat.PrereleaseSdkCheck
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(34)
public class JoinSpecToPlatformConverter {
    private JoinSpecToPlatformConverter() {}

    /**
     * Translates a Jetpack {@link JoinSpec} into a platform {@link android.app.appsearch.JoinSpec}.
     */
    @SuppressLint("WrongConstant")
    @NonNull
    public static android.app.appsearch.JoinSpec toPlatformJoinSpec(@NonNull JoinSpec jetpackSpec) {
        Preconditions.checkNotNull(jetpackSpec);
        return new android.app.appsearch.JoinSpec.Builder(jetpackSpec.getChildPropertyExpression())
                .setNestedSearch(
                        jetpackSpec.getNestedQuery(),
                        SearchSpecToPlatformConverter.toPlatformSearchSpec(
                                jetpackSpec.getNestedSearchSpec()))
                .setMaxJoinedResultCount(jetpackSpec.getMaxJoinedResultCount())
                .setAggregationScoringStrategy(jetpackSpec.getAggregationScoringStrategy())
                .build();
    }
}
