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

package androidx.media2;

import static androidx.media2.MediaMetadata2.METADATA_KEY_DISPLAY_TITLE;
import static androidx.media2.MediaMetadata2.METADATA_KEY_TITLE;
import static androidx.media2.SessionCommand2.COMMAND_CODE_CUSTOM;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaSessionManager;
import androidx.media.MediaSessionManager.RemoteUserInfo;
import androidx.media2.MediaController2.PlaybackInfo;
import androidx.media2.MediaSession2.CommandButton;
import androidx.media2.MediaSession2.ControllerCb;
import androidx.media2.MediaSession2.ControllerInfo;

import java.util.List;
import java.util.Set;

@TargetApi(Build.VERSION_CODES.KITKAT)
// Getting the commands from MediaControllerCompat'
class MediaSessionLegacyStub extends MediaSessionCompat.Callback {

    private static final String TAG = "MediaSessionLegacyStub";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final SparseArray<SessionCommand2> sCommandsForOnCommandRequest =
            new SparseArray<>();

    static {
        SessionCommandGroup2 group = new SessionCommandGroup2.Builder()
                .addAllPlaybackCommands()
                .addAllPlaylistCommands()
                .addAllVolumeCommands()
                .build();
        Set<SessionCommand2> commands = group.getCommands();
        for (SessionCommand2 command : commands) {
            sCommandsForOnCommandRequest.append(command.getCommandCode(), command);
        }
    }

    final ConnectedControllersManager<RemoteUserInfo> mConnectedControllersManager;

    final Object mLock = new Object();

    final MediaSession2.SupportLibraryImpl mSession;
    final MediaSessionManager mSessionManager;
    final Context mContext;
    final ControllerInfo mControllerInfoForAll;

    MediaSessionLegacyStub(MediaSession2.SupportLibraryImpl session) {
        mSession = session;
        mContext = mSession.getContext();
        mSessionManager = MediaSessionManager.getSessionManager(mContext);
        mControllerInfoForAll = new ControllerInfo(
                new RemoteUserInfo(
                        RemoteUserInfo.LEGACY_CONTROLLER, Process.myPid(), Process.myUid()),
                false /* trusted */,
                new ControllerLegacyCbForAll());
        mConnectedControllersManager = new ConnectedControllersManager<>(session);
    }

    @Override
    public void onCommand(final String commandName, final Bundle args, final ResultReceiver cb) {
        if (commandName == null) {
            return;
        }
        final SessionCommand2 command = new SessionCommand2(commandName, null);
        onSessionCommand(command, new SessionRunnable() {
            @Override
            public void run(final ControllerInfo controller) throws RemoteException {
                mSession.getCallback().onCustomCommand(
                        mSession.getInstance(), controller, command, args, cb);
            }
        });
    }

