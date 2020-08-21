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

package androidx.appsearch.app;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

import com.google.android.icing.proto.ResultSpecProto;
import com.google.android.icing.proto.ScoringSpecProto;
import com.google.android.icing.proto.SearchSpecProto;

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
        return spec.getSearchSpecProto();
    }

    /** Extracts {@link ResultSpecProto} information from a {@link SearchSpec}. */
    @NonNull
    public static ResultSpecProto toResultSpecProto(@NonNull SearchSpec spec) {
        Preconditions.checkNotNull(spec);
        return spec.getResultSpecProto();
    }

    /** Extracts {@link ScoringSpecProto} information from a {@link SearchSpec}. */
    @NonNull
    public static ScoringSpecProto toScoringSpecProto(@NonNull SearchSpec spec) {
        Preconditions.checkNotNull(spec);
        return spec.getScoringSpecProto();
    }
}
