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

package androidx.core.google.shortcuts.converters;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.GenericDocument;
import androidx.core.google.shortcuts.utils.ConverterUtils;
import androidx.core.util.Preconditions;

import com.google.android.gms.appindex.AppIndexInvalidArgumentException;
import com.google.android.gms.appindex.Indexable;

/**
 * Default converter for all {@link GenericDocument}. This converter will map each property into
 * its respective {@link Indexable} field. If a schema type is not registered with a specific
 * converter, then this should be used as fallback.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class GenericDocumentConverter implements AppSearchDocumentConverter {
    private static final String TAG = "GenericDocumentConverte"; // NOTYPO

    @NonNull
    @Override
    public Indexable.Builder convertGenericDocument(
            @NonNull Context context,
            @NonNull GenericDocument genericDocument) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(genericDocument);

        Indexable.Builder indexableBuilder = ConverterUtils.buildBaseIndexableFromGenericDocument(
                context, genericDocument.getSchemaType(), genericDocument);

        for (String property : genericDocument.getPropertyNames()) {
            Object rawProperty = genericDocument.getProperty(property);
            if (rawProperty instanceof String[]) {
                indexableBuilder.put(property, (String[]) rawProperty);
            } else if (rawProperty instanceof long[]) {
                indexableBuilder.put(property, (long[]) rawProperty);
            } else if (rawProperty instanceof double[]) {
                // TODO (b/205890624): add conversion for double once it's supported in Indexable.
                Log.w(TAG, "Property type double for " + property + " is not supported.");
            } else if (rawProperty instanceof boolean[]) {
                indexableBuilder.put(property, (boolean[]) rawProperty);
            } else if (rawProperty instanceof byte[][]) {
                // TODO (b/205890624): add conversion for byte[] once it's supported in Indexable.
                Log.w(TAG, "Property type byte[] for " + property + " is not supported.");
            } else if (rawProperty instanceof GenericDocument[]) {
                try {
                    indexableBuilder.put(property,
                            convertGenericDocuments(context, (GenericDocument[]) rawProperty));
                } catch (AppIndexInvalidArgumentException e) {
                    Log.e(TAG, "Cannot convert GenericDocument for property " + property);
                }
            } else {
                Log.e(TAG, "Undefined property type from " + property);
            }
        }
        return indexableBuilder;
    }

    private Indexable[] convertGenericDocuments(Context context,
            GenericDocument[] genericDocuments) {
        Indexable[] indexables = new Indexable[genericDocuments.length];
        for (int i = 0; i < genericDocuments.length; i++) {
            indexables[i] = convertGenericDocument(context, genericDocuments[i]).build();
        }

        return indexables;
    }
}
