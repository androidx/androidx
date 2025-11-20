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

import org.jspecify.annotations.NonNull;

/**
 * Interface used for a Tile Service to notify a Tile Renderer that it should fetch a new Timeline
 * from it.
 */
public interface TileUpdateRequester {
    /** Notify the Tile Renderer that it should fetch a new Timeline from this Tile Service. */
    void requestUpdate(@NonNull Class<? extends TileService> tileService);

    /**
     * Notify the Tile Renderer that it should fetch a new Timeline from this Tile Service for a
     * specific tile id.
     *
     * <p>If sdk version is API 36 or lower, or the tile id is invalid (i.e. doesn't exist or is not
     * owned by your package); tile id will be ignored and this method will be equivalent to {@link
     * #requestUpdate(Class)}.
     *
     * @param tileService The Tile Service to request an update from.
     * @param tileId The id of the tile to request an update from.
     */
    default void requestUpdate(@NonNull Class<? extends TileService> tileService, int tileId) {
        requestUpdate(tileService);
    }
}
