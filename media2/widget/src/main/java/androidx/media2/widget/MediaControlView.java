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

package androidx.media2.widget;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.SessionPlayer.TrackInfo;
import androidx.media2.common.UriMediaItem;
import androidx.media2.common.VideoSize;
import androidx.media2.session.MediaController;
import androidx.media2.session.MediaSession;
import androidx.media2.session.SessionCommand;
import androidx.media2.session.SessionCommandGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

/**
 * A View that contains the controls for {@link MediaController} or {@link SessionPlayer}.
 * It provides a wide range of buttons that serve the following functions: play/pause,
 * rewind/fast-forward, skip to next/previous, select subtitle track, enter/exit full screen mode,
 * select audio track, and adjust playback speed.
 * <p>
 * For simple use cases not requiring communication with {@link MediaSession}, apps need to create
 * a {@link SessionPlayer} (e.g. {@link androidx.media2.player.MediaPlayer}) and set it to this view
 * by calling {@link #setPlayer}.
 * For more advanced use cases that require {@link MediaSession} (e.g. handling media key events,
 * integrating with other MediaSession apps as Assistant), apps need to create
 * a {@link MediaController} attached to the {@link MediaSession} and set it to this view
 * by calling {@link #setMediaController}.
 * <p>
 * The easiest way to use a MediaControlView is by creating a {@link VideoView}, which will
 * internally create a MediaControlView instance and handle all the commands from buttons inside
 * MediaControlView. It is also possible to create a MediaControlView programmatically and add it
 * to a custom video view. For more information, refer to {@link VideoView}.
 *
 * By default, the buttons inside MediaControlView will not visible unless the corresponding
 * {@link SessionCommand} is marked as allowed. For more details, refer to {@link MediaSession}.
 * <p>
 * Currently, MediaControlView animates off-screen in two steps:
 *   1) Title and bottom bars slide up and down respectively and the transport controls fade out,
 *      leaving only the progress bar at the bottom of the view.
 *   2) Progress bar slides down off-screen.
 * <p>
 * In addition, the following customizations are supported:
 * 1) Set focus to the play/pause button by calling {@link #requestPlayButtonFocus()}.
 * 2) Set full screen behavior by calling {@link #setOnFullScreenListener(OnFullScreenListener)}
 * <p>
 * <em> Displaying metadata : </em>
 * MediaControlView supports displaying metadata by calling
 * {@link MediaItem#setMetadata(MediaMetadata)}.
 *
 * Metadata display is different for two different media types: music, and non-music.
 * For music, the following metadata are supported:
 * {@link MediaMetadata#METADATA_KEY_TITLE}, {@link MediaMetadata#METADATA_KEY_ARTIST},
 * and {@link MediaMetadata#METADATA_KEY_ALBUM_ART}.
 * If values for these keys are not set, the following default values will be shown, respectively:
 * {@link androidx.media2.widget.R.string#mcv2_music_title_unknown_text}
 * {@link androidx.media2.widget.R.string#mcv2_music_artist_unknown_text}
 * {@link androidx.media2.widget.R.drawable#ic_default_album_image}
 *
 * For non-music, only {@link MediaMetadata#METADATA_KEY_TITLE} metadata is supported.
 * If the value is not set, the following default value will be shown:
 * {@link androidx.media2.widget.R.string#mcv2_non_music_title_unknown_text}
 */
public class MediaControlView extends ViewGroup {
    private static final String TAG = "MediaControlView";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final int SETTINGS_MODE_AUDIO_TRACK = 0;
    private static final int SETTINGS_MODE_PLAYBACK_SPEED = 1;
    private static final int SETTINGS_MODE_SUBTITLE_TRACK = 2;
    private static final int SETTINGS_MODE_MAIN = 3;
    private static final int PLAYBACK_SPEED_1x_INDEX = 3;

    private static final int SIZE_TYPE_UNDEFINED = -1;
    private static final int SIZE_TYPE_EMBEDDED = 0;
    private static final int SIZE_TYPE_FULL = 1;
    private static final int SIZE_TYPE_MINIMAL = 2;

    // Int for defining the UX state where all the views (TitleBar, ProgressBar, BottomBar) are
    // all visible.
    private static final int UX_STATE_ALL_VISIBLE = 0;
    // Int for defining the UX state where only the ProgressBar view is visible.
    private static final int UX_STATE_ONLY_PROGRESS_VISIBLE = 1;
    // Int for defining the UX state where none of the views are visible.
    private static final int UX_STATE_NONE_VISIBLE = 2;
    // Int for defining the UX state where the views are being animated (shown or hidden).
    private static final int UX_STATE_ANIMATING = 3;

    private static final long DISABLE_DELAYED_ANIMATION = -1;
    private static final long DEFAULT_DELAYED_ANIMATION_INTERVAL_MS = 2000;
    private static final long DEFAULT_PROGRESS_UPDATE_TIME_MS = 1000;
    private static final long REWIND_TIME_MS = 10000;
    private static final long FORWARD_TIME_MS = 30000;
    private static final long AD_SKIP_WAIT_TIME_MS = 5000;
    private static final long HIDE_TIME_MS = 250;
    private static final long SHOW_TIME_MS = 250;
    private static final int MAX_PROGRESS = 1000;
    private static final int MAX_SCALE_LEVEL = 10000;
    private static final int RESOURCE_NON_EXISTENT = -1;
    private static final int SEEK_POSITION_NOT_SET = -1;
    private static final String RESOURCE_EMPTY = "";

    Resources mResources;
    PlayerWrapper mPlayer;
    OnFullScreenListener mOnFullScreenListener;
    private AccessibilityManager mAccessibilityManager;
    private int mEmbeddedSettingsItemWidth;
    private int mFullSettingsItemWidth;
    private int mSettingsItemHeight;
    private int mSettingsWindowMargin;
    int mSettingsMode;
    int mSelectedSubtitleTrackIndex;
    int mSelectedAudioTrackIndex;
    int mSelectedSpeedIndex;
    int mSizeType = SIZE_TYPE_UNDEFINED;
    int mUxState;
    long mDuration;
    long mDelayedAnimationIntervalMs;
    long mCurrentSeekPosition;
    long mNextSeekPosition;
    boolean mDragging;
    boolean mIsFullScreen;
    boolean mIsShowingReplayButton;
    boolean mOverflowIsShowing;
    boolean mSeekAvailable;
    boolean mIsAdvertisement;
    boolean mNeedToHideBars;
    boolean mNeedToShowBars;
    boolean mWasPlaying;

    private SparseArray<View> mTransportControlsMap = new SparseArray<>();

    // Relating to Title Bar View
    private View mTitleBar;
    private TextView mTitleView;
    private View mAdExternalLink;

    // Relating to Center View
    ViewGroup mCenterView;
    private View mCenterViewBackground;
    private View mEmbeddedTransportControls;
    private View mMinimalTransportControls;

    // Relating to Minimal Fullscreen View
    ViewGroup mMinimalFullScreenView;
    ImageButton mMinimalFullScreenButton;

    // Relating to Progress Bar View
    private ViewGroup mProgressBar;
    SeekBar mProgress;

    // Relating to Bottom Bar View
    private View mBottomBarBackground;

    // Relating to Bottom Bar Left View
    private ViewGroup mBottomBarLeft;
    private View mFullTransportControls;
    private ViewGroup mTimeView;
    private TextView mEndTime;
    TextView mCurrentTime;
    private TextView mAdSkipView;
    private StringBuilder mFormatBuilder;
    private Formatter mFormatter;

    // Relating to Bottom Bar Right View
    ViewGroup mBasicControls;
    ViewGroup mExtraControls;
    ImageButton mSubtitleButton;
    ImageButton mFullScreenButton;
    private TextView mAdRemainingView;

    // Relating to Settings List View
    private ListView mSettingsListView;
    private PopupWindow mSettingsWindow;
    SettingsAdapter mSettingsAdapter;
    SubSettingsAdapter mSubSettingsAdapter;
    private List<String> mSettingsMainTextsList;
    List<String> mSettingsSubTextsList;
    private List<Integer> mSettingsIconIdsList;
    List<String> mSubtitleDescriptionsList;
    int mVideoTrackCount;
    List<TrackInfo> mAudioTracks = new ArrayList<>();
    List<TrackInfo> mSubtitleTracks = new ArrayList<>();
    List<String> mAudioTrackDescriptionList;
    List<String> mPlaybackSpeedTextList;
    List<Integer> mPlaybackSpeedMultBy100List;
    int mCustomPlaybackSpeedIndex;

    AnimatorSet mHideMainBarsAnimator;
    AnimatorSet mHideProgressBarAnimator;
    AnimatorSet mHideAllBarsAnimator;
    AnimatorSet mShowMainBarsAnimator;
    AnimatorSet mShowAllBarsAnimator;
    ValueAnimator mOverflowShowAnimator;
    ValueAnimator mOverflowHideAnimator;

    public MediaControlView(@NonNull Context context) {
        this(context, null);
    }

    public MediaControlView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MediaControlView(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mResources = context.getResources();
        inflate(context, R.layout.media_controller, this);
        initControllerView();
        mDelayedAnimationIntervalMs = DEFAULT_DELAYED_ANIMATION_INTERVAL_MS;
        mAccessibilityManager = (AccessibilityManager) context.getSystemService(
                Context.ACCESSIBILITY_SERVICE);
    }

