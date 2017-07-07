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

package com.example.android.contentprovidersample.provider;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.android.contentprovidersample.data.Cheese;
import com.example.android.contentprovidersample.data.CheeseDao;
import com.example.android.contentprovidersample.data.SampleDatabase;

import java.util.ArrayList;


/**
 * A {@link ContentProvider} based on a Room database.
 *
 * <p>Note that you don't need to implement a ContentProvider unless you want to expose the data
 * outside your process or your application already uses a ContentProvider.</p>
 */
public class SampleContentProvider extends ContentProvider {

    /** The authority of this content provider. */
    public static final String AUTHORITY = "com.example.android.contentprovidersample.provider";

    /** The URI for the Cheese table. */
    public static final Uri URI_CHEESE = Uri.parse(
            "content://" + AUTHORITY + "/" + Cheese.TABLE_NAME);

    /** The match code for some items in the Cheese table. */
    private static final int CODE_CHEESE_DIR = 1;

    /** The match code for an item in the Cheese table. */
    private static final int CODE_CHEESE_ITEM = 2;

    /** The URI matcher. */
    private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        MATCHER.addURI(AUTHORITY, Cheese.TABLE_NAME, CODE_CHEESE_DIR);
        MATCHER.addURI(AUTHORITY, Cheese.TABLE_NAME + "/*", CODE_CHEESE_ITEM);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
            @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        final int code = MATCHER.match(uri);
        if (code == CODE_CHEESE_DIR || code == CODE_CHEESE_ITEM) {
            final Context context = getContext();
            if (context == null) {
                return null;
            }
            CheeseDao cheese = SampleDatabase.getInstance(context).cheese();
            final Cursor cursor;
            if (code == CODE_CHEESE_DIR) {
                cursor = cheese.selectAll();
            } else {
                cursor = cheese.selectById(ContentUris.parseId(uri));
            }
            cursor.setNotificationUri(context.getContentResolver(), uri);
            return cursor;
        } else {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (MATCHER.match(uri)) {
            case CODE_CHEESE_DIR:
                return "vnd.android.cursor.dir/" + AUTHORITY + "." + Cheese.TABLE_NAME;
            case CODE_CHEESE_ITEM:
                return "vnd.android.cursor.item/" + AUTHORITY + "." + Cheese.TABLE_NAME;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        switch (MATCHER.match(uri)) {
            case CODE_CHEESE_DIR:
                final Context context = getContext();
                if (context == null) {
                    return null;
                }
                final long id = SampleDatabase.getInstance(context).cheese()
                        .insert(Cheese.fromContentValues(values));
                context.getContentResolver().notifyChange(uri, null);
                return ContentUris.withAppendedId(uri, id);
            case CODE_CHEESE_ITEM:
                throw new IllegalArgumentException("Invalid URI, cannot insert with ID: " + uri);
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
            @Nullable String[] selectionArgs) {
        switch (MATCHER.match(uri)) {
            case CODE_CHEESE_DIR:
                throw new IllegalArgumentException("Invalid URI, cannot update without ID" + uri);
            case CODE_CHEESE_ITEM:
                final Context context = getContext();
                if (context == null) {
                    return 0;
                }
                final int count = SampleDatabase.getInstance(context).cheese()
                        .deleteById(ContentUris.parseId(uri));
                context.getContentResolver().notifyChange(uri, null);
                return count;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
            @Nullable String[] selectionArgs) {
        switch (MATCHER.match(uri)) {
            case CODE_CHEESE_DIR:
                throw new IllegalArgumentException("Invalid URI, cannot update without ID" + uri);
            case CODE_CHEESE_ITEM:
                final Context context = getContext();
                if (context == null) {
                    return 0;
                }
                final Cheese cheese = Cheese.fromContentValues(values);
                cheese.id = ContentUris.parseId(uri);
                final int count = SampleDatabase.getInstance(context).cheese()
                        .update(cheese);
                context.getContentResolver().notifyChange(uri, null);
                return count;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(
            @NonNull ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        final Context context = getContext();
        if (context == null) {
            return new ContentProviderResult[0];
        }
        final SampleDatabase database = SampleDatabase.getInstance(context);
        database.beginTransaction();
        try {
            final ContentProviderResult[] result = super.applyBatch(operations);
            database.setTransactionSuccessful();
            return result;
        } finally {
            database.endTransaction();
        }
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] valuesArray) {
        switch (MATCHER.match(uri)) {
            case CODE_CHEESE_DIR:
                final Context context = getContext();
                if (context == null) {
                    return 0;
                }
                final SampleDatabase database = SampleDatabase.getInstance(context);
                final Cheese[] cheeses = new Cheese[valuesArray.length];
                for (int i = 0; i < valuesArray.length; i++) {
                    cheeses[i] = Cheese.fromContentValues(valuesArray[i]);
                }
                return database.cheese().insertAll(cheeses).length;
            case CODE_CHEESE_ITEM:
                throw new IllegalArgumentException("Invalid URI, cannot insert with ID: " + uri);
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

}
