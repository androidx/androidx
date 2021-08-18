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

package androidx.appsearch.testutil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.localstorage.AppSearchLogger;
import androidx.appsearch.localstorage.stats.CallStats;
import androidx.appsearch.localstorage.stats.InitializeStats;
import androidx.appsearch.localstorage.stats.OptimizeStats;
import androidx.appsearch.localstorage.stats.PutDocumentStats;
import androidx.appsearch.localstorage.stats.RemoveStats;
import androidx.appsearch.localstorage.stats.SearchStats;
import androidx.appsearch.localstorage.stats.SetSchemaStats;

/**
 * Non-thread-safe simple logger implementation for testing.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SimpleTestLogger implements AppSearchLogger {
    /** Holds {@link CallStats} after logging. */
    @Nullable
    public CallStats mCallStats;
    /** Holds {@link PutDocumentStats} after logging. */
    @Nullable
    public PutDocumentStats mPutDocumentStats;
    /** Holds {@link InitializeStats} after logging. */
    @Nullable
    public InitializeStats mInitializeStats;
    /** Holds {@link SearchStats} after logging. */
    @Nullable
    public SearchStats mSearchStats;
    /** Holds {@link RemoveStats} after logging. */
    @Nullable
    public RemoveStats mRemoveStats;
    /** Holds {@link OptimizeStats} after logging. */
    @Nullable
    public OptimizeStats mOptimizeStats;
    /** Holds {@link SetSchemaStats} after logging. */
    @Nullable
    public SetSchemaStats mSetSchemaStats;

    @Override
    public void logStats(@NonNull CallStats stats) {
        mCallStats = stats;
    }

    @Override
    public void logStats(@NonNull PutDocumentStats stats) {
        mPutDocumentStats = stats;
    }

    @Override
    public void logStats(@NonNull InitializeStats stats) {
        mInitializeStats = stats;
    }

    @Override
    public void logStats(@NonNull SearchStats stats) {
        mSearchStats = stats;
    }

    @Override
    public void logStats(@NonNull RemoveStats stats) {
        mRemoveStats = stats;
    }

    @Override
    public void logStats(@NonNull OptimizeStats stats) {
        mOptimizeStats = stats;
    }

    @Override
    public void logStats(@NonNull SetSchemaStats stats) {
        mSetSchemaStats = stats;
    }
}
