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
package androidx.tvprovider.media.tv;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Build;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.tvprovider.media.tv.TvContractCompat.WatchNextPrograms;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
import java.util.Set;

/**
 * A convenience class to access {@link WatchNextPrograms} entries in the system content
 * provider.
 *
 * <p>This class makes it easy to insert or retrieve a program from the system content provider,
 * which is defined in {@link TvContractCompat}.
 *
 * <p>Usage example when inserting a "watch next" program:
 * <pre>
 * WatchNextProgram watchNextProgram = new WatchNextProgram.Builder()
 *         .setWatchNextType(WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
 *         .setType(PreviewPrograms.TYPE_MOVIE)
 *         .setTitle("Program Title")
 *         .setDescription("Program Description")
 *         .setPosterArtUri(Uri.parse("http://example.com/poster_art.png"))
 *         // Set more attributes...
 *         .build();
 * Uri watchNextProgramUri = getContentResolver().insert(WatchNextPrograms.CONTENT_URI,
 *         watchNextProgram.toContentValues());
 * </pre>
 *
 * <p>Usage example when retrieving a "watch next" program:
 * <pre>
 * WatchNextProgram watchNextProgram;
 * try (Cursor cursor = resolver.query(watchNextProgramUri, null, null, null, null)) {
 *     if (cursor != null && cursor.getCount() != 0) {
 *         cursor.moveToNext();
 *         watchNextProgram = WatchNextProgram.fromCursor(cursor);
 *     }
 * }
 * </pre>
 *
 * <p>Usage example when updating an existing "watch next" program:
 * <pre>
 * WatchNextProgram updatedProgram = new WatchNextProgram.Builder(watchNextProgram)
 *         .setLastEngagementTimeUtcMillis(System.currentTimeMillis())
 *         .build();
 * getContentResolver().update(TvContractCompat.buildWatchNextProgramUri(updatedProgram.getId()),
 *         updatedProgram.toContentValues(), null, null);
 * </pre>
 *
 * <p>Usage example when deleting a "watch next" program:
 * <pre>
 * getContentResolver().delete(TvContractCompat.buildWatchNextProgramUri(existingProgram.getId()),
 *         null, null);
 * </pre>
 */
@SuppressWarnings("HiddenSuperclass")
public final class WatchNextProgram extends BasePreviewProgram {
    /**
     * The projection for a {@link WatchNextProgram} query.
     * <p> This provides a array of strings containing the columns to be used in the
     * query and in creating a Cursor object, which is used to iterate through the rows in the
     * table.
     */
    @NonNull
    public static final String[] PROJECTION = getProjection();

    private static final long INVALID_LONG_VALUE = -1;

    @IntDef({
            WATCH_NEXT_TYPE_UNKNOWN,
            WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE,
            WatchNextPrograms.WATCH_NEXT_TYPE_NEXT,
            WatchNextPrograms.WATCH_NEXT_TYPE_NEW,
            WatchNextPrograms.WATCH_NEXT_TYPE_WATCHLIST,
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public @interface WatchNextType {
    }

    /**
     * The unknown watch next type. Use this type when the actual type is not known.
     */
    public static final int WATCH_NEXT_TYPE_UNKNOWN = -1;

    WatchNextProgram(Builder builder) {
        super(builder);
    }

    /**
     * @return The value of {@link WatchNextPrograms#COLUMN_WATCH_NEXT_TYPE} for the program,
     * or {@link #WATCH_NEXT_TYPE_UNKNOWN} if it's unknown.
     */
    public @WatchNextType int getWatchNextType() {
        Integer i = mValues.getAsInteger(WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE);
        return i == null ? WATCH_NEXT_TYPE_UNKNOWN : i;
    }

    /**
     * @return The value of {@link WatchNextPrograms#COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS} for the
     * program.
     */
    public long getLastEngagementTimeUtcMillis() {
        Long l = mValues.getAsLong(WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS);
        return l == null ? INVALID_LONG_VALUE : l;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof WatchNextProgram)) {
            return false;
        }
        return mValues.equals(((WatchNextProgram) other).mValues);
    }

