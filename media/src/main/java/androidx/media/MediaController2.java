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

package androidx.media;

import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.media.MediaConstants2.ARGUMENT_ALLOWED_COMMANDS;
import static androidx.media.MediaConstants2.ARGUMENT_ARGUMENTS;
import static androidx.media.MediaConstants2.ARGUMENT_BUFFERING_STATE;
import static androidx.media.MediaConstants2.ARGUMENT_COMMAND_BUTTONS;
import static androidx.media.MediaConstants2.ARGUMENT_COMMAND_CODE;
import static androidx.media.MediaConstants2.ARGUMENT_CUSTOM_COMMAND;
import static androidx.media.MediaConstants2.ARGUMENT_ERROR_CODE;
import static androidx.media.MediaConstants2.ARGUMENT_EXTRAS;
import static androidx.media.MediaConstants2.ARGUMENT_ICONTROLLER_CALLBACK;
import static androidx.media.MediaConstants2.ARGUMENT_MEDIA_ID;
import static androidx.media.MediaConstants2.ARGUMENT_MEDIA_ITEM;
import static androidx.media.MediaConstants2.ARGUMENT_PACKAGE_NAME;
import static androidx.media.MediaConstants2.ARGUMENT_PID;
import static androidx.media.MediaConstants2.ARGUMENT_PLAYBACK_INFO;
import static androidx.media.MediaConstants2.ARGUMENT_PLAYBACK_SPEED;
import static androidx.media.MediaConstants2.ARGUMENT_PLAYBACK_STATE_COMPAT;
import static androidx.media.MediaConstants2.ARGUMENT_PLAYER_STATE;
import static androidx.media.MediaConstants2.ARGUMENT_PLAYLIST;
import static androidx.media.MediaConstants2.ARGUMENT_PLAYLIST_INDEX;
import static androidx.media.MediaConstants2.ARGUMENT_PLAYLIST_METADATA;
import static androidx.media.MediaConstants2.ARGUMENT_QUERY;
import static androidx.media.MediaConstants2.ARGUMENT_RATING;
import static androidx.media.MediaConstants2.ARGUMENT_REPEAT_MODE;
import static androidx.media.MediaConstants2.ARGUMENT_RESULT_RECEIVER;
import static androidx.media.MediaConstants2.ARGUMENT_ROUTE_BUNDLE;
import static androidx.media.MediaConstants2.ARGUMENT_SEEK_POSITION;
import static androidx.media.MediaConstants2.ARGUMENT_SHUFFLE_MODE;
import static androidx.media.MediaConstants2.ARGUMENT_UID;
import static androidx.media.MediaConstants2.ARGUMENT_URI;
import static androidx.media.MediaConstants2.ARGUMENT_VOLUME;
import static androidx.media.MediaConstants2.ARGUMENT_VOLUME_DIRECTION;
import static androidx.media.MediaConstants2.ARGUMENT_VOLUME_FLAGS;
import static androidx.media.MediaConstants2.CONNECT_RESULT_CONNECTED;
import static androidx.media.MediaConstants2.CONNECT_RESULT_DISCONNECTED;
import static androidx.media.MediaConstants2.CONTROLLER_COMMAND_BY_COMMAND_CODE;
import static androidx.media.MediaConstants2.CONTROLLER_COMMAND_BY_CUSTOM_COMMAND;
import static androidx.media.MediaConstants2.CONTROLLER_COMMAND_CONNECT;
import static androidx.media.MediaConstants2.CONTROLLER_COMMAND_DISCONNECT;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_ALLOWED_COMMANDS_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_BUFFERING_STATE_CHAGNED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_CURRENT_MEDIA_ITEM_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_ERROR;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_PLAYBACK_INFO_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_PLAYBACK_SPEED_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_PLAYER_STATE_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_PLAYLIST_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_PLAYLIST_METADATA_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_REPEAT_MODE_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_ROUTES_INFO_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_ON_SHUFFLE_MODE_CHANGED;
import static androidx.media.MediaConstants2.SESSION_EVENT_SEND_CUSTOM_COMMAND;
import static androidx.media.MediaConstants2.SESSION_EVENT_SET_CUSTOM_LAYOUT;
import static androidx.media.MediaPlayerBase.BUFFERING_STATE_UNKNOWN;
import static androidx.media.MediaPlayerBase.UNKNOWN_TIME;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYBACK_PAUSE;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYBACK_PLAY;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYBACK_PREPARE;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYBACK_RESET;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYBACK_SEEK_TO;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYBACK_SET_SPEED;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_ADD_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_REMOVE_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_REPLACE_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SET_LIST;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SET_LIST_METADATA;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SET_REPEAT_MODE;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SET_SHUFFLE_MODE;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_TO_NEXT_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_TO_PLAYLIST_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_TO_PREV_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_FAST_FORWARD;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_SEARCH;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_URI;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_URI;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_REWIND;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_SELECT_ROUTE;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_SET_RATING;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_SUBSCRIBE_ROUTES_INFO;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO;
import static androidx.media.SessionCommand2.COMMAND_CODE_VOLUME_ADJUST_VOLUME;
import static androidx.media.SessionCommand2.COMMAND_CODE_VOLUME_SET_VOLUME;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.media.MediaPlaylistAgent.RepeatMode;
import androidx.media.MediaPlaylistAgent.ShuffleMode;
import androidx.media.MediaSession2.CommandButton;
import androidx.media.MediaSession2.ControllerInfo;
import androidx.media.MediaSession2.ErrorCode;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Allows an app to interact with an active {@link MediaSession2} in any status. Media buttons and
 * other commands can be sent to the session.
 * <p>
 * When you're done, use {@link #close()} to clean up resources. This also helps session service
 * to be destroyed when there's no controller associated with it.
 * <p>
 * When controlling {@link MediaSession2}, the controller will be available immediately after
 * the creation.
 * <p>
 * MediaController2 objects are thread-safe.
 * <p>
 * @see MediaSession2
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class MediaController2 implements AutoCloseable {
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({AudioManager.ADJUST_LOWER, AudioManager.ADJUST_RAISE, AudioManager.ADJUST_SAME,
            AudioManager.ADJUST_MUTE, AudioManager.ADJUST_UNMUTE, AudioManager.ADJUST_TOGGLE_MUTE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface VolumeDirection {}

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef(value = {AudioManager.FLAG_SHOW_UI, AudioManager.FLAG_ALLOW_RINGER_MODES,
            AudioManager.FLAG_PLAY_SOUND, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE,
            AudioManager.FLAG_VIBRATE}, flag = true)
    @Retention(RetentionPolicy.SOURCE)
    public @interface VolumeFlags {}

    /**
     * Interface for listening to change in activeness of the {@link MediaSession2}.  It's
     * active if and only if it has set a player.
     */
    public abstract static class ControllerCallback {
        /**
         * Called when the controller is successfully connected to the session. The controller
         * becomes available afterwards.
         *
         * @param controller the controller for this event
         * @param allowedCommands commands that's allowed by the session.
         */
        public void onConnected(@NonNull MediaController2 controller,
                @NonNull SessionCommandGroup2 allowedCommands) { }

        /**
         * Called when the session refuses the controller or the controller is disconnected from
         * the session. The controller becomes unavailable afterwards and the callback wouldn't
         * be called.
         * <p>
         * It will be also called after the {@link #close()}, so you can put clean up code here.
         * You don't need to call {@link #close()} after this.
         *
         * @param controller the controller for this event
         */
        public void onDisconnected(@NonNull MediaController2 controller) { }

        /**
         * Called when the session set the custom layout through the
         * {@link MediaSession2#setCustomLayout(ControllerInfo, List)}.
         * <p>
         * Can be called before {@link #onConnected(MediaController2, SessionCommandGroup2)}
         * is called.
         *
         * @param controller the controller for this event
         * @param layout
         */
        public void onCustomLayoutChanged(@NonNull MediaController2 controller,
                @NonNull List<CommandButton> layout) { }

        /**
         * Called when the session has changed anything related with the {@link PlaybackInfo}.
         *
         * @param controller the controller for this event
         * @param info new playback info
         */
        public void onPlaybackInfoChanged(@NonNull MediaController2 controller,
                @NonNull PlaybackInfo info) { }

        /**
         * Called when the allowed commands are changed by session.
         *
         * @param controller the controller for this event
         * @param commands newly allowed commands
         */
        public void onAllowedCommandsChanged(@NonNull MediaController2 controller,
                @NonNull SessionCommandGroup2 commands) { }

        /**
         * Called when the session sent a custom command.
         *
         * @param controller the controller for this event
         * @param command
         * @param args
         * @param receiver
         */
        public void onCustomCommand(@NonNull MediaController2 controller,
                @NonNull SessionCommand2 command, @Nullable Bundle args,
                @Nullable ResultReceiver receiver) { }

        /**
         * Called when the player state is changed.
         *
         * @param controller the controller for this event
         * @param state
         */
        public void onPlayerStateChanged(@NonNull MediaController2 controller, int state) { }

        /**
         * Called when playback speed is changed.
         *
         * @param controller the controller for this event
         * @param speed speed
         */
        public void onPlaybackSpeedChanged(@NonNull MediaController2 controller,
                float speed) { }

        /**
         * Called to report buffering events for a data source.
         * <p>
         * Use {@link #getBufferedPosition()} for current buffering position.
         *
         * @param controller the controller for this event
         * @param item the media item for which buffering is happening.
         * @param state the new buffering state.
         */
        public void onBufferingStateChanged(@NonNull MediaController2 controller,
                @NonNull MediaItem2 item, @MediaPlayerBase.BuffState int state) { }

        /**
         * Called to indicate that seeking is completed.
         *
         * @param controller the controller for this event.
         * @param position the previous seeking request.
         */
        public void onSeekCompleted(@NonNull MediaController2 controller, long position) { }

        /**
         * Called when a error from
         *
         * @param controller the controller for this event
         * @param errorCode error code
         * @param extras extra information
         */
        public void onError(@NonNull MediaController2 controller, @ErrorCode int errorCode,
                @Nullable Bundle extras) { }

        /**
         * Called when the player's currently playing item is changed
         * <p>
         * When it's called, you should invalidate previous playback information and wait for later
         * callbacks.
         *
         * @param controller the controller for this event
         * @param item new item
         * @see #onBufferingStateChanged(MediaController2, MediaItem2, int)
         */
        public void onCurrentMediaItemChanged(@NonNull MediaController2 controller,
                @NonNull MediaItem2 item) { }

        /**
         * Called when a playlist is changed.
         *
         * @param controller the controller for this event
         * @param list new playlist
         * @param metadata new metadata
         */
        public void onPlaylistChanged(@NonNull MediaController2 controller,
                @NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata) { }

        /**
         * Called when a playlist metadata is changed.
         *
         * @param controller the controller for this event
         * @param metadata new metadata
         */
        public void onPlaylistMetadataChanged(@NonNull MediaController2 controller,
                @Nullable MediaMetadata2 metadata) { }

        /**
         * Called when the shuffle mode is changed.
         *
         * @param controller the controller for this event
         * @param shuffleMode repeat mode
         * @see MediaPlaylistAgent#SHUFFLE_MODE_NONE
         * @see MediaPlaylistAgent#SHUFFLE_MODE_ALL
         * @see MediaPlaylistAgent#SHUFFLE_MODE_GROUP
         */
        public void onShuffleModeChanged(@NonNull MediaController2 controller,
                @MediaPlaylistAgent.ShuffleMode int shuffleMode) { }

        /**
         * Called when the repeat mode is changed.
         *
         * @param controller the controller for this event
         * @param repeatMode repeat mode
         * @see MediaPlaylistAgent#REPEAT_MODE_NONE
         * @see MediaPlaylistAgent#REPEAT_MODE_ONE
         * @see MediaPlaylistAgent#REPEAT_MODE_ALL
         * @see MediaPlaylistAgent#REPEAT_MODE_GROUP
         */
        public void onRepeatModeChanged(@NonNull MediaController2 controller,
                @MediaPlaylistAgent.RepeatMode int repeatMode) { }

        /**
         * Called when a property of the indicated media route has changed.
         *
         * @param controller the controller for this event
         * @param routes The list of Bundle from MediaRouteDescriptor.asBundle().
         *              See MediaRouteDescriptor.fromBundle(Bundle bundle) to get
         *              MediaRouteDescriptor object from the {@code routes}
         */
        public void onRoutesInfoChanged(@NonNull MediaController2 controller,
                @Nullable List<Bundle> routes) { }
    }

    /**
     * Holds information about the the way volume is handled for this session.
     */
    // The same as MediaController.PlaybackInfo
    public static final class PlaybackInfo {
        private static final String KEY_PLAYBACK_TYPE = "android.media.audio_info.playback_type";
        private static final String KEY_CONTROL_TYPE = "android.media.audio_info.control_type";
        private static final String KEY_MAX_VOLUME = "android.media.audio_info.max_volume";
        private static final String KEY_CURRENT_VOLUME = "android.media.audio_info.current_volume";
        private static final String KEY_AUDIO_ATTRIBUTES = "android.media.audio_info.audio_attrs";

        private final int mPlaybackType;
        private final int mControlType;
        private final int mMaxVolume;
        private final int mCurrentVolume;
        private final AudioAttributesCompat mAudioAttrsCompat;

        /**
         * The session uses remote playback.
         */
        public static final int PLAYBACK_TYPE_REMOTE = 2;
        /**
         * The session uses local playback.
         */
        public static final int PLAYBACK_TYPE_LOCAL = 1;

        PlaybackInfo(int playbackType, AudioAttributesCompat attrs, int controlType, int max,
                int current) {
            mPlaybackType = playbackType;
            mAudioAttrsCompat = attrs;
            mControlType = controlType;
            mMaxVolume = max;
            mCurrentVolume = current;
        }

        /**
         * Get the type of playback which affects volume handling. One of:
         * <ul>
         * <li>{@link #PLAYBACK_TYPE_LOCAL}</li>
         * <li>{@link #PLAYBACK_TYPE_REMOTE}</li>
         * </ul>
         *
         * @return The type of playback this session is using.
         */
        public int getPlaybackType() {
            return mPlaybackType;
        }

        /**
         * Get the audio attributes for this session. The attributes will affect
         * volume handling for the session. When the volume type is
         * {@link #PLAYBACK_TYPE_REMOTE} these may be ignored by the
         * remote volume handler.
         *
         * @return The attributes for this session.
         */
        public AudioAttributesCompat getAudioAttributes() {
            return mAudioAttrsCompat;
        }

        /**
         * Get the type of volume control that can be used. One of:
         * <ul>
         * <li>{@link VolumeProviderCompat#VOLUME_CONTROL_ABSOLUTE}</li>
         * <li>{@link VolumeProviderCompat#VOLUME_CONTROL_RELATIVE}</li>
         * <li>{@link VolumeProviderCompat#VOLUME_CONTROL_FIXED}</li>
         * </ul>
         *
         * @return The type of volume control that may be used with this session.
         */
        public int getControlType() {
            return mControlType;
        }

        /**
         * Get the maximum volume that may be set for this session.
         *
         * @return The maximum allowed volume where this session is playing.
         */
        public int getMaxVolume() {
            return mMaxVolume;
        }

        /**
         * Get the current volume for this session.
         *
         * @return The current volume where this session is playing.
         */
        public int getCurrentVolume() {
            return mCurrentVolume;
        }

        Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putInt(KEY_PLAYBACK_TYPE, mPlaybackType);
            bundle.putInt(KEY_CONTROL_TYPE, mControlType);
            bundle.putInt(KEY_MAX_VOLUME, mMaxVolume);
            bundle.putInt(KEY_CURRENT_VOLUME, mCurrentVolume);
            if (mAudioAttrsCompat != null) {
                bundle.putParcelable(KEY_AUDIO_ATTRIBUTES,
                        MediaUtils2.toAudioAttributesBundle(mAudioAttrsCompat));
            }
            return bundle;
        }

        static PlaybackInfo createPlaybackInfo(int playbackType, AudioAttributesCompat attrs,
                int controlType, int max, int current) {
            return new PlaybackInfo(playbackType, attrs, controlType, max, current);
        }

        static PlaybackInfo fromBundle(Bundle bundle) {
            if (bundle == null) {
                return null;
            }
            final int volumeType = bundle.getInt(KEY_PLAYBACK_TYPE);
            final int volumeControl = bundle.getInt(KEY_CONTROL_TYPE);
            final int maxVolume = bundle.getInt(KEY_MAX_VOLUME);
            final int currentVolume = bundle.getInt(KEY_CURRENT_VOLUME);
            final AudioAttributesCompat attrs = MediaUtils2.fromAudioAttributesBundle(
                    bundle.getBundle(KEY_AUDIO_ATTRIBUTES));
            return createPlaybackInfo(volumeType, attrs, volumeControl, maxVolume,
                    currentVolume);
        }
    }

    private final class ControllerCompatCallback extends MediaControllerCompat.Callback {
        @Override
        public void onSessionReady() {
            sendCommand(CONTROLLER_COMMAND_CONNECT, new ResultReceiver(mHandler) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    if (!mHandlerThread.isAlive()) {
                        return;
                    }
                    switch (resultCode) {
                        case CONNECT_RESULT_CONNECTED:
                            onConnectedNotLocked(resultData);
                            break;
                        case CONNECT_RESULT_DISCONNECTED:
                            mCallback.onDisconnected(MediaController2.this);
                            close();
                            break;
                    }
                }
            });
        }

        @Override
        public void onSessionDestroyed() {
            close();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            synchronized (mLock) {
                mPlaybackStateCompat = state;
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            synchronized (mLock) {
                mMediaMetadataCompat = metadata;
            }
        }

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            switch (event) {
                case SESSION_EVENT_ON_ALLOWED_COMMANDS_CHANGED: {
                    SessionCommandGroup2 allowedCommands = SessionCommandGroup2.fromBundle(
                            extras.getBundle(ARGUMENT_ALLOWED_COMMANDS));
                    synchronized (mLock) {
                        mAllowedCommands = allowedCommands;
                    }
                    mCallback.onAllowedCommandsChanged(MediaController2.this, allowedCommands);
                    break;
                }
                case SESSION_EVENT_ON_PLAYER_STATE_CHANGED: {
                    int playerState = extras.getInt(ARGUMENT_PLAYER_STATE);
                    synchronized (mLock) {
                        mPlayerState = playerState;
                    }
                    mCallback.onPlayerStateChanged(MediaController2.this, playerState);
                    break;
                }
                case SESSION_EVENT_ON_CURRENT_MEDIA_ITEM_CHANGED: {
                    MediaItem2 item = MediaItem2.fromBundle(extras.getBundle(ARGUMENT_MEDIA_ITEM));
                    if (item == null) {
                        return;
                    }
                    synchronized (mLock) {
                        mCurrentMediaItem = item;
                    }
                    mCallback.onCurrentMediaItemChanged(MediaController2.this, item);
                    break;
                }
                case SESSION_EVENT_ON_ERROR: {
                    int errorCode = extras.getInt(ARGUMENT_ERROR_CODE);
                    Bundle errorExtras = extras.getBundle(ARGUMENT_EXTRAS);
                    mCallback.onError(MediaController2.this, errorCode, errorExtras);
                    break;
                }
                case SESSION_EVENT_ON_ROUTES_INFO_CHANGED: {
                    List<Bundle> routes = MediaUtils2.toBundleList(
                            extras.getParcelableArray(ARGUMENT_ROUTE_BUNDLE));
                    mCallback.onRoutesInfoChanged(MediaController2.this, routes);
                    break;
                }
                case SESSION_EVENT_ON_PLAYLIST_CHANGED: {
                    MediaMetadata2 playlistMetadata = MediaMetadata2.fromBundle(
                            extras.getBundle(ARGUMENT_PLAYLIST_METADATA));
                    List<MediaItem2> playlist = MediaUtils2.fromMediaItem2ParcelableArray(
                            extras.getParcelableArray(ARGUMENT_PLAYLIST));
                    synchronized (mLock) {
                        mPlaylist = playlist;
                        mPlaylistMetadata = playlistMetadata;
                    }
                    mCallback.onPlaylistChanged(MediaController2.this, playlist, playlistMetadata);
                    break;
                }
                case SESSION_EVENT_ON_PLAYLIST_METADATA_CHANGED: {
                    MediaMetadata2 playlistMetadata = MediaMetadata2.fromBundle(
                            extras.getBundle(ARGUMENT_PLAYLIST_METADATA));
                    synchronized (mLock) {
                        mPlaylistMetadata = playlistMetadata;
                    }
                    mCallback.onPlaylistMetadataChanged(MediaController2.this, playlistMetadata);
                    break;
                }
                case SESSION_EVENT_ON_REPEAT_MODE_CHANGED: {
                    int repeatMode = extras.getInt(ARGUMENT_REPEAT_MODE);
                    synchronized (mLock) {
                        mRepeatMode = repeatMode;
                    }
                    mCallback.onRepeatModeChanged(MediaController2.this, repeatMode);
                    break;
                }
                case SESSION_EVENT_ON_SHUFFLE_MODE_CHANGED: {
                    int shuffleMode = extras.getInt(ARGUMENT_SHUFFLE_MODE);
                    synchronized (mLock) {
                        mShuffleMode = shuffleMode;
                    }
                    mCallback.onShuffleModeChanged(MediaController2.this, shuffleMode);
                    break;
                }
                case SESSION_EVENT_SEND_CUSTOM_COMMAND: {
                    Bundle commandBundle = extras.getBundle(ARGUMENT_CUSTOM_COMMAND);
                    if (commandBundle == null) {
                        return;
                    }
                    SessionCommand2 command = SessionCommand2.fromBundle(commandBundle);
                    Bundle args = extras.getBundle(ARGUMENT_ARGUMENTS);
                    ResultReceiver receiver = extras.getParcelable(ARGUMENT_RESULT_RECEIVER);
                    mCallback.onCustomCommand(MediaController2.this, command, args, receiver);
                    break;
                }
                case SESSION_EVENT_SET_CUSTOM_LAYOUT: {
                    List<CommandButton> layout = MediaUtils2.fromCommandButtonParcelableArray(
                            extras.getParcelableArray(ARGUMENT_COMMAND_BUTTONS));
                    if (layout == null) {
                        return;
                    }
                    mCallback.onCustomLayoutChanged(MediaController2.this, layout);
                    break;
                }
                case SESSION_EVENT_ON_PLAYBACK_INFO_CHANGED: {
                    PlaybackInfo info = PlaybackInfo.fromBundle(
                            extras.getBundle(ARGUMENT_PLAYBACK_INFO));
                    if (info == null) {
                        return;
                    }
                    synchronized (mLock) {
                        mPlaybackInfo = info;
                    }
                    mCallback.onPlaybackInfoChanged(MediaController2.this, info);
                    break;
                }
                case SESSION_EVENT_ON_PLAYBACK_SPEED_CHANGED: {
                    PlaybackStateCompat state =
                            extras.getParcelable(ARGUMENT_PLAYBACK_STATE_COMPAT);
                    if (state == null) {
                        return;
                    }
                    synchronized (mLock) {
                        mPlaybackStateCompat = state;
                    }
                    mCallback.onPlaybackSpeedChanged(
                            MediaController2.this, state.getPlaybackSpeed());
                    break;
                }
                case SESSION_EVENT_ON_BUFFERING_STATE_CHAGNED: {
                    MediaItem2 item = MediaItem2.fromBundle(extras.getBundle(ARGUMENT_MEDIA_ITEM));
                    int bufferingState = extras.getInt(ARGUMENT_BUFFERING_STATE);
                    if (item == null) {
                        return;
                    }
                    synchronized (mLock) {
                        mBufferingState = bufferingState;
                    }
                    mCallback.onBufferingStateChanged(MediaController2.this, item, bufferingState);
                    break;
                }
            }
        }
    }

    private static final String TAG = "MediaController2";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    // Note: Using {@code null} doesn't helpful here because MediaBrowserServiceCompat always wraps
    //       the rootHints so it becomes non-null.
    static final Bundle sDefaultRootExtras = new Bundle();
    static {
        sDefaultRootExtras.putBoolean(MediaConstants2.ROOT_EXTRA_DEFAULT, true);
    }

    private final Context mContext;
    private final Object mLock = new Object();

    private final SessionToken2 mToken;
    private final ControllerCallback mCallback;
    private final Executor mCallbackExecutor;
    private final IBinder.DeathRecipient mDeathRecipient;

    private final HandlerThread mHandlerThread;
    private final Handler mHandler;

    @GuardedBy("mLock")
    private MediaBrowserCompat mBrowserCompat;
    @GuardedBy("mLock")
    private boolean mIsReleased;
    @GuardedBy("mLock")
    private List<MediaItem2> mPlaylist;
    @GuardedBy("mLock")
    private MediaMetadata2 mPlaylistMetadata;
    @GuardedBy("mLock")
    private @RepeatMode int mRepeatMode;
    @GuardedBy("mLock")
    private @ShuffleMode int mShuffleMode;
    @GuardedBy("mLock")
    private int mPlayerState;
    @GuardedBy("mLock")
    private MediaItem2 mCurrentMediaItem;
    @GuardedBy("mLock")
    private int mBufferingState;
    @GuardedBy("mLock")
    private PlaybackInfo mPlaybackInfo;
    @GuardedBy("mLock")
    private SessionCommandGroup2 mAllowedCommands;

    // Media 1.0 variables
    @GuardedBy("mLock")
    private MediaControllerCompat mControllerCompat;
    @GuardedBy("mLock")
    private ControllerCompatCallback mControllerCompatCallback;
    @GuardedBy("mLock")
    private PlaybackStateCompat mPlaybackStateCompat;
    @GuardedBy("mLock")
    private MediaMetadataCompat mMediaMetadataCompat;

    // Assignment should be used with the lock hold, but should be used without a lock to prevent
    // potential deadlock.
    @GuardedBy("mLock")
    private volatile boolean mConnected;

    /**
     * Create a {@link MediaController2} from the {@link SessionToken2}.
     * This connects to the session and may wake up the service if it's not available.
     *
     * @param context Context
     * @param token token to connect to
     * @param executor executor to run callbacks on.
     * @param callback controller callback to receive changes in
     */
    public MediaController2(@NonNull Context context, @NonNull SessionToken2 token,
            @NonNull Executor executor, @NonNull ControllerCallback callback) {
        super();
        if (context == null) {
            throw new IllegalArgumentException("context shouldn't be null");
        }
        if (token == null) {
            throw new IllegalArgumentException("token shouldn't be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback shouldn't be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("executor shouldn't be null");
        }
        mContext = context;
        mHandlerThread = new HandlerThread("MediaController2_Thread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mToken = token;
        mCallback = callback;
        mCallbackExecutor = executor;
        mDeathRecipient = new IBinder.DeathRecipient() {
            @Override
            public void binderDied() {
                MediaController2.this.close();
            }
        };

        initialize();
    }

    /**
     * Release this object, and disconnect from the session. After this, callbacks wouldn't be
     * received.
     */
    @Override
    public void close() {
        if (DEBUG) {
            //Log.d(TAG, "release from " + mToken, new IllegalStateException());
        }
        synchronized (mLock) {
            if (mIsReleased) {
                // Prevent re-enterance from the ControllerCallback.onDisconnected()
                return;
            }
            mHandler.removeCallbacksAndMessages(null);
            mHandlerThread.quitSafely();

            mIsReleased = true;

            // Send command before the unregister callback to use mIControllerCallback in the
            // callback.
            sendCommand(CONTROLLER_COMMAND_DISCONNECT);
            if (mControllerCompat != null) {
                mControllerCompat.unregisterCallback(mControllerCompatCallback);
            }
            if (mBrowserCompat != null) {
                mBrowserCompat.disconnect();
                mBrowserCompat = null;
            }
            if (mControllerCompat != null) {
                mControllerCompat.unregisterCallback(mControllerCompatCallback);
                mControllerCompat = null;
            }
            mConnected = false;
        }
        mCallbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mCallback.onDisconnected(MediaController2.this);
            }
        });
    }

    /**
     * @return token
     */
    public @NonNull SessionToken2 getSessionToken() {
        return mToken;
    }

    /**
     * Returns whether this class is connected to active {@link MediaSession2} or not.
     */
    public boolean isConnected() {
        synchronized (mLock) {
            return mConnected;
        }
    }

    /**
     * Requests that the player starts or resumes playback.
     */
    public void play() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            sendCommand(COMMAND_CODE_PLAYBACK_PLAY);
        }
    }

    /**
     * Requests that the player pauses playback.
     */
    public void pause() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            sendCommand(COMMAND_CODE_PLAYBACK_PAUSE);
        }
    }

    /**
     * Requests that the player be reset to its uninitialized state.
     */
    public void reset() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            sendCommand(COMMAND_CODE_PLAYBACK_RESET);
        }
    }

    /**
     * Request that the player prepare its playback. In other words, other sessions can continue
     * to play during the preparation of this session. This method can be used to speed up the
     * start of the playback. Once the preparation is done, the session will change its playback
     * state to {@link MediaPlayerBase#PLAYER_STATE_PAUSED}. Afterwards, {@link #play} can be called
     * to start playback.
     */
    public void prepare() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            sendCommand(COMMAND_CODE_PLAYBACK_PREPARE);
        }
    }

    /**
     * Start fast forwarding. If playback is already fast forwarding this
     * may increase the rate.
     */
    public void fastForward() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            sendCommand(COMMAND_CODE_SESSION_FAST_FORWARD);
        }
    }

    /**
     * Start rewinding. If playback is already rewinding this may increase
     * the rate.
     */
    public void rewind() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            sendCommand(COMMAND_CODE_SESSION_REWIND);
        }
    }

    /**
     * Move to a new location in the media stream.
     *
     * @param pos Position to move to, in milliseconds.
     */
    public void seekTo(long pos) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            Bundle args = new Bundle();
            args.putLong(ARGUMENT_SEEK_POSITION, pos);
            sendCommand(COMMAND_CODE_PLAYBACK_SEEK_TO, args);
        }
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void skipForward() {
        // To match with KEYCODE_MEDIA_SKIP_FORWARD
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void skipBackward() {
        // To match with KEYCODE_MEDIA_SKIP_BACKWARD
    }

    /**
     * Request that the player start playback for a specific media id.
     *
     * @param mediaId The id of the requested media.
     * @param extras Optional extras that can include extra information about the media item
     *               to be played.
     */
    public void playFromMediaId(@NonNull String mediaId, @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            Bundle args = new Bundle();
            args.putString(ARGUMENT_MEDIA_ID, mediaId);
            args.putBundle(ARGUMENT_EXTRAS, extras);
            sendCommand(COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID, args);
        }
    }

    /**
     * Request that the player start playback for a specific search query.
     *
     * @param query The search query. Should not be an empty string.
     * @param extras Optional extras that can include extra information about the query.
     */
    public void playFromSearch(@NonNull String query, @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            Bundle args = new Bundle();
            args.putString(ARGUMENT_QUERY, query);
            args.putBundle(ARGUMENT_EXTRAS, extras);
            sendCommand(COMMAND_CODE_SESSION_PLAY_FROM_SEARCH, args);
        }
    }

    /**
     * Request that the player start playback for a specific {@link Uri}.
     *
     * @param uri The URI of the requested media.
     * @param extras Optional extras that can include extra information about the media item
     *               to be played.
     */
    public void playFromUri(@NonNull Uri uri, @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            Bundle args = new Bundle();
            args.putParcelable(ARGUMENT_URI, uri);
            args.putBundle(ARGUMENT_EXTRAS, extras);
            sendCommand(COMMAND_CODE_SESSION_PLAY_FROM_URI, args);
        }
    }

    /**
     * Request that the player prepare playback for a specific media id. In other words, other
     * sessions can continue to play during the preparation of this session. This method can be
     * used to speed up the start of the playback. Once the preparation is done, the session
     * will change its playback state to {@link MediaPlayerBase#PLAYER_STATE_PAUSED}. Afterwards,
     * {@link #play} can be called to start playback. If the preparation is not needed,
     * {@link #playFromMediaId} can be directly called without this method.
     *
     * @param mediaId The id of the requested media.
     * @param extras Optional extras that can include extra information about the media item
     *               to be prepared.
     */
    public void prepareFromMediaId(@NonNull String mediaId, @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            Bundle args = new Bundle();
            args.putString(ARGUMENT_MEDIA_ID, mediaId);
            args.putBundle(ARGUMENT_EXTRAS, extras);
            sendCommand(COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID, args);
        }
    }

    /**
     * Request that the player prepare playback for a specific search query.
     * In other words, other sessions can continue to play during the preparation of this session.
     * This method can be used to speed up the start of the playback.
     * Once the preparation is done, the session will change its playback state to
     * {@link MediaPlayerBase#PLAYER_STATE_PAUSED}. Afterwards,
     * {@link #play} can be called to start playback. If the preparation is not needed,
     * {@link #playFromSearch} can be directly called without this method.
     *
     * @param query The search query. Should not be an empty string.
     * @param extras Optional extras that can include extra information about the query.
     */
    public void prepareFromSearch(@NonNull String query, @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            Bundle args = new Bundle();
            args.putString(ARGUMENT_QUERY, query);
            args.putBundle(ARGUMENT_EXTRAS, extras);
            sendCommand(COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH, args);
        }
    }

    /**
     * Request that the player prepare playback for a specific {@link Uri}. In other words,
     * other sessions can continue to play during the preparation of this session. This method
     * can be used to speed up the start of the playback. Once the preparation is done, the
     * session will change its playback state to {@link MediaPlayerBase#PLAYER_STATE_PAUSED}.
     * Afterwards, {@link #play} can be called to start playback. If the preparation is not needed,
     * {@link #playFromUri} can be directly called without this method.
     *
     * @param uri The URI of the requested media.
     * @param extras Optional extras that can include extra information about the media item
     *               to be prepared.
     */
    public void prepareFromUri(@NonNull Uri uri, @Nullable Bundle extras) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            Bundle args = new Bundle();
            args.putParcelable(ARGUMENT_URI, uri);
            args.putBundle(ARGUMENT_EXTRAS, extras);
            sendCommand(COMMAND_CODE_SESSION_PREPARE_FROM_URI, args);
        }
    }

    /**
     * Set the volume of the output this session is playing on. The command will be ignored if it
     * does not support {@link VolumeProviderCompat#VOLUME_CONTROL_ABSOLUTE}.
     * <p>
     * If the session is local playback, this changes the device's volume with the stream that
     * session's player is using. Flags will be specified for the {@link AudioManager}.
     * <p>
     * If the session is remote player (i.e. session has set volume provider), its volume provider
     * will receive this request instead.
     *
     * @see #getPlaybackInfo()
     * @param value The value to set it to, between 0 and the reported max.
     * @param flags flags from {@link AudioManager} to include with the volume request for local
     *              playback
     */
    public void setVolumeTo(int value, @VolumeFlags int flags) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            Bundle args = new Bundle();
            args.putInt(ARGUMENT_VOLUME, value);
            args.putInt(ARGUMENT_VOLUME_FLAGS, flags);
            sendCommand(COMMAND_CODE_VOLUME_SET_VOLUME, args);
        }
    }

    /**
     * Adjust the volume of the output this session is playing on. The direction
     * must be one of {@link AudioManager#ADJUST_LOWER},
     * {@link AudioManager#ADJUST_RAISE}, or {@link AudioManager#ADJUST_SAME}.
     * <p>
     * The command will be ignored if the session does not support
     * {@link VolumeProviderCompat#VOLUME_CONTROL_RELATIVE} or
     * {@link VolumeProviderCompat#VOLUME_CONTROL_ABSOLUTE}.
     * <p>
     * If the session is local playback, this changes the device's volume with the stream that
     * session's player is using. Flags will be specified for the {@link AudioManager}.
     * <p>
     * If the session is remote player (i.e. session has set volume provider), its volume provider
     * will receive this request instead.
     *
     * @see #getPlaybackInfo()
     * @param direction The direction to adjust the volume in.
     * @param flags flags from {@link AudioManager} to include with the volume request for local
     *              playback
     */
    public void adjustVolume(@VolumeDirection int direction, @VolumeFlags int flags) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            Bundle args = new Bundle();
            args.putInt(ARGUMENT_VOLUME_DIRECTION, direction);
            args.putInt(ARGUMENT_VOLUME_FLAGS, flags);
            sendCommand(COMMAND_CODE_VOLUME_ADJUST_VOLUME, args);
        }
    }

    /**
     * Get an intent for launching UI associated with this session if one exists.
     *
     * @return A {@link PendingIntent} to launch UI or null.
     */
    public @Nullable PendingIntent getSessionActivity() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return null;
            }
            return mControllerCompat.getSessionActivity();
        }
    }

    /**
     * Get the lastly cached player state from
     * {@link ControllerCallback#onPlayerStateChanged(MediaController2, int)}.
     *
     * @return player state
     */
    public int getPlayerState() {
        synchronized (mLock) {
            return mPlayerState;
        }
    }

    /**
     * Gets the duration of the current media item, or {@link MediaPlayerBase#UNKNOWN_TIME} if
     * unknown.
     * @return the duration in ms, or {@link MediaPlayerBase#UNKNOWN_TIME}.
     */
    public long getDuration() {
        synchronized (mLock) {
            if (mMediaMetadataCompat != null
                    && mMediaMetadataCompat.containsKey(METADATA_KEY_DURATION)) {
                return mMediaMetadataCompat.getLong(METADATA_KEY_DURATION);
            }
        }
        return MediaPlayerBase.UNKNOWN_TIME;
    }

    /**
     * Gets the current playback position.
     * <p>
     * This returns the calculated value of the position, based on the difference between the
     * update time and current time.
     *
     * @return position
     */
    public long getCurrentPosition() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return UNKNOWN_TIME;
            }
            if (mPlaybackStateCompat != null) {
                long timeDiff = SystemClock.elapsedRealtime()
                        - mPlaybackStateCompat.getLastPositionUpdateTime();
                long expectedPosition = mPlaybackStateCompat.getPosition()
                        + (long) (mPlaybackStateCompat.getPlaybackSpeed() * timeDiff);
                return Math.max(0, expectedPosition);
            }
            return UNKNOWN_TIME;
        }
    }

    /**
     * Get the lastly cached playback speed from
     * {@link ControllerCallback#onPlaybackSpeedChanged(MediaController2, float)}.
     *
     * @return speed the lastly cached playback speed, or 0.0f if unknown.
     */
    public float getPlaybackSpeed() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return 0f;
            }
            return (mPlaybackStateCompat == null) ? 0f : mPlaybackStateCompat.getPlaybackSpeed();
        }
    }

    /**
     * Set the playback speed.
     */
    public void setPlaybackSpeed(float speed) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            Bundle args = new Bundle();
            args.putFloat(ARGUMENT_PLAYBACK_SPEED, speed);
            sendCommand(COMMAND_CODE_PLAYBACK_SET_SPEED, args);
        }
    }

    /**
     * Gets the current buffering state of the player.
     * During buffering, see {@link #getBufferedPosition()} for the quantifying the amount already
     * buffered.
     * @return the buffering state.
     */
    public @MediaPlayerBase.BuffState int getBufferingState() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return BUFFERING_STATE_UNKNOWN;
            }
            return mBufferingState;
        }
    }

    /**
     * Gets the lastly cached buffered position from the session when
     * {@link ControllerCallback#onBufferingStateChanged(MediaController2, MediaItem2, int)} is
     * called.
     *
     * @return buffering position in millis, or {@link MediaPlayerBase#UNKNOWN_TIME} if unknown.
     */
    public long getBufferedPosition() {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return UNKNOWN_TIME;
            }
            return (mPlaybackStateCompat == null) ? UNKNOWN_TIME
                    : mPlaybackStateCompat.getBufferedPosition();
        }
    }

    /**
     * Get the current playback info for this session.
     *
     * @return The current playback info or null.
     */
    public @Nullable PlaybackInfo getPlaybackInfo() {
        synchronized (mLock) {
            return mPlaybackInfo;
        }
    }

    /**
     * Rate the media. This will cause the rating to be set for the current user.
     * The rating style must follow the user rating style from the session.
     * You can get the rating style from the session through the
     * {@link MediaMetadata2#getRating(String)} with the key
     * {@link MediaMetadata2#METADATA_KEY_USER_RATING}.
     * <p>
     * If the user rating was {@code null}, the media item does not accept setting user rating.
     *
     * @param mediaId The id of the media
     * @param rating The rating to set
     */
    public void setRating(@NonNull String mediaId, @NonNull Rating2 rating) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            Bundle args = new Bundle();
            args.putString(ARGUMENT_MEDIA_ID, mediaId);
            args.putBundle(ARGUMENT_RATING, rating.toBundle());
            sendCommand(COMMAND_CODE_SESSION_SET_RATING, args);
        }
    }

    /**
     * Send custom command to the session
     *
     * @param command custom command
     * @param args optional argument
     * @param cb optional result receiver
     */
    public void sendCustomCommand(@NonNull SessionCommand2 command, @Nullable Bundle args,
            @Nullable ResultReceiver cb) {
        synchronized (mLock) {
            if (!mConnected) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return;
            }
            Bundle bundle = new Bundle();
            bundle.putBundle(ARGUMENT_CUSTOM_COMMAND, command.toBundle());
            bundle.putBundle(ARGUMENT_ARGUMENTS, args);
            sendCommand(CONTROLLER_COMMAND_BY_CUSTOM_COMMAND, bundle, cb);
        }
    }

    /**
     * Returns the cached playlist from {@link ControllerCallback#onPlaylistChanged}.
     * <p>
     * This list may differ with the list that was specified with
     * {@link #setPlaylist(List, MediaMetadata2)} depending on the {@link MediaPlaylistAgent}
     * implementation. Use media items returned here for other playlist agent APIs such as
     * {@link MediaPlaylistAgent#skipToPlaylistItem(MediaItem2)}.
     *
     * @return playlist. Can be {@code null} if the playlist hasn't set nor controller doesn't have
     *      enough permission.
     * @see SessionCommand2#COMMAND_CODE_PLAYLIST_GET_LIST
     */
    public @Nullable List<MediaItem2> getPlaylist() {
        synchronized (mLock) {
            return mPlaylist;
        }
    }

    /**
     * Sets the playlist.
     * <p>
     * Even when the playlist is successfully set, use the playlist returned from
     * {@link #getPlaylist()} for playlist APIs such as {@link #skipToPlaylistItem(MediaItem2)}.
     * Otherwise the session in the remote process can't distinguish between media items.
     *
     * @param list playlist
     * @param metadata metadata of the playlist
     * @see #getPlaylist()
     * @see ControllerCallback#onPlaylistChanged
     */
    public void setPlaylist(@NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata) {
        if (list == null) {
            throw new IllegalArgumentException("list shouldn't be null");
        }
        Bundle args = new Bundle();
        args.putParcelableArray(ARGUMENT_PLAYLIST, MediaUtils2.toMediaItem2ParcelableArray(list));
        args.putBundle(ARGUMENT_PLAYLIST_METADATA, metadata == null ? null : metadata.toBundle());
        sendCommand(COMMAND_CODE_PLAYLIST_SET_LIST, args);
    }

    /**
     * Updates the playlist metadata
     *
     * @param metadata metadata of the playlist
     */
    public void updatePlaylistMetadata(@Nullable MediaMetadata2 metadata) {
        Bundle args = new Bundle();
        args.putBundle(ARGUMENT_PLAYLIST_METADATA, metadata == null ? null : metadata.toBundle());
        sendCommand(COMMAND_CODE_PLAYLIST_SET_LIST_METADATA, args);
    }

    /**
     * Gets the lastly cached playlist playlist metadata either from
     * {@link ControllerCallback#onPlaylistMetadataChanged or
     * {@link ControllerCallback#onPlaylistChanged}.
     *
     * @return metadata metadata of the playlist, or null if none is set
     */
    public @Nullable MediaMetadata2 getPlaylistMetadata() {
        synchronized (mLock) {
            return mPlaylistMetadata;
        }
    }

    /**
     * Adds the media item to the playlist at position index. Index equals or greater than
     * the current playlist size (e.g. {@link Integer#MAX_VALUE}) will add the item at the end of
     * the playlist.
     * <p>
     * This will not change the currently playing media item.
     * If index is less than or equal to the current index of the playlist,
     * the current index of the playlist will be incremented correspondingly.
     *
     * @param index the index you want to add
     * @param item the media item you want to add
     */
    public void addPlaylistItem(int index, @NonNull MediaItem2 item) {
        Bundle args = new Bundle();
        args.putInt(ARGUMENT_PLAYLIST_INDEX, index);
        args.putBundle(ARGUMENT_MEDIA_ITEM, item.toBundle());
        sendCommand(COMMAND_CODE_PLAYLIST_ADD_ITEM, args);
    }

    /**
     * Removes the media item at index in the playlist.
     *<p>
     * If the item is the currently playing item of the playlist, current playback
     * will be stopped and playback moves to next source in the list.
     *
     * @param item the media item you want to add
     */
    public void removePlaylistItem(@NonNull MediaItem2 item) {
        Bundle args = new Bundle();
        args.putBundle(ARGUMENT_MEDIA_ITEM, item.toBundle());
        sendCommand(COMMAND_CODE_PLAYLIST_REMOVE_ITEM, args);
    }

    /**
     * Replace the media item at index in the playlist. This can be also used to update metadata of
     * an item.
     *
     * @param index the index of the item to replace
     * @param item the new item
     */
    public void replacePlaylistItem(int index, @NonNull MediaItem2 item) {
        Bundle args = new Bundle();
        args.putInt(ARGUMENT_PLAYLIST_INDEX, index);
        args.putBundle(ARGUMENT_MEDIA_ITEM, item.toBundle());
        sendCommand(COMMAND_CODE_PLAYLIST_REPLACE_ITEM, args);
    }

    /**
     * Get the lastly cached current item from
     * {@link ControllerCallback#onCurrentMediaItemChanged(MediaController2, MediaItem2)}.
     *
     * @return the currently playing item, or null if unknown.
     */
    public MediaItem2 getCurrentMediaItem() {
        synchronized (mLock) {
            return mCurrentMediaItem;
        }
    }

    /**
     * Skips to the previous item in the playlist.
     * <p>
     * This calls {@link MediaPlaylistAgent#skipToPreviousItem()}.
     */
    public void skipToPreviousItem() {
        sendCommand(COMMAND_CODE_PLAYLIST_SKIP_TO_PREV_ITEM);
    }

    /**
     * Skips to the next item in the playlist.
     * <p>
     * This calls {@link MediaPlaylistAgent#skipToNextItem()}.
     */
    public void skipToNextItem() {
        sendCommand(COMMAND_CODE_PLAYLIST_SKIP_TO_NEXT_ITEM);
    }

    /**
     * Skips to the item in the playlist.
     * <p>
     * This calls {@link MediaPlaylistAgent#skipToPlaylistItem(MediaItem2)}.
     *
     * @param item The item in the playlist you want to play
     */
    public void skipToPlaylistItem(@NonNull MediaItem2 item) {
        Bundle args = new Bundle();
        args.putBundle(ARGUMENT_MEDIA_ITEM, item.toBundle());
        sendCommand(COMMAND_CODE_PLAYLIST_SKIP_TO_PLAYLIST_ITEM, args);
    }

    /**
     * Gets the cached repeat mode from the {@link ControllerCallback#onRepeatModeChanged}.
     *
     * @return repeat mode
     * @see MediaPlaylistAgent#REPEAT_MODE_NONE
     * @see MediaPlaylistAgent#REPEAT_MODE_ONE
     * @see MediaPlaylistAgent#REPEAT_MODE_ALL
     * @see MediaPlaylistAgent#REPEAT_MODE_GROUP
     */
    public @RepeatMode int getRepeatMode() {
        synchronized (mLock) {
            return mRepeatMode;
        }
    }

    /**
     * Sets the repeat mode.
     *
     * @param repeatMode repeat mode
     * @see MediaPlaylistAgent#REPEAT_MODE_NONE
     * @see MediaPlaylistAgent#REPEAT_MODE_ONE
     * @see MediaPlaylistAgent#REPEAT_MODE_ALL
     * @see MediaPlaylistAgent#REPEAT_MODE_GROUP
     */
    public void setRepeatMode(@RepeatMode int repeatMode) {
        Bundle args = new Bundle();
        args.putInt(ARGUMENT_REPEAT_MODE, repeatMode);
        sendCommand(COMMAND_CODE_PLAYLIST_SET_REPEAT_MODE, args);
    }

    /**
     * Gets the cached shuffle mode from the {@link ControllerCallback#onShuffleModeChanged}.
     *
     * @return The shuffle mode
     * @see MediaPlaylistAgent#SHUFFLE_MODE_NONE
     * @see MediaPlaylistAgent#SHUFFLE_MODE_ALL
     * @see MediaPlaylistAgent#SHUFFLE_MODE_GROUP
     */
    public @ShuffleMode int getShuffleMode() {
        synchronized (mLock) {
            return mShuffleMode;
        }
    }

    /**
     * Sets the shuffle mode.
     *
     * @param shuffleMode The shuffle mode
     * @see MediaPlaylistAgent#SHUFFLE_MODE_NONE
     * @see MediaPlaylistAgent#SHUFFLE_MODE_ALL
     * @see MediaPlaylistAgent#SHUFFLE_MODE_GROUP
     */
    public void setShuffleMode(@ShuffleMode int shuffleMode) {
        Bundle args = new Bundle();
        args.putInt(ARGUMENT_SHUFFLE_MODE, shuffleMode);
        sendCommand(COMMAND_CODE_PLAYLIST_SET_SHUFFLE_MODE, args);
    }

    /**
     * Queries for information about the routes currently known.
     */
    public void subscribeRoutesInfo() {
        sendCommand(COMMAND_CODE_SESSION_SUBSCRIBE_ROUTES_INFO);
    }

    /**
     * Unsubscribes for changes to the routes.
     * <p>
     * The {@link ControllerCallback#onRoutesInfoChanged callback} will no longer be invoked for
     * the routes once this method returns.
     * </p>
     */
    public void unsubscribeRoutesInfo() {
        sendCommand(COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO);
    }

    /**
     * Selects the specified route.
     *
     * @param route The route to select.
     */
    public void selectRoute(@NonNull Bundle route) {
        if (route == null) {
            throw new IllegalArgumentException("route shouldn't be null");
        }
        Bundle args = new Bundle();
        args.putBundle(ARGUMENT_ROUTE_BUNDLE, route);
        sendCommand(COMMAND_CODE_SESSION_SELECT_ROUTE, args);
    }

    // Should be used without a lock to prevent potential deadlock.
    void onConnectedNotLocked(Bundle data) {
        // is enough or should we pass it while connecting?
        final SessionCommandGroup2 allowedCommands = SessionCommandGroup2.fromBundle(
                data.getBundle(ARGUMENT_ALLOWED_COMMANDS));
        final int playerState = data.getInt(ARGUMENT_PLAYER_STATE);
        final int bufferingState = data.getInt(ARGUMENT_BUFFERING_STATE);
        final PlaybackStateCompat playbackStateCompat = data.getParcelable(
                ARGUMENT_PLAYBACK_STATE_COMPAT);
        final int repeatMode = data.getInt(ARGUMENT_REPEAT_MODE);
        final int shuffleMode = data.getInt(ARGUMENT_SHUFFLE_MODE);
        final List<MediaItem2> playlist = MediaUtils2.fromMediaItem2ParcelableArray(
                data.getParcelableArray(ARGUMENT_PLAYLIST));
        final MediaItem2 currentMediaItem = MediaItem2.fromBundle(
                data.getBundle(ARGUMENT_MEDIA_ITEM));
        final PlaybackInfo playbackInfo =
                PlaybackInfo.fromBundle(data.getBundle(ARGUMENT_PLAYBACK_INFO));
        final MediaMetadata2 metadata = MediaMetadata2.fromBundle(
                data.getBundle(ARGUMENT_PLAYLIST_METADATA));
        if (DEBUG) {
            Log.d(TAG, "onConnectedNotLocked sessionCompatToken=" + mToken.getSessionCompatToken()
                    + ", allowedCommands=" + allowedCommands);
        }
        boolean close = false;
        try {
            synchronized (mLock) {
                if (mIsReleased) {
                    return;
                }
                if (mConnected) {
                    Log.e(TAG, "Cannot be notified about the connection result many times."
                            + " Probably a bug or malicious app.");
                    close = true;
                    return;
                }
                mAllowedCommands = allowedCommands;
                mPlayerState = playerState;
                mBufferingState = bufferingState;
                mPlaybackStateCompat = playbackStateCompat;
                mRepeatMode = repeatMode;
                mShuffleMode = shuffleMode;
                mPlaylist = playlist;
                mCurrentMediaItem = currentMediaItem;
                mPlaylistMetadata = metadata;
                mConnected = true;
                mPlaybackInfo = playbackInfo;
            }
            mCallbackExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    // Note: We may trigger ControllerCallbacks with the initial values
                    // But it's hard to define the order of the controller callbacks
                    // Only notify about the
                    mCallback.onConnected(MediaController2.this, allowedCommands);
                }
            });
        } finally {
            if (close) {
                // Trick to call release() without holding the lock, to prevent potential deadlock
                // with the developer's custom lock within the ControllerCallback.onDisconnected().
                close();
            }
        }
    }

    private void initialize() {
        if (mToken.getType() == SessionToken2.TYPE_SESSION) {
            synchronized (mLock) {
                mBrowserCompat = null;
            }
            connectToSession(mToken.getSessionCompatToken());
        } else {
            connectToService();
        }
    }

    private void connectToSession(MediaSessionCompat.Token sessionCompatToken) {
        MediaControllerCompat controllerCompat = null;
        try {
            controllerCompat = new MediaControllerCompat(mContext, sessionCompatToken);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        synchronized (mLock) {
            mControllerCompat = controllerCompat;
            mControllerCompatCallback = new ControllerCompatCallback();
            mControllerCompat.registerCallback(mControllerCompatCallback, mHandler);
        }

        if (controllerCompat.isSessionReady()) {
            sendCommand(CONTROLLER_COMMAND_CONNECT, new ResultReceiver(mHandler) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    if (!mHandlerThread.isAlive()) {
                        return;
                    }
                    switch (resultCode) {
                        case CONNECT_RESULT_CONNECTED:
                            onConnectedNotLocked(resultData);
                            break;
                        case CONNECT_RESULT_DISCONNECTED:
                            mCallback.onDisconnected(MediaController2.this);
                            close();
                            break;
                    }
                }
            });
        }
    }

    private void connectToService() {
        synchronized (mLock) {
            mBrowserCompat = new MediaBrowserCompat(mContext, mToken.getComponentName(),
                    new ConnectionCallback(), sDefaultRootExtras);
            mBrowserCompat.connect();
        }
    }

    private void sendCommand(int commandCode) {
        sendCommand(commandCode, null);
    }

    private void sendCommand(int commandCode, Bundle args) {
        if (args == null) {
            args = new Bundle();
        }
        args.putInt(ARGUMENT_COMMAND_CODE, commandCode);
        sendCommand(CONTROLLER_COMMAND_BY_COMMAND_CODE, args, null);
    }

    private void sendCommand(String command) {
        sendCommand(command, null, null);
    }

    private void sendCommand(String command, ResultReceiver receiver) {
        sendCommand(command, null, receiver);
    }

    private void sendCommand(String command, Bundle args, ResultReceiver receiver) {
        if (args == null) {
            args = new Bundle();
        }
        MediaControllerCompat controller;
        ControllerCompatCallback callback;
        synchronized (mLock) {
            controller = mControllerCompat;
            callback = mControllerCompatCallback;
        }
        args.putBinder(ARGUMENT_ICONTROLLER_CALLBACK, callback.getIControllerCallback().asBinder());
        args.putString(ARGUMENT_PACKAGE_NAME, mContext.getPackageName());
        args.putInt(ARGUMENT_UID, Process.myUid());
        args.putInt(ARGUMENT_PID, Process.myPid());
        controller.sendCommand(command, args, receiver);
    }

    @NonNull Context getContext() {
        return mContext;
    }

    @NonNull ControllerCallback getCallback() {
        return mCallback;
    }

    @NonNull Executor getCallbackExecutor() {
        return mCallbackExecutor;
    }

    @Nullable MediaBrowserCompat getBrowserCompat() {
        synchronized (mLock) {
            return mBrowserCompat;
        }
    }

    private class ConnectionCallback extends MediaBrowserCompat.ConnectionCallback {
        @Override
        public void onConnected() {
            MediaBrowserCompat browser = getBrowserCompat();
            if (browser != null) {
                connectToSession(browser.getSessionToken());
            } else if (DEBUG) {
                Log.d(TAG, "Controller is closed prematually", new IllegalStateException());
            }
        }

        @Override
        public void onConnectionSuspended() {
            close();
        }

        @Override
        public void onConnectionFailed() {
            close();
        }
    }
}
