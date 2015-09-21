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

import static android.widget.SeekBar.OnSeekBarChangeListener;

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
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.mediarouter.R;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.List;

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
    private static final String TAG = "MediaRouteControllerDialog";

    // Time to wait before updating the volume when the user lets go of the seek bar
    // to allow the route provider time to propagate the change and publish a new
    // route descriptor.
    private static final int VOLUME_UPDATE_DELAY_MILLIS = 250;
    private static final int VOLUME_SLIDER_TAG_MASTER = 0;
    private static final int VOLUME_SLIDER_TAG_BASE = 100;

    private static final int BUTTON_NEUTRAL_RES_ID = android.R.id.button3;
    private static final int BUTTON_DISCONNECT_RES_ID = android.R.id.button2;
    private static final int BUTTON_STOP_RES_ID = android.R.id.button1;

    private final MediaRouter mRouter;
    private final MediaRouterCallback mCallback;
    private final MediaRouter.RouteInfo mRoute;

    private boolean mCreated;
    private boolean mAttachedToWindow;

    private int mDialogContentWidth;

    private View mCustomControlView;

    private Button mDisconnectButton;
    private Button mStopCastingButton;
    private ImageButton mPlayPauseButton;
    private ImageButton mCloseButton;
    private MediaRouteExpandCollapseButton mGroupExpandCollapseButton;

    private FrameLayout mCustomControlLayout;
    private FrameLayout mDefaultControlLayout;
    private ImageView mArtView;
    private TextView mTitleView;
    private TextView mSubtitleView;
    private TextView mRouteNameTextView;

    private boolean mVolumeControlEnabled = true;
    // Layout for media controllers including play/pause button and the main volume slider.
    private LinearLayout mMediaMainControlLayout;
    private RelativeLayout mPlaybackControl;
    private LinearLayout mVolumeControl;
    private View mDividerView;

    private ListView mVolumeGroupList;
    private SeekBar mVolumeSlider;
    private VolumeChangeListener mVolumeChangeListener;
    private boolean mVolumeSliderTouched;
    private int mVolumeGroupListItemIconSize;
    private int mVolumeGroupListItemHeight;
    private int mVolumeGroupListMaxHeight;
    private final int mVolumeGroupListPaddingTop;

    private MediaControllerCompat mMediaController;
    private MediaControllerCallback mControllerCallback;
    private PlaybackStateCompat mState;
    private MediaDescriptionCompat mDescription;

    private FetchArtTask mFetchArtTask;
    private Bitmap mArtIconBitmap;
    private Uri mArtIconUri;
    private boolean mIsGroupExpanded;
    private boolean mIsGroupListAnimationNeeded;
    private int mGroupListAnimationDurationMs;

    private final AccessibilityManager mAccessibilityManager;

    public MediaRouteControllerDialog(Context context) {
        this(context, 0);
    }

    public MediaRouteControllerDialog(Context context, int theme) {
        super(MediaRouterThemeHelper.createThemedContext(context), theme);
        context = getContext();

        mControllerCallback = new MediaControllerCallback();
        mRouter = MediaRouter.getInstance(context);
        mCallback = new MediaRouterCallback();
        mRoute = mRouter.getSelectedRoute();
        setMediaSession(mRouter.getMediaSessionToken());
        mVolumeGroupListPaddingTop = context.getResources().getDimensionPixelSize(
                R.dimen.mr_controller_volume_group_list_padding_top);
        mAccessibilityManager =
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
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
     * Provides the subclass an opportunity to create a view that will
     * be included within the body of the dialog to offer additional media controls
     * for the currently playing content.
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
                updateVolumeControl();
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
            mMediaController = new MediaControllerCompat(getContext(), sessionToken);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.mr_controller_material_dialog_b);

        // Remove the neutral button.
        findViewById(BUTTON_NEUTRAL_RES_ID).setVisibility(View.GONE);

        ClickListener listener = new ClickListener();

        mDisconnectButton = (Button) findViewById(BUTTON_DISCONNECT_RES_ID);
        mDisconnectButton.setText(R.string.mr_controller_disconnect);
        mDisconnectButton.setOnClickListener(listener);

        mStopCastingButton = (Button) findViewById(BUTTON_STOP_RES_ID);
        mStopCastingButton.setText(R.string.mr_controller_stop);
        mStopCastingButton.setOnClickListener(listener);

        TypedValue value = new TypedValue();
        if (getContext().getTheme().resolveAttribute(R.attr.colorPrimary, value, true)) {
            mDisconnectButton.setTextColor(value.data);
            mStopCastingButton.setTextColor(value.data);
        }

        mRouteNameTextView = (TextView) findViewById(R.id.mr_name);
        mCloseButton = (ImageButton) findViewById(R.id.mr_close);
        mCloseButton.setOnClickListener(listener);
        mCustomControlLayout = (FrameLayout) findViewById(R.id.mr_custom_control);
        mDefaultControlLayout = (FrameLayout) findViewById(R.id.mr_default_control);
        mArtView = (ImageView) findViewById(R.id.mr_art);

        mMediaMainControlLayout = (LinearLayout) findViewById(R.id.mr_media_main_control);
        mDividerView = findViewById(R.id.mr_control_divider);

        mPlaybackControl = (RelativeLayout) findViewById(R.id.mr_playback_control);
        mTitleView = (TextView) findViewById(R.id.mr_control_title);
        mSubtitleView = (TextView) findViewById(R.id.mr_control_subtitle);
        mPlayPauseButton = (ImageButton) findViewById(R.id.mr_control_play_pause);
        mPlayPauseButton.setOnClickListener(listener);

        mVolumeControl = (LinearLayout) findViewById(R.id.mr_volume_control);
        mVolumeSlider = (SeekBar) findViewById(R.id.mr_volume_slider);
        mVolumeSlider.setTag(VOLUME_SLIDER_TAG_MASTER);
        mVolumeChangeListener = new VolumeChangeListener();
        mVolumeSlider.setOnSeekBarChangeListener(mVolumeChangeListener);

        mVolumeGroupList = (ListView) findViewById(R.id.mr_volume_group_list);
        mGroupExpandCollapseButton =
                (MediaRouteExpandCollapseButton) findViewById(R.id.mr_group_expand_collapse);
        mGroupExpandCollapseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsGroupExpanded = !mIsGroupExpanded;
                if (mIsGroupExpanded) {
                    mVolumeGroupList.setVisibility(View.VISIBLE);
                    mVolumeGroupList.setAdapter(
                            new VolumeGroupAdapter(getContext(), getGroup().getRoutes()));
                } else {
                    // Request layout to update UI based on {@code mIsGroupExpanded}.
                    mDefaultControlLayout.requestLayout();
                }
                mIsGroupListAnimationNeeded = true;
                updateLayoutHeight();
            }
        });
        mGroupListAnimationDurationMs = getContext().getResources().getInteger(
                        R.integer.mr_controller_volume_group_list_animation_duration_ms);

        mCustomControlView = onCreateMediaControlView(savedInstanceState);
        if (mCustomControlView != null) {
            mCustomControlLayout.addView(mCustomControlView);
            mCustomControlLayout.setVisibility(View.VISIBLE);
            mArtView.setVisibility(View.GONE);
        }
        mCreated = true;
        updateLayout();
    }

    /**
     * Sets the width of the dialog. Also called when configuration changes.
     */
    void updateLayout() {
        int width = MediaRouteDialogHelper.getDialogWidth(getContext());
        getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);

        View decorView = getWindow().getDecorView();
        mDialogContentWidth = width - decorView.getPaddingLeft() - decorView.getPaddingRight();

        Resources res = getContext().getResources();
        mVolumeGroupListItemIconSize = res.getDimensionPixelSize(
                R.dimen.mr_controller_volume_group_list_item_icon_size);
        mVolumeGroupListItemHeight = res.getDimensionPixelSize(
                R.dimen.mr_controller_volume_group_list_item_height);
        mVolumeGroupListMaxHeight = res.getDimensionPixelSize(
                R.dimen.mr_controller_volume_group_list_max_height);

        // Ensure the mArtView is updated.
        mArtIconBitmap = null;
        mArtIconUri = null;
        update();
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

    private void update() {
        if (!mRoute.isSelected() || mRoute.isDefault()) {
            dismiss();
            return;
        }
        if (!mCreated) {
            return;
        }

        mRouteNameTextView.setText(mRoute.getName());
        mDisconnectButton.setVisibility(mRoute.canDisconnect() ? View.VISIBLE : View.GONE);

        if (mCustomControlView == null) {
            if (mFetchArtTask != null) {
                mFetchArtTask.cancel(true);
            }
            mFetchArtTask = new FetchArtTask();
            mFetchArtTask.execute();
        }
        updateVolumeControl();
        updatePlaybackControl();
    }

    private boolean isPlaybackControlAvailable() {
        return mCustomControlView == null && (mDescription != null || mState != null);
    }

    /**
     * Returns the height of main media controller which includes playback control and master
     * volume control.
     */
    private int getMainControllerHeight(boolean showPlaybackControl) {
        int height = 0;
        if (showPlaybackControl || mVolumeControl.getVisibility() == View.VISIBLE) {
            height += mMediaMainControlLayout.getPaddingTop()
                    + mMediaMainControlLayout.getPaddingBottom();
            if (showPlaybackControl) {
                height +=  mPlaybackControl.getMeasuredHeight();
            }
            if (mVolumeControl.getVisibility() == View.VISIBLE) {
                height += mVolumeControl.getMeasuredHeight();
            }
            if (showPlaybackControl && mVolumeControl.getVisibility() == View.VISIBLE) {
                height += mDividerView.getMeasuredHeight();
            }
        }
        return height;
    }

    private void updateMediaControlVisibility(boolean showPlaybackControl) {
        // TODO: Update the top and bottom padding of the control layout according to the display
        // height.
        mDividerView.setVisibility((mVolumeControl.getVisibility() == View.VISIBLE
                && showPlaybackControl) ? View.VISIBLE : View.GONE);
        mMediaMainControlLayout.setVisibility((mVolumeControl.getVisibility() == View.GONE
                && !showPlaybackControl) ? View.GONE : View.VISIBLE);
    }

    private void updateLayoutHeight() {
        // We need to defer the update until the first layout has occurred, as we don't yet know the
        // overall visible display size in which the window this view is attached to has been
        // positioned in.
        ViewTreeObserver observer = mDefaultControlLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mDefaultControlLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                updateLayoutHeightInternal();
            }
        });
    }

    /**
     * Updates the height of views and hide artwork or metadata if space is limited.
     */
    private void updateLayoutHeightInternal() {
        if (mCustomControlView != null) {
            return;
        }
        // Measure the size of widgets and get the height of main components.
        updateMediaControlVisibility(isPlaybackControlAvailable());
        int oldBottomMargin = getLayoutBottomMargin(mMediaMainControlLayout);
        setLayoutBottomMargin(mMediaMainControlLayout, 0);
        View decorView = getWindow().getDecorView();
        decorView.measure(
                MeasureSpec.makeMeasureSpec(getWindow().getAttributes().width, MeasureSpec.EXACTLY),
                MeasureSpec.UNSPECIFIED);
        setLayoutBottomMargin(mMediaMainControlLayout, oldBottomMargin);
        int artViewHeight = 0;
        if (mArtView.getDrawable() instanceof BitmapDrawable) {
            Bitmap art = ((BitmapDrawable) mArtView.getDrawable()).getBitmap();
            if (art != null) {
                artViewHeight = getDesiredArtHeight(art.getWidth(), art.getHeight());
                mArtView.setScaleType(art.getWidth() >= art.getHeight()
                        ? ImageView.ScaleType.FIT_XY : ImageView.ScaleType.FIT_CENTER);
            }
        }
        int mainControllerHeight = getMainControllerHeight(isPlaybackControlAvailable());
        int volumeGroupListCount = mVolumeGroupList.getAdapter() != null
                ? mVolumeGroupList.getAdapter().getCount() : 0;
        // Scale down volume group list items in landscape mode.
        for (int i = 0; i < volumeGroupListCount; i++) {
            View item = mVolumeGroupList.getChildAt(i);
            if (item != null) {
                setLayoutHeight(item, mVolumeGroupListItemHeight);
                setLayoutHeight(item.findViewById(R.id.mr_volume_item_icon),
                        mVolumeGroupListItemIconSize);
            }
        }
        int expandedGroupListHeight = mVolumeGroupListItemHeight * volumeGroupListCount;
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
        int nonControlViewHeight = decorView.getMeasuredHeight()
                - mDefaultControlLayout.getMeasuredHeight();
        // Maximum allowed height for controls to fit screen.
        int maximumControlViewHeight = visibleRect.height() - nonControlViewHeight;

        // Show artwork if it fits the screen.
        if (artViewHeight > 0 && desiredControlLayoutHeight <= maximumControlViewHeight) {
            mArtView.setVisibility(View.VISIBLE);
            setLayoutHeight(mArtView, artViewHeight);
        } else {
            artViewHeight = 0;
            desiredControlLayoutHeight = visibleGroupListHeight + mainControllerHeight;
        }
        // Show control if it fits the screen
        if (isPlaybackControlAvailable()
                && desiredControlLayoutHeight <= maximumControlViewHeight) {
            mPlaybackControl.setVisibility(View.VISIBLE);
        } else {
            mPlaybackControl.setVisibility(View.GONE);
        }
        updateMediaControlVisibility(mPlaybackControl.getVisibility() == View.VISIBLE);
        mainControllerHeight = getMainControllerHeight(
                mPlaybackControl.getVisibility() == View.VISIBLE);
        desiredControlLayoutHeight =
                Math.max(artViewHeight, visibleGroupListHeight) + mainControllerHeight;

        // Limit the volume group list height to fit the screen.
        if (desiredControlLayoutHeight > maximumControlViewHeight) {
            visibleGroupListHeight -= (desiredControlLayoutHeight - maximumControlViewHeight);
            desiredControlLayoutHeight = maximumControlViewHeight;
        }
        setLayoutHeight(mDefaultControlLayout, desiredControlLayoutHeight);

        // Animate the main control position if needed.
        if (mVolumeGroupList.getVisibility() == View.VISIBLE
                && mArtView.getVisibility() == View.VISIBLE && mIsGroupListAnimationNeeded) {
            setLayoutHeight(mVolumeGroupList, mIsGroupExpanded ? expandedGroupListHeight
                    : Math.min(mArtView.getHeight(), getLayoutHeight(mVolumeGroupList)));
            updateMainControlBottomMargin(visibleGroupListHeight, mainControllerHeight,
                    true /* animation */);
        } else {
            // Rely on AlertDialog's animation if there is no art work.
            // TODO: Add group list animation even when there is no art work.
            setLayoutHeight(mVolumeGroupList, visibleGroupListHeight);
            updateMainControlBottomMargin(visibleGroupListHeight, mainControllerHeight,
                    false /* animation */);
            if (artViewHeight == 0) {
                mArtView.setVisibility(View.GONE);
            }
            if (!mIsGroupExpanded) {
                mVolumeGroupList.setVisibility(View.GONE);
            }
        }
        mIsGroupListAnimationNeeded = false;
    }

    private void updateMainControlBottomMargin(final int bottomMargin,
            final int mainControllerHeight, boolean animation) {
        final boolean isExpanding = bottomMargin != 0;
        if (!animation) {
            setLayoutBottomMargin(mMediaMainControlLayout, bottomMargin);
            View frontView = isExpanding ? mVolumeGroupList : mArtView;
            frontView.bringToFront();
            ((View) frontView.getParent()).invalidate();
        } else {
            Animation existingAnim = mMediaMainControlLayout.getAnimation();
            boolean animationInProgress = existingAnim != null && !existingAnim.hasEnded();
            if (animationInProgress) {
                mMediaMainControlLayout.clearAnimation();
            }
            final int volumeGroupListHeight = getLayoutHeight(mVolumeGroupList);
            int rightBelowArtWork = getLayoutHeight(mDefaultControlLayout)
                    - mArtView.getHeight() - mainControllerHeight;
            final int startValue = animationInProgress
                    ? getLayoutBottomMargin(mMediaMainControlLayout)
                    : isExpanding ? rightBelowArtWork : volumeGroupListHeight;
            final int endValue = bottomMargin;
            Animation anim = new Animation() {
                private boolean mReordered;

                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    int margin = startValue - (int) ((startValue - endValue) * interpolatedTime);
                    setLayoutBottomMargin(mMediaMainControlLayout, margin);
                    // Since there could be an overlapping area of the artwork and volume group list
                    // , z-order of the art work and volume group list should be exchanged when the
                    // main control covers the overlapping area.
                    if (!mReordered) {
                        if (isExpanding) {
                            if (margin + mainControllerHeight >= volumeGroupListHeight) {
                                mVolumeGroupList.bringToFront();
                                ((View) mVolumeGroupList.getParent()).invalidate();
                                mReordered = true;
                            }
                        } else {
                            if (volumeGroupListHeight >= margin + mainControllerHeight) {
                                mArtView.bringToFront();
                                ((View) mArtView.getParent()).invalidate();
                                mReordered = true;
                            }
                        }
                    }
                }
            };
            anim.setDuration(mGroupListAnimationDurationMs);
            mMediaMainControlLayout.startAnimation(anim);
        }
    }

    private void updateVolumeControl() {
        if (!mVolumeSliderTouched) {
            if (isVolumeControlAvailable(mRoute)) {
                mVolumeControl.setVisibility(View.VISIBLE);
                mVolumeSlider.setMax(mRoute.getVolumeMax());
                mVolumeSlider.setProgress(mRoute.getVolume());
                if (getGroup() == null) {
                    mGroupExpandCollapseButton.setVisibility(View.GONE);
                } else {
                    mGroupExpandCollapseButton.setVisibility(View.VISIBLE);
                    VolumeGroupAdapter adapter =
                            (VolumeGroupAdapter) mVolumeGroupList.getAdapter();
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                }
            } else {
                mVolumeControl.setVisibility(View.GONE);
            }
            updateLayoutHeight();
        } else if (mVolumeControl.getVisibility() == View.VISIBLE) {
            mVolumeSlider.setProgress(mRoute.getVolume());
            if (mIsGroupExpanded) {
                for (int i = 0; i < mVolumeGroupList.getChildCount(); ++i) {
                    MediaRouter.RouteInfo route = getGroup().getRouteAt(i);
                    if (isVolumeControlAvailable(route)) {
                        SeekBar volumeSlider = (SeekBar) mVolumeGroupList.getChildAt(i)
                                .findViewById(R.id.mr_volume_slider);
                        volumeSlider.setProgress(route.getVolume());
                    }
                }
            }
        }
    }

    private void updatePlaybackControl() {
        if (isPlaybackControlAvailable()) {
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
                boolean supportsPlay = (mState.getActions() & (PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE)) != 0;
                boolean supportsPause = (mState.getActions() & (PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE)) != 0;
                if (isPlaying && supportsPause) {
                    mPlayPauseButton.setVisibility(View.VISIBLE);
                    mPlayPauseButton.setImageResource(MediaRouterThemeHelper.getThemeResource(
                            getContext(), R.attr.mediaRoutePauseDrawable));
                    mPlayPauseButton.setContentDescription(getContext().getResources()
                            .getText(R.string.mr_controller_pause));
                } else if (!isPlaying && supportsPlay) {
                    mPlayPauseButton.setVisibility(View.VISIBLE);
                    mPlayPauseButton.setImageResource(MediaRouterThemeHelper.getThemeResource(
                            getContext(), R.attr.mediaRoutePlayDrawable));
                    mPlayPauseButton.setContentDescription(getContext().getResources()
                            .getText(R.string.mr_controller_play));
                } else {
                    mPlayPauseButton.setVisibility(View.GONE);
                }
            }
        }
        updateLayoutHeight();
    }

    private boolean isVolumeControlAvailable(MediaRouter.RouteInfo route) {
        return mVolumeControlEnabled && route.getVolumeHandling()
                == MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE;
    }

    private static int getLayoutHeight(View view) {
        return view.getLayoutParams().height;
    }

    private static void setLayoutHeight(View view, int height) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.height = height;
        view.setLayoutParams(lp);
    }

    private static int getLayoutBottomMargin(View view) {
        return ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).bottomMargin;
    }

    private static void setLayoutBottomMargin(View view, int bottomMargin) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        params.bottomMargin = bottomMargin;
        view.setLayoutParams(params);
    }

    /**
     * Returns desired art height to fit into controller dialog.
     */
    private int getDesiredArtHeight(int originalWidth, int originalHeight) {
        if (originalWidth >= originalHeight) {
            // For landscape art, fit width to dialog width.
            return (int) ((float) mDialogContentWidth * originalHeight / originalWidth + 0.5f);
        }
        // For portrait art, fit height to 16:9 ratio case's height.
        return (int) ((float) mDialogContentWidth * 9 / 16 + 0.5f);
    }

    private final class MediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            update();
        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            update();
        }

        @Override
        public void onRouteVolumeChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            if (route == mRoute) {
                updateVolumeControl();
            }
        }
    }

    private final class MediaControllerCallback extends MediaControllerCompat.Callback {
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
            update();
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            mDescription = metadata == null ? null : metadata.getDescription();
            update();
        }
    }

    private final class ClickListener implements View.OnClickListener {
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
            } else if (id == R.id.mr_control_play_pause) {
                if (mMediaController != null && mState != null) {
                    boolean isPlaying = mState.getState() == PlaybackStateCompat.STATE_PLAYING;
                    if (isPlaying) {
                        mMediaController.getTransportControls().pause();
                    } else {
                        mMediaController.getTransportControls().play();
                    }
                    // Announce the action for accessibility.
                    if (mAccessibilityManager != null && mAccessibilityManager.isEnabled()) {
                        AccessibilityEvent event = AccessibilityEvent.obtain(
                                AccessibilityEventCompat.TYPE_ANNOUNCEMENT);
                        event.setPackageName(getContext().getPackageName());
                        event.setClassName(getClass().getName());
                        int resId = isPlaying ?
                                R.string.mr_controller_pause : R.string.mr_controller_play;
                        event.getText().add(getContext().getString(resId));
                        mAccessibilityManager.sendAccessibilityEvent(event);
                    }
                }
            } else if (id == R.id.mr_close) {
                dismiss();
            }
        }
    }

    private class VolumeChangeListener implements OnSeekBarChangeListener {
        private final Runnable mStopTrackingTouch = new Runnable() {
            @Override
            public void run() {
                if (mVolumeSliderTouched) {
                    mVolumeSliderTouched = false;
                    updateVolumeControl();
                }
            }
        };

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            if (mVolumeSliderTouched) {
                mVolumeSlider.removeCallbacks(mStopTrackingTouch);
            } else {
                mVolumeSliderTouched = true;
            }
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
                int tag = (int) seekBar.getTag();
                if (tag == VOLUME_SLIDER_TAG_MASTER) {
                    mRoute.requestSetVolume(progress);
                } else if (tag - VOLUME_SLIDER_TAG_BASE >= 0
                        && tag - VOLUME_SLIDER_TAG_BASE < getGroup().getRouteCount()) {
                    getGroup().getRouteAt(tag - VOLUME_SLIDER_TAG_BASE).requestSetVolume(progress);
                }
            }
        }
    }

    private class VolumeGroupAdapter extends ArrayAdapter<MediaRouter.RouteInfo> {
        final static float DISABLED_ALPHA = .3f;

        public VolumeGroupAdapter(Context context, List<MediaRouter.RouteInfo> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = LayoutInflater.from(getContext()).inflate(
                        R.layout.mr_controller_volume_item, parent, false);
            }

            MediaRouter.RouteInfo route = getItem(position);
            if (route != null) {
                boolean isEnabled = route.isEnabled();

                TextView routeName = (TextView) v.findViewById(R.id.mr_name);
                routeName.setEnabled(isEnabled);
                routeName.setText(route.getName());

                MediaRouteVolumeSlider volumeSlider =
                        (MediaRouteVolumeSlider) v.findViewById(R.id.mr_volume_slider);
                volumeSlider.setTag(VOLUME_SLIDER_TAG_BASE + position);
                volumeSlider.setShowThumb(isEnabled);
                if (isEnabled) {
                    if (isVolumeControlAvailable(route)) {
                        volumeSlider.setMax(route.getVolumeMax());
                        volumeSlider.setProgress(route.getVolume());
                        volumeSlider.setOnSeekBarChangeListener(mVolumeChangeListener);
                        volumeSlider.setEnabled(true);
                    } else {
                        volumeSlider.setMax(100);
                        volumeSlider.setProgress(100);
                        volumeSlider.setEnabled(false);
                    }
                }

                ImageView volumeItemIcon =
                        (ImageView) v.findViewById(R.id.mr_volume_item_icon);
                volumeItemIcon.setAlpha(isEnabled ? 255 : (int) (255 * DISABLED_ALPHA));
            }
            return v;
        }
    }

    private class FetchArtTask extends AsyncTask<Void, Void, Bitmap> {
        final Bitmap mIconBitmap;
        final Uri mIconUri;
        int mBackgroundColor;

        FetchArtTask() {
            mIconBitmap = mDescription == null ? null : mDescription.getIconBitmap();
            mIconUri = mDescription == null ? null : mDescription.getIconUri();
        }

        @Override
        protected void onPreExecute() {
            if (mArtIconBitmap == mIconBitmap && mArtIconUri == mIconUri) {
                // Already handled the current art.
                cancel(true);
            }
        }

        @Override
        protected Bitmap doInBackground(Void... arg) {
            Bitmap art = null;
            if (mIconBitmap != null) {
                art = mIconBitmap;
            } else if (mIconUri != null) {
                String scheme = mIconUri.getScheme();
                if (!(ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme)
                        || ContentResolver.SCHEME_CONTENT.equals(scheme)
                        || ContentResolver.SCHEME_FILE.equals(scheme))) {
                    Log.w(TAG, "Icon Uri should point to local resources.");
                    return null;
                }
                BufferedInputStream stream = null;
                try {
                    stream = new BufferedInputStream(
                            getContext().getContentResolver().openInputStream(mIconUri));

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
                        stream = new BufferedInputStream(getContext().getContentResolver()
                                .openInputStream(mIconUri));
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
            if (art != null && art.getWidth() < art.getHeight()) {
                // Portrait art requires dominant color as background color.
                Palette palette = new Palette.Builder(art).maximumColorCount(1).generate();
                mBackgroundColor = palette.getSwatches().isEmpty()
                        ? 0 : palette.getSwatches().get(0).getRgb();
            }
            return art;
        }

        @Override
        protected void onCancelled() {
            mFetchArtTask = null;
        }

        @Override
        protected void onPostExecute(Bitmap art) {
            mFetchArtTask = null;
            if (mArtIconBitmap != mIconBitmap || mArtIconUri != mIconUri) {
                mArtIconBitmap = mIconBitmap;
                mArtIconUri = mIconUri;

                mArtView.setImageBitmap(art);
                mArtView.setBackgroundColor(mBackgroundColor);
                updateLayoutHeight();
            }
        }
    }
}
