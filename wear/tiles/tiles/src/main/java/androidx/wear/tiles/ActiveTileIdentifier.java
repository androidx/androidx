/*
 * Copyright 2023 The Android Open Source Project
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

import android.content.ComponentName;

import org.jspecify.annotations.NonNull;

/**
 * Tile information containing the tile instance ID and component name for identifying a tile
 * instance.
 */
public final class ActiveTileIdentifier {
    private final ComponentName mComponentName;
    private final int mInstanceId;

    public ActiveTileIdentifier(@NonNull ComponentName componentName, int instanceId) {
        this.mComponentName = componentName;
        this.mInstanceId = instanceId;
    }

    /** Component name of the tile provider. */
    public @NonNull ComponentName getComponentName() {
        return mComponentName;
    }

    /** Tile ID for identifying an active tile instance. */
    public int getInstanceId() {
        return mInstanceId;
    }

    /**
     * Return a String that unambiguously describes both the tile id and component name contained in
     * the {@link ActiveTileIdentifier}. You can later recover the {@link ActiveTileIdentifier} from
     * this string through {@code unflattenFromString(String)}.
     *
     * @return Returns a new String holding the tile id, package and class names. This is
     *     represented as the tileId, concatenated with a ':' and then the component name flattened
     *     to string.
     */
    @NonNull String flattenToString() {
        return mInstanceId + ":" + mComponentName.flattenToString();
    }

    /**
     * Recover an {@link ActiveTileIdentifier from} a String that was previously created with {@code
     * flattenToString()}. It splits the string at the first ':', taking the part before as the tile
     * id and the part after as component name flattened to string.
     *
     * @param string The String that was returned by {@code flattenToString()}.
     * @return Returns a new {@link ActiveTileIdentifier} containing the tile id and component name
     *     that were encoded in {@code string}.
     */
    static @NonNull ActiveTileIdentifier unflattenFromString(@NonNull String string) {
        int delimiterIndex = string.indexOf(":");
        return new ActiveTileIdentifier(
                ComponentName.unflattenFromString(string.substring(delimiterIndex + 1)),
                Integer.parseInt(string.substring(0, delimiterIndex)));
    }
}
