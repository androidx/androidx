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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.SeekBarJellybean;
import android.support.v7.mediarouter.R;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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

    // STOPSHIP: Remove the flag when the group volume control implementation completes.
    private static final boolean USE_GROUP = false;

    // Time to wait before updating the volume when the user lets go of the seek bar
    // to allow the route provider time to propagate the change and publish a new
    // route descriptor.
    private static final int VOLUME_UPDATE_DELAY_MILLIS = 250;

    private final MediaRouter mRouter;
    private final MediaRouterCallback mCallback;
    private final MediaRouter.RouteInfo mRoute;

    private boolean mCreated;
    private boolean mAttachedToWindow;

    private int mOrientation;
    private int mDialogWidthPortrait;
    private int mDialogWidthLandscape;
    private int mDialogPaddingHorizontal;

    private View mCustomControlView;

    private Button mDisconnectButton;
    private Button mStopCastingButton;
    private ImageButton mPlayPauseButton;
    private ImageButton mCloseButton;
    private ImageButton mGroupExpandCollapseButton;

    private FrameLayout mCustomControlLayout;
    private FrameLayout mDefaultControlLayout;
    private boolean mNeedToAdjustControlFrameLayout = true;
    private ImageView mArtView;
    private TextView mTitleView;
    private TextView mSubtitleView;
    private TextView mRouteNameTextView;

    private boolean mVolumeControlEnabled = true;
    private LinearLayout mControlLayout;
    private RelativeLayout mPlaybackControl;
    private LinearLayout mVolumeControl;
    private View mDividerView;

    private ListView mVolumeGroupList;
    private SeekBar mVolumeSlider;
    private boolean mVolumeSliderTouched;
    private int mVolumeSliderColor;

    private MediaControllerCompat mMediaController;
    private MediaControllerCallback mControllerCallback;
    private PlaybackStateCompat mState;
    private MediaDescriptionCompat mDescription;

    private FetchArtTask mFetchArtTask;
    private boolean mIsGroupExpanded;

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
     * Gets the description being used by the default UI.
     *
     * @return The current description.
     */
    public MediaSessionCompat.Token getMediaSession() {
        return mMediaController == null ? null : mMediaController.getSessionToken();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.mr_controller_material_dialog_b);
        Resources res = getContext().getResources();
        mDialogWidthPortrait = res.getDimensionPixelSize(R.dimen.mr_dialog_fixed_width_minor);
        mDialogWidthLandscape = res.getDimensionPixelSize(R.dimen.mr_dialog_fixed_width_major);

        ClickListener listener = new ClickListener();

        mDisconnectButton = (Button) findViewById(R.id.mr_button_disconnect);
        mDisconnectButton.setOnClickListener(listener);

        mStopCastingButton = (Button) findViewById(R.id.mr_button_stop);
        mStopCastingButton.setOnClickListener(listener);

        mRouteNameTextView = (TextView) findViewById(R.id.mr_name);
        mCloseButton = (ImageButton) findViewById(R.id.mr_close);
        mCloseButton.setOnClickListener(listener);
        mCustomControlLayout = (FrameLayout) findViewById(R.id.mr_custom_control);
        mDefaultControlLayout = (FrameLayout) findViewById(R.id.mr_default_control);
        ViewTreeObserver observer = mDefaultControlLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                updateControlFrameLayout();
            }
        });
        mArtView = (ImageView) findViewById(R.id.mr_art);

        mControlLayout = (LinearLayout) findViewById(R.id.mr_control);
        mDividerView = findViewById(R.id.mr_control_divider);

        mPlaybackControl = (RelativeLayout) findViewById(R.id.mr_playback_control);
        mTitleView = (TextView) findViewById(R.id.mr_control_title);
        mSubtitleView = (TextView) findViewById(R.id.mr_control_subtitle);
        mPlayPauseButton = (ImageButton) findViewById(R.id.mr_control_play_pause);
        mPlayPauseButton.setOnClickListener(listener);

        mVolumeControl = (LinearLayout) findViewById(R.id.mr_volume_control);
        mVolumeSlider = (SeekBar) findViewById(R.id.mr_volume_slider);
        mVolumeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                    mRoute.requestSetVolume(progress);
                }
            }
        });
        mVolumeSliderColor = MediaRouterThemeHelper.getVolumeSliderColor(getContext());
        setVolumeSliderColor(mVolumeSlider, mVolumeSliderColor);

        TypedArray styledAttributes = getContext().obtainStyledAttributes(new int[] {
                R.attr.mediaRouteExpandGroupDrawable,
                R.attr.mediaRouteCollapseGroupDrawable
        });
        final Drawable expandGroupDrawable = styledAttributes.getDrawable(0);
        final Drawable collapseGroupDrawable = styledAttributes.getDrawable(1);
        styledAttributes.recycle();

        mVolumeGroupList = (ListView)findViewById(R.id.mr_volume_group_list);
        mGroupExpandCollapseButton = (ImageButton) findViewById(R.id.mr_group_expand_collapse);
        mGroupExpandCollapseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsGroupExpanded = !mIsGroupExpanded;
                if (mIsGroupExpanded) {
                    mGroupExpandCollapseButton.setImageDrawable(collapseGroupDrawable);
                    mVolumeGroupList.setVisibility(View.VISIBLE);
                    mVolumeGroupList.setAdapter(
                            new VolumeGroupAdapter(getContext(), getGroup().getRoutes()));
                } else {
                    mGroupExpandCollapseButton.setImageDrawable(expandGroupDrawable);
                    mVolumeGroupList.setVisibility(View.GONE);
                }
                mNeedToAdjustControlFrameLayout = true;
            }
        });

        mCustomControlView = onCreateMediaControlView(savedInstanceState);
        if (mCustomControlView != null) {
            mCustomControlLayout.addView(mCustomControlView);
            mCustomControlLayout.setVisibility(View.VISIBLE);
            mArtView.setVisibility(View.GONE);
        }

        mCreated = true;
        update();
    }

    /**
     * Called by {@link MediaRouteControllerDialogFragment} when the device configuration
     * is changed.
     */
    void onConfigurationChanged(Configuration newConfig) {
        onOrientationChanged(newConfig.orientation);
    }

    private void onOrientationChanged(int orientation) {
        if (!mAttachedToWindow || mOrientation == orientation) {
            return;
        }
        mOrientation = orientation;
        int dialogWidth = mOrientation == Configuration.ORIENTATION_LANDSCAPE
                ? mDialogWidthLandscape : mDialogWidthPortrait;
        getWindow().setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);

        View decorView = getWindow().getDecorView();
        mDialogPaddingHorizontal = decorView.getPaddingLeft() + decorView.getPaddingRight();

        // Manually set the width of buttons to workaround unexpected text alignment changes.
        int buttonWidth = (dialogWidth - mDialogPaddingHorizontal) / 2;

        ViewGroup.LayoutParams lp = mStopCastingButton.getLayoutParams();
        lp.width = buttonWidth;
        mStopCastingButton.setLayoutParams(lp);

        lp = mDisconnectButton.getLayoutParams();
        lp.width = buttonWidth;
        mDisconnectButton.setLayoutParams(lp);

        updateArtView();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachedToWindow = true;

        mRouter.addCallback(MediaRouteSelector.EMPTY, mCallback,
                MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS);
        setMediaSession(mRouter.getMediaSessionToken());
        onOrientationChanged(getContext().getResources().getConfiguration().orientation);
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
        mDisconnectButton.setVisibility(mRoute.canDisconnect() ? View.VISIBLE : View.INVISIBLE);

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

    private void updateVolumeControl() {
        if (!mVolumeSliderTouched) {
            if (isVolumeControlAvailable()) {
                mVolumeControl.setVisibility(View.VISIBLE);
                mVolumeSlider.setMax(mRoute.getVolumeMax());
                mVolumeSlider.setProgress(mRoute.getVolume());
                if (USE_GROUP) {
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
                }
            } else {
                mVolumeControl.setVisibility(View.GONE);
            }
            updateControlLayoutHeight();
        }
    }

    private void updatePlaybackControl() {
        if (mCustomControlView == null && (mDescription != null || mState != null)) {
            mPlaybackControl.setVisibility(View.VISIBLE);
            CharSequence title = mDescription == null ? null : mDescription.getTitle();
            boolean hasTitle = !TextUtils.isEmpty(title);

            CharSequence subtitle = mDescription == null ? null : mDescription.getSubtitle();
            boolean hasSubtitle = !TextUtils.isEmpty(subtitle);

            if (!hasTitle && !hasSubtitle) {
                if (mRoute.getPresentationDisplayId()
                        != MediaRouter.RouteInfo.PRESENTATION_DISPLAY_ID_NONE) {
                    // The user is currently casting screen.
                    mTitleView.setText(R.string.mr_controller_casting_screen);
                } else {
                    mTitleView.setText((mState == null
                            || mState.getState() == PlaybackStateCompat.STATE_NONE)
                                    ? R.string.mr_controller_no_media_selected
                                    : R.string.mr_controller_no_info_available);
                }
                mTitleView.setVisibility(View.VISIBLE);
                mSubtitleView.setVisibility(View.GONE);
            } else {
                mTitleView.setText(title);
                mTitleView.setVisibility(hasTitle ? View.VISIBLE : View.GONE);
                mSubtitleView.setText(subtitle);
                mSubtitleView.setVisibility(hasSubtitle ? View.VISIBLE : View.GONE);
            }
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
        } else {
            mPlaybackControl.setVisibility(View.GONE);
        }
        updateControlLayoutHeight();
    }

    private void updateControlLayoutHeight() {
        // TODO: Update the top and bottom padding of the control layout according to the display
        // height.
        mDividerView.setVisibility((mVolumeControl.getVisibility() == View.VISIBLE
                && mPlaybackControl.getVisibility() == View.VISIBLE)
                ? View.VISIBLE : View.GONE);
        mControlLayout.setVisibility((mVolumeControl.getVisibility() == View.GONE
                && mPlaybackControl.getVisibility() == View.GONE)
                ? View.GONE : View.VISIBLE);
    }

    private void updateControlFrameLayout() {
        if (!mNeedToAdjustControlFrameLayout) {
            return;
        }
        int height;
        if (mArtView.getVisibility() == View.GONE) {
            height = LinearLayout.LayoutParams.WRAP_CONTENT;
        } else if (!mIsGroupExpanded) {
            height = mArtView.getHeight() + mControlLayout.getHeight();
        } else {
            if (mVolumeGroupList.getAdapter().getCount() <= 2) {
                // Push the controls up and cover the artwork.
                height = mArtView.getHeight() + mControlLayout.getHeight()
                        - mVolumeGroupList.getHeight();
            } else {
                // If there are 3 or more, the controls completely cover the artwork.
                height = mControlLayout.getHeight();
            }
        }
        mDefaultControlLayout.setLayoutParams(
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, height));
        mNeedToAdjustControlFrameLayout = false;
    }

    private boolean isVolumeControlAvailable() {
        return mVolumeControlEnabled && mRoute.getVolumeHandling() ==
                MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE;
    }

    private void updateArtView() {
        mNeedToAdjustControlFrameLayout = true;
        if (!(mArtView.getDrawable() instanceof BitmapDrawable)) {
            mArtView.setVisibility(View.GONE);
            return;
        }
        Bitmap art = ((BitmapDrawable) mArtView.getDrawable()).getBitmap();
        if (art == null) {
            mArtView.setVisibility(View.GONE);
            return;
        }
        int desiredArtHeight = getDesiredArtHeight(art.getWidth(), art.getHeight());
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int dialogWidth = displayMetrics.widthPixels < displayMetrics.heightPixels
                ? mDialogWidthPortrait : mDialogWidthLandscape;
        View decorView = getWindow().getDecorView();
        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(dialogWidth, MeasureSpec.EXACTLY);
        decorView.measure(widthMeasureSpec, MeasureSpec.UNSPECIFIED);
        // Show art if and only if it fits in the screen.
        if (mArtView.getVisibility() == View.GONE) {
            if (decorView.getMeasuredHeight() + desiredArtHeight <= displayMetrics.heightPixels) {
                mArtView.setVisibility(View.VISIBLE);
                mArtView.setMaxHeight(desiredArtHeight);
            }
        } else {
            if (decorView.getMeasuredHeight() - mArtView.getMeasuredHeight() + desiredArtHeight
                    <= displayMetrics.heightPixels) {
                mArtView.setMaxHeight(desiredArtHeight);
            } else {
                mArtView.setVisibility(View.GONE);
            }
        }
    }

    private static void setVolumeSliderColor(SeekBar volumeSlider, int color) {
        volumeSlider.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        if (Build.VERSION.SDK_INT >= 16) {
            SeekBarJellybean.getThumb(volumeSlider).setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
    }

    /**
     * Returns desired art height to fit into controller dialog.
     */
    private int getDesiredArtHeight(int originalWidth, int originalHeight) {
        int dialogWidth = getWindow().getAttributes().width - mDialogPaddingHorizontal;
        if (originalWidth >= originalHeight) {
            // For landscape art, fit width to dialog width.
            return (int) ((float) dialogWidth * originalHeight / originalWidth + 0.5f);
        }
        // For portrait art, fit height to 16:9 ratio case's height.
        return (int) ((float) dialogWidth * 9 / 16 + 0.5f);
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
            if (id == R.id.mr_button_stop || id == R.id.mr_button_disconnect) {
                if (mRoute.isSelected()) {
                    mRouter.unselect(id == R.id.mr_button_stop ?
                            MediaRouter.UNSELECT_REASON_STOPPED :
                            MediaRouter.UNSELECT_REASON_DISCONNECTED);
                }
                dismiss();
            } else if (id == R.id.mr_control_play_pause) {
                if (mMediaController != null && mState != null) {
                    if (mState.getState() == PlaybackStateCompat.STATE_PLAYING) {
                        mMediaController.getTransportControls().pause();
                    } else {
                        mMediaController.getTransportControls().play();
                    }
                }
            } else if (id == R.id.mr_close) {
                dismiss();
            }
        }
    }

    private class VolumeGroupAdapter extends ArrayAdapter<MediaRouter.RouteInfo> {
        final static float DISABLED_ALPHA = .3f;

        final OnSeekBarChangeListener mOnSeekBarChangeListener = new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int position = (int) seekBar.getTag();
                    getGroup().getRouteAt(position).requestSetVolume(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO: Implement
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO: Implement
            }
        };

        public VolumeGroupAdapter(Context context, List<MediaRouter.RouteInfo> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = LayoutInflater.from(getContext()).inflate(
                        R.layout.mr_controller_volume_item, null);
                setVolumeSliderColor(
                        (SeekBar) v.findViewById(R.id.mr_volume_slider), mVolumeSliderColor);
            }
            MediaRouter.RouteInfo route = getItem(position);
            if (route != null) {
                boolean isEnabled = route.isEnabled();

                TextView routeName = (TextView) v.findViewById(R.id.mr_name);
                routeName.setEnabled(isEnabled);
                routeName.setText(route.getName());

                SeekBar volumeSlider = (SeekBar) v.findViewById(R.id.mr_volume_slider);
                volumeSlider.setEnabled(isEnabled);
                volumeSlider.setTag(position);
                if (isEnabled) {
                    if (route.getVolumeHandling()
                            == MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE) {
                        volumeSlider.setMax(route.getVolumeMax());
                        volumeSlider.setProgress(route.getVolume());
                        volumeSlider.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
                    } else {
                        volumeSlider.setMax(100);
                        volumeSlider.setProgress(100);
                        volumeSlider.setEnabled(false);
                    }
                }
                if (Build.VERSION.SDK_INT >= 16) {
                    SeekBarJellybean.getThumb(volumeSlider).mutate().setAlpha(isEnabled ? 255 : 0);
                    // TODO: Still see an artifact even though the thumb is transparent. Remove it.
                }

                ImageView volumeItemIcon =
                        (ImageView) v.findViewById(R.id.mr_volume_item_icon);
                volumeItemIcon.setAlpha(isEnabled ? 255 : (int) (255 * DISABLED_ALPHA));
            }
            return v;
        }
    }

    private class FetchArtTask extends AsyncTask<Void, Void, Bitmap> {
        private int mBackgroundColor;

        @Override
        protected Bitmap doInBackground(Void... arg) {
            Bitmap art = null;
            if (mDescription == null) {
                return null;
            }
            if (mDescription.getIconBitmap() != null) {
                art = mDescription.getIconBitmap();
            } else if (mDescription.getIconUri() != null) {
                Uri iconUri = mDescription.getIconUri();
                String scheme = iconUri.getScheme();
                if (!(ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme)
                        || ContentResolver.SCHEME_CONTENT.equals(scheme)
                        || ContentResolver.SCHEME_FILE.equals(scheme))) {
                    Log.w(TAG, "Icon Uri should point to local resources.");
                    return null;
                }
                BufferedInputStream stream = null;
                try {
                    stream = new BufferedInputStream(
                            getContext().getContentResolver().openInputStream(iconUri));

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
                                .openInputStream(iconUri));
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
                    Log.w(TAG, "Unable to open content: " + iconUri, e);
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
                // Portrait art requires background color.
                Palette palette = new Palette.Builder(art).maximumColorCount(1).generate();
                mBackgroundColor = (palette.getSwatches() == null)
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
            mArtView.setImageBitmap(art);
            mArtView.setBackgroundColor(mBackgroundColor);
            updateArtView();
        }
    }
}