    /**
     * Sets {@link MediaController} to control playback with this view.
     * Setting a MediaController will unset any MediaController or SessionPlayer
     * that was previously set.
     * <p>
     * Note that MediaControlView allows controlling playback through its UI components, but calling
     * the corresponding methods (e.g. {@link MediaController#play()},
     * {@link MediaController#pause()}) will work as well.
     *
     * @param controller the controller
     * @see #setPlayer
     */
    public void setMediaController(@NonNull MediaController controller) {
        if (controller == null) {
            throw new NullPointerException("controller must not be null");
        }
        if (mPlayer != null) {
            mPlayer.detachCallback();
        }
        mPlayer = new PlayerWrapper(controller, ContextCompat.getMainExecutor(getContext()),
                new PlayerCallback());
        if (isAttachedToWindow()) {
            mPlayer.attachCallback();
        }
    }

    /**
     * Sets {@link SessionPlayer} to control playback with this view.
     * Setting a SessionPlayer will unset any MediaController or SessionPlayer
     * that was previously set.
     * <p>
     * Note that MediaControlView allows controlling playback through its UI components, but calling
     * the corresponding methods (e.g. {@link SessionPlayer#play()}, {@link SessionPlayer#pause()})
     * will work as well.
     *
     * @param player the player
     * @see #setMediaController
     */
    public void setPlayer(@NonNull SessionPlayer player) {
        if (player == null) {
            throw new NullPointerException("player must not be null");
        }
        if (mPlayer != null) {
            mPlayer.detachCallback();
        }
        mPlayer = new PlayerWrapper(player, ContextCompat.getMainExecutor(getContext()),
                new PlayerCallback());
        if (isAttachedToWindow()) {
            mPlayer.attachCallback();
        }
    }

    /**
     * Sets a listener to be called when the fullscreen mode should be changed.
     * A non-null listener needs to be set in order to display the fullscreen button.
     *
     * @param listener The listener to be called. A value of <code>null</code> removes any
     * existing listener and hides the fullscreen button.
     */
    public void setOnFullScreenListener(@Nullable OnFullScreenListener listener) {
        if (listener == null) {
            mOnFullScreenListener = null;
            mFullScreenButton.setVisibility(View.GONE);
        } else {
            mOnFullScreenListener = listener;
            mFullScreenButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     *  Requests focus for the play/pause button.
     */
    public void requestPlayButtonFocus() {
        ImageButton button = findControlButton(mSizeType, R.id.pause);
        if (button != null) {
            button.requestFocus();
        }
    }

    /**
     * Interface definition of a callback to be invoked to inform the fullscreen mode is changed.
     * Application should handle the fullscreen mode accordingly.
     */
    public interface OnFullScreenListener {
        /**
         * Called to indicate a fullscreen mode change.
         */
        void onFullScreen(@NonNull View view, boolean fullScreen);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        // Class name may be obfuscated by Proguard. Hardcode the string for accessibility usage.
        return "androidx.media2.widget.MediaControlView";
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mPlayer == null) {
            return super.onTouchEvent(ev);
        }
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            if (!isCurrentItemMusic() || mSizeType != SIZE_TYPE_FULL) {
                if (mUxState == UX_STATE_ALL_VISIBLE) {
                    hideMediaControlView();
                } else {
                    showMediaControlView();
                }
            }
        }
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        if (mPlayer == null) {
            return super.onTrackballEvent(ev);
        }
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            if (!isCurrentItemMusic() || mSizeType != SIZE_TYPE_FULL) {
                if (mUxState == UX_STATE_ALL_VISIBLE) {
                    hideMediaControlView();
                } else {
                    showMediaControlView();
                }
            }
        }
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        int childWidth = width - getPaddingLeft() - getPaddingRight();
        int childHeight = height - getPaddingTop() - getPaddingBottom();
        int childState = 0;

