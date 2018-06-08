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

package androidx.contentpager.content;

import static org.junit.Assert.assertEquals;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.AbstractWindowedCursor;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * A stub data paging provider used for testing of paging support.
 * Ignores client supplied projections.
 */
public final class TestContentProvider extends ContentProvider {

    public static final String AUTHORITY = "androidx.contentpager.content.test.testpagingprovider";

    public static final String UNPAGED_PATH = "/un-paged";
    public static final String PAGED_PATH = "/paged";
    public static final String PAGED_WINDOWED_PATH = PAGED_PATH + "/windowed";

    public static final Uri UNPAGED_URI = new Uri.Builder()
            .scheme("content")
            .authority(AUTHORITY)
            .path(UNPAGED_PATH)
            .build();
    public static final Uri PAGED_URI = new Uri.Builder()
            .scheme("content")
            .authority(AUTHORITY)
            .path(PAGED_PATH)
            .build();
    public static final Uri PAGED_WINDOWED_URI = new Uri.Builder()
            .scheme("content")
            .authority(AUTHORITY)
            .path(PAGED_WINDOWED_PATH)
            .build();

    public static final String COLUMN_POS = "ColumnPos";
    public static final String COLUMN_A = "ColumnA";
    public static final String COLUMN_B = "ColumnB";
    public static final String COLUMN_C = "ColumnC";
    public static final String COLUMN_D = "ColumnD";
    public static final String[] PROJECTION = {
            COLUMN_POS,
            COLUMN_A,
            COLUMN_B,
            COLUMN_C,
            COLUMN_D
    };

    @VisibleForTesting
    public static final String RECORD_COUNT = "test-record-count";

    @VisibleForTesting
    public static final int DEFAULT_RECORD_COUNT = 567;

    private static final String TAG = "TestPagingProvider";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(
            Uri uri, @Nullable String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        return query(uri, projection, null, null);
    }

