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

import android.app.Dialog;
import android.content.Context;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.mediarouter.R;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * This class implements the route controller dialog for {@link MediaRouter}.
 * <p>
 * This dialog allows the user to control or disconnect from the currently selected route.
 * </p>
 *
 * @see MediaRouteButton
 * @see MediaRouteActionProvider
 */
public class MediaRouteControllerDialog extends Dialog {
    private static final String TAG = "MediaRouteControllerDialog";

    private final MediaRouter mRouter;
    private final MediaRouterCallback mCallback;
    private final MediaRouter.RouteInfo mRoute;

    private boolean mCreated;
    private boolean mAttachedToWindow;
    private Drawable mMediaRouteConnectingDrawable;
    private Drawable mMediaRouteOnDrawable;
    private Drawable mCurrentIconDrawable;
    private Drawable mSettingsDrawable;

    private View mControlView;

    private Button mDisconnectButton;
    private Button mStopCastingButton;
    private ImageButton mPlayPauseButton;
    private ImageButton mSettingsButton;

    private ImageView mArtView;
    private TextView mTitleView;
    private TextView mSubtitleView;
    private TextView mRouteNameView;
    private View mTitlesWrapper;

    private MediaControllerCompat mMediaController;
    private MediaControllerCallback mControllerCallback;
    private PlaybackStateCompat mState;
    private MediaDescriptionCompat mDescription;


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
        return mControlView;
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

        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.mr_media_route_controller_material_dialog_b);

        ClickListener listener = new ClickListener();

        mDisconnectButton = (Button) findViewById(R.id.disconnect);
        mDisconnectButton.setOnClickListener(listener);

        mStopCastingButton = (Button) findViewById(R.id.stop);
        mStopCastingButton.setOnClickListener(listener);

        mSettingsButton = (ImageButton) findViewById(R.id.settings);
        mSettingsButton.setOnClickListener(listener);

        mArtView = (ImageView) findViewById(R.id.art);
        mTitleView = (TextView) findViewById(R.id.title);
        mSubtitleView = (TextView) findViewById(R.id.subtitle);
        mTitlesWrapper = findViewById(R.id.text_wrapper);
        mPlayPauseButton = (ImageButton) findViewById(R.id.play_pause);
        mPlayPauseButton.setOnClickListener(listener);
        mRouteNameView = (TextView) findViewById(R.id.route_name);

        mCreated = true;
        if (update()) {
            mControlView = onCreateMediaControlView(savedInstanceState);
            FrameLayout controlFrame =
                    (FrameLayout)findViewById(R.id.media_route_control_frame);
            if (mControlView != null) {
                controlFrame.findViewById(R.id.default_control_frame).setVisibility(View.GONE);
                controlFrame.addView(mControlView);
            }
        }
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

    private boolean update() {
        if (!mRoute.isSelected() || mRoute.isDefault()) {
            dismiss();
            return false;
        }
        if (!mCreated) {
            return false;
        }

        mRouteNameView.setText(mRoute.getName());

        if (mRoute.canDisconnect()) {
            mDisconnectButton.setVisibility(View.VISIBLE);
        } else {
            mDisconnectButton.setVisibility(View.GONE);
        }

        if (mRoute.getSettingsIntent() != null) {
            mSettingsButton.setVisibility(View.VISIBLE);
        } else {
            mSettingsButton.setVisibility(View.GONE);
        }

        if (mControlView == null) {
            if (mDescription != null) {
                if (mDescription.getIconBitmap() != null) {
                    mArtView.setImageBitmap(mDescription.getIconBitmap());
                    mArtView.setVisibility(View.VISIBLE);
                } else if (mDescription.getIconUri() != null) {
                    // TODO replace with background load of icon
                    mArtView.setImageURI(mDescription.getIconUri());
                    mArtView.setVisibility(View.VISIBLE);
                } else {
                    mArtView.setImageDrawable(null);
                    mArtView.setVisibility(View.GONE);
                }

                boolean haveText = false;
                CharSequence text = mDescription.getTitle();
                if (!TextUtils.isEmpty(text)) {
                    mTitleView.setText(text);
                    haveText = true;
                } else {
                    mTitleView.setText(null);
                    mTitleView.setVisibility(View.GONE);
                }
                text = mDescription.getSubtitle();
                if (!TextUtils.isEmpty(text)) {
                    mSubtitleView.setText(mDescription.getSubtitle());
                    haveText = true;
                } else {
                    mSubtitleView.setText(null);
                    mSubtitleView.setVisibility(View.GONE);
                }
                if (!haveText) {
                    mTitlesWrapper.setVisibility(View.GONE);
                } else {
                    mTitlesWrapper.setVisibility(View.VISIBLE);
                }
            } else {
                mArtView.setVisibility(View.GONE);
                mTitlesWrapper.setVisibility(View.GONE);
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
                            .getText(R.string.mr_media_route_controller_pause));
                } else if (!isPlaying && supportsPlay) {
                    mPlayPauseButton.setVisibility(View.VISIBLE);
                    mPlayPauseButton.setImageResource(MediaRouterThemeHelper.getThemeResource(
                            getContext(), R.attr.mediaRoutePlayDrawable));
                    mPlayPauseButton.setContentDescription(getContext().getResources()
                            .getText(R.string.mr_media_route_controller_play));
                } else {
                    mPlayPauseButton.setVisibility(View.GONE);
                }
            } else {
                mPlayPauseButton.setVisibility(View.GONE);
            }
        }
        return true;
    }

    private Drawable getIconDrawable() {
        if (mRoute.isConnecting()) {
            if (mMediaRouteConnectingDrawable == null) {
                mMediaRouteConnectingDrawable = MediaRouterThemeHelper.getThemeDrawable(
                        getContext(), R.attr.mediaRouteConnectingDrawable);
            }
            return mMediaRouteConnectingDrawable;
        } else {
            if (mMediaRouteOnDrawable == null) {
                mMediaRouteOnDrawable = MediaRouterThemeHelper.getThemeDrawable(
                        getContext(), R.attr.mediaRouteOnDrawable);
            }
            return mMediaRouteOnDrawable;
        }
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
            if (id == R.id.stop || id == R.id.disconnect) {
                if (mRoute.isSelected()) {
                    mRouter.unselect(id == R.id.stop ?
                            MediaRouter.UNSELECT_REASON_STOPPED :
                            MediaRouter.UNSELECT_REASON_DISCONNECTED);
                }
                dismiss();
            } else if (id == R.id.play_pause) {
                if (mMediaController != null && mState != null) {
                    if (mState.getState() == PlaybackStateCompat.STATE_PLAYING) {
                        mMediaController.getTransportControls().pause();
                    } else {
                        mMediaController.getTransportControls().play();
                    }
                }
            } else if (id == R.id.settings) {
                IntentSender is = mRoute.getSettingsIntent();
                if (is != null) {
                    try {
                        is.sendIntent(null, 0, null, null, null);
                        dismiss();
                    } catch (Exception e) {
                        Log.e(TAG, "Error opening route settings.", e);
                    }
                }
            }
        }
    }
}
