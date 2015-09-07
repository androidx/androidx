/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v7.app;

import static android.support.v7.media.MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTED;
import static android.support.v7.media.MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTING;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.mediarouter.R;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * This class implements the route chooser dialog for {@link MediaRouter}.
 * <p>
 * This dialog allows the user to choose a route that matches a given selector.
 * </p>
 *
 * @see MediaRouteButton
 * @see MediaRouteActionProvider
 */
public class MediaRouteChooserDialog extends Dialog {
    private static final String TAG = "MediaRouteChooserDialog";

    // Should match to SystemMediaRouteProvider.PACKAGE_NAME.
    static final String SYSTEM_MEDIA_ROUTE_PROVIDER_PACKAGE_NAME = "android";

    private final MediaRouter mRouter;
    private final MediaRouterCallback mCallback;

    private MediaRouteSelector mSelector = MediaRouteSelector.EMPTY;
    private ArrayList<MediaRouter.RouteInfo> mRoutes;
    private RouteAdapter mAdapter;
    private ListView mListView;
    private boolean mAttachedToWindow;

    public MediaRouteChooserDialog(Context context) {
        this(context, 0);
    }

    public MediaRouteChooserDialog(Context context, int theme) {
        super(MediaRouterThemeHelper.createThemedContext(context), theme);
        context = getContext();

        mRouter = MediaRouter.getInstance(context);
        mCallback = new MediaRouterCallback();
    }

    /**
     * Gets the media route selector for filtering the routes that the user can select.
     *
     * @return The selector, never null.
     */
    @NonNull
    public MediaRouteSelector getRouteSelector() {
        return mSelector;
    }

