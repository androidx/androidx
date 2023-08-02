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

import android.content.UriMatcher;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

/**
 * Utility methods for {@link Uri} for AppSearch Documents.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class EntityUriUtils {
    private static final String ENTITY_URI_SCHEME = "appsearch";
    private static final String ENTITY_URI_AUTHORITY = "__shortcut_adapter_db__";
    private static final String ENTITY_URI_NS_PATH = "__shortcut_adapter_ns__";

    private static final int ENTITY_ID = 1;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(ENTITY_URI_AUTHORITY, ENTITY_URI_NS_PATH + "/*", ENTITY_ID);
    }

    /**
     * If the uri is a valid AppSearch entity uri, then return its entity id, otherwise return
     * null.
     */
    // TODO (ddong): move this to ShortcutAdapter.
    @Nullable
    public static String getEntityId(@NonNull String entityUriString) {
        Preconditions.checkNotNull(entityUriString);

        Uri entityUri = Uri.parse(entityUriString);
        if (!ENTITY_URI_SCHEME.equals(entityUri.getScheme())) {
            return null;
        }

        int match = sUriMatcher.match(entityUri);
        if (match == ENTITY_ID) {
            return entityUri.getLastPathSegment();
        }

        return null;
    }

    private EntityUriUtils() {}
}
