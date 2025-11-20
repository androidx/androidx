/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.tiles;

import android.os.Bundle;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Holder for Tiles' Tile class, to be parceled and transferred to Wear.
 *
 * <ul>
 *   <li>Version 1: encodes the version (int) and the Tile as a proto contents (byte[]).
 *   <li>Version 2: encodes the version (int), the Tile as a proto contents (byte[]) and a {@link
 *       Bundle}.
 * </ul>
 *
 * <p>IMPORTANT: Only use Version 2 if the reader of this Parcelable can also handle V2.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class TileData extends ProtoParcelable {
    /** Version 1: encodes the version (int) and the Tile as a proto contents (byte[]). */
    public static final int VERSION_PROTOBUF_1 = 1;

    /**
     * Version 2: encodes the version (int), the Tile as a proto contents (byte[]) and a {@link
     * Bundle}.
     */
    public static final int VERSION_PROTOBUF_2 = 2;

    /** The key for retrieving {@code PendingIntent} bundle from the extras of the tile data. */
    public static final String PENDING_INTENT_KEY = "pending_intents";

    public static final Creator<TileData> CREATOR =
            newCreator(
                    TileData.class,
                    (bytes, extras, version) -> new TileData(bytes, extras, version));

    public TileData(byte @NonNull [] tile, int version) {
        this(tile, /* extras= */ null, version);
    }

    public TileData(byte @NonNull [] tile, @Nullable Bundle extras, int version) {
        super(tile, extras, version);
    }
}
