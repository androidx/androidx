/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.mediarouter.app;

import static androidx.mediarouter.media.MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTED;
import static androidx.mediarouter.media.MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTING;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.mediarouter.R;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
public class MediaRouteChooserDialog extends AppCompatDialog {
    static final String TAG = "MediaRouteChooserDialog";

    // Do not update the route list immediately to avoid unnatural dialog change.
    private static final long UPDATE_ROUTES_DELAY_MS = 300L;
    private static final int MSG_UPDATE_ROUTES = 1;
    private static final int MSG_SHOW_WIFI_HINT = 2;
    private static final int MSG_SHOW_NO_ROUTES = 3;
    private static final int SHOW_WIFI_HINT_DELAY_MS = 5000;
    private static final int SHOW_NO_ROUTES_DELAY_MS = 15000;

    private final MediaRouter mRouter;
    private final MediaRouterCallback mCallback;
    private MediaRouteSelector mSelector = MediaRouteSelector.EMPTY;
    private ArrayList<MediaRouter.RouteInfo> mRoutes;

    // UI View elements
    private TextView mTitleView;
    private TextView mSearchingRoutesTextView;
    private RelativeLayout mWifiWarningContainer;
    private TextView mWifiWarningTextView;
    private TextView mLearnMoreTextView;
    private LinearLayout mOkButtonContainer;
    private Button mOkButton;
    private ProgressBar mSearchingProgressBar;
    private ListView mListView;
    private RouteAdapter mAdapter;
    private ScreenOnOffReceiver mScreenOnOffReceiver;

