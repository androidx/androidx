/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.slice.widget;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(api = 28)
class SliceMetricsWrapper extends SliceMetrics {

    private final android.app.slice.SliceMetrics mSliceMetrics;

    SliceMetricsWrapper(@NonNull Context context, @NonNull Uri uri) {
        this.mSliceMetrics = new android.app.slice.SliceMetrics(context, uri);
    }

    @Override
    protected void logVisible() {
        mSliceMetrics.logVisible();
    }

    @Override
    protected void logHidden() {
        mSliceMetrics.logHidden();
    }

    @Override
    protected void logTouch(int actionType, @NonNull Uri subSlice) {
        mSliceMetrics.logTouch(actionType, subSlice);
    }
}
