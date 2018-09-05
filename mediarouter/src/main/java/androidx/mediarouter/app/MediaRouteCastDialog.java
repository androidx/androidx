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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
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
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appcompat.app.AppCompatDialog;
import androidx.core.util.ObjectsCompat;
import androidx.mediarouter.R;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;
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
@RestrictTo(LIBRARY_GROUP)
public class MediaRouteCastDialog extends AppCompatDialog {
    static final String TAG = "MediaRouteCastDialog";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    // Do not update the route list immediately to avoid unnatural dialog change.
    private static final int UPDATE_ROUTES_DELAY_MS = 300;
    private static final int CONNECTION_TIMEOUT_MS = 30000;
    private static final int UPDATE_VOLUME_DELAY_MS = 500;
    private static final int PROGRESS_BAR_DISPLAY_MS = 400;

    static final int MSG_UPDATE_ROUTES = 1;
    static final int MSG_UPDATE_ROUTE_VOLUME_BY_USER = 2;

    // TODO (b/111731099): Remove this once dark theme is implemented inside MediaRouterThemeHelper.
    static final int COLOR_WHITE_ON_DARK_BACKGROUND = Color.WHITE;

    static final int MUTED_VOLUME = 0;
    static final int MIN_UNMUTED_VOLUME = 1;

    private static final int BLUR_RADIUS = 10;

    final MediaRouter mRouter;
    private final MediaRouterCallback mCallback;
    private MediaRouteSelector mSelector = MediaRouteSelector.EMPTY;
    MediaRouter.RouteInfo mSelectedRoute;
    final List<MediaRouter.RouteInfo> mRoutes = new ArrayList<>();

