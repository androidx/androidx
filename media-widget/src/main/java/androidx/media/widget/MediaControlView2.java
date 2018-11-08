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

import static androidx.media2.MediaController.ControllerResult.RESULT_CODE_NOT_SUPPORTED;
import static androidx.media2.MediaController.ControllerResult.RESULT_CODE_SUCCESS;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.ScaleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.media2.MediaController;
import androidx.media2.MediaItem;
import androidx.media2.MediaMetadata;
import androidx.media2.MediaPlayer;
import androidx.media2.MediaSession;
import androidx.media2.SessionCommand;
import androidx.media2.SessionCommandGroup;
import androidx.media2.SessionPlayer;
import androidx.media2.SessionToken;
import androidx.media2.UriMediaItem;
import androidx.mediarouter.app.MediaRouteButton;
import androidx.mediarouter.media.MediaRouteSelector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

/**
 * A View that contains the controls for {@link MediaPlayer}.
 * It provides a wide range of buttons that serve the following functions: play/pause,
 * rewind/fast-forward, skip to next/previous, select subtitle track, enter/exit full screen mode,
 * adjust video quality, select audio track, and adjust playback speed.
 * <p>
 * The easiest way to use a MediaControlView2 is by creating a {@link VideoView2}, which will
 * internally create a MediaControlView2 instance and handle all the commands from buttons inside
 * MediaControlView2. For more information, refer to {@link VideoView2}.
 *
 * It is also possible to create a MediaControlView2 programmatically and add it to a custom video
 * view. In this case, the app will need to create a {@link MediaSession} instance and set
 * {@link SessionToken its token} inside MediaControlView2 by calling
 * {@link #setSession2Token(SessionToken)}. Then MediaControlView2 will create a
 * {@link MediaController} and could send commands to the connected {@link MediaSession session}.
 * By default, the buttons inside MediaControlView2 will not visible unless the corresponding
 * {@link SessionCommand} is marked as allowed. For more details, refer to {@link MediaSession}.
 * <p>
 * Currently, MediaControlView2 animates off-screen in two steps:
 *   1) Title and bottom bars slide up and down respectively and the transport controls fade out,
 *      leaving only the progress bar at the bottom of the view.
 *   2) Progress bar slides down off-screen.
 * <p>
 * In addition, the following customizations are supported:
 * 1) Set focus to the play/pause button by calling {@link #requestPlayButtonFocus()}.
 * 2) Set full screen behavior by calling {@link #setOnFullScreenListener(OnFullScreenListener)}
 *
 */
@TargetApi(Build.VERSION_CODES.P)
@RequiresApi(21) // TODO correct minSdk API use incompatibilities and remove before release.
public class MediaControlView2 extends BaseLayout {
    private static final String TAG = "MediaControlView2";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    static final String KEY_VIDEO_TRACK_COUNT = "VideoTrackCount";
    static final String KEY_AUDIO_TRACK_COUNT = "AudioTrackCount";
    static final String KEY_SUBTITLE_TRACK_COUNT = "SubtitleTrackCount";
    static final String KEY_SUBTITLE_TRACK_LANGUAGE_LIST = "SubtitleTrackLanguageList";
    static final String KEY_SELECTED_AUDIO_INDEX = "SelectedAudioIndex";
    static final String KEY_SELECTED_SUBTITLE_INDEX = "SelectedSubtitleIndex";
    static final String EVENT_UPDATE_TRACK_STATUS = "UpdateTrackStatus";
    static final String KEY_STATE_IS_ADVERTISEMENT = "MediaTypeAdvertisement";
    static final String EVENT_UPDATE_MEDIA_TYPE_STATUS = "UpdateMediaTypeStatus";
    static final String EVENT_UPDATE_SUBTITLE_SELECTED = "UpdateSubtitleSelected";
    static final String EVENT_UPDATE_SUBTITLE_DESELECTED = "UpdateSubtitleDeselected";

    // String for sending command to show subtitle to MediaSession.
    static final String COMMAND_SHOW_SUBTITLE = "showSubtitle";
    // String for sending command to hide subtitle to MediaSession.
    static final String COMMAND_HIDE_SUBTITLE = "hideSubtitle";
    // String for sending command to select audio track to MediaSession.
    static final String COMMAND_SELECT_AUDIO_TRACK = "SelectTrack";

    private static final int SETTINGS_MODE_AUDIO_TRACK = 0;
    private static final int SETTINGS_MODE_PLAYBACK_SPEED = 1;
    private static final int SETTINGS_MODE_SUBTITLE_TRACK = 2;
    private static final int SETTINGS_MODE_VIDEO_QUALITY = 3;
    private static final int SETTINGS_MODE_MAIN = 4;
    private static final int PLAYBACK_SPEED_1x_INDEX = 3;

    private static final int MEDIA_TYPE_DEFAULT = 0;
    private static final int MEDIA_TYPE_MUSIC = 1;
    private static final int MEDIA_TYPE_ADVERTISEMENT = 2;

    private static final int BOTTOM_BAR_RIGHT_VIEW_MAX_ICON_NUM_DEFAULT = 3;
    private static final int BOTTOM_BAR_RIGHT_VIEW_MAX_ICON_NUM_MUSIC = 2;

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

