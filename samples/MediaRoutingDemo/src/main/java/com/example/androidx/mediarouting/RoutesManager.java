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

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.mediarouter.media.MediaRouter;
import androidx.mediarouter.media.MediaRouterParams;
import androidx.mediarouter.media.RouteListingPreference;

import com.example.androidx.mediarouting.activities.MainActivity;
import com.example.androidx.mediarouting.data.RouteItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Holds the data needed to control the provider for the routes dynamically. */
public final class RoutesManager {

    private static final String VARIABLE_VOLUME_BASIC_ROUTE_ID = "variable_basic";
    private static final String SENDER_DRIVEN_BASIC_ROUTE_ID = "sender_driven_route";
    private static final int VOLUME_MAX = 25;
    private static final int VOLUME_DEFAULT = 5;

    private static RoutesManager sInstance;

    private final Context mContext;
    private final Map<String, RouteItem> mRouteItems;
    private boolean mDynamicRoutingEnabled;
    private DialogType mDialogType;
    private final MediaRouter mMediaRouter;
    private boolean mRouteListingPreferenceEnabled;
    private boolean mRouteListingSystemOrderingPreferred;
    private List<RouteListingPreferenceItemHolder> mRouteListingPreferenceItems;

    private RoutesManager(Context context) {
        mContext = context;
        mDynamicRoutingEnabled = true;
        mDialogType = DialogType.OUTPUT_SWITCHER;
        mRouteItems = new HashMap<>();
        mRouteListingPreferenceItems = Collections.emptyList();
        mMediaRouter = MediaRouter.getInstance(context);
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
     * Gets the route with the passed id, or null if no route with the given id exists.
     *
     * @param id of the route to search for.
     * @return the route with the passed id, or null if it does not exist.
     */
    @Nullable
    public RouteItem getRouteWithId(@Nullable String id) {
        return mRouteItems.get(id);
    }

    /** Adds the given route to the manager, replacing any existing route with a matching id. */
    public void addRoute(@NonNull RouteItem routeItem) {
        mRouteItems.put(routeItem.getId(), routeItem);
    }

    /**
     * Returns whether route listing preference is enabled.
     *
     * @see #setRouteListingPreferenceEnabled
     */
    public boolean isRouteListingPreferenceEnabled() {
        return mRouteListingPreferenceEnabled;
    }

    /**
     * Sets whether the use of route listing preference is enabled or not.
     *
     * <p>If route listing preference is enabled, the route listing preference configuration for
     * this app is maintained following the item list provided via {@link
     * #setRouteListingPreferenceItems}. Otherwise, if route listing preference is disabled, the
     * route listing preference for this app is set to null.
     *
     * <p>Does not affect the system's state if called on a device running API 33 or older.
     */
    public void setRouteListingPreferenceEnabled(boolean routeListingPreferenceEnabled) {
        mRouteListingPreferenceEnabled = routeListingPreferenceEnabled;
        onRouteListingPreferenceChanged();
    }

    /** Returns whether the system ordering for route listing is preferred. */
    public boolean getRouteListingSystemOrderingPreferred() {
        return mRouteListingSystemOrderingPreferred;
    }

    /**
     * Sets whether to prefer the system ordering for route listing.
     *
     * <p>True means that the ordering for route listing is the one in the {@link #getRouteItems()}
     * list. If false, the ordering of said list is ignored, and the system uses its builtin
     * ordering for the items.
     *
     * <p>Does not affect the system's state if called on a device running API 33 or older.
     */
    public void setRouteListingSystemOrderingPreferred(
            boolean routeListingSystemOrderringPreferred) {
            mRouteListingSystemOrderingPreferred = routeListingSystemOrderringPreferred;
        onRouteListingPreferenceChanged();
    }

    /**
     * The current list of route listing preference items, as set via {@link
     * #setRouteListingPreferenceItems}.
     */
    @NonNull
    public List<RouteListingPreferenceItemHolder> getRouteListingPreferenceItems() {
        return mRouteListingPreferenceItems;
    }

    /**
     * Sets the route listing preference items.
     *
     * <p>Does not affect the system's state if called on a device running API 33 or older.
     *
     * @see #setRouteListingPreferenceEnabled
     */
    public void setRouteListingPreferenceItems(
            @NonNull List<RouteListingPreferenceItemHolder> preference) {
            mRouteListingPreferenceItems =
                    Collections.unmodifiableList(new ArrayList<>(preference));
        onRouteListingPreferenceChanged();
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

        RouteItem r5 = new RouteItem();
        r5.setId(SENDER_DRIVEN_BASIC_ROUTE_ID + "1");
        r5.setName(r.getString(R.string.sender_driven_route_name1));
        r5.setDescription(r.getString(R.string.sample_route_description));
        r5.setControlFilter(BASIC);
        r5.setDeviceType(TV);
        r5.setPlaybackStream(MUSIC);
        r5.setPlaybackType(REMOTE);
        r5.setVolumeHandling(VARIABLE);
        r5.setVolumeMax(VOLUME_MAX);
        r5.setVolume(VOLUME_DEFAULT);
        r5.setCanDisconnect(true);
        r5.setSenderDriven(true);

        RouteItem r6 = new RouteItem();
        r6.setId(SENDER_DRIVEN_BASIC_ROUTE_ID + "2");
        r6.setName(r.getString(R.string.sender_driven_route_name2));
        r6.setDescription(r.getString(R.string.sample_route_description));
        r6.setControlFilter(BASIC);
        r6.setDeviceType(TV);
        r6.setPlaybackStream(MUSIC);
        r6.setPlaybackType(REMOTE);
        r6.setVolumeHandling(VARIABLE);
        r6.setVolumeMax(VOLUME_MAX);
        r6.setVolume(VOLUME_DEFAULT);
        r6.setCanDisconnect(true);
        r6.setSenderDriven(true);

        mRouteItems.put(r1.getId(), r1);
        mRouteItems.put(r2.getId(), r2);
        mRouteItems.put(r3.getId(), r3);
        mRouteItems.put(r4.getId(), r4);
        mRouteItems.put(r5.getId(), r5);
        mRouteItems.put(r6.getId(), r6);
    }

    private void onRouteListingPreferenceChanged() {
        RouteListingPreference routeListingPreference = null;
        if (mRouteListingPreferenceEnabled) {
            ArrayList<RouteListingPreference.Item> items = new ArrayList<>();
            for (RouteListingPreferenceItemHolder item : mRouteListingPreferenceItems) {
                items.add(item.mItem);
            }
            routeListingPreference =
                    new RouteListingPreference.Builder()
                            .setItems(items)
                            .setLinkedItemComponentName(
                                    new ComponentName(mContext, MainActivity.class))
                            .setSystemOrderingEnabled(mRouteListingSystemOrderingPreferred)
                            .build();
        }
        mMediaRouter.setRouteListingPreference(routeListingPreference);
    }

    public enum DialogType {
        DEFAULT,
        DYNAMIC_GROUP,
        OUTPUT_SWITCHER
    }

    /**
     * Holds a {@link RouteListingPreference.Item} and the associated route's name.
     *
     * <p>Convenient pair-like class for populating UI elements, ensuring we have an associated
     * route name for each route listing preference item even after the corresponding route no
     * longer exists.
     */
    public static final class RouteListingPreferenceItemHolder {

        @NonNull public final RouteListingPreference.Item mItem;
        @NonNull public final String mRouteName;

        public RouteListingPreferenceItemHolder(
                @NonNull RouteListingPreference.Item item, @NonNull String routeName) {
            mItem = item;
            mRouteName = routeName;
        }

        /** Returns the name of the corresponding route. */
        @Override
        @NonNull
        public String toString() {
            return mRouteName;
        }

        /**
         * Returns whether the contained {@link RouteListingPreference.Item} has the given {@code
         * flag} set.
         */
        public boolean hasFlag(int flag) {
            return (mItem.getFlags() & flag) == flag;
        }
    }
}
