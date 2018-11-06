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
import static androidx.media2.MediaSession2.SessionResult.RESULT_CODE_SUCCESS;
import static androidx.media2.MediaUtils2.TRANSACTION_SIZE_LIMIT_IN_BYTES;
import static androidx.media2.SessionCommand2.COMMAND_CODE_CUSTOM;
import static androidx.media2.SessionCommand2.COMMAND_VERSION_CURRENT;

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
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaSessionManager;
import androidx.media.MediaSessionManager.RemoteUserInfo;
import androidx.media2.MediaController2.PlaybackInfo;
import androidx.media2.MediaLibraryService2.LibraryParams;
import androidx.media2.MediaLibraryService2.LibraryResult;
import androidx.media2.MediaSession2.CommandButton;
import androidx.media2.MediaSession2.ControllerCb;
import androidx.media2.MediaSession2.ControllerInfo;
import androidx.media2.MediaSession2.MediaSession2Impl;
import androidx.media2.MediaSession2.SessionResult;
import androidx.media2.SessionCommand2.CommandCode;
import androidx.media2.SessionPlayer2.PlayerResult;

import java.util.List;
import java.util.Set;

// Getting the commands from MediaControllerCompat'
class MediaSessionLegacyStub extends MediaSessionCompat.Callback {

    private static final String TAG = "MediaSessionLegacyStub";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final SparseArray<SessionCommand2> sCommandsForOnCommandRequest =
            new SparseArray<>();

    static {
        SessionCommandGroup2 group = new SessionCommandGroup2.Builder()
                .addAllPlayerCommands(COMMAND_VERSION_CURRENT)
                .addAllVolumeCommands(COMMAND_VERSION_CURRENT)
                .build();
        Set<SessionCommand2> commands = group.getCommands();
        for (SessionCommand2 command : commands) {
            sCommandsForOnCommandRequest.append(command.getCommandCode(), command);
        }
    }

    final ConnectedControllersManager<RemoteUserInfo> mConnectedControllersManager;

    final Object mLock = new Object();

    final MediaSession2Impl mSessionImpl;
    final MediaSessionManager mSessionManager;
    final Context mContext;
    final ControllerInfo mControllerInfoForAll;