        if (childWidth < 0) {
            childWidth = 0;
            childState |= View.MEASURED_STATE_TOO_SMALL;
        }
        if (childHeight < 0) {
            childHeight = 0;
            childState |= (View.MEASURED_STATE_TOO_SMALL >> View.MEASURED_HEIGHT_STATE_SHIFT);
        }

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }
            LayoutParams lp = child.getLayoutParams();

            int childWidthSpec;
            if (lp.width == LayoutParams.MATCH_PARENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY);
            } else if (lp.width == LayoutParams.WRAP_CONTENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.UNSPECIFIED);
            } else {
                childWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
            }

            int childHeightSpec;
            if (lp.height == LayoutParams.MATCH_PARENT) {
                childHeightSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY);
            } else if (lp.height == LayoutParams.WRAP_CONTENT) {
                childHeightSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.UNSPECIFIED);
            } else {
                childHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
            }

            child.measure(childWidthSpec, childHeightSpec);
            childState |= child.getMeasuredState();
        }

        setMeasuredDimension(
                resolveSizeAndState(width, widthMeasureSpec, childState),
                resolveSizeAndState(height, heightMeasureSpec,
                        childState << View.MEASURED_HEIGHT_STATE_SHIFT));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int width = right - left - getPaddingLeft() - getPaddingRight();
        final int height = bottom - top - getPaddingTop() - getPaddingBottom();

        final int fullWidth = mBottomBarLeft.getMeasuredWidth()
                + mTimeView.getMeasuredWidth()
                + mBasicControls.getMeasuredWidth();
        final int fullHeight = mTitleBar.getMeasuredHeight()
                + mProgressBar.getMeasuredHeight()
                + mBottomBarBackground.getMeasuredHeight();

        final int embeddedWidth = mTimeView.getMeasuredWidth()
                + mBasicControls.getMeasuredWidth();
        final int embeddedHeight = mTitleBar.getMeasuredHeight()
                + mEmbeddedTransportControls.getMeasuredHeight()
                + mProgressBar.getMeasuredHeight()
                + mBottomBarBackground.getMeasuredHeight();

        int sizeType;
        if (mIsAdvertisement || (fullWidth <= width && fullHeight <= height)) {
            sizeType = SIZE_TYPE_FULL;
        } else if (embeddedWidth <= width && embeddedHeight <= height) {
            sizeType = SIZE_TYPE_EMBEDDED;
        } else {
            sizeType = SIZE_TYPE_MINIMAL;
        }

        if (mSizeType != sizeType) {
            mSizeType = sizeType;
            updateLayoutForSizeChange(sizeType);
        }

        mTitleBar.setVisibility(
                sizeType != SIZE_TYPE_MINIMAL ? View.VISIBLE : View.INVISIBLE);
        mCenterViewBackground.setVisibility(
                sizeType != SIZE_TYPE_FULL ? View.VISIBLE : View.INVISIBLE);
        mEmbeddedTransportControls.setVisibility(
                sizeType == SIZE_TYPE_EMBEDDED ? View.VISIBLE : View.INVISIBLE);
        mMinimalTransportControls.setVisibility(
                sizeType == SIZE_TYPE_MINIMAL ? View.VISIBLE : View.INVISIBLE);
        mBottomBarBackground.setVisibility(
                sizeType != SIZE_TYPE_MINIMAL ? View.VISIBLE : View.INVISIBLE);
        mBottomBarLeft.setVisibility(
                sizeType == SIZE_TYPE_FULL ? View.VISIBLE : View.INVISIBLE);
        mTimeView.setVisibility(
                sizeType != SIZE_TYPE_MINIMAL ? View.VISIBLE : View.INVISIBLE);
        mBasicControls.setVisibility(
                sizeType != SIZE_TYPE_MINIMAL ? View.VISIBLE : View.INVISIBLE);
        mMinimalFullScreenButton.setVisibility(
                sizeType == SIZE_TYPE_MINIMAL ? View.VISIBLE : View.INVISIBLE);

        final int childLeft = getPaddingLeft();
        final int childRight = childLeft + width;
        final int childTop = getPaddingTop();
        final int childBottom = childTop + height;

        layoutChild(mTitleBar,
                childLeft,
                childTop);
        layoutChild(mCenterView,
                childLeft,
                childTop);
        layoutChild(mBottomBarBackground,
                childLeft,
                childBottom - mBottomBarBackground.getMeasuredHeight());
        layoutChild(mBottomBarLeft,
                childLeft,
                childBottom - mBottomBarLeft.getMeasuredHeight());
        layoutChild(mTimeView,
                sizeType == SIZE_TYPE_FULL
                        ? childRight - mBasicControls.getMeasuredWidth()
                                - mTimeView.getMeasuredWidth()
                        : childLeft,
                childBottom - mTimeView.getMeasuredHeight());
        layoutChild(mBasicControls,
                childRight - mBasicControls.getMeasuredWidth(),
                childBottom - mBasicControls.getMeasuredHeight());
        layoutChild(mExtraControls,
                childRight,
                childBottom - mExtraControls.getMeasuredHeight());
        layoutChild(mProgressBar,
                childLeft,
                sizeType == SIZE_TYPE_MINIMAL
                        ? childBottom - mProgressBar.getMeasuredHeight()
                        : childBottom - mProgressBar.getMeasuredHeight()
                                - mResources.getDimensionPixelSize(
                                        R.dimen.mcv2_custom_progress_margin_bottom));
        layoutChild(mMinimalFullScreenView,
                childLeft,
                childBottom - mMinimalFullScreenView.getMeasuredHeight());
    }

    private void layoutChild(View child, int left, int top) {
        child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
    }

    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);

        if (mPlayer == null) return;
        if (isVisible) {
            removeCallbacks(mUpdateProgress);
            post(mUpdateProgress);
        } else {
            removeCallbacks(mUpdateProgress);
        }
    }

    void setDelayedAnimationInterval(long interval) {
        mDelayedAnimationIntervalMs = interval;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mPlayer != null) {
            mPlayer.attachCallback();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mPlayer != null) {
            mPlayer.detachCallback();
        }
    }

    ///////////////////////////////////////////////////
    // Protected or private methods
    ///////////////////////////////////////////////////

    static View inflateLayout(Context context, int resId) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(resId, null);
    }

    private void initControllerView() {
        // Relating to Title Bar View
        mTitleBar = findViewById(R.id.title_bar);
        mTitleView = findViewById(R.id.title_text);
        mAdExternalLink = findViewById(R.id.ad_external_link);

        // Relating to Center View
        mCenterView = findViewById(R.id.center_view);
        mCenterViewBackground = findViewById(R.id.center_view_background);
        mEmbeddedTransportControls = initTransportControls(R.id.embedded_transport_controls);
        mMinimalTransportControls = initTransportControls(R.id.minimal_transport_controls);

        // Relating to Minimal Size FullScreen View
        mMinimalFullScreenView = findViewById(R.id.minimal_fullscreen_view);
        mMinimalFullScreenButton = findViewById(R.id.minimal_fullscreen);
        mMinimalFullScreenButton.setOnClickListener(mFullScreenListener);

        // Relating to Progress Bar View
        mProgressBar = findViewById(R.id.progress_bar);
        mProgress = findViewById(R.id.progress);
        mProgress.setOnSeekBarChangeListener(mSeekListener);
        mProgress.setMax(MAX_PROGRESS);
        mCurrentSeekPosition = SEEK_POSITION_NOT_SET;
        mNextSeekPosition = SEEK_POSITION_NOT_SET;

        // Relating to Bottom Bar View
        mBottomBarBackground = findViewById(R.id.bottom_bar_background);

        // Relating to Bottom Bar Left View
        mBottomBarLeft = findViewById(R.id.bottom_bar_left);
        mFullTransportControls = initTransportControls(R.id.full_transport_controls);
        mTimeView = findViewById(R.id.time);
        mEndTime = findViewById(R.id.time_end);
        mCurrentTime = findViewById(R.id.time_current);
        mAdSkipView = findViewById(R.id.ad_skip_time);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

        // Relating to Bottom Bar Right View
        mBasicControls = findViewById(R.id.basic_controls);
        mExtraControls = findViewById(R.id.extra_controls);
        mSubtitleButton = findViewById(R.id.subtitle);
        mSubtitleButton.setOnClickListener(mSubtitleListener);
        mFullScreenButton = findViewById(R.id.fullscreen);
        mFullScreenButton.setOnClickListener(mFullScreenListener);
        ImageButton overflowShowButton = findViewById(R.id.overflow_show);
        overflowShowButton.setOnClickListener(mOverflowShowListener);
        ImageButton overflowHideButton = findViewById(R.id.overflow_hide);
        overflowHideButton.setOnClickListener(mOverflowHideListener);
        ImageButton settingsButton = findViewById(R.id.settings);
        settingsButton.setOnClickListener(mSettingsButtonListener);
        mAdRemainingView = findViewById(R.id.ad_remaining);

        // Relating to Settings List View
        initializeSettingsLists();
        mSettingsListView = (ListView) inflateLayout(getContext(), R.layout.settings_list);
        mSettingsAdapter = new SettingsAdapter(mSettingsMainTextsList, mSettingsSubTextsList,
                mSettingsIconIdsList);
        mSubSettingsAdapter = new SubSettingsAdapter(null, 0);
        mSettingsListView.setAdapter(mSettingsAdapter);
        mSettingsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mSettingsListView.setOnItemClickListener(mSettingsItemClickListener);

        // TransportControlsMap
        mTransportControlsMap.append(SIZE_TYPE_EMBEDDED, mEmbeddedTransportControls);
        mTransportControlsMap.append(SIZE_TYPE_FULL, mFullTransportControls);
        mTransportControlsMap.append(SIZE_TYPE_MINIMAL, mMinimalTransportControls);

        mEmbeddedSettingsItemWidth = mResources.getDimensionPixelSize(
                R.dimen.mcv2_embedded_settings_width);
        mFullSettingsItemWidth = mResources.getDimensionPixelSize(R.dimen.mcv2_full_settings_width);
        mSettingsItemHeight = mResources.getDimensionPixelSize(
                R.dimen.mcv2_settings_height);
        mSettingsWindowMargin = (-1) * mResources.getDimensionPixelSize(
                R.dimen.mcv2_settings_offset);
        mSettingsWindow = new PopupWindow(mSettingsListView, mEmbeddedSettingsItemWidth,
                LayoutParams.WRAP_CONTENT, true);
        mSettingsWindow.setBackgroundDrawable(new ColorDrawable());
        mSettingsWindow.setOnDismissListener(mSettingsDismissListener);

        float titleBarHeight = mResources.getDimension(R.dimen.mcv2_title_bar_height);
        float progressBarHeight = mResources.getDimension(R.dimen.mcv2_custom_progress_thumb_size);
        float bottomBarHeight = mResources.getDimension(R.dimen.mcv2_bottom_bar_height);

        View[] bottomBarGroup = { mBottomBarBackground, mBottomBarLeft, mTimeView, mBasicControls,
                mExtraControls, mProgressBar };

        ValueAnimator fadeOutAnimator = ValueAnimator.ofFloat(1.0f, 0.0f);
        fadeOutAnimator.setInterpolator(new LinearInterpolator());
        fadeOutAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float alpha = (float) animation.getAnimatedValue();
                int scaleLevel = mSizeType == SIZE_TYPE_MINIMAL ? 0 : MAX_SCALE_LEVEL;
                mProgress.getThumb().setLevel((int) (scaleLevel * alpha));

                mCenterView.setAlpha(alpha);
                mMinimalFullScreenView.setAlpha(alpha);
            }
        });
        fadeOutAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCenterView.setVisibility(View.INVISIBLE);
                mMinimalFullScreenView.setVisibility(View.INVISIBLE);
            }
        });

        ValueAnimator fadeInAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        fadeInAnimator.setInterpolator(new LinearInterpolator());
        fadeInAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float alpha = (float) animation.getAnimatedValue();
                int scaleLevel = mSizeType == SIZE_TYPE_MINIMAL ? 0 : MAX_SCALE_LEVEL;
                mProgress.getThumb().setLevel((int) (scaleLevel * alpha));

                mCenterView.setAlpha(alpha);
                mMinimalFullScreenView.setAlpha(alpha);
            }
        });
        fadeInAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mCenterView.setVisibility(View.VISIBLE);
                mMinimalFullScreenView.setVisibility(View.VISIBLE);
            }
        });

        mHideMainBarsAnimator = new AnimatorSet();
        mHideMainBarsAnimator.play(fadeOutAnimator)
                .with(AnimatorUtil.ofTranslationY(0, -titleBarHeight, mTitleBar))
                .with(AnimatorUtil.ofTranslationYTogether(0, bottomBarHeight, bottomBarGroup));
        mHideMainBarsAnimator.setDuration(HIDE_TIME_MS);
        mHideMainBarsAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mUxState = UX_STATE_ANIMATING;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mUxState = UX_STATE_ONLY_PROGRESS_VISIBLE;
                if (mNeedToShowBars) {
                    post(mShowAllBars);
                    mNeedToShowBars = false;
                }
            }
        });

        mHideProgressBarAnimator = AnimatorUtil.ofTranslationYTogether(
                bottomBarHeight, bottomBarHeight + progressBarHeight, bottomBarGroup);
        mHideProgressBarAnimator.setDuration(HIDE_TIME_MS);
        mHideProgressBarAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mUxState = UX_STATE_ANIMATING;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mUxState = UX_STATE_NONE_VISIBLE;
                if (mNeedToShowBars) {
                    post(mShowAllBars);
                    mNeedToShowBars = false;
                }
            }
        });

        mHideAllBarsAnimator = new AnimatorSet();
        mHideAllBarsAnimator.play(fadeOutAnimator)
                .with(AnimatorUtil.ofTranslationY(0, -titleBarHeight, mTitleBar))
                .with(AnimatorUtil.ofTranslationYTogether(
                        0, bottomBarHeight + progressBarHeight, bottomBarGroup));
        mHideAllBarsAnimator.setDuration(HIDE_TIME_MS);
        mHideAllBarsAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mUxState = UX_STATE_ANIMATING;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mUxState = UX_STATE_NONE_VISIBLE;
                if (mNeedToShowBars) {
                    post(mShowAllBars);
                    mNeedToShowBars = false;
                }
            }
        });

        mShowMainBarsAnimator = new AnimatorSet();
        mShowMainBarsAnimator.play(fadeInAnimator)
                .with(AnimatorUtil.ofTranslationY(-titleBarHeight, 0, mTitleBar))
                .with(AnimatorUtil.ofTranslationYTogether(bottomBarHeight, 0, bottomBarGroup));
        mShowMainBarsAnimator.setDuration(SHOW_TIME_MS);
        mShowMainBarsAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mUxState = UX_STATE_ANIMATING;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mUxState = UX_STATE_ALL_VISIBLE;
            }
        });

        mShowAllBarsAnimator = new AnimatorSet();
        mShowAllBarsAnimator.play(fadeInAnimator)
                .with(AnimatorUtil.ofTranslationY(-titleBarHeight, 0, mTitleBar))
                .with(AnimatorUtil.ofTranslationYTogether(
                        bottomBarHeight + progressBarHeight, 0, bottomBarGroup));
        mShowAllBarsAnimator.setDuration(SHOW_TIME_MS);
        mShowAllBarsAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mUxState = UX_STATE_ANIMATING;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mUxState = UX_STATE_ALL_VISIBLE;
            }
        });

        mOverflowShowAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        mOverflowShowAnimator.setDuration(SHOW_TIME_MS);
        mOverflowShowAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                animateOverflow((float) animation.getAnimatedValue());
            }
        });
        mOverflowShowAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mExtraControls.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mBasicControls.setVisibility(View.INVISIBLE);

                findControlButton(SIZE_TYPE_FULL, R.id.ffwd).setVisibility(
                        mPlayer != null && mPlayer.canSeekForward() ? View.INVISIBLE : View.GONE);
            }
        });

        mOverflowHideAnimator = ValueAnimator.ofFloat(1.0f, 0.0f);
        mOverflowHideAnimator.setDuration(SHOW_TIME_MS);
        mOverflowHideAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                animateOverflow((float) animation.getAnimatedValue());
            }
        });
        mOverflowHideAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mBasicControls.setVisibility(View.VISIBLE);

                findControlButton(SIZE_TYPE_FULL, R.id.ffwd).setVisibility(
                        mPlayer != null && mPlayer.canSeekForward() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mExtraControls.setVisibility(View.GONE);
            }
        });
    }

    final Runnable mUpdateProgress = new Runnable() {
        @Override
        public void run() {
            boolean isShowing = getVisibility() == View.VISIBLE;
            if (!mDragging && isShowing && mPlayer != null && mPlayer.isPlaying()) {
                long pos = setProgress();
                postDelayedRunnable(mUpdateProgress,
                        DEFAULT_PROGRESS_UPDATE_TIME_MS - (pos % DEFAULT_PROGRESS_UPDATE_TIME_MS));
            }
        }
    };

    String stringForTime(long timeMs) {
        long totalSeconds = timeMs / 1000;

        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    long setProgress() {
        ensurePlayerIsNotNull();

        int positionOnProgressBar = 0;
        long currentPosition = mPlayer.getCurrentPosition();
        if (currentPosition > mDuration) {
            currentPosition = mDuration;
        }
        if (mDuration > 0) {
            positionOnProgressBar = (int) (MAX_PROGRESS * currentPosition / mDuration);
        }
        if (mProgress != null && currentPosition != mDuration) {
            mProgress.setProgress(positionOnProgressBar);
            // If the media is a local file, there is no need to set a buffer, so set secondary
            // progress to maximum.
            if (mPlayer.getBufferPercentage() < 0) {
                mProgress.setSecondaryProgress(MAX_PROGRESS);
            } else {
                mProgress.setSecondaryProgress((int) mPlayer.getBufferPercentage() * 10);
            }
        }

        if (mEndTime != null) {
            mEndTime.setText(stringForTime(mDuration));
        }
        if (mCurrentTime != null) {
            mCurrentTime.setText(stringForTime(currentPosition));
        }

        if (mIsAdvertisement) {
            // Update the remaining number of seconds until the first 5 seconds of the
            // advertisement.
            if (mAdSkipView != null) {
                if (currentPosition <= AD_SKIP_WAIT_TIME_MS) {
                    if (mAdSkipView.getVisibility() == View.GONE) {
                        mAdSkipView.setVisibility(View.VISIBLE);
                    }
                    String skipTimeText = mResources.getString(
                            R.string.MediaControlView_ad_skip_wait_time,
                            ((AD_SKIP_WAIT_TIME_MS - currentPosition) / 1000 + 1));
                    mAdSkipView.setText(skipTimeText);
                } else {
                    if (mAdSkipView.getVisibility() == View.VISIBLE) {
                        mAdSkipView.setVisibility(View.GONE);
                        findControlButton(SIZE_TYPE_FULL, R.id.next).setEnabled(true);
                        findControlButton(SIZE_TYPE_FULL, R.id.next).clearColorFilter();
                    }
                }
            }
            // Update the remaining number of seconds of the advertisement.
            if (mAdRemainingView != null) {
                long remainingTime =
                        (mDuration - currentPosition < 0) ? 0 : (mDuration - currentPosition);
                String remainingTimeText = mResources.getString(
                        R.string.MediaControlView_ad_remaining_time,
                        stringForTime(remainingTime));
                mAdRemainingView.setText(remainingTimeText);
            }
        }
        return currentPosition;
    }

    void togglePausePlayState() {
        ensurePlayerIsNotNull();

        ImageButton playPauseButton = findControlButton(mSizeType, R.id.pause);
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
            playPauseButton.setImageDrawable(
                    mResources.getDrawable(R.drawable.ic_play_circle_filled));
            playPauseButton.setContentDescription(
                    mResources.getString(R.string.mcv2_play_button_desc));
        } else {
            if (mIsShowingReplayButton) {
                mPlayer.seekTo(0);
            }
            mPlayer.play();
            playPauseButton.setImageDrawable(
                    mResources.getDrawable(R.drawable.ic_pause_circle_filled));
            playPauseButton.setContentDescription(
                    mResources.getString(R.string.mcv2_pause_button_desc));
        }
    }

    private void showMediaControlView() {
        if (mUxState == UX_STATE_ANIMATING) {
            return;
        }
        removeCallbacks(mHideMainBars);
        removeCallbacks(mHideProgressBar);
        post(mShowAllBars);
    }

    private void hideMediaControlView() {
        if (shouldNotHideBars() || mUxState == UX_STATE_ANIMATING) {
            return;
        }
        removeCallbacks(mHideMainBars);
        removeCallbacks(mHideProgressBar);
        post(mHideAllBars);
    }

    final Runnable mShowAllBars = new Runnable() {
        @Override
        public void run() {
            switch (mUxState) {
                case UX_STATE_NONE_VISIBLE:
                    mShowAllBarsAnimator.start();
                    break;
                case UX_STATE_ONLY_PROGRESS_VISIBLE:
                    mShowMainBarsAnimator.start();
                    break;
                case UX_STATE_ANIMATING:
                    mNeedToShowBars = true;
            }

            if (mPlayer.isPlaying()) {
                postDelayedRunnable(mHideMainBars, mDelayedAnimationIntervalMs);
            }
        }
    };

    private final Runnable mHideAllBars = new Runnable() {
        @Override
        public void run() {
            if (shouldNotHideBars()) {
                return;
            }
            mHideAllBarsAnimator.start();
        }
    };

    Runnable mHideMainBars = new Runnable() {
        @Override
        public void run() {
            if (!mPlayer.isPlaying() || shouldNotHideBars()) {
                return;
            }
            mHideMainBarsAnimator.start();
            postDelayedRunnable(mHideProgressBar, mDelayedAnimationIntervalMs);
        }
    };

    final Runnable mHideProgressBar = new Runnable() {
        @Override
        public void run() {
            if (!mPlayer.isPlaying() || shouldNotHideBars()) {
                return;
            }
            mHideProgressBarAnimator.start();
        }
    };

    // There are two scenarios that can trigger the seekbar listener to trigger:
    //
    // The first is the user using the touchpad to adjust the position of the
    // seekbar's thumb. In this case onStartTrackingTouch is called followed by
    // a number of onProgressChanged notifications, concluded by onStopTrackingTouch.
    // We're setting the field "mDragging" to true for the duration of the dragging
    // session to avoid jumps in the position in case of ongoing playback.
    //
    // The second scenario involves the user operating the scroll ball, in this
    // case there WON'T BE onStartTrackingTouch/onStopTrackingTouch notifications,
    // we will simply apply the updated position without suspending regular updates.
    private final OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        @Override
        public void onStartTrackingTouch(SeekBar bar) {
            if (mPlayer == null || !mSeekAvailable) {
                return;
            }

            mDragging = true;

            // By removing these pending progress messages we make sure
            // that a) we won't update the progress while the user adjusts
            // the seekbar and b) once the user is done dragging the thumb
            // we will post one of these messages to the queue again and
            // this ensures that there will be exactly one message queued up.
            removeCallbacks(mUpdateProgress);
            removeCallbacks(mHideMainBars);
            removeCallbacks(mHideProgressBar);

            // Check if playback is currently stopped. In this case, update the pause button to
            // show the play image instead of the replay image.
            if (mIsShowingReplayButton) {
                updateReplayButton(false);
            }

            if (isCurrentMediaItemFromNetwork() && mPlayer.isPlaying()) {
                mWasPlaying = true;
                mPlayer.pause();
            }
        }

        @Override
        public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            if (mPlayer == null || !mSeekAvailable) {
                return;
            }
            if (!fromUser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return;
            }
            // Check if progress bar is being dragged since this method may be called after
            // onStopTrackingTouch() is called.
            if (mDragging && mDuration > 0) {
                long newPosition = ((mDuration * progress) / MAX_PROGRESS);
                // Do not seek if the current media item has a http scheme URL to improve seek
                // performance.
                boolean shouldSeekNow = !isCurrentMediaItemFromNetwork();
                seekTo(newPosition, shouldSeekNow);
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar bar) {
            if (mPlayer == null || !mSeekAvailable) {
                return;
            }
            mDragging = false;

            long latestSeekPosition = getLatestSeekPosition();
            // Reset existing seek positions since we only need to seek to the latest position.
            if (isCurrentMediaItemFromNetwork()) {
                mCurrentSeekPosition = SEEK_POSITION_NOT_SET;
                mNextSeekPosition = SEEK_POSITION_NOT_SET;
            }
            seekTo(latestSeekPosition, true);

            if (mWasPlaying) {
                mWasPlaying = false;
                mPlayer.play();
            }
        }
    };

    private final OnClickListener mPlayPauseListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mPlayer == null) return;
            resetHideCallbacks();
            togglePausePlayState();
        }
    };

    private final OnClickListener mRewListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mPlayer == null) return;
            resetHideCallbacks();
            removeCallbacks(mUpdateProgress);

            // If replay button is shown, seek to 10 seconds before the end of the media.
            boolean stoppedWithDuration = mIsShowingReplayButton && mDuration != 0;
            long currentPosition = stoppedWithDuration ? mDuration : getLatestSeekPosition();
            long seekPosition = Math.max(currentPosition - REWIND_TIME_MS, 0);
            seekTo(seekPosition, /* shouldSeekNow= */ true);
            if (stoppedWithDuration) {
                updateReplayButton(/* toBeShown */ false);
            }
        }
    };

    private final OnClickListener mFfwdListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mPlayer == null) return;
            resetHideCallbacks();
            removeCallbacks(mUpdateProgress);

            long latestSeekPosition = getLatestSeekPosition();
            seekTo(Math.min(latestSeekPosition + FORWARD_TIME_MS, mDuration), true);

            // Note: In some edge cases, mDuration might be less than actual duration of
            // the stream. If controller is in playing state, it should not show replay
            // button even when the seekPosition >= mDuration.
            if (latestSeekPosition + FORWARD_TIME_MS >= mDuration && !mPlayer.isPlaying()) {
                updateReplayButton(/* toBeShown */ true);
            }
        }
    };

    private final OnClickListener mNextListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mPlayer == null) return;
            resetHideCallbacks();
            mPlayer.skipToNextItem();
        }
    };

    private final OnClickListener mPrevListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mPlayer == null) return;
            resetHideCallbacks();
            mPlayer.skipToPreviousItem();
        }
    };

    private final OnClickListener mSubtitleListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mPlayer == null) return;
            removeCallbacks(mHideMainBars);
            removeCallbacks(mHideProgressBar);

            mSettingsMode = SETTINGS_MODE_SUBTITLE_TRACK;
            mSubSettingsAdapter.setTexts(mSubtitleDescriptionsList);
            mSubSettingsAdapter.setCheckPosition(mSelectedSubtitleTrackIndex + 1);
            displaySettingsWindow(mSubSettingsAdapter);
        }
    };

    private final OnClickListener mFullScreenListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mOnFullScreenListener == null) {
                return;
            }

            final boolean isEnteringFullScreen = !mIsFullScreen;
            if (isEnteringFullScreen) {
                mFullScreenButton.setImageDrawable(
                        mResources.getDrawable(R.drawable.ic_fullscreen_exit));
                mMinimalFullScreenButton.setImageDrawable(
                        mResources.getDrawable(R.drawable.ic_fullscreen_exit));
            } else {
                mFullScreenButton.setImageDrawable(
                        mResources.getDrawable(R.drawable.ic_fullscreen));
                mMinimalFullScreenButton.setImageDrawable(
                        mResources.getDrawable(R.drawable.ic_fullscreen));
            }
            mIsFullScreen = isEnteringFullScreen;
            mOnFullScreenListener.onFullScreen(MediaControlView.this,
                    mIsFullScreen);
        }
    };

    private final OnClickListener mOverflowShowListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mPlayer == null) return;
            resetHideCallbacks();

            mOverflowIsShowing = true;
            mOverflowShowAnimator.start();
        }
    };

    private final OnClickListener mOverflowHideListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mPlayer == null) return;
            resetHideCallbacks();

            mOverflowIsShowing = false;
            mOverflowHideAnimator.start();
        }
    };

    private final OnClickListener mSettingsButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mPlayer == null) return;
            removeCallbacks(mHideMainBars);
            removeCallbacks(mHideProgressBar);

            mSettingsMode = SETTINGS_MODE_MAIN;
            mSettingsAdapter.setSubTexts(mSettingsSubTextsList);
            displaySettingsWindow(mSettingsAdapter);
        }
    };

    private final AdapterView.OnItemClickListener mSettingsItemClickListener =
            new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch (mSettingsMode) {
                case SETTINGS_MODE_MAIN:
                    if (position == SETTINGS_MODE_AUDIO_TRACK) {
                        mSubSettingsAdapter.setTexts(mAudioTrackDescriptionList);
                        mSubSettingsAdapter.setCheckPosition(mSelectedAudioTrackIndex);
                        mSettingsMode = SETTINGS_MODE_AUDIO_TRACK;
                    } else if (position == SETTINGS_MODE_PLAYBACK_SPEED) {
                        mSubSettingsAdapter.setTexts(mPlaybackSpeedTextList);
                        mSubSettingsAdapter.setCheckPosition(mSelectedSpeedIndex);
                        mSettingsMode = SETTINGS_MODE_PLAYBACK_SPEED;
                    }
                    displaySettingsWindow(mSubSettingsAdapter);
                    break;
                case SETTINGS_MODE_AUDIO_TRACK:
                    if (position != mSelectedAudioTrackIndex) {
                        if (mAudioTracks.size() > 0) {
                            mPlayer.selectTrack(mAudioTracks.get(position));
                        }
                    }
                    dismissSettingsWindow();
                    break;
                case SETTINGS_MODE_PLAYBACK_SPEED:
                    if (position != mSelectedSpeedIndex) {
                        float speed = mPlaybackSpeedMultBy100List.get(position) / 100.0f;
                        mPlayer.setPlaybackSpeed(speed);
                    }
                    dismissSettingsWindow();
                    break;
                case SETTINGS_MODE_SUBTITLE_TRACK:
                    if (position != mSelectedSubtitleTrackIndex + 1) {
                        if (position > 0) {
                            mPlayer.selectTrack(mSubtitleTracks.get(position - 1));
                        } else {
                            mPlayer.deselectTrack(mSubtitleTracks.get(mSelectedSubtitleTrackIndex));
                        }
                    }
                    dismissSettingsWindow();
                    break;
            }
        }
    };

    private PopupWindow.OnDismissListener mSettingsDismissListener =
            new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    if (mNeedToHideBars) {
                        postDelayedRunnable(mHideMainBars, mDelayedAnimationIntervalMs);
                    }
                }
            };

    void updateTimeViews(MediaItem item) {
        if (item == null) {
            mProgress.setProgress(0);
            mCurrentTime.setText(mResources.getString(R.string.MediaControlView_time_placeholder));
            mEndTime.setText(mResources.getString(R.string.MediaControlView_time_placeholder));
            return;
        }

        ensurePlayerIsNotNull();

        long duration = mPlayer.getDurationMs();
        if (duration > 0) {
            mDuration = duration;
            setProgress();
        }
    }

    void updateTitleView(MediaItem item) {
        if (item == null) {
            mTitleView.setText(null);
            return;
        }

        if (!isCurrentItemMusic()) {
            CharSequence title = mPlayer.getTitle();
            if (title == null) {
                title = mResources.getString(R.string.mcv2_non_music_title_unknown_text);
            }
            mTitleView.setText(title.toString());
        } else {
            CharSequence title = mPlayer.getTitle();
            if (title == null) {
                title = mResources.getString(R.string.mcv2_music_title_unknown_text);
            }
            CharSequence artist = mPlayer.getArtistText();
            if (artist == null) {
                artist = mResources.getString(R.string.mcv2_music_artist_unknown_text);
            }
            // Update title for Embedded size type
            mTitleView.setText(title.toString() + " - " + artist.toString());
        }
    }

    void updateLayoutForAd() {
        ensurePlayerIsNotNull();

        if (mIsAdvertisement) {
            findControlButton(SIZE_TYPE_FULL, R.id.rew).setVisibility(View.GONE);
            findControlButton(SIZE_TYPE_FULL, R.id.ffwd).setVisibility(View.GONE);
            findControlButton(SIZE_TYPE_FULL, R.id.prev).setVisibility(View.GONE);

            findControlButton(SIZE_TYPE_FULL, R.id.next).setVisibility(View.VISIBLE);
            findControlButton(SIZE_TYPE_FULL, R.id.next).setEnabled(false);
            findControlButton(SIZE_TYPE_FULL, R.id.next).setColorFilter(R.color.gray);

            mTimeView.setVisibility(View.GONE);
            mAdSkipView.setVisibility(View.VISIBLE);
            mAdRemainingView.setVisibility(View.VISIBLE);
            mAdExternalLink.setVisibility(View.VISIBLE);

            mProgress.setEnabled(false);
        } else {
            findControlButton(SIZE_TYPE_FULL, R.id.rew).setVisibility(
                    mPlayer.canSeekBackward() ? View.VISIBLE : View.GONE);
            findControlButton(SIZE_TYPE_FULL, R.id.ffwd).setVisibility(
                    mPlayer.canSeekForward() ? View.VISIBLE : View.GONE);
            findControlButton(SIZE_TYPE_FULL, R.id.prev).setVisibility(
                    mPlayer.canSkipToPrevious() ? View.VISIBLE : View.GONE);

            findControlButton(SIZE_TYPE_FULL, R.id.next).setVisibility(
                    mPlayer.canSkipToNext() ? View.VISIBLE : View.GONE);
            findControlButton(SIZE_TYPE_FULL, R.id.next).setEnabled(true);
            findControlButton(SIZE_TYPE_FULL, R.id.next).clearColorFilter();

            mTimeView.setVisibility(View.VISIBLE);
            mAdSkipView.setVisibility(View.GONE);
            mAdRemainingView.setVisibility(View.GONE);
            mAdExternalLink.setVisibility(View.GONE);

            mProgress.setEnabled(mSeekAvailable);
        }
    }

    private void updateLayoutForSizeChange(int sizeType) {
        switch (sizeType) {
            case SIZE_TYPE_FULL:
            case SIZE_TYPE_EMBEDDED:
                // Relating to Progress Bar
                mProgress.getThumb().setLevel(MAX_SCALE_LEVEL);
                break;
            case SIZE_TYPE_MINIMAL:
                // Relating to Progress Bar
                mProgress.getThumb().setLevel(0);
                break;
        }

        // Update play/pause and ffwd buttons based on whether currently the replay button is shown
        // or not.
        updateReplayButton(mIsShowingReplayButton);
    }

    private View initTransportControls(int id) {
        View v = findViewById(id);
        ImageButton playPauseButton = v.findViewById(R.id.pause);
        if (playPauseButton != null) {
            playPauseButton.setOnClickListener(mPlayPauseListener);
        }
        ImageButton ffwdButton = v.findViewById(R.id.ffwd);
        if (ffwdButton != null) {
            ffwdButton.setOnClickListener(mFfwdListener);
        }
        ImageButton rewButton = v.findViewById(R.id.rew);
        if (rewButton != null) {
            rewButton.setOnClickListener(mRewListener);
        }
        ImageButton nextButton = v.findViewById(R.id.next);
        if (nextButton != null) {
            nextButton.setOnClickListener(mNextListener);
        }
        ImageButton prevButton = v.findViewById(R.id.prev);
        if (prevButton != null) {
            prevButton.setOnClickListener(mPrevListener);
        }
        return v;
    }

    private void initializeSettingsLists() {
        mSettingsMainTextsList = new ArrayList<String>();
        mSettingsMainTextsList.add(
                mResources.getString(R.string.MediaControlView_audio_track_text));
        mSettingsMainTextsList.add(
                mResources.getString(R.string.MediaControlView_playback_speed_text));

        mSettingsSubTextsList = new ArrayList<String>();
        mSettingsSubTextsList.add(
                mResources.getString(R.string.MediaControlView_audio_track_none_text));
        String normalSpeed = mResources.getString(R.string.MediaControlView_playback_speed_normal);
        mSettingsSubTextsList.add(normalSpeed);
        mSettingsSubTextsList.add(RESOURCE_EMPTY);

        mSettingsIconIdsList = new ArrayList<Integer>();
        mSettingsIconIdsList.add(R.drawable.ic_audiotrack);
        mSettingsIconIdsList.add(R.drawable.ic_speed);

        mAudioTrackDescriptionList = new ArrayList<String>();
        mAudioTrackDescriptionList.add(
                mResources.getString(R.string.MediaControlView_audio_track_none_text));

        mPlaybackSpeedTextList = new ArrayList<String>(Arrays.asList(
                mResources.getStringArray(R.array.MediaControlView_playback_speeds)));
        // Select the normal speed (1x) as the default value.
        mPlaybackSpeedTextList.add(PLAYBACK_SPEED_1x_INDEX, normalSpeed);
        mSelectedSpeedIndex = PLAYBACK_SPEED_1x_INDEX;

        mPlaybackSpeedMultBy100List = new ArrayList<Integer>();
        int[] speeds = mResources.getIntArray(R.array.speed_multiplied_by_100);
        for (int i = 0; i < speeds.length; i++) {
            mPlaybackSpeedMultBy100List.add(speeds[i]);
        }
        mCustomPlaybackSpeedIndex = -1;
    }

    ImageButton findControlButton(int sizeType, @IdRes int id) {
        return mTransportControlsMap.get(sizeType).findViewById(id);
    }

    /**
     * @return true iff the current media item is from network.
     */
    boolean isCurrentMediaItemFromNetwork() {
        ensurePlayerIsNotNull();

        MediaItem currentMediaItem = mPlayer.getCurrentMediaItem();

        if (!(currentMediaItem instanceof UriMediaItem)) {
            return false;
        }

        Uri uri = ((UriMediaItem) currentMediaItem).getUri();
        return UriUtil.isFromNetwork(uri);
    }

    void displaySettingsWindow(BaseAdapter adapter) {
        // Set Adapter
        mSettingsListView.setAdapter(adapter);

        // Set width of window
        int itemWidth = (mSizeType == SIZE_TYPE_EMBEDDED)
                ? mEmbeddedSettingsItemWidth : mFullSettingsItemWidth;
        mSettingsWindow.setWidth(itemWidth);

        // Calculate height of window
        int maxHeight = getMeasuredHeight() + mSettingsWindowMargin * 2;
        int totalHeight = adapter.getCount() * mSettingsItemHeight;
        int height = (totalHeight < maxHeight) ? totalHeight : maxHeight;
        mSettingsWindow.setHeight(height);

        // Show window
        mNeedToHideBars = false;
        mSettingsWindow.dismiss();
        // Workaround for b/123271636.
        if (height > 0) {
            mSettingsWindow.showAsDropDown(this, mSettingsWindowMargin,
                    mSettingsWindowMargin - height, Gravity.BOTTOM | Gravity.RIGHT);
            mNeedToHideBars = true;
        }
    }

    void dismissSettingsWindow() {
        mNeedToHideBars = true;
        mSettingsWindow.dismiss();
    }

    void animateOverflow(float animatedValue) {
        int extraControlWidth = mExtraControls.getWidth();
        int extraControlTranslationX = (-1) * (int) (extraControlWidth * animatedValue);
        mExtraControls.setTranslationX(extraControlTranslationX);

        mTimeView.setAlpha(1 - animatedValue);
        mBasicControls.setAlpha(1 - animatedValue);

        int transportControlLeftWidth = findControlButton(SIZE_TYPE_FULL, R.id.pause).getLeft();
        int transportControlTranslationX = (-1) * (int) (transportControlLeftWidth * animatedValue);
        mFullTransportControls.setTranslationX(transportControlTranslationX);
        findControlButton(SIZE_TYPE_FULL, R.id.ffwd).setAlpha(1 - animatedValue);
    }

    void resetHideCallbacks() {
        removeCallbacks(mHideMainBars);
        removeCallbacks(mHideProgressBar);
        postDelayedRunnable(mHideMainBars, mDelayedAnimationIntervalMs);
    }

    void updateAllowedCommands() {
        ensurePlayerIsNotNull();

        boolean canPause = mPlayer.canPause();
        boolean canRew = mPlayer.canSeekBackward();
        boolean canFfwd = mPlayer.canSeekForward();
        boolean canPrev = mPlayer.canSkipToPrevious();
        boolean canNext = mPlayer.canSkipToNext();

        int n = mTransportControlsMap.size();
        for (int i = 0; i < n; i++) {
            int sizeType = mTransportControlsMap.keyAt(i);

            View playPauseButton = findControlButton(sizeType, R.id.pause);
            if (playPauseButton != null) {
                playPauseButton.setVisibility(canPause ? View.VISIBLE : View.GONE);
            }
            View rewButton = findControlButton(sizeType, R.id.rew);
            if (rewButton != null) {
                rewButton.setVisibility(canRew ? View.VISIBLE : View.GONE);
            }
            View ffwdButton = findControlButton(sizeType, R.id.ffwd);
            if (ffwdButton != null) {
                ffwdButton.setVisibility(canFfwd ? View.VISIBLE : View.GONE);
            }
            View prevButton = findControlButton(sizeType, R.id.prev);
            if (prevButton != null) {
                prevButton.setVisibility(canPrev ? View.VISIBLE : View.GONE);
            }
            View nextButton = findControlButton(sizeType, R.id.next);
            if (nextButton != null) {
                nextButton.setVisibility(canNext ? View.VISIBLE : View.GONE);
            }
        }
        if (mPlayer.canSeekTo()) {
            mSeekAvailable = true;
            mProgress.setEnabled(true);
        }
        if (mPlayer.canSelectDeselectTrack()) {
            mSubtitleButton.setVisibility(View.VISIBLE);
        } else {
            mSubtitleButton.setVisibility(View.GONE);
        }
    }

    boolean shouldNotHideBars() {
        return (isCurrentItemMusic() && mSizeType == SIZE_TYPE_FULL)
                || mAccessibilityManager.isTouchExplorationEnabled()
                || mPlayer.getPlayerState() == SessionPlayer.PLAYER_STATE_ERROR
                || mPlayer.getPlayerState() == SessionPlayer.PLAYER_STATE_IDLE;
    }

    void seekTo(long newPosition, boolean shouldSeekNow) {
        ensurePlayerIsNotNull();

        int positionOnProgressBar = (mDuration <= 0)
                ? 0 : (int) (MAX_PROGRESS * newPosition / mDuration);
        mProgress.setProgress(positionOnProgressBar);
        mCurrentTime.setText(stringForTime(newPosition));

        if (mCurrentSeekPosition == SEEK_POSITION_NOT_SET) {
            // If current seek position is not set, update its value and seek now if necessary.
            mCurrentSeekPosition = newPosition;

            if (shouldSeekNow) {
                mPlayer.seekTo(mCurrentSeekPosition);
            }
        } else {
            // If current seek position is already set, update the next seek position.
            mNextSeekPosition = newPosition;
        }
    }

    long getLatestSeekPosition() {
        ensurePlayerIsNotNull();

        if (mNextSeekPosition != SEEK_POSITION_NOT_SET) {
            return mNextSeekPosition;
        } else if (mCurrentSeekPosition != SEEK_POSITION_NOT_SET) {
            return mCurrentSeekPosition;
        }
        return mPlayer.getCurrentPosition();
    }

    void removeCustomSpeedFromList() {
        mPlaybackSpeedMultBy100List.remove(mCustomPlaybackSpeedIndex);
        mPlaybackSpeedTextList.remove(mCustomPlaybackSpeedIndex);
        mCustomPlaybackSpeedIndex = -1;
    }

    void updateSelectedSpeed(int selectedSpeedIndex, String selectedSpeedText) {
        mSelectedSpeedIndex = selectedSpeedIndex;
        mSettingsSubTextsList.set(SETTINGS_MODE_PLAYBACK_SPEED, selectedSpeedText);
        mSubSettingsAdapter.setTexts(mPlaybackSpeedTextList);
        mSubSettingsAdapter.setCheckPosition(mSelectedSpeedIndex);
    }

    void updateReplayButton(boolean toBeShown) {
        ImageButton playPauseButton = findControlButton(mSizeType, R.id.pause);
        ImageButton ffwdButton = findControlButton(mSizeType, R.id.ffwd);
        if (toBeShown) {
            mIsShowingReplayButton = true;
            if (playPauseButton != null) {
                playPauseButton.setImageDrawable(
                        mResources.getDrawable(R.drawable.ic_replay_circle_filled));
                playPauseButton.setContentDescription(
                        mResources.getString(R.string.mcv2_replay_button_desc));
            }
            if (ffwdButton != null) {
                ffwdButton.setAlpha(0.5f);
                ffwdButton.setEnabled(false);
            }
        } else {
            mIsShowingReplayButton = false;
            if (playPauseButton != null) {
                if (mPlayer != null && mPlayer.isPlaying()) {
                    playPauseButton.setImageDrawable(
                            mResources.getDrawable(R.drawable.ic_pause_circle_filled));
                    playPauseButton.setContentDescription(
                            mResources.getString(R.string.mcv2_pause_button_desc));
                } else {
                    playPauseButton.setImageDrawable(
                            mResources.getDrawable(R.drawable.ic_play_circle_filled));
                    playPauseButton.setContentDescription(
                            mResources.getString(R.string.mcv2_play_button_desc));
                }
            }
            if (ffwdButton != null) {
                ffwdButton.setAlpha(1.0f);
                ffwdButton.setEnabled(true);
            }
        }
    }

    void postDelayedRunnable(Runnable runnable, long interval) {
        if (interval != DISABLE_DELAYED_ANIMATION) {
            postDelayed(runnable, interval);
        }
    }

    void ensurePlayerIsNotNull() {
        if (mPlayer == null) {
            throw new IllegalStateException("mPlayer must not be null");
        }
    }

    void updateTracks(PlayerWrapper player, List<TrackInfo> trackInfos) {
        // Update video track count, audio & subtitle track lists.
        mVideoTrackCount = 0;
        mAudioTracks = new ArrayList<>();
        mSubtitleTracks = new ArrayList<>();
        mSelectedAudioTrackIndex = 0;
        // Default is -1 since subtitle selection always includes "Off" item
        mSelectedSubtitleTrackIndex = -1;
        TrackInfo audioTrack = player.getSelectedTrack(TrackInfo.MEDIA_TRACK_TYPE_AUDIO);
        TrackInfo subtitleTrack = player.getSelectedTrack(TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE);
        for (int i = 0; i < trackInfos.size(); i++) {
            int trackType = trackInfos.get(i).getTrackType();
            if (trackType == TrackInfo.MEDIA_TRACK_TYPE_VIDEO) {
                mVideoTrackCount++;
            } else if (trackType == TrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
                if (trackInfos.get(i).equals(audioTrack)) {
                    mSelectedAudioTrackIndex = mAudioTracks.size();
                }
                mAudioTracks.add(trackInfos.get(i));
            } else if (trackType == TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE) {
                if (trackInfos.get(i).equals(subtitleTrack)) {
                    mSelectedSubtitleTrackIndex = mSubtitleTracks.size();
                }
                mSubtitleTracks.add(trackInfos.get(i));
            }
        }

        // Update audio description list.
        mAudioTrackDescriptionList = new ArrayList<>();
        if (mAudioTracks.isEmpty()) {
            mAudioTrackDescriptionList.add(
                    mResources.getString(R.string.MediaControlView_audio_track_none_text));
        } else {
            for (int i = 0; i < mAudioTracks.size(); i++) {
                mAudioTrackDescriptionList.add(mResources.getString(
                        R.string.MediaControlView_audio_track_number_text, i + 1));
            }
        }

        // Update text for audio displayed inside the Settings window.
        mSettingsSubTextsList.set(SETTINGS_MODE_AUDIO_TRACK,
                mAudioTrackDescriptionList.get(mSelectedAudioTrackIndex));

        // Update subtitle description list and subtitle button visibility.
        mSubtitleDescriptionsList = new ArrayList<>();
        if (mSubtitleTracks.isEmpty()) {
            // For Audio only media item, CC button will not be shown when there's
            // no subtitle tracks.
            if (isCurrentItemMusic()) {
                mSubtitleButton.setVisibility(View.GONE);
            } else {
                mSubtitleButton.setVisibility(View.VISIBLE);
                mSubtitleButton.setAlpha(0.5f);
                mSubtitleButton.setEnabled(false);
            }
        } else {
            mSubtitleDescriptionsList.add(mResources.getString(
                    R.string.MediaControlView_subtitle_off_text));
            for (int i = 0; i < mSubtitleTracks.size(); i++) {
                String lang = mSubtitleTracks.get(i).getLanguage().getISO3Language();
                String trackDescription;
                if (lang.equals("und")) {
                    trackDescription = mResources.getString(
                            R.string.MediaControlView_subtitle_track_number_text, i + 1);
                } else {
                    trackDescription = mResources.getString(
                            R.string.MediaControlView_subtitle_track_number_and_lang_text,
                            i + 1, lang);
                }
                mSubtitleDescriptionsList.add(trackDescription);
            }
            mSubtitleButton.setVisibility(View.VISIBLE);
            mSubtitleButton.setAlpha(1.0f);
            mSubtitleButton.setEnabled(true);
        }
    }

    private boolean hasActualVideo() {
        if (mVideoTrackCount > 0) {
            return true;
        }
        VideoSize videoSize = mPlayer.getVideoSize();
        if (videoSize.getHeight() > 0 && videoSize.getWidth() > 0) {
            Log.w(TAG, "video track count is zero, but it renders video. size: " + videoSize);
            return true;
        }
        return false;
    }

    private boolean isCurrentItemMusic() {
        return !hasActualVideo() && mAudioTracks.size() > 0;
    }

    private class SettingsAdapter extends BaseAdapter {
        private List<Integer> mIconIds;
        private List<String> mMainTexts;
        private List<String> mSubTexts;

        SettingsAdapter(List<String> mainTexts, @Nullable List<String> subTexts,
                @Nullable List<Integer> iconIds) {
            mMainTexts = mainTexts;
            mSubTexts = subTexts;
            mIconIds = iconIds;
        }

        @Override
        public int getCount() {
            return (mMainTexts == null) ? 0 : mMainTexts.size();
        }

        @Override
        public long getItemId(int position) {
            // Auto-generated method stub--does not have any purpose here
            return 0;
        }

        @Override
        public Object getItem(int position) {
            // Auto-generated method stub--does not have any purpose here
            return null;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            View row = inflateLayout(getContext(), R.layout.settings_list_item);
            TextView mainTextView = (TextView) row.findViewById(R.id.main_text);
            TextView subTextView = (TextView) row.findViewById(R.id.sub_text);
            ImageView iconView = (ImageView) row.findViewById(R.id.icon);

            // Set main text
            mainTextView.setText(mMainTexts.get(position));

            // Remove sub text and center the main text if sub texts do not exist at all or the sub
            // text at this particular position is empty.
            if (mSubTexts == null || RESOURCE_EMPTY.equals(mSubTexts.get(position))) {
                subTextView.setVisibility(View.GONE);
            } else {
                // Otherwise, set sub text.
                subTextView.setText(mSubTexts.get(position));
            }

            // Remove main icon and set visibility to gone if icons are set to null or the icon at
            // this particular position is set to RESOURCE_NON_EXISTENT.
            if (mIconIds == null || mIconIds.get(position) == RESOURCE_NON_EXISTENT) {
                iconView.setVisibility(View.GONE);
            } else {
                // Otherwise, set main icon.
                iconView.setImageDrawable(mResources.getDrawable(mIconIds.get(position)));
            }
            return row;
        }

        public void setSubTexts(List<String> subTexts) {
            mSubTexts = subTexts;
        }
    }

    private class SubSettingsAdapter extends BaseAdapter {
        private List<String> mTexts;
        private int mCheckPosition;

        SubSettingsAdapter(List<String> texts, int checkPosition) {
            mTexts = texts;
            mCheckPosition = checkPosition;
        }

        public String getMainText(int position) {
            if (mTexts != null) {
                if (position < mTexts.size()) {
                    return mTexts.get(position);
                }
            }
            return RESOURCE_EMPTY;
        }

        @Override
        public int getCount() {
            return (mTexts == null) ? 0 : mTexts.size();
        }

        @Override
        public long getItemId(int position) {
            // Auto-generated method stub--does not have any purpose here
            return 0;
        }

        @Override
        public Object getItem(int position) {
            // Auto-generated method stub--does not have any purpose here
            return null;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            View row = inflateLayout(getContext(), R.layout.sub_settings_list_item);
            TextView textView = (TextView) row.findViewById(R.id.text);
            ImageView checkView = (ImageView) row.findViewById(R.id.check);

            textView.setText(mTexts.get(position));
            if (position != mCheckPosition) {
                checkView.setVisibility(View.INVISIBLE);
            }
            return row;
        }

        public void setTexts(List<String> texts) {
            mTexts = texts;
        }

        public void setCheckPosition(int checkPosition) {
            mCheckPosition = checkPosition;
        }
    }

    // TODO (b/122440911): Enable advertisement mode
    class PlayerCallback extends PlayerWrapper.PlayerCallback {
        @Override
        public void onPlayerStateChanged(@NonNull PlayerWrapper player, int state) {
            if (player != mPlayer) return;

            if (DEBUG) {
                Log.d(TAG, "onPlayerStateChanged(state: " + state + ")");
            }

            updateTimeViews(player.getCurrentMediaItem());

            // Update pause button depending on playback state for the following two reasons:
            //   1) Need to handle case where app customizes playback state behavior when app
            //      activity is resumed.
            //   2) Need to handle case where the media file reaches end of duration.
            ImageButton playPauseButton = findControlButton(mSizeType, R.id.pause);
            switch (state) {
                case SessionPlayer.PLAYER_STATE_PLAYING:
                    removeCallbacks(mUpdateProgress);
                    post(mUpdateProgress);
                    resetHideCallbacks();
                    updateReplayButton(false);
                    break;
                case SessionPlayer.PLAYER_STATE_PAUSED:
                    playPauseButton.setImageDrawable(
                            mResources.getDrawable(R.drawable.ic_play_circle_filled));
                    playPauseButton.setContentDescription(
                            mResources.getString(R.string.mcv2_play_button_desc));
                    removeCallbacks(mUpdateProgress);
                    removeCallbacks(mHideMainBars);
                    removeCallbacks(mHideProgressBar);
                    post(mShowAllBars);
                    break;
                case SessionPlayer.PLAYER_STATE_ERROR:
                    playPauseButton.setImageDrawable(
                            mResources.getDrawable(R.drawable.ic_play_circle_filled));
                    playPauseButton.setContentDescription(
                            mResources.getString(R.string.mcv2_play_button_desc));
                    removeCallbacks(mUpdateProgress);
                    if (getWindowToken() != null) {
                        new AlertDialog.Builder(getContext())
                                .setMessage(R.string.mcv2_playback_error_text)
                                .setPositiveButton(R.string.mcv2_error_dialog_button,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(
                                                    DialogInterface dialogInterface,
                                                    int i) {
                                                dialogInterface.dismiss();
                                            }
                                        })
                                .setCancelable(true)
                                .show();
                    }
                    break;
            }
        }

        @Override
        public void onSeekCompleted(@NonNull PlayerWrapper player, long position) {
            if (player != mPlayer) return;

            if (DEBUG) {
                Log.d(TAG, "onSeekCompleted(): " + position);
            }
            // Update progress bar and time text.
            int positionOnProgressBar = (mDuration <= 0)
                    ? 0 : (int) (MAX_PROGRESS * position / mDuration);
            mProgress.setProgress(positionOnProgressBar);
            mCurrentTime.setText(stringForTime(position));

            if (mNextSeekPosition != SEEK_POSITION_NOT_SET) {
                mCurrentSeekPosition = mNextSeekPosition;

                // If the next seek position is set, seek to that position.
                player.seekTo(mNextSeekPosition);
                mNextSeekPosition = SEEK_POSITION_NOT_SET;
            } else {
                mCurrentSeekPosition = SEEK_POSITION_NOT_SET;

                // If the next seek position is not set and the progress bar thumb is not being
                // dragged, start to update progress.
                if (!mDragging) {
                    removeCallbacks(mUpdateProgress);
                    removeCallbacks(mHideMainBars);
                    post(mUpdateProgress);
                    postDelayedRunnable(mHideMainBars, mDelayedAnimationIntervalMs);
                }
            }
        }

        @Override
        public void onCurrentMediaItemChanged(@NonNull PlayerWrapper player,
                @Nullable MediaItem mediaItem) {
            if (player != mPlayer) return;

            if (DEBUG) {
                Log.d(TAG, "onCurrentMediaItemChanged(): " + mediaItem);
            }
            updateTimeViews(mediaItem);
            updateTitleView(mediaItem);
        }

        @Override
        public void onPlaybackCompleted(@NonNull PlayerWrapper player) {
            if (player != mPlayer) return;

            if (DEBUG) {
                Log.d(TAG, "onPlaybackCompleted()");
            }
            updateReplayButton(true);
            // The progress bar and current time text may not have been updated.
            mProgress.setProgress(MAX_PROGRESS);
            mCurrentTime.setText(stringForTime(mDuration));
        }

        @Override
        public void onAllowedCommandsChanged(@NonNull PlayerWrapper player,
                @NonNull SessionCommandGroup commands) {
            if (player != mPlayer) return;

            updateAllowedCommands();
        }

        @Override
        public void onPlaybackSpeedChanged(@NonNull PlayerWrapper player, float speed) {
            if (player != mPlayer) return;

            int customSpeedMultBy100 = Math.round(speed * 100);
            // An application may set a custom playback speed that is not included in the
            // default playback speed list. The code below handles adding/removing the custom
            // playback speed to the default list.
            if (mCustomPlaybackSpeedIndex != -1) {
                // Remove existing custom playback speed
                removeCustomSpeedFromList();
            }

            if (mPlaybackSpeedMultBy100List.contains(customSpeedMultBy100)) {
                for (int i = 0; i < mPlaybackSpeedMultBy100List.size(); i++) {
                    if (customSpeedMultBy100 == mPlaybackSpeedMultBy100List.get(i)) {
                        updateSelectedSpeed(i, mPlaybackSpeedTextList.get(i));
                        break;
                    }
                }
            } else {
                String customSpeedText = mResources.getString(
                        R.string.MediaControlView_custom_playback_speed_text,
                        customSpeedMultBy100 / 100.0f);

                for (int i = 0; i < mPlaybackSpeedMultBy100List.size(); i++) {
                    if (customSpeedMultBy100 < mPlaybackSpeedMultBy100List.get(i)) {
                        mPlaybackSpeedMultBy100List.add(i, customSpeedMultBy100);
                        mPlaybackSpeedTextList.add(i, customSpeedText);
                        updateSelectedSpeed(i, customSpeedText);
                        break;
                    }
                    // Add to end of list if the custom speed value is greater than all the
                    // value in the default speed list.
                    if (i == mPlaybackSpeedMultBy100List.size() - 1
                            && customSpeedMultBy100 > mPlaybackSpeedMultBy100List.get(i)) {
                        mPlaybackSpeedMultBy100List.add(customSpeedMultBy100);
                        mPlaybackSpeedTextList.add(customSpeedText);
                        updateSelectedSpeed(i + 1, customSpeedText);
                    }
                }
                mCustomPlaybackSpeedIndex = mSelectedSpeedIndex;
            }
        }

        @Override
        void onTrackInfoChanged(@NonNull PlayerWrapper player,
                @NonNull List<TrackInfo> trackInfos) {
            if (player != mPlayer) return;

            if (DEBUG) {
                Log.d(TAG, "onTrackInfoChanged(): " + trackInfos);
            }

            updateTracks(player, trackInfos);
            updateTimeViews(player.getCurrentMediaItem());
            updateTitleView(player.getCurrentMediaItem());
        }

        @Override
        void onTrackSelected(@NonNull PlayerWrapper player, @NonNull TrackInfo trackInfo) {
            if (player != mPlayer) return;

            if (DEBUG) {
                Log.d(TAG, "onTrackSelected(): " + trackInfo);
            }
            if (trackInfo.getTrackType() == TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE) {
                for (int i = 0; i < mSubtitleTracks.size(); i++) {
                    if (mSubtitleTracks.get(i).equals(trackInfo)) {
                        mSelectedSubtitleTrackIndex = i;

                        if (mSettingsMode == SETTINGS_MODE_SUBTITLE_TRACK) {
                            mSubSettingsAdapter.setCheckPosition(mSelectedSubtitleTrackIndex + 1);
                        }
                        mSubtitleButton.setImageDrawable(
                                mResources.getDrawable(R.drawable.ic_subtitle_on));
                        mSubtitleButton.setContentDescription(
                                mResources.getString(R.string.mcv2_cc_is_on));
                        break;
                    }
                }
            } else if (trackInfo.getTrackType() == TrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
                for (int i = 0; i < mAudioTracks.size(); i++) {
                    if (mAudioTracks.get(i).equals(trackInfo)) {
                        mSelectedAudioTrackIndex = i;

                        mSettingsSubTextsList.set(SETTINGS_MODE_AUDIO_TRACK,
                                mSubSettingsAdapter.getMainText(mSelectedAudioTrackIndex));
                        break;
                    }
                }
            }
        }

        @Override
        void onTrackDeselected(@NonNull PlayerWrapper player, @NonNull TrackInfo trackInfo) {
            if (player != mPlayer) return;

            if (DEBUG) {
                Log.d(TAG, "onTrackDeselected(): " + trackInfo);
            }
            if (trackInfo.getTrackType() == TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE) {
                for (int i = 0; i < mSubtitleTracks.size(); i++) {
                    if (mSubtitleTracks.get(i).equals(trackInfo)) {
                        mSelectedSubtitleTrackIndex = -1;

                        if (mSettingsMode == SETTINGS_MODE_SUBTITLE_TRACK) {
                            mSubSettingsAdapter.setCheckPosition(mSelectedSubtitleTrackIndex + 1);
                        }
                        mSubtitleButton.setImageDrawable(
                                mResources.getDrawable(R.drawable.ic_subtitle_off));
                        mSubtitleButton.setContentDescription(
                                mResources.getString(R.string.mcv2_cc_is_off));
                        break;
                    }
                }
            }
        }

        @Override
        void onVideoSizeChanged(@NonNull PlayerWrapper player, @NonNull MediaItem item,
                @NonNull VideoSize videoSize) {
            if (player != mPlayer) return;

            if (DEBUG) {
                Log.d(TAG, "onVideoSizeChanged(): " + videoSize);
            }
            if (mVideoTrackCount == 0 && videoSize.getHeight() > 0 && videoSize.getWidth() > 0) {
                List<TrackInfo> tracks = player.getTrackInfo();
                if (tracks != null) {
                    updateTracks(player, tracks);
                }
            }
        }
    }
}
