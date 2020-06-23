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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appcompat.app.AppCompatDialog;
import androidx.core.util.ObjectsCompat;
import androidx.mediarouter.R;
import androidx.mediarouter.media.MediaRouteProvider;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;
import androidx.mediarouter.media.MediaRouterParams;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class implements the route cast dialog for {@link MediaRouter}.
 * <p>
 * This dialog allows the user to dynamically control or disconnect from the
 * currently selected route.
 *
 * @see MediaRouteButton
 * @see MediaRouteActionProvider
 * @hide
 */
@RestrictTo(LIBRARY)
public class MediaRouteDynamicControllerDialog extends AppCompatDialog {
    private static final String TAG = "MediaRouteCtrlDialog";
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    // Do not update the route list immediately to avoid unnatural dialog change.
    private static final int UPDATE_ROUTES_VIEW_DELAY_MS = 300;
    private static final int CONNECTION_TIMEOUT_MS = 30000;
    private static final int UPDATE_VOLUME_DELAY_MS = 500;

    private static final int MSG_UPDATE_ROUTES_VIEW = 1;
    private static final int MSG_UPDATE_ROUTE_VOLUME_BY_USER = 2;

    // TODO (b/111731099): Remove this once dark theme is implemented inside MediaRouterThemeHelper.
    private static final int COLOR_WHITE_ON_DARK_BACKGROUND = Color.WHITE;

    private static final int MUTED_VOLUME = 0;
    private static final int MIN_UNMUTED_VOLUME = 1;

    private static final int BLUR_RADIUS = 10;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final MediaRouter mRouter;
    private final MediaRouterCallback mCallback;
    private MediaRouteSelector mSelector = MediaRouteSelector.EMPTY;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaRouter.RouteInfo mSelectedRoute;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final List<MediaRouter.RouteInfo> mMemberRoutes = new ArrayList<>();
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final List<MediaRouter.RouteInfo> mGroupableRoutes = new ArrayList<>();
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final List<MediaRouter.RouteInfo> mTransferableRoutes = new ArrayList<>();

