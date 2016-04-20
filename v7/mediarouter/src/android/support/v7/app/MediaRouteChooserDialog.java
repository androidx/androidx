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
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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

    private final MediaRouter mRouter;
    private final MediaRouterCallback mCallback;

    private MediaRouteSelector mSelector = MediaRouteSelector.EMPTY;
    private ArrayList<MediaRouter.RouteInfo> mRoutes;
    private RouteAdapter mAdapter;
    private ListView mListView;
    private boolean mAttachedToWindow;
    private AsyncTask<Void, Void, Void> mRefreshRoutesTask;
    private AsyncTask<Void, Void, Void> mOnItemClickTask;

    public MediaRouteChooserDialog(Context context) {
        this(context, 0);
    }

    public MediaRouteChooserDialog(Context context, int theme) {
        super(MediaRouterThemeHelper.createThemedContext(context, theme), theme);
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
        return !route.isDefaultOrBluetooth() && route.isEnabled()
                && route.matchesSelector(mSelector);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.mr_chooser_dialog);
        setTitle(R.string.mr_chooser_title);

        mRoutes = new ArrayList<>();
        mAdapter = new RouteAdapter(getContext(), mRoutes);
        mListView = (ListView)findViewById(R.id.mr_chooser_list);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(mAdapter);
        mListView.setEmptyView(findViewById(android.R.id.empty));

        updateLayout();
    }

    /**
     * Sets the width of the dialog. Also called when configuration changes.
     */
    void updateLayout() {
        getWindow().setLayout(MediaRouteDialogHelper.getDialogWidth(getContext()),
                ViewGroup.LayoutParams.WRAP_CONTENT);
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
            if (mRefreshRoutesTask != null) {
                mRefreshRoutesTask.cancel(true);
                mRefreshRoutesTask = null;
            }
            mRefreshRoutesTask = new AsyncTask<Void, Void, Void>() {
                private ArrayList<MediaRouter.RouteInfo> mNewRoutes;

                @Override
                protected void onPreExecute() {
                    mNewRoutes = new ArrayList<>(mRouter.getRoutes());
                    onFilterRoutes(mNewRoutes);
                }

                @Override
                protected Void doInBackground(Void... params) {
                    // In API 4 ~ 10, AsyncTasks are running in parallel. Needs synchronization.
                    synchronized (MediaRouteChooserDialog.this) {
                        if (!isCancelled()) {
                            RouteComparator.getInstance(getContext())
                                    .loadRouteUsageScores(mNewRoutes);
                        }
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void params) {
                    mRoutes.clear();
                    mRoutes.addAll(mNewRoutes);
                    Collections.sort(mRoutes, RouteComparator.sInstance);
                    mAdapter.notifyDataSetChanged();
                    mRefreshRoutesTask = null;
                }
            }.execute();
        }
    }

    private final class RouteAdapter extends ArrayAdapter<MediaRouter.RouteInfo>
            implements ListView.OnItemClickListener {
        private final LayoutInflater mInflater;
        private final Drawable mDefaultIcon;
        private final Drawable mTvIcon;
        private final Drawable mSpeakerIcon;
        private final Drawable mSpeakerGroupIcon;

        public RouteAdapter(Context context, List<MediaRouter.RouteInfo> routes) {
            super(context, 0, routes);
            mInflater = LayoutInflater.from(context);
            TypedArray styledAttributes = getContext().obtainStyledAttributes(new int[] {
                    R.attr.mediaRouteDefaultIconDrawable,
                    R.attr.mediaRouteTvIconDrawable,
                    R.attr.mediaRouteSpeakerIconDrawable,
                    R.attr.mediaRouteSpeakerGroupIconDrawable});
            mDefaultIcon = styledAttributes.getDrawable(0);
            mTvIcon = styledAttributes.getDrawable(1);
            mSpeakerIcon = styledAttributes.getDrawable(2);
            mSpeakerGroupIcon = styledAttributes.getDrawable(3);
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
            final MediaRouter.RouteInfo route = getItem(position);
            if (route.isEnabled() && mOnItemClickTask == null) {
                mOnItemClickTask = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected void onPreExecute() {
                        route.select();
                    }

                    @Override
                    protected Void doInBackground(Void... params) {
                        RouteComparator.getInstance(getContext())
                                .storeRouteUsageScores(route.getId());
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void params) {
                        dismiss();
                        mOnItemClickTask = null;
                    }
                }.execute();
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
            // If the type of the receiver device is specified, use it.
            switch (route.getDeviceType()) {
                case  MediaRouter.RouteInfo.DEVICE_TYPE_TV:
                    return mTvIcon;
                case MediaRouter.RouteInfo.DEVICE_TYPE_SPEAKER:
                    return mSpeakerIcon;
            }

            // Otherwise, make the best guess based on other route information.
            if (route instanceof MediaRouter.RouteGroup) {
                // Only speakers can be grouped for now.
                return mSpeakerGroupIcon;
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

        private static RouteComparator sInstance;
        private final HashMap<String, Float> mRouteUsageScoreMap;
        private final SharedPreferences mPreferences;

        public static RouteComparator getInstance(Context context) {
            if (sInstance == null) {
                sInstance = new RouteComparator(context);
            }
            return sInstance;
        }

        private RouteComparator(Context context) {
            mRouteUsageScoreMap = new HashMap();
            mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        }

        @Override
        public int compare(MediaRouter.RouteInfo lhs, MediaRouter.RouteInfo rhs) {
            if (lhs == null) {
                return rhs == null ? 0 : -1;
            } else if (rhs == null) {
                return 1;
            }
            Float lhsUsageScore = mRouteUsageScoreMap.get(lhs.getId());
            if (lhsUsageScore == null) {
                lhsUsageScore = 0f;
            }
            Float rhsUsageScore = mRouteUsageScoreMap.get(rhs.getId());
            if (rhsUsageScore == null) {
                rhsUsageScore = 0f;
            }
            if (!lhsUsageScore.equals(rhsUsageScore)) {
                return lhsUsageScore > rhsUsageScore ? -1 : 1;
            }
            return lhs.getName().compareTo(rhs.getName());
        }

        private void loadRouteUsageScores(List<MediaRouter.RouteInfo> routes) {
            for (MediaRouter.RouteInfo route : routes) {
                if (mRouteUsageScoreMap.get(route.getId()) == null) {
                    mRouteUsageScoreMap.put(route.getId(),
                            mPreferences.getFloat(PREF_USAGE_SCORE_PREFIX + route.getId(), 0f));
                }
            }
        }

        private void storeRouteUsageScores(String selectedRouteId) {
            SharedPreferences.Editor prefEditor = mPreferences.edit();
            List<String> routeIds = new ArrayList<>(
                    Arrays.asList(mPreferences.getString(PREF_ROUTE_IDS, "").split(",")));
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
                float newUsageScore = mPreferences.getFloat(routeUsageScoreKey, 0f)
                        * USAGE_SCORE_DECAY_FACTOR;
                if (selectedRouteId.equals(routeId)) {
                    newUsageScore += 1f;
                }
                if (newUsageScore < MIN_USAGE_SCORE) {
                    mRouteUsageScoreMap.remove(routeId);
                    prefEditor.remove(routeId);
                } else {
                    mRouteUsageScoreMap.put(routeId, newUsageScore);
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
}
