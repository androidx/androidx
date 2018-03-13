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

import androidx.annotation.RestrictTo;
import androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms;

import java.util.Objects;
import java.util.Set;

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
 *
 * <p>Usage example when updating an existing preview program:
 * <pre>
 * PreviewProgram updatedProgram = new PreviewProgram.Builder(previewProgram)
 *         .setWeight(20)
 *         .build();
 * getContentResolver().update(TvContractCompat.buildPreviewProgramUri(updatedProgram.getId()),
 *         updatedProgram.toContentValues(), null, null);
 * </pre>
 *
 * <p>Usage example when deleting a preview program:
 * <pre>
 * getContentResolver().delete(TvContractCompat.buildPreviewProgramUri(existingProgram.getId()),
 *         null, null);
 * </pre>
 */
public final class PreviewProgram extends BasePreviewProgram {
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final String[] PROJECTION = getProjection();

    private static final long INVALID_LONG_VALUE = -1;
    private static final int INVALID_INT_VALUE = -1;

    private PreviewProgram(Builder builder) {
        super(builder);
    }

    /**
     * @return The value of {@link PreviewPrograms#COLUMN_CHANNEL_ID} for the program.
     */
    public long getChannelId() {
        Long l = mValues.getAsLong(PreviewPrograms.COLUMN_CHANNEL_ID);
        return l == null ? INVALID_LONG_VALUE : l;
    }

    /**
     * @return The value of {@link PreviewPrograms#COLUMN_WEIGHT} for the program.
     */
    public int getWeight() {
        Integer i = mValues.getAsInteger(PreviewPrograms.COLUMN_WEIGHT);
        return i == null ? INVALID_INT_VALUE : i;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PreviewProgram)) {
            return false;
        }
        return mValues.equals(((PreviewProgram) other).mValues);
    }

    /**
     * Indicates whether some other PreviewProgram has any set attribute that is different from
     * this PreviewProgram's respective attributes. An attribute is considered "set" if its key
     * is present in the ContentValues vector.
     */
    public boolean hasAnyUpdatedValues(PreviewProgram update) {
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
        return "PreviewProgram{" + mValues.toString() + "}";
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
     * Returns fields of the PreviewProgram in the ContentValues format to be easily inserted
     * into the TV Input Framework database.
     *
     * @param includeProtectedFields Whether the fields protected by system is included or not.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public ContentValues toContentValues(boolean includeProtectedFields) {
        ContentValues values = super.toContentValues(includeProtectedFields);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            values.remove(PreviewPrograms.COLUMN_CHANNEL_ID);
            values.remove(PreviewPrograms.COLUMN_WEIGHT);
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
        String[] oColumns = new String[]{
                PreviewPrograms.COLUMN_CHANNEL_ID,
                PreviewPrograms.COLUMN_WEIGHT,
        };
        return CollectionUtils.concatAll(BasePreviewProgram.PROJECTION, oColumns);
    }

    /**
     * This Builder class simplifies the creation of a {@link PreviewProgram} object.
     */
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
        public Builder(PreviewProgram other) {
            mValues = new ContentValues(other.mValues);
        }

        /**
         * Sets the ID of the {@link Channel} that contains this program.
         *
         * @param channelId The value of {@link PreviewPrograms#COLUMN_CHANNEL_ID for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setChannelId(long channelId) {
            mValues.put(PreviewPrograms.COLUMN_CHANNEL_ID, channelId);
            return this;
        }

        /**
         * Sets the weight of the preview program within the channel.
         *
         * @param weight The value of {@link PreviewPrograms#COLUMN_WEIGHT} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setWeight(int weight) {
            mValues.put(PreviewPrograms.COLUMN_WEIGHT, weight);
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
