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

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

/**
 * {@link TileUpdateRequester} which notifies the viewer that it should fetch a new version of the
 * Timeline.
 */
class ViewerTileUpdateRequester implements TileUpdateRequester {
    /**
     * The intent action used so a Tile Provider can request that the platform fetches a new
     * Timeline from it.
     */
    public static final String ACTION_REQUEST_TILE_UPDATE =
            "androidx.wear.tiles.action.REQUEST_TILE_UPDATE";

    private final Context mContext;

    public ViewerTileUpdateRequester(@NonNull Context context) {
        this.mContext = context;
    }

    @Override
    public void requestUpdate(@NonNull Class<? extends TileProviderService> tileProvider) {
        mContext.sendBroadcast(buildUpdateIntent(mContext.getPackageName()));
    }

    private static Intent buildUpdateIntent(String packageName) {
        Intent i = new Intent(ACTION_REQUEST_TILE_UPDATE);
        i.setPackage(packageName);

        return i;
    }
}
