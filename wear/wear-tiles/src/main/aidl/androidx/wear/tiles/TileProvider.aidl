/*
 * Copyright 2021 The Android Open Source Project
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

import android.widget.RemoteViews;
import androidx.wear.tiles.TileCallback;
import androidx.wear.tiles.TileAddEventData;
import androidx.wear.tiles.TileEnterEventData;
import androidx.wear.tiles.TileLeaveEventData;
import androidx.wear.tiles.TileRequestData;
import androidx.wear.tiles.TileRemoveEventData;
import androidx.wear.tiles.ResourcesCallback;
import androidx.wear.tiles.ResourcesRequestData;

/**
  * Interface to be implemented by a service which provides Tiles to a Wear
  * device.
  *
  * @hide
  */
interface TileProvider {

    const int API_VERSION = 1;

    /**
      * Gets the version of this TileProvider interface implemented by this
      * service. Note that this does not imply any schema version; just which
      * calls within this interface are supported.
      *
      * @since version 1
      */
    int getApiVersion() = 0;

    /**
      * Called when the system requests a new Tile timeline from this tile
      * provider. The tile provider should optionally call host.updateTileData
      * with the supplied ID, and a Tile timeline.
      *
      * @since version 1
      */
    oneway void onTileRequest(int id, in TileRequestData requestData, TileCallback callback) = 1;

    /**
      * Called when the system requires a new resource bundle, typically after
      * providing a tile with a different resource version. The tile provider
      * must call host.updateResourceData with the supplied ID, and a resource
      * bundle.
      *
      * @since version 1
      */
    oneway void onResourcesRequest(int id, in ResourcesRequestData requestData, ResourcesCallback callback) = 2;

    /**
     * Called when the Tile is added to the carousel. This will be followed by
     * a call to onTileRequest when the system is ready to render the tile.
     *
     * @since version 1
     */
    oneway void onTileAddEvent(in TileAddEventData requestData) = 5;

    /**
     * Called when the Tile is removed from the carousel.
     *
     * @since version 1
     */
    oneway void onTileRemoveEvent(in TileRemoveEventData requestData) = 6;

    /**
     * Called when the Tile is entered (i.e. the user browses to it).
     *
     * @since version 1
     */
    oneway void onTileEnterEvent(in TileEnterEventData requestData) = 7;

    /**
     * Called when the Tile is left (i.e. the user browses away from it).
     *
     * @since version 1
     */
    oneway void onTileLeaveEvent(in TileLeaveEventData requestData) = 8;
}
