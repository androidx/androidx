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
import android.support.annotation.RestrictTo;
import android.support.media.tv.TvContractCompat.PreviewPrograms;

import java.util.Objects;

/**
 * A convenience class to access {@link PreviewPrograms} entries in the system content
 * provider.
 *
 * <p>This class makes it easy to insert or retrieve a preview program from the system content
 * provider, which is defined in {@link TvContractCompat}.
 *
 * <p>Usage example when inserting a preview program:
 * <pre>
 * PreviewProgram previewProgram = new PreviewProgram.Builder()
 *         .setChannelId(channel.getId())
 *         .setType(PreviewPrograms.TYPE_MOVIE)
 *         .setTitle("Program Title")
 *         .setDescription("Program Description")
 *         .setPosterArtUri(Uri.parse("http://example.com/poster_art.png"))
 *         // Set more attributes...
 *         .build();
 * Uri previewProgramUri = getContentResolver().insert(PreviewPrograms.CONTENT_URI,
 *         previewProgram.toContentValues());
 * </pre>
 *
 * <p>Usage example when retrieving a preview program:
 * <pre>
 * PreviewProgram previewProgram;
 * try (Cursor cursor = resolver.query(previewProgramUri, null, null, null, null)) {
 *     if (cursor != null && cursor.getCount() != 0) {
 *         cursor.moveToNext();
 *         previewProgram = PreviewProgram.fromCursor(cursor);
 *     }
 * }
 * </pre>
 */
@TargetApi(26)
public final class PreviewProgram extends BasePreviewProgram {
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final String[] PROJECTION = getProjection();

    private static final long INVALID_LONG_VALUE = -1;
    private static final int INVALID_INT_VALUE = -1;

    private final long mChannelId;
    private final int mWeight;

    private PreviewProgram(Builder builder) {
        super(builder);
        mChannelId = builder.mChannelId;
        mWeight = builder.mWeight;
    }

    /**
     * @return The value of {@link PreviewPrograms#COLUMN_CHANNEL_ID} for the program.
     */
    public long getChannelId() {
        return mChannelId;
    }

    /**
     * @return The value of {@link PreviewPrograms#COLUMN_WEIGHT} for the program.
     */
    public int getWeight() {
        return mWeight;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PreviewProgram)) {
            return false;
        }
        if (!super.equals(other)) {
            return false;
        }
        PreviewProgram program = (PreviewProgram) other;
        return mChannelId == program.mChannelId && Objects.equals(mWeight, program.mWeight);
    }

    @Override
    public String toString() {
        return "Program{"
                + ", channelId=" + mChannelId
                + ", weight=" + mWeight
                + "}";
    }

    /**
     * @return The fields of the Program in the ContentValues format to be easily inserted into the
     * TV Input Framework database.
     */
    public ContentValues toContentValues() {
        ContentValues values = super.toContentValues();
        if (mChannelId != INVALID_LONG_VALUE) {
            values.put(PreviewPrograms.COLUMN_CHANNEL_ID, mChannelId);
        } else {
            values.putNull(PreviewPrograms.COLUMN_CHANNEL_ID);
        }
        if (mWeight != INVALID_INT_VALUE) {
            values.put(PreviewPrograms.COLUMN_WEIGHT, mWeight);
        }
        return values;
    }

    /**
     * Creates a Program object from a cursor including the fields defined in
     * {@link PreviewPrograms}.
     *
     * @param cursor A row from the TV Input Framework database.
     * @return A Program with the values taken from the cursor.
     */
    public static PreviewProgram fromCursor(Cursor cursor) {
        // TODO: Add additional API which does not use costly getColumnIndex().
        Builder builder = new Builder();
        BasePreviewProgram.setFieldsFromCursor(cursor, builder);
        int index;
        if ((index = cursor.getColumnIndex(PreviewPrograms.COLUMN_CHANNEL_ID)) >= 0
                && !cursor.isNull(index)) {
            builder.setChannelId(cursor.getLong(index));
        }
        if ((index = cursor.getColumnIndex(PreviewPrograms.COLUMN_WEIGHT)) >= 0
                && !cursor.isNull(index)) {
            builder.setWeight(cursor.getInt(index));
        }
        return builder.build();
    }

    private static String[] getProjection() {
        String[] oColumns = new String[] {
                PreviewPrograms.COLUMN_CHANNEL_ID,
                PreviewPrograms.COLUMN_WEIGHT,
        };
        return CollectionUtils.concatAll(BasePreviewProgram.PROJECTION, oColumns);
    }

    /**
     * This Builder class simplifies the creation of a {@link PreviewProgram} object.
     */
    public static final class Builder extends BasePreviewProgram.Builder<Builder> {
        private long mChannelId = INVALID_LONG_VALUE;
        private int mWeight = INVALID_INT_VALUE;

        /**
         * Creates a new Builder object.
         */
        public Builder() {
        }

        /**
         * Creates a new Builder object with values copied from another Program.
         * @param other The Program you're copying from.
         */
        public Builder(PreviewProgram other) {
            super(other);
            mChannelId = other.mChannelId;
            mWeight = other.mWeight;
        }

        /**
         * Sets the ID of the {@link Channel} that contains this program.
         *
         * @param channelId The value of {@link PreviewPrograms#COLUMN_CHANNEL_ID for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setChannelId(long channelId) {
            mChannelId = channelId;
            return this;
        }

        /**
         * Sets the weight of the preview program within the channel.
         *
         * @param weight The value of {@link PreviewPrograms#COLUMN_WEIGHT} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setWeight(int weight) {
            mWeight = weight;
            return this;
        }

        /**
         * @return A new Program with values supplied by the Builder.
         */
        public PreviewProgram build() {
            return new PreviewProgram(this);
        }
    }
}
