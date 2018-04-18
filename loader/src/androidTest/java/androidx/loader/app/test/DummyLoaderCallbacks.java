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

package androidx.loader.app.test;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

public class DummyLoaderCallbacks implements LoaderManager.LoaderCallbacks<Boolean> {
    private final Context mContext;

    public boolean mOnLoadFinished;
    public boolean mOnLoaderReset;

    public DummyLoaderCallbacks(Context context) {
        mContext = context;
    }

    @NonNull
    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        return new DummyLoader(mContext);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Boolean> loader, Boolean data) {
        mOnLoadFinished = true;
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Boolean> loader) {
        mOnLoaderReset = true;
    }
}