    MediaSessionLegacyStub(MediaSession2Impl session) {
        mSessionImpl = session;
        mContext = mSessionImpl.getContext();
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
        dispatchSessionTask(SessionCommand2.COMMAND_CODE_PLAYER_PREPARE, new SessionTask() {
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                mSessionImpl.prepare();
            }
        });
    }

    @Override
    public void onPrepareFromMediaId(final String mediaId, final Bundle extras) {
        dispatchSessionTask(SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID,
                new SessionTask() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        if (TextUtils.isEmpty(mediaId)) {
                            Log.w(TAG, "onPrepareFromMediaId(): Ignoring empty mediaId from "
                                    + controller);
                            return;
                        }
                        mSessionImpl.getCallback().onPrepareFromMediaId(mSessionImpl.getInstance(),
                                controller, mediaId, extras);
                    }
                });
    }

    @Override
    public void onPrepareFromSearch(final String query, final Bundle extras) {
        dispatchSessionTask(SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH,
                new SessionTask() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        if (TextUtils.isEmpty(query)) {
                            Log.w(TAG, "onPrepareFromSearch(): Ignoring empty query from "
                                    + controller);
                            return;
                        }
                        mSessionImpl.getCallback().onPrepareFromSearch(mSessionImpl.getInstance(),
                                controller, query, extras);
                    }
                });
    }

    @Override
    public void onPrepareFromUri(final Uri uri, final Bundle extras) {
        if (uri == null) {
            return;
        }
        dispatchSessionTask(SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_URI,
                new SessionTask() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.getCallback().onPrepareFromUri(mSessionImpl.getInstance(),
                                controller, uri, extras);
                    }
                });
    }

    @Override
    public void onPlay() {
        dispatchSessionTask(SessionCommand2.COMMAND_CODE_PLAYER_PLAY, new SessionTask() {
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                mSessionImpl.play();
            }
        });
    }

    @Override
    public void onPlayFromMediaId(final String mediaId, final Bundle extras) {
        dispatchSessionTask(SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID,
                new SessionTask() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        if (TextUtils.isEmpty(mediaId)) {
                            Log.w(TAG, "onPlayFromMediaId(): Ignoring empty mediaId from "
                                    + controller);
                            return;
                        }
                        mSessionImpl.getCallback().onPlayFromMediaId(mSessionImpl.getInstance(),
                                controller, mediaId, extras);
                    }
                });
    }

    @Override
    public void onPlayFromSearch(final String query, final Bundle extras) {
        dispatchSessionTask(SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_SEARCH,
                new SessionTask() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        if (TextUtils.isEmpty(query)) {
                            Log.w(TAG, "onPlayFromSearch(): Ignoring empty query from "
                                    + controller);
                            return;
                        }
                        mSessionImpl.getCallback().onPlayFromSearch(mSessionImpl.getInstance(),
                                controller, query, extras);
                    }
                });
    }

    @Override
    public void onPlayFromUri(final Uri uri, final Bundle extras) {
        if (uri == null) {
            return;
        }
        dispatchSessionTask(SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_URI,
                new SessionTask() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.getCallback().onPlayFromUri(mSessionImpl.getInstance(),
                                controller, uri, extras);
                    }
                });
    }

    @Override
    public void onPause() {
        dispatchSessionTask(SessionCommand2.COMMAND_CODE_PLAYER_PAUSE, new SessionTask() {
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                mSessionImpl.pause();
            }
        });
    }

    @Override
    public void onStop() {
        // Here, we don't call SessionPlayer2#reset() since it may result removing
        // all callbacks from the player. Instead, we pause and seek to zero.
        // Here, we check both permissions: Pause / SeekTo.
        dispatchSessionTask(SessionCommand2.COMMAND_CODE_PLAYER_PAUSE, new SessionTask() {
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                handleTaskOnExecutor(controller, null,
                        SessionCommand2.COMMAND_CODE_PLAYER_SEEK_TO, new SessionTask() {
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
        dispatchSessionTask(SessionCommand2.COMMAND_CODE_PLAYER_SEEK_TO, new SessionTask() {
            @Override
            public void run(ControllerInfo controller) throws RemoteException {
                mSessionImpl.seekTo(pos);
            }
        });
    }

    @Override
    public void onSkipToNext() {
        dispatchSessionTask(SessionCommand2.COMMAND_CODE_PLAYER_SKIP_TO_NEXT_PLAYLIST_ITEM,
                new SessionTask() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.skipToNextItem();
                    }
                });
    }

    @Override
    public void onSkipToPrevious() {
        dispatchSessionTask(SessionCommand2.COMMAND_CODE_PLAYER_SKIP_TO_PREVIOUS_PLAYLIST_ITEM,
                new SessionTask() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.skipToPreviousItem();
                    }
                });
    }

    @Override
    public void onSkipToQueueItem(final long queueId) {
        dispatchSessionTask(SessionCommand2.COMMAND_CODE_PLAYER_SKIP_TO_PLAYLIST_ITEM,
                new SessionTask() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        List<MediaItem2> playlist = mSessionImpl.getPlayer().getPlaylist();
                        if (playlist == null) {
                            return;
                        }
                        // Use queueId as an index as we've published {@link QueueItem} as so.
                        // see: {@link MediaUtils2#convertToQueueItemList}.
                        mSessionImpl.skipToPlaylistItem((int) queueId);
                    }
                });
    }

    @Override
    public void onFastForward() {
        dispatchSessionTask(SessionCommand2.COMMAND_CODE_SESSION_FAST_FORWARD,
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
        dispatchSessionTask(SessionCommand2.COMMAND_CODE_SESSION_REWIND,
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
        dispatchSessionTask(SessionCommand2.COMMAND_CODE_SESSION_SET_RATING,
                new SessionTask() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        MediaItem2 currentItem = mSessionImpl.getCurrentMediaItem();
                        if (currentItem == null) {
                            return;
                        }
                        mSessionImpl.getCallback().onSetRating(mSessionImpl.getInstance(),
                                controller, currentItem.getMediaId(),
                                MediaUtils2.convertToRating2(rating));
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
        dispatchSessionTask(SessionCommand2.COMMAND_CODE_PLAYER_SET_REPEAT_MODE,
                new SessionTask() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        mSessionImpl.setRepeatMode(repeatMode);
                    }
                });
    }

    @Override
    public void onSetShuffleMode(final int shuffleMode) {
        dispatchSessionTask(SessionCommand2.COMMAND_CODE_PLAYER_SET_SHUFFLE_MODE,
                new SessionTask() {
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
        dispatchSessionTask(SessionCommand2.COMMAND_CODE_PLAYER_ADD_PLAYLIST_ITEM,
                new SessionTask() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        String mediaId = description.getMediaId();
                        if (TextUtils.isEmpty(mediaId)) {
                            Log.w(TAG, "onAddQueueItem(): Media ID shouldn't be empty");
                            return;
                        }
                        MediaItem2 newItem = mSessionImpl.getCallback().onCreateMediaItem(
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
        dispatchSessionTask(SessionCommand2.COMMAND_CODE_PLAYER_REMOVE_PLAYLIST_ITEM,
                new SessionTask() {
                    @Override
                    public void run(ControllerInfo controller) throws RemoteException {
                        String mediaId = description.getMediaId();
                        if (TextUtils.isEmpty(mediaId)) {
                            Log.w(TAG, "onRemoveQueueItem(): Media ID shouldn't be null");
                            return;
                        }
                        List<MediaItem2> playlist = mSessionImpl.getPlaylist();
                        for (int i = 0; i < playlist.size(); i++) {
                            MediaItem2 item = playlist.get(i);
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
        dispatchSessionTask(SessionCommand2.COMMAND_CODE_PLAYER_REMOVE_PLAYLIST_ITEM,
                new SessionTask() {
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

    ControllerInfo getControllersForAll() {
        return mControllerInfoForAll;
    }

    ConnectedControllersManager getConnectedControllersManager() {
        return mConnectedControllersManager;
    }

    private void dispatchSessionTask(@CommandCode final int commandCode,
            @NonNull final SessionTask task) {
        dispatchSessionTaskInternal(null, commandCode, task);
    }

    private void dispatchSessionTask(@NonNull final SessionCommand2 sessionCommand,
            @NonNull final SessionTask task) {
        dispatchSessionTaskInternal(sessionCommand, COMMAND_CODE_CUSTOM, task);
    }

    private void dispatchSessionTaskInternal(@Nullable final SessionCommand2 sessionCommand,
            @CommandCode final int commandCode, @NonNull final SessionTask task) {
        if (mSessionImpl.isClosed()) {
            return;
        }
        RemoteUserInfo remoteUserInfo = mSessionImpl.getSessionCompat().getCurrentControllerInfo();
        final ControllerInfo controller;
        synchronized (mLock) {
            if (remoteUserInfo == null) {
                Log.d(TAG, "RemoteUserInfo is null, ignoring command=" + sessionCommand
                        + ", commandCode=" + commandCode);
                return;
            } else {
                ControllerInfo ctrl = mConnectedControllersManager.getController(remoteUserInfo);
                if (ctrl != null) {
                    controller = ctrl;
                } else {
                    controller = new ControllerInfo(
                            remoteUserInfo,
                            mSessionManager.isTrustedForMediaControl(remoteUserInfo),
                            new ControllerLegacyCb(remoteUserInfo));
                }
            }
        }
        mSessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (mSessionImpl.isClosed()) {
                    return;
                }
                if (!mConnectedControllersManager.isConnected(controller)) {
                    SessionCommandGroup2 allowedCommands = mSessionImpl.getCallback().onConnect(
                            mSessionImpl.getInstance(), controller);
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
                handleTaskOnExecutor(controller, sessionCommand, commandCode, task);
            }
        });
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void handleTaskOnExecutor(@NonNull final ControllerInfo controller,
            @Nullable final SessionCommand2 sessionCommand, @CommandCode final int commandCode,
            @NonNull final SessionTask task) {
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
            int resultCode = mSessionImpl.getCallback().onCommandRequest(
                    mSessionImpl.getInstance(), controller, command);
            if (resultCode != RESULT_CODE_SUCCESS) {
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

    @FunctionalInterface
    private interface SessionTask {
        void run(ControllerInfo controller) throws RemoteException;
    }

    @SuppressWarnings("ClassCanBeStatic")
    final class ControllerLegacyCb extends ControllerCb {
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
        void setCustomLayout(int seq, List<CommandButton> layout) throws RemoteException {
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
        void sendCustomCommand(int seq, SessionCommand2 command, Bundle args)
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
        void onPlaybackCompleted() throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onRoutesInfoChanged(List<Bundle> routes) throws RemoteException {
            // no-op
        }

        @Override
        void onChildrenChanged(String parentId, int itemCount, LibraryParams params)
                throws RemoteException {
            // no-op
        }
        @Override
        void onSearchResultChanged(String query, int itemCount, LibraryParams params)
                throws RemoteException {
            // no-op
        }

        @Override
        void onDisconnected() throws RemoteException {
            // no-op
        }
    }

    // TODO: Find a way to notify error through PlaybackStateCompat
    final class ControllerLegacyCbForAll extends ControllerCb {
        ControllerLegacyCbForAll() {
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
        void setCustomLayout(int seq, List<CommandButton> layout) throws RemoteException {
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
        void sendCustomCommand(int seq, SessionCommand2 command, Bundle args)
                throws RemoteException {
            // no-op
        }

        @Override
        void onPlayerStateChanged(long eventTimeMs, long positionMs, int playerState)
                throws RemoteException {
            // Note: This method does not use any of the given arguments.
            mSessionImpl.getSessionCompat().setPlaybackState(
                    mSessionImpl.createPlaybackStateCompat());
        }

        @Override
        void onPlaybackSpeedChanged(long eventTimeMs, long positionMs, float speed)
                throws RemoteException {
            // Note: This method does not use any of the given arguments.
            mSessionImpl.getSessionCompat().setPlaybackState(
                    mSessionImpl.createPlaybackStateCompat());
        }

        @Override
        void onBufferingStateChanged(MediaItem2 item, int bufferingState, long bufferedPositionMs)
                throws RemoteException {
            // Note: This method does not use any of the given arguments.
            mSessionImpl.getSessionCompat().setPlaybackState(
                    mSessionImpl.createPlaybackStateCompat());
        }

        @Override
        void onSeekCompleted(long eventTimeMs, long positionMs, long position)
                throws RemoteException {
            // Note: This method does not use any of the given arguments.
            mSessionImpl.getSessionCompat().setPlaybackState(
                    mSessionImpl.createPlaybackStateCompat());
        }

        @Override
        void onCurrentMediaItemChanged(MediaItem2 item) throws RemoteException {
            mSessionImpl.getSessionCompat().setMetadata(item == null ? null
                    : MediaUtils2.convertToMediaMetadataCompat(item.getMetadata()));
        }

        @Override
        void onPlaylistChanged(List<MediaItem2> playlist, MediaMetadata2 metadata)
                throws RemoteException {
            if (Build.VERSION.SDK_INT < 21) {
                // In order to avoid TransactionTooLargeException for below API 21,
                // we need to cut the list so that it doesn't exceed the binder transaction limit.
                List<QueueItem> queueItemList = MediaUtils2.convertToQueueItemList(playlist);
                List<QueueItem> truncatedList = MediaUtils2.truncateListBySize(
                        queueItemList, TRANSACTION_SIZE_LIMIT_IN_BYTES);
                if (truncatedList.size() != playlist.size()) {
                    Log.i(TAG, "Sending " + truncatedList.size() + " items out of "
                            + playlist.size());
                }
                mSessionImpl.getSessionCompat().setQueue(truncatedList);
            } else {
                // Framework MediaSession#setQueue() uses ParceledListSlice,
                // which means we can safely send long lists.
                mSessionImpl.getSessionCompat().setQueue(
                        MediaUtils2.convertToQueueItemList(playlist));
            }
            onPlaylistMetadataChanged(metadata);
        }

        @Override
        void onPlaylistMetadataChanged(MediaMetadata2 metadata) throws RemoteException {
            // Since there is no 'queue metadata', only set title of the queue.
            CharSequence oldTitle = mSessionImpl.getSessionCompat().getController().getQueueTitle();
            CharSequence newTitle = null;

            if (metadata != null) {
                newTitle = metadata.getText(METADATA_KEY_DISPLAY_TITLE);
                if (newTitle == null) {
                    newTitle = metadata.getText(METADATA_KEY_TITLE);
                }
            }

            if (!TextUtils.equals(oldTitle, newTitle)) {
                mSessionImpl.getSessionCompat().setQueueTitle(newTitle);
            }
        }

        @Override
        void onShuffleModeChanged(int shuffleMode) throws RemoteException {
            mSessionImpl.getSessionCompat().setShuffleMode(shuffleMode);
        }

        @Override
        void onRepeatModeChanged(int repeatMode) throws RemoteException {
            mSessionImpl.getSessionCompat().setRepeatMode(repeatMode);
        }

        @Override
        void onPlaybackCompleted() throws RemoteException {
            PlaybackStateCompat state = mSessionImpl.createPlaybackStateCompat();
            if (state.getState() != PlaybackStateCompat.STATE_PAUSED) {
                state = new PlaybackStateCompat.Builder(state)
                        .setState(PlaybackStateCompat.STATE_PAUSED, state.getPosition(),
                                state.getPlaybackSpeed())
                        .build();
            }
            mSessionImpl.getSessionCompat().setPlaybackState(state);
        }

        @Override
        void onRoutesInfoChanged(List<Bundle> routes) throws RemoteException {
            throw new AssertionError("This shouldn't be called.");
        }

        @Override
        void onChildrenChanged(String parentId, int itemCount, LibraryParams params)
                throws RemoteException {
            // no-op
        }
        @Override
        void onSearchResultChanged(String query, int itemCount, LibraryParams params)
                throws RemoteException {
            // no-op
        }

        @Override
        void onDisconnected() throws RemoteException {
            // no-op. Calling MediaSessionCompat#release() is already done in close().
        }
    }
}
