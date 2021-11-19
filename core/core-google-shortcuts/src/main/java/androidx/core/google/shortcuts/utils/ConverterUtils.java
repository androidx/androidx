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

package androidx.core.google.shortcuts.utils;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.GenericDocument;
import androidx.core.google.shortcuts.converters.AppSearchDocumentConverter;
import androidx.core.google.shortcuts.converters.IndexableKeys;
import androidx.core.util.Preconditions;

import com.google.firebase.appindexing.Indexable;

/**
 * Utility methods for {@link AppSearchDocumentConverter}.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class ConverterUtils {
    /** Creates an {@link Indexable.Builder} from {@link GenericDocument} with common fields set. */
    @NonNull
    public static Indexable.Builder buildBaseIndexableFromGenericDocument(
            @NonNull Context context,
            @NonNull GenericDocument genericDocument) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(genericDocument);

        return new Indexable.Builder(genericDocument.getSchemaType())
                .setId(genericDocument.getId())
                // TODO (b/206020715): remove name when it's no longer a required field.
                .setName(genericDocument.getNamespace())
                .setMetadata(new Indexable.Metadata.Builder().setScore(genericDocument.getScore()))
                .setUrl(ShortcutUtils.getIndexableUrl(context, genericDocument.getId()))
                .put(IndexableKeys.NAMESPACE, genericDocument.getNamespace())
                .put(IndexableKeys.CREATION_TIMESTAMP_MILLIS,
                        genericDocument.getCreationTimestampMillis())
                .put(IndexableKeys.TTL_MILLIS, genericDocument.getTtlMillis());
    }

    private ConverterUtils() {}
}