    /**
     * Indicates whether some other WatchNextProgram has any set attribute that is different from
     * this WatchNextProgram's respective attributes. An attribute is considered "set" if its key
     * is present in the ContentValues vector.
     */
    public boolean hasAnyUpdatedValues(WatchNextProgram update) {
        Set<String> updateKeys = update.mValues.keySet();
        for (String key : updateKeys) {
            Object updateValue = update.mValues.get(key);
            Object currValue = mValues.get(key);
            if (!Objects.deepEquals(updateValue, currValue)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "WatchNextProgram{" + mValues.toString() + "}";
    }

    /**
     * @return The fields of the Program in the ContentValues format to be easily inserted into the
     * TV Input Framework database.
     */
    @Override
    public ContentValues toContentValues() {
        return toContentValues(false);
    }

    /**
     * Returns fields of the WatchNextProgram in the ContentValues format to be easily inserted
     * into the TV Input Framework database.
     *
     * @param includeProtectedFields Whether the fields protected by system is included or not.
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Override
    public ContentValues toContentValues(boolean includeProtectedFields) {
        ContentValues values = super.toContentValues(includeProtectedFields);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            values.remove(WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE);
            values.remove(WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS);
        }
        return values;
    }

    /**
     * Creates a WatchNextProgram object from a cursor including the fields defined in
     * {@link WatchNextPrograms}.
     *
     * @param cursor A row from the TV Input Framework database.
     * @return A Program with the values taken from the cursor.
     */
    public static WatchNextProgram fromCursor(Cursor cursor) {
        // TODO: Add additional API which does not use costly getColumnIndex().
        Builder builder = new Builder();
        BasePreviewProgram.setFieldsFromCursor(cursor, builder);
        int index;
        if ((index = cursor.getColumnIndex(WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE)) >= 0
                && !cursor.isNull(index)) {
            builder.setWatchNextType(cursor.getInt(index));
        }
        if ((index = cursor.getColumnIndex(
                WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS)) >= 0
                && !cursor.isNull(index)) {
            builder.setLastEngagementTimeUtcMillis(cursor.getLong(index));
        }
        return builder.build();
    }

    private static String[] getProjection() {
        String[] oColumns = new String[]{
                WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE,
                WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS,
        };
        return CollectionUtils.concatAll(BasePreviewProgram.PROJECTION, oColumns);
    }

    /**
     * This Builder class simplifies the creation of a {@link WatchNextProgram} object.
     */
    @SuppressWarnings("HiddenSuperclass")
    public static final class Builder extends BasePreviewProgram.Builder<Builder> {

        /**
         * Creates a new Builder object.
         */
        public Builder() {
        }

        /**
         * Creates a new Builder object with values copied from another Program.
         *
         * @param other The Program you're copying from.
         */
        public Builder(WatchNextProgram other) {
            mValues = new ContentValues(other.mValues);
        }

        /**
         * Sets the "watch next" type of this program content.
         *
         * <p>The value should match one of the followings:
         * {@link WatchNextPrograms#WATCH_NEXT_TYPE_CONTINUE},
         * {@link WatchNextPrograms#WATCH_NEXT_TYPE_NEXT}, and
         * {@link WatchNextPrograms#WATCH_NEXT_TYPE_NEW}.
         *
         * @param watchNextType The value of {@link WatchNextPrograms#COLUMN_WATCH_NEXT_TYPE} for
         *                      the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setWatchNextType(@WatchNextType int watchNextType) {
            mValues.put(WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE, watchNextType);
            return this;
        }

        /**
         * Sets the time when the program is going to begin in milliseconds since the epoch.
         *
         * @param lastEngagementTimeUtcMillis The value of
         *      {@link WatchNextPrograms#COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS}
         *      for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setLastEngagementTimeUtcMillis(long lastEngagementTimeUtcMillis) {
            mValues.put(WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS,
                    lastEngagementTimeUtcMillis);
            return this;
        }

        /**
         * @return A new Program with values supplied by the Builder.
         */
        public WatchNextProgram build() {
            return new WatchNextProgram(this);
        }
    }
}
