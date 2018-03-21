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

import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY_PAUSE;
import static android.support.v4.media.session.PlaybackStateCompat.ACTION_STOP;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.util.ObjectsCompat;
import androidx.core.view.accessibility.AccessibilityEventCompat;
import android.support.v4.media.session.MediaControllerCompat;
import androidx.mediarouter.R;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;
import androidx.palette.graphics.Palette;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * This class implements the route controller dialog for {@link MediaRouter}.
 * <p>
 * This dialog allows the user to control or disconnect from the currently selected route.
 * </p>
 *
 * @see MediaRouteButton
 * @see MediaRouteActionProvider
 */
public class MediaRouteControllerDialog extends AlertDialog {
    // Tags should be less than 24 characters long (see docs for android.util.Log.isLoggable())
    static final String TAG = "MediaRouteCtrlDialog";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    // Time to wait before updating the volume when the user lets go of the seek bar
    // to allow the route provider time to propagate the change and publish a new
    // route descriptor.
    static final int VOLUME_UPDATE_DELAY_MILLIS = 500;
    static final int CONNECTION_TIMEOUT_MILLIS = (int) TimeUnit.SECONDS.toMillis(30L);

    private static final int BUTTON_NEUTRAL_RES_ID = android.R.id.button3;
    static final int BUTTON_DISCONNECT_RES_ID = android.R.id.button2;
    static final int BUTTON_STOP_RES_ID = android.R.id.button1;

    final MediaRouter mRouter;
    private final MediaRouterCallback mCallback;
    final MediaRouter.RouteInfo mRoute;

    Context mContext;
    private boolean mCreated;
    private boolean mAttachedToWindow;

    private int mDialogContentWidth;

    private View mCustomControlView;

    private Button mDisconnectButton;
    private Button mStopCastingButton;
    private ImageButton mPlaybackControlButton;
    private ImageButton mCloseButton;
    private MediaRouteExpandCollapseButton mGroupExpandCollapseButton;

    private FrameLayout mExpandableAreaLayout;
    private LinearLayout mDialogAreaLayout;
    FrameLayout mDefaultControlLayout;
    private FrameLayout mCustomControlLayout;
    private ImageView mArtView;
    private TextView mTitleView;
    private TextView mSubtitleView;
    private TextView mRouteNameTextView;

    private boolean mVolumeControlEnabled = true;
    // Layout for media controllers including play/pause button and the main volume slider.
    private LinearLayout mMediaMainControlLayout;
    private RelativeLayout mPlaybackControlLayout;
    private LinearLayout mVolumeControlLayout;
    private View mDividerView;

    OverlayListView mVolumeGroupList;
    VolumeGroupAdapter mVolumeGroupAdapter;
    private List<MediaRouter.RouteInfo> mGroupMemberRoutes;
    Set<MediaRouter.RouteInfo> mGroupMemberRoutesAdded;
    private Set<MediaRouter.RouteInfo> mGroupMemberRoutesRemoved;
    Set<MediaRouter.RouteInfo> mGroupMemberRoutesAnimatingWithBitmap;
    SeekBar mVolumeSlider;
    VolumeChangeListener mVolumeChangeListener;
    MediaRouter.RouteInfo mRouteInVolumeSliderTouched;
    private int mVolumeGroupListItemIconSize;
    private int mVolumeGroupListItemHeight;
    private int mVolumeGroupListMaxHeight;
    private final int mVolumeGroupListPaddingTop;
    Map<MediaRouter.RouteInfo, SeekBar> mVolumeSliderMap;

    MediaControllerCompat mMediaController;
    MediaControllerCallback mControllerCallback;
    PlaybackStateCompat mState;
    MediaDescriptionCompat mDescription;

    FetchArtTask mFetchArtTask;
    Bitmap mArtIconBitmap;
    Uri mArtIconUri;
    boolean mArtIconIsLoaded;
    Bitmap mArtIconLoadedBitmap;
    int mArtIconBackgroundColor;

    boolean mHasPendingUpdate;
    boolean mPendingUpdateAnimationNeeded;

    boolean mIsGroupExpanded;
    boolean mIsGroupListAnimating;
    boolean mIsGroupListAnimationPending;
    int mGroupListAnimationDurationMs;
    private int mGroupListFadeInDurationMs;
    private int mGroupListFadeOutDurationMs;

    private Interpolator mInterpolator;
    private Interpolator mLinearOutSlowInInterpolator;
    private Interpolator mFastOutSlowInInterpolator;
    private Interpolator mAccelerateDecelerateInterpolator;

    final AccessibilityManager mAccessibilityManager;

    Runnable mGroupListFadeInAnimation = new Runnable() {
        @Override
        public void run() {
            startGroupListFadeInAnimation();
        }
    };

    public MediaRouteControllerDialog(Context context) {
        this(context, 0);
    }

