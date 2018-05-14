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

package androidx.media.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.media.BaseMediaPlayer;
import androidx.media.MediaController2;
import androidx.media.MediaItem2;
import androidx.media.MediaMetadata2;
import androidx.media.SessionCommand2;
import androidx.media.SessionCommandGroup2;
import androidx.media.SessionToken2;
import androidx.mediarouter.app.MediaRouteButton;
import androidx.mediarouter.media.MediaRouteSelector;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

/**
 * A View that contains the controls for {@link android.media.MediaPlayer}.
 * It provides a wide range of buttons that serve the following functions: play/pause,
 * rewind/fast-forward, skip to next/previous, select subtitle track, enter/exit full screen mode,
 * adjust video quality, select audio track, mute/unmute, and adjust playback speed.
 *
 * <p>
 * <em> MediaControlView2 can be initialized in two different ways: </em>
 * 1) When initializing {@link VideoView2} a default MediaControlView2 is created.
 * 2) Initialize MediaControlView2 programmatically and add it to a {@link ViewGroup} instance.
 *
 * In the first option, VideoView2 automatically connects MediaControlView2 to MediaController,
 * which is necessary to communicate with MediaSession. In the second option, however, the
 * developer needs to manually retrieve a MediaController instance from MediaSession and set it to
 * MediaControlView2.
 *
 * <p>
 * There is no separate method that handles the show/hide behavior for MediaControlView2. Instead,
 * one can directly change the visibility of this view by calling {@link View#setVisibility(int)}.
 * The values supported are View.VISIBLE and View.GONE.
 *
 * <p>
 * In addition, the following customizations are supported:
 * 1) Set focus to the play/pause button by calling requestPlayButtonFocus().
 * 2) Set full screen mode
 *
 */
