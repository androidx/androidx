/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.session;

import static androidx.media2.common.MediaMetadata.METADATA_KEY_DISPLAY_TITLE;
import static androidx.media2.common.MediaMetadata.METADATA_KEY_TITLE;
import static androidx.media2.session.MediaUtils.TRANSACTION_SIZE_LIMIT_IN_BYTES;
import static androidx.media2.session.SessionCommand.COMMAND_VERSION_CURRENT;
import static androidx.media2.session.SessionResult.RESULT_SUCCESS;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;
import androidx.media.MediaSessionManager;
import androidx.media.MediaSessionManager.RemoteUserInfo;
import androidx.media.VolumeProviderCompat;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.Rating;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.SessionPlayer.PlayerResult;
import androidx.media2.common.SessionPlayer.TrackInfo;
import androidx.media2.common.SubtitleData;
import androidx.media2.common.VideoSize;
import androidx.media2.session.MediaController.PlaybackInfo;
import androidx.media2.session.MediaLibraryService.LibraryParams;
import androidx.media2.session.MediaSession.CommandButton;
import androidx.media2.session.MediaSession.ControllerCb;
import androidx.media2.session.MediaSession.ControllerInfo;
import androidx.media2.session.SessionCommand.CommandCode;

import java.io.Closeable;
import java.util.List;
import java.util.Set;

// Getting the commands from MediaControllerCompat'
class MediaSessionLegacyStub extends MediaSessionCompat.Callback implements Closeable {

    private static final String TAG = "MediaSessionLegacyStub";
    private static final String DEFAULT_MEDIA_SESSION_TAG_PREFIX = "androidx.media2.session.id";
    private static final String DEFAULT_MEDIA_SESSION_TAG_DELIM = ".";

    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    // Used to call onDisconnected() after the timeout.
    private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 300_000; // 5 min.

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final SparseArray<SessionCommand> sCommandsForOnCommandRequest =
            new SparseArray<>();

    static {
        SessionCommandGroup group = new SessionCommandGroup.Builder()
                .addAllPlayerCommands(COMMAND_VERSION_CURRENT)
                .addAllVolumeCommands(COMMAND_VERSION_CURRENT)
                .build();
        Set<SessionCommand> commands = group.getCommands();
        for (SessionCommand command : commands) {
            sCommandsForOnCommandRequest.append(command.getCommandCode(), command);
        }
    }

    final ConnectedControllersManager<RemoteUserInfo> mConnectedControllersManager;

    final MediaSession.MediaSessionImpl mSessionImpl;
    final MediaSessionManager mSessionManager;
    final Context mContext;
    final ControllerCb mControllerLegacyCbForBroadcast;
    final ConnectionTimeoutHandler mConnectionTimeoutHandler;
    final MediaSessionCompat mSessionCompat;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    volatile long mConnectionTimeoutMs;

    private final Handler mHandler;