    private static final long DEFAULT_SHOW_CONTROLLER_INTERVAL_MS = 2000;
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
    Controller mController;
    OnFullScreenListener mOnFullScreenListener;
    private AccessibilityManager mAccessibilityManager;
    private WindowManager mWindowManager;
    private int mPrevWidth;
    private int mPrevOrientation;
    private int mOriginalLeftBarWidth;
    private int mMaxTimeViewWidth;
    private int mEmbeddedSettingsItemWidth;
    private int mFullSettingsItemWidth;
    private int mSettingsItemHeight;
    private int mSettingsWindowMargin;
    private int mIconSize;
    int mVideoTrackCount;
    int mAudioTrackCount;
    int mSubtitleTrackCount;
    int mSettingsMode;
    int mSelectedSubtitleTrackIndex;
    int mSelectedAudioTrackIndex;
    int mSelectedVideoQualityIndex;
    int mSelectedSpeedIndex;
    int mMediaType;
    // TODO: Add lock for accessing mSizeType and mUxState (b/111862062)
    int mSizeType;
    int mUxState;
    long mDuration;
    long mPlaybackActions;
    long mShowControllerIntervalMs;
    // TODO: Add lock for accessing mCurrentSeekPosition and mNextSeekPosition (b/111862062)
    long mCurrentSeekPosition;
    long mNextSeekPosition;
    boolean mDragging;
    boolean mIsFullScreen;
    boolean mOverflowIsShowing;
    boolean mIsStopped;
    boolean mSeekAvailable;
    boolean mIsAdvertisement;
    boolean mNeedToHideBars;
    boolean mWasPlaying;

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
    View mTransportControls;
    ImageButton mPlayPauseButton;
    ImageButton mFfwdButton;
    ImageButton mRewButton;
    // TODO: Disable Next/Previous buttons when the current item does not have a next/previous
    // item in the playlist. (b/119159436)
    private ImageButton mNextButton;
    private ImageButton mPrevButton;

    // Relating to Minimal Size Fullscreen View
    private LinearLayout mMinimalSizeFullScreenView;

    // Relating to Progress Bar View
    View mProgressBar;
    ProgressBar mProgress;
    private View mProgressBuffer;

    // Relating to Bottom Bar View
    private ViewGroup mBottomBar;

    // Relating to Bottom Bar Left View
    private ViewGroup mBottomBarLeftView;
    private ViewGroup mTimeView;
    private TextView mEndTime;
    TextView mCurrentTime;
    private TextView mAdSkipView;
    private StringBuilder mFormatBuilder;
    private Formatter mFormatter;

