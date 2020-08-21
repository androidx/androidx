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

import com.google.android.icing.proto.DocumentProto;

/**
 * Translates a {@link GenericDocument} into a {@link DocumentProto}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class GenericDocumentToProtoConverter {
    private GenericDocumentToProtoConverter() {}

    /** Converts a {@link GenericDocument} into a {@link DocumentProto}. */
    @NonNull
    public static DocumentProto convert(@NonNull GenericDocument document) {
        Preconditions.checkNotNull(document);
        return document.getProto();
    }

    /** Converts a {@link DocumentProto} into a {@link GenericDocument}. */
    @NonNull
    public static GenericDocument convert(@NonNull DocumentProto proto) {
        Preconditions.checkNotNull(proto);
        return new GenericDocument(proto);
    }
}