    @Override
    public Cursor query(Uri uri, String[] ignored, Bundle queryArgs,
            CancellationSignal cancellationSignal) {

        queryArgs = queryArgs != null ? queryArgs : Bundle.EMPTY;

        int recordCount = getIntValue(RECORD_COUNT, queryArgs, uri, DEFAULT_RECORD_COUNT);
        if (recordCount < 0) {
            throw new RuntimeException("Recordset size must be >= 0");
        }

        Cursor cursor = null;
        switch (uri.getPath()) {
            case UNPAGED_PATH:
                cursor = buildUnpagedResults(recordCount);
                break;
            case PAGED_PATH:
                cursor = buildPagedResults(uri, queryArgs, recordCount);
                break;
            case PAGED_WINDOWED_PATH:
                cursor = buildPagedWindowedResults(uri, queryArgs, recordCount);
                break;
            default:
                throw new IllegalArgumentException("Unrecognized path: " + uri.getPath());
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    /**
     * Return a int value specified in Bundle key, Uri query arg, or fallback default value.
     */
    private static int getIntValue(String key, Bundle queryArgs, Uri uri, int defaultValue) {
        int value = queryArgs.getInt(key, Integer.MIN_VALUE);
        if (value != Integer.MIN_VALUE) {
            return value;
        }

        @Nullable String argValue = uri.getQueryParameter(key);
        if (argValue != null) {
            try {
                return Integer.parseInt(argValue);
            } catch (NumberFormatException ignored) {
            }
        }

        return defaultValue;
    }

    private MatrixCursor buildPagedResults(Uri uri, Bundle queryArgs, int recordsetSize) {
        int offset = getIntValue(ContentResolver.QUERY_ARG_OFFSET, queryArgs, uri, 0);
        int limit = getIntValue(ContentResolver.QUERY_ARG_LIMIT, queryArgs, uri, recordsetSize);

        MatrixCursor c = createInMemoryCursor();
        Bundle extras = c.getExtras();

        // Calculate the number of items to include in the cursor.
        int numItems = constrain(recordsetSize - offset, 0, limit);

        // Build the paged result set.
        for (int i = offset; i < offset + numItems; i++) {
            fillRow(c.newRow(), i);
        }

        extras.putStringArray(ContentResolver.EXTRA_HONORED_ARGS, new String[] {
                ContentResolver.QUERY_ARG_OFFSET,
                ContentResolver.QUERY_ARG_LIMIT
        });
        extras.putInt(ContentResolver.EXTRA_TOTAL_COUNT, recordsetSize);
        return c;
    }

    private AbstractWindowedCursor buildPagedWindowedResults(
            Uri uri, Bundle queryArgs, int recordsetSize) {
        int offset = getIntValue(ContentResolver.QUERY_ARG_OFFSET, queryArgs, uri, 0);
        int limit = getIntValue(ContentResolver.QUERY_ARG_LIMIT, queryArgs, uri, recordsetSize);

        int windowSize = limit - 1;

        TestWindowedCursor c = new TestWindowedCursor(PROJECTION, recordsetSize);
        CursorWindow window = c.getWindow();
        window.setNumColumns(PROJECTION.length);

        Bundle extras = c.getExtras();

        // Build the unpaged result set.
        for (int row = 0; row < windowSize; row++) {
            if (!window.allocRow()) {
                break;
            }
            if (!fillRow(window, row)) {
                window.freeLastRow();
                break;
            }
        }

        extras.putStringArray(ContentResolver.EXTRA_HONORED_ARGS, new String[] {
                ContentResolver.QUERY_ARG_OFFSET,
                ContentResolver.QUERY_ARG_LIMIT
        });
        extras.putInt(ContentResolver.EXTRA_TOTAL_COUNT, recordsetSize);
        return c;
    }

    private MatrixCursor buildUnpagedResults(int recordsetSize) {
        MatrixCursor c = createInMemoryCursor();

        // Build the unpaged result set.
        for (int i = 0; i < recordsetSize; i++) {
            fillRow(c.newRow(), i);
        }

        return c;
    }

    /**
     * Returns data type of the given object's value.
     *<p>
     * Returned values are
     * <ul>
     *   <li>{@link Cursor#FIELD_TYPE_NULL}</li>
     *   <li>{@link Cursor#FIELD_TYPE_INTEGER}</li>
     *   <li>{@link Cursor#FIELD_TYPE_FLOAT}</li>
     *   <li>{@link Cursor#FIELD_TYPE_STRING}</li>
     *   <li>{@link Cursor#FIELD_TYPE_BLOB}</li>
     *</ul>
     *</p>
     */
    public static int getTypeOfObject(Object obj) {
        if (obj == null) {
            return Cursor.FIELD_TYPE_NULL;
        } else if (obj instanceof byte[]) {
            return Cursor.FIELD_TYPE_BLOB;
        } else if (obj instanceof Float || obj instanceof Double) {
            return Cursor.FIELD_TYPE_FLOAT;
        } else if (obj instanceof Long || obj instanceof Integer
                || obj instanceof Short || obj instanceof Byte) {
            return Cursor.FIELD_TYPE_INTEGER;
        } else {
            return Cursor.FIELD_TYPE_STRING;
        }
    }

    private MatrixCursor createInMemoryCursor() {
        MatrixCursor c = new MatrixCursor(PROJECTION);
        Bundle extras = new Bundle();
        c.setExtras(extras);
        return c;
    }

    private void fillRow(RowBuilder row, int rowId) {
        row.add(createCellValue(rowId, 0));
        row.add(createCellValue(rowId, 1));
        row.add(createCellValue(rowId, 2));
        row.add(createCellValue(rowId, 3));
        row.add(createCellValue(rowId, 4));
    }

    /**
     * @return true if the row was successfully populated. If false, caller should freeLastRow.
     */
    private static boolean fillRow(CursorWindow window, int row) {
        if (!window.putLong((int) createCellValue(row, 0), row, 0)) {
            return false;
        }
        for (int i = 1; i < PROJECTION.length; i++) {
            if (!window.putString((String) createCellValue(row, i), row, i)) {
                return false;
            }
        }
        return true;
    }

    private static Object createCellValue(int row, int col) {
        switch(col) {
            case 0:
                return row;
            case 1:
                return "--aaa--" + row;
            case 2:
                return "**bbb**" + row;
            case 3:
                return ("^^ccc^^" + row);
            case 4:
                return "##ddd##" + row;
            default:
                throw new IllegalArgumentException("Unsupported column: " + col);
        }
    }

    /**
     * Asserts that the value at the current cursor position x column
     * is expected test data for the supplied row.
     *
     * <p>Cursor must be pre-positioned.
     *
     * @param cursor must be prepositioned to the row to be tested.
     * @param row row value expected to be reflected in cell. This can be different
     *            than the cursor position due to paging.
     * @param column
     */
    @VisibleForTesting
    public static void assertExpectedCellValue(Cursor cursor, int row, int column) {
        int type = cursor.getType(column);
        switch(type) {
            case Cursor.FIELD_TYPE_NULL:
                throw new UnsupportedOperationException("Not implemented.");
            case Cursor.FIELD_TYPE_INTEGER:
                assertEquals(createCellValue(row, column), cursor.getInt(column));
                break;
            case Cursor.FIELD_TYPE_FLOAT:
                assertEquals(createCellValue(row, column), cursor.getDouble(column));
                break;
            case Cursor.FIELD_TYPE_BLOB:
                assertEquals(createCellValue(row, column), cursor.getBlob(column));
                break;
            case Cursor.FIELD_TYPE_STRING:
                assertEquals(createCellValue(row, column), cursor.getString(column));
                break;
            default:
                throw new UnsupportedOperationException("Unknown column type: " + type);
        }
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    private static int constrain(int amount, int low, int high) {
        return amount < low ? low : (amount > high ? high : amount);
    }

    /**
     * Returns a Uri that includes paging information embedded in the URI.
     * This allows a test client to force paged results when running on older SDKs...
     * pre Android O SDKs lacking the ContentResolver#query w/ Bundle override
     * necessary for paging.
     */
    public static Uri forcePagingSpec(Uri uri, int offset, int limit) {
        assert (uri.getPath().equals(TestContentProvider.PAGED_PATH)
                || uri.getPath().equals(TestContentProvider.PAGED_WINDOWED_PATH));
        return uri.buildUpon()
                .appendQueryParameter(ContentResolver.QUERY_ARG_OFFSET, String.valueOf(offset))
                .appendQueryParameter(ContentResolver.QUERY_ARG_LIMIT, String.valueOf(limit))
                .build();
    }

    public static Uri forceRecordCount(Uri uri, int recordCount) {
        return uri.buildUpon()
                .appendQueryParameter(RECORD_COUNT, String.valueOf(recordCount))
                .build();
    }

    private static final class TestWindowedCursor extends AbstractWindowedCursor {

        private final String[] mProjection;
        private final int mCount;
        private final Bundle mExtras;

        TestWindowedCursor(String[] projection, int count) {
            mProjection = projection;
            mCount = count;
            mExtras = new Bundle();

            setWindow(new CursorWindow("stevie"));
        }

        @Override
        public Bundle getExtras() {
            return mExtras;
        }

        @Override
        public int getCount() {
            return mCount;
        }

        @Override
        public String[] getColumnNames() {
            return mProjection;
        }
    }
}
