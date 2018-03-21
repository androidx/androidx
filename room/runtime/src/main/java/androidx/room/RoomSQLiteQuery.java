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

package androidx.room;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.sqlite.db.SupportSQLiteProgram;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class is used as an intermediate place to keep binding arguments so that we can run
 * Cursor queries with correct types rather than passing everything as a string.
 * <p>
 * Because it is relatively a big object, they are pooled and must be released after each use.
 *
 * @hide
 */
@SuppressWarnings("unused")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RoomSQLiteQuery implements SupportSQLiteQuery, SupportSQLiteProgram {
    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    // Maximum number of queries we'll keep cached.
    static final int POOL_LIMIT = 15;
    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    // Once we hit POOL_LIMIT, we'll bring the pool size back to the desired number. We always
    // clear the bigger queries (# of arguments).
    static final int DESIRED_POOL_SIZE = 10;
    private volatile String mQuery;
    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    final long[] mLongBindings;
    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    final double[] mDoubleBindings;
    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    final String[] mStringBindings;
    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    final byte[][] mBlobBindings;

    @Binding
    private final int[] mBindingTypes;
    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    final int mCapacity;
    // number of arguments in the query
    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    int mArgCount;


    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    static final TreeMap<Integer, RoomSQLiteQuery> sQueryPool = new TreeMap<>();

    /**
     * Copies the given SupportSQLiteQuery and converts it into RoomSQLiteQuery.
     *
     * @param supportSQLiteQuery The query to copy from
     * @return A new query copied from the provided one.
     */
    public static RoomSQLiteQuery copyFrom(SupportSQLiteQuery supportSQLiteQuery) {
        final RoomSQLiteQuery query = RoomSQLiteQuery.acquire(
                supportSQLiteQuery.getSql(),
                supportSQLiteQuery.getArgCount());
        supportSQLiteQuery.bindTo(new SupportSQLiteProgram() {
            @Override
            public void bindNull(int index) {
                query.bindNull(index);
            }

            @Override
            public void bindLong(int index, long value) {
                query.bindLong(index, value);
            }

            @Override
            public void bindDouble(int index, double value) {
                query.bindDouble(index, value);
            }

            @Override
            public void bindString(int index, String value) {
                query.bindString(index, value);
            }

            @Override
            public void bindBlob(int index, byte[] value) {
                query.bindBlob(index, value);
            }

            @Override
            public void clearBindings() {
                query.clearBindings();
            }

            @Override
            public void close() {
                // ignored.
            }
        });
        return query;
    }

    /**
     * Returns a new RoomSQLiteQuery that can accept the given number of arguments and holds the
     * given query.
     *
     * @param query         The query to prepare
     * @param argumentCount The number of query arguments
     * @return A RoomSQLiteQuery that holds the given query and has space for the given number of
     * arguments.
     */
    @SuppressWarnings("WeakerAccess")
    public static RoomSQLiteQuery acquire(String query, int argumentCount) {
        synchronized (sQueryPool) {
            final Map.Entry<Integer, RoomSQLiteQuery> entry =
                    sQueryPool.ceilingEntry(argumentCount);
            if (entry != null) {
                sQueryPool.remove(entry.getKey());
                final RoomSQLiteQuery sqliteQuery = entry.getValue();
                sqliteQuery.init(query, argumentCount);
                return sqliteQuery;
            }
        }
        RoomSQLiteQuery sqLiteQuery = new RoomSQLiteQuery(argumentCount);
        sqLiteQuery.init(query, argumentCount);
        return sqLiteQuery;
    }

    private RoomSQLiteQuery(int capacity) {
        mCapacity = capacity;
        // because, 1 based indices... we don't want to offsets everything with 1 all the time.
        int limit = capacity + 1;
        //noinspection WrongConstant
        mBindingTypes = new int[limit];
        mLongBindings = new long[limit];
        mDoubleBindings = new double[limit];
        mStringBindings = new String[limit];
        mBlobBindings = new byte[limit][];
    }

    @SuppressWarnings("WeakerAccess")
    void init(String query, int argCount) {
        mQuery = query;
        mArgCount = argCount;
    }

    /**
     * Releases the query back to the pool.
     * <p>
     * After released, the statement might be returned when {@link #acquire(String, int)} is called
     * so you should never re-use it after releasing.
     */
    @SuppressWarnings("WeakerAccess")
    public void release() {
        synchronized (sQueryPool) {
            sQueryPool.put(mCapacity, this);
            prunePoolLocked();
        }
    }

    private static void prunePoolLocked() {
        if (sQueryPool.size() > POOL_LIMIT) {
            int toBeRemoved = sQueryPool.size() - DESIRED_POOL_SIZE;
            final Iterator<Integer> iterator = sQueryPool.descendingKeySet().iterator();
            while (toBeRemoved-- > 0) {
                iterator.next();
                iterator.remove();
            }
        }
    }

    @Override
    public String getSql() {
        return mQuery;
    }

    @Override
    public int getArgCount() {
        return mArgCount;
    }

    @Override
    public void bindTo(SupportSQLiteProgram program) {
        for (int index = 1; index <= mArgCount; index++) {
            switch (mBindingTypes[index]) {
                case NULL:
                    program.bindNull(index);
                    break;
                case LONG:
                    program.bindLong(index, mLongBindings[index]);
                    break;
                case DOUBLE:
                    program.bindDouble(index, mDoubleBindings[index]);
                    break;
                case STRING:
                    program.bindString(index, mStringBindings[index]);
                    break;
                case BLOB:
                    program.bindBlob(index, mBlobBindings[index]);
                    break;
            }
        }
    }

    @Override
    public void bindNull(int index) {
        mBindingTypes[index] = NULL;
    }

    @Override
    public void bindLong(int index, long value) {
        mBindingTypes[index] = LONG;
        mLongBindings[index] = value;
    }

    @Override
    public void bindDouble(int index, double value) {
        mBindingTypes[index] = DOUBLE;
        mDoubleBindings[index] = value;
    }

    @Override
    public void bindString(int index, String value) {
        mBindingTypes[index] = STRING;
        mStringBindings[index] = value;
    }

    @Override
    public void bindBlob(int index, byte[] value) {
        mBindingTypes[index] = BLOB;
        mBlobBindings[index] = value;
    }

    @Override
    public void close() {
        // no-op. not calling release because it is internal API.
    }

    /**
     * Copies arguments from another RoomSQLiteQuery into this query.
     *
     * @param other The other query, which holds the arguments to be copied.
     */
    public void copyArgumentsFrom(RoomSQLiteQuery other) {
        int argCount = other.getArgCount() + 1; // +1 for the binding offsets
        System.arraycopy(other.mBindingTypes, 0, mBindingTypes, 0, argCount);
        System.arraycopy(other.mLongBindings, 0, mLongBindings, 0, argCount);
        System.arraycopy(other.mStringBindings, 0, mStringBindings, 0, argCount);
        System.arraycopy(other.mBlobBindings, 0, mBlobBindings, 0, argCount);
        System.arraycopy(other.mDoubleBindings, 0, mDoubleBindings, 0, argCount);
    }

    @Override
    public void clearBindings() {
        Arrays.fill(mBindingTypes, NULL);
        Arrays.fill(mStringBindings, null);
        Arrays.fill(mBlobBindings, null);
        mQuery = null;
        // no need to clear others
    }

    private static final int NULL = 1;
    private static final int LONG = 2;
    private static final int DOUBLE = 3;
    private static final int STRING = 4;
    private static final int BLOB = 5;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NULL, LONG, DOUBLE, STRING, BLOB})
    @interface Binding {
    }
}
