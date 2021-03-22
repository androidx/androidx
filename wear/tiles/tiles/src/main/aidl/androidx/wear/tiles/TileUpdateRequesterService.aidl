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

import android.content.ComponentName;
import androidx.wear.tiles.TileUpdateRequestData;

/**
  * Interface, implemented by Tile Renderers, which allows a Tile Provider to
  * request that a Tile Renderer fetches a new Timeline from it.
  *
  * @hide
  */
interface TileUpdateRequesterService {
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
      * Request that the Tile Renderer fetches a new Timeline from the Tile
      * Provider service identified by {@code component}. The package name in
      * {@code component} must have the same UID as the calling process,
      * otherwise this call will be ignored.
      *
      * {@code updateData} is provided as a placeholder to allow a payload of
      * parameters to be passed in future. This is currently blank in all
      * implementations, but allows for easy expansion.
      *
      * Note that this call may be rate limited, hence the tile fetch request
      * may not occur immediately after calling this method.
      *
      * @since version 1
      */
    oneway void requestUpdate(in ComponentName component, in TileUpdateRequestData updateData) = 1;
}
