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

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.mediarouter.R;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private static final String PREF_ROUTE_IDS = "android.support.v7.media.MediaRoute_route_ids";
    private static final float MIN_USAGE_SCORE = 0.1f;
    private static final float MEMORY_DECAY_FACTOR = 0.95f;

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

        setContentView(R.layout.mr_media_route_chooser_dialog);
        setTitle(R.string.mr_media_route_chooser_title);

        mRoutes = new ArrayList<MediaRouter.RouteInfo>();
        mAdapter = new RouteAdapter(getContext(), mRoutes);
        mListView = (ListView)findViewById(R.id.media_route_list);
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
            readRouteUsageScore(RouteComparator.sRouteUsageScoreMap);
            Collections.sort(mRoutes, RouteComparator.sInstance);
            mAdapter.notifyDataSetChanged();
        }
    }

    private void readRouteUsageScore(HashMap<String, Float> usageScoreMap) {
        usageScoreMap.clear();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        for (MediaRouter.RouteInfo route : mRoutes) {
            usageScoreMap.put(route.getId(), preferences.getFloat(route.getId(), 0f));
        }
    }

    private void writeRouteUsageScore(String selectedRouteId) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor prefEditor = preferences.edit();
        Set<String> routeIdSet = new HashSet<String>(
                Arrays.asList(preferences.getString(PREF_ROUTE_IDS, "").split(",")));
        routeIdSet.add(selectedRouteId);
        StringBuilder routeIdsBuilder = new StringBuilder();
        for (String routeId : routeIdSet) {
            // The next route usage score will be calculated as follows:
            // 1) usageScore * MEMORY_DECAY_FACTOR + 1, if the route is the selected one,
            // 2) 0, if usageScore * MEMORY_DECAY_FACTOR < MIN_USAGE_SCORE, and
            // 3) usageScore * MEMORY_DECAY_FACTOR, otherwise,
            float newUsageScore = preferences.getFloat(routeId, 0f) * MEMORY_DECAY_FACTOR;
            if (selectedRouteId.equals(routeId)) {
                newUsageScore += 1f;
            }
            if (newUsageScore < MIN_USAGE_SCORE) {
                // We only keep the usage score for more than MIN_USAGE_SCORE.
                prefEditor.remove(routeId);
            } else {
                prefEditor.putFloat(routeId, newUsageScore);
                if (routeIdsBuilder.length() > 0) {
                    routeIdsBuilder.append(',');
                }
                routeIdsBuilder.append(routeId);
            }
        }
        // Save the route ids in order to apply MEMORY_DECAY_FACTOR for all the saved usage scores.
        prefEditor.putString(PREF_ROUTE_IDS, routeIdsBuilder.toString());
        prefEditor.commit();
    }

    private final class RouteAdapter extends ArrayAdapter<MediaRouter.RouteInfo>
            implements ListView.OnItemClickListener {
        private final LayoutInflater mInflater;
        private final Drawable mDefaultIcon;
        private final Drawable mSpeakerIcon;
        private final Drawable mSpeakerGroupIcon;

        public RouteAdapter(Context context, List<MediaRouter.RouteInfo> routes) {
            super(context, 0, routes);
            mInflater = LayoutInflater.from(context);
            int[] attrs = new int[] {
                    R.attr.mediaRouteDefaultIconDrawable,
                    R.attr.mediaRouteSpeakerIconDrawable,
                    R.attr.mediaRouteSpeakerGroupIconDrawable };
            TypedArray styledAttributes = getContext().obtainStyledAttributes(attrs);
            mDefaultIcon = styledAttributes.getDrawable(0);
            mSpeakerIcon = styledAttributes.getDrawable(1);
            mSpeakerGroupIcon = styledAttributes.getDrawable(2);
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
                view = mInflater.inflate(R.layout.mr_media_route_list_item, parent, false);
            }

            MediaRouter.RouteInfo route = getItem(position);
            TextView text1 = (TextView) view.findViewById(android.R.id.text1);
            TextView text2 = (TextView) view.findViewById(android.R.id.text2);
            text1.setText(route.getName());
            String description = route.getDescription();
            if (TextUtils.isEmpty(description)) {
                text2.setVisibility(View.GONE);
                text2.setText("");
            } else {
                text2.setVisibility(View.VISIBLE);
                text2.setText(description);
            }
            view.setEnabled(route.isEnabled());

            ImageView iconView = (ImageView) view.findViewById(R.id.routeIcon);
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
                writeRouteUsageScore(route.getId());
                dismiss();
            }
        }

        private Drawable getIconDrawable(MediaRouter.RouteInfo route) {
            if (route instanceof MediaRouter.RouteGroup) {
                // Only speakers can be grouped for now.
                return mSpeakerGroupIcon;
            }
            if (route.supportsControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
                    && !route.supportsControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)) {
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
        // Should match to SystemMediaRouteProvider.PACKAGE_NAME.
        static final String SYSTEM_MEDIA_ROUTE_PROVIDER_PACKAGE_NAME = "android";

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
            if (lhsUsageScore.equals(rhsUsageScore)) {
                return lhs.getName().compareTo(rhs.getName());
            }
            return lhsUsageScore > rhsUsageScore ? -1 : 1;
        }

        private boolean isSystemLiveAudioOnlyRoute(MediaRouter.RouteInfo route) {
            return isSystemMediaRouteProvider(route)
                    && route.supportsControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
                    && !route.supportsControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO);
        }

        private boolean isSystemMediaRouteProvider(MediaRouter.RouteInfo route) {
            return TextUtils.equals(route.getProviderInstance().getMetadata().getPackageName(),
                    SYSTEM_MEDIA_ROUTE_PROVIDER_PACKAGE_NAME);
        }
    }
}
