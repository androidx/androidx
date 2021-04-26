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

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Tile update requester which uses multiple underlying update requesters.
 *
 * <p>This can be used to dispatch a tile update request to both the viewer classes and to SysUI at
 * the same time.
 */
class CompositeTileUpdateRequester implements TileUpdateRequester {
    private final List<TileUpdateRequester> mUpdateRequesters;

    CompositeTileUpdateRequester(List<TileUpdateRequester> requesters) {
        this.mUpdateRequesters = requesters;
    }

    @Override
    public void requestUpdate(@NonNull Class<? extends TileProviderService> tileProvider) {
        for (TileUpdateRequester requester : mUpdateRequesters) {
            requester.requestUpdate(tileProvider);
        }
    }
}