@RequiresApi(21) // TODO correct minSdk API use incompatibilities and remove before release.
public class MediaControlView2 extends BaseLayout {
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({
            BUTTON_PLAY_PAUSE,
            BUTTON_FFWD,
            BUTTON_REW,
            BUTTON_NEXT,
            BUTTON_PREV,
            BUTTON_SUBTITLE,
            BUTTON_FULL_SCREEN,
            BUTTON_OVERFLOW,
            BUTTON_MUTE,
            BUTTON_ASPECT_RATIO,
            BUTTON_SETTINGS
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Button {}

    /**
     * MediaControlView2 button value for playing and pausing media.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int BUTTON_PLAY_PAUSE = 1;
    /**
     * MediaControlView2 button value for jumping 30 seconds forward.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int BUTTON_FFWD = 2;
    /**
     * MediaControlView2 button value for jumping 10 seconds backward.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int BUTTON_REW = 3;
    /**
     * MediaControlView2 button value for jumping to next media.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int BUTTON_NEXT = 4;
    /**
     * MediaControlView2 button value for jumping to previous media.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int BUTTON_PREV = 5;
    /**
     * MediaControlView2 button value for showing/hiding subtitle track.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int BUTTON_SUBTITLE = 6;
    /**
     * MediaControlView2 button value for toggling full screen.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int BUTTON_FULL_SCREEN = 7;
    /**
     * MediaControlView2 button value for showing/hiding overflow buttons.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int BUTTON_OVERFLOW = 8;
    /**
     * MediaControlView2 button value for muting audio.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int BUTTON_MUTE = 9;
    /**
     * MediaControlView2 button value for adjusting aspect ratio of view.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int BUTTON_ASPECT_RATIO = 10;
    /**
     * MediaControlView2 button value for showing/hiding settings page.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int BUTTON_SETTINGS = 11;

    private static final String TAG = "MediaControlView2";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    static final String KEY_VIDEO_TRACK_COUNT = "VideoTrackCount";
    static final String KEY_AUDIO_TRACK_COUNT = "AudioTrackCount";
    static final String KEY_SUBTITLE_TRACK_COUNT = "SubtitleTrackCount";
    static final String KEY_PLAYBACK_SPEED = "PlaybackSpeed";
    static final String KEY_SELECTED_AUDIO_INDEX = "SelectedAudioIndex";
    static final String KEY_SELECTED_SUBTITLE_INDEX = "SelectedSubtitleIndex";
    static final String EVENT_UPDATE_TRACK_STATUS = "UpdateTrackStatus";
    static final String KEY_STATE_IS_ADVERTISEMENT = "MediaTypeAdvertisement";
    static final String EVENT_UPDATE_MEDIA_TYPE_STATUS = "UpdateMediaTypeStatus";

    // String for sending command to show subtitle to MediaSession.
    static final String COMMAND_SHOW_SUBTITLE = "showSubtitle";
    // String for sending command to hide subtitle to MediaSession.
    static final String COMMAND_HIDE_SUBTITLE = "hideSubtitle";
    // String for sending command to select audio track to MediaSession.
    static final String COMMAND_SELECT_AUDIO_TRACK = "SelectTrack";
    // String for sending command to set playback speed to MediaSession.
    static final String COMMAND_SET_PLAYBACK_SPEED = "SetPlaybackSpeed";
    // String for sending command to mute audio to MediaSession.
    static final String COMMAND_MUTE = "Mute";
    // String for sending command to unmute audio to MediaSession.
    static final String COMMAND_UNMUTE = "Unmute";

    private static final int SETTINGS_MODE_AUDIO_TRACK = 0;
    private static final int SETTINGS_MODE_PLAYBACK_SPEED = 1;
    private static final int SETTINGS_MODE_HELP = 2;
    private static final int SETTINGS_MODE_SUBTITLE_TRACK = 3;
    private static final int SETTINGS_MODE_VIDEO_QUALITY = 4;
    private static final int SETTINGS_MODE_MAIN = 5;
    private static final int PLAYBACK_SPEED_1x_INDEX = 3;

    private static final int MEDIA_TYPE_DEFAULT = 0;
    private static final int MEDIA_TYPE_MUSIC = 1;
    private static final int MEDIA_TYPE_ADVERTISEMENT = 2;

    private static final int SIZE_TYPE_EMBEDDED = 0;
    private static final int SIZE_TYPE_FULL = 1;
    private static final int SIZE_TYPE_MINIMAL = 2;

    private static final int UX_STATE_FULL = 0;
    private static final int UX_STATE_PROGRESS_BAR_ONLY = 1;
    private static final int UX_STATE_EMPTY = 2;
    private static final int UX_STATE_ANIMATING = 3;

    private static final long DEFAULT_SHOW_CONTROLLER_INTERVAL_MS = 2000;
    private static final int MAX_PROGRESS = 1000;
    private static final int DEFAULT_PROGRESS_UPDATE_TIME_MS = 1000;
    private static final int REWIND_TIME_MS = 10000;
    private static final int FORWARD_TIME_MS = 30000;
    private static final int AD_SKIP_WAIT_TIME_MS = 5000;
    private static final int RESOURCE_NON_EXISTENT = -1;
    private static final int HIDE_TIME_MS = 250;
    private static final int SHOW_TIME_MS = 250;
    private static final String RESOURCE_EMPTY = "";

    private Resources mResources;
    private ControllerInterface mController;
    private OnFullScreenListener mOnFullScreenListener;
    private AccessibilityManager mAccessibilityManager;
    private int mDuration;
    private int mPrevState;
    private int mPrevWidth;
    private int mOriginalLeftBarWidth;
    private int mVideoTrackCount;
    private int mAudioTrackCount;
    private int mSubtitleTrackCount;
    private int mSettingsMode;
    private int mSelectedSubtitleTrackIndex;
    private int mSelectedAudioTrackIndex;
    private int mSelectedVideoQualityIndex;
    private int mSelectedSpeedIndex;
    private int mEmbeddedSettingsItemWidth;
    private int mFullSettingsItemWidth;
    private int mSettingsItemHeight;
    private int mSettingsWindowMargin;
    private int mMediaType;
    private int mSizeType;
    private int mUxState;
    private long mPlaybackActions;
    private long mShowControllerIntervalMs;
    private boolean mDragging;
    private boolean mIsFullScreen;
    private boolean mOverflowIsShowing;
    private boolean mIsStopped;
    private boolean mSeekAvailable;
    private boolean mIsAdvertisement;
    private boolean mIsMute;
    private boolean mNeedUxUpdate;
    private boolean mNeedToHideBars;

    // Relating to Title Bar View
    private ViewGroup mRoot;
    private View mTitleBar;
    private TextView mTitleView;
    private View mAdExternalLink;
    private ImageButton mBackButton;
    private MediaRouteButton mRouteButton;
    private MediaRouteSelector mRouteSelector;

    // Relating to Center View
    private ViewGroup mCenterView;
    private View mTransportControls;
    private ImageButton mPlayPauseButton;
    private ImageButton mFfwdButton;
    private ImageButton mRewButton;
    private ImageButton mNextButton;
    private ImageButton mPrevButton;

    // Relating to Minimal Extra View
    private LinearLayout mMinimalExtraView;

    // Relating to Progress Bar View
    private View mProgressBar;
    private ProgressBar mProgress;
    private View mProgressBuffer;

    // Relating to Bottom Bar View
    private ViewGroup mBottomBar;

    // Relating to Bottom Bar Left View
    private ViewGroup mBottomBarLeftView;
    private ViewGroup mTimeView;
    private TextView mEndTime;
    private TextView mCurrentTime;
    private TextView mAdSkipView;
    private StringBuilder mFormatBuilder;
    private Formatter mFormatter;

    // Relating to Bottom Bar Right View
    private ViewGroup mBottomBarRightView;
    private ViewGroup mBasicControls;
    private ViewGroup mExtraControls;
    private ViewGroup mCustomButtons;
    private ImageButton mSubtitleButton;
    private ImageButton mFullScreenButton;
    private ImageButton mOverflowShowButton;
    private ImageButton mOverflowHideButton;
    private ImageButton mMuteButton;
    private ImageButton mVideoQualityButton;
    private ImageButton mSettingsButton;
    private TextView mAdRemainingView;

    // Relating to Settings List View
    private ListView mSettingsListView;
    private PopupWindow mSettingsWindow;
    private SettingsAdapter mSettingsAdapter;
    private SubSettingsAdapter mSubSettingsAdapter;
    private List<String> mSettingsMainTextsList;
    private List<String> mSettingsSubTextsList;
    private List<Integer> mSettingsIconIdsList;
    private List<String> mSubtitleDescriptionsList;
    private List<String> mAudioTrackList;
    private List<String> mVideoQualityList;
    private List<String> mPlaybackSpeedTextList;
    private List<Float> mPlaybackSpeedList;

    private AnimatorSet mHideMainBarsAnimator;
    private AnimatorSet mHideProgressBarAnimator;
    private AnimatorSet mHideAllBarsAnimator;
    private AnimatorSet mShowMainBarsAnimator;
    private AnimatorSet mShowAllBarsAnimator;
    private ValueAnimator mOverflowShowAnimator;
    private ValueAnimator mOverflowHideAnimator;

    public MediaControlView2(@NonNull Context context) {
        this(context, null);
    }

    public MediaControlView2(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MediaControlView2(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mResources = context.getResources();
        // Inflate MediaControlView2 from XML
        mRoot = makeControllerView();
        addView(mRoot);
        mShowControllerIntervalMs = DEFAULT_SHOW_CONTROLLER_INTERVAL_MS;
        mAccessibilityManager = (AccessibilityManager) context.getSystemService(
                Context.ACCESSIBILITY_SERVICE);
    }

    /**
     * Sets MediaSession2 token to control corresponding MediaSession2. It makes it possible to
     * send and receive data between MediaControlView2 and VideoView2.
     * @hide TODO: unhide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void setMediaSessionToken(SessionToken2 token) {
        if (mController != null) {
            mController.close();
        }
        mController = new Controller2(token);
        if (mController.hasMetadata()) {
            updateDuration();
            updateTitle();
        }
    }

    /**
     * Sets MediaControllerCompat to control corresponding MediaSession2.
     * This is temporally provided for working with MP1 on lower devices.
     * @hide
     */
    // TODO: Remove with impl_with_mp1 once MP2 compat starts supporting lower devices.
    @RestrictTo(LIBRARY_GROUP)
    public void setController(MediaControllerCompat controllerCompat) {
        mController = new ControllerCompat(controllerCompat);
        if (mController.hasMetadata()) {
            updateDuration();
            updateTitle();
        }
    }

    /**
     * Registers a callback to be invoked when the fullscreen mode should be changed.
     * @param l The callback that will be run
     */
    public void setOnFullScreenListener(OnFullScreenListener l) {
        mOnFullScreenListener = l;
    }

    /**
     * Changes the visibility state of an individual button. Default value is View.Visible.
     *
     * @param button the {@code Button} assigned to individual buttons
     * <ul>
     * <li>{@link #BUTTON_PLAY_PAUSE}
     * <li>{@link #BUTTON_FFWD}
     * <li>{@link #BUTTON_REW}
     * <li>{@link #BUTTON_NEXT}
     * <li>{@link #BUTTON_PREV}
     * <li>{@link #BUTTON_SUBTITLE}
     * <li>{@link #BUTTON_FULL_SCREEN}
     * <li>{@link #BUTTON_MUTE}
     * <li>{@link #BUTTON_OVERFLOW}
     * <li>{@link #BUTTON_ASPECT_RATIO}
     * <li>{@link #BUTTON_SETTINGS}
     * </ul>
     * @param visibility One of {@link #VISIBLE}, {@link #INVISIBLE}, or {@link #GONE}.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void setButtonVisibility(@Button int button, /*@Visibility*/ int visibility) {
        switch (button) {
            case MediaControlView2.BUTTON_PLAY_PAUSE:
                if (mPlayPauseButton != null && canPause()) {
                    mPlayPauseButton.setVisibility(visibility);
                }
                break;
            case MediaControlView2.BUTTON_FFWD:
                if (mFfwdButton != null && canSeekForward()) {
                    mFfwdButton.setVisibility(visibility);
                }
                break;
            case MediaControlView2.BUTTON_REW:
                if (mRewButton != null && canSeekBackward()) {
                    mRewButton.setVisibility(visibility);
                }
                break;
            case MediaControlView2.BUTTON_NEXT:
                if (mNextButton != null) {
                    mNextButton.setVisibility(visibility);
                }
                break;
            case MediaControlView2.BUTTON_PREV:
                if (mPrevButton != null) {
                    mPrevButton.setVisibility(visibility);
                }
                break;
            case MediaControlView2.BUTTON_SUBTITLE:
                if (mSubtitleButton != null && mSubtitleTrackCount > 0) {
                    mSubtitleButton.setVisibility(visibility);
                }
                break;
            case MediaControlView2.BUTTON_FULL_SCREEN:
                if (mFullScreenButton != null) {
                    mFullScreenButton.setVisibility(visibility);
                }
                break;
            case MediaControlView2.BUTTON_OVERFLOW:
                if (mOverflowShowButton != null) {
                    mOverflowShowButton.setVisibility(visibility);
                }
                break;
            case MediaControlView2.BUTTON_MUTE:
                if (mMuteButton != null) {
                    mMuteButton.setVisibility(visibility);
                }
                break;
            case MediaControlView2.BUTTON_SETTINGS:
                if (mSettingsButton != null) {
                    mSettingsButton.setVisibility(visibility);
                }
                break;
            default:
                break;
        }
    }

    /**
     *  Requests focus for the play/pause button.
     */
    public void requestPlayButtonFocus() {
        if (mPlayPauseButton != null) {
            mPlayPauseButton.requestFocus();
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
        void onFullScreen(View view, boolean fullScreen);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return MediaControlView2.class.getName();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            if (mMediaType != MEDIA_TYPE_MUSIC || mSizeType != SIZE_TYPE_FULL) {
                toggleMediaControlViewVisibility();
            }
        }
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            if (mMediaType != MEDIA_TYPE_MUSIC || mSizeType != SIZE_TYPE_FULL) {
                toggleMediaControlViewVisibility();
            }
        }
        return true;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // Update layout when this view's width changes in order to avoid any UI overlap between
        // transport controls.
        if (mPrevWidth != getMeasuredWidth() || mNeedUxUpdate) {
            // Dismiss SettingsWindow if it is showing.
            mSettingsWindow.dismiss();

            // Hide Overflow if it is showing.
            if (mOverflowIsShowing) {
                mOverflowHideAnimator.start();
            }

            // The following views may not have been initialized yet.
            if (mTransportControls.getWidth() == 0 || mTimeView.getWidth() == 0) {
                return;
            }

            // Update layout if necessary
            int currWidth = getMeasuredWidth();
            int currHeight = getMeasuredHeight();
            WindowManager manager = (WindowManager) getContext().getApplicationContext()
                    .getSystemService(Context.WINDOW_SERVICE);
            Point screenSize = new Point();
            manager.getDefaultDisplay().getSize(screenSize);
            int screenWidth = screenSize.x;
            int screenHeight = screenSize.y;
            int iconSize = mResources.getDimensionPixelSize(R.dimen.mcv2_icon_size);

            if (mMediaType == MEDIA_TYPE_DEFAULT) {
                // Max number of icons inside BottomBarRightView for Music mode is 4.
                int maxIconCount = 4;
                updateLayout(maxIconCount, iconSize, currWidth, currHeight, screenWidth,
                        screenHeight);

            } else if (mMediaType == MEDIA_TYPE_MUSIC) {
                if (mNeedUxUpdate) {
                    // One-time operation for Music media type
                    mBasicControls.removeView(mMuteButton);
                    mExtraControls.addView(mMuteButton, 0);
                    mVideoQualityButton.setVisibility(View.GONE);
                    if (mFfwdButton != null) {
                        mFfwdButton.setVisibility(View.GONE);
                    }
                    if (mRewButton != null) {
                        mRewButton.setVisibility(View.GONE);
                    }
                }
                mNeedUxUpdate = false;

                // Max number of icons inside BottomBarRightView for Music mode is 3.
                int maxIconCount = 3;
                updateLayout(maxIconCount, iconSize, currWidth, currHeight, screenWidth,
                        screenHeight);
            }
            mPrevWidth = currWidth;

            // By default, show the full view when view size is changed.
            if (mUxState != UX_STATE_FULL) {
                removeCallbacks(mHideMainBars);
                removeCallbacks(mHideProgressBar);
                post(mShowMainBars);
            }
        }
        // Update title bar parameters in order to avoid overlap between title view and the right
        // side of the title bar.
        updateTitleBarLayout();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (mPlayPauseButton != null) {
            mPlayPauseButton.setEnabled(enabled);
        }
        if (mFfwdButton != null) {
            mFfwdButton.setEnabled(enabled);
        }
        if (mRewButton != null) {
            mRewButton.setEnabled(enabled);
        }
        if (mNextButton != null) {
            mNextButton.setEnabled(enabled);
        }
        if (mPrevButton != null) {
            mPrevButton.setEnabled(enabled);
        }
        if (mProgress != null) {
            mProgress.setEnabled(enabled);
        }
        if (mSubtitleButton != null) {
            mSubtitleButton.setEnabled(enabled);
        }
        if (mFullScreenButton != null) {
            mFullScreenButton.setEnabled(enabled);
        }
        if (mOverflowShowButton != null) {
            mOverflowShowButton.setEnabled(enabled);
        }
        if (mOverflowHideButton != null) {
            mOverflowHideButton.setEnabled(enabled);
        }
        if (mMuteButton != null) {
            mMuteButton.setEnabled(enabled);
        }
        if (mVideoQualityButton != null) {
            mVideoQualityButton.setEnabled(enabled);
        }
        if (mSettingsButton != null) {
            mSettingsButton.setEnabled(enabled);
        }
        if (mBackButton != null) {
            mBackButton.setEnabled(enabled);
        }
        if (mRouteButton != null) {
            mRouteButton.setEnabled(enabled);
        }
        disableUnsupportedButtons();
    }

    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);

        if (isVisible) {
            disableUnsupportedButtons();
            removeCallbacks(mUpdateProgress);
            post(mUpdateProgress);
        } else {
            removeCallbacks(mUpdateProgress);
        }
    }

    void setRouteSelector(MediaRouteSelector selector) {
        mRouteSelector = selector;
        if (mRouteSelector != null && !mRouteSelector.isEmpty()) {
            mRouteButton.setRouteSelector(selector);
            mRouteButton.setVisibility(View.VISIBLE);
        } else {
            mRouteButton.setRouteSelector(MediaRouteSelector.EMPTY);
            mRouteButton.setVisibility(View.GONE);
        }
    }

    void setShowControllerInterval(long interval) {
        mShowControllerIntervalMs = interval;
    }

    ///////////////////////////////////////////////////
    // Protected or private methods
    ///////////////////////////////////////////////////

    private boolean isPlaying() {
        return mController.isPlaying();
    }

    private int getCurrentPosition() {
        return mController.getCurrentPosition();
    }

    private int getBufferPercentage() {
        if (mDuration == 0) {
            return 0;
        }
        long bufferedPos = mController.getBufferedPosition();
        return (bufferedPos < 0) ? -1 : (int) (bufferedPos * 100 / mDuration);
    }

    private boolean canPause() {
        return mController.canPause();
    }

    private boolean canSeekBackward() {
        return mController.canSeekBackward();
    }

    private boolean canSeekForward() {
        return mController.canSeekForward();
    }

    /**
     * Create the view that holds the widgets that control playback.
     * Derived classes can override this to create their own.
     *
     * @return The controller view.
     */
    private ViewGroup makeControllerView() {
        ViewGroup root = (ViewGroup) inflateLayout(getContext(), R.layout.media_controller);
        initControllerView(root);
        return root;
    }

    private View inflateLayout(Context context, int resId) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(resId, null);
    }

    @SuppressWarnings("deprecation")
    private void initControllerView(ViewGroup v) {
        // Relating to Title Bar View
        mTitleBar = v.findViewById(R.id.title_bar);
        mTitleView = v.findViewById(R.id.title_text);
        mAdExternalLink = v.findViewById(R.id.ad_external_link);
        mBackButton = v.findViewById(R.id.back);
        if (mBackButton != null) {
            mBackButton.setOnClickListener(mBackListener);
            mBackButton.setVisibility(View.GONE);
        }
        mRouteButton = v.findViewById(R.id.cast);

        // Relating to Center View
        mCenterView = v.findViewById(R.id.center_view);
        mTransportControls = inflateTransportControls(R.layout.embedded_transport_controls);
        mCenterView.addView(mTransportControls);

        // Relating to Minimal Extra View
        mMinimalExtraView = (LinearLayout) v.findViewById(R.id.minimal_extra_view);
        LinearLayout.LayoutParams params =
                (LinearLayout.LayoutParams) mMinimalExtraView.getLayoutParams();
        int iconSize = mResources.getDimensionPixelSize(R.dimen.mcv2_icon_size);
        int marginSize = mResources.getDimensionPixelSize(R.dimen.mcv2_icon_margin);
        params.setMargins(0, (iconSize + marginSize * 2) * (-1), 0, 0);
        mMinimalExtraView.setLayoutParams(params);
        mMinimalExtraView.setVisibility(View.GONE);

        // Relating to Progress Bar View
        mProgressBar = v.findViewById(R.id.progress_bar);
        mProgress = v.findViewById(R.id.progress);
        if (mProgress != null) {
            if (mProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mProgress;
                seeker.setOnSeekBarChangeListener(mSeekListener);
                seeker.setProgressDrawable(mResources.getDrawable(R.drawable.custom_progress));
                seeker.setThumb(mResources.getDrawable(R.drawable.custom_progress_thumb));
            }
            mProgress.setMax(MAX_PROGRESS);
        }
        mProgressBuffer = v.findViewById(R.id.progress_buffer);

        // Relating to Bottom Bar View
        mBottomBar = v.findViewById(R.id.bottom_bar);

        // Relating to Bottom Bar Left View
        mBottomBarLeftView = v.findViewById(R.id.bottom_bar_left);
        mTimeView = v.findViewById(R.id.time);
        mEndTime = v.findViewById(R.id.time_end);
        mCurrentTime = v.findViewById(R.id.time_current);
        mAdSkipView = v.findViewById(R.id.ad_skip_time);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

        // Relating to Bottom Bar Right View
        mBasicControls = v.findViewById(R.id.basic_controls);
        mExtraControls = v.findViewById(R.id.extra_controls);
        mCustomButtons = v.findViewById(R.id.custom_buttons);
        mSubtitleButton = v.findViewById(R.id.subtitle);
        if (mSubtitleButton != null) {
            mSubtitleButton.setOnClickListener(mSubtitleListener);
        }
        mFullScreenButton = v.findViewById(R.id.fullscreen);
        if (mFullScreenButton != null) {
            mFullScreenButton.setOnClickListener(mFullScreenListener);
        }
        mOverflowShowButton = v.findViewById(R.id.overflow_show);
        if (mOverflowShowButton != null) {
            mOverflowShowButton.setOnClickListener(mOverflowShowListener);
        }
        mOverflowHideButton = v.findViewById(R.id.overflow_hide);
        if (mOverflowHideButton != null) {
            mOverflowHideButton.setOnClickListener(mOverflowHideListener);
        }
        mMuteButton = v.findViewById(R.id.mute);
        if (mMuteButton != null) {
            mMuteButton.setOnClickListener(mMuteButtonListener);
        }
        mSettingsButton = v.findViewById(R.id.settings);
        if (mSettingsButton != null) {
            mSettingsButton.setOnClickListener(mSettingsButtonListener);
        }
        mVideoQualityButton = v.findViewById(R.id.video_quality);
        if (mVideoQualityButton != null) {
            mVideoQualityButton.setOnClickListener(mVideoQualityListener);
        }
        mAdRemainingView = v.findViewById(R.id.ad_remaining);

        // Relating to Settings List View
        initializeSettingsLists();
        mSettingsListView = (ListView) inflateLayout(getContext(), R.layout.settings_list);
        mSettingsAdapter = new SettingsAdapter(mSettingsMainTextsList, mSettingsSubTextsList,
                mSettingsIconIdsList);
        mSubSettingsAdapter = new SubSettingsAdapter(null, 0);
        mSettingsListView.setAdapter(mSettingsAdapter);
        mSettingsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mSettingsListView.setOnItemClickListener(mSettingsItemClickListener);

        mEmbeddedSettingsItemWidth = mResources.getDimensionPixelSize(
                R.dimen.mcv2_embedded_settings_width);
        mFullSettingsItemWidth = mResources.getDimensionPixelSize(R.dimen.mcv2_full_settings_width);
        mSettingsItemHeight = mResources.getDimensionPixelSize(
                R.dimen.mcv2_settings_height);
        mSettingsWindowMargin = (-1) * mResources.getDimensionPixelSize(
                R.dimen.mcv2_settings_offset);
        mSettingsWindow = new PopupWindow(mSettingsListView, mEmbeddedSettingsItemWidth,
                LayoutParams.WRAP_CONTENT, true);
        mSettingsWindow.setOnDismissListener(mSettingsDismissListener);

        int titleBarTranslateY =
                (-1) * mResources.getDimensionPixelSize(R.dimen.mcv2_title_bar_height);
        int bottomBarHeight = mResources.getDimensionPixelSize(R.dimen.mcv2_bottom_bar_height);
        int progressThumbHeight = mResources.getDimensionPixelSize(
                R.dimen.mcv2_custom_progress_thumb_size);
        int progressBarHeight = mResources.getDimensionPixelSize(
                R.dimen.mcv2_custom_progress_max_size);
        int bottomBarTranslateY = bottomBarHeight + progressThumbHeight / 2 - progressBarHeight / 2;

        ValueAnimator fadeOutAnimator = ValueAnimator.ofFloat(1.0f, 0.0f);
        fadeOutAnimator.setInterpolator(new LinearInterpolator());
        fadeOutAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float alpha = (float) animation.getAnimatedValue();
                SeekBar seekBar = (SeekBar) mProgress;
                GradientDrawable thumb = (GradientDrawable)
                        mResources.getDrawable(R.drawable.custom_progress_thumb);
                int newSize = (int) (mResources.getDimensionPixelSize(
                        R.dimen.mcv2_custom_progress_thumb_size) * alpha);
                thumb.setSize(newSize, newSize);
                seekBar.setThumb(thumb);

                mTransportControls.setAlpha(alpha);
                if (mSizeType == SIZE_TYPE_MINIMAL) {
                    mFullScreenButton.setAlpha(alpha);
                }
                if (alpha == 0.0f) {
                    mTransportControls.setVisibility(View.GONE);
                } else if (alpha == 1.0f) {
                    setEnabled(false);
                }
            }
        });

        ValueAnimator fadeInAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        fadeInAnimator.setInterpolator(new LinearInterpolator());
        fadeInAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float alpha = (float) animation.getAnimatedValue();
                SeekBar seekBar = (SeekBar) mProgress;
                GradientDrawable thumb =
                        (GradientDrawable) mResources.getDrawable(R.drawable.custom_progress_thumb);
                int newSize = (int) (mResources.getDimensionPixelSize(
                        R.dimen.mcv2_custom_progress_thumb_size) * alpha);
                thumb.setSize(newSize, newSize);
                seekBar.setThumb(thumb);

                mTransportControls.setAlpha(alpha);
                if (mSizeType == SIZE_TYPE_MINIMAL) {
                    mFullScreenButton.setAlpha(alpha);
                }
                if (alpha == 0.0f) {
                    mTransportControls.setVisibility(View.VISIBLE);
                } else if (alpha == 1.0f) {
                    setEnabled(true);
                }
            }
        });

        mHideMainBarsAnimator = new AnimatorSet();
        mHideMainBarsAnimator
                .play(ObjectAnimator.ofFloat(mTitleBar, "translationY",
                        0, titleBarTranslateY))
                .with(ObjectAnimator.ofFloat(mBottomBar, "translationY",
                        0, bottomBarTranslateY))
                .with(ObjectAnimator.ofFloat(mProgressBar, "translationY",
                        0, bottomBarTranslateY))
                .with(fadeOutAnimator);
        mHideMainBarsAnimator.setDuration(HIDE_TIME_MS);
        mHideMainBarsAnimator.getChildAnimations().get(0).addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        setEnabled(false);
                        mUxState = UX_STATE_ANIMATING;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        setEnabled(true);
                        mUxState = UX_STATE_PROGRESS_BAR_ONLY;
                    }
                });

        mHideProgressBarAnimator = new AnimatorSet();
        mHideProgressBarAnimator
                .play(ObjectAnimator.ofFloat(mBottomBar, "translationY",
                        bottomBarTranslateY, bottomBarTranslateY + progressBarHeight))
                .with(ObjectAnimator.ofFloat(mProgressBar, "translationY",
                        bottomBarTranslateY, bottomBarTranslateY + progressBarHeight));
        mHideProgressBarAnimator.setDuration(HIDE_TIME_MS);
        mHideProgressBarAnimator.getChildAnimations().get(0).addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        setEnabled(false);
                        mUxState = UX_STATE_ANIMATING;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        setEnabled(true);
                        mUxState = UX_STATE_EMPTY;
                    }
                });

        mHideAllBarsAnimator = new AnimatorSet();
        mHideAllBarsAnimator
                .play(ObjectAnimator.ofFloat(mTitleBar, "translationY",
                        0, titleBarTranslateY))
                .with(ObjectAnimator.ofFloat(mBottomBar, "translationY",
                        0, bottomBarTranslateY + progressBarHeight))
                .with(ObjectAnimator.ofFloat(mProgressBar, "translationY",
                        0, bottomBarTranslateY + progressBarHeight))
                .with(fadeOutAnimator);
        mHideAllBarsAnimator.setDuration(HIDE_TIME_MS);
        mHideAllBarsAnimator.getChildAnimations().get(0).addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                setEnabled(false);
                mUxState = UX_STATE_ANIMATING;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                setEnabled(true);
                mUxState = UX_STATE_EMPTY;
            }
        });

        mShowMainBarsAnimator = new AnimatorSet();
        mShowMainBarsAnimator
                .play(ObjectAnimator.ofFloat(mTitleBar, "translationY",
                        titleBarTranslateY, 0))
                .with(ObjectAnimator.ofFloat(mBottomBar, "translationY",
                        bottomBarTranslateY, 0))
                .with(ObjectAnimator.ofFloat(mProgressBar, "translationY",
                        bottomBarTranslateY, 0))
                .with(fadeInAnimator);
        mShowMainBarsAnimator.setDuration(SHOW_TIME_MS);
        mShowMainBarsAnimator.getChildAnimations().get(0).addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        setEnabled(false);
                        mUxState = UX_STATE_ANIMATING;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        setEnabled(true);
                        mUxState = UX_STATE_FULL;
                    }
                });

        mShowAllBarsAnimator = new AnimatorSet();
        mShowAllBarsAnimator
                .play(ObjectAnimator.ofFloat(mTitleBar, "translationY",
                        titleBarTranslateY, 0))
                .with(ObjectAnimator.ofFloat(mBottomBar, "translationY",
                        bottomBarTranslateY + progressBarHeight, 0))
                .with(ObjectAnimator.ofFloat(mProgressBar, "translationY",
                        bottomBarTranslateY + progressBarHeight, 0))
                .with(fadeInAnimator);
        mShowAllBarsAnimator.setDuration(SHOW_TIME_MS);
        mShowAllBarsAnimator.getChildAnimations().get(0).addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                setEnabled(false);
                mUxState = UX_STATE_ANIMATING;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                setEnabled(true);
                mUxState = UX_STATE_FULL;
            }
        });

        mOverflowShowAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        mOverflowShowAnimator.setDuration(SHOW_TIME_MS);
        mOverflowShowAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                animateOverflow(animation);
            }
        });
        mOverflowShowAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mExtraControls.setVisibility(View.VISIBLE);
                mOverflowShowButton.setVisibility(View.GONE);
                mOverflowHideButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mBasicControls.setVisibility(View.GONE);

                if (mSizeType == SIZE_TYPE_FULL) {
                    mFfwdButton.setVisibility(View.GONE);
                }
            }
        });

        mOverflowHideAnimator = ValueAnimator.ofFloat(1.0f, 0.0f);
        mOverflowHideAnimator.setDuration(SHOW_TIME_MS);
        mOverflowHideAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                animateOverflow(animation);
            }
        });
        mOverflowHideAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mBasicControls.setVisibility(View.VISIBLE);
                mOverflowShowButton.setVisibility(View.VISIBLE);
                mOverflowHideButton.setVisibility(View.GONE);

                if (mSizeType == SIZE_TYPE_FULL) {
                    mFfwdButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mExtraControls.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Disable pause or seek buttons if the stream cannot be paused or seeked.
     * This requires the control interface to be a MediaPlayerControlExt
     */
    private void disableUnsupportedButtons() {
        try {
            if (mPlayPauseButton != null && !canPause()) {
                mPlayPauseButton.setEnabled(false);
            }
            if (mRewButton != null && !canSeekBackward()) {
                mRewButton.setEnabled(false);
            }
            if (mFfwdButton != null && !canSeekForward()) {
                mFfwdButton.setEnabled(false);
            }
            if (mProgress != null && !canSeekBackward() && !canSeekForward()) {
                mProgress.setEnabled(false);
            }
        } catch (IncompatibleClassChangeError ex) {
            // We were given an old version of the interface, that doesn't have
            // the canPause/canSeekXYZ methods. This is OK, it just means we
            // assume the media can be paused and seeked, and so we don't disable
            // the buttons.
        }
    }

    private final Runnable mUpdateProgress = new Runnable() {
        @Override
        public void run() {
            boolean isShowing = getVisibility() == View.VISIBLE;
            if (!mDragging && isShowing && isPlaying()) {
                int pos = setProgress();
                postDelayed(mUpdateProgress,
                        DEFAULT_PROGRESS_UPDATE_TIME_MS - (pos % DEFAULT_PROGRESS_UPDATE_TIME_MS));
            }
        }
    };

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private int setProgress() {
        if (mController == null || mDragging) {
            return 0;
        }
        int positionOnProgressBar = 0;
        int currentPosition = getCurrentPosition();
        if (mDuration > 0) {
            positionOnProgressBar = (int) (MAX_PROGRESS * (long) currentPosition / mDuration);
        }
        if (mProgress != null && currentPosition != mDuration) {
            mProgress.setProgress(positionOnProgressBar);
            // If the media is a local file, there is no need to set a buffer, so set secondary
            // progress to maximum.
            if (getBufferPercentage() < 0) {
                mProgress.setSecondaryProgress(MAX_PROGRESS);
            } else {
                mProgress.setSecondaryProgress(getBufferPercentage() * 10);
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
                            R.string.MediaControlView2_ad_skip_wait_time,
                            ((AD_SKIP_WAIT_TIME_MS - currentPosition) / 1000 + 1));
                    mAdSkipView.setText(skipTimeText);
                } else {
                    if (mAdSkipView.getVisibility() == View.VISIBLE) {
                        mAdSkipView.setVisibility(View.GONE);
                        mNextButton.setEnabled(true);
                        mNextButton.clearColorFilter();
                    }
                }
            }
            // Update the remaining number of seconds of the advertisement.
            if (mAdRemainingView != null) {
                int remainingTime =
                        (mDuration - currentPosition < 0) ? 0 : (mDuration - currentPosition);
                String remainingTimeText = mResources.getString(
                        R.string.MediaControlView2_ad_remaining_time,
                        stringForTime(remainingTime));
                mAdRemainingView.setText(remainingTimeText);
            }
        }
        return currentPosition;
    }

    private void togglePausePlayState() {
        if (isPlaying()) {
            mController.pause();
            mPlayPauseButton.setImageDrawable(
                    mResources.getDrawable(R.drawable.ic_play_circle_filled, null));
            mPlayPauseButton.setContentDescription(
                    mResources.getString(R.string.mcv2_play_button_desc));
        } else {
            mController.play();
            mPlayPauseButton.setImageDrawable(
                    mResources.getDrawable(R.drawable.ic_pause_circle_filled, null));
            mPlayPauseButton.setContentDescription(
                    mResources.getString(R.string.mcv2_pause_button_desc));
        }
    }

    private void toggleMediaControlViewVisibility() {
        if ((mMediaType == MEDIA_TYPE_MUSIC && mSizeType == SIZE_TYPE_FULL)
                || mShowControllerIntervalMs == 0
                || mAccessibilityManager.isTouchExplorationEnabled()
                || mUxState == UX_STATE_ANIMATING) {
            return;
        }
        removeCallbacks(mHideMainBars);
        removeCallbacks(mHideProgressBar);

        switch (mUxState) {
            case UX_STATE_EMPTY:
                post(mShowAllBars);
                break;
            case UX_STATE_PROGRESS_BAR_ONLY:
                post(mShowMainBars);
                break;
            case UX_STATE_FULL:
                post(mHideAllBars);
                break;
        }
    }

    private final Runnable mShowAllBars = new Runnable() {
        @Override
        public void run() {
            mShowAllBarsAnimator.start();
            if (isPlaying()) {
                postDelayed(mHideMainBars, mShowControllerIntervalMs);
            }
        }
    };

    private final Runnable mShowMainBars = new Runnable() {
        @Override
        public void run() {
            mShowMainBarsAnimator.start();
            postDelayed(mHideMainBars, mShowControllerIntervalMs);
        }
    };

    private final Runnable mHideAllBars = new Runnable() {
        @Override
        public void run() {
            mHideAllBarsAnimator.start();
        }
    };

    private final Runnable mHideMainBars = new Runnable() {
        @Override
        public void run() {
            if (!isPlaying()) {
                return;
            }
            mHideMainBarsAnimator.start();
            postDelayed(mHideProgressBar, mShowControllerIntervalMs);
        }
    };

    private final Runnable mHideProgressBar = new Runnable() {
        @Override
        public void run() {
            if (!isPlaying()) {
                return;
            }
            mHideProgressBarAnimator.start();
        }
    };

    // There are two scenarios that can trigger the seekbar listener to trigger:
    //
    // The first is the user using the touchpad to adjust the posititon of the
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
            if (!mSeekAvailable) {
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
            if (mIsStopped) {
                mPlayPauseButton.setImageDrawable(
                        mResources.getDrawable(R.drawable.ic_play_circle_filled, null));
                mPlayPauseButton.setContentDescription(
                        mResources.getString(R.string.mcv2_play_button_desc));
                mIsStopped = false;
            }
        }

        @Override
        public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            if (!mSeekAvailable) {
                return;
            }
            if (!fromUser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return;
            }
            if (mDuration > 0) {
                int position = (int) (((long) mDuration * progress) / MAX_PROGRESS);
                mController.seekTo(position);

                if (mCurrentTime != null) {
                    mCurrentTime.setText(stringForTime(position));
                }
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar bar) {
            if (!mSeekAvailable) {
                return;
            }
            mDragging = false;

            setProgress();

            // Ensure that progress is properly updated in the future,
            // the call to show() does not guarantee this because it is a
            // no-op if we are already showing.
            post(mUpdateProgress);
            postDelayed(mHideMainBars, mShowControllerIntervalMs);
        }
    };

    private final OnClickListener mPlayPauseListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            resetHideCallbacks();
            togglePausePlayState();
        }
    };

    private final OnClickListener mRewListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            resetHideCallbacks();
            int pos = getCurrentPosition() - REWIND_TIME_MS;
            mController.seekTo(pos);
            setProgress();
        }
    };

    private final OnClickListener mFfwdListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            resetHideCallbacks();
            int pos = getCurrentPosition() + FORWARD_TIME_MS;
            mController.seekTo(pos);
            setProgress();
        }
    };

    private final OnClickListener mNextListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            resetHideCallbacks();
            mController.skipToNextItem();
        }
    };

    private final OnClickListener mPrevListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            resetHideCallbacks();
            mController.skipToPreviousItem();
        }
    };

    private final OnClickListener mBackListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            View parent = (View) getParent();
            if (parent != null) {
                parent.onKeyDown(KeyEvent.KEYCODE_BACK,
                        new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
            }
        }
    };

    private final OnClickListener mSubtitleListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            removeCallbacks(mHideMainBars);
            removeCallbacks(mHideProgressBar);

            mSettingsMode = SETTINGS_MODE_SUBTITLE_TRACK;
            mSubSettingsAdapter.setTexts(mSubtitleDescriptionsList);
            mSubSettingsAdapter.setCheckPosition(mSelectedSubtitleTrackIndex);
            displaySettingsWindow(mSubSettingsAdapter);
        }
    };

    private final OnClickListener mVideoQualityListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            removeCallbacks(mHideMainBars);
            removeCallbacks(mHideProgressBar);

            mSettingsMode = SETTINGS_MODE_VIDEO_QUALITY;
            mSubSettingsAdapter.setTexts(mVideoQualityList);
            mSubSettingsAdapter.setCheckPosition(mSelectedVideoQualityIndex);
            displaySettingsWindow(mSubSettingsAdapter);
        }
    };

    private final OnClickListener mFullScreenListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            resetHideCallbacks();

            if (mOnFullScreenListener == null) {
                return;
            }

            final boolean isEnteringFullScreen = !mIsFullScreen;
            if (isEnteringFullScreen) {
                mFullScreenButton.setImageDrawable(
                        mResources.getDrawable(R.drawable.ic_fullscreen_exit, null));
            } else {
                mFullScreenButton.setImageDrawable(
                        mResources.getDrawable(R.drawable.ic_fullscreen, null));
            }
            mIsFullScreen = isEnteringFullScreen;
            mOnFullScreenListener.onFullScreen(MediaControlView2.this,
                    mIsFullScreen);
        }
    };

    private final OnClickListener mOverflowShowListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            resetHideCallbacks();

            mOverflowIsShowing = true;
            mOverflowShowAnimator.start();
        }
    };

    private final OnClickListener mOverflowHideListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            resetHideCallbacks();

            mOverflowIsShowing = false;
            mOverflowHideAnimator.start();
        }
    };

    private final OnClickListener mMuteButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            resetHideCallbacks();

            if (!mIsMute) {
                mMuteButton.setImageDrawable(
                        mResources.getDrawable(R.drawable.ic_mute, null));
                mMuteButton.setContentDescription(
                        mResources.getString(R.string.mcv2_muted_button_desc));
                mIsMute = true;
                mController.volumeMute();
            } else {
                mMuteButton.setImageDrawable(
                        mResources.getDrawable(R.drawable.ic_unmute, null));
                mMuteButton.setContentDescription(
                        mResources.getString(R.string.mcv2_unmuted_button_desc));
                mIsMute = false;
                mController.volumeUnmute();
            }
        }
    };

    private final OnClickListener mSettingsButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
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
                        mSubSettingsAdapter.setTexts(mAudioTrackList);
                        mSubSettingsAdapter.setCheckPosition(mSelectedAudioTrackIndex);
                        mSettingsMode = SETTINGS_MODE_AUDIO_TRACK;
                    } else if (position == SETTINGS_MODE_PLAYBACK_SPEED) {
                        mSubSettingsAdapter.setTexts(mPlaybackSpeedTextList);
                        mSubSettingsAdapter.setCheckPosition(mSelectedSpeedIndex);
                        mSettingsMode = SETTINGS_MODE_PLAYBACK_SPEED;
                    } else if (position == SETTINGS_MODE_HELP) {
                        dismissSettingsWindow();
                        return;
                    }
                    displaySettingsWindow(mSubSettingsAdapter);
                    break;
                case SETTINGS_MODE_AUDIO_TRACK:
                    if (position != mSelectedAudioTrackIndex) {
                        mSelectedAudioTrackIndex = position;
                        if (mAudioTrackCount > 0) {
                            mController.selectAudioTrack(position);
                        }
                        mSettingsSubTextsList.set(SETTINGS_MODE_AUDIO_TRACK,
                                mSubSettingsAdapter.getMainText(position));
                    }
                    dismissSettingsWindow();
                    break;
                case SETTINGS_MODE_PLAYBACK_SPEED:
                    if (position != mSelectedSpeedIndex) {
                        mSelectedSpeedIndex = position;
                        float speed = mPlaybackSpeedList.get(position);
                        mController.setSpeed(speed);
                        mSettingsSubTextsList.set(SETTINGS_MODE_PLAYBACK_SPEED,
                                mSubSettingsAdapter.getMainText(position));
                    }
                    dismissSettingsWindow();
                    break;
                case SETTINGS_MODE_HELP:
                    break;
                case SETTINGS_MODE_SUBTITLE_TRACK:
                    if (position != mSelectedSubtitleTrackIndex) {
                        mSelectedSubtitleTrackIndex = position;
                        if (position > 0) {
                            mController.showSubtitle(position - 1);
                            mSubtitleButton.setImageDrawable(
                                    mResources.getDrawable(R.drawable.ic_subtitle_on, null));
                            mSubtitleButton.setContentDescription(
                                    mResources.getString(R.string.mcv2_cc_is_on));
                        } else {
                            mController.hideSubtitle();
                            mSubtitleButton.setImageDrawable(
                                    mResources.getDrawable(R.drawable.ic_subtitle_off, null));
                            mSubtitleButton.setContentDescription(
                                    mResources.getString(R.string.mcv2_cc_is_off));
                        }
                    }
                    dismissSettingsWindow();
                    break;
                case SETTINGS_MODE_VIDEO_QUALITY:
                    mSelectedVideoQualityIndex = position;
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
                        postDelayed(mHideMainBars, mShowControllerIntervalMs);
                    }
                }
            };

    private void updateDuration() {
        if (mController.hasMetadata()) {
            mDuration = mController.getDurationMs();
            setProgress();
        }
    }

    private void updateTitle() {
        if (mController.hasMetadata()) {
            mTitleView.setText(mController.getTitle());
        }
    }

    // The title bar is made up of two separate LinearLayouts. If the sum of the two bars are
    // greater than the length of the title bar, reduce the size of the left bar (which makes the
    // TextView that contains the title of the media file shrink).
    private void updateTitleBarLayout() {
        if (mTitleBar != null) {
            int titleBarWidth = mTitleBar.getWidth();

            View leftBar = mTitleBar.findViewById(R.id.title_bar_left);
            View rightBar = mTitleBar.findViewById(R.id.title_bar_right);
            int leftBarWidth = leftBar.getWidth();
            int rightBarWidth = rightBar.getWidth();

            RelativeLayout.LayoutParams params =
                    (RelativeLayout.LayoutParams) leftBar.getLayoutParams();
            if (leftBarWidth + rightBarWidth > titleBarWidth) {
                params.width = titleBarWidth - rightBarWidth;
                mOriginalLeftBarWidth = leftBarWidth;
            } else if (leftBarWidth + rightBarWidth < titleBarWidth && mOriginalLeftBarWidth != 0) {
                params.width = mOriginalLeftBarWidth;
                mOriginalLeftBarWidth = 0;
            }
            leftBar.setLayoutParams(params);
        }
    }

    private void updateAudioMetadata() {
        if (mMediaType != MEDIA_TYPE_MUSIC) {
            return;
        }
        if (mController.hasMetadata()) {
            String titleText = mController.getTitle();
            String artistText = mController.getArtistText();
            if (titleText == null) {
                titleText = mResources.getString(R.string.mcv2_music_title_unknown_text);
            }
            if (artistText == null) {
                artistText = mResources.getString(R.string.mcv2_music_artist_unknown_text);
            }

            // Update title for Embedded size type
            mTitleView.setText(titleText + " - " + artistText);

            // Set to true to update layout inside onMeasure()
            mNeedUxUpdate = true;
        }
    }

    private void updateLayout() {
        if (mIsAdvertisement) {
            mRewButton.setVisibility(View.GONE);
            mFfwdButton.setVisibility(View.GONE);
            mPrevButton.setVisibility(View.GONE);
            mTimeView.setVisibility(View.GONE);

            mAdSkipView.setVisibility(View.VISIBLE);
            mAdRemainingView.setVisibility(View.VISIBLE);
            mAdExternalLink.setVisibility(View.VISIBLE);

            mProgress.setEnabled(false);
            mNextButton.setEnabled(false);
            mNextButton.setColorFilter(R.color.gray);
        } else {
            mRewButton.setVisibility(View.VISIBLE);
            mFfwdButton.setVisibility(View.VISIBLE);
            mPrevButton.setVisibility(View.VISIBLE);
            mTimeView.setVisibility(View.VISIBLE);

            mAdSkipView.setVisibility(View.GONE);
            mAdRemainingView.setVisibility(View.GONE);
            mAdExternalLink.setVisibility(View.GONE);

            mProgress.setEnabled(true);
            mNextButton.setEnabled(true);
            mNextButton.clearColorFilter();
            disableUnsupportedButtons();
        }
    }

    private void updateLayout(int maxIconCount, int iconSize, int currWidth,
             int currHeight, int screenWidth, int screenHeight) {
        int bottomBarRightWidthMax = iconSize * maxIconCount;
        int fullWidth = mTransportControls.getWidth() + mTimeView.getWidth()
                + bottomBarRightWidthMax;
        int embeddedWidth = mTimeView.getWidth() + bottomBarRightWidthMax;
        int screenMaxLength = Math.max(screenWidth, screenHeight);

        boolean isFullSize = (mMediaType == MEDIA_TYPE_DEFAULT) ? (currWidth == screenMaxLength) :
                (currWidth == screenWidth && currHeight == screenHeight);

        if (isFullSize) {
            if (mSizeType != SIZE_TYPE_FULL) {
                updateLayoutForSizeChange(SIZE_TYPE_FULL);
                if (mMediaType == MEDIA_TYPE_MUSIC) {
                    mTitleView.setVisibility(View.GONE);
                } else {
                    mUxState = UX_STATE_EMPTY;
                    toggleMediaControlViewVisibility();
                }
            }
        } else if (embeddedWidth <= currWidth) {
            if (mSizeType != SIZE_TYPE_EMBEDDED) {
                updateLayoutForSizeChange(SIZE_TYPE_EMBEDDED);
                if (mMediaType == MEDIA_TYPE_MUSIC) {
                    mTitleView.setVisibility(View.VISIBLE);
                }
            }
        } else {
            if (mSizeType != SIZE_TYPE_MINIMAL) {
                updateLayoutForSizeChange(SIZE_TYPE_MINIMAL);
                if (mMediaType == MEDIA_TYPE_MUSIC) {
                    mTitleView.setVisibility(View.GONE);
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void updateLayoutForSizeChange(int sizeType) {
        mSizeType = sizeType;
        RelativeLayout.LayoutParams timeViewParams =
                (RelativeLayout.LayoutParams) mTimeView.getLayoutParams();
        SeekBar seeker = (SeekBar) mProgress;
        switch (mSizeType) {
            case SIZE_TYPE_EMBEDDED:
                // Relating to Title Bar
                mTitleBar.setVisibility(View.VISIBLE);
                mBackButton.setVisibility(View.GONE);
                mTitleView.setPadding(
                        mResources.getDimensionPixelSize(R.dimen.mcv2_embedded_icon_padding),
                        mTitleView.getPaddingTop(),
                        mTitleView.getPaddingRight(),
                        mTitleView.getPaddingBottom());

                // Relating to Full Screen Button
                mMinimalExtraView.setVisibility(View.GONE);
                mFullScreenButton = mBasicControls.findViewById(R.id.fullscreen);
                mFullScreenButton.setOnClickListener(mFullScreenListener);

                // Relating to Center View
                mCenterView.removeAllViews();
                mBottomBarLeftView.removeView(mTransportControls);
                mBottomBarLeftView.setVisibility(View.GONE);
                mTransportControls = inflateTransportControls(R.layout.embedded_transport_controls);
                mCenterView.addView(mTransportControls);

                // Relating to Progress Bar
                GradientDrawable thumb = (GradientDrawable) mResources.getDrawable(
                        R.drawable.custom_progress_thumb);
                if (mUxState == UX_STATE_FULL) {
                    int originalSize = mResources.getDimensionPixelSize(
                            R.dimen.mcv2_custom_progress_thumb_size);
                    thumb.setSize(originalSize, originalSize);
                }
                seeker.setThumb(thumb);
                mProgressBuffer.setVisibility(View.VISIBLE);

                // Relating to Bottom Bar
                mBottomBar.setVisibility(View.VISIBLE);
                if (timeViewParams.getRules()[RelativeLayout.LEFT_OF] != 0) {
                    timeViewParams.removeRule(RelativeLayout.LEFT_OF);
                    timeViewParams.addRule(RelativeLayout.RIGHT_OF, R.id.bottom_bar_left);
                }
                break;
            case SIZE_TYPE_FULL:
                // Relating to Title Bar
                mTitleBar.setVisibility(View.VISIBLE);
                mBackButton.setVisibility(View.VISIBLE);
                mTitleView.setPadding(
                        0,
                        mTitleView.getPaddingTop(),
                        mTitleView.getPaddingRight(),
                        mTitleView.getPaddingBottom());

                // Relating to Full Screen Button
                mMinimalExtraView.setVisibility(View.GONE);
                mFullScreenButton = mBasicControls.findViewById(R.id.fullscreen);
                mFullScreenButton.setOnClickListener(mFullScreenListener);

                // Relating to Center View
                mCenterView.removeAllViews();
                mBottomBarLeftView.removeView(mTransportControls);
                mTransportControls = inflateTransportControls(R.layout.full_transport_controls);
                mBottomBarLeftView.addView(mTransportControls, 0);
                mBottomBarLeftView.setVisibility(View.VISIBLE);

                // Relating to Progress Bar
                seeker.setThumb(mResources.getDrawable(R.drawable.custom_progress_thumb));
                mProgressBuffer.setVisibility(View.VISIBLE);

                // Relating to Bottom Bar
                mBottomBar.setVisibility(View.VISIBLE);
                if (timeViewParams.getRules()[RelativeLayout.RIGHT_OF] != 0) {
                    timeViewParams.removeRule(RelativeLayout.RIGHT_OF);
                    timeViewParams.addRule(RelativeLayout.LEFT_OF, R.id.basic_controls);
                }
                break;
            case SIZE_TYPE_MINIMAL:
                // Relating to Title Bar
                mTitleBar.setVisibility(View.GONE);
                mBackButton.setVisibility(View.GONE);

                // Relating to Full Screen Button
                mMinimalExtraView.setVisibility(View.VISIBLE);
                mFullScreenButton = mMinimalExtraView.findViewById(R.id.minimal_fullscreen);
                mFullScreenButton.setOnClickListener(mFullScreenListener);

                // Relating to Center View
                mCenterView.removeAllViews();
                mBottomBarLeftView.removeView(mTransportControls);
                mTransportControls = inflateTransportControls(R.layout.minimal_transport_controls);
                mCenterView.addView(mTransportControls);

                // Relating to Progress Bar
                seeker.setThumb(null);
                mProgressBuffer.setVisibility(View.GONE);

                // Relating to Bottom Bar
                mBottomBar.setVisibility(View.GONE);
                break;
        }
        mTimeView.setLayoutParams(timeViewParams);

        if (isPlaying()) {
            mPlayPauseButton.setImageDrawable(
                    mResources.getDrawable(R.drawable.ic_pause_circle_filled, null));
            mPlayPauseButton.setContentDescription(
                    mResources.getString(R.string.mcv2_pause_button_desc));
        } else {
            mPlayPauseButton.setImageDrawable(
                    mResources.getDrawable(R.drawable.ic_play_circle_filled, null));
            mPlayPauseButton.setContentDescription(
                    mResources.getString(R.string.mcv2_play_button_desc));
        }

        if (mIsFullScreen) {
            mFullScreenButton.setImageDrawable(
                    mResources.getDrawable(R.drawable.ic_fullscreen_exit, null));
        } else {
            mFullScreenButton.setImageDrawable(
                    mResources.getDrawable(R.drawable.ic_fullscreen, null));
        }
    }

    private View inflateTransportControls(int layoutId) {
        View v = inflateLayout(getContext(), layoutId);
        mPlayPauseButton = v.findViewById(R.id.pause);
        if (mPlayPauseButton != null) {
            mPlayPauseButton.requestFocus();
            mPlayPauseButton.setOnClickListener(mPlayPauseListener);
        }
        mFfwdButton = v.findViewById(R.id.ffwd);
        if (mFfwdButton != null) {
            mFfwdButton.setOnClickListener(mFfwdListener);
            if (mMediaType == MEDIA_TYPE_MUSIC) {
                mFfwdButton.setVisibility(View.GONE);
            }
        }
        mRewButton = v.findViewById(R.id.rew);
        if (mRewButton != null) {
            mRewButton.setOnClickListener(mRewListener);
            if (mMediaType == MEDIA_TYPE_MUSIC) {
                mRewButton.setVisibility(View.GONE);
            }
        }
        mNextButton = v.findViewById(R.id.next);
        if (mNextButton != null) {
            mNextButton.setOnClickListener(mNextListener);
            mNextButton.setVisibility(View.GONE);
        }
        mPrevButton = v.findViewById(R.id.prev);
        if (mPrevButton != null) {
            mPrevButton.setOnClickListener(mPrevListener);
            mPrevButton.setVisibility(View.GONE);
        }
        return v;
    }

    private void initializeSettingsLists() {
        mSettingsMainTextsList = new ArrayList<String>();
        mSettingsMainTextsList.add(
                mResources.getString(R.string.MediaControlView2_audio_track_text));
        mSettingsMainTextsList.add(
                mResources.getString(R.string.MediaControlView2_playback_speed_text));
        mSettingsMainTextsList.add(
                mResources.getString(R.string.MediaControlView2_help_text));

        mSettingsSubTextsList = new ArrayList<String>();
        mSettingsSubTextsList.add(
                mResources.getString(R.string.MediaControlView2_audio_track_none_text));
        mSettingsSubTextsList.add(
                mResources.getStringArray(
                        R.array.MediaControlView2_playback_speeds)[PLAYBACK_SPEED_1x_INDEX]);
        mSettingsSubTextsList.add(RESOURCE_EMPTY);

        mSettingsIconIdsList = new ArrayList<Integer>();
        mSettingsIconIdsList.add(R.drawable.ic_audiotrack);
        mSettingsIconIdsList.add(R.drawable.ic_play_circle_filled);
        mSettingsIconIdsList.add(R.drawable.ic_help);

        mAudioTrackList = new ArrayList<String>();
        mAudioTrackList.add(
                mResources.getString(R.string.MediaControlView2_audio_track_none_text));

        mVideoQualityList = new ArrayList<String>();
        mVideoQualityList.add(
                mResources.getString(R.string.MediaControlView2_video_quality_auto_text));

        mPlaybackSpeedTextList = new ArrayList<String>(Arrays.asList(
                mResources.getStringArray(R.array.MediaControlView2_playback_speeds)));
        // Select the "1x" speed as the default value.
        mSelectedSpeedIndex = PLAYBACK_SPEED_1x_INDEX;

        mPlaybackSpeedList = new ArrayList<Float>();
        int[] speeds = mResources.getIntArray(R.array.speed_multiplied_by_100);
        for (int i = 0; i < speeds.length; i++) {
            float speed = (float) speeds[i] / 100.0f;
            mPlaybackSpeedList.add(speed);
        }
    }

    private void displaySettingsWindow(BaseAdapter adapter) {
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
        mSettingsWindow.showAsDropDown(this, mSettingsWindowMargin,
                mSettingsWindowMargin - height, Gravity.BOTTOM | Gravity.RIGHT);
        mNeedToHideBars = true;
    }

    private void dismissSettingsWindow() {
        mNeedToHideBars = true;
        mSettingsWindow.dismiss();
    }

    private void animateOverflow(ValueAnimator animation) {
        RelativeLayout.LayoutParams extraControlsParams =
                (RelativeLayout.LayoutParams) mExtraControls.getLayoutParams();
        int iconWidth = mResources.getDimensionPixelSize(R.dimen.mcv2_icon_size);
        // Currently, mExtraControls view is set to the right end of the bottom bar
        // view. This animates the view by setting the initial margin value to the
        // negative value of its width ((-2) * iconWidth) and the final margin value
        // to the positive value of the overflow button width (iconWidth).
        int extraControlMargin = (-2 * iconWidth)
                + (int) (3 * iconWidth * (float) animation.getAnimatedValue());
        extraControlsParams.setMargins(0, 0, extraControlMargin, 0);
        mExtraControls.setLayoutParams(extraControlsParams);

        mTimeView.setAlpha(1 - (float) animation.getAnimatedValue());
        mBasicControls.setAlpha(1 - (float) animation.getAnimatedValue());

        if (mSizeType == SIZE_TYPE_FULL) {
            int transportControlMargin =
                    (-1) * (int) (iconWidth * (float) animation.getAnimatedValue());
            LinearLayout.LayoutParams transportControlsParams =
                    (LinearLayout.LayoutParams) mTransportControls.getLayoutParams();
            transportControlsParams.setMargins(transportControlMargin, 0, 0, 0);
            mTransportControls.setLayoutParams(transportControlsParams);

            mFfwdButton.setAlpha(1 - (float) animation.getAnimatedValue());
        }
    }

    private void resetHideCallbacks() {
        removeCallbacks(mHideMainBars);
        removeCallbacks(mHideProgressBar);
        postDelayed(mHideMainBars, mShowControllerIntervalMs);
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

        public void updateSubTexts(List<String> subTexts) {
            mSubTexts = subTexts;
            notifyDataSetChanged();
        }

        public String getMainText(int position) {
            if (mMainTexts != null) {
                if (position < mMainTexts.size()) {
                    return mMainTexts.get(position);
                }
            }
            return RESOURCE_EMPTY;
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
            View row;
            if (mSizeType == SIZE_TYPE_FULL) {
                row = inflateLayout(getContext(), R.layout.full_settings_list_item);
            } else {
                row = inflateLayout(getContext(), R.layout.embedded_settings_list_item);
            }
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
                iconView.setImageDrawable(mResources.getDrawable(mIconIds.get(position), null));
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
            View row;
            if (mSizeType == SIZE_TYPE_FULL) {
                row = inflateLayout(getContext(), R.layout.full_sub_settings_list_item);
            } else {
                row = inflateLayout(getContext(), R.layout.embedded_sub_settings_list_item);
            }
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

    interface ControllerInterface {
        boolean hasMetadata();
        boolean isPlaying();
        int getCurrentPosition();
        long getBufferedPosition();
        boolean canPause();
        boolean canSeekForward();
        boolean canSeekBackward();
        void pause();
        void play();
        void seekTo(int posMs);
        void skipToNextItem();
        void skipToPreviousItem();
        void volumeMute();
        void volumeUnmute();
        void setSpeed(float speed);
        void selectAudioTrack(int trackIndex);
        void showSubtitle(int trackIndex);
        void hideSubtitle();

        int getDurationMs();
        String getTitle();
        String getArtistText();

        void close();
    }

    class Controller2 implements ControllerInterface {
        private MediaController2 mController2;
        private int mPlaybackState;
        private MediaMetadata2 mMediaMetadata2;
        private Executor mCallbackExecutor;
        private SessionCommandGroup2 mAllowedCommands;

        Controller2(SessionToken2 token) {
            mCallbackExecutor =  MainHandlerExecutor
                    .getExecutor(MediaControlView2.this.getContext());
            mController2 = new MediaController2(getContext(), token, mCallbackExecutor,
                    new MediaControllerCallback());
            mPlaybackState = mController2.getPlayerState();
            MediaItem2 currentItem = mController2.getCurrentMediaItem();
            mMediaMetadata2 = currentItem != null ? currentItem.getMetadata() : null;
        }

        @Override
        public boolean hasMetadata() {
            return mMediaMetadata2 != null;
        }
        @Override
        public boolean isPlaying() {
            return mPlaybackState == BaseMediaPlayer.PLAYER_STATE_PLAYING;
        }
        @Override
        public int getCurrentPosition() {
            int currentPosition = (int) mController2.getCurrentPosition();
            return (currentPosition < 0) ? 0 : currentPosition;
        }
        @Override
        public long getBufferedPosition() {
            return mController2.getBufferedPosition();
        }
        @Override
        public boolean canPause() {
            return true;
            //return mAllowedCommands.hasCommand(SessionCommand2.COMMAND_CODE_PLAYBACK_PAUSE);
        }
        @Override
        public boolean canSeekBackward() {
            return true;
            //return mAllowedCommands.hasCommand(SessionCommand2.COMMAND_CODE_SESSION_REWIND);
        }
        @Override
        public boolean canSeekForward() {
            return true;
            //return mAllowedCommands.hasCommand(SessionCommand2.COMMAND_CODE_SESSION_FAST_FORWARD);
        }
        @Override
        public void pause() {
            mController2.pause();
        }
        @Override
        public void play() {
            mController2.play();
        }
        @Override
        public void seekTo(int posMs) {
            mController2.seekTo(posMs);
        }
        @Override
        public void skipToNextItem() {
            mController2.skipToNextItem();
        }
        @Override
        public void skipToPreviousItem() {
            mController2.skipToPreviousItem();
        }
        @Override
        public void volumeMute() {
            mController2.adjustVolume(AudioManager.ADJUST_MUTE,
                    AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        }
        @Override
        public void volumeUnmute() {
            mController2.adjustVolume(AudioManager.ADJUST_UNMUTE,
                    AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        }
        @Override
        public void setSpeed(float speed) {
            mController2.setPlaybackSpeed(speed);
        }
        @Override
        public void selectAudioTrack(int trackIndex) {
            Bundle extra = new Bundle();
            extra.putInt(KEY_SELECTED_AUDIO_INDEX, trackIndex);
            mController2.sendCustomCommand(
                    new SessionCommand2(COMMAND_SELECT_AUDIO_TRACK, null),
                    extra, null);
        }
        @Override
        public void showSubtitle(int trackIndex) {
            Bundle extra = new Bundle();
            extra.putInt(KEY_SELECTED_SUBTITLE_INDEX, trackIndex);
            mController2.sendCustomCommand(
                    new SessionCommand2(COMMAND_SHOW_SUBTITLE, null), extra, null);
        }
        @Override
        public void hideSubtitle() {
            mController2.sendCustomCommand(
                    new SessionCommand2(COMMAND_HIDE_SUBTITLE, null), null, null);
        }
        @Override
        public int getDurationMs() {
            if (mMediaMetadata2 != null) {
                if (mMediaMetadata2.containsKey(MediaMetadata2.METADATA_KEY_DURATION)) {
                    return (int) mMediaMetadata2.getLong(MediaMetadata2.METADATA_KEY_DURATION);
                }
            }
            return -1;
        }
        @Override
        public String getTitle() {
            if (mMediaMetadata2 != null) {
                if (mMediaMetadata2.containsKey(MediaMetadata2.METADATA_KEY_TITLE)) {
                    return mMediaMetadata2.getString(MediaMetadata2.METADATA_KEY_TITLE);
                }
            }
            return null;
        }
        @Override
        public String getArtistText() {
            if (mMediaMetadata2 != null) {
                if (mMediaMetadata2.containsKey(MediaMetadata2.METADATA_KEY_ARTIST)) {
                    return mMediaMetadata2.getString(MediaMetadata2.METADATA_KEY_ARTIST);
                }
            }
            return null;
        }
        @Override
        public void close() {
            mController2.close();
        }

        class MediaControllerCallback extends MediaController2.ControllerCallback {
            @Override
            public void onPlayerStateChanged(@NonNull MediaController2 controller,
                    @BaseMediaPlayer.PlayerState int state) {
                if (DEBUG) {
                    Log.d(TAG, "onPlayerStateChanged(state: " + state + ")");
                }
                mPlaybackState = state;

                // Update pause button depending on playback state for the following two reasons:
                //   1) Need to handle case where app customizes playback state behavior when app
                //      activity is resumed.
                //   2) Need to handle case where the media file reaches end of duration.
                if (mPlaybackState != mPrevState) {
                    switch (mPlaybackState) {
                        case BaseMediaPlayer.PLAYER_STATE_PLAYING:
                            mPlayPauseButton.setImageDrawable(
                                    mResources.getDrawable(
                                            R.drawable.ic_pause_circle_filled, null));
                            mPlayPauseButton.setContentDescription(
                                    mResources.getString(R.string.mcv2_pause_button_desc));
                            removeCallbacks(mUpdateProgress);
                            post(mUpdateProgress);
                            resetHideCallbacks();
                            mIsStopped = false;
                            break;
                        case BaseMediaPlayer.PLAYER_STATE_PAUSED:
                            mPlayPauseButton.setImageDrawable(
                                    mResources.getDrawable(R.drawable.ic_play_circle_filled, null));
                            mPlayPauseButton.setContentDescription(
                                    mResources.getString(R.string.mcv2_play_button_desc));
                            removeCallbacks(mUpdateProgress);
                            break;
                        default:
                            break;
                    }
                    mPrevState = mPlaybackState;
                }
            }

            @Override
            public void onCurrentMediaItemChanged(@NonNull MediaController2 controller,
                    @Nullable MediaItem2 mediaItem) {
                // This logic is for detecting end of playback, but it's not working. (b/79715323)
                Log.d(TAG, "onCurrentMediaItemChanged()" + mediaItem);
                if (mediaItem == null) {
                    mPlayPauseButton.setImageDrawable(
                            mResources.getDrawable(
                                    R.drawable.ic_replay_circle_filled, null));
                    mPlayPauseButton.setContentDescription(
                            mResources.getString(R.string.mcv2_replay_button_desc));
                    mIsStopped = true;
                }
            }

            @Override
            public void onAllowedCommandsChanged(@NonNull MediaController2 controller,
                    @NonNull SessionCommandGroup2 commands) {
                if (DEBUG) {
                    Log.d(TAG, "onAllowedCommandsChanged(): commands: " + commands);
                }
                if (mAllowedCommands.equals(commands)) {
                    return;
                }
                mAllowedCommands = commands;
                if (commands.hasCommand(SessionCommand2.COMMAND_CODE_PLAYBACK_PAUSE)) {
                    mPlayPauseButton.setVisibility(View.VISIBLE);
                } else {
                    mPlayPauseButton.setVisibility(View.GONE);
                }
                if (commands.hasCommand(SessionCommand2.COMMAND_CODE_SESSION_REWIND)
                        && mMediaType != MEDIA_TYPE_MUSIC) {
                    if (mRewButton != null) {
                        mRewButton.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (mRewButton != null) {
                        mRewButton.setVisibility(View.GONE);
                    }
                }
                if (commands.hasCommand(SessionCommand2.COMMAND_CODE_SESSION_FAST_FORWARD)
                        && mMediaType != MEDIA_TYPE_MUSIC) {
                    if (mFfwdButton != null) {
                        mFfwdButton.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (mFfwdButton != null) {
                        mFfwdButton.setVisibility(View.GONE);
                    }
                }
                mSeekAvailable = commands.hasCommand(SessionCommand2.COMMAND_CODE_PLAYBACK_SEEK_TO);
            }

            @Override
            public void onPlaylistChanged(@NonNull MediaController2 controller,
                    @NonNull List<MediaItem2> list,
                    @Nullable MediaMetadata2 metadata) {
                if (DEBUG) {
                    Log.d(TAG, "onPlaylistChanged(): list: " + list);
                }
                // Note that currently MediaControlView2 assumes single media item to play.
                MediaItem2 mediaItem = list.isEmpty() ? null : list.get(0);
                mMediaMetadata2 = (mediaItem != null) ? mediaItem.getMetadata() : null;
                updateDuration();
                updateTitle();
                updateAudioMetadata();
            }

            @Override
            public void onCustomCommand(@NonNull MediaController2 controller,
                    @NonNull SessionCommand2 command, @Nullable Bundle args,
                    @Nullable ResultReceiver receiver) {
                if (DEBUG) {
                    Log.d(TAG, "onCustomCommand(): command: " + command);
                }
                switch (command.getCustomCommand()) {
                    case EVENT_UPDATE_TRACK_STATUS:
                        mVideoTrackCount = (args != null) ? args.getInt(KEY_VIDEO_TRACK_COUNT) : 0;
                        // If there is one or more audio tracks, and this information has not been
                        // reflected into the Settings window yet, automatically check the first
                        // track.
                        // Otherwise, the Audio Track selection will be defaulted to "None".
                        mAudioTrackCount = (args != null) ? args.getInt(KEY_AUDIO_TRACK_COUNT) : 0;
                        mAudioTrackList = new ArrayList<String>();
                        if (mAudioTrackCount > 0) {
                            for (int i = 0; i < mAudioTrackCount; i++) {
                                String track = mResources.getString(
                                        R.string.MediaControlView2_audio_track_number_text, i + 1);
                                mAudioTrackList.add(track);
                            }
                            // Change sub text inside the Settings window.
                            mSettingsSubTextsList.set(SETTINGS_MODE_AUDIO_TRACK,
                                    mAudioTrackList.get(0));
                        } else {
                            mAudioTrackList.add(mResources.getString(
                                    R.string.MediaControlView2_audio_track_none_text));
                        }
                        if (mVideoTrackCount == 0 && mAudioTrackCount > 0) {
                            mMediaType = MEDIA_TYPE_MUSIC;
                        }
                        mSubtitleTrackCount = (args != null)
                                ? args.getInt(KEY_SUBTITLE_TRACK_COUNT) : 0;
                        mSubtitleDescriptionsList = new ArrayList<String>();
                        if (mSubtitleTrackCount > 0) {
                            mSubtitleButton.setVisibility(View.VISIBLE);
                            mSubtitleButton.setEnabled(true);
                            mSubtitleDescriptionsList.add(mResources.getString(
                                    R.string.MediaControlView2_subtitle_off_text));
                            for (int i = 0; i < mSubtitleTrackCount; i++) {
                                String track = mResources.getString(
                                        R.string.MediaControlView2_subtitle_track_number_text,
                                        i + 1);
                                mSubtitleDescriptionsList.add(track);
                            }
                        } else {
                            mSubtitleButton.setVisibility(View.GONE);
                            mSubtitleButton.setEnabled(false);
                        }
                        break;
                    case EVENT_UPDATE_MEDIA_TYPE_STATUS:
                        boolean isAd = (args != null)
                                && args.getBoolean(KEY_STATE_IS_ADVERTISEMENT);
                        if (isAd != mIsAdvertisement) {
                            mIsAdvertisement = isAd;
                            updateLayout();
                        }
                        break;
                }
            }
        }
    }

    class ControllerCompat implements ControllerInterface {
        private MediaControllerCompat mControllerCompat;
        private MediaControllerCompat.TransportControls mControls;
        private MediaMetadataCompat mMediaMetadata;
        private PlaybackStateCompat mPlaybackState;

        ControllerCompat(MediaControllerCompat controllerCompat) {
            mControllerCompat = controllerCompat;
            if (controllerCompat != null) {
                mControls = mControllerCompat.getTransportControls();
                mPlaybackState = mControllerCompat.getPlaybackState();
                mMediaMetadata = mControllerCompat.getMetadata();
                updateDuration();
                updateTitle();
                mControllerCompat.registerCallback(new MediaControllerCompatCallback());
            }
        }

        @Override
        public boolean hasMetadata() {
            return mMediaMetadata != null;
        }
        @Override
        public boolean isPlaying() {
            return mPlaybackState != null
                    && mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING;
        }
        @Override
        public int getCurrentPosition() {
            mPlaybackState = mControllerCompat.getPlaybackState();
            return (mPlaybackState != null) ? (int) mPlaybackState.getPosition() : 0;
        }
        @Override
        public long getBufferedPosition() {
            mPlaybackState = mControllerCompat.getPlaybackState();
            if (mPlaybackState != null) {
                return mPlaybackState.getBufferedPosition();
            }
            return 0;
        }
        @Override
        public boolean canPause() {
            if (mPlaybackState != null) {
                return (mPlaybackState.getActions() & PlaybackStateCompat.ACTION_PAUSE) != 0;
            }
            return true;
        }

        @Override
        public boolean canSeekBackward() {
            if (mPlaybackState != null) {
                return (mPlaybackState.getActions() & PlaybackStateCompat.ACTION_REWIND) != 0;
            }
            return true;
        }
        @Override
        public boolean canSeekForward() {
            if (mPlaybackState != null) {
                return (mPlaybackState.getActions() & PlaybackStateCompat.ACTION_FAST_FORWARD) != 0;
            }
            return true;
        }
        @Override
        public void pause() {
            mControls.pause();
        }
        @Override
        public void play() {
            mControls.play();
        }
        @Override
        public void seekTo(int posMs) {
            mControls.seekTo(posMs);
        }
        @Override
        public void skipToNextItem() {
            mControls.skipToNext();
        }
        @Override
        public void skipToPreviousItem() {
            mControls.skipToPrevious();
        }
        @Override
        public void volumeMute() {
            mControllerCompat.sendCommand(COMMAND_MUTE, null, null);
        }
        @Override
        public void volumeUnmute() {
            mControllerCompat.sendCommand(COMMAND_UNMUTE, null, null);
        }

        @Override
        public void setSpeed(float speed) {
            Bundle extra = new Bundle();
            extra.putFloat(KEY_PLAYBACK_SPEED, speed);
            mControllerCompat.sendCommand(COMMAND_SET_PLAYBACK_SPEED, extra, null);
        }
        @Override
        public void selectAudioTrack(int trackIndex) {
            Bundle extra = new Bundle();
            extra.putInt(KEY_SELECTED_AUDIO_INDEX, trackIndex);
            mControllerCompat.sendCommand(COMMAND_SELECT_AUDIO_TRACK, extra, null);
        }
        @Override
        public void showSubtitle(int trackIndex) {
            Bundle extra = new Bundle();
            extra.putInt(KEY_SELECTED_SUBTITLE_INDEX, trackIndex);
            mControllerCompat.sendCommand(COMMAND_SHOW_SUBTITLE, extra, null);
        }
        @Override
        public void hideSubtitle() {
            mControllerCompat.sendCommand(COMMAND_HIDE_SUBTITLE, null, null);
        }
        @Override
        public int getDurationMs() {
            return mMediaMetadata == null
                    ? 0 : (int) mMediaMetadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        }
        @Override
        public String getTitle() {
            return mMediaMetadata == null
                    ? null : mMediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
        }
        @Override
        public String getArtistText() {
            return mMediaMetadata == null
                    ? null : mMediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
        }
        @Override
        public void close() {
            // Nothing.
        }

        private class MediaControllerCompatCallback extends MediaControllerCompat.Callback {
            @Override
            public void onPlaybackStateChanged(PlaybackStateCompat state) {
                mPlaybackState = state;

                // Update pause button depending on playback state for the following two reasons:
                //   1) Need to handle case where app customizes playback state behavior when app
                //      activity is resumed.
                //   2) Need to handle case where the media file reaches end of duration.
                if (mPlaybackState.getState() != mPrevState) {
                    switch (mPlaybackState.getState()) {
                        case PlaybackStateCompat.STATE_PLAYING:
                            mPlayPauseButton.setImageDrawable(
                                    mResources.getDrawable(
                                            R.drawable.ic_pause_circle_filled, null));
                            mPlayPauseButton.setContentDescription(
                                    mResources.getString(R.string.mcv2_pause_button_desc));
                            removeCallbacks(mUpdateProgress);
                            post(mUpdateProgress);
                            resetHideCallbacks();
                            mIsStopped = false;
                            break;
                        case PlaybackStateCompat.STATE_PAUSED:
                            mPlayPauseButton.setImageDrawable(
                                    mResources.getDrawable(R.drawable.ic_play_circle_filled, null));
                            mPlayPauseButton.setContentDescription(
                                    mResources.getString(R.string.mcv2_play_button_desc));
                            break;
                        case PlaybackStateCompat.STATE_STOPPED:
                            mPlayPauseButton.setImageDrawable(
                                    mResources.getDrawable(
                                            R.drawable.ic_replay_circle_filled, null));
                            mPlayPauseButton.setContentDescription(
                                    mResources.getString(R.string.mcv2_replay_button_desc));
                            mIsStopped = true;
                            break;
                        default:
                            break;
                    }
                    mPrevState = mPlaybackState.getState();
                }

                if (mPlaybackActions != mPlaybackState.getActions()) {
                    long newActions = mPlaybackState.getActions();
                    if ((newActions & PlaybackStateCompat.ACTION_PAUSE) != 0) {
                        mPlayPauseButton.setVisibility(View.VISIBLE);
                    }
                    if ((newActions & PlaybackStateCompat.ACTION_REWIND) != 0
                            && mMediaType != MEDIA_TYPE_MUSIC) {
                        if (mRewButton != null) {
                            mRewButton.setVisibility(View.VISIBLE);
                        }
                    }
                    if ((newActions & PlaybackStateCompat.ACTION_FAST_FORWARD) != 0
                            && mMediaType != MEDIA_TYPE_MUSIC) {
                        if (mFfwdButton != null) {
                            mFfwdButton.setVisibility(View.VISIBLE);
                        }
                    }
                    if ((newActions & PlaybackStateCompat.ACTION_SEEK_TO) != 0) {
                        mSeekAvailable = true;
                    } else {
                        mSeekAvailable = false;
                    }
                    mPlaybackActions = newActions;
                }

                // Add buttons if custom actions are present.
                List<PlaybackStateCompat.CustomAction> customActions =
                        mPlaybackState.getCustomActions();
                mCustomButtons.removeAllViews();
                if (customActions.size() > 0) {
                    for (final PlaybackStateCompat.CustomAction action : customActions) {
                        ImageButton button = new ImageButton(getContext(),
                                null /* AttributeSet */, 0 /* Style */);
                        // Refer Constructor with argument (int defStyleRes) of View.java
                        button.setImageResource(action.getIcon());
                        final String actionString = action.getAction().toString();
                        button.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mControls.sendCustomAction(actionString, action.getExtras());
                                setVisibility(View.VISIBLE);
                            }
                        });
                        mCustomButtons.addView(button);
                    }
                }
            }

            @Override
            public void onMetadataChanged(MediaMetadataCompat metadata) {
                mMediaMetadata = metadata;
                updateDuration();
                updateTitle();
                updateAudioMetadata();
            }

            @Override
            public void onSessionEvent(String event, Bundle extras) {
                switch (event) {
                    case EVENT_UPDATE_TRACK_STATUS:
                        mVideoTrackCount = extras.getInt(KEY_VIDEO_TRACK_COUNT);
                        // If there is one or more audio tracks, and this information has not been
                        // reflected into the Settings window yet, automatically check the first
                        // track.
                        // Otherwise, the Audio Track selection will be defaulted to "None".
                        mAudioTrackCount = extras.getInt(KEY_AUDIO_TRACK_COUNT);
                        mAudioTrackList = new ArrayList<String>();
                        if (mAudioTrackCount > 0) {
                            for (int i = 0; i < mAudioTrackCount; i++) {
                                String track = mResources.getString(
                                        R.string.MediaControlView2_audio_track_number_text, i + 1);
                                mAudioTrackList.add(track);
                            }
                            // Change sub text inside the Settings window.
                            mSettingsSubTextsList.set(SETTINGS_MODE_AUDIO_TRACK,
                                    mAudioTrackList.get(0));
                        } else {
                            mAudioTrackList.add(mResources.getString(
                                    R.string.MediaControlView2_audio_track_none_text));
                        }
                        if (mVideoTrackCount == 0 && mAudioTrackCount > 0) {
                            mMediaType = MEDIA_TYPE_MUSIC;
                        }

                        mSubtitleTrackCount = extras.getInt(KEY_SUBTITLE_TRACK_COUNT);
                        mSubtitleDescriptionsList = new ArrayList<String>();
                        if (mSubtitleTrackCount > 0) {
                            mSubtitleButton.setVisibility(View.VISIBLE);
                            mSubtitleButton.setEnabled(true);
                            mSubtitleDescriptionsList.add(mResources.getString(
                                    R.string.MediaControlView2_subtitle_off_text));
                            for (int i = 0; i < mSubtitleTrackCount; i++) {
                                String track = mResources.getString(
                                        R.string.MediaControlView2_subtitle_track_number_text,
                                        i + 1);
                                mSubtitleDescriptionsList.add(track);
                            }
                        } else {
                            mSubtitleButton.setVisibility(View.GONE);
                            mSubtitleButton.setEnabled(false);
                        }
                        break;
                    case EVENT_UPDATE_MEDIA_TYPE_STATUS:
                        boolean newStatus = extras.getBoolean(KEY_STATE_IS_ADVERTISEMENT);
                        if (newStatus != mIsAdvertisement) {
                            mIsAdvertisement = newStatus;
                            updateLayout();
                        }
                        break;
                }
            }
        }
    }
}
