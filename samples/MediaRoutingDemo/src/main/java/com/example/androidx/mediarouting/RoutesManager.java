/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.example.androidx.mediarouting;

import static com.example.androidx.mediarouting.data.RouteItem.ControlFilter.BASIC;
import static com.example.androidx.mediarouting.data.RouteItem.DeviceType.SPEAKER;
import static com.example.androidx.mediarouting.data.RouteItem.DeviceType.TV;
import static com.example.androidx.mediarouting.data.RouteItem.PlaybackStream.MUSIC;
import static com.example.androidx.mediarouting.data.RouteItem.PlaybackType.REMOTE;
import static com.example.androidx.mediarouting.data.RouteItem.VolumeHandling.VARIABLE;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.mediarouter.media.MediaRouter;
import androidx.mediarouter.media.MediaRouterParams;

import com.example.androidx.mediarouting.data.RouteItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Holds the data needed to control the provider for the routes dynamically. */
public final class RoutesManager {

    private static final String VARIABLE_VOLUME_BASIC_ROUTE_ID = "variable_basic";
    private static final int VOLUME_MAX = 25;
    private static final int VOLUME_DEFAULT = 5;

    private static RoutesManager sInstance;

    private final Context mContext;
    private final Map<String, RouteItem> mRouteItems;
    private boolean mDynamicRoutingEnabled;
    private DialogType mDialogType;

    private RoutesManager(Context context) {
        mContext = context;
        mDynamicRoutingEnabled = true;
        mDialogType = DialogType.OUTPUT_SWITCHER;
        mRouteItems = new HashMap<>();
        initTestRoutes();
    }

    /** Singleton method. */
    @NonNull
    public static RoutesManager getInstance(@NonNull Context context) {
        synchronized (RoutesManager.class) {
            if (sInstance == null) {
                sInstance = new RoutesManager(context);
            }
        }
        return sInstance;
    }

    @NonNull
    public List<RouteItem> getRouteItems() {
        return new ArrayList<>(mRouteItems.values());
    }

    public boolean isDynamicRoutingEnabled() {
        return mDynamicRoutingEnabled;
    }

    public void setDynamicRoutingEnabled(boolean dynamicRoutingEnabled) {
        this.mDynamicRoutingEnabled = dynamicRoutingEnabled;
    }

    public void setDialogType(@NonNull DialogType dialogType) {
        this.mDialogType = dialogType;
    }

    /**
     * Deletes the route with the passed id.
     *
     * @param id of the route to be deleted.
     */
    public void deleteRouteWithId(@NonNull String id) {
        mRouteItems.remove(id);
    }

    /**
     * Gets the route with the passed id or null if not exists.
     *
     * @param id of the route to search for.
     * @return the route with the passed id or null if not exists.
     */
    @Nullable
    public RouteItem getRouteWithId(@NonNull String id) {
        return mRouteItems.get(id);
    }

    /** Adds the given route to the manager, replacing any existing route with a matching id. */
    public void addRoute(@NonNull RouteItem routeItem) {
        mRouteItems.put(routeItem.getId(), routeItem);
    }

    /** Changes the media router dialog type with the type stored in {@link RoutesManager} */
    public void reloadDialogType() {
        MediaRouter mediaRouter = MediaRouter.getInstance(mContext.getApplicationContext());
        MediaRouterParams.Builder builder =
                new MediaRouterParams.Builder(mediaRouter.getRouterParams());
        switch (mDialogType) {
            case DEFAULT:
                builder.setDialogType(MediaRouterParams.DIALOG_TYPE_DEFAULT)
                        .setOutputSwitcherEnabled(false);
                mediaRouter.setRouterParams(builder.build());
                break;
            case DYNAMIC_GROUP:
                builder.setDialogType(MediaRouterParams.DIALOG_TYPE_DYNAMIC_GROUP)
                        .setOutputSwitcherEnabled(false);
                mediaRouter.setRouterParams(builder.build());
                break;
            case OUTPUT_SWITCHER:
                builder.setOutputSwitcherEnabled(true);
                mediaRouter.setRouterParams(builder.build());
                break;
        }
    }

    private void initTestRoutes() {
        Resources r = mContext.getResources();

        RouteItem r1 = new RouteItem();
        r1.setId(VARIABLE_VOLUME_BASIC_ROUTE_ID + "1");
        r1.setName(r.getString(R.string.dg_tv_route_name1));
        r1.setDescription(r.getString(R.string.sample_route_description));
        r1.setControlFilter(BASIC);
        r1.setDeviceType(TV);
        r1.setPlaybackStream(MUSIC);
        r1.setPlaybackType(REMOTE);
        r1.setVolumeHandling(VARIABLE);
        r1.setVolumeMax(VOLUME_MAX);
        r1.setVolume(VOLUME_DEFAULT);
        r1.setCanDisconnect(true);

        RouteItem r2 = new RouteItem();
        r2.setId(VARIABLE_VOLUME_BASIC_ROUTE_ID + "2");
        r2.setName(r.getString(R.string.dg_tv_route_name2));
        r2.setDescription(r.getString(R.string.sample_route_description));
        r2.setControlFilter(BASIC);
        r2.setDeviceType(TV);
        r2.setPlaybackStream(MUSIC);
        r2.setPlaybackType(REMOTE);
        r2.setVolumeHandling(VARIABLE);
        r2.setVolumeMax(VOLUME_MAX);
        r2.setVolume(VOLUME_DEFAULT);
        r2.setCanDisconnect(true);

        RouteItem r3 = new RouteItem();
        r3.setId(VARIABLE_VOLUME_BASIC_ROUTE_ID + "3");
        r3.setName(r.getString(R.string.dg_speaker_route_name3));
        r3.setDescription(r.getString(R.string.sample_route_description));
        r3.setControlFilter(BASIC);
        r3.setDeviceType(SPEAKER);
        r3.setPlaybackStream(MUSIC);
        r3.setPlaybackType(REMOTE);
        r3.setVolumeHandling(VARIABLE);
        r3.setVolumeMax(VOLUME_MAX);
        r3.setVolume(VOLUME_DEFAULT);
        r3.setCanDisconnect(true);

        RouteItem r4 = new RouteItem();
        r4.setId(VARIABLE_VOLUME_BASIC_ROUTE_ID + "4");
        r4.setName(r.getString(R.string.dg_speaker_route_name4));
        r4.setDescription(r.getString(R.string.sample_route_description));
        r4.setControlFilter(BASIC);
        r4.setDeviceType(SPEAKER);
        r4.setPlaybackStream(MUSIC);
        r4.setPlaybackType(REMOTE);
        r4.setVolumeHandling(VARIABLE);
        r4.setVolumeMax(VOLUME_MAX);
        r4.setVolume(VOLUME_DEFAULT);
        r4.setCanDisconnect(true);

        mRouteItems.put(r1.getId(), r1);
        mRouteItems.put(r2.getId(), r2);
        mRouteItems.put(r3.getId(), r3);
        mRouteItems.put(r4.getId(), r4);
    }

    public enum DialogType {
        DEFAULT,
        DYNAMIC_GROUP,
        OUTPUT_SWITCHER
    }
}
