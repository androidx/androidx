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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.tvprovider.media.tv.TvContractCompat.Programs;
import androidx.tvprovider.media.tv.TvContractCompat.Programs.Genres.Genre;

/**
 * A convenience class to access {@link TvContractCompat.Programs} entries in the system content
 * provider.
 *
 * <p>This class makes it easy to insert or retrieve a program from the system content provider,
 * which is defined in {@link TvContractCompat}.
 *
 * <p>Usage example when inserting a program:
 * <pre>
 * Program program = new Program.Builder()
 *         .setChannelId(channel.getId())
 *         .setTitle("Program Title")
 *         .setDescription("Program Description")
 *         .setPosterArtUri(Uri.parse("http://example.com/poster_art.png"))
 *         // Set more attributes...
 *         .build();
 * Uri programUri = getContentResolver().insert(Programs.CONTENT_URI, program.toContentValues());
 * </pre>
 *
 * <p>Usage example when retrieving a program:
 * <pre>
 * Program program;
 * try (Cursor cursor = resolver.query(programUri, null, null, null, null)) {
 *     if (cursor != null && cursor.getCount() != 0) {
 *         cursor.moveToNext();
 *         program = Program.fromCursor(cursor);
 *     }
 * }
 * </pre>
 *
 * <p>Usage example when updating an existing program:
 * <pre>
 * Program updatedProgram = new Program.Builder(program)
 *         .setEndTimeUtcMillis(newProgramEndTime)
 *         .build();
 * getContentResolver().update(TvContractCompat.buildProgramUri(updatedProgram.getId()),
 *         updatedProgram.toContentValues(), null, null);
 * </pre>
 *
 * <p>Usage example when deleting a program:
 * <pre>
 * getContentResolver().delete(TvContractCompat.buildProgramUri(existingProgram.getId()),
 *         null, null);
 * </pre>
 */
public final class Program extends BaseProgram implements Comparable<Program> {
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final String[] PROJECTION = getProjection();

    private static final long INVALID_LONG_VALUE = -1;
    private static final int IS_RECORDING_PROHIBITED = 1;

    private Program(Builder builder) {
        super(builder);
    }

    /**
     * @return The value of {@link Programs#COLUMN_CHANNEL_ID} for the program.
     */
    public long getChannelId() {
        Long l = mValues.getAsLong(Programs.COLUMN_CHANNEL_ID);
        return l == null ? INVALID_LONG_VALUE : l;
    }

    /**
     * @return The value of {@link Programs#COLUMN_START_TIME_UTC_MILLIS} for the program.
     */
    public long getStartTimeUtcMillis() {
        Long l = mValues.getAsLong(Programs.COLUMN_START_TIME_UTC_MILLIS);
        return l == null ? INVALID_LONG_VALUE : l;
    }

    /**
     * @return The value of {@link Programs#COLUMN_END_TIME_UTC_MILLIS} for the program.
     */
    public long getEndTimeUtcMillis() {
        Long l = mValues.getAsLong(Programs.COLUMN_END_TIME_UTC_MILLIS);
        return l == null ? INVALID_LONG_VALUE : l;
    }

    /**
     * @return The value of {@link Programs#COLUMN_BROADCAST_GENRE} for the program.
     */
    public String[] getBroadcastGenres() {
        return Programs.Genres.decode(mValues.getAsString(Programs.COLUMN_BROADCAST_GENRE));
    }

    /**
     * @return The value of {@link Programs#COLUMN_RECORDING_PROHIBITED} for the program.
     */
    public boolean isRecordingProhibited() {
        Integer i = mValues.getAsInteger(Programs.COLUMN_RECORDING_PROHIBITED);
        return i != null && i == IS_RECORDING_PROHIBITED;
    }