    // Relating to Bottom Bar Right View
    private ViewGroup mBottomBarRightView;
    ViewGroup mBasicControls;
    ViewGroup mExtraControls;
    ViewGroup mCustomButtons;
    ImageButton mSubtitleButton;
    ImageButton mFullScreenButton;
    ImageButton mOverflowShowButton;
    ImageButton mOverflowHideButton;
    private ImageButton mVideoQualityButton;
    private ImageButton mSettingsButton;
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
    List<String> mAudioTrackList;
    List<String> mVideoQualityList;
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
        mController = new Controller();
        // Inflate MediaControlView2 from XML
        mRoot = makeControllerView();
        addView(mRoot);
        mShowControllerIntervalMs = DEFAULT_SHOW_CONTROLLER_INTERVAL_MS;
        mAccessibilityManager = (AccessibilityManager) context.getSystemService(
                Context.ACCESSIBILITY_SERVICE);
    }

    /**
     * Sets MediaSession token to control corresponding MediaSession. It makes it possible to
     * send and receive data between MediaControlView2 and VideoView2.
     */
    public void setSessionToken(@NonNull SessionToken token) {
        mController.setSessionToken(token);
        if (mController.hasMetadata()) {
            updateMetadata();
        }
    }

    /**
     * Registers a callback to be invoked when the fullscreen mode should be changed.
     * This needs to be implemented in order to display the fullscreen button.
     * @param l The callback that will be run
     */
    public void setOnFullScreenListener(@NonNull OnFullScreenListener l) {
        mOnFullScreenListener = l;
        mFullScreenButton.setVisibility(View.VISIBLE);
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
        void onFullScreen(@NonNull View view, boolean fullScreen);
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
        if (mPrevWidth != getMeasuredWidth()) {
            // The following view may not have been initialized yet.
            if (mTimeView.getWidth() == 0) {
                return;
            }

            // Update layout if necessary
            int currWidth = getMeasuredWidth();
            int currHeight = getMeasuredHeight();
            Point screenSize = new Point();
            mWindowManager.getDefaultDisplay().getSize(screenSize);
            if (mMediaType == MEDIA_TYPE_DEFAULT) {
                updateLayout(BOTTOM_BAR_RIGHT_VIEW_MAX_ICON_NUM_DEFAULT, currWidth,
                        currHeight, screenSize.x, screenSize.y);
            } else if (mMediaType == MEDIA_TYPE_MUSIC) {
                updateLayout(BOTTOM_BAR_RIGHT_VIEW_MAX_ICON_NUM_MUSIC, currWidth,
                        currHeight, screenSize.x, screenSize.y);
            }
            mPrevWidth = currWidth;

            // By default, show all bars and hide settings window and overflow view when view size
            // is changed.
            showAllBars();
            hideSettingsAndOverflow();
        }

        // By default, show all bars and hide settings window and overflow view when view
        // orientation is changed.
        int currOrientation = retrieveOrientation();
        if (currOrientation != mPrevOrientation) {
            showAllBars();
            hideSettingsAndOverflow();
            mPrevOrientation = currOrientation;
        }
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

    static View inflateLayout(Context context, int resId) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(resId, null);
    }

    @SuppressWarnings("deprecation")
    private void initControllerView(ViewGroup v) {
        mWindowManager = (WindowManager) getContext().getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);
        mIconSize = mResources.getDimensionPixelSize(R.dimen.mcv2_icon_size);

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

        // Relating to Minimal Size FullScreen View. This view is visible only when the current
        // size type is Minimal and it is a view that stretches from left to right end
        // and helps locate the fullscreen button at the right end of the screen.
        mMinimalSizeFullScreenView = (LinearLayout) v.findViewById(R.id.minimal_fullscreen_view);
        LinearLayout.LayoutParams params =
                (LinearLayout.LayoutParams) mMinimalSizeFullScreenView.getLayoutParams();
        int iconSize = mResources.getDimensionPixelSize(R.dimen.mcv2_icon_size);
        params.setMargins(0, iconSize * (-1), 0, 0);
        mMinimalSizeFullScreenView.setLayoutParams(params);
        mMinimalSizeFullScreenView.setVisibility(View.GONE);

        // Relating to Progress Bar View
        mProgressBar = v.findViewById(R.id.progress_bar);
        mProgress = v.findViewById(R.id.progress);
        if (mProgress != null) {
            if (mProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mProgress;
                seeker.setOnSeekBarChangeListener(mSeekListener);
                seeker.setProgressDrawable(mResources.getDrawable(R.drawable.custom_progress));
                seeker.setThumb(mResources.getDrawable(R.drawable.custom_progress_thumb));
                seeker.setThumbOffset(0);
            }
            mProgress.setMax(MAX_PROGRESS);
        }
        mProgressBuffer = v.findViewById(R.id.progress_buffer);
        mCurrentSeekPosition = SEEK_POSITION_NOT_SET;
        mNextSeekPosition = SEEK_POSITION_NOT_SET;

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
                if (mSizeType != SIZE_TYPE_MINIMAL) {
                    ScaleDrawable thumb = (ScaleDrawable) seekBar.getThumb();
                    if (thumb != null) {
                        thumb.setLevel((int) (MAX_SCALE_LEVEL * alpha));
                    }
                }

                mTransportControls.setAlpha(alpha);
                if (alpha == 0.0f) {
                    mTransportControls.setVisibility(View.GONE);
                } else if (alpha == 1.0f) {
                    setEnabled(false);
                }
                if (mSizeType == SIZE_TYPE_MINIMAL) {
                    mFullScreenButton.setAlpha(alpha);
                    mProgressBar.setAlpha(alpha);
                    if (alpha == 0.0f) {
                        if (mOnFullScreenListener != null) {
                            mFullScreenButton.setVisibility(View.GONE);
                        }
                        mProgressBar.setVisibility(View.GONE);
                    }
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
                if (mSizeType != SIZE_TYPE_MINIMAL) {
                    ScaleDrawable thumb = (ScaleDrawable) seekBar.getThumb();
                    if (thumb != null) {
                        thumb.setLevel((int) (MAX_SCALE_LEVEL * alpha));
                    }
                }

                mTransportControls.setAlpha(alpha);
                if (alpha == 0.0f) {
                    mTransportControls.setVisibility(View.VISIBLE);
                } else if (alpha == 1.0f) {
                    setEnabled(true);
                }
                if (mSizeType == SIZE_TYPE_MINIMAL) {
                    mFullScreenButton.setAlpha(alpha);
                    mProgressBar.setAlpha(alpha);
                    if (alpha == 0.0f) {
                        if (mOnFullScreenListener != null) {
                            mFullScreenButton.setVisibility(View.VISIBLE);
                        }
                        mProgressBar.setVisibility(View.VISIBLE);
                    }
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
                        mUxState = UX_STATE_ONLY_PROGRESS_VISIBLE;
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
                        mUxState = UX_STATE_NONE_VISIBLE;
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
                mUxState = UX_STATE_NONE_VISIBLE;
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
                        mUxState = UX_STATE_ALL_VISIBLE;
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
                mUxState = UX_STATE_ALL_VISIBLE;
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

                if (mSizeType == SIZE_TYPE_FULL && mMediaType == MEDIA_TYPE_DEFAULT) {
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

                if (mSizeType == SIZE_TYPE_FULL && mMediaType == MEDIA_TYPE_DEFAULT) {
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
     * TODO: b/110905302
     */
    private void disableUnsupportedButtons() {
        try {
            if (mPlayPauseButton != null && !mController.canPause()) {
                mPlayPauseButton.setEnabled(false);
            }
            if (mRewButton != null && !mController.canSeekBackward()) {
                mRewButton.setEnabled(false);
            }
            if (mFfwdButton != null && !mController.canSeekForward()) {
                mFfwdButton.setEnabled(false);
            }
            if (mProgress != null && !mController.canSeekBackward()
                    && !mController.canSeekForward()) {
                mProgress.setEnabled(false);
            }
        } catch (IncompatibleClassChangeError ex) {
            // We were given an old version of the interface, that doesn't have
            // the canPause/canSeekXYZ methods. This is OK, it just means we
            // assume the media can be paused and seeked, and so we don't disable
            // the buttons.
        }
    }

    final Runnable mUpdateProgress = new Runnable() {
        @Override
        public void run() {
            boolean isShowing = getVisibility() == View.VISIBLE;
            if (!mDragging && isShowing && mController.isPlaying()) {
                long pos = setProgress();
                postDelayed(mUpdateProgress,
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
        int positionOnProgressBar = 0;
        long currentPosition = mController.getCurrentPosition();
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
            if (mController.getBufferPercentage() < 0) {
                mProgress.setSecondaryProgress(MAX_PROGRESS);
            } else {
                mProgress.setSecondaryProgress((int) mController.getBufferPercentage() * 10);
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
                long remainingTime =
                        (mDuration - currentPosition < 0) ? 0 : (mDuration - currentPosition);
                String remainingTimeText = mResources.getString(
                        R.string.MediaControlView2_ad_remaining_time,
                        stringForTime(remainingTime));
                mAdRemainingView.setText(remainingTimeText);
            }
        }
        return currentPosition;
    }

    void togglePausePlayState() {
        if (mController.isPlaying()) {
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
        if (shouldNotHideBars() || mShowControllerIntervalMs == 0
                || mUxState == UX_STATE_ANIMATING) {
            return;
        }
        removeCallbacks(mHideMainBars);
        removeCallbacks(mHideProgressBar);

        switch (mUxState) {
            case UX_STATE_NONE_VISIBLE:
                post(mShowAllBars);
                break;
            case UX_STATE_ONLY_PROGRESS_VISIBLE:
                post(mShowMainBars);
                break;
            case UX_STATE_ALL_VISIBLE:
                post(mHideAllBars);
                break;
        }
    }

    private final Runnable mShowAllBars = new Runnable() {
        @Override
        public void run() {
            mShowAllBarsAnimator.start();
            if (mController.isPlaying()) {
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
            if (shouldNotHideBars()) {
                return;
            }
            mHideAllBarsAnimator.start();
        }
    };

    Runnable mHideMainBars = new Runnable() {
        @Override
        public void run() {
            if (!mController.isPlaying() || shouldNotHideBars()) {
                return;
            }
            mHideMainBarsAnimator.start();
            postDelayed(mHideProgressBar, mShowControllerIntervalMs);
        }
    };

    final Runnable mHideProgressBar = new Runnable() {
        @Override
        public void run() {
            if (!mController.isPlaying() || shouldNotHideBars()) {
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
                updateForStoppedState(false);
            }

            if (isHttpSchemeUrl(mController.getCurrentMediaItem()) && mController.isPlaying()) {
                mWasPlaying = true;
                mController.pause();
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
            // Check if progress bar is being dragged since this method may be called after
            // onStopTrackingTouch() is called.
            if (mDragging && mDuration > 0) {
                long newPosition = ((mDuration * progress) / MAX_PROGRESS);
                // Do not seek if the current media item has a http scheme URL to improve seek
                // performance.
                boolean shouldSeekNow = !isHttpSchemeUrl(mController.getCurrentMediaItem());
                seekTo(newPosition, shouldSeekNow);
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar bar) {
            if (!mSeekAvailable) {
                return;
            }
            mDragging = false;

            long latestSeekPosition = getLatestSeekPosition();
            // Reset existing seek positions since we only need to seek to the latest position.
            if (isHttpSchemeUrl(mController.getCurrentMediaItem())) {
                mCurrentSeekPosition = SEEK_POSITION_NOT_SET;
                mNextSeekPosition = SEEK_POSITION_NOT_SET;
            }
            seekTo(latestSeekPosition, true);

            if (mWasPlaying) {
                mWasPlaying = false;
                mController.play();
            }
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
            removeCallbacks(mUpdateProgress);

            long latestSeekPosition = getLatestSeekPosition();
            if (mIsStopped && mDuration != 0) {
                // If the media is currently stopped, rewinding will start the media from the
                // beginning. Instead, seek to 10 seconds before the end of the media.
                seekTo(mDuration - REWIND_TIME_MS, true);
                updateForStoppedState(false);
            } else {
                seekTo(Math.max(latestSeekPosition - REWIND_TIME_MS, 0), true);
            }
        }
    };

    private final OnClickListener mFfwdListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            resetHideCallbacks();
            removeCallbacks(mUpdateProgress);

            long latestSeekPosition = getLatestSeekPosition();
            seekTo(Math.min(latestSeekPosition + FORWARD_TIME_MS, mDuration), true);
            if (latestSeekPosition + FORWARD_TIME_MS >= mDuration) {
                // If the media is currently paused, fast-forwarding beyond the duration value will
                // not return a callback that updates the play/pause and ffwd buttons. Thus,
                // update the buttons manually here.
                updateForStoppedState(true);
            }
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
                        float speed = mPlaybackSpeedMultBy100List.get(position) / 100.0f;
                        mController.setSpeed(speed);
                    }
                    dismissSettingsWindow();
                    break;
                case SETTINGS_MODE_SUBTITLE_TRACK:
                    if (position != mSelectedSubtitleTrackIndex) {
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

    void updateMetadata() {
        if (!mController.hasMetadata()) {
            return;
        }

        long duration = mController.getDurationMs();
        if (duration != 0) {
            mDuration = duration;
            mTimeView.setVisibility(View.VISIBLE);
            setProgress();
        }

        if (mMediaType != MEDIA_TYPE_MUSIC) {
            CharSequence title = mController.getTitle();
            if (title != null) {
                mTitleView.setText(title.toString());
            }
        } else {
            CharSequence title = mController.getTitle();
            if (title == null) {
                title = mResources.getString(R.string.mcv2_music_title_unknown_text);
            }
            CharSequence artist = mController.getArtistText();
            if (artist == null) {
                artist = mResources.getString(R.string.mcv2_music_artist_unknown_text);
            }
            // Update title for Embedded size type
            mTitleView.setText(title.toString() + " - " + artist.toString());

            // Remove unnecessary buttons
            mVideoQualityButton.setVisibility(View.GONE);
            if (mFfwdButton != null) {
                mFfwdButton.setVisibility(View.GONE);
            }
            if (mRewButton != null) {
                mRewButton.setVisibility(View.GONE);
            }

            Point screenSize = new Point();
            mWindowManager.getDefaultDisplay().getSize(screenSize);
            updateLayout(BOTTOM_BAR_RIGHT_VIEW_MAX_ICON_NUM_MUSIC, getMeasuredWidth(),
                    getMeasuredHeight(), screenSize.x, screenSize.y);
        }
    }

    void updateLayoutForAd() {
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

    private void updateLayout(int maxIconNum, int currWidth, int currHeight, int screenWidth,
            int screenHeight) {
        if (mMaxTimeViewWidth == 0) {
            // Save the width of the initial time view since it represents the maximum width that
            // this class supports (00:00:00 · 00:00:00).
            mMaxTimeViewWidth = mTimeView.getWidth();
        }
        int bottomBarRightWidthMax = mIconSize * maxIconNum;
        int fullWidth = mTransportControls.getWidth() + mMaxTimeViewWidth + bottomBarRightWidthMax;
        int screenMaxLength = Math.max(screenWidth, screenHeight);
        int embeddedWidth = mMaxTimeViewWidth + bottomBarRightWidthMax;

        // If Media type is default, the size of MCV2 is full only when the current width is equal
        // to the max length of the screen (only landscape mode). If Media type is music, however,
        // the size of MCV2 is full when the current width is equal to the current screen width
        // (both landscape and portrait modes).
        boolean isFullSize = (mMediaType == MEDIA_TYPE_DEFAULT) ? currWidth == screenMaxLength
                : currWidth == screenWidth;
        if (isFullSize) {
            if (mSizeType != SIZE_TYPE_FULL) {
                updateLayoutForSizeChange(SIZE_TYPE_FULL);
                if (mMediaType == MEDIA_TYPE_MUSIC) {
                    mTitleView.setVisibility(View.GONE);
                } else {
                    mUxState = UX_STATE_NONE_VISIBLE;
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
                if (mOnFullScreenListener != null) {
                    mMinimalSizeFullScreenView.setVisibility(View.GONE);
                    mFullScreenButton = mBasicControls.findViewById(R.id.fullscreen);
                    mFullScreenButton.setOnClickListener(mFullScreenListener);
                }

                // Relating to Center View
                mCenterView.removeAllViews();
                mBottomBarLeftView.removeView(mTransportControls);
                mBottomBarLeftView.setVisibility(View.GONE);
                mTransportControls = inflateTransportControls(R.layout.embedded_transport_controls);
                mCenterView.addView(mTransportControls);

                // Relating to Progress Bar
                seeker.setThumb(mResources.getDrawable(R.drawable.custom_progress_thumb));
                seeker.setThumbOffset(0);
                seeker.invalidate();
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
                if (mOnFullScreenListener != null) {
                    mMinimalSizeFullScreenView.setVisibility(View.GONE);
                    mFullScreenButton = mBasicControls.findViewById(R.id.fullscreen);
                    mFullScreenButton.setOnClickListener(mFullScreenListener);
                }

                // Relating to Center View
                mCenterView.removeAllViews();
                mBottomBarLeftView.removeView(mTransportControls);
                mTransportControls = inflateTransportControls(R.layout.full_transport_controls);
                mBottomBarLeftView.addView(mTransportControls, 0);
                mBottomBarLeftView.setVisibility(View.VISIBLE);

                // Relating to Progress Bar
                seeker.setThumb(mResources.getDrawable(R.drawable.custom_progress_thumb));
                seeker.setThumbOffset(0);
                seeker.invalidate();
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
                if (mOnFullScreenListener != null) {
                    mMinimalSizeFullScreenView.setVisibility(View.VISIBLE);
                    mFullScreenButton = mMinimalSizeFullScreenView.findViewById(
                            R.id.minimal_fullscreen);
                    mFullScreenButton.setOnClickListener(mFullScreenListener);
                }

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

        // Update play/pause and ffwd buttons based on whether the media is currently stopped or
        // not.
        updateForStoppedState(mIsStopped);

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
            if (mMediaType == MEDIA_TYPE_MUSIC) {
                mFfwdButton.setVisibility(View.GONE);
            } else {
                mFfwdButton.setOnClickListener(mFfwdListener);
            }
        }
        mRewButton = v.findViewById(R.id.rew);
        if (mRewButton != null) {
            if (mMediaType == MEDIA_TYPE_MUSIC) {
                mRewButton.setVisibility(View.GONE);
            } else {
                mRewButton.setOnClickListener(mRewListener);
            }
        }
        mNextButton = v.findViewById(R.id.next);
        if (mNextButton != null) {
            if (mController.canSkipToNext()) {
                mNextButton.setOnClickListener(mNextListener);
            } else {
                mNextButton.setVisibility(View.GONE);
            }
        }
        mPrevButton = v.findViewById(R.id.prev);
        if (mPrevButton != null) {
            if (mController.canSkipToPrevious()) {
                mPrevButton.setOnClickListener(mPrevListener);
            } else {
                mPrevButton.setVisibility(View.GONE);
            }
        }
        return v;
    }

    private void initializeSettingsLists() {
        mSettingsMainTextsList = new ArrayList<String>();
        mSettingsMainTextsList.add(
                mResources.getString(R.string.MediaControlView2_audio_track_text));
        mSettingsMainTextsList.add(
                mResources.getString(R.string.MediaControlView2_playback_speed_text));

        mSettingsSubTextsList = new ArrayList<String>();
        mSettingsSubTextsList.add(
                mResources.getString(R.string.MediaControlView2_audio_track_none_text));
        String normalSpeed = mResources.getString(R.string.MediaControlView2_playback_speed_normal);
        mSettingsSubTextsList.add(normalSpeed);
        mSettingsSubTextsList.add(RESOURCE_EMPTY);

        mSettingsIconIdsList = new ArrayList<Integer>();
        mSettingsIconIdsList.add(R.drawable.ic_audiotrack);
        mSettingsIconIdsList.add(R.drawable.ic_play_circle_filled);

        mAudioTrackList = new ArrayList<String>();
        mAudioTrackList.add(
                mResources.getString(R.string.MediaControlView2_audio_track_none_text));

        mVideoQualityList = new ArrayList<String>();
        mVideoQualityList.add(
                mResources.getString(R.string.MediaControlView2_video_quality_auto_text));

        mPlaybackSpeedTextList = new ArrayList<String>(Arrays.asList(
                mResources.getStringArray(R.array.MediaControlView2_playback_speeds)));
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

    boolean isHttpSchemeUrl(MediaItem currentMediaItem) {
        if (currentMediaItem == null) {
            return false;
        }

        Uri uri = currentMediaItem instanceof UriMediaItem
                ? ((UriMediaItem) currentMediaItem).getUri() : null;
        if (uri == null) {
            // Something wrong.
            return false;
        }

        String scheme = uri.getScheme();
        if (scheme != null) {
            if (scheme.equals("http") || scheme.equals("https")) {
                return true;
            }
        }
        return false;
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
        mSettingsWindow.showAsDropDown(this, mSettingsWindowMargin,
                mSettingsWindowMargin - height, Gravity.BOTTOM | Gravity.RIGHT);
        mNeedToHideBars = true;
    }

    void dismissSettingsWindow() {
        mNeedToHideBars = true;
        mSettingsWindow.dismiss();
    }

    void animateOverflow(ValueAnimator animation) {
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

        if (mSizeType == SIZE_TYPE_FULL && mMediaType == MEDIA_TYPE_DEFAULT) {
            int transportControlMargin =
                    (-1) * (int) (iconWidth * (float) animation.getAnimatedValue());
            LinearLayout.LayoutParams transportControlsParams =
                    (LinearLayout.LayoutParams) mTransportControls.getLayoutParams();
            transportControlsParams.setMargins(transportControlMargin, 0, 0, 0);
            mTransportControls.setLayoutParams(transportControlsParams);

            mFfwdButton.setAlpha(1 - (float) animation.getAnimatedValue());
        }
    }

    void resetHideCallbacks() {
        removeCallbacks(mHideMainBars);
        removeCallbacks(mHideProgressBar);
        postDelayed(mHideMainBars, mShowControllerIntervalMs);
    }

    void updateAllowedCommands(SessionCommandGroup commands) {
        if (DEBUG) {
            Log.d(TAG, "updateAllowedCommands(): commands: " + commands);
        }

        if (mController.getAllowedCommands() == commands) {
            return;
        }
        mController.setAllowedCommands(commands);

        if (commands.hasCommand(SessionCommand.COMMAND_CODE_PLAYER_PAUSE)) {
            mPlayPauseButton.setVisibility(View.VISIBLE);
            mPlayPauseButton.setEnabled(true);
        } else {
            mPlayPauseButton.setVisibility(View.GONE);
        }
        if (commands.hasCommand(SessionCommand.COMMAND_CODE_SESSION_REWIND)
                && mMediaType != MEDIA_TYPE_MUSIC) {
            if (mRewButton != null) {
                mRewButton.setVisibility(View.VISIBLE);
                mRewButton.setEnabled(true);
            }
        } else {
            if (mRewButton != null) {
                mRewButton.setVisibility(View.GONE);
            }
        }
        if (commands.hasCommand(SessionCommand.COMMAND_CODE_SESSION_FAST_FORWARD)
                && mMediaType != MEDIA_TYPE_MUSIC) {
            if (mFfwdButton != null) {
                mFfwdButton.setVisibility(View.VISIBLE);
                mFfwdButton.setEnabled(true);
            }
        } else {
            if (mFfwdButton != null) {
                mFfwdButton.setVisibility(View.GONE);
            }
        }
        if (commands.hasCommand(
                SessionCommand.COMMAND_CODE_PLAYER_SKIP_TO_PREVIOUS_PLAYLIST_ITEM)) {
            if (mPrevButton != null) {
                mPrevButton.setVisibility(VISIBLE);
                mPrevButton.setEnabled(true);
            }
        } else {
            if (mPrevButton != null) {
                mPrevButton.setVisibility(View.GONE);
            }
        }
        if (commands.hasCommand(
                SessionCommand.COMMAND_CODE_PLAYER_SKIP_TO_NEXT_PLAYLIST_ITEM)) {
            if (mNextButton != null) {
                mNextButton.setVisibility(VISIBLE);
                mNextButton.setEnabled(true);
            }
        } else {
            if (mNextButton != null) {
                mNextButton.setVisibility(View.GONE);
            }
        }
        if (commands.hasCommand(SessionCommand.COMMAND_CODE_PLAYER_SEEK_TO)) {
            mSeekAvailable = true;
            mProgress.setEnabled(true);
        }
        if (commands.hasCommand(new SessionCommand(COMMAND_SHOW_SUBTITLE, null))
                && commands.hasCommand(new SessionCommand(COMMAND_HIDE_SUBTITLE, null))) {
            mSubtitleButton.setVisibility(View.VISIBLE);
        } else {
            mSubtitleButton.setVisibility(View.GONE);
        }
    }

    private int retrieveOrientation() {
        DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
        int width = dm.widthPixels;
        int height = dm.heightPixels;

        return (height > width)
                ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    }

    boolean shouldNotHideBars() {
        return (mMediaType == MEDIA_TYPE_MUSIC && mSizeType == SIZE_TYPE_FULL)
                || mAccessibilityManager.isTouchExplorationEnabled()
                || mController.getPlaybackState() == SessionPlayer.PLAYER_STATE_ERROR
                || mController.getPlaybackState() == SessionPlayer.PLAYER_STATE_IDLE;
    }

    void seekTo(long newPosition, boolean shouldSeekNow) {
        int positionOnProgressBar = (mDuration <= 0)
                ? 0 : (int) (MAX_PROGRESS * newPosition / mDuration);
        mProgress.setProgress(positionOnProgressBar);
        mCurrentTime.setText(stringForTime(newPosition));

        if (mCurrentSeekPosition == SEEK_POSITION_NOT_SET) {
            // If current seek position is not set, update its value and seek now if necessary.
            mCurrentSeekPosition = newPosition;

            if (shouldSeekNow) {
                mController.seekTo(mCurrentSeekPosition);
            }
        } else {
            // If current seek position is already set, update the next seek position.
            mNextSeekPosition = newPosition;
        }
    }

    private void showAllBars() {
        if (mUxState != UX_STATE_ALL_VISIBLE) {
            removeCallbacks(mHideMainBars);
            removeCallbacks(mHideProgressBar);
            // b/112570875
            post(mShowMainBars);
        } else {
            resetHideCallbacks();
        }
    }

    private void hideSettingsAndOverflow() {
        mSettingsWindow.dismiss();
        if (mOverflowIsShowing) {
            mOverflowHideAnimator.start();
        }
    }

    long getLatestSeekPosition() {
        if (mNextSeekPosition != SEEK_POSITION_NOT_SET) {
            return mNextSeekPosition;
        } else if (mCurrentSeekPosition != SEEK_POSITION_NOT_SET) {
            return mCurrentSeekPosition;
        }
        return mController.getCurrentPosition();
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

    void updateForStoppedState(boolean isStopped) {
        if (isStopped) {
            mIsStopped = true;
            if (mPlayPauseButton != null) {
                mPlayPauseButton.setImageDrawable(
                        mResources.getDrawable(R.drawable.ic_replay_circle_filled, null));
                mPlayPauseButton.setContentDescription(
                        mResources.getString(R.string.mcv2_replay_button_desc));
            }
            if (mFfwdButton != null) {
                mFfwdButton.setAlpha(0.5f);
                mFfwdButton.setEnabled(false);
            }
        } else {
            mIsStopped = false;
            if (mPlayPauseButton != null) {
                if (mController.isPlaying()) {
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
            }
            if (mFfwdButton != null) {
                mFfwdButton.setAlpha(1.0f);
                mFfwdButton.setEnabled(true);
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

    class Controller {
        private MediaController mController;
        int mPlaybackState = SessionPlayer.PLAYER_STATE_IDLE;
        int mPrevState = SessionPlayer.PLAYER_STATE_IDLE;
        MediaMetadata mMediaMetadata;
        private Executor mCallbackExecutor;
        SessionCommandGroup mAllowedCommands;

        Controller() {
            mCallbackExecutor = ContextCompat.getMainExecutor(getContext());
        }

        void setSessionToken(SessionToken token) {
            if (mController != null) {
                mController.close();
            }
            mController = new MediaController(getContext(), token, mCallbackExecutor,
                    new MediaControllerCallback());
            mPlaybackState = mController.getPlayerState();
            MediaItem currentItem = mController.getCurrentMediaItem();
            mMediaMetadata = currentItem != null ? currentItem.getMetadata() : null;
        }

        boolean hasMetadata() {
            return mMediaMetadata != null;
        }

        boolean isPlaying() {
            return mPlaybackState == SessionPlayer.PLAYER_STATE_PLAYING;
        }

        long getCurrentPosition() {
            if (mController != null) {
                long currentPosition = mController.getCurrentPosition();
                return (currentPosition < 0) ? 0 : currentPosition;
            }
            return 0;
        }

        long getBufferPercentage() {
            if (mController != null && mDuration != 0) {
                long bufferedPos = mController.getBufferedPosition();
                return (bufferedPos < 0) ? -1 : (bufferedPos * 100 / mDuration);
            }
            return 0;
        }

        int getPlaybackState() {
            if (mController != null) {
                return mController.getPlayerState();
            }
            return SessionPlayer.PLAYER_STATE_IDLE;
        }

        boolean canPause() {
            return mAllowedCommands != null && mAllowedCommands.hasCommand(
                    SessionCommand.COMMAND_CODE_PLAYER_PAUSE);
        }

        boolean canSeekBackward() {
            return mAllowedCommands != null && mAllowedCommands.hasCommand(
                    SessionCommand.COMMAND_CODE_SESSION_REWIND);
        }

        boolean canSeekForward() {
            return mAllowedCommands != null && mAllowedCommands.hasCommand(
                    SessionCommand.COMMAND_CODE_SESSION_FAST_FORWARD);
        }

        boolean canSkipToNext() {
            return mAllowedCommands != null && mAllowedCommands.hasCommand(
                    SessionCommand.COMMAND_CODE_PLAYER_SKIP_TO_NEXT_PLAYLIST_ITEM);
        }

        boolean canSkipToPrevious() {
            return mAllowedCommands != null && mAllowedCommands.hasCommand(
                    SessionCommand.COMMAND_CODE_PLAYER_SKIP_TO_PREVIOUS_PLAYLIST_ITEM);
        }

        void pause() {
            if (mController != null) {
                mController.pause();
            }
        }

        void play() {
            if (mController != null) {
                mController.play();
            }
        }

        void seekTo(long posMs) {
            if (mController != null) {
                mController.seekTo(posMs);
            }
        }

        void skipToNextItem() {
            if (mController != null) {
                mController.skipToNextPlaylistItem();
            }
        }

        void skipToPreviousItem() {
            if (mController != null) {
                mController.skipToPreviousPlaylistItem();
            }
        }

        void setSpeed(float speed) {
            if (mController != null) {
                mController.setPlaybackSpeed(speed);
            }
        }

        void selectAudioTrack(int trackIndex) {
            if (mController != null) {
                Bundle extra = new Bundle();
                extra.putInt(KEY_SELECTED_AUDIO_INDEX, trackIndex);
                mController.sendCustomCommand(
                        new SessionCommand(COMMAND_SELECT_AUDIO_TRACK, null),
                        extra);
            }
        }

        void showSubtitle(int trackIndex) {
            if (mController != null) {
                Bundle extra = new Bundle();
                extra.putInt(KEY_SELECTED_SUBTITLE_INDEX, trackIndex);
                mController.sendCustomCommand(
                        new SessionCommand(COMMAND_SHOW_SUBTITLE, null), extra);
            }
        }

        void hideSubtitle() {
            if (mController != null) {
                mController.sendCustomCommand(
                        new SessionCommand(COMMAND_HIDE_SUBTITLE, null), null);
            }
        }

        long getDurationMs() {
            // TODO Remove this if-block after b/109639439 is fixed.
            if (mMediaMetadata != null) {
                if (mMediaMetadata.containsKey(MediaMetadata.METADATA_KEY_DURATION)) {
                    return mMediaMetadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
                }
            }
            if (mController != null) {
                return mController.getDuration();
            }
            return 0;
        }

        CharSequence getTitle() {
            if (mMediaMetadata != null) {
                if (mMediaMetadata.containsKey(MediaMetadata.METADATA_KEY_TITLE)) {
                    return mMediaMetadata.getText(MediaMetadata.METADATA_KEY_TITLE);
                }
            }
            return null;
        }

        CharSequence getArtistText() {
            if (mMediaMetadata != null) {
                if (mMediaMetadata.containsKey(MediaMetadata.METADATA_KEY_ARTIST)) {
                    return mMediaMetadata.getText(MediaMetadata.METADATA_KEY_ARTIST);
                }
            }
            return null;
        }

        MediaItem getCurrentMediaItem() {
            if (mController != null) {
                return mController.getCurrentMediaItem();
            }
            return null;
        }

        void setAllowedCommands(SessionCommandGroup commands) {
            mAllowedCommands = commands;
        }

        SessionCommandGroup getAllowedCommands() {
            return mAllowedCommands;
        }

        class MediaControllerCallback extends MediaController.ControllerCallback {
            @Override
            public void onPlayerStateChanged(@NonNull MediaController controller,
                    @SessionPlayer.PlayerState int state) {
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
                        case SessionPlayer.PLAYER_STATE_PLAYING:
                            removeCallbacks(mUpdateProgress);
                            post(mUpdateProgress);
                            resetHideCallbacks();
                            updateForStoppedState(false);
                            break;
                        case SessionPlayer.PLAYER_STATE_PAUSED:
                            mPlayPauseButton.setImageDrawable(
                                    mResources.getDrawable(R.drawable.ic_play_circle_filled, null));
                            mPlayPauseButton.setContentDescription(
                                    mResources.getString(R.string.mcv2_play_button_desc));
                            removeCallbacks(mUpdateProgress);
                            break;
                        case SessionPlayer.PLAYER_STATE_ERROR:
                            MediaControlView2.this.setEnabled(false);
                            mPlayPauseButton.setImageDrawable(
                                    mResources.getDrawable(R.drawable.ic_play_circle_filled, null));
                            mPlayPauseButton.setContentDescription(
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
                        default:
                            break;
                    }
                    mPrevState = mPlaybackState;
                }
            }

            @Override
            public void onSeekCompleted(MediaController controller, long position) {
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
                    MediaControlView2.this.mController.seekTo(mNextSeekPosition);
                    mNextSeekPosition = SEEK_POSITION_NOT_SET;
                } else {
                    mCurrentSeekPosition = SEEK_POSITION_NOT_SET;

                    // If the next seek position is not set, start to update progress.
                    removeCallbacks(mUpdateProgress);
                    removeCallbacks(mHideMainBars);
                    post(mUpdateProgress);
                    postDelayed(mHideMainBars, mShowControllerIntervalMs);
                }
            }

            @Override
            public void onCurrentMediaItemChanged(@NonNull MediaController controller,
                    @NonNull MediaItem mediaItem) {
                if (DEBUG) {
                    Log.d(TAG, "onCurrentMediaItemChanged(): " + mediaItem);
                }
                mMediaMetadata = mediaItem.getMetadata();
                updateMetadata();
            }

            @Override
            public void onPlaybackCompleted(MediaController controller) {
                if (DEBUG) {
                    Log.d(TAG, "onPlaybackCompleted()");
                }
                updateForStoppedState(true);
                // The progress bar and current time text may not have been updated.
                mProgress.setProgress(MAX_PROGRESS);
                mCurrentTime.setText(stringForTime(mDuration));
            }

            @Override
            public void onConnected(@NonNull MediaController controller,
                    @NonNull SessionCommandGroup allowedCommands) {
                if (DEBUG) {
                    Log.d(TAG, "onConnected(): " + allowedCommands);
                }
                updateAllowedCommands(allowedCommands);

                MediaItem mediaItem = controller.getCurrentMediaItem();
                if (mediaItem != null) {
                    mMediaMetadata = mediaItem.getMetadata();
                    updateMetadata();
                }
            }

            @Override
            public void onAllowedCommandsChanged(@NonNull MediaController controller,
                    @NonNull SessionCommandGroup commands) {
                updateAllowedCommands(commands);
            }

            @Override
            public void onPlaylistChanged(@NonNull MediaController controller,
                    @NonNull List<MediaItem> list,
                    @Nullable MediaMetadata metadata) {
                if (DEBUG) {
                    Log.d(TAG, "onPlaylistChanged(): list: " + list);
                }
            }

            @Override
            public void onPlaybackSpeedChanged(@NonNull MediaController controller, float speed) {
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
                            R.string.MediaControlView2_custom_playback_speed_text,
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
            public MediaController.ControllerResult onCustomCommand(
                    @NonNull MediaController controller, @NonNull SessionCommand command,
                    @Nullable Bundle args) {
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
                        List<String> subtitleTracksLanguageList = (args != null)
                                ? args.getStringArrayList(KEY_SUBTITLE_TRACK_LANGUAGE_LIST) : null;
                        mSubtitleDescriptionsList = new ArrayList<String>();
                        if (mSubtitleTrackCount > 0) {
                            mSubtitleButton.setAlpha(1.0f);
                            mSubtitleButton.setEnabled(true);
                            mSubtitleDescriptionsList.add(mResources.getString(
                                    R.string.MediaControlView2_subtitle_off_text));
                            for (int i = 0; i < mSubtitleTrackCount; i++) {
                                String lang = subtitleTracksLanguageList.get(i);
                                String track;
                                if (lang.equals("")) {
                                    track = mResources.getString(
                                            R.string.MediaControlView2_subtitle_track_number_text,
                                            i + 1);
                                } else {
                                    track = mResources.getString(
                                            R.string
                                            .MediaControlView2_subtitle_track_number_and_lang_text,
                                            i + 1, lang);
                                }
                                mSubtitleDescriptionsList.add(track);
                            }
                        } else {
                            if (mMediaType == MEDIA_TYPE_MUSIC) {
                                mSubtitleButton.setVisibility(View.GONE);
                            } else {
                                mSubtitleButton.setAlpha(0.5f);
                                mSubtitleButton.setEnabled(false);
                            }
                        }
                        break;
                    case EVENT_UPDATE_MEDIA_TYPE_STATUS:
                        boolean isAd = (args != null)
                                && args.getBoolean(KEY_STATE_IS_ADVERTISEMENT);
                        if (isAd != mIsAdvertisement) {
                            mIsAdvertisement = isAd;
                            updateLayoutForAd();
                        }
                        break;
                    case EVENT_UPDATE_SUBTITLE_SELECTED:
                        int selectedTrackIndex = args != null
                                ? args.getInt(KEY_SELECTED_SUBTITLE_INDEX, -1)
                                : -1;
                        if (selectedTrackIndex < 0 || selectedTrackIndex >= mSubtitleTrackCount) {
                            Log.w(TAG, "Selected subtitle track index (" + selectedTrackIndex
                                    + ") is out of range.");
                            break;
                        }
                        mSelectedSubtitleTrackIndex = selectedTrackIndex + 1;
                        if (mSettingsMode == SETTINGS_MODE_SUBTITLE_TRACK) {
                            mSubSettingsAdapter.setCheckPosition(mSelectedSubtitleTrackIndex);
                        }
                        break;
                    case EVENT_UPDATE_SUBTITLE_DESELECTED:
                        mSelectedSubtitleTrackIndex = 0;
                        if (mSettingsMode == SETTINGS_MODE_SUBTITLE_TRACK) {
                            mSubSettingsAdapter.setCheckPosition(mSelectedSubtitleTrackIndex);
                        }
                        break;
                    default:
                        return new MediaController.ControllerResult(
                                RESULT_CODE_NOT_SUPPORTED, null);
                }
                return new MediaController.ControllerResult(RESULT_CODE_SUCCESS, null);
            }
        }
    }
}
