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
package android.support.media.tv;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.media.tv.TvContractCompat.Programs;

import java.util.Arrays;
import java.util.Objects;

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
 */
@TargetApi(21)
public final class Program extends BaseProgram implements Comparable<Program> {
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final String[] PROJECTION = getProjection();

    private static final long INVALID_LONG_VALUE = -1;
    private static final int INVALID_INT_VALUE = -1;
    private static final int IS_RECORDING_PROHIBITED = 1;

    private final long mChannelId;
    private final long mStartTimeUtcMillis;
    private final long mEndTimeUtcMillis;
    private final String[] mBroadcastGenres;
    private final int mRecordingProhibited;

    private Program(Builder builder) {
        super(builder);
        mChannelId = builder.mChannelId;
        mStartTimeUtcMillis = builder.mStartTimeUtcMillis;
        mEndTimeUtcMillis = builder.mEndTimeUtcMillis;
        mBroadcastGenres = builder.mBroadcastGenres;
        mRecordingProhibited = builder.mRecordingProhibited;
    }

    /**
     * @return The value of {@link Programs#COLUMN_CHANNEL_ID} for the program.
     */
    public long getChannelId() {
        return mChannelId;
    }

    /**
     * @return The value of {@link Programs#COLUMN_START_TIME_UTC_MILLIS} for the program.
     */
    public long getStartTimeUtcMillis() {
        return mStartTimeUtcMillis;
    }

    /**
     * @return The value of {@link Programs#COLUMN_END_TIME_UTC_MILLIS} for the program.
     */
    public long getEndTimeUtcMillis() {
        return mEndTimeUtcMillis;
    }

    /**
     * @return The value of {@link Programs#COLUMN_BROADCAST_GENRE} for the program.
     */
    public String[] getBroadcastGenres() {
        return mBroadcastGenres;
    }

    /**
     * @return The value of {@link Programs#COLUMN_RECORDING_PROHIBITED} for the program.
     */
    public boolean isRecordingProhibited() {
        return mRecordingProhibited == IS_RECORDING_PROHIBITED;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mChannelId, mStartTimeUtcMillis, mEndTimeUtcMillis);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Program)) {
            return false;
        }
        if (!super.equals(other)) {
            return false;
        }
        Program program = (Program) other;
        return mChannelId == program.mChannelId
                && mStartTimeUtcMillis == program.mStartTimeUtcMillis
                && mEndTimeUtcMillis == program.mEndTimeUtcMillis
                && Arrays.equals(mBroadcastGenres, program.mBroadcastGenres)
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.N
                        || Objects.equals(mRecordingProhibited, program.mRecordingProhibited));
    }

    /**
     * @param other The program you're comparing to.
     * @return The chronological order of the programs.
     */
    @Override
    public int compareTo(@NonNull Program other) {
        return Long.compare(mStartTimeUtcMillis, other.mStartTimeUtcMillis);
    }

    @Override
    public String toString() {
        return "Program{"
                + "channelId=" + mChannelId
                + ", startTimeUtcSec=" + mStartTimeUtcMillis
                + ", endTimeUtcSec=" + mEndTimeUtcMillis
                + "}";
    }

    /**
     * @return The fields of the Program in the ContentValues format to be easily inserted into the
     * TV Input Framework database.
     */
    @Override
    public ContentValues toContentValues() {
        ContentValues values = super.toContentValues();
        if (mChannelId != INVALID_LONG_VALUE) {
            values.put(Programs.COLUMN_CHANNEL_ID, mChannelId);
        } else {
            values.putNull(Programs.COLUMN_CHANNEL_ID);
        }
        if (mBroadcastGenres != null && mBroadcastGenres.length > 0) {
            values.put(Programs.COLUMN_BROADCAST_GENRE,
                    Programs.Genres.encode(mBroadcastGenres));
        } else {
            values.putNull(Programs.COLUMN_BROADCAST_GENRE);
        }
        if (mStartTimeUtcMillis != INVALID_LONG_VALUE) {
            values.put(Programs.COLUMN_START_TIME_UTC_MILLIS, mStartTimeUtcMillis);
        } else {
            values.putNull(Programs.COLUMN_START_TIME_UTC_MILLIS);
        }
        if (mEndTimeUtcMillis != INVALID_LONG_VALUE) {
            values.put(Programs.COLUMN_END_TIME_UTC_MILLIS, mEndTimeUtcMillis);
        } else {
            values.putNull(Programs.COLUMN_END_TIME_UTC_MILLIS);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (mRecordingProhibited != INVALID_INT_VALUE) {
                values.put(Programs.COLUMN_RECORDING_PROHIBITED, mRecordingProhibited);
            }
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
        private long mChannelId = INVALID_LONG_VALUE;
        private long mStartTimeUtcMillis = INVALID_LONG_VALUE;
        private long mEndTimeUtcMillis = INVALID_LONG_VALUE;
        private String[] mBroadcastGenres;
        private int mRecordingProhibited = INVALID_INT_VALUE;

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
            super(other);
            mChannelId = other.mChannelId;
            mStartTimeUtcMillis = other.mStartTimeUtcMillis;
            mEndTimeUtcMillis = other.mEndTimeUtcMillis;
            mBroadcastGenres = other.mBroadcastGenres;
            mRecordingProhibited = other.mRecordingProhibited;
        }

        /**
         * Sets the ID of the {@link Channel} that contains this program.
         *
         * @param channelId The value of {@link Programs#COLUMN_CHANNEL_ID for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setChannelId(long channelId) {
            mChannelId = channelId;
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
            mStartTimeUtcMillis = startTimeUtcMillis;
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
            mEndTimeUtcMillis = endTimeUtcMillis;
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
        public Builder setBroadcastGenres(String[] genres) {
            mBroadcastGenres = genres;
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
            mRecordingProhibited = prohibited ? IS_RECORDING_PROHIBITED : 0;
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
