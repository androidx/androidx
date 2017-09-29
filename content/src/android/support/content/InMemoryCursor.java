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

package android.support.content;

import static android.support.v4.util.Preconditions.checkArgument;

import android.database.AbstractCursor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RestrictTo;

/**
 * A {@link Cursor} implementation that stores information in-memory, in a type-safe fashion.
 * Values are stored, when possible, as primitives to avoid the need for the autoboxing (as is
 * necessary when working with MatrixCursor).
 *
 * <p>Unlike {@link android.database.MatrixCursor}, this cursor is not mutable at runtime.
 * It exists solely as a destination for data copied by {@link ContentPager} from a source
 * Cursor when a page is being synthesized. It is not anticipated at this time that this
 * will be useful outside of this package. As such it is immutable once constructed.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
final class InMemoryCursor extends AbstractCursor {

    private static final int NUM_TYPES = 5;

    private final String[] mColumnNames;
    private final int mRowCount;

    // This is an index of column, by type. Maps back to position
    // in native array.
    // E.g. if we have columns typed like [string, int, int, string, int, int]
    // the values in this index will be:
    // mTypedColumnIndex[string][0] == 0
    // mTypedColumnIndex[int][1] == 0
    // mTypedColumnIndex[int][2] == 1
    // mTypedColumnIndex[string][3] == 1
    // mTypedColumnIndex[int][4] == 2
    // mTypedColumnIndex[int][4] == 3
    // This allows us to calculate the number of cells by type in a row
    // which, in turn, allows us to calculate the size of the primitive storage arrays.
    // We also use this information to lookup a particular typed value given
    // the row offset and column offset.
    private final int[][] mTypedColumnIndex;

    // simple index to column to type.
    private final int[] mColumnType;

    // count of number of columns by type.
    private final int[] mColumnTypeCount;

    private final Bundle mExtras;

    private final ObserverRelay mObserverRelay;

    // Row data decomposed by type.
    private long[] mLongs;
    private double[] mDoubles;
    private byte[][] mBlobs;
    private String[] mStrings;

    /**
     * @param cursor source of data to copy. Ownership is reserved to the called, meaning
     *               we won't ever close it.
     */
    InMemoryCursor(Cursor cursor, int offset, int length, int disposition) {
        checkArgument(offset < cursor.getCount());

        // NOTE: The cursor could simply be saved to a field, but we choose to wrap
        // in a dedicated relay class to avoid hanging directly onto a reference
        // to the cursor...so future authors are not enticed to think there's
        // a live link between the delegate cursor and this cursor.
        mObserverRelay = new ObserverRelay(cursor);

        mColumnNames = cursor.getColumnNames();
        mRowCount = Math.min(length, cursor.getCount() - offset);
        int numColumns = cursor.getColumnCount();

        mExtras = ContentPager.buildExtras(cursor.getExtras(), cursor.getCount(), disposition);

        mColumnType = new int[numColumns];
        mTypedColumnIndex = new int[NUM_TYPES][numColumns];
        mColumnTypeCount = new int[NUM_TYPES];

        if (!cursor.moveToFirst()) {
            throw new RuntimeException("Can't position cursor to first row.");
        }

        for (int col = 0; col < numColumns; col++) {
            int type = cursor.getType(col);
            mColumnType[col] = type;
            mTypedColumnIndex[type][col] = mColumnTypeCount[type]++;
        }

        mLongs = new long[mRowCount * mColumnTypeCount[FIELD_TYPE_INTEGER]];
        mDoubles = new double[mRowCount * mColumnTypeCount[FIELD_TYPE_FLOAT]];
        mBlobs = new byte[mRowCount * mColumnTypeCount[FIELD_TYPE_BLOB]][];
        mStrings = new String[mRowCount * mColumnTypeCount[FIELD_TYPE_STRING]];

        for (int row = 0; row < mRowCount; row++) {
            if (!cursor.moveToPosition(offset + row)) {
                throw new RuntimeException("Unable to position cursor.");
            }

            // Now copy data from the row into primitive arrays.
            for (int col = 0; col < mColumnType.length; col++) {
                int type = mColumnType[col];
                int position = getCellPosition(row, col, type);

                switch(type) {
                    case FIELD_TYPE_NULL:
                        throw new UnsupportedOperationException("Not implemented.");
                    case FIELD_TYPE_INTEGER:
                        mLongs[position] = cursor.getLong(col);
                        break;
                    case FIELD_TYPE_FLOAT:
                        mDoubles[position] = cursor.getDouble(col);
                        break;
                    case FIELD_TYPE_BLOB:
                        mBlobs[position] = cursor.getBlob(col);
                        break;
                    case FIELD_TYPE_STRING:
                        mStrings[position] = cursor.getString(col);
                        break;
                }
            }
        }
    }

    @Override
    public Bundle getExtras() {
        return mExtras;
    }

    // Returns the "cell" position for a specific row+column+type.
    private int getCellPosition(int row,  int col, int type) {
        return (row * mColumnTypeCount[type]) + mTypedColumnIndex[type][col];
    }

    @Override
    public int getCount() {
        return mRowCount;
    }

    @Override
    public String[] getColumnNames() {
        return mColumnNames;
    }

    @Override
    public short getShort(int column) {
        checkValidColumn(column);
        checkValidPosition();
        return (short) mLongs[getCellPosition(getPosition(), column, FIELD_TYPE_INTEGER)];
    }

    @Override
    public int getInt(int column) {
        checkValidColumn(column);
        checkValidPosition();
        return (int) mLongs[getCellPosition(getPosition(), column, FIELD_TYPE_INTEGER)];
    }

    @Override
    public long getLong(int column) {
        checkValidColumn(column);
        checkValidPosition();
        return mLongs[getCellPosition(getPosition(), column, FIELD_TYPE_INTEGER)];
    }

    @Override
    public float getFloat(int column) {
        checkValidColumn(column);
        checkValidPosition();
        return (float) mDoubles[getCellPosition(getPosition(), column, FIELD_TYPE_FLOAT)];
    }

    @Override
    public double getDouble(int column) {
        checkValidColumn(column);
        checkValidPosition();
        return mDoubles[getCellPosition(getPosition(), column, FIELD_TYPE_FLOAT)];
    }

    @Override
    public byte[] getBlob(int column) {
        checkValidColumn(column);
        checkValidPosition();
        return mBlobs[getCellPosition(getPosition(), column, FIELD_TYPE_BLOB)];
    }

    @Override
    public String getString(int column) {
        checkValidColumn(column);
        checkValidPosition();
        return mStrings[getCellPosition(getPosition(), column, FIELD_TYPE_STRING)];
    }

    @Override
    public int getType(int column) {
        checkValidColumn(column);
        return mColumnType[column];
    }

    @Override
    public boolean isNull(int column) {
        checkValidColumn(column);
        switch (mColumnType[column]) {
            case FIELD_TYPE_STRING:
                return getString(column) != null;
            case FIELD_TYPE_BLOB:
                return getBlob(column) != null;
            default:
                return false;
        }
    }

    private void checkValidPosition() {
        if (getPosition() < 0) {
            throw new CursorIndexOutOfBoundsException("Before first row.");
        }
        if (getPosition() >= mRowCount) {
            throw new CursorIndexOutOfBoundsException("After last row.");
        }
    }

    private void checkValidColumn(int column) {
        if (column < 0 || column >= mColumnNames.length) {
            throw new CursorIndexOutOfBoundsException(
                    "Requested column: " + column + ", # of columns: " + mColumnNames.length);
        }
    }

    @Override
    public void registerContentObserver(ContentObserver observer) {
        mObserverRelay.registerContentObserver(observer);
    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        mObserverRelay.unregisterContentObserver(observer);
    }

    private static class ObserverRelay extends ContentObserver {
        private final Cursor mCursor;

        ObserverRelay(Cursor cursor) {
            super(new Handler(Looper.getMainLooper()));
            mCursor = cursor;
        }

        void registerContentObserver(ContentObserver observer) {
            mCursor.registerContentObserver(observer);
        }

        void unregisterContentObserver(ContentObserver observer) {
            mCursor.unregisterContentObserver(observer);
        }
    }
}