    MediaSessionLegacyStub(MediaSession.MediaSessionImpl session,
            ComponentName mbrComponent, PendingIntent mediaButtonIntent, Handler handler) {
        mSessionImpl = session;
        mContext = mSessionImpl.getContext();
        mSessionManager = MediaSessionManager.getSessionManager(mContext);
        mControllerLegacyCbForBroadcast = new ControllerLegacyCbForBroadcast();
        mConnectionTimeoutHandler = new ConnectionTimeoutHandler(handler.getLooper());
        mConnectedControllersManager = new ConnectedControllersManager<>(session);
        mConnectionTimeoutMs = DEFAULT_CONNECTION_TIMEOUT_MS;
        mHandler = handler;

        String sessionCompatId = TextUtils.join(DEFAULT_MEDIA_SESSION_TAG_DELIM,
                new String[] {DEFAULT_MEDIA_SESSION_TAG_PREFIX, session.getId()});
        mSessionCompat = new MediaSessionCompat(mContext,
                sessionCompatId,
                mbrComponent,
                mediaButtonIntent, session.getToken().getExtras(),
                session.getToken());
        mSessionCompat.setSessionActivity(session.getSessionActivity());
        mSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS);
        // Note: Rest of mSessionCompat initialization will be done via
        // {@link ControllerLegacyCbForBroadcast#onPlayerChanged} and {@link #start}, called
        // indirectly by {@link MediaSessionImplBase#MediaSessionImplBase}.
    }

    /**
     * Starts to receive and send commands.
     */
    public void start() {
        mSessionCompat.setCallback(this, mHandler);
        mSessionCompat.setActive(true);
    }

    @Override
    public void close() {
        mSessionCompat.release();
    }

    public MediaSessionCompat getSessionCompat() {
        return mSessionCompat;
    }

    @Override
    public void onCommand(final String commandName, final Bundle args, final ResultReceiver cb) {
        if (commandName == null) {
            return;
        }
        final SessionCommand command = new SessionCommand(commandName, null);
        dispatchSessionTask(command, new SessionTask() {
            @Override
            public void run(final ControllerInfo controller) throws RemoteException {
                SessionResult result = mSessionImpl.getCallback().onCustomCommand(
                        mSessionImpl.getInstance(), controller, command, args);
                if (cb != null) {
                    cb.send(result.getResultCode(), result.getCustomCommandResult());
                }
            }
        });
    }

    @Override
    public void onPrepare() {
        dispatchSessionTask(SessionCommand.COMMAND_CODE_PLAYER_PREPARE, new SessionTask() {
            @SuppressWarnings("FutureReturnValueIgnored")
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                mSessionImpl.prepare();
            }
        });
    }

    @Override
    public void onPrepareFromMediaId(final String mediaId, final Bundle extras) {
        Uri mediaUri = new Uri.Builder()
                .scheme(MediaConstants.MEDIA_URI_SCHEME)
                .authority(MediaConstants.MEDIA_URI_AUTHORITY)
                .path(MediaConstants.MEDIA_URI_PATH_PREPARE_FROM_MEDIA_ID)
                .appendQueryParameter(MediaConstants.MEDIA_URI_QUERY_ID, mediaId)
                .build();
        onPrepareFromUri(mediaUri, extras);
    }

    @Override
    public void onPrepareFromSearch(final String query, final Bundle extras) {
        Uri mediaUri = new Uri.Builder()
                .scheme(MediaConstants.MEDIA_URI_SCHEME)
                .authority(MediaConstants.MEDIA_URI_AUTHORITY)
                .path(MediaConstants.MEDIA_URI_PATH_PREPARE_FROM_SEARCH)
                .appendQueryParameter(MediaConstants.MEDIA_URI_QUERY_QUERY, query)
                .build();
        onPrepareFromUri(mediaUri, extras);
    }

    @Override
    public void onPrepareFromUri(final Uri mediaUri, final Bundle extras) {
        dispatchSessionTask(SessionCommand.COMMAND_CODE_SESSION_SET_MEDIA_URI, new SessionTask() {
            @SuppressWarnings("FutureReturnValueIgnored")
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                if (mSessionImpl.getCallback().onSetMediaUri(mSessionImpl.getInstance(),
                        controller, mediaUri, extras) == RESULT_SUCCESS) {
                    mSessionImpl.prepare();
                }
            }
        });
    }

    @Override
    public void onPlay() {
        dispatchSessionTask(SessionCommand.COMMAND_CODE_PLAYER_PLAY, new SessionTask() {
            @SuppressWarnings("FutureReturnValueIgnored")
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                mSessionImpl.play();
            }
        });
    }

    @Override
    public void onPlayFromMediaId(final String mediaId, final Bundle extras) {
        Uri mediaUri = new Uri.Builder()
                .scheme(MediaConstants.MEDIA_URI_SCHEME)
                .authority(MediaConstants.MEDIA_URI_AUTHORITY)
                .path(MediaConstants.MEDIA_URI_PATH_PLAY_FROM_MEDIA_ID)
                .appendQueryParameter(MediaConstants.MEDIA_URI_QUERY_ID, mediaId)
                .build();
        onPlayFromUri(mediaUri, extras);
    }

    @Override
    public void onPlayFromSearch(final String query, final Bundle extras) {
        Uri mediaUri = new Uri.Builder()
                .scheme(MediaConstants.MEDIA_URI_SCHEME)
                .authority(MediaConstants.MEDIA_URI_AUTHORITY)
                .path(MediaConstants.MEDIA_URI_PATH_PLAY_FROM_SEARCH)
                .appendQueryParameter(MediaConstants.MEDIA_URI_QUERY_QUERY, query)
                .build();
        onPlayFromUri(mediaUri, extras);
    }

    @Override
    public void onPlayFromUri(final Uri mediaUri, final Bundle extras) {
        dispatchSessionTask(SessionCommand.COMMAND_CODE_SESSION_SET_MEDIA_URI, new SessionTask() {
            @SuppressWarnings("FutureReturnValueIgnored")
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                if (mSessionImpl.getCallback().onSetMediaUri(mSessionImpl.getInstance(),
                        controller, mediaUri, extras) == RESULT_SUCCESS) {
                    mSessionImpl.play();
                }
            }
        });
    }

    @Override
    public void onPause() {
        dispatchSessionTask(SessionCommand.COMMAND_CODE_PLAYER_PAUSE, new SessionTask() {
            @SuppressWarnings("FutureReturnValueIgnored")
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                mSessionImpl.pause();
            }
        });
    }

    @Override
    public void onStop() {
        // Here, we don't call SessionPlayer#reset() since it may result removing
        // all callbacks from the player. Instead, we pause and seek to zero.
        // Here, we check both permissions: Pause / SeekTo.
        dispatchSessionTask(SessionCommand.COMMAND_CODE_PLAYER_PAUSE, new SessionTask() {
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                handleTaskOnExecutor(controller, null,
                        SessionCommand.COMMAND_CODE_PLAYER_SEEK_TO, new SessionTask() {
                            @SuppressWarnings("FutureReturnValueIgnored")
                            @Override
                            public void run(ControllerInfo controller) throws RemoteException {
                                mSessionImpl.pause();
                                mSessionImpl.seekTo(0);
                            }
                        });
            }
        });
    }

    @Override
    public void onSeekTo(final long pos) {
        dispatchSessionTask(SessionCommand.COMMAND_CODE_PLAYER_SEEK_TO, new SessionTask() {
            @SuppressWarnings("FutureReturnValueIgnored")
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                mSessionImpl.seekTo(pos);
            }
        });
    }

    @Override
    public void onSkipToNext() {
        dispatchSessionTask(SessionCommand.COMMAND_CODE_PLAYER_SKIP_TO_NEXT_PLAYLIST_ITEM,
                new SessionTask() {
                    @SuppressWarnings("FutureReturnValueIgnored")
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.skipToNextItem();
                    }
                });
    }

    @Override
    public void onSkipToPrevious() {
        dispatchSessionTask(SessionCommand.COMMAND_CODE_PLAYER_SKIP_TO_PREVIOUS_PLAYLIST_ITEM,
                new SessionTask() {
                    @SuppressWarnings("FutureReturnValueIgnored")
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.skipToPreviousItem();
                    }
                });
    }

    @Override
    public void onSetPlaybackSpeed(final float speed) {
        dispatchSessionTask(SessionCommand.COMMAND_CODE_PLAYER_SET_SPEED, new SessionTask() {
            @SuppressWarnings("FutureReturnValueIgnored")
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                mSessionImpl.setPlaybackSpeed(speed);
            }
        });
    }

    @Override
    public void onSkipToQueueItem(final long queueId) {
        dispatchSessionTask(SessionCommand.COMMAND_CODE_PLAYER_SKIP_TO_PLAYLIST_ITEM,
                new SessionTask() {
                    @SuppressWarnings("FutureReturnValueIgnored")
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        List<MediaItem> playlist = mSessionImpl.getPlayer().getPlaylist();
                        if (playlist == null) {
                            return;
                        }
                        // Use queueId as an index as we've published {@link QueueItem} as so.
                        // see: {@link MediaUtils#convertToQueueItemList}.
                        mSessionImpl.skipToPlaylistItem((int) queueId);
                    }
                });
    }

    @Override
    public void onFastForward() {
        dispatchSessionTask(SessionCommand.COMMAND_CODE_SESSION_FAST_FORWARD,
                new SessionTask() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.getCallback().onFastForward(
                                mSessionImpl.getInstance(), controller);
                    }
                });
    }

    @Override
    public void onRewind() {
        dispatchSessionTask(SessionCommand.COMMAND_CODE_SESSION_REWIND,
                new SessionTask() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.getCallback().onRewind(mSessionImpl.getInstance(), controller);
                    }
                });
    }

    @Override
    public void onSetRating(final RatingCompat rating) {
        onSetRating(rating, null);
    }

    @Override
    public void onSetRating(final RatingCompat rating, Bundle extras) {
        if (rating == null) {
            return;
        }
        // extras is ignored.
        dispatchSessionTask(SessionCommand.COMMAND_CODE_SESSION_SET_RATING,
                new SessionTask() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        MediaItem currentItem = mSessionImpl.getCurrentMediaItem();
                        if (currentItem == null) {
                            return;
                        }
                        mSessionImpl.getCallback().onSetRating(mSessionImpl.getInstance(),
                                controller, currentItem.getMediaId(),
                                MediaUtils.convertToRating(rating));
                    }
                });
    }

    @Override
    public void onCustomAction(@NonNull String action, @Nullable Bundle extras) {
        // no-op
    }

    @Override
    public void onSetCaptioningEnabled(boolean enabled) {
        // no-op
    }

    @Override
    public void onSetRepeatMode(final int repeatMode) {
        dispatchSessionTask(SessionCommand.COMMAND_CODE_PLAYER_SET_REPEAT_MODE,
                new SessionTask() {
                    @SuppressWarnings("FutureReturnValueIgnored")
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.setRepeatMode(repeatMode);
                    }
                });
    }

    @Override
    public void onSetShuffleMode(final int shuffleMode) {
        dispatchSessionTask(SessionCommand.COMMAND_CODE_PLAYER_SET_SHUFFLE_MODE,
                new SessionTask() {
                    @SuppressWarnings("FutureReturnValueIgnored")
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.setShuffleMode(shuffleMode);
                    }
                });
    }

    @Override
    public void onAddQueueItem(final MediaDescriptionCompat description) {
        onAddQueueItem(description, Integer.MAX_VALUE);
    }

    @Override
    public void onAddQueueItem(final MediaDescriptionCompat description, final int index) {
        if (description == null) {
            return;
        }
        dispatchSessionTask(SessionCommand.COMMAND_CODE_PLAYER_ADD_PLAYLIST_ITEM,
                new SessionTask() {
                    @SuppressWarnings("FutureReturnValueIgnored")
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        String mediaId = description.getMediaId();
                        if (TextUtils.isEmpty(mediaId)) {
                            Log.w(TAG, "onAddQueueItem(): Media ID shouldn't be empty");
                            return;
                        }
                        MediaItem newItem = mSessionImpl.getCallback().onCreateMediaItem(
                                mSessionImpl.getInstance(), controller, mediaId);
                        mSessionImpl.addPlaylistItem(index, newItem);
                    }
                });
    }

    @Override
    public void onRemoveQueueItem(final MediaDescriptionCompat description) {
        if (description == null) {
            return;
        }
        dispatchSessionTask(SessionCommand.COMMAND_CODE_PLAYER_REMOVE_PLAYLIST_ITEM,
                new SessionTask() {
                    @SuppressWarnings("FutureReturnValueIgnored")
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        String mediaId = description.getMediaId();
                        if (TextUtils.isEmpty(mediaId)) {
                            Log.w(TAG, "onRemoveQueueItem(): Media ID shouldn't be null");
                            return;
                        }
                        List<MediaItem> playlist = mSessionImpl.getPlaylist();
                        for (int i = 0; i < playlist.size(); i++) {
                            MediaItem item = playlist.get(i);
                            if (TextUtils.equals(item.getMediaId(), mediaId)) {
                                mSessionImpl.removePlaylistItem(i);
                                return;
                            }
                        }
                    }
                });
    }

    @Override
    public void onRemoveQueueItemAt(final int index) {
        dispatchSessionTask(SessionCommand.COMMAND_CODE_PLAYER_REMOVE_PLAYLIST_ITEM,
                new SessionTask() {
                    @SuppressWarnings("FutureReturnValueIgnored")
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        if (index < 0) {
                            Log.w(TAG, "onRemoveQueueItem(): index shouldn't be negative");
                            return;
                        }
                        mSessionImpl.removePlaylistItem(index);
                    }
                });
    }

    ControllerCb getControllerLegacyCbForBroadcast() {
        return mControllerLegacyCbForBroadcast;
    }

    ConnectedControllersManager<RemoteUserInfo> getConnectedControllersManager() {
        return mConnectedControllersManager;
    }

    private void dispatchSessionTask(@CommandCode final int commandCode,
            @NonNull final SessionTask task) {
        dispatchSessionTaskInternal(null, commandCode, task);
    }

    private void dispatchSessionTask(@NonNull final SessionCommand sessionCommand,
            @NonNull final SessionTask task) {
        dispatchSessionTaskInternal(sessionCommand, SessionCommand.COMMAND_CODE_CUSTOM, task);
    }

    @SuppressWarnings("ObjectToString")
    private void dispatchSessionTaskInternal(@Nullable final SessionCommand sessionCommand,
            @CommandCode final int commandCode, @NonNull final SessionTask task) {
        if (mSessionImpl.isClosed()) {
            return;
        }
        final RemoteUserInfo remoteUserInfo =
                mSessionCompat.getCurrentControllerInfo();
        if (remoteUserInfo == null) {
            Log.d(TAG, "RemoteUserInfo is null, ignoring command=" + sessionCommand
                    + ", commandCode=" + commandCode);
            return;
        }
        mSessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (mSessionImpl.isClosed()) {
                    return;
                }
                ControllerInfo controller =
                        mConnectedControllersManager.getController(remoteUserInfo);
                if (controller == null) {
                    // Try connect.
                    controller = new ControllerInfo(
                            remoteUserInfo, MediaUtils.VERSION_UNKNOWN,
                            mSessionManager.isTrustedForMediaControl(remoteUserInfo),
                            new ControllerLegacyCb(remoteUserInfo), /* connectionHints= */ null);

                    SessionCommandGroup allowedCommands = mSessionImpl.getCallback().onConnect(
                            mSessionImpl.getInstance(), controller);
                    if (allowedCommands == null) {
                        try {
                            controller.getControllerCb().onDisconnected(/* seq= */ 0);
                        } catch (RemoteException ex) {
                            // Controller may have died prematurely.
                        }
                        return;
                    }
                    mConnectedControllersManager.addController(
                            controller.getRemoteUserInfo(), controller, allowedCommands);
                }

                // Reset disconnect timeout.
                mConnectionTimeoutHandler.disconnectControllerAfterTimeout(
                        controller, mConnectionTimeoutMs);
                handleTaskOnExecutor(controller, sessionCommand, commandCode, task);
            }
        });
    }

    /* synthetic access */
    @SuppressWarnings({"WeakerAccess", "ObjectToString"})
    void handleTaskOnExecutor(@NonNull final ControllerInfo controller,
            @Nullable final SessionCommand sessionCommand, @CommandCode final int commandCode,
            @NonNull final SessionTask task) {
        SessionCommand command;
        if (sessionCommand != null) {
            if (!mConnectedControllersManager.isAllowedCommand(controller, sessionCommand)) {
                return;
            }
            command = sCommandsForOnCommandRequest.get(sessionCommand.getCommandCode());
        } else {
            if (!mConnectedControllersManager.isAllowedCommand(controller, commandCode)) {
                return;
            }
            command = sCommandsForOnCommandRequest.get(commandCode);
        }
        if (command != null) {
            int resultCode = mSessionImpl.getCallback().onCommandRequest(
                    mSessionImpl.getInstance(), controller, command);
            if (resultCode != RESULT_SUCCESS) {
                // Don't run rejected command.
                if (DEBUG) {
                    Log.d(TAG, "Command (" + command + ") from "
                            + controller + " was rejected by " + mSessionImpl);
                }
                return;
            }
        }
        try {
            task.run(controller);
        } catch (RemoteException e) {
            // Currently it's TransactionTooLargeException or DeadSystemException.
            // We'd better to leave log for those cases because
            //   - TransactionTooLargeException means that we may need to fix our code.
            //     (e.g. add pagination or special way to deliver Bitmap)
            //   - DeadSystemException means that errors around it can be ignored.
            Log.w(TAG, "Exception in " + controller, e);
        }
    }

    public void setLegacyControllerDisconnectTimeoutMs(long timeoutMs) {
        mConnectionTimeoutMs = timeoutMs;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static VolumeProviderCompat createVolumeProviderCompat(
            @NonNull RemoteSessionPlayer player) {
        return new VolumeProviderCompat(player.getVolumeControlType(), player.getMaxVolume(),
                player.getVolume()) {
            @SuppressWarnings("FutureReturnValueIgnored")
            @Override
            public void onSetVolumeTo(int volume) {
                player.setVolume(volume);
            }

            @SuppressWarnings("FutureReturnValueIgnored")
            @Override
            public void onAdjustVolume(int direction) {
                player.adjustVolume(direction);
            }
        };
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static int getRatingType(@Nullable Rating rating) {
        if (rating instanceof HeartRating) {
            return RatingCompat.RATING_HEART;
        } else if (rating instanceof ThumbRating) {
            return RatingCompat.RATING_THUMB_UP_DOWN;
        } else if (rating instanceof StarRating) {
            switch (((StarRating) rating).getMaxStars()) {
                case 3:
                    return RatingCompat.RATING_3_STARS;
                case 4:
                    return RatingCompat.RATING_4_STARS;
                case 5:
                    return RatingCompat.RATING_5_STARS;
                default:
                    return RatingCompat.RATING_NONE;
            }
        } else if (rating instanceof PercentageRating) {
            return RatingCompat.RATING_PERCENTAGE;
        }
        return RatingCompat.RATING_NONE;
    }

    @FunctionalInterface
    private interface SessionTask {
        void run(ControllerInfo controller) throws RemoteException;
    }

    @SuppressWarnings("ClassCanBeStatic")
    final class ControllerLegacyCb extends MediaSession.ControllerCb {
        private final RemoteUserInfo mRemoteUserInfo;

        ControllerLegacyCb(RemoteUserInfo remoteUserInfo) {
            mRemoteUserInfo = remoteUserInfo;
        }

        @Override
        void onPlayerResult(int seq, PlayerResult result) throws RemoteException {
            // no-op.
        }

        @Override
        void onSessionResult(int seq, SessionResult result) throws RemoteException {
            // no-op.
        }

        @Override
        void onLibraryResult(int seq, LibraryResult result) throws RemoteException {
            // no-op
        }

        @Override
        void onPlayerChanged(int seq, @Nullable SessionPlayer oldPlayer,
                @Nullable PlaybackInfo oldPlaybackInfo, @NonNull SessionPlayer player,
                @NonNull PlaybackInfo playbackInfo) throws RemoteException {
            // no-op
        }

        @Override
        void setCustomLayout(int seq, @NonNull List<CommandButton> layout) throws RemoteException {
            // no-op.
        }

        @Override
        void onPlaybackInfoChanged(int seq, @NonNull PlaybackInfo info) throws RemoteException {
            throw new AssertionError("This shouldn't be called");
        }

        @Override
        void onAllowedCommandsChanged(int seq, @NonNull SessionCommandGroup commands)
                throws RemoteException {
            // no-op
        }

        @Override
        void sendCustomCommand(int seq, @NonNull SessionCommand command, Bundle args)
                throws RemoteException {
            // no-op
        }

        @Override
        void onPlayerStateChanged(int seq, long eventTimeMs, long positionMs, int playerState)
                throws RemoteException {
            throw new AssertionError("This shouldn't be called");
        }

        @Override
        void onPlaybackSpeedChanged(int seq, long eventTimeMs, long positionMs, float speed)
                throws RemoteException {
            throw new AssertionError("This shouldn't be called");
        }

        @Override
        void onBufferingStateChanged(int seq, @NonNull MediaItem item, int bufferingState,
                long bufferedPositionMs, long eventTimeMs, long positionMs) throws RemoteException {
            throw new AssertionError("This shouldn't be called");
        }

        @Override
        void onSeekCompleted(int seq, long eventTimeMs, long positionMs, long position)
                throws RemoteException {
            throw new AssertionError("This shouldn't be called");
        }

        @Override
        void onCurrentMediaItemChanged(int seq, MediaItem item, int currentIdx, int previousIdx,
                int nextIdx) throws RemoteException {
            throw new AssertionError("This shouldn't be called");
        }

        @Override
        void onPlaylistChanged(int seq, @NonNull List<MediaItem> playlist, MediaMetadata metadata,
                int currentIdx, int previousIdx, int nextIdx) throws RemoteException {
            throw new AssertionError("This shouldn't be called");
        }

        @Override
        void onPlaylistMetadataChanged(int seq, MediaMetadata metadata) throws RemoteException {
            throw new AssertionError("This shouldn't be called");
        }

        @Override
        void onShuffleModeChanged(int seq, int shuffleMode, int currentIdx, int previousIdx,
                int nextIdx) throws RemoteException {
            throw new AssertionError("This shouldn't be called");
        }

        @Override
        void onRepeatModeChanged(int seq, int repeatMode, int currentIdx, int previousIdx,
                int nextIdx) throws RemoteException {
            throw new AssertionError("This shouldn't be called");
        }

        @Override
        void onPlaybackCompleted(int seq) throws RemoteException {
            throw new AssertionError("This shouldn't be called");
        }

        @Override
        void onChildrenChanged(int seq, @NonNull String parentId, int itemCount,
                LibraryParams params) throws RemoteException {
            // no-op
        }
        @Override
        void onSearchResultChanged(int seq, @NonNull String query, int itemCount,
                LibraryParams params) throws RemoteException {
            // no-op
        }

        @Override
        void onDisconnected(int seq) throws RemoteException {
            // no-op
        }

        @Override
        void onVideoSizeChanged(int seq, @NonNull VideoSize videoSize) throws RemoteException {
            // no-op
        }

        @Override
        void onTracksChanged(int seq, List<TrackInfo> tracks,
                TrackInfo selectedVideoTrack, TrackInfo selectedAudioTrack,
                TrackInfo selectedSubtitleTrack, TrackInfo selectedMetadataTrack)
                throws RemoteException {
            // no-op
        }

        @Override
        void onTrackSelected(int seq, TrackInfo trackInfo) throws RemoteException {
            // no-op
        }

        @Override
        void onTrackDeselected(int seq, TrackInfo trackInfo) throws RemoteException {
            // no-op
        }

        @Override
        void onSubtitleData(int seq, @NonNull MediaItem item,
                @NonNull TrackInfo track, @NonNull SubtitleData data) {
            // no-op
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(mRemoteUserInfo);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != ControllerLegacyCb.class) {
                return false;
            }
            ControllerLegacyCb other = (ControllerLegacyCb) obj;
            return ObjectsCompat.equals(mRemoteUserInfo, other.mRemoteUserInfo);
        }
    }

    // TODO: Find a way to notify error through PlaybackStateCompat
    final class ControllerLegacyCbForBroadcast extends MediaSession.ControllerCb {
        ControllerLegacyCbForBroadcast() {
        }

        @Override
        void onPlayerResult(int seq, PlayerResult result) throws RemoteException {
            // no-op
        }

        @Override
        void onSessionResult(int seq, SessionResult result) throws RemoteException {
            // no-op
        }

        @Override
        void onLibraryResult(int seq, LibraryResult result) throws RemoteException {
            // no-op
        }

        @Override
        void onPlayerChanged(int seq, @Nullable SessionPlayer oldPlayer,
                @Nullable PlaybackInfo oldPlaybackInfo,
                @NonNull SessionPlayer player, @NonNull PlaybackInfo playbackInfo)
                throws RemoteException {
            // Tells the playlist change first, so current media item index change notification
            // can point to the valid current media item in the playlist.
            if (oldPlayer == null
                    || !ObjectsCompat.equals(oldPlayer.getPlaylist(), player.getPlaylist())) {
                onPlaylistChanged(seq,
                        player.getPlaylist(), player.getPlaylistMetadata(),
                        player.getCurrentMediaItemIndex(),
                        player.getPreviousMediaItemIndex(),
                        player.getNextMediaItemIndex());
            } else if (oldPlayer == null
                    || !ObjectsCompat.equals(oldPlayer.getPlaylistMetadata(),
                            player.getPlaylistMetadata())) {
                onPlaylistMetadataChanged(seq, player.getPlaylistMetadata());
            }
            if (oldPlayer == null || oldPlayer.getShuffleMode() != player.getShuffleMode()) {
                onShuffleModeChanged(seq, player.getShuffleMode(),
                        player.getCurrentMediaItemIndex(), player.getPreviousMediaItemIndex(),
                        player.getNextMediaItemIndex());
            }
            if (oldPlayer == null || oldPlayer.getRepeatMode() != player.getRepeatMode()) {
                onRepeatModeChanged(seq, player.getRepeatMode(),
                        player.getCurrentMediaItemIndex(), player.getPreviousMediaItemIndex(),
                        player.getNextMediaItemIndex());
            }
            if (oldPlayer == null
                    || !ObjectsCompat.equals(oldPlayer.getCurrentMediaItem(),
                            player.getCurrentMediaItem())) {
                // Note: This will update PlaybackStateCompat.
                onCurrentMediaItemChanged(seq, player.getCurrentMediaItem(),
                        player.getCurrentMediaItemIndex(), player.getPreviousMediaItemIndex(),
                        player.getNextMediaItemIndex());
            } else {
                // If PlaybackStateCompat isn't updated by above if-statement, forcefully update
                // PlaybackStateCompat to tell the latest position and its event
                // time. This would also update playback speed and buffering/player state.
                mSessionCompat.setPlaybackState(mSessionImpl.createPlaybackStateCompat());
            }

            // Forcefully update playback info to update VolumeProviderCompat attached to the
            // old player.
            onPlaybackInfoChanged(seq, playbackInfo);
        }

        @Override
        void setCustomLayout(int seq, @NonNull List<CommandButton> layout) throws RemoteException {
            throw new AssertionError("This shouldn't be called");
        }

        @Override
        void onPlaybackInfoChanged(int seq,
                @NonNull PlaybackInfo playbackInfo) throws RemoteException {
            if (playbackInfo.getPlaybackType() == PlaybackInfo.PLAYBACK_TYPE_REMOTE) {
                VolumeProviderCompat volumeProviderCompat =
                        createVolumeProviderCompat(
                                (RemoteSessionPlayer) mSessionImpl.getPlayer());
                mSessionCompat.setPlaybackToRemote(volumeProviderCompat);
            } else {
                int stream = MediaUtils.getLegacyStreamType(playbackInfo.getAudioAttributes());
                mSessionCompat.setPlaybackToLocal(stream);
            }
        }

        @Override
        void onAllowedCommandsChanged(int seq, @NonNull SessionCommandGroup commands)
                throws RemoteException {
            throw new AssertionError("This shouldn't be called");
        }

        @Override
        void sendCustomCommand(int seq, @NonNull SessionCommand command, Bundle args)
                throws RemoteException {
            // no-op
        }

        @Override
        void onPlayerStateChanged(int seq, long eventTimeMs, long positionMs, int playerState)
                throws RemoteException {
            // Note: This method does not use any of the given arguments.
            mSessionCompat.setPlaybackState(
                    mSessionImpl.createPlaybackStateCompat());
        }

        @Override
        void onPlaybackSpeedChanged(int seq, long eventTimeMs, long positionMs, float speed)
                throws RemoteException {
            // Note: This method does not use any of the given arguments.
            mSessionCompat.setPlaybackState(
                    mSessionImpl.createPlaybackStateCompat());
        }

        @Override
        void onBufferingStateChanged(int seq, @NonNull MediaItem item, int bufferingState,
                long bufferedPositionMs, long eventTimeMs, long positionMs) throws RemoteException {
            // Note: This method does not use any of the given arguments.
            mSessionCompat.setPlaybackState(
                    mSessionImpl.createPlaybackStateCompat());
        }

        @Override
        void onSeekCompleted(int seq, long eventTimeMs, long positionMs, long position)
                throws RemoteException {
            // Note: This method does not use any of the given arguments.
            mSessionCompat.setPlaybackState(
                    mSessionImpl.createPlaybackStateCompat());
        }

        @Override
        void onCurrentMediaItemChanged(int seq, MediaItem item, int currentIdx, int previousIdx,
                int nextIdx) throws RemoteException {
            MediaMetadata metadata = (item == null) ? null : item.getMetadata();
            mSessionCompat.setMetadata(MediaUtils.convertToMediaMetadataCompat(metadata));
            int ratingType = getRatingType(
                    (metadata == null)
                            ? null
                            : metadata.getRating(MediaMetadata.METADATA_KEY_USER_RATING));
            mSessionCompat.setRatingType(ratingType);
            mSessionCompat.setPlaybackState(
                    mSessionImpl.createPlaybackStateCompat());
        }

        @Override
        void onPlaylistChanged(int seq, @NonNull List<MediaItem> playlist, MediaMetadata metadata,
                int currentIdx, int previousIdx, int nextIdx) throws RemoteException {
            if (Build.VERSION.SDK_INT < 21) {
                if (playlist == null) {
                    mSessionCompat.setQueue(null);
                } else {
                    // In order to avoid TransactionTooLargeException for below API 21, we need to
                    // cut the list so that it doesn't exceed the binder transaction limit.
                    List<QueueItem> queueItemList = MediaUtils.convertToQueueItemList(playlist);
                    List<QueueItem> truncatedList = MediaUtils.truncateListBySize(
                            queueItemList, TRANSACTION_SIZE_LIMIT_IN_BYTES);
                    if (truncatedList.size() != playlist.size()) {
                        Log.i(TAG, "Sending " + truncatedList.size() + " items out of "
                                + playlist.size());
                    }
                    mSessionCompat.setQueue(truncatedList);
                }

            } else {
                // Framework MediaSession#setQueue() uses ParceledListSlice,
                // which means we can safely send long lists.
                mSessionCompat.setQueue(
                        MediaUtils.convertToQueueItemList(playlist));
            }
            onPlaylistMetadataChanged(seq, metadata);
        }

        @Override
        void onPlaylistMetadataChanged(int seq, MediaMetadata metadata) throws RemoteException {
            // Since there is no 'queue metadata', only set title of the queue.
            CharSequence oldTitle = mSessionCompat.getController().getQueueTitle();
            CharSequence newTitle = null;

            if (metadata != null) {
                newTitle = metadata.getText(METADATA_KEY_DISPLAY_TITLE);
                if (newTitle == null) {
                    newTitle = metadata.getText(METADATA_KEY_TITLE);
                }
            }

            if (!TextUtils.equals(oldTitle, newTitle)) {
                mSessionCompat.setQueueTitle(newTitle);
            }
        }

        @Override
        void onShuffleModeChanged(int seq, int shuffleMode, int currentIdx, int previousIdx,
                int nextIdx) throws RemoteException {
            mSessionCompat.setShuffleMode(shuffleMode);
        }

        @Override
        void onRepeatModeChanged(int seq, int repeatMode, int currentIdx, int previousIdx,
                int nextIdx) throws RemoteException {
            mSessionCompat.setRepeatMode(repeatMode);
        }

        @Override
        void onPlaybackCompleted(int seq) throws RemoteException {
            PlaybackStateCompat state = mSessionImpl.createPlaybackStateCompat();
            if (state.getState() != PlaybackStateCompat.STATE_PAUSED) {
                state = new PlaybackStateCompat.Builder(state)
                        .setState(PlaybackStateCompat.STATE_PAUSED, state.getPosition(),
                                state.getPlaybackSpeed())
                        .build();
            }
            mSessionCompat.setPlaybackState(state);
        }

        @Override
        void onChildrenChanged(int seq, @NonNull String parentId, int itemCount,
                LibraryParams params) throws RemoteException {
            // no-op
        }
        @Override
        void onSearchResultChanged(int seq, @NonNull String query, int itemCount,
                LibraryParams params) throws RemoteException {
            // no-op
        }

        @Override
        void onDisconnected(int seq) throws RemoteException {
            // no-op. Calling MediaSessionCompat#release() is already done in close().
        }

        @Override
        void onVideoSizeChanged(int seq, @NonNull VideoSize videoSize) throws RemoteException {
            // no-op
        }

        @Override
        void onTracksChanged(int seq, List<TrackInfo> tracks,
                TrackInfo selectedVideoTrack, TrackInfo selectedAudioTrack,
                TrackInfo selectedSubtitleTrack, TrackInfo selectedMetadataTrack)
                throws RemoteException {
            // no-op
        }

        @Override
        void onTrackSelected(int seq, TrackInfo trackInfo) throws RemoteException {
            // no-op
        }

        @Override
        void onTrackDeselected(int seq, TrackInfo trackInfo) throws RemoteException {
            // no-op
        }

        @Override
        void onSubtitleData(int seq, @NonNull MediaItem item,
                @NonNull TrackInfo track, @NonNull SubtitleData data) {
            // no-op
        }
    }

    private class ConnectionTimeoutHandler extends Handler {
        private static final int MSG_CONNECTION_TIMED_OUT = 1001;

        ConnectionTimeoutHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            ControllerInfo controller = (ControllerInfo) msg.obj;
            if (mConnectedControllersManager.isConnected(controller)) {
                try {
                    controller.getControllerCb().onDisconnected(/* seq= */ 0);
                } catch (RemoteException ex) {
                    // Controller may have died prematurely.
                }
                mConnectedControllersManager.removeController(controller);
            }
        }

        public void disconnectControllerAfterTimeout(@NonNull ControllerInfo controller,
                long disconnectTimeoutMs) {
            removeMessages(MSG_CONNECTION_TIMED_OUT, controller);
            Message msg = obtainMessage(MSG_CONNECTION_TIMED_OUT, controller);
            sendMessageDelayed(msg, disconnectTimeoutMs);
        }
    }
}