    @Override
    public int hashCode() {
        return mValues.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Program)) {
            return false;
        }
        return mValues.equals(((Program) other).mValues);
    }

    /**
     * @param other The program you're comparing to.
     * @return The chronological order of the programs.
     */
    @Override
    public int compareTo(@NonNull Program other) {
        return Long.compare(mValues.getAsLong(Programs.COLUMN_START_TIME_UTC_MILLIS),
                other.mValues.getAsLong(Programs.COLUMN_START_TIME_UTC_MILLIS));
    }

    @Override
    public String toString() {
        return "Program{" + mValues.toString() + "}";
    }

    /**
     * @return The fields of the Program in the ContentValues format to be easily inserted into the
     * TV Input Framework database.
     */
    @Override
    public ContentValues toContentValues() {
        ContentValues values = super.toContentValues();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            values.remove(Programs.COLUMN_RECORDING_PROHIBITED);
        }
        return values;
    }

    /**
     * Creates a Program object from a cursor including the fields defined in {@link Programs}.
     *
     * @param cursor A row from the TV Input Framework database.
     * @return A Program with the values taken from the cursor.
     */
    public static Program fromCursor(Cursor cursor) {
        // TODO: Add additional API which does not use costly getColumnIndex().
        Builder builder = new Builder();
        BaseProgram.setFieldsFromCursor(cursor, builder);
        int index;
        if ((index = cursor.getColumnIndex(Programs.COLUMN_CHANNEL_ID)) >= 0
                && !cursor.isNull(index)) {
            builder.setChannelId(cursor.getLong(index));
        }
        if ((index = cursor.getColumnIndex(Programs.COLUMN_BROADCAST_GENRE)) >= 0
                && !cursor.isNull(index)) {
            builder.setBroadcastGenres(Programs.Genres.decode(
                    cursor.getString(index)));
        }
        if ((index = cursor.getColumnIndex(Programs.COLUMN_START_TIME_UTC_MILLIS)) >= 0
                && !cursor.isNull(index)) {
            builder.setStartTimeUtcMillis(cursor.getLong(index));
        }
        if ((index = cursor.getColumnIndex(Programs.COLUMN_END_TIME_UTC_MILLIS)) >= 0
                && !cursor.isNull(index)) {
            builder.setEndTimeUtcMillis(cursor.getLong(index));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if ((index = cursor.getColumnIndex(Programs.COLUMN_RECORDING_PROHIBITED)) >= 0
                    && !cursor.isNull(index)) {
                builder.setRecordingProhibited(cursor.getInt(index) == IS_RECORDING_PROHIBITED);
            }
        }
        return builder.build();
    }

    private static String[] getProjection() {
        String[] baseColumns = new String[] {
                Programs.COLUMN_CHANNEL_ID,
                Programs.COLUMN_BROADCAST_GENRE,
                Programs.COLUMN_START_TIME_UTC_MILLIS,
                Programs.COLUMN_END_TIME_UTC_MILLIS,
        };
        String[] nougatColumns = new String[] {
                Programs.COLUMN_RECORDING_PROHIBITED
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return CollectionUtils.concatAll(BaseProgram.PROJECTION, baseColumns, nougatColumns);
        } else {
            return CollectionUtils.concatAll(BaseProgram.PROJECTION, baseColumns);
        }
    }

    /**
     * This Builder class simplifies the creation of a {@link Program} object.
     */
    public static class Builder extends BaseProgram.Builder<Builder> {

        /**
         * Creates a new Builder object.
         */
        public Builder() {
        }

        /**
         * Creates a new Builder object with values copied from another Program.
         * @param other The Program you're copying from.
         */
        public Builder(Program other) {
            mValues = new ContentValues(other.mValues);
        }

        /**
         * Sets the ID of the {@link Channel} that contains this program.
         *
         * @param channelId The value of {@link Programs#COLUMN_CHANNEL_ID for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setChannelId(long channelId) {
            mValues.put(Programs.COLUMN_CHANNEL_ID, channelId);
            return this;
        }

        /**
         * Sets the time when the program is going to begin in milliseconds since the epoch.
         *
         * @param startTimeUtcMillis The value of {@link Programs#COLUMN_START_TIME_UTC_MILLIS} for
         *                           the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setStartTimeUtcMillis(long startTimeUtcMillis) {
            mValues.put(Programs.COLUMN_START_TIME_UTC_MILLIS, startTimeUtcMillis);
            return this;
        }

        /**
         * Sets the time when this program is going to end in milliseconds since the epoch.
         *
         * @param endTimeUtcMillis The value of {@link Programs#COLUMN_END_TIME_UTC_MILLIS} for the
         *                         program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setEndTimeUtcMillis(long endTimeUtcMillis) {
            mValues.put(Programs.COLUMN_END_TIME_UTC_MILLIS, endTimeUtcMillis);
            return this;
        }

        /**
         * Sets the broadcast-specified genres of the program.
         *
         * @param genres Array of genres that apply to the program based on the broadcast standard
         *               which will be flattened to a String to store in a database.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see Programs#COLUMN_BROADCAST_GENRE
         */
        public Builder setBroadcastGenres(@Genre String[] genres) {
            mValues.put(Programs.COLUMN_BROADCAST_GENRE, Programs.Genres.encode(genres));
            return this;
        }

        /**
         * Sets whether this program cannot be recorded.
         *
         * @param prohibited The value of {@link Programs#COLUMN_RECORDING_PROHIBITED} for the
         *                   program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setRecordingProhibited(boolean prohibited) {
            mValues.put(Programs.COLUMN_RECORDING_PROHIBITED,
                    prohibited ? IS_RECORDING_PROHIBITED : 0);
            return this;
        }

        /**
         * @return A new Program with values supplied by the Builder.
         */
        public Program build() {
            return new Program(this);
        }
    }
}