    public MediaRouteControllerDialog(Context context, int theme) {
        super(context = MediaRouterThemeHelper.createThemedDialogContext(context, theme, true),
                MediaRouterThemeHelper.createThemedDialogStyle(context));
        mContext = getContext();

        mControllerCallback = new MediaControllerCallback();
        mRouter = MediaRouter.getInstance(mContext);
        mCallback = new MediaRouterCallback();
        mRoute = mRouter.getSelectedRoute();
        setMediaSession(mRouter.getMediaSessionToken());
        mVolumeGroupListPaddingTop = mContext.getResources().getDimensionPixelSize(
                R.dimen.mr_controller_volume_group_list_padding_top);
        mAccessibilityManager =
                (AccessibilityManager) mContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context,
                    R.interpolator.mr_linear_out_slow_in);
            mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context,
                    R.interpolator.mr_fast_out_slow_in);
        }
        mAccelerateDecelerateInterpolator = new AccelerateDecelerateInterpolator();
    }

    /**
     * Gets the route that this dialog is controlling.
     */
    public MediaRouter.RouteInfo getRoute() {
        return mRoute;
    }

    private MediaRouter.RouteGroup getGroup() {
        if (mRoute instanceof MediaRouter.RouteGroup) {
            return (MediaRouter.RouteGroup) mRoute;
        }
        return null;
    }

    /**
     * Provides the subclass an opportunity to create a view that will replace the default media
     * controls for the currently playing content.
     *
     * @param savedInstanceState The dialog's saved instance state.
     * @return The media control view, or null if none.
     */
    public View onCreateMediaControlView(Bundle savedInstanceState) {
        return null;
    }

    /**
     * Gets the media control view that was created by {@link #onCreateMediaControlView(Bundle)}.
     *
     * @return The media control view, or null if none.
     */
    public View getMediaControlView() {
        return mCustomControlView;
    }

    /**
     * Sets whether to enable the volume slider and volume control using the volume keys
     * when the route supports it.
     * <p>
     * The default value is true.
     * </p>
     */
    public void setVolumeControlEnabled(boolean enable) {
        if (mVolumeControlEnabled != enable) {
            mVolumeControlEnabled = enable;
            if (mCreated) {
                update(false);
            }
        }
    }

    /**
     * Returns whether to enable the volume slider and volume control using the volume keys
     * when the route supports it.
     */
    public boolean isVolumeControlEnabled() {
        return mVolumeControlEnabled;
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
        mState = mMediaController == null ? null : mMediaController.getPlaybackState();
        updateArtIconIfNeeded();
        update(false);
    }

    /**
     * Gets the session to use for metadata and transport controls.
     *
     * @return The token for the session to use or null if none.
     */
    public MediaSessionCompat.Token getMediaSession() {
        return mMediaController == null ? null : mMediaController.getSessionToken();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        setContentView(R.layout.mr_controller_material_dialog_b);

        // Remove the neutral button.
        findViewById(BUTTON_NEUTRAL_RES_ID).setVisibility(View.GONE);

        ClickListener listener = new ClickListener();

        mExpandableAreaLayout = findViewById(R.id.mr_expandable_area);
        mExpandableAreaLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        mDialogAreaLayout = findViewById(R.id.mr_dialog_area);
        mDialogAreaLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Eat unhandled touch events.
            }
        });
        int color = MediaRouterThemeHelper.getButtonTextColor(mContext);
        mDisconnectButton = findViewById(BUTTON_DISCONNECT_RES_ID);
        mDisconnectButton.setText(R.string.mr_controller_disconnect);
        mDisconnectButton.setTextColor(color);
        mDisconnectButton.setOnClickListener(listener);

        mStopCastingButton = findViewById(BUTTON_STOP_RES_ID);
        mStopCastingButton.setText(R.string.mr_controller_stop_casting);
        mStopCastingButton.setTextColor(color);
        mStopCastingButton.setOnClickListener(listener);

        mRouteNameTextView = findViewById(R.id.mr_name);
        mCloseButton = findViewById(R.id.mr_close);
        mCloseButton.setOnClickListener(listener);
        mCustomControlLayout = findViewById(R.id.mr_custom_control);
        mDefaultControlLayout = findViewById(R.id.mr_default_control);

        // Start the session activity when a content item (album art, title or subtitle) is clicked.
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaController != null) {
                    PendingIntent pi = mMediaController.getSessionActivity();
                    if (pi != null) {
                        try {
                            pi.send();
                            dismiss();
                        } catch (PendingIntent.CanceledException e) {
                            Log.e(TAG, pi + " was not sent, it had been canceled.");
                        }
                    }
                }
            }
        };
        mArtView = findViewById(R.id.mr_art);
        mArtView.setOnClickListener(onClickListener);
        findViewById(R.id.mr_control_title_container).setOnClickListener(onClickListener);

        mMediaMainControlLayout = findViewById(R.id.mr_media_main_control);
        mDividerView = findViewById(R.id.mr_control_divider);

        mPlaybackControlLayout = findViewById(R.id.mr_playback_control);
        mTitleView = findViewById(R.id.mr_control_title);
        mSubtitleView = findViewById(R.id.mr_control_subtitle);
        mPlaybackControlButton = findViewById(R.id.mr_control_playback_ctrl);
        mPlaybackControlButton.setOnClickListener(listener);

        mVolumeControlLayout = findViewById(R.id.mr_volume_control);
        mVolumeControlLayout.setVisibility(View.GONE);
        mVolumeSlider = findViewById(R.id.mr_volume_slider);
        mVolumeSlider.setTag(mRoute);
        mVolumeChangeListener = new VolumeChangeListener();
        mVolumeSlider.setOnSeekBarChangeListener(mVolumeChangeListener);

        mVolumeGroupList = findViewById(R.id.mr_volume_group_list);
        mGroupMemberRoutes = new ArrayList<MediaRouter.RouteInfo>();
        mVolumeGroupAdapter = new VolumeGroupAdapter(mVolumeGroupList.getContext(),
                mGroupMemberRoutes);
        mVolumeGroupList.setAdapter(mVolumeGroupAdapter);
        mGroupMemberRoutesAnimatingWithBitmap = new HashSet<>();

        MediaRouterThemeHelper.setMediaControlsBackgroundColor(mContext,
                mMediaMainControlLayout, mVolumeGroupList, getGroup() != null);
        MediaRouterThemeHelper.setVolumeSliderColor(mContext,
                (MediaRouteVolumeSlider) mVolumeSlider, mMediaMainControlLayout);
        mVolumeSliderMap = new HashMap<>();
        mVolumeSliderMap.put(mRoute, mVolumeSlider);

        mGroupExpandCollapseButton =
                findViewById(R.id.mr_group_expand_collapse);
        mGroupExpandCollapseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsGroupExpanded = !mIsGroupExpanded;
                if (mIsGroupExpanded) {
                    mVolumeGroupList.setVisibility(View.VISIBLE);
                }
                loadInterpolator();
                updateLayoutHeight(true);
            }
        });
        loadInterpolator();
        mGroupListAnimationDurationMs = mContext.getResources().getInteger(
                R.integer.mr_controller_volume_group_list_animation_duration_ms);
        mGroupListFadeInDurationMs = mContext.getResources().getInteger(
                R.integer.mr_controller_volume_group_list_fade_in_duration_ms);
        mGroupListFadeOutDurationMs = mContext.getResources().getInteger(
                R.integer.mr_controller_volume_group_list_fade_out_duration_ms);

        mCustomControlView = onCreateMediaControlView(savedInstanceState);
        if (mCustomControlView != null) {
            mCustomControlLayout.addView(mCustomControlView);
            mCustomControlLayout.setVisibility(View.VISIBLE);
        }
        mCreated = true;
        updateLayout();
    }

    /**
     * Sets the width of the dialog. Also called when configuration changes.
     */
    void updateLayout() {
        int width = MediaRouteDialogHelper.getDialogWidth(mContext);
        getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);

        View decorView = getWindow().getDecorView();
        mDialogContentWidth = width - decorView.getPaddingLeft() - decorView.getPaddingRight();

        Resources res = mContext.getResources();
        mVolumeGroupListItemIconSize = res.getDimensionPixelSize(
                R.dimen.mr_controller_volume_group_list_item_icon_size);
        mVolumeGroupListItemHeight = res.getDimensionPixelSize(
                R.dimen.mr_controller_volume_group_list_item_height);
        mVolumeGroupListMaxHeight = res.getDimensionPixelSize(
                R.dimen.mr_controller_volume_group_list_max_height);

        // Fetch art icons again for layout changes to resize it accordingly
        mArtIconBitmap = null;
        mArtIconUri = null;
        updateArtIconIfNeeded();
        update(false);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachedToWindow = true;

        mRouter.addCallback(MediaRouteSelector.EMPTY, mCallback,
                MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS);
        setMediaSession(mRouter.getMediaSessionToken());
    }

    @Override
    public void onDetachedFromWindow() {
        mRouter.removeCallback(mCallback);
        setMediaSession(null);
        mAttachedToWindow = false;
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            mRoute.requestUpdateVolume(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ? -1 : 1);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    void update(boolean animate) {
        // Defer dialog updates if a user is adjusting a volume in the list
        if (mRouteInVolumeSliderTouched != null) {
            mHasPendingUpdate = true;
            mPendingUpdateAnimationNeeded |= animate;
            return;
        }
        mHasPendingUpdate = false;
        mPendingUpdateAnimationNeeded = false;
        if (!mRoute.isSelected() || mRoute.isDefaultOrBluetooth()) {
            dismiss();
            return;
        }
        if (!mCreated) {
            return;
        }

        mRouteNameTextView.setText(mRoute.getName());
        mDisconnectButton.setVisibility(mRoute.canDisconnect() ? View.VISIBLE : View.GONE);
        if (mCustomControlView == null && mArtIconIsLoaded) {
            if (isBitmapRecycled(mArtIconLoadedBitmap)) {
                Log.w(TAG, "Can't set artwork image with recycled bitmap: " + mArtIconLoadedBitmap);
            } else {
                mArtView.setImageBitmap(mArtIconLoadedBitmap);
                mArtView.setBackgroundColor(mArtIconBackgroundColor);
            }
            clearLoadedBitmap();
        }
        updateVolumeControlLayout();
        updatePlaybackControlLayout();
        updateLayoutHeight(animate);
    }

    private boolean isBitmapRecycled(Bitmap bitmap) {
        return bitmap != null && bitmap.isRecycled();
    }

    private boolean canShowPlaybackControlLayout() {
        return mCustomControlView == null && (mDescription != null || mState != null);
    }

    /**
     * Returns the height of main media controller which includes playback control and master
     * volume control.
     */
    private int getMainControllerHeight(boolean showPlaybackControl) {
        int height = 0;
        if (showPlaybackControl || mVolumeControlLayout.getVisibility() == View.VISIBLE) {
            height += mMediaMainControlLayout.getPaddingTop()
                    + mMediaMainControlLayout.getPaddingBottom();
            if (showPlaybackControl) {
                height +=  mPlaybackControlLayout.getMeasuredHeight();
            }
            if (mVolumeControlLayout.getVisibility() == View.VISIBLE) {
                height += mVolumeControlLayout.getMeasuredHeight();
            }
            if (showPlaybackControl && mVolumeControlLayout.getVisibility() == View.VISIBLE) {
                height += mDividerView.getMeasuredHeight();
            }
        }
        return height;
    }

    private void updateMediaControlVisibility(boolean canShowPlaybackControlLayout) {
        // TODO: Update the top and bottom padding of the control layout according to the display
        // height.
        mDividerView.setVisibility((mVolumeControlLayout.getVisibility() == View.VISIBLE
                && canShowPlaybackControlLayout) ? View.VISIBLE : View.GONE);
        mMediaMainControlLayout.setVisibility((mVolumeControlLayout.getVisibility() == View.GONE
                && !canShowPlaybackControlLayout) ? View.GONE : View.VISIBLE);
    }

    void updateLayoutHeight(final boolean animate) {
        // We need to defer the update until the first layout has occurred, as we don't yet know the
        // overall visible display size in which the window this view is attached to has been
        // positioned in.
        mDefaultControlLayout.requestLayout();
        ViewTreeObserver observer = mDefaultControlLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mDefaultControlLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                if (mIsGroupListAnimating) {
                    mIsGroupListAnimationPending = true;
                } else {
                    updateLayoutHeightInternal(animate);
                }
            }
        });
    }

    /**
     * Updates the height of views and hide artwork or metadata if space is limited.
     */
    void updateLayoutHeightInternal(boolean animate) {
        // Measure the size of widgets and get the height of main components.
        int oldHeight = getLayoutHeight(mMediaMainControlLayout);
        setLayoutHeight(mMediaMainControlLayout, ViewGroup.LayoutParams.MATCH_PARENT);
        updateMediaControlVisibility(canShowPlaybackControlLayout());
        View decorView = getWindow().getDecorView();
        decorView.measure(
                MeasureSpec.makeMeasureSpec(getWindow().getAttributes().width, MeasureSpec.EXACTLY),
                MeasureSpec.UNSPECIFIED);
        setLayoutHeight(mMediaMainControlLayout, oldHeight);
        int artViewHeight = 0;
        if (mCustomControlView == null && mArtView.getDrawable() instanceof BitmapDrawable) {
            Bitmap art = ((BitmapDrawable) mArtView.getDrawable()).getBitmap();
            if (art != null) {
                artViewHeight = getDesiredArtHeight(art.getWidth(), art.getHeight());
                mArtView.setScaleType(art.getWidth() >= art.getHeight()
                        ? ImageView.ScaleType.FIT_XY : ImageView.ScaleType.FIT_CENTER);
            }
        }
        int mainControllerHeight = getMainControllerHeight(canShowPlaybackControlLayout());
        int volumeGroupListCount = mGroupMemberRoutes.size();
        // Scale down volume group list items in landscape mode.
        int expandedGroupListHeight = getGroup() == null ? 0 :
                mVolumeGroupListItemHeight * getGroup().getRoutes().size();
        if (volumeGroupListCount > 0) {
            expandedGroupListHeight += mVolumeGroupListPaddingTop;
        }
        expandedGroupListHeight = Math.min(expandedGroupListHeight, mVolumeGroupListMaxHeight);
        int visibleGroupListHeight = mIsGroupExpanded ? expandedGroupListHeight : 0;

        int desiredControlLayoutHeight =
                Math.max(artViewHeight, visibleGroupListHeight) + mainControllerHeight;
        Rect visibleRect = new Rect();
        decorView.getWindowVisibleDisplayFrame(visibleRect);
        // Height of non-control views in decor view.
        // This includes title bar, button bar, and dialog's vertical padding which should be
        // always shown.
        int nonControlViewHeight = mDialogAreaLayout.getMeasuredHeight()
                - mDefaultControlLayout.getMeasuredHeight();
        // Maximum allowed height for controls to fit screen.
        int maximumControlViewHeight = visibleRect.height() - nonControlViewHeight;

        // Show artwork if it fits the screen.
        if (mCustomControlView == null && artViewHeight > 0
                && desiredControlLayoutHeight <= maximumControlViewHeight) {
            mArtView.setVisibility(View.VISIBLE);
            setLayoutHeight(mArtView, artViewHeight);
        } else {
            if (getLayoutHeight(mVolumeGroupList) + mMediaMainControlLayout.getMeasuredHeight()
                    >= mDefaultControlLayout.getMeasuredHeight()) {
                mArtView.setVisibility(View.GONE);
            }
            artViewHeight = 0;
            desiredControlLayoutHeight = visibleGroupListHeight + mainControllerHeight;
        }
        // Show the playback control if it fits the screen.
        if (canShowPlaybackControlLayout()
                && desiredControlLayoutHeight <= maximumControlViewHeight) {
            mPlaybackControlLayout.setVisibility(View.VISIBLE);
        } else {
            mPlaybackControlLayout.setVisibility(View.GONE);
        }
        updateMediaControlVisibility(mPlaybackControlLayout.getVisibility() == View.VISIBLE);
        mainControllerHeight = getMainControllerHeight(
                mPlaybackControlLayout.getVisibility() == View.VISIBLE);
        desiredControlLayoutHeight =
                Math.max(artViewHeight, visibleGroupListHeight) + mainControllerHeight;

        // Limit the volume group list height to fit the screen.
        if (desiredControlLayoutHeight > maximumControlViewHeight) {
            visibleGroupListHeight -= (desiredControlLayoutHeight - maximumControlViewHeight);
            desiredControlLayoutHeight = maximumControlViewHeight;
        }
        // Update the layouts with the computed heights.
        mMediaMainControlLayout.clearAnimation();
        mVolumeGroupList.clearAnimation();
        mDefaultControlLayout.clearAnimation();
        if (animate) {
            animateLayoutHeight(mMediaMainControlLayout, mainControllerHeight);
            animateLayoutHeight(mVolumeGroupList, visibleGroupListHeight);
            animateLayoutHeight(mDefaultControlLayout, desiredControlLayoutHeight);
        } else {
            setLayoutHeight(mMediaMainControlLayout, mainControllerHeight);
            setLayoutHeight(mVolumeGroupList, visibleGroupListHeight);
            setLayoutHeight(mDefaultControlLayout, desiredControlLayoutHeight);
        }
        // Maximize the window size with a transparent layout in advance for smooth animation.
        setLayoutHeight(mExpandableAreaLayout, visibleRect.height());
        rebuildVolumeGroupList(animate);
    }

    void updateVolumeGroupItemHeight(View item) {
        LinearLayout container = (LinearLayout) item.findViewById(R.id.volume_item_container);
        setLayoutHeight(container, mVolumeGroupListItemHeight);
        View icon = item.findViewById(R.id.mr_volume_item_icon);
        ViewGroup.LayoutParams lp = icon.getLayoutParams();
        lp.width = mVolumeGroupListItemIconSize;
        lp.height = mVolumeGroupListItemIconSize;
        icon.setLayoutParams(lp);
    }

    private void animateLayoutHeight(final View view, int targetHeight) {
        final int startValue = getLayoutHeight(view);
        final int endValue = targetHeight;
        Animation anim = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                int height = startValue - (int) ((startValue - endValue) * interpolatedTime);
                setLayoutHeight(view, height);
            }
        };
        anim.setDuration(mGroupListAnimationDurationMs);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            anim.setInterpolator(mInterpolator);
        }
        view.startAnimation(anim);
    }

    void loadInterpolator() {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            mInterpolator = mIsGroupExpanded ? mLinearOutSlowInInterpolator
                    : mFastOutSlowInInterpolator;
        } else {
            mInterpolator = mAccelerateDecelerateInterpolator;
        }
    }

    private void updateVolumeControlLayout() {
        if (isVolumeControlAvailable(mRoute)) {
            if (mVolumeControlLayout.getVisibility() == View.GONE) {
                mVolumeControlLayout.setVisibility(View.VISIBLE);
                mVolumeSlider.setMax(mRoute.getVolumeMax());
                mVolumeSlider.setProgress(mRoute.getVolume());
                mGroupExpandCollapseButton.setVisibility(getGroup() == null ? View.GONE
                        : View.VISIBLE);
            }
        } else {
            mVolumeControlLayout.setVisibility(View.GONE);
        }
    }

    private void rebuildVolumeGroupList(boolean animate) {
        List<MediaRouter.RouteInfo> routes = getGroup() == null ? null : getGroup().getRoutes();
        if (routes == null) {
            mGroupMemberRoutes.clear();
            mVolumeGroupAdapter.notifyDataSetChanged();
        } else if (MediaRouteDialogHelper.listUnorderedEquals(mGroupMemberRoutes, routes)) {
            mVolumeGroupAdapter.notifyDataSetChanged();
        } else {
            HashMap<MediaRouter.RouteInfo, Rect> previousRouteBoundMap = animate
                    ? MediaRouteDialogHelper.getItemBoundMap(mVolumeGroupList, mVolumeGroupAdapter)
                    : null;
            HashMap<MediaRouter.RouteInfo, BitmapDrawable> previousRouteBitmapMap = animate
                    ? MediaRouteDialogHelper.getItemBitmapMap(mContext, mVolumeGroupList,
                            mVolumeGroupAdapter) : null;
            mGroupMemberRoutesAdded =
                    MediaRouteDialogHelper.getItemsAdded(mGroupMemberRoutes, routes);
            mGroupMemberRoutesRemoved = MediaRouteDialogHelper.getItemsRemoved(mGroupMemberRoutes,
                    routes);
            mGroupMemberRoutes.addAll(0, mGroupMemberRoutesAdded);
            mGroupMemberRoutes.removeAll(mGroupMemberRoutesRemoved);
            mVolumeGroupAdapter.notifyDataSetChanged();
            if (animate && mIsGroupExpanded
                    && mGroupMemberRoutesAdded.size() + mGroupMemberRoutesRemoved.size() > 0) {
                animateGroupListItems(previousRouteBoundMap, previousRouteBitmapMap);
            } else {
                mGroupMemberRoutesAdded = null;
                mGroupMemberRoutesRemoved = null;
            }
        }
    }

    private void animateGroupListItems(final Map<MediaRouter.RouteInfo, Rect> previousRouteBoundMap,
            final Map<MediaRouter.RouteInfo, BitmapDrawable> previousRouteBitmapMap) {
        mVolumeGroupList.setEnabled(false);
        mVolumeGroupList.requestLayout();
        mIsGroupListAnimating = true;
        ViewTreeObserver observer = mVolumeGroupList.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mVolumeGroupList.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                animateGroupListItemsInternal(previousRouteBoundMap, previousRouteBitmapMap);
            }
        });
    }

    void animateGroupListItemsInternal(
            Map<MediaRouter.RouteInfo, Rect> previousRouteBoundMap,
            Map<MediaRouter.RouteInfo, BitmapDrawable> previousRouteBitmapMap) {
        if (mGroupMemberRoutesAdded == null || mGroupMemberRoutesRemoved == null) {
            return;
        }
        int groupSizeDelta = mGroupMemberRoutesAdded.size() - mGroupMemberRoutesRemoved.size();
        boolean listenerRegistered = false;
        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mVolumeGroupList.startAnimationAll();
                mVolumeGroupList.postDelayed(mGroupListFadeInAnimation,
                        mGroupListAnimationDurationMs);
            }

            @Override
            public void onAnimationEnd(Animation animation) { }

            @Override
            public void onAnimationRepeat(Animation animation) { }
        };

        // Animate visible items from previous positions to current positions except routes added
        // just before. Added routes will remain hidden until translate animation finishes.
        int first = mVolumeGroupList.getFirstVisiblePosition();
        for (int i = 0; i < mVolumeGroupList.getChildCount(); ++i) {
            View view = mVolumeGroupList.getChildAt(i);
            int position = first + i;
            MediaRouter.RouteInfo route = mVolumeGroupAdapter.getItem(position);
            Rect previousBounds = previousRouteBoundMap.get(route);
            int currentTop = view.getTop();
            int previousTop = previousBounds != null ? previousBounds.top
                    : (currentTop + mVolumeGroupListItemHeight * groupSizeDelta);
            AnimationSet animSet = new AnimationSet(true);
            if (mGroupMemberRoutesAdded != null && mGroupMemberRoutesAdded.contains(route)) {
                previousTop = currentTop;
                Animation alphaAnim = new AlphaAnimation(0.0f, 0.0f);
                alphaAnim.setDuration(mGroupListFadeInDurationMs);
                animSet.addAnimation(alphaAnim);
            }
            Animation translationAnim = new TranslateAnimation(0, 0, previousTop - currentTop, 0);
            translationAnim.setDuration(mGroupListAnimationDurationMs);
            animSet.addAnimation(translationAnim);
            animSet.setFillAfter(true);
            animSet.setFillEnabled(true);
            animSet.setInterpolator(mInterpolator);
            if (!listenerRegistered) {
                listenerRegistered = true;
                animSet.setAnimationListener(listener);
            }
            view.clearAnimation();
            view.startAnimation(animSet);
            previousRouteBoundMap.remove(route);
            previousRouteBitmapMap.remove(route);
        }

        // If a member route doesn't exist any longer, it can be either removed or moved out of the
        // ListView layout boundary. In this case, use the previously captured bitmaps for
        // animation.
        for (Map.Entry<MediaRouter.RouteInfo, BitmapDrawable> item
                : previousRouteBitmapMap.entrySet()) {
            final MediaRouter.RouteInfo route = item.getKey();
            final BitmapDrawable bitmap = item.getValue();
            final Rect bounds = previousRouteBoundMap.get(route);
            OverlayListView.OverlayObject object = null;
            if (mGroupMemberRoutesRemoved.contains(route)) {
                object = new OverlayListView.OverlayObject(bitmap, bounds).setAlphaAnimation(1.0f, 0.0f)
                        .setDuration(mGroupListFadeOutDurationMs)
                        .setInterpolator(mInterpolator);
            } else {
                int deltaY = groupSizeDelta * mVolumeGroupListItemHeight;
                object = new OverlayListView.OverlayObject(bitmap, bounds).setTranslateYAnimation(deltaY)
                        .setDuration(mGroupListAnimationDurationMs)
                        .setInterpolator(mInterpolator)
                        .setAnimationEndListener(new OverlayListView.OverlayObject.OnAnimationEndListener() {
                            @Override
                            public void onAnimationEnd() {
                                mGroupMemberRoutesAnimatingWithBitmap.remove(route);
                                mVolumeGroupAdapter.notifyDataSetChanged();
                            }
                        });
                mGroupMemberRoutesAnimatingWithBitmap.add(route);
            }
            mVolumeGroupList.addOverlayObject(object);
        }
    }

    void startGroupListFadeInAnimation() {
        clearGroupListAnimation(true);
        mVolumeGroupList.requestLayout();
        ViewTreeObserver observer = mVolumeGroupList.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mVolumeGroupList.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                startGroupListFadeInAnimationInternal();
            }
        });
    }

    void startGroupListFadeInAnimationInternal() {
        if (mGroupMemberRoutesAdded != null && mGroupMemberRoutesAdded.size() != 0) {
            fadeInAddedRoutes();
        } else {
            finishAnimation(true);
        }
    }

    void finishAnimation(boolean animate) {
        mGroupMemberRoutesAdded = null;
        mGroupMemberRoutesRemoved = null;
        mIsGroupListAnimating = false;
        if (mIsGroupListAnimationPending) {
            mIsGroupListAnimationPending = false;
            updateLayoutHeight(animate);
        }
        mVolumeGroupList.setEnabled(true);
    }

    private void fadeInAddedRoutes() {
        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                finishAnimation(true);
            }

            @Override
            public void onAnimationRepeat(Animation animation) { }
        };
        boolean listenerRegistered = false;
        int first = mVolumeGroupList.getFirstVisiblePosition();
        for (int i = 0; i < mVolumeGroupList.getChildCount(); ++i) {
            View view = mVolumeGroupList.getChildAt(i);
            int position = first + i;
            MediaRouter.RouteInfo route = mVolumeGroupAdapter.getItem(position);
            if (mGroupMemberRoutesAdded.contains(route)) {
                Animation alphaAnim = new AlphaAnimation(0.0f, 1.0f);
                alphaAnim.setDuration(mGroupListFadeInDurationMs);
                alphaAnim.setFillEnabled(true);
                alphaAnim.setFillAfter(true);
                if (!listenerRegistered) {
                    listenerRegistered = true;
                    alphaAnim.setAnimationListener(listener);
                }
                view.clearAnimation();
                view.startAnimation(alphaAnim);
            }
        }
    }

    void clearGroupListAnimation(boolean exceptAddedRoutes) {
        int first = mVolumeGroupList.getFirstVisiblePosition();
        for (int i = 0; i < mVolumeGroupList.getChildCount(); ++i) {
            View view = mVolumeGroupList.getChildAt(i);
            int position = first + i;
            MediaRouter.RouteInfo route = mVolumeGroupAdapter.getItem(position);
            if (exceptAddedRoutes && mGroupMemberRoutesAdded != null
                    && mGroupMemberRoutesAdded.contains(route)) {
                continue;
            }
            LinearLayout container = (LinearLayout) view.findViewById(R.id.volume_item_container);
            container.setVisibility(View.VISIBLE);
            AnimationSet animSet = new AnimationSet(true);
            Animation alphaAnim = new AlphaAnimation(1.0f, 1.0f);
            alphaAnim.setDuration(0);
            animSet.addAnimation(alphaAnim);
            Animation translationAnim = new TranslateAnimation(0, 0, 0, 0);
            translationAnim.setDuration(0);
            animSet.setFillAfter(true);
            animSet.setFillEnabled(true);
            view.clearAnimation();
            view.startAnimation(animSet);
        }
        mVolumeGroupList.stopAnimationAll();
        if (!exceptAddedRoutes) {
            finishAnimation(false);
        }
    }

    private void updatePlaybackControlLayout() {
        if (canShowPlaybackControlLayout()) {
            CharSequence title = mDescription == null ? null : mDescription.getTitle();
            boolean hasTitle = !TextUtils.isEmpty(title);

            CharSequence subtitle = mDescription == null ? null : mDescription.getSubtitle();
            boolean hasSubtitle = !TextUtils.isEmpty(subtitle);

            boolean showTitle = false;
            boolean showSubtitle = false;
            if (mRoute.getPresentationDisplayId()
                    != MediaRouter.RouteInfo.PRESENTATION_DISPLAY_ID_NONE) {
                // The user is currently casting screen.
                mTitleView.setText(R.string.mr_controller_casting_screen);
                showTitle = true;
            } else if (mState == null || mState.getState() == PlaybackStateCompat.STATE_NONE) {
                // Show "No media selected" as we don't yet know the playback state.
                mTitleView.setText(R.string.mr_controller_no_media_selected);
                showTitle = true;
            } else if (!hasTitle && !hasSubtitle) {
                mTitleView.setText(R.string.mr_controller_no_info_available);
                showTitle = true;
            } else {
                if (hasTitle) {
                    mTitleView.setText(title);
                    showTitle = true;
                }
                if (hasSubtitle) {
                    mSubtitleView.setText(subtitle);
                    showSubtitle = true;
                }
            }
            mTitleView.setVisibility(showTitle ? View.VISIBLE : View.GONE);
            mSubtitleView.setVisibility(showSubtitle ? View.VISIBLE : View.GONE);

            if (mState != null) {
                boolean isPlaying = mState.getState() == PlaybackStateCompat.STATE_BUFFERING
                        || mState.getState() == PlaybackStateCompat.STATE_PLAYING;
                Context playbackControlButtonContext = mPlaybackControlButton.getContext();
                boolean visible = true;
                int iconDrawableAttr = 0;
                int iconDescResId = 0;
                if (isPlaying && isPauseActionSupported()) {
                    iconDrawableAttr = R.attr.mediaRoutePauseDrawable;
                    iconDescResId = R.string.mr_controller_pause;
                } else if (isPlaying && isStopActionSupported()) {
                    iconDrawableAttr = R.attr.mediaRouteStopDrawable;
                    iconDescResId = R.string.mr_controller_stop;
                } else if (!isPlaying && isPlayActionSupported()) {
                    iconDrawableAttr = R.attr.mediaRoutePlayDrawable;
                    iconDescResId = R.string.mr_controller_play;
                } else {
                    visible = false;
                }
                mPlaybackControlButton.setVisibility(visible ? View.VISIBLE : View.GONE);
                if (visible) {
                    mPlaybackControlButton.setImageResource(
                            MediaRouterThemeHelper.getThemeResource(
                                    playbackControlButtonContext, iconDrawableAttr));
                    mPlaybackControlButton.setContentDescription(
                            playbackControlButtonContext.getResources()
                                    .getText(iconDescResId));
                }
            }
        }
    }

    private boolean isPlayActionSupported() {
        return (mState.getActions() & (ACTION_PLAY | ACTION_PLAY_PAUSE)) != 0;
    }

    private boolean isPauseActionSupported() {
        return (mState.getActions() & (ACTION_PAUSE | ACTION_PLAY_PAUSE)) != 0;
    }

    private boolean isStopActionSupported() {
        return (mState.getActions() & ACTION_STOP) != 0;
    }

    boolean isVolumeControlAvailable(MediaRouter.RouteInfo route) {
        return mVolumeControlEnabled && route.getVolumeHandling()
                == MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE;
    }

    private static int getLayoutHeight(View view) {
        return view.getLayoutParams().height;
    }

    static void setLayoutHeight(View view, int height) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.height = height;
        view.setLayoutParams(lp);
    }

    private static boolean uriEquals(Uri uri1, Uri uri2) {
        if (uri1 != null && uri1.equals(uri2)) {
            return true;
        } else if (uri1 == null && uri2 == null) {
            return true;
        }
        return false;
    }

    /**
     * Returns desired art height to fit into controller dialog.
     */
    int getDesiredArtHeight(int originalWidth, int originalHeight) {
        if (originalWidth >= originalHeight) {
            // For landscape art, fit width to dialog width.
            return (int) ((float) mDialogContentWidth * originalHeight / originalWidth + 0.5f);
        }
        // For portrait art, fit height to 16:9 ratio case's height.
        return (int) ((float) mDialogContentWidth * 9 / 16 + 0.5f);
    }

    void updateArtIconIfNeeded() {
        if (mCustomControlView != null || !isIconChanged()) {
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
        } else if (oldBitmap == null && !uriEquals(oldUri, newUri)) {
            return true;
        }
        return false;
    }

    private final class MediaRouterCallback extends MediaRouter.Callback {
        MediaRouterCallback() {
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            update(false);
        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            update(true);
        }

        @Override
        public void onRouteVolumeChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            SeekBar volumeSlider = mVolumeSliderMap.get(route);
            int volume = route.getVolume();
            if (DEBUG) {
                Log.d(TAG, "onRouteVolumeChanged(), route.getVolume:" + volume);
            }
            if (volumeSlider != null && mRouteInVolumeSliderTouched != route) {
                volumeSlider.setProgress(volume);
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
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            mState = state;
            update(false);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            mDescription = metadata == null ? null : metadata.getDescription();
            updateArtIconIfNeeded();
            update(false);
        }
    }

    private final class ClickListener implements View.OnClickListener {
        ClickListener() {
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == BUTTON_STOP_RES_ID || id == BUTTON_DISCONNECT_RES_ID) {
                if (mRoute.isSelected()) {
                    mRouter.unselect(id == BUTTON_STOP_RES_ID ?
                            MediaRouter.UNSELECT_REASON_STOPPED :
                            MediaRouter.UNSELECT_REASON_DISCONNECTED);
                }
                dismiss();
            } else if (id == R.id.mr_control_playback_ctrl) {
                if (mMediaController != null && mState != null) {
                    boolean isPlaying = mState.getState() == PlaybackStateCompat.STATE_PLAYING;
                    int actionDescResId = 0;
                    if (isPlaying && isPauseActionSupported()) {
                        mMediaController.getTransportControls().pause();
                        actionDescResId = R.string.mr_controller_pause;
                    } else if (isPlaying && isStopActionSupported()) {
                        mMediaController.getTransportControls().stop();
                        actionDescResId = R.string.mr_controller_stop;
                    } else if (!isPlaying && isPlayActionSupported()){
                        mMediaController.getTransportControls().play();
                        actionDescResId = R.string.mr_controller_play;
                    }
                    // Announce the action for accessibility.
                    if (mAccessibilityManager != null && mAccessibilityManager.isEnabled()
                            && actionDescResId != 0) {
                        AccessibilityEvent event = AccessibilityEvent.obtain(
                                AccessibilityEventCompat.TYPE_ANNOUNCEMENT);
                        event.setPackageName(mContext.getPackageName());
                        event.setClassName(getClass().getName());
                        event.getText().add(mContext.getString(actionDescResId));
                        mAccessibilityManager.sendAccessibilityEvent(event);
                    }
                }
            } else if (id == R.id.mr_close) {
                dismiss();
            }
        }
    }

    private class VolumeChangeListener implements SeekBar.OnSeekBarChangeListener {
        private final Runnable mStopTrackingTouch = new Runnable() {
            @Override
            public void run() {
                if (mRouteInVolumeSliderTouched != null) {
                    mRouteInVolumeSliderTouched = null;
                    if (mHasPendingUpdate) {
                        update(mPendingUpdateAnimationNeeded);
                    }
                }
            }
        };

        VolumeChangeListener() {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            if (mRouteInVolumeSliderTouched != null) {
                mVolumeSlider.removeCallbacks(mStopTrackingTouch);
            }
            mRouteInVolumeSliderTouched = (MediaRouter.RouteInfo) seekBar.getTag();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // Defer resetting mVolumeSliderTouched to allow the media route provider
            // a little time to settle into its new state and publish the final
            // volume update.
            mVolumeSlider.postDelayed(mStopTrackingTouch, VOLUME_UPDATE_DELAY_MILLIS);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                MediaRouter.RouteInfo route = (MediaRouter.RouteInfo) seekBar.getTag();
                if (DEBUG) {
                    Log.d(TAG, "onProgressChanged(): calling "
                            + "MediaRouter.RouteInfo.requestSetVolume(" + progress + ")");
                }
                route.requestSetVolume(progress);
            }
        }
    }

    private class VolumeGroupAdapter extends ArrayAdapter<MediaRouter.RouteInfo> {
        final float mDisabledAlpha;

        public VolumeGroupAdapter(Context context, List<MediaRouter.RouteInfo> objects) {
            super(context, 0, objects);
            mDisabledAlpha = MediaRouterThemeHelper.getDisabledAlpha(context);
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.mr_controller_volume_item, parent, false);
            } else {
                updateVolumeGroupItemHeight(v);
            }

            MediaRouter.RouteInfo route = getItem(position);
            if (route != null) {
                boolean isEnabled = route.isEnabled();

                TextView routeName = (TextView) v.findViewById(R.id.mr_name);
                routeName.setEnabled(isEnabled);
                routeName.setText(route.getName());

                MediaRouteVolumeSlider volumeSlider =
                        (MediaRouteVolumeSlider) v.findViewById(R.id.mr_volume_slider);
                MediaRouterThemeHelper.setVolumeSliderColor(
                        parent.getContext(), volumeSlider, mVolumeGroupList);
                volumeSlider.setTag(route);
                mVolumeSliderMap.put(route, volumeSlider);
                volumeSlider.setHideThumb(!isEnabled);
                volumeSlider.setEnabled(isEnabled);
                if (isEnabled) {
                    if (isVolumeControlAvailable(route)) {
                        volumeSlider.setMax(route.getVolumeMax());
                        volumeSlider.setProgress(route.getVolume());
                        volumeSlider.setOnSeekBarChangeListener(mVolumeChangeListener);
                    } else {
                        volumeSlider.setMax(100);
                        volumeSlider.setProgress(100);
                        volumeSlider.setEnabled(false);
                    }
                }

                ImageView volumeItemIcon =
                        (ImageView) v.findViewById(R.id.mr_volume_item_icon);
                volumeItemIcon.setAlpha(isEnabled ? 0xFF : (int) (0xFF * mDisabledAlpha));

                // If overlay bitmap exists, real view should remain hidden until
                // the animation ends.
                LinearLayout container = (LinearLayout) v.findViewById(R.id.volume_item_container);
                container.setVisibility(mGroupMemberRoutesAnimatingWithBitmap.contains(route)
                        ? View.INVISIBLE : View.VISIBLE);

                // Routes which are being added will be invisible until animation ends.
                if (mGroupMemberRoutesAdded != null && mGroupMemberRoutesAdded.contains(route)) {
                    Animation alphaAnim = new AlphaAnimation(0.0f, 0.0f);
                    alphaAnim.setDuration(0);
                    alphaAnim.setFillEnabled(true);
                    alphaAnim.setFillAfter(true);
                    v.clearAnimation();
                    v.startAnimation(alphaAnim);
                }
            }
            return v;
        }
    }

    private class FetchArtTask extends AsyncTask<Void, Void, Bitmap> {
        // Show animation only when fetching takes a long time.
        private static final long SHOW_ANIM_TIME_THRESHOLD_MILLIS = 120L;

        private final Bitmap mIconBitmap;
        private final Uri mIconUri;
        private int mBackgroundColor;
        private long mStartTimeMillis;

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
            mStartTimeMillis = SystemClock.uptimeMillis();
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
                } catch (IOException e){
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
                long elapsedTimeMillis = SystemClock.uptimeMillis() - mStartTimeMillis;
                // Loaded bitmap will be applied on the next update
                update(elapsedTimeMillis > SHOW_ANIM_TIME_THRESHOLD_MILLIS);
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
                conn.setConnectTimeout(CONNECTION_TIMEOUT_MILLIS);
                conn.setReadTimeout(CONNECTION_TIMEOUT_MILLIS);
                stream = conn.getInputStream();
            }
            return (stream == null) ? null : new BufferedInputStream(stream);
        }
    }
}