    private boolean mAttachedToWindow;
    private long mLastUpdateTime;
    @SuppressWarnings({"unchecked", "deprecation"})
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MSG_UPDATE_ROUTES:
                    handleUpdateRoutes((List<MediaRouter.RouteInfo>) message.obj);
                    break;
                case MSG_SHOW_WIFI_HINT:
                    handleShowNoWifiWarning();
                    break;
                case MSG_SHOW_NO_ROUTES:
                    handleShowNoRoutes();
                    break;
            }
        }
    };

    private static final int FINDING_DEVICES = 0;
    private static final int SHOWING_ROUTES = 1;
    private static final int NO_DEVICES_NO_WIFI_HINT = 2;
    private static final int NO_ROUTES = 3;


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FINDING_DEVICES, SHOWING_ROUTES, NO_DEVICES_NO_WIFI_HINT, NO_ROUTES})
    private @interface MediaRouterChooserDialogState {}

    public MediaRouteChooserDialog(@NonNull Context context) {
        this(context, 0);
    }

    public MediaRouteChooserDialog(@NonNull Context context, int theme) {
        super(context = MediaRouterThemeHelper.createThemedDialogContext(context, theme, false),
                MediaRouterThemeHelper.createThemedDialogStyle(context));
        context = getContext();

        mRouter = MediaRouter.getInstance(context);
        mCallback = new MediaRouterCallback();
        mScreenOnOffReceiver = new ScreenOnOffReceiver();
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
    public void setTitle(@Nullable CharSequence title) {
        mTitleView.setText(title);
    }

    @Override
    public void setTitle(int titleId) {
        mTitleView.setText(titleId);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.mr_chooser_dialog);

        mRoutes = new ArrayList<>();
        mAdapter = new RouteAdapter(getContext(), mRoutes);

        mTitleView = findViewById(R.id.mr_chooser_title);

        mSearchingRoutesTextView = findViewById(R.id.mr_chooser_searching);
        mWifiWarningContainer = findViewById(R.id.mr_chooser_wifi_warning_container);
        mWifiWarningTextView = findViewById(R.id.mr_chooser_wifi_warning_description);
        mLearnMoreTextView = findViewById(R.id.mr_chooser_wifi_learn_more);
        mOkButtonContainer = findViewById(R.id.mr_chooser_ok_button_container);
        mOkButton = findViewById(R.id.mr_chooser_ok_button);
        mSearchingProgressBar = findViewById(R.id.mr_chooser_search_progress_bar);

        String wifiWarningText = DeviceUtils.getDialogChooserWifiWarningDescription(getContext());
        mWifiWarningTextView.setText(wifiWarningText);

        mLearnMoreTextView.setMovementMethod(LinkMovementMethod.getInstance());
        mOkButton.setOnClickListener(view -> dismiss());

        mListView = findViewById(R.id.mr_chooser_list);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(mAdapter);
        mListView.setEmptyView(findViewById(android.R.id.empty));

        updateLayout();

        registerBroadcastReceiver();
    }

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        getContext().registerReceiver(mScreenOnOffReceiver, filter);
    }

    @Override
    public void dismiss() {
        unregisterBroadcastReceiver();
        super.dismiss();
    }

    private void unregisterBroadcastReceiver() {
        try {
            getContext().unregisterReceiver(mScreenOnOffReceiver);
        } catch (IllegalArgumentException e) {
            // May already be unregistered; ignore.
        }
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

        mHandler.removeMessages(MSG_SHOW_WIFI_HINT);
        mHandler.removeMessages(MSG_SHOW_NO_ROUTES);
        mHandler.removeMessages(MSG_UPDATE_ROUTES);

        mHandler.sendMessageDelayed(
                mHandler.obtainMessage(MSG_SHOW_WIFI_HINT), SHOW_WIFI_HINT_DELAY_MS);
    }

    @Override
    public void onDetachedFromWindow() {
        mAttachedToWindow = false;

        mRouter.removeCallback(mCallback);
        mHandler.removeMessages(MSG_UPDATE_ROUTES);
        mHandler.removeMessages(MSG_SHOW_WIFI_HINT);
        mHandler.removeMessages(MSG_SHOW_NO_ROUTES);

        super.onDetachedFromWindow();
    }

    /**
     * Refreshes the list of routes that are shown in the chooser dialog.
     */
    public void refreshRoutes() {
        if (mAttachedToWindow) {
            ArrayList<MediaRouter.RouteInfo> routes = new ArrayList<>(mRouter.getRoutes());
            onFilterRoutes(routes);
            Collections.sort(routes, RouteComparator.sInstance);
            if (SystemClock.uptimeMillis() - mLastUpdateTime >= UPDATE_ROUTES_DELAY_MS) {
                handleUpdateRoutes(routes);
            } else {
                mHandler.removeMessages(MSG_UPDATE_ROUTES);
                mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_UPDATE_ROUTES, routes),
                        mLastUpdateTime + UPDATE_ROUTES_DELAY_MS);
            }
        }
    }

    void handleUpdateRoutes(List<MediaRouter.RouteInfo> routes) {
        mLastUpdateTime = SystemClock.uptimeMillis();
        mRoutes.clear();
        mRoutes.addAll(routes);
        mAdapter.notifyDataSetChanged();

        mHandler.removeMessages(MSG_SHOW_NO_ROUTES);
        mHandler.removeMessages(MSG_SHOW_WIFI_HINT);

        if (routes.isEmpty()) {
            // When all routes are removed or disconnected
            updateViewForState(FINDING_DEVICES);

            mHandler.sendMessageDelayed(
                    mHandler.obtainMessage(MSG_SHOW_WIFI_HINT), SHOW_WIFI_HINT_DELAY_MS);
        } else {
            updateViewForState(SHOWING_ROUTES);
        }
    }

    void handleShowNoWifiWarning() {
        if (mRoutes.isEmpty()) {
            updateViewForState(NO_DEVICES_NO_WIFI_HINT);
            mHandler.removeMessages(MSG_SHOW_WIFI_HINT);
            mHandler.removeMessages(MSG_SHOW_NO_ROUTES);
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SHOW_NO_ROUTES),
                    SHOW_NO_ROUTES_DELAY_MS);
        }
    }

    void handleShowNoRoutes() {
        if (mRoutes.isEmpty()) {
            updateViewForState(NO_ROUTES);
            mHandler.removeMessages(MSG_SHOW_WIFI_HINT);
            mHandler.removeMessages(MSG_SHOW_NO_ROUTES);
            mHandler.removeMessages(MSG_UPDATE_ROUTES);
            mRouter.removeCallback(mCallback);
        }
    }

    void updateViewForState(@MediaRouterChooserDialogState int state) {
        switch (state) {
            case FINDING_DEVICES:
                updateViewForFindingDevices();
                break;
            case NO_DEVICES_NO_WIFI_HINT:
                updateViewForNoDevicesNoWifiHint();
                break;
            case NO_ROUTES:
                updateViewForNoRoutes();
                break;
            case SHOWING_ROUTES:
                updateViewForShowingRoutes();
                break;
        }
    }

    private void updateViewForFindingDevices() {
        setTitle(R.string.mr_chooser_title);
        mListView.setVisibility(View.GONE);
        mSearchingRoutesTextView.setVisibility(View.VISIBLE);
        mSearchingProgressBar.setVisibility(View.VISIBLE);
        mOkButtonContainer.setVisibility(View.GONE);
        mOkButton.setVisibility(View.GONE);
        mLearnMoreTextView.setVisibility(View.GONE);
        mWifiWarningContainer.setVisibility(View.GONE);
    }

    private void updateViewForNoDevicesNoWifiHint() {
        setTitle(R.string.mr_chooser_title);
        mListView.setVisibility(View.GONE);
        mSearchingRoutesTextView.setVisibility(View.GONE);
        mSearchingProgressBar.setVisibility(View.VISIBLE);
        mOkButtonContainer.setVisibility(View.GONE);
        mOkButton.setVisibility(View.GONE);
        mLearnMoreTextView.setVisibility(View.INVISIBLE);
        mWifiWarningContainer.setVisibility(View.VISIBLE);
    }

    private void updateViewForNoRoutes() {
        setTitle(R.string.mr_chooser_zero_routes_found_title);
        mListView.setVisibility(View.GONE);
        mSearchingRoutesTextView.setVisibility(View.GONE);
        mSearchingProgressBar.setVisibility(View.GONE);
        mOkButtonContainer.setVisibility(View.VISIBLE);
        mOkButton.setVisibility(View.VISIBLE);
        mLearnMoreTextView.setVisibility(View.VISIBLE);
        mWifiWarningContainer.setVisibility(View.VISIBLE);
    }

    private void updateViewForShowingRoutes() {
        setTitle(R.string.mr_chooser_title);
        mListView.setVisibility(View.VISIBLE);
        mSearchingRoutesTextView.setVisibility(View.GONE);
        mSearchingProgressBar.setVisibility(View.GONE);
        mOkButtonContainer.setVisibility(View.GONE);
        mOkButton.setVisibility(View.GONE);
        mLearnMoreTextView.setVisibility(View.GONE);
        mWifiWarningContainer.setVisibility(View.GONE);
    }

    private static final class RouteAdapter extends ArrayAdapter<MediaRouter.RouteInfo>
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

            mDefaultIcon = AppCompatResources.getDrawable(context,
                    styledAttributes.getResourceId(0, 0));
            mTvIcon = AppCompatResources.getDrawable(context,
                    styledAttributes.getResourceId(1, 0));
            mSpeakerIcon = AppCompatResources.getDrawable(context,
                    styledAttributes.getResourceId(2, 0));
            mSpeakerGroupIcon = AppCompatResources.getDrawable(context,
                    styledAttributes.getResourceId(3, 0));
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

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = mInflater.inflate(R.layout.mr_chooser_list_item, parent, false);
            }

            MediaRouter.RouteInfo route = getItem(position);
            TextView text1 = view.findViewById(R.id.mr_chooser_route_name);
            TextView text2 = view.findViewById(R.id.mr_chooser_route_desc);
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

            ImageView iconView = view.findViewById(R.id.mr_chooser_route_icon);
            if (iconView != null) {
                iconView.setImageDrawable(getIconDrawable(route));
            }
            return view;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            MediaRouter.RouteInfo route = getItem(position);
            ImageView iconView = view.findViewById(R.id.mr_chooser_route_icon);
            ProgressBar progressBar = view.findViewById(R.id.mr_chooser_route_progress_bar);
            // Show the progress bar
            if (iconView != null && progressBar != null) {
                iconView.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            }
            route.select();
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
                case MediaRouter.RouteInfo.DEVICE_TYPE_TV:
                    return mTvIcon;
                case MediaRouter.RouteInfo.DEVICE_TYPE_SPEAKER:
                    return mSpeakerIcon;
            }

            // Otherwise, make the best guess based on other route information.
            if (route.isGroup()) {
                // Only speakers can be grouped for now.
                return mSpeakerGroupIcon;
            }
            return mDefaultIcon;
        }
    }

    private final class MediaRouterCallback extends MediaRouter.Callback {
        MediaRouterCallback() {
        }

        @Override
        public void onRouteAdded(@NonNull MediaRouter router, @NonNull MediaRouter.RouteInfo info) {
            refreshRoutes();
        }

        @Override
        public void onRouteRemoved(@NonNull MediaRouter router,
                @NonNull MediaRouter.RouteInfo info) {
            refreshRoutes();
        }

        @Override
        public void onRouteChanged(@NonNull MediaRouter router,
                @NonNull MediaRouter.RouteInfo info) {
            refreshRoutes();
        }

        @Override
        public void onRouteSelected(@NonNull MediaRouter router,
                @NonNull MediaRouter.RouteInfo route) {
            dismiss();
        }
    }

    static final class RouteComparator implements Comparator<MediaRouter.RouteInfo> {
        public static final RouteComparator sInstance = new RouteComparator();

        @Override
        public int compare(MediaRouter.RouteInfo lhs, MediaRouter.RouteInfo rhs) {
            return lhs.getName().compareToIgnoreCase(rhs.getName());
        }
    }

    final class ScreenOnOffReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                dismiss();
            }
        }
    }
}