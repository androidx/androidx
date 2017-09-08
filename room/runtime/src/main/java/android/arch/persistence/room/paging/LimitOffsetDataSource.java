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

package android.arch.persistence.room.paging;

import android.arch.paging.TiledDataSource;
import android.arch.persistence.room.InvalidationTracker;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.RoomSQLiteQuery;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import java.util.List;
import java.util.Set;

/**
 * A simple data source implementation that uses Limit & Offset to page the query.
 * <p>
 * This is NOT the most efficient way to do paging on SQLite. It is
 * <a href="http://www.sqlite.org/cvstrac/wiki?p=ScrollingCursor">recommended</a> to use an indexed
 * ORDER BY statement but that requires a more complex API. This solution is technically equal to
 * receiving a {@link Cursor} from a large query but avoids the need to manually manage it, and
 * never returns inconsistent data if it is invalidated.
 *
 * @param <T> Data type returned by the data source.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class LimitOffsetDataSource<T> extends TiledDataSource<T> {
    private final RoomSQLiteQuery mSourceQuery;
    private final String mCountQuery;
    private final String mLimitOffsetQuery;
    private final RoomDatabase mDb;
    @SuppressWarnings("FieldCanBeLocal")
    private final InvalidationTracker.Observer mObserver;

    protected LimitOffsetDataSource(RoomDatabase db, RoomSQLiteQuery query, String... tables) {
        mDb = db;
        mSourceQuery = query;
        mCountQuery = "SELECT COUNT(*) FROM ( " + mSourceQuery.getSql() + " )";
        mLimitOffsetQuery = "SELECT * FROM ( " + mSourceQuery.getSql() + " ) LIMIT ? OFFSET ?";
        mObserver = new InvalidationTracker.Observer(tables) {
            @Override
            public void onInvalidated(@NonNull Set<String> tables) {
                invalidate();
            }
        };
        db.getInvalidationTracker().addWeakObserver(mObserver);
    }

    @Override
    public int countItems() {
        final RoomSQLiteQuery sqLiteQuery = RoomSQLiteQuery.acquire(mCountQuery,
                mSourceQuery.getArgCount());
        sqLiteQuery.copyArgumentsFrom(mSourceQuery);
        Cursor cursor = mDb.query(sqLiteQuery);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return 0;
        } finally {
            cursor.close();
            sqLiteQuery.release();
        }
    }

    @Override
    public boolean isInvalid() {
        mDb.getInvalidationTracker().refreshVersionsSync();
        return super.isInvalid();
    }

    @SuppressWarnings("WeakerAccess")
    protected abstract List<T> convertRows(Cursor cursor);

    @Nullable
    @Override
    public List<T> loadRange(int startPosition, int loadCount) {
        final RoomSQLiteQuery sqLiteQuery = RoomSQLiteQuery.acquire(mLimitOffsetQuery,
                mSourceQuery.getArgCount() + 2);
        sqLiteQuery.copyArgumentsFrom(mSourceQuery);
        sqLiteQuery.bindLong(sqLiteQuery.getArgCount() - 1, loadCount);
        sqLiteQuery.bindLong(sqLiteQuery.getArgCount(), startPosition);
        Cursor cursor = mDb.query(sqLiteQuery);

        try {
            return convertRows(cursor);
        } finally {
            cursor.close();
            sqLiteQuery.release();
        }
    }
}
