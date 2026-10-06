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

import androidx.core.location.altitude.impl.proto.MapParamsProto;
import androidx.core.location.altitude.impl.protobuf.InvalidProtocolBufferException;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;

import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Defines the entity type and its converters within the MapParams table. */
@AutoValue
@Entity(tableName = "MapParams")
public abstract class MapParamsEntity {

    private static final String TAG = "MapParamsEntity";

    @Contract("_, _ -> new")
    static @NonNull MapParamsEntity create(int id, MapParamsProto value) {
        return new AutoValue_MapParamsEntity(id, value);
    }

    /** Encodes a {@link MapParamsProto} */
    @TypeConverter
    public static byte @NonNull [] fromValue(@NonNull MapParamsProto value) {
        return value.toByteArray();
    }

    /** Decodes a {@link MapParamsProto} */
    @TypeConverter
    public static @Nullable MapParamsProto toValue(byte @NonNull [] byteArray) {
        try {
            return MapParamsProto.parseFrom(byteArray);
        } catch (InvalidProtocolBufferException e) {
            Log.e(TAG, "Unable to parse map params.");
            return null;
        }
    }

    @CopyAnnotations
    @PrimaryKey
    @ColumnInfo(name = "id")
    abstract int id();

    /**
     * Returns parameters for a spherically projected geoid map and corresponding tile management.
     */
    @CopyAnnotations
    @ColumnInfo(name = "value")
    public abstract @NonNull MapParamsProto value();
}