    /**
     * Sets the media route selector for filtering the routes that the user can select.
     *
     * @param selector The selector, must not be null.
     */
    public void setRouteSelector(@NonNull MediaRouteSelector selector) {
        if (selector == null) {
            throw new IllegalArgumentException("selector must not be null");
        }

        if (!mSelector.equals(selector)) {
            mSelector = selector;

            if (mAttachedToWindow) {
                mRouter.removeCallback(mCallback);
                mRouter.addCallback(selector, mCallback,
                        MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
            }

            refreshRoutes();
        }
    }

    /**
     * Called to filter the set of routes that should be included in the list.
     * <p>
     * The default implementation iterates over all routes in the provided list and
     * removes those for which {@link #onFilterRoute} returns false.
     * </p>
     *
     * @param routes The list of routes to filter in-place, never null.
     */
    public void onFilterRoutes(@NonNull List<MediaRouter.RouteInfo> routes) {
        for (int i = routes.size(); i-- > 0; ) {
            if (!onFilterRoute(routes.get(i))) {
                routes.remove(i);
            }
        }
    }

    /**
     * Returns true if the route should be included in the list.
     * <p>
     * The default implementation returns true for enabled non-default routes that
     * match the selector.  Subclasses can override this method to filter routes
     * differently.
     * </p>
     *
     * @param route The route to consider, never null.
     * @return True if the route should be included in the chooser dialog.
     */
    public boolean onFilterRoute(@NonNull MediaRouter.RouteInfo route) {
        return !route.isDefault() && route.isEnabled() && route.matchesSelector(mSelector);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.mr_chooser_dialog);
        setTitle(R.string.mr_chooser_title);

        getWindow().setLayout(MediaRouteDialogHelper.getDialogWidth(getContext()),
                ViewGroup.LayoutParams.WRAP_CONTENT);

        mRoutes = new ArrayList<>();
        mAdapter = new RouteAdapter(getContext(), mRoutes);
        mListView = (ListView)findViewById(R.id.mr_chooser_list);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(mAdapter);
        mListView.setEmptyView(findViewById(android.R.id.empty));
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        mAttachedToWindow = true;
        mRouter.addCallback(mSelector, mCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
        refreshRoutes();
    }

    @Override
    public void onDetachedFromWindow() {
        mAttachedToWindow = false;
        mRouter.removeCallback(mCallback);

        super.onDetachedFromWindow();
    }

    /**
     * Refreshes the list of routes that are shown in the chooser dialog.
     */
    public void refreshRoutes() {
        if (mAttachedToWindow) {
            mRoutes.clear();
            mRoutes.addAll(mRouter.getRoutes());
            onFilterRoutes(mRoutes);
            RouteComparator.loadRouteUsageScores(getContext(), mRoutes);
            Collections.sort(mRoutes, RouteComparator.sInstance);
            mAdapter.notifyDataSetChanged();
        }
    }

    private final class RouteAdapter extends ArrayAdapter<MediaRouter.RouteInfo>
            implements ListView.OnItemClickListener {
        private final LayoutInflater mInflater;
        private final Drawable mDefaultIcon;
        private final Drawable mSpeakerIcon;
        private final Drawable mSpeakerGroupIcon;
        private final Drawable mBluetoothIcon;

        public RouteAdapter(Context context, List<MediaRouter.RouteInfo> routes) {
            super(context, 0, routes);
            mInflater = LayoutInflater.from(context);
            TypedArray styledAttributes = getContext().obtainStyledAttributes(new int[] {
                    R.attr.mediaRouteDefaultIconDrawable,
                    R.attr.mediaRouteSpeakerIconDrawable,
                    R.attr.mediaRouteSpeakerGroupIconDrawable,
                    R.attr.mediaRouteBluetoothIconDrawable });
            mDefaultIcon = styledAttributes.getDrawable(0);
            mSpeakerIcon = styledAttributes.getDrawable(1);
            mSpeakerGroupIcon = styledAttributes.getDrawable(2);
            mBluetoothIcon = styledAttributes.getDrawable(3);
            styledAttributes.recycle();
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItem(position).isEnabled();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = mInflater.inflate(R.layout.mr_chooser_list_item, parent, false);
            }

            MediaRouter.RouteInfo route = getItem(position);
            TextView text1 = (TextView) view.findViewById(R.id.mr_chooser_route_name);
            TextView text2 = (TextView) view.findViewById(R.id.mr_chooser_route_desc);
            text1.setText(route.getName());
            String description = route.getDescription();
            boolean isConnectedOrConnecting =
                    route.getConnectionState() == CONNECTION_STATE_CONNECTED
                            || route.getConnectionState() == CONNECTION_STATE_CONNECTING;
            if (isConnectedOrConnecting && !TextUtils.isEmpty(description)) {
                text1.setGravity(Gravity.BOTTOM);
                text2.setVisibility(View.VISIBLE);
                text2.setText(description);
            } else {
                text1.setGravity(Gravity.CENTER_VERTICAL);
                text2.setVisibility(View.GONE);
                text2.setText("");
            }
            view.setEnabled(route.isEnabled());

            ImageView iconView = (ImageView) view.findViewById(R.id.mr_chooser_route_icon);
            if (iconView != null) {
                iconView.setImageDrawable(getIconDrawable(route));
            }
            return view;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            MediaRouter.RouteInfo route = getItem(position);
            if (route.isEnabled()) {
                route.select();
                RouteComparator.storeRouteUsageScores(getContext(), route.getId());
                dismiss();
            }
        }

        private Drawable getIconDrawable(MediaRouter.RouteInfo route) {
            Uri iconUri = route.getIconUri();
            if (iconUri != null) {
                try {
                    InputStream is = getContext().getContentResolver().openInputStream(iconUri);
                    Drawable drawable = Drawable.createFromStream(is, null);
                    if (drawable != null) {
                        return drawable;
                    }
                } catch (IOException e) {
                    Log.w(TAG, "Failed to load " + iconUri, e);
                    // Falls back.
                }
            }
            return getDefaultIconDrawable(route);
        }

        private Drawable getDefaultIconDrawable(MediaRouter.RouteInfo route) {
            if (route instanceof MediaRouter.RouteGroup) {
                // Only speakers can be grouped for now.
                return mSpeakerGroupIcon;
            }
            if (isSystemLiveAudioOnlyRoute(route)) {
                return mBluetoothIcon;
            }
            if (route.supportsControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
                    && !route.supportsControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)) {
                return mSpeakerIcon;
            }
            // A workaround to get the speaker icon for known audio devices.
            // TODO: Remove once the new setIconUri API is fully launched.
            if (TextUtils.equals(route.getProvider().getPackageName(), "com.sonos.acr")
                    || route.getDescription().toLowerCase().contains("audio") ) {
                return mSpeakerIcon;
            }
            return mDefaultIcon;
        }
    }

    private final class MediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo info) {
            refreshRoutes();
        }

        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo info) {
            refreshRoutes();
        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo info) {
            refreshRoutes();
        }

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            dismiss();
        }
    }

    private static final class RouteComparator implements Comparator<MediaRouter.RouteInfo> {
        private static final String PREF_ROUTE_IDS =
                "android.support.v7.app.MediaRouteChooserDialog_route_ids";
        private static final String PREF_USAGE_SCORE_PREFIX =
                "android.support.v7.app.MediaRouteChooserDialog_route_usage_score_";
        // Routes with the usage score less than MIN_USAGE_SCORE are decayed.
        private static final float MIN_USAGE_SCORE = 0.1f;
        private static final float USAGE_SCORE_DECAY_FACTOR = 0.95f;

        public static final RouteComparator sInstance = new RouteComparator();
        public static final HashMap<String, Float> sRouteUsageScoreMap = new HashMap();

        @Override
        public int compare(MediaRouter.RouteInfo lhs, MediaRouter.RouteInfo rhs) {
            if (isSystemLiveAudioOnlyRoute(lhs)) {
                if (!isSystemLiveAudioOnlyRoute(rhs)) {
                    return 1;
                }
            } else if (isSystemLiveAudioOnlyRoute(rhs)) {
                return -1;
            }
            Float lhsUsageScore = sRouteUsageScoreMap.get(lhs.getId());
            if (lhsUsageScore == null) {
                lhsUsageScore = 0f;
            }
            Float rhsUsageScore = sRouteUsageScoreMap.get(rhs.getId());
            if (rhsUsageScore == null) {
                rhsUsageScore = 0f;
            }
            if (!lhsUsageScore.equals(rhsUsageScore)) {
                return lhsUsageScore > rhsUsageScore ? -1 : 1;
            }
            return lhs.getName().compareTo(rhs.getName());
        }

        private static void loadRouteUsageScores(
                Context context, List<MediaRouter.RouteInfo> routes) {
            sRouteUsageScoreMap.clear();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            for (MediaRouter.RouteInfo route : routes) {
                sRouteUsageScoreMap.put(route.getId(),
                        preferences.getFloat(PREF_USAGE_SCORE_PREFIX + route.getId(), 0f));
            }
        }

        private static void storeRouteUsageScores(Context context, String selectedRouteId) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor prefEditor = preferences.edit();
            List<String> routeIds = new ArrayList<String>(
                    Arrays.asList(preferences.getString(PREF_ROUTE_IDS, "").split(",")));
            if (!routeIds.contains(selectedRouteId)) {
                routeIds.add(selectedRouteId);
            }
            StringBuilder routeIdsBuilder = new StringBuilder();
            for (String routeId : routeIds) {
                // The new route usage score is calculated as follows:
                // 1) usageScore * USAGE_SCORE_DECAY_FACTOR + 1, if the route is selected,
                // 2) 0, if usageScore * USAGE_SCORE_DECAY_FACTOR < MIN_USAGE_SCORE, or
                // 3) usageScore * USAGE_SCORE_DECAY_FACTOR, otherwise,
                String routeUsageScoreKey = PREF_USAGE_SCORE_PREFIX + routeId;
                float newUsageScore = preferences.getFloat(routeUsageScoreKey, 0f)
                        * USAGE_SCORE_DECAY_FACTOR;
                if (selectedRouteId.equals(routeId)) {
                    newUsageScore += 1f;
                }
                if (newUsageScore < MIN_USAGE_SCORE) {
                    prefEditor.remove(routeId);
                } else {
                    prefEditor.putFloat(routeUsageScoreKey, newUsageScore);
                    if (routeIdsBuilder.length() > 0) {
                        routeIdsBuilder.append(',');
                    }
                    routeIdsBuilder.append(routeId);
                }
            }
            prefEditor.putString(PREF_ROUTE_IDS, routeIdsBuilder.toString());
            prefEditor.commit();
        }
    }

    // Used to determine whether the route represents a bluetooth device.
    // TODO: Find a better way to precisely detect bluetooth routes.
    private static boolean isSystemLiveAudioOnlyRoute(MediaRouter.RouteInfo route) {
        return isSystemMediaRouteProvider(route)
                && route.supportsControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
                && !route.supportsControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO);
    }

    private static boolean isSystemMediaRouteProvider(MediaRouter.RouteInfo route) {
        return TextUtils.equals(route.getProviderInstance().getMetadata().getPackageName(),
                SYSTEM_MEDIA_ROUTE_PROVIDER_PACKAGE_NAME);
    }
}