    Context mContext;
    private boolean mCreated;
    private boolean mAttachedToWindow;
    private long mLastUpdateTime;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MSG_UPDATE_ROUTES:
                    updateRoutes((List<MediaRouter.RouteInfo>) message.obj);
                    break;
                case MSG_UPDATE_ROUTE_VOLUME_BY_USER:
                    if (mRouteForVolumeUpdatingByUser != null) {
                        mRouteForVolumeUpdatingByUser = null;
                        if (mHasPendingUpdate) {
                            update();
                        }
                    }
                    break;
            }
        }
    };
    private RecyclerView mRecyclerView;
    private RecyclerAdapter mAdapter;
    VolumeChangeListener mVolumeChangeListener;
    int mVolumeSliderColor;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Map<String, MediaRouteVolumeSliderHolder> mVolumeSliderHolderMap;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Map<String, Integer> mBeforeMuteVolumeMap;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaRouter.RouteInfo mRouteForVolumeUpdatingByUser;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mHasPendingUpdate;
    boolean mIsSelectingRoute;

    private ImageButton mCloseButton;
    private Button mStopCastingButton;

    private ImageView mMetadataBackground;
    private View mMetadataBlackScrim;
    private ImageView mArtView;
    private TextView mTitleView;
    private TextView mSubtitleView;
    private String mTitlePlaceholder;

    MediaControllerCompat mMediaController;
    MediaControllerCallback mControllerCallback;
    MediaDescriptionCompat mDescription;

    FetchArtTask mFetchArtTask;
    Bitmap mArtIconBitmap;
    Uri mArtIconUri;
    boolean mArtIconIsLoaded;
    Bitmap mArtIconLoadedBitmap;
    int mArtIconBackgroundColor;

    public MediaRouteCastDialog(Context context) {
        this(context, 0);
    }

    public MediaRouteCastDialog(Context context, int theme) {
        super(context = MediaRouterThemeHelper.createThemedDialogContext(context, theme, false),
                MediaRouterThemeHelper.createThemedDialogStyle(context));
        mContext = getContext();

        mRouter = MediaRouter.getInstance(mContext);
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
        try {
            mMediaController = new MediaControllerCompat(mContext, sessionToken);
        } catch (RemoteException e) {
            Log.e(TAG, "Error creating media controller in setMediaSession.", e);
        }
        if (mMediaController != null) {
            mMediaController.registerCallback(mControllerCallback);
        }
        MediaMetadataCompat metadata = mMediaController == null ? null
                : mMediaController.getMetadata();
        mDescription = metadata == null ? null : metadata.getDescription();
        updateArtIconIfNeeded();
        update();
    }

    /**
     * Gets the session to use for metadata and transport controls.
     *
     * @return The token for the session to use or null if none.
     */
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
            }
            refreshRoutes();
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
                && route.matchesSelector(mSelector);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.mr_cast_dialog);

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
        mVolumeSliderColor = MediaRouterThemeHelper.getControllerColor(mContext, 0);
        mVolumeSliderHolderMap = new HashMap<>();
        mBeforeMuteVolumeMap = new HashMap<>();

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
        int width = MediaRouteDialogHelper.getDialogWidth(mContext);
        int height = MediaRouteDialogHelper.getDialogHeight(mContext);
        getWindow().setLayout(width, height);

        mArtIconBitmap = null;
        mArtIconUri = null;
        updateArtIconIfNeeded();
        update();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachedToWindow = true;

        mRouter.addCallback(mSelector, mCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
        refreshRoutes();
        setMediaSession(mRouter.getMediaSessionToken());
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttachedToWindow = false;

        mRouter.removeCallback(mCallback);
        mHandler.removeMessages(MSG_UPDATE_ROUTES);
        setMediaSession(null);
    }

    void update() {
        // Defer dialog updates when user is adjusting volume or selecting route.
        // Since onRouteUnselected is triggered before onRouteSelected when transferring to another
        // route, pending update if mIsSelectingRoute is true to prevent dialog from being dismissed
        // in the process of selecting route.
        if (mRouteForVolumeUpdatingByUser != null || mIsSelectingRoute) {
            mHasPendingUpdate = true;
            return;
        }
        mHasPendingUpdate = false;
        if (!mSelectedRoute.isSelected() || mSelectedRoute.isDefaultOrBluetooth()) {
            dismiss();
            return;
        }
        if (!mCreated) {
            return;
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
        updateMetadataLayout();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static boolean isBitmapRecycled(Bitmap bitmap) {
        return bitmap != null && bitmap.isRecycled();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int getDesiredArtHeight(int originalWidth, int originalHeight) {
        return mArtView.getHeight();
    }

    void updateArtIconIfNeeded() {
        if (!isIconChanged()) {
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
    void clearLoadedBitmap() {
        mArtIconIsLoaded = false;
        mArtIconLoadedBitmap = null;
        mArtIconBackgroundColor = 0;
    }

    /**
     * Returns whether a new art image is different from an original art image. Compares
     * Bitmap objects first, and then compares URIs only if bitmap is unchanged with
     * a null value.
     */
    private boolean isIconChanged() {
        Bitmap newBitmap = mDescription == null ? null : mDescription.getIconBitmap();
        Uri newUri = mDescription == null ? null : mDescription.getIconUri();
        Bitmap oldBitmap = mFetchArtTask == null ? mArtIconBitmap : mFetchArtTask.getIconBitmap();
        Uri oldUri = mFetchArtTask == null ? mArtIconUri : mFetchArtTask.getIconUri();
        if (oldBitmap != newBitmap) {
            return true;
        } else if (oldBitmap == null && ObjectsCompat.equals(oldUri, newUri)) {
            return true;
        }
        return false;
    }

    private void updateMetadataLayout() {
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
     * Refreshes the list of routes that are shown in the chooser dialog.
     */
    public void refreshRoutes() {
        if (mAttachedToWindow) {
            ArrayList<MediaRouter.RouteInfo> routes = new ArrayList<>(mRouter.getRoutes());
            onFilterRoutes(routes);
            Collections.sort(routes, MediaRouteChooserDialog.RouteComparator.sInstance);
            if (SystemClock.uptimeMillis() - mLastUpdateTime >= UPDATE_ROUTES_DELAY_MS) {
                updateRoutes(routes);
            } else {
                mHandler.removeMessages(MSG_UPDATE_ROUTES);
                mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_UPDATE_ROUTES, routes),
                        mLastUpdateTime + UPDATE_ROUTES_DELAY_MS);
            }
        }
    }

    void updateRoutes(List<MediaRouter.RouteInfo> routes) {
        mLastUpdateTime = SystemClock.uptimeMillis();
        mRoutes.clear();
        mRoutes.addAll(routes);
        mAdapter.setItems();
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
        blurAllocation.copyTo(bitmap);

        allocation.destroy();
        blurAllocation.destroy();
        blurScript.destroy();
        rs.destroy();
        return bitmap;
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
                mBeforeMuteVolumeMap.put(mRoute.getId(), mVolumeSlider.getProgress());
            } else {
                mBeforeMuteVolumeMap.remove(mRoute.getId());
            }
        }

        int getUnmutedVolume() {
            Integer beforeMuteVolume = mBeforeMuteVolumeMap.get(mRoute.getId());

            return (beforeMuteVolume == null)
                    ? MIN_UNMUTED_VOLUME : Math.max(MIN_UNMUTED_VOLUME, beforeMuteVolume);
        }
    }

    private final class RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final String TAG = "RecyclerAdapter";
        private static final int ITEM_TYPE_GROUP_VOLUME = 1;
        private static final int ITEM_TYPE_HEADER = 2;
        private static final int ITEM_TYPE_ROUTE = 3;
        private static final int ITEM_TYPE_GROUP = 4;

        private final ArrayList<Item> mItems;
        private final ArrayList<MediaRouter.RouteInfo> mAvailableRoutes;
        private final ArrayList<MediaRouter.RouteInfo> mAvailableGroups;

        private final LayoutInflater mInflater;
        private final Drawable mDefaultIcon;
        private final Drawable mTvIcon;
        private final Drawable mSpeakerIcon;
        private final Drawable mSpeakerGroupIcon;

        RecyclerAdapter() {
            mItems = new ArrayList<>();
            mAvailableRoutes = new ArrayList<>();
            mAvailableGroups = new ArrayList<>();

            mInflater = LayoutInflater.from(mContext);
            mDefaultIcon = MediaRouterThemeHelper.getDefaultDrawableIcon(mContext);
            mTvIcon = MediaRouterThemeHelper.getTvDrawableIcon(mContext);
            mSpeakerIcon = MediaRouterThemeHelper.getSpeakerDrawableIcon(mContext);
            mSpeakerGroupIcon = MediaRouterThemeHelper.getSpeakerGroupDrawableIcon(mContext);

            setItems();
        }

        boolean isSelectedRoute(MediaRouter.RouteInfo route) {
            if (route.isSelected()) {
                return true;
            }
            // If currently casting on a group and route is a member of the group
            if (mSelectedRoute instanceof MediaRouter.RouteGroup) {
                List<MediaRouter.RouteInfo> memberRoutes =
                        ((MediaRouter.RouteGroup) mSelectedRoute).getMemberRoutes();

                for (MediaRouter.RouteInfo memberRoute : memberRoutes) {
                    if (memberRoute.getId().equals(route.getId())) {
                        return true;
                    }
                }
            }
            return false;
        }

        // Create a list of items with mMemberRoutes and add them to mItems
        void setItems() {
            mItems.clear();
            // Add Group Volume item only when currently casting on a group
            if (mSelectedRoute instanceof MediaRouter.RouteGroup) {
                mItems.add(new Item(mSelectedRoute, ITEM_TYPE_GROUP_VOLUME));
                List<MediaRouter.RouteInfo> routes =
                        ((MediaRouter.RouteGroup) mSelectedRoute).getMemberRoutes();

                for (MediaRouter.RouteInfo route: routes) {
                    mItems.add(new Item(route, ITEM_TYPE_ROUTE));
                }
            } else {
                mItems.add(new Item(mSelectedRoute, ITEM_TYPE_ROUTE));
            }

            mAvailableRoutes.clear();
            mAvailableGroups.clear();

            for (MediaRouter.RouteInfo route: mRoutes) {
                // If route is current selected route, skip
                if (isSelectedRoute(route)) {
                    continue;
                }
                if (route instanceof MediaRouter.RouteGroup) {
                    mAvailableGroups.add(route);
                } else {
                    mAvailableRoutes.add(route);
                }
            }

            // Add list items of available routes section to mItems
            if (mAvailableRoutes.size() > 0) {
                mItems.add(new Item(mContext.getString(R.string.mr_dialog_groupable_header),
                        ITEM_TYPE_HEADER));
                for (MediaRouter.RouteInfo route : mAvailableRoutes) {
                    mItems.add(new Item(route, ITEM_TYPE_ROUTE));
                }
            }

            // Add list items of available groups section to mItems
            if (mAvailableGroups.size() > 0) {
                mItems.add(new Item(mContext.getString(R.string.mr_dialog_transferable_header),
                        ITEM_TYPE_HEADER));
                for (MediaRouter.RouteInfo route : mAvailableGroups) {
                    mItems.add(new Item(route, ITEM_TYPE_GROUP));
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
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
        public void onViewRecycled(RecyclerView.ViewHolder holder) {
            super.onViewRecycled(holder);
            mVolumeSliderHolderMap.values().remove(holder);
        }

        @Override
        public int getItemCount() {
            return mItems.size();
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
            if (route instanceof MediaRouter.RouteGroup) {
                // Only speakers can be grouped for now.
                return mSpeakerGroupIcon;
            }
            return mDefaultIcon;
        }

        @Override
        public int getItemViewType(int position) {
            return mItems.get(position).getType();
        }

        public Item getItem(int position) {
            return mItems.get(position);
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

            GroupVolumeViewHolder(View itemView) {
                super(itemView, (ImageButton) itemView.findViewById(R.id.mr_cast_mute_button),
                        (MediaRouteVolumeSlider) itemView.findViewById(R.id.mr_cast_volume_slider));
                mTextView = itemView.findViewById(R.id.mr_group_volume_route_name);
            }

            public void bindGroupVolumeViewHolder(Item item) {
                MediaRouter.RouteInfo route = (MediaRouter.RouteInfo) item.getData();

                super.bindRouteVolumeSliderHolder(route);
                mTextView.setText(route.getName());
            }
        }

        private class HeaderViewHolder extends RecyclerView.ViewHolder {
            private final TextView mTextView;

            HeaderViewHolder(View itemView) {
                super(itemView);
                mTextView = itemView.findViewById(R.id.mr_cast_header_name);
            }

            public void bindHeaderViewHolder(Item item) {
                String headerName = item.getData().toString();

                mTextView.setText(headerName);
            }
        }

        private class RouteViewHolder extends MediaRouteVolumeSliderHolder {
            final ImageView mImageView;
            final ProgressBar mProgressBar;
            final TextView mTextView;
            final RelativeLayout mVolumeSliderLayout;
            final CheckBox mCheckBox;

            final int mExpandedLayoutHeight;
            final int mCollapsedLayoutHeight;
            private final int mLayoutAnimationDurationMs;
            private Interpolator mAccelerateDecelerateInterpolator;

            final Runnable mSelectRoute = new Runnable() {
                @Override
                public void run() {
                    mImageView.setVisibility(View.VISIBLE);
                    mProgressBar.setVisibility(View.INVISIBLE);
                    mCheckBox.setEnabled(true);
                    animateLayoutHeight(mVolumeSliderLayout, mExpandedLayoutHeight);
                }
            };
            final View.OnClickListener mCheckBoxClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (((CheckBox) v).isChecked()) {
                        mImageView.setVisibility(View.INVISIBLE);
                        mProgressBar.setVisibility(View.VISIBLE);
                        mCheckBox.setEnabled(false);
                        mCheckBox.postDelayed(mSelectRoute, PROGRESS_BAR_DISPLAY_MS);
                    } else {
                        mCheckBox.removeCallbacks(mSelectRoute);
                        animateLayoutHeight(mVolumeSliderLayout, mCollapsedLayoutHeight);
                    }
                }
            };

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

                anim.setDuration(mLayoutAnimationDurationMs);
                anim.setInterpolator(mAccelerateDecelerateInterpolator);
                view.startAnimation(anim);
            }

            RouteViewHolder(View itemView) {
                super(itemView, (ImageButton) itemView.findViewById(R.id.mr_cast_mute_button),
                        (MediaRouteVolumeSlider) itemView.findViewById(R.id.mr_cast_volume_slider));
                mImageView = itemView.findViewById(R.id.mr_cast_route_icon);
                mProgressBar = itemView.findViewById(R.id.mr_cast_progress_bar);
                mTextView = itemView.findViewById(R.id.mr_cast_route_name);
                mVolumeSliderLayout = itemView.findViewById(R.id.mr_cast_volume_layout);
                mCheckBox = itemView.findViewById(R.id.mr_cast_checkbox);

                Drawable checkBoxIcon = MediaRouterThemeHelper.getCheckBoxDrawableIcon(mContext);
                MediaRouterThemeHelper.setIndeterminateProgressBarColor(mContext, mProgressBar);
                mCheckBox.setButtonDrawable(checkBoxIcon);

                Resources res = mContext.getResources();
                DisplayMetrics metrics = res.getDisplayMetrics();
                TypedValue value = new TypedValue();
                res.getValue(R.dimen.mr_dynamic_dialog_row_height, value, true);
                mExpandedLayoutHeight = (int) value.getDimension(metrics);
                mCollapsedLayoutHeight = 0;
                mLayoutAnimationDurationMs = res.getInteger(
                        R.integer.mr_cast_volume_slider_layout_animation_duration_ms);
                mAccelerateDecelerateInterpolator = new AccelerateDecelerateInterpolator();
            }

            public void bindRouteViewHolder(Item item) {
                MediaRouter.RouteInfo route = (MediaRouter.RouteInfo) item.getData();
                boolean selected = isSelectedRoute(route);

                super.bindRouteVolumeSliderHolder(route);
                mImageView.setImageDrawable(getIconDrawable(route));
                mTextView.setText(route.getName());
                setLayoutHeight(mVolumeSliderLayout, selected
                        ? mExpandedLayoutHeight : mCollapsedLayoutHeight);
                mCheckBox.setChecked(selected);
                mCheckBox.setOnClickListener(mCheckBoxClickListener);
            }
        }

        private class GroupViewHolder extends RecyclerView.ViewHolder {
            private final View mItemView;
            private final ImageView mImageView;
            private final TextView mTextView;
            MediaRouter.RouteInfo mRoute;

            GroupViewHolder(View itemView) {
                super(itemView);
                mItemView = itemView;
                mImageView = itemView.findViewById(R.id.mr_cast_group_icon);
                mTextView = itemView.findViewById(R.id.mr_cast_group_name);
            }

            public void bindGroupViewHolder(Item item) {
                final MediaRouter.RouteInfo route = (MediaRouter.RouteInfo) item.getData();
                mRoute = route;

                mItemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mIsSelectingRoute = true;
                        mRoute.select();
                    }
                });
                mImageView.setImageDrawable(getIconDrawable(route));
                mTextView.setText(route.getName());
            }
        }
    }

    private final class MediaRouterCallback extends MediaRouter.Callback {
        MediaRouterCallback() {
        }

        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo info) {
            refreshRoutes();
        }

        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo info) {
            refreshRoutes();
        }

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            mSelectedRoute = route;
            mIsSelectingRoute = false;
            update();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            update();
        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            // Call refreshRoutes only when there's no route for volume updating by user.
            if (mRouteForVolumeUpdatingByUser == null) {
                refreshRoutes();
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
            updateArtIconIfNeeded();
            update();
        }
    }

    private class FetchArtTask extends AsyncTask<Void, Void, Bitmap> {
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

        public Bitmap getIconBitmap() {
            return mIconBitmap;
        }

        public Uri getIconUri() {
            return mIconUri;
        }

        @Override
        protected void onPreExecute() {
            clearLoadedBitmap();
        }

        @Override
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
                    int reqHeight = getDesiredArtHeight(options.outWidth, options.outHeight);
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
                update();
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
}