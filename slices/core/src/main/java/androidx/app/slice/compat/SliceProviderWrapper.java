/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.app.slice.compat;

import static androidx.app.slice.SliceConvert.wrap;

import android.annotation.TargetApi;
import android.app.slice.Slice;
import android.app.slice.SliceProvider;
import android.app.slice.SliceSpec;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;

import java.util.List;

import androidx.app.slice.SliceConvert;

/**
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
@TargetApi(28)
public class SliceProviderWrapper extends SliceProvider {

    private androidx.app.slice.SliceProvider mSliceProvider;

    public SliceProviderWrapper(androidx.app.slice.SliceProvider provider) {
        mSliceProvider = provider;
    }

    @Override
    public boolean onCreate() {
        return mSliceProvider.onCreateSliceProvider();
    }

    @Override
    public Slice onBindSlice(Uri sliceUri, List<SliceSpec> supportedVersions) {
        androidx.app.slice.SliceProvider.setSpecs(wrap(supportedVersions));
        return SliceConvert.unwrap(mSliceProvider.onBindSlice(sliceUri));
    }

    /**
     * Maps intents to uris.
     */
    @Override
    public @NonNull Uri onMapIntentToUri(Intent intent) {
        return mSliceProvider.onMapIntentToUri(intent);
    }
}