    @Override
    public void onPrepare() {
        onSessionCommand(SessionCommand2.COMMAND_CODE_PLAYBACK_PREPARE, new SessionRunnable() {
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                mSession.getInstance().prepare();
            }
        });
    }

    @Override
    public void onPrepareFromMediaId(final String mediaId, final Bundle extras) {
        if (mediaId == null) {
            return;
        }
        onSessionCommand(SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getCallback().onPrepareFromMediaId(mSession.getInstance(),
                                controller, mediaId, extras);
                    }
                });
    }

    @Override
    public void onPrepareFromSearch(final String query, final Bundle extras) {
        if (query == null) {
            return;
        }
        onSessionCommand(SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getCallback().onPrepareFromSearch(mSession.getInstance(),
                                controller, query, extras);
                    }
                });
    }

    @Override
    public void onPrepareFromUri(final Uri uri, final Bundle extras) {
        if (uri == null) {
            return;
        }
        onSessionCommand(SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_URI,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getCallback().onPrepareFromUri(mSession.getInstance(),
                                controller, uri, extras);
                    }
                });
    }

    @Override
    public void onPlay() {
        onSessionCommand(SessionCommand2.COMMAND_CODE_PLAYBACK_PLAY, new SessionRunnable() {
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                mSession.getInstance().play();
            }
        });
    }

    @Override
    public void onPlayFromMediaId(final String mediaId, final Bundle extras) {
        if (mediaId == null) {
            return;
        }
        onSessionCommand(SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getCallback().onPlayFromMediaId(mSession.getInstance(),
                                controller, mediaId, extras);
                    }
                });
    }

    @Override
    public void onPlayFromSearch(final String query, final Bundle extras) {
        if (query == null) {
            return;
        }
        onSessionCommand(SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_SEARCH,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getCallback().onPlayFromSearch(mSession.getInstance(),
                                controller, query, extras);
                    }
                });
    }

    @Override
    public void onPlayFromUri(final Uri uri, final Bundle extras) {
        if (uri == null) {
            return;
        }
        onSessionCommand(SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_URI,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getCallback().onPlayFromUri(mSession.getInstance(),
                                controller, uri, extras);
                    }
                });
    }

    @Override
    public void onPause() {
        onSessionCommand(SessionCommand2.COMMAND_CODE_PLAYBACK_PAUSE, new SessionRunnable() {
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                mSession.getInstance().pause();
            }
        });
    }

    @Override
    public void onStop() {
        // Here, we don't call MediaPlayerConnector#reset() since it may result removing
        // all callbacks from the player. Instead, we pause and seek to zero.
        // Here, we check both permissions: Pause / SeekTo.
        onSessionCommand(SessionCommand2.COMMAND_CODE_PLAYBACK_PAUSE, new SessionRunnable() {
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                handleCommandOnExecutor(controller, null,
                        SessionCommand2.COMMAND_CODE_PLAYBACK_SEEK_TO, new SessionRunnable() {
                            @Override
                            public void run(ControllerInfo controller) throws RemoteException {
                                mSession.getInstance().pause();
                                mSession.getInstance().seekTo(0);
                            }
                        });
            }
        });
    }

    @Override
    public void onSeekTo(final long pos) {
        onSessionCommand(SessionCommand2.COMMAND_CODE_PLAYBACK_SEEK_TO, new SessionRunnable() {
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                mSession.getInstance().seekTo(pos);
            }
        });
    }

    @Override
    public void onSkipToNext() {
        onSessionCommand(SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_TO_NEXT_ITEM,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getInstance().skipToNextItem();
                    }
                });
    }

    @Override
    public void onSkipToPrevious() {
        onSessionCommand(SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_TO_PREV_ITEM,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getInstance().skipToPreviousItem();
                    }
                });
    }

    @Override
    public void onSkipToQueueItem(final long id) {
        onSessionCommand(SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_TO_PLAYLIST_ITEM,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        List<MediaItem2> playlist = mSession.getPlaylistAgent().getPlaylist();
                        if (playlist == null) {
                            return;
                        }
                        for (int i = 0; i < playlist.size(); i++) {
                            MediaItem2 item = playlist.get(i);
                            if (item != null && item.getUuid().getMostSignificantBits() == id) {
                                mSession.getInstance().skipToPlaylistItem(item);
                                break;
                            }
                        }
                    }
                });
    }

    @Override
    public void onFastForward() {
        onSessionCommand(SessionCommand2.COMMAND_CODE_SESSION_FAST_FORWARD,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getCallback().onFastForward(
                                mSession.getInstance(), controller);
                    }
                });
    }

    @Override
    public void onRewind() {
        onSessionCommand(SessionCommand2.COMMAND_CODE_SESSION_REWIND,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getCallback().onRewind(mSession.getInstance(), controller);
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
        onSessionCommand(SessionCommand2.COMMAND_CODE_SESSION_SET_RATING,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        MediaItem2 currentItem = mSession.getCurrentMediaItem();
                        if (currentItem == null) {
                            return;
                        }
                        mSession.getCallback().onSetRating(mSession.getInstance(), controller,
                                currentItem.getMediaId(), MediaUtils2.convertToRating2(rating));
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
        onSessionCommand(SessionCommand2.COMMAND_CODE_PLAYLIST_SET_REPEAT_MODE,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getInstance().setRepeatMode(repeatMode);
                    }
                });
    }

    @Override
    public void onSetShuffleMode(final int shuffleMode) {
        onSessionCommand(SessionCommand2.COMMAND_CODE_PLAYLIST_SET_SHUFFLE_MODE,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getInstance().setShuffleMode(shuffleMode);
                    }
                });
    }

    @Override
    public void onAddQueueItem(final MediaDescriptionCompat description) {
        if (description == null) {
            return;
        }
        onSessionCommand(SessionCommand2.COMMAND_CODE_PLAYLIST_ADD_ITEM,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        // Add the item at the end of the playlist.
                        mSession.getInstance().addPlaylistItem(Integer.MAX_VALUE,
                                MediaUtils2.convertToMediaItem2(description));
                    }
                });
    }

    @Override
    public void onAddQueueItem(final MediaDescriptionCompat description, final int index) {
        if (description == null) {
            return;
        }
        onSessionCommand(SessionCommand2.COMMAND_CODE_PLAYLIST_ADD_ITEM,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSession.getInstance().addPlaylistItem(index,
                                MediaUtils2.convertToMediaItem2(description));
                    }
                });
    }

    @Override
    public void onRemoveQueueItem(final MediaDescriptionCompat description) {
        if (description == null) {
            return;
        }
        onSessionCommand(SessionCommand2.COMMAND_CODE_PLAYLIST_REMOVE_ITEM,
                new SessionRunnable() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        // Note: Here we cannot simply call
                        // removePlaylistItem(MediaUtils2.convertToMediaItem2(description)),
                        // because the result of the method will have different UUID.
                        List<MediaItem2> playlist = mSession.getInstance().getPlaylist();
                        for (int i = 0; i < playlist.size(); i++) {
                            MediaItem2 item = playlist.get(i);
                            if (TextUtils.equals(item.getMediaId(), description.getMediaId())) {
                                mSession.getInstance().removePlaylistItem(item);
                                return;
                            }
                        }
                    }
                });
    }

    ControllerInfo getControllersForAll() {
        return mControllerInfoForAll;
    }

    ConnectedControllersManager getConnectedControllersManager() {
        return mConnectedControllersManager;
    }

    private void onSessionCommand(final int commandCode, @NonNull final SessionRunnable runnable) {
        onSessionCommandInternal(null, commandCode, runnable);
    }

    private void onSessionCommand(@NonNull final SessionCommand2 sessionCommand,
            @NonNull final SessionRunnable runnable) {
        onSessionCommandInternal(sessionCommand, COMMAND_CODE_CUSTOM, runnable);
    }

    private void onSessionCommandInternal(@Nullable final SessionCommand2 sessionCommand,
            final int commandCode, @NonNull final SessionRunnable runnable) {
        if (mSession.isClosed()) {
            return;
        }
        RemoteUserInfo remoteUserInfo = mSession.getSessionCompat().getCurrentControllerInfo();
        final ControllerInfo controller;
        synchronized (mLock) {
            if (remoteUserInfo == null) {
                // TODO: Fix here to allow MediaControllerCompat to send commands on API 21~27.
                // In API 21~27, getCurrentControllerInfo() always returns null.
                // Due to this, on those API versions no MediaControllerCompat can send command
                // to the session.
                controller = null;
            } else {
                ControllerInfo ctrl = mConnectedControllersManager.getController(remoteUserInfo);
                if (ctrl != null) {
                    controller = ctrl;
                } else {
                    controller = new ControllerInfo(
                            remoteUserInfo,
                            mSessionManager.isTrustedForMediaControl(remoteUserInfo),
                            new ControllerLegacyCb(remoteUserInfo));
                    connect(controller);
                }
            }
        }
        mSession.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                synchronized (mLock) {
                    if (controller != null
                            && !mConnectedControllersManager.isConnected(controller)) {
                        return;
                    }
                }
                handleCommandOnExecutor(controller, sessionCommand, commandCode, runnable);
            }
        });
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void handleCommandOnExecutor(@Nullable final ControllerInfo controller,
            @Nullable final SessionCommand2 sessionCommand, final int commandCode,
            @NonNull final SessionRunnable runnable) {
        SessionCommand2 command;
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
            boolean accepted = mSession.getCallback().onCommandRequest(
                    mSession.getInstance(), controller, command);
            if (!accepted) {
                // Don't run rejected command.
                if (DEBUG) {
                    Log.d(TAG, "Command (" + command + ") from "
                            + controller + " was rejected by " + mSession);
                }
                return;
            }
        }
        try {
            runnable.run(controller);
        } catch (RemoteException e) {
            // Currently it's TransactionTooLargeException or DeadSystemException.
            // We'd better to leave log for those cases because
            //   - TransactionTooLargeException means that we may need to fix our code.
            //     (e.g. add pagination or special way to deliver Bitmap)
            //   - DeadSystemException means that errors around it can be ignored.
            Log.w(TAG, "Exception in " + controller.toString(), e);
        }
    }

    private void connect(final ControllerInfo controller) {
        mSession.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (mSession.isClosed()) {
                    return;
                }
                SessionCommandGroup2 allowedCommands = mSession.getCallback().onConnect(
                        mSession.getInstance(), controller);
                if (allowedCommands == null) {
                    try {
                        controller.getControllerCb().onDisconnected();
                    } catch (RemoteException ex) {
                        // Controller may have died prematurely.
                    }
                    return;
                }
                mConnectedControllersManager.addController(
                        controller.getRemoteUserInfo(), controller, allowedCommands);
            }
        });
    }

    @FunctionalInterface
    private interface SessionRunnable {
        void run(ControllerInfo controller) throws RemoteException;
    }

    @SuppressWarnings("ClassCanBeStatic")
    final class ControllerLegacyCb extends ControllerCb {
        private final RemoteUserInfo mRemoteUserInfo;

        ControllerLegacyCb(RemoteUserInfo remoteUserInfo) {
            mRemoteUserInfo = remoteUserInfo;
        }

        @Override
        void onCustomLayoutChanged(List<CommandButton> layout) throws RemoteException {
            // no-op.
        }

        @Override
        void onPlaybackInfoChanged(PlaybackInfo info) throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onAllowedCommandsChanged(SessionCommandGroup2 commands) throws RemoteException {
            // no-op
        }

        @Override
        void onCustomCommand(SessionCommand2 command, Bundle args, ResultReceiver receiver)
                throws RemoteException {
            // no-op
        }

        @Override
        void onPlayerStateChanged(long eventTimeMs, long positionMs, int playerState)
                throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onPlaybackSpeedChanged(long eventTimeMs, long positionMs, float speed)
                throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onBufferingStateChanged(MediaItem2 item, int bufferingState, long bufferedPositionMs)
                throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onSeekCompleted(long eventTimeMs, long positionMs, long position)
                throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onError(int errorCode, Bundle extras) throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onCurrentMediaItemChanged(MediaItem2 item) throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onPlaylistChanged(List<MediaItem2> playlist, MediaMetadata2 metadata)
                throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onPlaylistMetadataChanged(MediaMetadata2 metadata) throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onShuffleModeChanged(int shuffleMode) throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onRepeatModeChanged(int repeatMode) throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onRoutesInfoChanged(List<Bundle> routes) throws RemoteException {
            // no-op
        }

        @Override
        void onGetLibraryRootDone(Bundle rootHints, String rootMediaId, Bundle rootExtra)
                throws RemoteException {
            // no-op
        }

        @Override
        void onChildrenChanged(String parentId, int itemCount, Bundle extras)
                throws RemoteException {
            // no-op
        }

        @Override
        void onGetChildrenDone(String parentId, int page, int pageSize, List<MediaItem2> result,
                Bundle extras) throws RemoteException {
            // no-op
        }

        @Override
        void onGetItemDone(String mediaId, MediaItem2 result) throws RemoteException {
            // no-op
        }

        @Override
        void onSearchResultChanged(String query, int itemCount, Bundle extras)
                throws RemoteException {
            // no-op
        }

        @Override
        void onGetSearchResultDone(String query, int page, int pageSize, List<MediaItem2> result,
                Bundle extras) throws RemoteException {
            // no-op
        }

        @Override
        void onDisconnected() throws RemoteException {
            // no-op
        }
    }

    final class ControllerLegacyCbForAll extends ControllerCb {
        ControllerLegacyCbForAll() {
        }

        @Override
        void onCustomLayoutChanged(List<CommandButton> layout) throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onPlaybackInfoChanged(PlaybackInfo info) throws RemoteException {
            // no-op. Calling MediaSessionCompat#setPlaybackToLocal/Remote
            // is already done in updatePlayerConnector().
        }

        @Override
        void onAllowedCommandsChanged(SessionCommandGroup2 commands) throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onCustomCommand(SessionCommand2 command, Bundle args, ResultReceiver receiver)
                throws RemoteException {
            // no-op
        }

        @Override
        void onPlayerStateChanged(long eventTimeMs, long positionMs, int playerState)
                throws RemoteException {
            // Note: This method does not use any of the given arguments.
            mSession.getSessionCompat().setPlaybackState(mSession.createPlaybackStateCompat());
        }

        @Override
        void onPlaybackSpeedChanged(long eventTimeMs, long positionMs, float speed)
                throws RemoteException {
            // Note: This method does not use any of the given arguments.
            mSession.getSessionCompat().setPlaybackState(mSession.createPlaybackStateCompat());
        }

        @Override
        void onBufferingStateChanged(MediaItem2 item, int bufferingState, long bufferedPositionMs)
                throws RemoteException {
            // Note: This method does not use any of the given arguments.
            mSession.getSessionCompat().setPlaybackState(mSession.createPlaybackStateCompat());
        }

        @Override
        void onSeekCompleted(long eventTimeMs, long positionMs, long position)
                throws RemoteException {
            // Note: This method does not use any of the given arguments.
            mSession.getSessionCompat().setPlaybackState(mSession.createPlaybackStateCompat());
        }

        @Override
        void onError(int errorCode, Bundle extras) throws RemoteException {
            PlaybackStateCompat stateWithoutError = mSession.createPlaybackStateCompat();
            // We don't set the state here as PlaybackStateCompat#STATE_ERROR, since
            // MediaSession2#notifyError() does not affect the player state.
            // This prevents MediaControllerCompat from remaining long time in error state.
            PlaybackStateCompat stateWithError = new PlaybackStateCompat.Builder(stateWithoutError)
                    .setErrorMessage(errorCode, "")
                    .setExtras(extras)
                    .build();
            mSession.getSessionCompat().setPlaybackState(stateWithError);
        }

        @Override
        void onCurrentMediaItemChanged(MediaItem2 item) throws RemoteException {
            mSession.getSessionCompat().setMetadata(item == null ? null
                    : MediaUtils2.convertToMediaMetadataCompat(item.getMetadata()));
        }

        @Override
        void onPlaylistChanged(List<MediaItem2> playlist, MediaMetadata2 metadata)
                throws RemoteException {
            mSession.getSessionCompat().setQueue(MediaUtils2.convertToQueueItemList(playlist));
            onPlaylistMetadataChanged(metadata);
        }

        @Override
        void onPlaylistMetadataChanged(MediaMetadata2 metadata) throws RemoteException {
            // Since there is no 'queue metadata', only set title of the queue.
            CharSequence oldTitle = mSession.getSessionCompat().getController().getQueueTitle();
            CharSequence newTitle = null;

            if (metadata != null) {
                newTitle = metadata.getText(METADATA_KEY_DISPLAY_TITLE);
                if (newTitle == null) {
                    newTitle = metadata.getText(METADATA_KEY_TITLE);
                }
            }

            if (!TextUtils.equals(oldTitle, newTitle)) {
                mSession.getSessionCompat().setQueueTitle(newTitle);
            }
        }

        @Override
        void onShuffleModeChanged(int shuffleMode) throws RemoteException {
            mSession.getSessionCompat().setShuffleMode(shuffleMode);
        }

        @Override
        void onRepeatModeChanged(int repeatMode) throws RemoteException {
            mSession.getSessionCompat().setRepeatMode(repeatMode);
        }

        @Override
        void onRoutesInfoChanged(List<Bundle> routes) throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onGetLibraryRootDone(Bundle rootHints, String rootMediaId, Bundle rootExtra)
                throws RemoteException {
            // no-op
        }

        @Override
        void onChildrenChanged(String parentId, int itemCount, Bundle extras)
                throws RemoteException {
            // no-op
        }

        @Override
        void onGetChildrenDone(String parentId, int page, int pageSize, List<MediaItem2> result,
                Bundle extras) throws RemoteException {
            // no-op
        }

        @Override
        void onGetItemDone(String mediaId, MediaItem2 result) throws RemoteException {
            // no-op
        }

        @Override
        void onSearchResultChanged(String query, int itemCount, Bundle extras)
                throws RemoteException {
            // no-op
        }

        @Override
        void onGetSearchResultDone(String query, int page, int pageSize, List<MediaItem2> result,
                Bundle extras) throws RemoteException {
            // no-op
        }

        @Override
        void onDisconnected() throws RemoteException {
            // no-op. Calling MediaSessionCompat#release() is already done in close().
        }
    }
}