    // List of routes that were previously groupable but temporarily ungroupable.
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final List<MediaRouter.RouteInfo> mUngroupableRoutes = new ArrayList<>();

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Context mContext;
    private boolean mCreated;
    private boolean mAttachedToWindow;
    private long mLastUpdateTime;
    @SuppressWarnings({"WeakerAccess", "deprecation"}) /* synthetic access */
    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MSG_UPDATE_ROUTES_VIEW:
                    updateRoutesView();
                    break;
                case MSG_UPDATE_ROUTE_VOLUME_BY_USER:
                    if (mRouteForVolumeUpdatingByUser != null) {
                        mRouteForVolumeUpdatingByUser = null;
                        // Since updates of views are deferred when the volume is being updated,
                        // call updateViewsIfNeeded to ensure that views are updated properly.
                        updateViewsIfNeeded();
                    }
                    break;
            }
        }
    };
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    RecyclerView mRecyclerView;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    RecyclerAdapter mAdapter;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    VolumeChangeListener mVolumeChangeListener;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Map<String, MediaRouteVolumeSliderHolder> mVolumeSliderHolderMap;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaRouter.RouteInfo mRouteForVolumeUpdatingByUser;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Map<String, Integer> mUnmutedVolumeMap;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mIsSelectingRoute;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mIsAnimatingVolumeSliderLayout;

    private boolean mUpdateRoutesViewDeferred;
    private boolean mUpdateMetadataViewsDeferred;

    private ImageButton mCloseButton;
    private Button mStopCastingButton;

    private ImageView mMetadataBackground;
    private View mMetadataBlackScrim;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    ImageView mArtView;
    private TextView mTitleView;
    private TextView mSubtitleView;
    private String mTitlePlaceholder;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaControllerCompat mMediaController;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaControllerCallback mControllerCallback;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaDescriptionCompat mDescription;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    FetchArtTask mFetchArtTask;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Bitmap mArtIconBitmap;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Uri mArtIconUri;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mArtIconIsLoaded;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Bitmap mArtIconLoadedBitmap;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mArtIconBackgroundColor;
    final boolean mDisableGroupVolumeUX;

    public MediaRouteDynamicControllerDialog(@NonNull Context context) {
        this(context, 0);
    }

    public MediaRouteDynamicControllerDialog(@NonNull Context context, int theme) {
        super(context = MediaRouterThemeHelper.createThemedDialogContext(context, theme, false),
                MediaRouterThemeHelper.createThemedDialogStyle(context));
        mContext = getContext();

        mRouter = MediaRouter.getInstance(mContext);
        MediaRouterParams params = mRouter.getRouterParams();
        Bundle extras = (params != null) ? params.getExtras() : null;
        mDisableGroupVolumeUX = (extras != null
                && extras.getBoolean(MediaRouterParams.EXTRAS_KEY_DISABLE_GROUP_VOLUME_UX));
        mCallback = new MediaRouterCallback();
        mSelectedRoute = mRouter.getSelectedRoute();
        mControllerCallback = new MediaControllerCallback();
        setMediaSession(mRouter.getMediaSessionToken());
    }

    /**
     * Set the session to use for metadata and transport controls. The dialog
     * will listen to changes on this session and update the UI automatically in
     * response to changes.
     *
     * @param sessionToken The token for the session to use.
     */
    private void setMediaSession(MediaSessionCompat.Token sessionToken) {
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mControllerCallback);
            mMediaController = null;
        }
        if (sessionToken == null) {
            return;
        }
        if (!mAttachedToWindow) {
            return;
        }
        mMediaController = new MediaControllerCompat(mContext, sessionToken);
        mMediaController.registerCallback(mControllerCallback);
        MediaMetadataCompat metadata = mMediaController.getMetadata();
        mDescription = metadata == null ? null : metadata.getDescription();
        reloadIconIfNeeded();
        updateMetadataViews();
    }

    /**
     * Gets the session to use for metadata and transport controls.
     *
     * @return The token for the session to use or null if none.
     */
    @Nullable
    public MediaSessionCompat.Token getMediaSession() {
        return mMediaController == null ? null : mMediaController.getSessionToken();
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
                updateRoutes();
            }
        }
    }

    /**
     * Called to filter the set of routes that should be included in the list.
     * <p>
     * The default implementation iterates over all routes in the provided list and
     * removes those for which {@link #onFilterRoute} returns false.
     *
     * @param routes The list of routes to filter in-place, never null.
     */
    public void onFilterRoutes(@NonNull List<MediaRouter.RouteInfo> routes) {
        for (int i = routes.size() - 1; i >= 0; i--) {
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
                && route.matchesSelector(mSelector) && !(mSelectedRoute == route);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.mr_cast_dialog);
        MediaRouterThemeHelper.setDialogBackgroundColor(mContext, this);

        mCloseButton = findViewById(R.id.mr_cast_close_button);
        mCloseButton.setColorFilter(COLOR_WHITE_ON_DARK_BACKGROUND);
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        mStopCastingButton = findViewById(R.id.mr_cast_stop_button);
        mStopCastingButton.setTextColor(COLOR_WHITE_ON_DARK_BACKGROUND);
        mStopCastingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedRoute.isSelected()) {
                    mRouter.unselect(MediaRouter.UNSELECT_REASON_STOPPED);
                }
                dismiss();
            }
        });

        mAdapter = new RecyclerAdapter();
        mRecyclerView = findViewById(R.id.mr_cast_list);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mVolumeChangeListener = new VolumeChangeListener();
        mVolumeSliderHolderMap = new HashMap<>();
        mUnmutedVolumeMap = new HashMap<>();

        mMetadataBackground = findViewById(R.id.mr_cast_meta_background);
        mMetadataBlackScrim = findViewById(R.id.mr_cast_meta_black_scrim);
        mArtView = findViewById(R.id.mr_cast_meta_art);
        mTitleView = findViewById(R.id.mr_cast_meta_title);
        mTitleView.setTextColor(COLOR_WHITE_ON_DARK_BACKGROUND);
        mSubtitleView = findViewById(R.id.mr_cast_meta_subtitle);
        mSubtitleView.setTextColor(COLOR_WHITE_ON_DARK_BACKGROUND);
        Resources res = mContext.getResources();
        mTitlePlaceholder = res.getString(R.string.mr_cast_dialog_title_view_placeholder);

        mCreated = true;
        updateLayout();
    }

    /**
     * Sets the width of the dialog. Also called when configuration changes.
     */
    void updateLayout() {
        int width = MediaRouteDialogHelper.getDialogWidthForDynamicGroup(mContext);
        int height = MediaRouteDialogHelper.getDialogHeight(mContext);
        getWindow().setLayout(width, height);

        mArtIconBitmap = null;
        mArtIconUri = null;
        reloadIconIfNeeded();
        updateMetadataViews();
        updateRoutesView();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachedToWindow = true;

        mRouter.addCallback(mSelector, mCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
        updateRoutes();
        setMediaSession(mRouter.getMediaSessionToken());
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttachedToWindow = false;

        mRouter.removeCallback(mCallback);
        mHandler.removeCallbacksAndMessages(null);
        setMediaSession(null);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static boolean isBitmapRecycled(Bitmap bitmap) {
        return bitmap != null && bitmap.isRecycled();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void reloadIconIfNeeded() {
        Bitmap newBitmap = mDescription == null ? null : mDescription.getIconBitmap();
        Uri newUri = mDescription == null ? null : mDescription.getIconUri();
        Bitmap oldBitmap = mFetchArtTask == null ? mArtIconBitmap : mFetchArtTask.getIconBitmap();
        Uri oldUri = mFetchArtTask == null ? mArtIconUri : mFetchArtTask.getIconUri();

        if (oldBitmap == newBitmap
                && (oldBitmap != null || ObjectsCompat.equals(oldUri, newUri))) {
            return;
        }
        if (mFetchArtTask != null) {
            mFetchArtTask.cancel(true);
        }
        mFetchArtTask = new FetchArtTask();
        mFetchArtTask.execute();
    }

    /**
     * Clear the bitmap loaded by FetchArtTask. Will be called after the loaded bitmaps are applied
     * to artwork, or no longer valid.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void clearLoadedBitmap() {
        mArtIconIsLoaded = false;
        mArtIconLoadedBitmap = null;
        mArtIconBackgroundColor = 0;
    }

    /**
     * Returns whether updateMetadataViews and updateRoutesView should defer updating views.
     */
    private boolean shouldDeferUpdateViews() {
        // Defer updating views when user is adjusting volume or selecting route.
        // Since onRouteUnselected is triggered before onRouteSelected when transferring to
        // another route, pending update if mIsSelectingRoute is true to prevent dialog from
        // being dismissed in the process of selecting route.
        if (mRouteForVolumeUpdatingByUser != null || mIsSelectingRoute
                || mIsAnimatingVolumeSliderLayout) {
            return true;
        }
        // Defer updating views if corresponding views aren't created yet.
        return !mCreated;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void updateViewsIfNeeded() {
        // Call updateRoutesView if update of routes view is deferred.
        if (mUpdateRoutesViewDeferred) {
            updateRoutesView();
        }
        // Call updateMetadataViews if update of metadata views are deferred.
        if (mUpdateMetadataViewsDeferred) {
            updateMetadataViews();
        }
    }

    /* synthetic access */
    @SuppressWarnings({"WeakerAccess", "ObjectToString"})
    void updateMetadataViews() {
        if (shouldDeferUpdateViews()) {
            mUpdateMetadataViewsDeferred = true;
            return;
        }
        mUpdateMetadataViewsDeferred = false;
        // Dismiss dialog if there's no non-default selected route.
        if (!mSelectedRoute.isSelected() || mSelectedRoute.isDefaultOrBluetooth()) {
            dismiss();
        }
        if (mArtIconIsLoaded && !isBitmapRecycled(mArtIconLoadedBitmap)
                && mArtIconLoadedBitmap != null) {
            mArtView.setVisibility(View.VISIBLE);
            mArtView.setImageBitmap(mArtIconLoadedBitmap);
            mArtView.setBackgroundColor(mArtIconBackgroundColor);

            // Blur will not be supported for SDK < 17 devices to avoid unnecessarily bloating
            // the size of this package (approximately two-fold). Instead, only the black scrim
            // will be placed on top of the metadata background.
            mMetadataBlackScrim.setVisibility(View.VISIBLE);
            if (Build.VERSION.SDK_INT >= 17) {
                Bitmap blurredBitmap = blurBitmap(mArtIconLoadedBitmap, BLUR_RADIUS, mContext);
                mMetadataBackground.setImageBitmap(blurredBitmap);
            } else {
                mMetadataBackground.setImageBitmap(Bitmap.createBitmap(mArtIconLoadedBitmap));
            }
        } else {
            if (isBitmapRecycled(mArtIconLoadedBitmap)) {
                Log.w(TAG, "Can't set artwork image with recycled bitmap: " + mArtIconLoadedBitmap);
            }
            mArtView.setVisibility(View.GONE);
            mMetadataBlackScrim.setVisibility(View.GONE);
            mMetadataBackground.setImageBitmap(null);
        }
        clearLoadedBitmap();

        CharSequence title = mDescription == null ? null : mDescription.getTitle();
        boolean hasTitle = !TextUtils.isEmpty(title);

        CharSequence subtitle = mDescription == null ? null : mDescription.getSubtitle();
        boolean hasSubtitle = !TextUtils.isEmpty(subtitle);

        if (hasTitle) {
            mTitleView.setText(title);
        } else {
            mTitleView.setText(mTitlePlaceholder);
        }
        if (hasSubtitle) {
            mSubtitleView.setText(subtitle);
            mSubtitleView.setVisibility(View.VISIBLE);
        } else {
            mSubtitleView.setVisibility(View.GONE);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static void setLayoutHeight(View view, int height) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.height = height;
        view.setLayoutParams(lp);
    }

    private class VolumeChangeListener implements SeekBar.OnSeekBarChangeListener {
        VolumeChangeListener() {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            if (mRouteForVolumeUpdatingByUser != null) {
                mHandler.removeMessages(MSG_UPDATE_ROUTE_VOLUME_BY_USER);
            }
            mRouteForVolumeUpdatingByUser = (MediaRouter.RouteInfo) seekBar.getTag();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // Defer resetting mRouteForTouchedVolumeSlider to allow the media route provider
            // a little time to settle into its new state and publish the final
            // volume update.
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_ROUTE_VOLUME_BY_USER,
                    UPDATE_VOLUME_DELAY_MS);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                MediaRouter.RouteInfo route = (MediaRouter.RouteInfo) seekBar.getTag();
                MediaRouteVolumeSliderHolder holder = mVolumeSliderHolderMap.get(route.getId());

                if (holder != null) {
                    holder.setMute(progress == MUTED_VOLUME);
                }
                route.requestSetVolume(progress);
            }
        }
    }

    /**
     * Returns a list of currently groupable routes of the selected route.
     * If the selected route is not dynamic group, returns empty list.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    List<MediaRouter.RouteInfo> getCurrentGroupableRoutes() {
        List<MediaRouter.RouteInfo> groupableRoutes = new ArrayList<>();
        for (MediaRouter.RouteInfo route : mSelectedRoute.getProvider().getRoutes()) {
            MediaRouter.RouteInfo.DynamicGroupState state =
                    mSelectedRoute.getDynamicGroupState(route);
            if (state != null && state.isGroupable()) {
                groupableRoutes.add(route);
            }
        }
        return groupableRoutes;
    }

    /**
     * Updates the visible status(groupable/unselectable status and volume) of routes.
     * The position of the routes is not changed and no routes are added/removed.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void updateRoutesView() {
        if (mAttachedToWindow) {
            if (SystemClock.uptimeMillis() - mLastUpdateTime >= UPDATE_ROUTES_VIEW_DELAY_MS) {
                if (shouldDeferUpdateViews()) {
                    mUpdateRoutesViewDeferred = true;
                    return;
                }
                mUpdateRoutesViewDeferred = false;
                // Dismiss dialog if there's no non-default selected route.
                if (!mSelectedRoute.isSelected() || mSelectedRoute.isDefaultOrBluetooth()) {
                    dismiss();
                }
                mLastUpdateTime = SystemClock.uptimeMillis();
                mAdapter.notifyAdapterDataSetChanged();
            } else {
                mHandler.removeMessages(MSG_UPDATE_ROUTES_VIEW);
                mHandler.sendEmptyMessageAtTime(MSG_UPDATE_ROUTES_VIEW,
                        mLastUpdateTime + UPDATE_ROUTES_VIEW_DELAY_MS);
            }
        }
    }

    /**
     * Updates routes and items of the adapter.
     * It introduces new routes or hides removed routes.
     * Calling this method would result in sudden UI changes due to change of the adapter.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void updateRoutes() {
        mMemberRoutes.clear();
        mGroupableRoutes.clear();
        mTransferableRoutes.clear();

        mMemberRoutes.addAll(mSelectedRoute.getMemberRoutes());
        for (MediaRouter.RouteInfo route : mSelectedRoute.getProvider().getRoutes()) {
            MediaRouter.RouteInfo.DynamicGroupState state =
                    mSelectedRoute.getDynamicGroupState(route);
            if (state == null) continue;

            if (state.isGroupable()) {
                mGroupableRoutes.add(route);
            }
            if (state.isTransferable()) {
                mTransferableRoutes.add(route);
            }
        }

        // Filter routes.
        onFilterRoutes(mGroupableRoutes);
        onFilterRoutes(mTransferableRoutes);

        // Sort routes.
        Collections.sort(mMemberRoutes, RouteComparator.sInstance);
        Collections.sort(mGroupableRoutes, RouteComparator.sInstance);
        Collections.sort(mTransferableRoutes, RouteComparator.sInstance);

        mAdapter.updateItems();
    }

    @RequiresApi(17)
    private static Bitmap blurBitmap(Bitmap bitmap, float radius, Context context) {
        RenderScript rs = RenderScript.create(context);
        Allocation allocation = Allocation.createFromBitmap(rs, bitmap);
        Allocation blurAllocation = Allocation.createTyped(rs, allocation.getType());

        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        blurScript.setRadius(radius);
        blurScript.setInput(allocation);
        blurScript.forEach(blurAllocation);

        Bitmap mutableBitmap = bitmap.copy(bitmap.getConfig(), true /* isMutable */);
        blurAllocation.copyTo(mutableBitmap);

        allocation.destroy();
        blurAllocation.destroy();
        blurScript.destroy();
        rs.destroy();
        return mutableBitmap;
    }

    private abstract class MediaRouteVolumeSliderHolder extends RecyclerView.ViewHolder {
        MediaRouter.RouteInfo mRoute;
        final ImageButton mMuteButton;
        final MediaRouteVolumeSlider mVolumeSlider;

        MediaRouteVolumeSliderHolder(
                View itemView, ImageButton muteButton, MediaRouteVolumeSlider volumeSlider) {
            super(itemView);
            mMuteButton = muteButton;
            mVolumeSlider = volumeSlider;

            Drawable muteButtonIcon = MediaRouterThemeHelper.getMuteButtonDrawableIcon(mContext);
            mMuteButton.setImageDrawable(muteButtonIcon);
            MediaRouterThemeHelper.setVolumeSliderColor(mContext, mVolumeSlider);
        }

        @CallSuper
        void bindRouteVolumeSliderHolder(MediaRouter.RouteInfo route) {
            mRoute = route;
            int volume = mRoute.getVolume();
            boolean isMuted = (volume == MUTED_VOLUME);

            mMuteButton.setActivated(isMuted);
            mMuteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mRouteForVolumeUpdatingByUser != null) {
                        mHandler.removeMessages(MSG_UPDATE_ROUTE_VOLUME_BY_USER);
                    }
                    mRouteForVolumeUpdatingByUser = mRoute;

                    boolean mute = !v.isActivated();
                    int volume = mute ? MUTED_VOLUME : getUnmutedVolume();

                    setMute(mute);
                    mVolumeSlider.setProgress(volume);
                    mRoute.requestSetVolume(volume);
                    // Defer resetting mRouteForClickedMuteButton to allow the media route provider
                    // a little time to settle into its new state and publish the final
                    // volume update.
                    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_ROUTE_VOLUME_BY_USER,
                            UPDATE_VOLUME_DELAY_MS);
                }
            });

            mVolumeSlider.setTag(mRoute);
            mVolumeSlider.setMax(route.getVolumeMax());
            mVolumeSlider.setProgress(volume);
            mVolumeSlider.setOnSeekBarChangeListener(mVolumeChangeListener);
        }

        void updateVolume() {
            int volume = mRoute.getVolume();

            setMute(volume == MUTED_VOLUME);
            mVolumeSlider.setProgress(volume);
        }

        void setMute(boolean mute) {
            boolean wasMuted = mMuteButton.isActivated();
            if (wasMuted == mute) {
                return;
            }

            mMuteButton.setActivated(mute);

            if (mute) {
                // Save current progress, who is the progress just before muted, so that the volume
                // can be restored to that value when user unmutes it.
                mUnmutedVolumeMap.put(mRoute.getId(), mVolumeSlider.getProgress());
            } else {
                mUnmutedVolumeMap.remove(mRoute.getId());
            }
        }

        int getUnmutedVolume() {
            Integer beforeMuteVolume = mUnmutedVolumeMap.get(mRoute.getId());

            return (beforeMuteVolume == null)
                    ? MIN_UNMUTED_VOLUME : Math.max(MIN_UNMUTED_VOLUME, beforeMuteVolume);
        }
    }

    private final class RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int ITEM_TYPE_GROUP_VOLUME = 1;
        private static final int ITEM_TYPE_HEADER = 2;
        private static final int ITEM_TYPE_ROUTE = 3;
        private static final int ITEM_TYPE_GROUP = 4;

        private final ArrayList<Item> mItems;
        private final LayoutInflater mInflater;
        private final Drawable mDefaultIcon;
        private final Drawable mTvIcon;
        private final Drawable mSpeakerIcon;
        private final Drawable mSpeakerGroupIcon;
        private Item mGroupVolumeItem;
        private final int mLayoutAnimationDurationMs;
        private final Interpolator mAccelerateDecelerateInterpolator;

        RecyclerAdapter() {
            mItems = new ArrayList<>();
            mInflater = LayoutInflater.from(mContext);
            mDefaultIcon = MediaRouterThemeHelper.getDefaultDrawableIcon(mContext);
            mTvIcon = MediaRouterThemeHelper.getTvDrawableIcon(mContext);
            mSpeakerIcon = MediaRouterThemeHelper.getSpeakerDrawableIcon(mContext);
            mSpeakerGroupIcon = MediaRouterThemeHelper.getSpeakerGroupDrawableIcon(mContext);

            Resources res = mContext.getResources();
            mLayoutAnimationDurationMs = res.getInteger(
                    R.integer.mr_cast_volume_slider_layout_animation_duration_ms);
            mAccelerateDecelerateInterpolator = new AccelerateDecelerateInterpolator();

            updateItems();
        }

        boolean isGroupVolumeNeeded() {
            return !mDisableGroupVolumeUX && mSelectedRoute.getMemberRoutes().size() > 1;
        }

        void animateLayoutHeight(final View view, int targetHeight) {
            final int startValue = view.getLayoutParams().height;
            final int endValue = targetHeight;

            Animation anim = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    int deltaHeight = (int) ((endValue - startValue) * interpolatedTime);
                    setLayoutHeight(view, startValue + deltaHeight);
                }
            };

            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationStart(Animation animation) {
                    mIsAnimatingVolumeSliderLayout = true;
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mIsAnimatingVolumeSliderLayout = false;
                    updateViewsIfNeeded();
                }
            });
            anim.setDuration(mLayoutAnimationDurationMs);
            anim.setInterpolator(mAccelerateDecelerateInterpolator);
            view.startAnimation(anim);
        }

        void mayUpdateGroupVolume(MediaRouter.RouteInfo route, boolean selected) {
            List<MediaRouter.RouteInfo> members = mSelectedRoute.getMemberRoutes();
            // Assume we have at least one member route(itself)
            int memberCount = Math.max(1, members.size());

            if (route.isGroup()) {
                for (MediaRouter.RouteInfo changedRoute : route.getMemberRoutes()) {
                    if (members.contains(changedRoute) != selected) {
                        memberCount += selected ? 1 : -1;
                    }
                }
            } else {
                memberCount += selected ? 1 : -1;
            }

            boolean wasShown = isGroupVolumeNeeded();
            // Group volume is shown when two or more members are in the selected route.
            boolean shouldShow = !mDisableGroupVolumeUX && memberCount >= 2;

            if (wasShown != shouldShow) {
                RecyclerView.ViewHolder viewHolder =
                        mRecyclerView.findViewHolderForAdapterPosition(0);

                if (viewHolder instanceof GroupVolumeViewHolder) {
                    GroupVolumeViewHolder groupVolumeHolder = (GroupVolumeViewHolder) viewHolder;
                    animateLayoutHeight(groupVolumeHolder.itemView, shouldShow
                            ? groupVolumeHolder.getExpandedHeight() : 0);
                }
            }
        }

        // Create a list of items with mMemberRoutes and add them to mItems
        void updateItems() {
            mItems.clear();

            mGroupVolumeItem = new Item(mSelectedRoute, ITEM_TYPE_GROUP_VOLUME);
            if (!mMemberRoutes.isEmpty()) {
                for (MediaRouter.RouteInfo memberRoute : mMemberRoutes) {
                    mItems.add(new Item(memberRoute, ITEM_TYPE_ROUTE));
                }
            } else {
                mItems.add(new Item(mSelectedRoute, ITEM_TYPE_ROUTE));
            }

            if (!mGroupableRoutes.isEmpty()) {
                boolean headerAdded = false;
                for (MediaRouter.RouteInfo groupableRoute : mGroupableRoutes) {
                    if (!mMemberRoutes.contains(groupableRoute)) {
                        if (!headerAdded) {
                            MediaRouteProvider.DynamicGroupRouteController controller =
                                    mSelectedRoute.getDynamicGroupController();
                            String title = (controller != null)
                                    ? controller.getGroupableSelectionTitle() : null;
                            if (TextUtils.isEmpty(title)) {
                                title = mContext.getString(R.string.mr_dialog_groupable_header);
                            }
                            mItems.add(new Item(title, ITEM_TYPE_HEADER));
                            headerAdded = true;
                        }
                        mItems.add(new Item(groupableRoute, ITEM_TYPE_ROUTE));
                    }
                }
            }

            if (!mTransferableRoutes.isEmpty()) {
                boolean headerAdded = false;
                for (MediaRouter.RouteInfo transferableRoute : mTransferableRoutes) {
                    if (mSelectedRoute != transferableRoute) {
                        if (!headerAdded) {
                            headerAdded = true;
                            MediaRouteProvider.DynamicGroupRouteController controller =
                                    mSelectedRoute.getDynamicGroupController();
                            String title = (controller != null)
                                    ? controller.getTransferableSectionTitle()
                                    : null;
                            if (TextUtils.isEmpty(title)) {
                                title = mContext.getString(R.string.mr_dialog_transferable_header);
                            }
                            mItems.add(new Item(title, ITEM_TYPE_HEADER));
                        }
                        mItems.add(new Item(transferableRoute, ITEM_TYPE_GROUP));
                    }
                }
            }
            notifyAdapterDataSetChanged();
        }

        /*
         * Can't override RecyclerView.Adpater#notifyDataSetChanged because it's final method. So,
         * implement method with slightly different name.
         */
        void notifyAdapterDataSetChanged() {
            // Get ungroupable routes which are positioning at groupable routes section.
            // This can happen when dynamically added routes can't be grouped with some of other
            // routes at groupable routes section.
            mUngroupableRoutes.clear();
            mUngroupableRoutes.addAll(MediaRouteDialogHelper.getItemsRemoved(mGroupableRoutes,
                    getCurrentGroupableRoutes()));
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;

            switch (viewType) {
                case ITEM_TYPE_GROUP_VOLUME:
                    view = mInflater.inflate(R.layout.mr_cast_group_volume_item, parent, false);
                    return new GroupVolumeViewHolder(view);
                case ITEM_TYPE_HEADER:
                    view = mInflater.inflate(R.layout.mr_cast_header_item, parent, false);
                    return new HeaderViewHolder(view);
                case ITEM_TYPE_ROUTE:
                    view = mInflater.inflate(R.layout.mr_cast_route_item, parent, false);
                    return new RouteViewHolder(view);
                case ITEM_TYPE_GROUP:
                    view = mInflater.inflate(R.layout.mr_cast_group_item, parent, false);
                    return new GroupViewHolder(view);
                default:
                    Log.w(TAG, "Cannot create ViewHolder because of wrong view type");
                    return null;
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            int viewType = getItemViewType(position);
            Item item = getItem(position);

            switch (viewType) {
                case ITEM_TYPE_GROUP_VOLUME: {
                    MediaRouter.RouteInfo route = (MediaRouter.RouteInfo) item.getData();
                    mVolumeSliderHolderMap.put(
                            route.getId(), (MediaRouteVolumeSliderHolder) holder);
                    ((GroupVolumeViewHolder) holder).bindGroupVolumeViewHolder(item);
                    break;
                }
                case ITEM_TYPE_HEADER: {
                    ((HeaderViewHolder) holder).bindHeaderViewHolder(item);
                    break;
                }
                case ITEM_TYPE_ROUTE: {
                    MediaRouter.RouteInfo route = (MediaRouter.RouteInfo) item.getData();
                    mVolumeSliderHolderMap.put(
                            route.getId(), (MediaRouteVolumeSliderHolder) holder);
                    ((RouteViewHolder) holder).bindRouteViewHolder(item);
                    break;
                }
                case ITEM_TYPE_GROUP: {
                    ((GroupViewHolder) holder).bindGroupViewHolder(item);
                    break;
                }
                default: {
                    Log.w(TAG, "Cannot bind item to ViewHolder because of wrong view type");
                    break;
                }
            }
        }

        @Override
        public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
            super.onViewRecycled(holder);
            mVolumeSliderHolderMap.values().remove(holder);
        }

        @Override
        public int getItemCount() {
            return mItems.size() + 1;
        }

        Drawable getIconDrawable(MediaRouter.RouteInfo route) {
            Uri iconUri = route.getIconUri();
            if (iconUri != null) {
                try {
                    InputStream is = mContext.getContentResolver().openInputStream(iconUri);
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

        @Override
        public int getItemViewType(int position) {
            return getItem(position).getType();
        }

        public Item getItem(int position) {
            if (position == 0) {
                return mGroupVolumeItem;
            } else {
                return mItems.get(position - 1);
            }
        }

        /**
         * Item class contains information of section header(text of section header) and
         * route(text of route name, icon of route type)
         */
        private class Item {
            private final Object mData;
            private final int mType;

            Item(Object data, int type) {
                mData = data;
                mType = type;
            }

            public Object getData() {
                return mData;
            }

            public int getType() {
                return mType;
            }
        }

        private class GroupVolumeViewHolder extends MediaRouteVolumeSliderHolder {
            private final TextView mTextView;
            private final int mExpandedHeight;

            GroupVolumeViewHolder(View itemView) {
                super(itemView, (ImageButton) itemView.findViewById(R.id.mr_cast_mute_button),
                        (MediaRouteVolumeSlider) itemView.findViewById(R.id.mr_cast_volume_slider));
                mTextView = itemView.findViewById(R.id.mr_group_volume_route_name);

                Resources res = mContext.getResources();
                DisplayMetrics metrics = res.getDisplayMetrics();
                TypedValue value = new TypedValue();
                res.getValue(R.dimen.mr_dynamic_volume_group_list_item_height, value, true);
                mExpandedHeight = (int) value.getDimension(metrics);
            }

            void bindGroupVolumeViewHolder(Item item) {
                setLayoutHeight(itemView, isGroupVolumeNeeded() ? mExpandedHeight : 0);

                MediaRouter.RouteInfo route = (MediaRouter.RouteInfo) item.getData();

                super.bindRouteVolumeSliderHolder(route);
                mTextView.setText(route.getName());
            }

            int getExpandedHeight() {
                return mExpandedHeight;
            }
        }

        private class HeaderViewHolder extends RecyclerView.ViewHolder {
            private final TextView mTextView;

            HeaderViewHolder(View itemView) {
                super(itemView);
                mTextView = itemView.findViewById(R.id.mr_cast_header_name);
            }

            void bindHeaderViewHolder(Item item) {
                String headerName = item.getData().toString();

                mTextView.setText(headerName);
            }
        }

        private class RouteViewHolder extends MediaRouteVolumeSliderHolder {
            final View mItemView;
            final ImageView mImageView;
            final ProgressBar mProgressBar;
            final TextView mTextView;
            final RelativeLayout mVolumeSliderLayout;
            final CheckBox mCheckBox;

            final float mDisabledAlpha;
            final int mExpandedLayoutHeight;
            final int mCollapsedLayoutHeight;

            final View.OnClickListener mViewClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Toggle it's state
                    boolean selected = !isSelected(mRoute);
                    boolean isGroup = mRoute.isGroup();

                    if (selected) {
                        mRouter.addMemberToDynamicGroup(mRoute);
                    } else {
                        mRouter.removeMemberFromDynamicGroup(mRoute);
                    }
                    showSelectingProgress(selected, !isGroup);
                    if (isGroup) {
                        List<MediaRouter.RouteInfo> selectedRoutes =
                                mSelectedRoute.getMemberRoutes();
                        for (MediaRouter.RouteInfo route : mRoute.getMemberRoutes()) {
                            if (selectedRoutes.contains(route) != selected) {
                                MediaRouteVolumeSliderHolder volumeSliderHolder =
                                        mVolumeSliderHolderMap.get(route.getId());
                                if (volumeSliderHolder instanceof RouteViewHolder) {
                                    RouteViewHolder routeViewHolder =
                                            (RouteViewHolder) volumeSliderHolder;
                                    routeViewHolder.showSelectingProgress(selected, true);
                                }
                            }
                        }
                    }
                    mayUpdateGroupVolume(mRoute, selected);
                }
            };

            RouteViewHolder(View itemView) {
                super(itemView, (ImageButton) itemView.findViewById(R.id.mr_cast_mute_button),
                        (MediaRouteVolumeSlider) itemView.findViewById(R.id.mr_cast_volume_slider));
                mItemView = itemView;
                mImageView = itemView.findViewById(R.id.mr_cast_route_icon);
                mProgressBar = itemView.findViewById(R.id.mr_cast_route_progress_bar);
                mTextView = itemView.findViewById(R.id.mr_cast_route_name);
                mVolumeSliderLayout = itemView.findViewById(R.id.mr_cast_volume_layout);
                mCheckBox = itemView.findViewById(R.id.mr_cast_checkbox);

                Drawable checkBoxIcon = MediaRouterThemeHelper.getCheckBoxDrawableIcon(mContext);
                mCheckBox.setButtonDrawable(checkBoxIcon);
                MediaRouterThemeHelper.setIndeterminateProgressBarColor(mContext, mProgressBar);

                mDisabledAlpha = MediaRouterThemeHelper.getDisabledAlpha(mContext);
                Resources res = mContext.getResources();
                DisplayMetrics metrics = res.getDisplayMetrics();
                TypedValue value = new TypedValue();
                res.getValue(R.dimen.mr_dynamic_dialog_row_height, value, true);
                mExpandedLayoutHeight = (int) value.getDimension(metrics);
                mCollapsedLayoutHeight = 0;
            }

            boolean isSelected(MediaRouter.RouteInfo route) {
                if (route.isSelected()) {
                    return true;
                }
                MediaRouter.RouteInfo.DynamicGroupState state =
                        mSelectedRoute.getDynamicGroupState(route);
                return state != null && state.getSelectionState()
                        == MediaRouteProvider.DynamicGroupRouteController
                        .DynamicRouteDescriptor.SELECTED;
            }

            private boolean isEnabled(MediaRouter.RouteInfo route) {
                // Ungroupable route that is in groupable section has to be disabled.
                if (mUngroupableRoutes.contains(route)) {
                    return false;
                }
                // The last member route can not be removed.
                if (isSelected(route) && mSelectedRoute.getMemberRoutes().size() < 2) {
                    return false;
                }
                // Selected route that can't be unselected has to be disabled.
                if (isSelected(route)) {
                    MediaRouter.RouteInfo.DynamicGroupState state =
                            mSelectedRoute.getDynamicGroupState(route);
                    return state != null && state.isUnselectable();
                }
                return true;
            }

            void bindRouteViewHolder(Item item) {
                MediaRouter.RouteInfo route = (MediaRouter.RouteInfo) item.getData();

                // This is required to sync volume and the name of the route
                if (route == mSelectedRoute && route.getMemberRoutes().size() > 0) {
                    for (MediaRouter.RouteInfo memberRoute : route.getMemberRoutes()) {
                        if (!mGroupableRoutes.contains(memberRoute)) {
                            route = memberRoute;
                            break;
                        }
                    }
                }
                bindRouteVolumeSliderHolder(route);

                // Get icons for route and checkbox.
                mImageView.setImageDrawable(getIconDrawable(route));
                mTextView.setText(route.getName());
                mCheckBox.setVisibility(View.VISIBLE);
                boolean selected = isSelected(route);
                boolean enabled = isEnabled(route);

                // Set checked state of checkbox and replace progress bar with route type icon.
                mCheckBox.setChecked(selected);
                mProgressBar.setVisibility(View.INVISIBLE);
                mImageView.setVisibility(View.VISIBLE);

                // Set enabled states of views, height of volume slider layout and alpha value
                // of itemView.
                mItemView.setEnabled(enabled);
                mCheckBox.setEnabled(enabled);
                mMuteButton.setEnabled(enabled || selected);
                mVolumeSlider.setEnabled(enabled || selected);
                mItemView.setOnClickListener(mViewClickListener);
                mCheckBox.setOnClickListener(mViewClickListener);

                // Do not show the volume slider of a group in this row
                setLayoutHeight(mVolumeSliderLayout, selected
                        && !mRoute.isGroup()
                        ? mExpandedLayoutHeight : mCollapsedLayoutHeight);

                mItemView.setAlpha(enabled || selected ? 1.0f : mDisabledAlpha);
                mCheckBox.setAlpha(enabled || !selected ? 1.0f : mDisabledAlpha);
            }

            void showSelectingProgress(boolean selected, boolean shouldChangeHeight) {
                // Disable views not to be clicked twice
                // They will be enabled when the view is refreshed
                mCheckBox.setEnabled(false);
                mItemView.setEnabled(false);
                mCheckBox.setChecked(selected);
                if (selected) {
                    mImageView.setVisibility(View.INVISIBLE);
                    mProgressBar.setVisibility(View.VISIBLE);
                }
                if (shouldChangeHeight) {
                    animateLayoutHeight(mVolumeSliderLayout, selected
                            ? mExpandedLayoutHeight : mCollapsedLayoutHeight);
                }
            }
        }

        private class GroupViewHolder extends RecyclerView.ViewHolder {
            final View mItemView;
            final ImageView mImageView;
            final ProgressBar mProgressBar;
            final TextView mTextView;
            final float mDisabledAlpha;
            MediaRouter.RouteInfo mRoute;

            GroupViewHolder(View itemView) {
                super(itemView);
                mItemView = itemView;
                mImageView = itemView.findViewById(R.id.mr_cast_group_icon);
                mProgressBar = itemView.findViewById(R.id.mr_cast_group_progress_bar);
                mTextView = itemView.findViewById(R.id.mr_cast_group_name);
                mDisabledAlpha = MediaRouterThemeHelper.getDisabledAlpha(mContext);

                MediaRouterThemeHelper.setIndeterminateProgressBarColor(mContext, mProgressBar);
            }

            private boolean isEnabled(MediaRouter.RouteInfo route) {
                List<MediaRouter.RouteInfo> currentMemberRoutes =
                        mSelectedRoute.getMemberRoutes();
                // Disable individual route if the only member of dynamic group is that route.
                if (currentMemberRoutes.size() == 1 && currentMemberRoutes.get(0) == route) {
                    return false;
                }
                return true;
            }

            void bindGroupViewHolder(Item item) {
                final MediaRouter.RouteInfo route = (MediaRouter.RouteInfo) item.getData();
                mRoute = route;
                mImageView.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.INVISIBLE);

                boolean enabled = isEnabled(route);
                mItemView.setAlpha(enabled ? 1.0f : mDisabledAlpha);
                mItemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mRouter.transferToRoute(mRoute);
                        mImageView.setVisibility(View.INVISIBLE);
                        mProgressBar.setVisibility(View.VISIBLE);
                    }
                });
                mImageView.setImageDrawable(getIconDrawable(route));
                mTextView.setText(route.getName());
            }
        }
    }

    // When a new route is selected, member/groupable/transferable routes are not updated
    // immediately in onRouteSelected(). Instead, onRouteChanged() is called after a while.
    // So we should refresh items in onRouteChanged().
    // But onRouteChanged() is also called when a member is added/removed so we refresh
    // items only when a new route is found, which happens right after a new member is selected.
    private final class MediaRouterCallback extends MediaRouter.Callback {
        MediaRouterCallback() {
        }

        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo info) {
            updateRoutesView();
        }

        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo info) {
            updateRoutesView();
        }

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            mSelectedRoute = route;

            mIsSelectingRoute = false;
            // Since updates of views are deferred when selecting the route,
            // call updateViewsIfNeeded to ensure that views are updated properly.
            updateViewsIfNeeded();
            updateRoutes();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            updateRoutesView();
        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            boolean shouldRefreshRoute = false;
            if (route == mSelectedRoute && route.getDynamicGroupController() != null) {
                for (MediaRouter.RouteInfo memberRoute : route.getProvider().getRoutes()) {
                    if (mSelectedRoute.getMemberRoutes().contains(memberRoute)) {
                        continue;
                    }
                    MediaRouter.RouteInfo.DynamicGroupState state =
                            mSelectedRoute.getDynamicGroupState(memberRoute);

                    // Refresh items only when a new groupable route is found.
                    if (state != null && state.isGroupable()
                            && !mGroupableRoutes.contains(memberRoute)) {
                        shouldRefreshRoute = true;
                        break;
                    }
                }
            }
            if (shouldRefreshRoute) {
                updateViewsIfNeeded();
                // Calls updateRoutes to show new routes.
                updateRoutes();
            } else {
                updateRoutesView();
            }
        }

        @Override
        public void onRouteVolumeChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            int volume = route.getVolume();
            if (DEBUG) {
                Log.d(TAG, "onRouteVolumeChanged(), route.getVolume:" + volume);
            }
            if (mRouteForVolumeUpdatingByUser != route) {
                MediaRouteVolumeSliderHolder holder = mVolumeSliderHolderMap.get(route.getId());
                if (holder != null) {
                    holder.updateVolume();
                }
            }
        }
    }

    private final class MediaControllerCallback extends MediaControllerCompat.Callback {
        MediaControllerCallback() {
        }

        @Override
        public void onSessionDestroyed() {
            if (mMediaController != null) {
                mMediaController.unregisterCallback(mControllerCallback);
                mMediaController = null;
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            mDescription = metadata == null ? null : metadata.getDescription();
            reloadIconIfNeeded();
            updateMetadataViews();
        }
    }

    private class FetchArtTask extends android.os.AsyncTask<Void, Void, Bitmap> {
        private final Bitmap mIconBitmap;
        private final Uri mIconUri;
        private int mBackgroundColor;

        FetchArtTask() {
            Bitmap bitmap = mDescription == null ? null : mDescription.getIconBitmap();
            if (isBitmapRecycled(bitmap)) {
                Log.w(TAG, "Can't fetch the given art bitmap because it's already recycled.");
                bitmap = null;
            }
            mIconBitmap = bitmap;
            mIconUri = mDescription == null ? null : mDescription.getIconUri();
        }

        Bitmap getIconBitmap() {
            return mIconBitmap;
        }

        Uri getIconUri() {
            return mIconUri;
        }

        @Override
        protected void onPreExecute() {
            clearLoadedBitmap();
        }

        @Override
        @SuppressWarnings("ObjectToString")
        protected Bitmap doInBackground(Void... arg) {
            Bitmap art = null;
            if (mIconBitmap != null) {
                art = mIconBitmap;
            } else if (mIconUri != null) {
                InputStream stream = null;
                try {
                    if ((stream = openInputStreamByScheme(mIconUri)) == null) {
                        Log.w(TAG, "Unable to open: " + mIconUri);
                        return null;
                    }
                    // Query art size.
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(stream, null, options);
                    if (options.outWidth == 0 || options.outHeight == 0) {
                        return null;
                    }
                    // Rewind the stream in order to restart art decoding.
                    try {
                        stream.reset();
                    } catch (IOException e) {
                        // Failed to rewind the stream, try to reopen it.
                        stream.close();
                        if ((stream = openInputStreamByScheme(mIconUri)) == null) {
                            Log.w(TAG, "Unable to open: " + mIconUri);
                            return null;
                        }
                    }
                    // Calculate required size to decode the art and possibly resize it.
                    options.inJustDecodeBounds = false;
                    int reqHeight = mContext.getResources().getDimensionPixelSize(
                            R.dimen.mr_cast_meta_art_size);
                    int ratio = options.outHeight / reqHeight;
                    options.inSampleSize = Math.max(1, Integer.highestOneBit(ratio));
                    if (isCancelled()) {
                        return null;
                    }
                    art = BitmapFactory.decodeStream(stream, null, options);
                } catch (IOException e) {
                    Log.w(TAG, "Unable to open: " + mIconUri, e);
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
            if (isBitmapRecycled(art)) {
                Log.w(TAG, "Can't use recycled bitmap: " + art);
                return null;
            }
            if (art != null && art.getWidth() < art.getHeight()) {
                // Portrait art requires dominant color as background color.
                Palette palette = new Palette.Builder(art).maximumColorCount(1).generate();
                mBackgroundColor = palette.getSwatches().isEmpty()
                        ? 0 : palette.getSwatches().get(0).getRgb();
            }
            return art;
        }

        @Override
        protected void onPostExecute(Bitmap art) {
            mFetchArtTask = null;
            if (!ObjectsCompat.equals(mArtIconBitmap, mIconBitmap)
                    || !ObjectsCompat.equals(mArtIconUri, mIconUri)) {
                mArtIconBitmap = mIconBitmap;
                mArtIconLoadedBitmap = art;
                mArtIconUri = mIconUri;
                mArtIconBackgroundColor = mBackgroundColor;
                mArtIconIsLoaded = true;
                // Loaded bitmap will be applied on the next update
                updateMetadataViews();
            }
        }

        private InputStream openInputStreamByScheme(Uri uri) throws IOException {
            String scheme = uri.getScheme().toLowerCase();
            InputStream stream = null;
            if (ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme)
                    || ContentResolver.SCHEME_CONTENT.equals(scheme)
                    || ContentResolver.SCHEME_FILE.equals(scheme)) {
                stream = mContext.getContentResolver().openInputStream(uri);
            } else {
                URL url = new URL(uri.toString());
                URLConnection conn = url.openConnection();
                conn.setConnectTimeout(CONNECTION_TIMEOUT_MS);
                conn.setReadTimeout(CONNECTION_TIMEOUT_MS);
                stream = conn.getInputStream();
            }
            return (stream == null) ? null : new BufferedInputStream(stream);
        }
    }

    static final class RouteComparator implements Comparator<MediaRouter.RouteInfo> {
        static final RouteComparator sInstance = new RouteComparator();

        @Override
        public int compare(MediaRouter.RouteInfo lhs, MediaRouter.RouteInfo rhs) {
            return lhs.getName().compareToIgnoreCase(rhs.getName());
        }
    }
}
