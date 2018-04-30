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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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

    private static final int MAX_PROGRESS = 1000;
    private static final int DEFAULT_PROGRESS_UPDATE_TIME_MS = 1000;
    private static final int REWIND_TIME_MS = 10000;
    private static final int FORWARD_TIME_MS = 30000;
    private static final int AD_SKIP_WAIT_TIME_MS = 5000;
    private static final int RESOURCE_NON_EXISTENT = -1;
    private static final String RESOURCE_EMPTY = "";

    private Resources mResources;
    private MediaControllerCompat mController;
    private MediaControllerCompat.TransportControls mControls;
    private PlaybackStateCompat mPlaybackState;
    private MediaMetadataCompat mMetadata;
    private OnFullScreenListener mOnFullScreenListener;
    private int mDuration;
    private int mPrevState;
    private int mPrevWidth;
    private int mPrevHeight;
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
    private int mOrientation;
    private long mPlaybackActions;
    private boolean mDragging;
    private boolean mIsFullScreen;
    private boolean mOverflowExpanded;
    private boolean mIsStopped;
    private boolean mSubtitleIsEnabled;
    private boolean mSeekAvailable;
    private boolean mIsAdvertisement;
    private boolean mIsMute;
    private boolean mNeedUXUpdate;

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
    private ImageButton mOverflowButtonRight;
    private ImageButton mOverflowButtonLeft;
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
    }

    /**
     * Sets MediaSession2 token to control corresponding MediaSession2.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void setMediaSessionToken(SessionToken2 token) {
    }

    /**
     * Registers a callback to be invoked when the fullscreen mode should be changed.
     * @param l The callback that will be run
     */
    public void setOnFullScreenListener(OnFullScreenListener l) {
        mOnFullScreenListener = l;
    }

    /**
     * Sets MediaController instance to MediaControlView2, which makes it possible to send and
     * receive data between MediaControlView2 and VideoView2. This method does not need to be called
     * when MediaControlView2 is initialized with VideoView2.
     * @hide TODO: remove once the implementation is revised
     */
    @RestrictTo(LIBRARY_GROUP)
    public void setController(MediaControllerCompat controller) {
        mController = controller;
        if (controller != null) {
            mControls = mController.getTransportControls();
            // Set mMetadata and mPlaybackState to existing MediaSession variables since they may
            // be called before the callback is called
            mPlaybackState = mController.getPlaybackState();
            mMetadata = mController.getMetadata();
            updateDuration();
            updateTitle();

            mController.registerCallback(new MediaControllerCallback());
        }
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
                if (mOverflowButtonRight != null) {
                    mOverflowButtonRight.setVisibility(visibility);
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
        return false;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // Update layout when this view's width changes in order to avoid any UI overlap between
        // transport controls.
        if (mPrevWidth != getMeasuredWidth()
                || mPrevHeight != getMeasuredHeight() || mNeedUXUpdate) {
            // Dismiss SettingsWindow if it is showing.
            mSettingsWindow.dismiss();

            // These views may not have been initialized yet.
            if (mTransportControls.getWidth() == 0 || mTimeView.getWidth() == 0) {
                return;
            }

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
                if (mNeedUXUpdate) {
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
                mNeedUXUpdate = false;

                // Max number of icons inside BottomBarRightView for Music mode is 3.
                int maxIconCount = 3;
                updateLayout(maxIconCount, iconSize, currWidth, currHeight, screenWidth,
                        screenHeight);
            }
            mPrevWidth = currWidth;
            mPrevHeight = currHeight;
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

    ///////////////////////////////////////////////////
    // Protected or private methods
    ///////////////////////////////////////////////////

    private boolean isPlaying() {
        if (mPlaybackState != null) {
            return mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING;
        }
        return false;
    }

    private int getCurrentPosition() {
        mPlaybackState = mController.getPlaybackState();
        if (mPlaybackState != null) {
            return (int) mPlaybackState.getPosition();
        }
        return 0;
    }

    private int getBufferPercentage() {
        if (mDuration == 0) {
            return 0;
        }
        mPlaybackState = mController.getPlaybackState();
        if (mPlaybackState != null) {
            long bufferedPos = mPlaybackState.getBufferedPosition();
            return (bufferedPos == -1) ? -1 : (int) (bufferedPos * 100 / mDuration);
        }
        return 0;
    }

    private boolean canPause() {
        if (mPlaybackState != null) {
            return (mPlaybackState.getActions() & PlaybackStateCompat.ACTION_PAUSE) != 0;
        }
        return true;
    }

    private boolean canSeekBackward() {
        if (mPlaybackState != null) {
            return (mPlaybackState.getActions() & PlaybackStateCompat.ACTION_REWIND) != 0;
        }
        return true;
    }

    private boolean canSeekForward() {
        if (mPlaybackState != null) {
            return (mPlaybackState.getActions() & PlaybackStateCompat.ACTION_FAST_FORWARD) != 0;
        }
        return true;
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
        mBottomBarRightView = v.findViewById(R.id.bottom_bar_right);
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
        mOverflowButtonRight = v.findViewById(R.id.overflow_right);
        if (mOverflowButtonRight != null) {
            mOverflowButtonRight.setOnClickListener(mOverflowRightListener);
        }
        mOverflowButtonLeft = v.findViewById(R.id.overflow_left);
        if (mOverflowButtonLeft != null) {
            mOverflowButtonLeft.setOnClickListener(mOverflowLeftListener);
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
            int pos = setProgress();
            boolean isShowing = getVisibility() == View.VISIBLE;
            if (!mDragging && isShowing && isPlaying()) {
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
            mControls.pause();
            mPlayPauseButton.setImageDrawable(
                    mResources.getDrawable(R.drawable.ic_play_circle_filled, null));
            mPlayPauseButton.setContentDescription(
                    mResources.getString(R.string.mcv2_play_button_desc));
        } else {
            mControls.play();
            mPlayPauseButton.setImageDrawable(
                    mResources.getDrawable(R.drawable.ic_pause_circle_filled, null));
            mPlayPauseButton.setContentDescription(
                    mResources.getString(R.string.mcv2_pause_button_desc));
        }
    }

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
                mControls.seekTo(position);

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
        }
    };

    private final OnClickListener mPlayPauseListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            togglePausePlayState();
        }
    };

    private final OnClickListener mRewListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int pos = getCurrentPosition() - REWIND_TIME_MS;
            mControls.seekTo(pos);
            setProgress();
        }
    };

    private final OnClickListener mFfwdListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int pos = getCurrentPosition() + FORWARD_TIME_MS;
            mControls.seekTo(pos);
            setProgress();
        }
    };

    private final OnClickListener mNextListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mControls.skipToNext();
        }
    };

    private final OnClickListener mPrevListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mControls.skipToPrevious();
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
            mSettingsMode = SETTINGS_MODE_SUBTITLE_TRACK;
            mSubSettingsAdapter.setTexts(mSubtitleDescriptionsList);
            mSubSettingsAdapter.setCheckPosition(mSelectedSubtitleTrackIndex);
            displaySettingsWindow(mSubSettingsAdapter);
        }
    };

    private final OnClickListener mVideoQualityListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mSettingsMode = SETTINGS_MODE_VIDEO_QUALITY;
            mSubSettingsAdapter.setTexts(mVideoQualityList);
            mSubSettingsAdapter.setCheckPosition(mSelectedVideoQualityIndex);
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

    private final OnClickListener mOverflowRightListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mBasicControls.setVisibility(View.GONE);
            mExtraControls.setVisibility(View.VISIBLE);
        }
    };

    private final OnClickListener mOverflowLeftListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mBasicControls.setVisibility(View.VISIBLE);
            mExtraControls.setVisibility(View.GONE);
        }
    };

    private final OnClickListener mMuteButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mIsMute) {
                mMuteButton.setImageDrawable(
                        mResources.getDrawable(R.drawable.ic_mute, null));
                mMuteButton.setContentDescription(
                        mResources.getString(R.string.mcv2_muted_button_desc));
                mIsMute = true;
                mController.sendCommand(COMMAND_MUTE, null, null);
            } else {
                mMuteButton.setImageDrawable(
                        mResources.getDrawable(R.drawable.ic_unmute, null));
                mMuteButton.setContentDescription(
                        mResources.getString(R.string.mcv2_unmuted_button_desc));
                mIsMute = false;
                mController.sendCommand(COMMAND_UNMUTE, null, null);
            }
        }
    };

    private final OnClickListener mSettingsButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
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
                        mSettingsWindow.dismiss();
                        return;
                    }
                    displaySettingsWindow(mSubSettingsAdapter);
                    break;
                case SETTINGS_MODE_AUDIO_TRACK:
                    if (position != mSelectedAudioTrackIndex) {
                        mSelectedAudioTrackIndex = position;
                        if (mAudioTrackCount > 0) {
                            Bundle extra = new Bundle();
                            extra.putInt(KEY_SELECTED_AUDIO_INDEX, position);
                            mController.sendCommand(COMMAND_SELECT_AUDIO_TRACK, extra, null);
                        }
                        mSettingsSubTextsList.set(SETTINGS_MODE_AUDIO_TRACK,
                                mSubSettingsAdapter.getMainText(position));
                    }
                    mSettingsWindow.dismiss();
                    break;
                case SETTINGS_MODE_PLAYBACK_SPEED:
                    if (position != mSelectedSpeedIndex) {
                        mSelectedSpeedIndex = position;
                        Bundle extra = new Bundle();
                        extra.putFloat(KEY_PLAYBACK_SPEED, mPlaybackSpeedList.get(position));
                        mController.sendCommand(COMMAND_SET_PLAYBACK_SPEED, extra, null);
                        mSettingsSubTextsList.set(SETTINGS_MODE_PLAYBACK_SPEED,
                                mSubSettingsAdapter.getMainText(position));
                    }
                    mSettingsWindow.dismiss();
                    break;
                case SETTINGS_MODE_HELP:
                    break;
                case SETTINGS_MODE_SUBTITLE_TRACK:
                    if (position != mSelectedSubtitleTrackIndex) {
                        mSelectedSubtitleTrackIndex = position;
                        if (position > 0) {
                            Bundle extra = new Bundle();
                            extra.putInt(KEY_SELECTED_SUBTITLE_INDEX, position - 1);
                            mController.sendCommand(COMMAND_SHOW_SUBTITLE, extra, null);
                            mSubtitleButton.setImageDrawable(
                                    mResources.getDrawable(R.drawable.ic_subtitle_on, null));
                            mSubtitleButton.setContentDescription(
                                    mResources.getString(R.string.mcv2_cc_is_on));
                            mSubtitleIsEnabled = true;
                        } else {
                            mController.sendCommand(COMMAND_HIDE_SUBTITLE, null, null);
                            mSubtitleButton.setImageDrawable(
                                    mResources.getDrawable(R.drawable.ic_subtitle_off, null));
                            mSubtitleButton.setContentDescription(
                                    mResources.getString(R.string.mcv2_cc_is_off));
                            mSubtitleIsEnabled = false;
                        }
                    }
                    mSettingsWindow.dismiss();
                    break;
                case SETTINGS_MODE_VIDEO_QUALITY:
                    mSelectedVideoQualityIndex = position;
                    mSettingsWindow.dismiss();
                    break;
            }
        }
    };

    private void updateDuration() {
        if (mMetadata != null) {
            if (mMetadata.containsKey(MediaMetadataCompat.METADATA_KEY_DURATION)) {
                mDuration = (int) mMetadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
                // update progress bar
                setProgress();
            }
        }
    }

    private void updateTitle() {
        if (mMetadata != null) {
            if (mMetadata.containsKey(MediaMetadataCompat.METADATA_KEY_TITLE)) {
                mTitleView.setText(mMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
            }
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

        if (mMetadata != null) {
            String titleText = "";
            String artistText = "";
            if (mMetadata.containsKey(MediaMetadataCompat.METADATA_KEY_TITLE)) {
                titleText = mMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
            } else {
                titleText = mResources.getString(R.string.mcv2_music_title_unknown_text);
            }

            if (mMetadata.containsKey(MediaMetadataCompat.METADATA_KEY_ARTIST)) {
                artistText = mMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
            } else {
                artistText = mResources.getString(R.string.mcv2_music_artist_unknown_text);
            }

            // Update title for Embedded size type
            mTitleView.setText(titleText + " - " + artistText);

            // Set to true to update layout inside onMeasure()
            mNeedUXUpdate = true;
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
                mFullScreenButton = mBottomBarRightView.findViewById(R.id.fullscreen);
                mFullScreenButton.setOnClickListener(mFullScreenListener);

                // Relating to Center View
                mCenterView.removeAllViews();
                mBottomBarLeftView.removeView(mTransportControls);
                mBottomBarLeftView.setVisibility(View.GONE);
                mTransportControls = inflateTransportControls(R.layout.embedded_transport_controls);
                mCenterView.addView(mTransportControls);

                // Relating to Progress Bar
                seeker.setThumb(mResources.getDrawable(R.drawable.custom_progress_thumb));
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
                mFullScreenButton = mBottomBarRightView.findViewById(R.id.fullscreen);
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
                    timeViewParams.addRule(RelativeLayout.LEFT_OF, R.id.bottom_bar_right);
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

        // Calculate height of window and show
        int totalHeight = adapter.getCount() * mSettingsItemHeight;
        mSettingsWindow.dismiss();
        mSettingsWindow.showAsDropDown(this, mSettingsWindowMargin,
                mSettingsWindowMargin - totalHeight, Gravity.BOTTOM | Gravity.RIGHT);
    }

    private class MediaControllerCallback extends MediaControllerCompat.Callback {
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
                                mResources.getDrawable(R.drawable.ic_pause_circle_filled, null));
                        mPlayPauseButton.setContentDescription(
                                mResources.getString(R.string.mcv2_pause_button_desc));
                        removeCallbacks(mUpdateProgress);
                        post(mUpdateProgress);
                        break;
                    case PlaybackStateCompat.STATE_PAUSED:
                        mPlayPauseButton.setImageDrawable(
                                mResources.getDrawable(R.drawable.ic_play_circle_filled, null));
                        mPlayPauseButton.setContentDescription(
                                mResources.getString(R.string.mcv2_play_button_desc));
                        break;
                    case PlaybackStateCompat.STATE_STOPPED:
                        mPlayPauseButton.setImageDrawable(
                                mResources.getDrawable(R.drawable.ic_replay_circle_filled, null));
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
            mMetadata = metadata;
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
                    // reflected into the Settings window yet, automatically check the first track.
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
                                    R.string.MediaControlView2_subtitle_track_number_text, i + 1);
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
}
