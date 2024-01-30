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

package androidx.appsearch.platformstorage.converter;

import android.app.appsearch.AppSearchManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.platformstorage.PlatformStorage;
import androidx.core.util.Preconditions;

/**
 * Translates a Jetpack {@link androidx.appsearch.platformstorage.PlatformStorage.SearchContext}
 * into a platform {@link android.app.appsearch.AppSearchManager.SearchContext}.
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.S)
public final class SearchContextToPlatformConverter {
    private SearchContextToPlatformConverter() {}

    /**
     * Translates a Jetpack {@link androidx.appsearch.platformstorage.PlatformStorage.SearchContext}
     * into a platform {@link android.app.appsearch.AppSearchManager.SearchContext}.
     */
    @NonNull
    public static AppSearchManager.SearchContext toPlatformSearchContext(
            @NonNull PlatformStorage.SearchContext jetpackSearchContext) {
        Preconditions.checkNotNull(jetpackSearchContext);
        return new AppSearchManager.SearchContext.Builder(jetpackSearchContext.getDatabaseName())
                .build();
    }
}
