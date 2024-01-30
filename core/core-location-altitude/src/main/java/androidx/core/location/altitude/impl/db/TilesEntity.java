/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.core.location.altitude.impl.db;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.location.altitude.impl.proto.InvalidProtocolBufferException;
import androidx.core.location.altitude.impl.proto.S2TileProto;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;

import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

/** Defines the entity type and its converters within the Tiles table. */
@AutoValue
@Entity(tableName = "Tiles")
public abstract class TilesEntity {

    private static final String TAG = "MapParamsEntity";

    static TilesEntity create(String token, S2TileProto tile) {
        return new AutoValue_TilesEntity(token, tile);
    }

    /** Encodes a {@link S2TileProto}. */
    @NonNull
    @TypeConverter
    public static byte[] fromTile(@NonNull S2TileProto tile) {
        return tile.toByteArray();
    }

    /** Decodes a {@link S2TileProto}. */
    @Nullable
    @TypeConverter
    public static S2TileProto toTile(@NonNull byte[] byteArray) {
        try {
            return S2TileProto.parseFrom(byteArray);
        } catch (InvalidProtocolBufferException e) {
            Log.e(TAG, "Unable to parse tile.");
            return null;
        }
    }

    /** Returns an identifier for a tile within an S2 cell ID to unit interval map. */
    @CopyAnnotations
    @PrimaryKey
    @ColumnInfo(name = "token")
    @NonNull
    public abstract String token();

    /** Returns a tile within an S2 cell ID to unit interval map. */
    @CopyAnnotations
    @ColumnInfo(name = "tile")
    @NonNull
    public abstract S2TileProto tile();
}
