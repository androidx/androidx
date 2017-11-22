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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;

/**
 * @hide
 */
// TODO: Remove as soon as we have better systems in place for this.
@RestrictTo(Scope.LIBRARY)
public class ContentProviderWrapper extends ContentProvider {

    private ContentProvider mImpl;

    /**
     * Triggers an attach with the object to wrap.
     */
    public void attachInfo(Context context, ProviderInfo info, ContentProvider impl) {
        mImpl = impl;
        mImpl.attachInfo(context, info);
        super.attachInfo(context, info);
    }

    @Override
    public final boolean onCreate() {
        return mImpl.onCreate();
    }

    @Nullable
    @Override
    public final Cursor query(@NonNull Uri uri, @Nullable String[] projection,
            @Nullable String selection, @Nullable String[] selectionArgs,
            @Nullable String sortOrder) {
        return mImpl.query(uri, projection, selection, selectionArgs, sortOrder);
    }

    @Nullable
    @Override
    @RequiresApi(28)
    public final Cursor query(@NonNull Uri uri, @Nullable String[] projection,
            @Nullable Bundle queryArgs, @Nullable CancellationSignal cancellationSignal) {
        return mImpl.query(uri, projection, queryArgs, cancellationSignal);
    }

    @Nullable
    @Override
    public final Cursor query(@NonNull Uri uri, @Nullable String[] projection,
            @Nullable String selection, @Nullable String[] selectionArgs,
            @Nullable String sortOrder, @Nullable CancellationSignal cancellationSignal) {
        return mImpl.query(uri, projection, selection, selectionArgs, sortOrder,
                cancellationSignal);
    }

    @Nullable
    @Override
    public final String getType(@NonNull Uri uri) {
        return mImpl.getType(uri);
    }

    @Nullable
    @Override
    public final Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return mImpl.insert(uri, values);
    }

    @Override
    public final int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        return mImpl.bulkInsert(uri, values);
    }

    @Override
    public final int delete(@NonNull Uri uri, @Nullable String selection,
            @Nullable String[] selectionArgs) {
        return mImpl.delete(uri, selection, selectionArgs);
    }

    @Override
    public final int update(@NonNull Uri uri, @Nullable ContentValues values,
            @Nullable String selection, @Nullable String[] selectionArgs) {
        return mImpl.update(uri, values, selection, selectionArgs);
    }

    @Nullable
    @Override
    public final Bundle call(@NonNull String method, @Nullable String arg,
            @Nullable Bundle extras) {
        return mImpl.call(method, arg, extras);
    }

    @Nullable
    @Override
    public final Uri canonicalize(@NonNull Uri url) {
        return mImpl.canonicalize(url);
    }
}
