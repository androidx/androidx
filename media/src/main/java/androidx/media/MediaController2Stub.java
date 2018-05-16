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

import android.app.PendingIntent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import androidx.media.BaseMediaPlayer.BuffState;
import androidx.media.MediaController2.PlaybackInfo;
import androidx.media.MediaSession2.CommandButton;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

class MediaController2Stub extends IMediaController2.Stub {
    private static final String TAG = "MediaController2Stub";
    private static final boolean DEBUG = true; // TODO(jaewan): Change

    private final WeakReference<MediaController2ImplBase> mController;

    MediaController2Stub(MediaController2ImplBase controller) {
        mController = new WeakReference<>(controller);
    }

    @Override
    public void onCurrentMediaItemChanged(Bundle item) {
        final MediaController2ImplBase controller;
        try {
            controller = getController();
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
            return;
        }
        controller.notifyCurrentMediaItemChanged(MediaItem2.fromBundle(item));
    }

    @Override
    public void onPlayerStateChanged(long eventTimeMs, long positionMs, int state) {
        final MediaController2ImplBase controller;
        try {
            controller = getController();
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
            return;
        }
        controller.notifyPlayerStateChanges(eventTimeMs, positionMs, state);
    }

    @Override
    public void onPlaybackSpeedChanged(long eventTimeMs, long positionMs, float speed) {
        final MediaController2ImplBase controller;
        try {
            controller = getController();
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
            return;
        }
        controller.notifyPlaybackSpeedChanges(eventTimeMs, positionMs, speed);
    }

    @Override
    public void onBufferingStateChanged(Bundle itemBundle, @BuffState int state,
            long bufferedPositionMs) {
        final MediaController2ImplBase controller;
        try {
            controller = getController();
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
            return;
        }
        controller.notifyBufferingStateChanged(MediaItem2.fromBundle(itemBundle), state,
                bufferedPositionMs);
    }

    @Override
    public void onPlaylistChanged(List<Bundle> playlistBundle, Bundle metadataBundle) {
        final MediaController2ImplBase controller;
        try {
            controller = getController();
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
            return;
        }
        if (playlistBundle == null) {
            Log.w(TAG, "onPlaylistChanged(): Ignoring null playlist from " + controller);
            return;
        }
        List<MediaItem2> playlist = new ArrayList<>();
        for (Bundle bundle : playlistBundle) {
            MediaItem2 item = MediaItem2.fromBundle(bundle);
            if (item == null) {
                Log.w(TAG, "onPlaylistChanged(): Ignoring null item in playlist");
            } else {
                playlist.add(item);
            }
        }
        MediaMetadata2 metadata = MediaMetadata2.fromBundle(metadataBundle);
        controller.notifyPlaylistChanges(playlist, metadata);
    }

    @Override
    public void onPlaylistMetadataChanged(Bundle metadataBundle) throws RuntimeException {
        final MediaController2ImplBase controller;
        try {
            controller = getController();
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
            return;
        }
        MediaMetadata2 metadata = MediaMetadata2.fromBundle(metadataBundle);
        controller.notifyPlaylistMetadataChanges(metadata);
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
        final MediaController2ImplBase controller;
        try {
            controller = getController();
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
            return;
        }
        controller.notifyRepeatModeChanges(repeatMode);
    }

    @Override
    public void onShuffleModeChanged(int shuffleMode) {
        final MediaController2ImplBase controller;
        try {
            controller = getController();
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
            return;
        }
        controller.notifyShuffleModeChanges(shuffleMode);
    }

    @Override
    public void onPlaybackInfoChanged(Bundle playbackInfo) throws RuntimeException {
        if (DEBUG) {
            Log.d(TAG, "onPlaybackInfoChanged");
        }
        final MediaController2ImplBase controller;
        try {
            controller = getController();
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
            return;
        }
        PlaybackInfo info = PlaybackInfo.fromBundle(playbackInfo);
        if (info == null) {
            Log.w(TAG, "onPlaybackInfoChanged(): Ignoring null playbackInfo");
            return;
        }
        controller.notifyPlaybackInfoChanges(info);
    }

    @Override
    public void onSeekCompleted(long eventTimeMs, long positionMs, long seekPositionMs) {
        final MediaController2ImplBase controller;
        try {
            controller = getController();
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
            return;
        }
        controller.notifySeekCompleted(eventTimeMs, positionMs, seekPositionMs);
    }

    @Override
    public void onError(int errorCode, Bundle extras) {
        final MediaController2ImplBase controller;
        try {
            controller = getController();
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
            return;
        }
        controller.notifyError(errorCode, extras);
    }

    @Override
    public void onRoutesInfoChanged(final List<Bundle> routes) {
        final MediaController2ImplBase controller;
        try {
            controller = getController();
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
            return;
        }
        controller.notifyRoutesInfoChanged(routes);
    }

    @Override
    public void onConnected(IMediaSession2 sessionBinder, Bundle commandGroup, int playerState,
            Bundle currentItem, long positionEventTimeMs, long positionMs, float playbackSpeed,
            long bufferedPositionMs, Bundle playbackInfo, int shuffleMode, int repeatMode,
            List<Bundle> itemBundleList, PendingIntent sessionActivity) {
        final MediaController2ImplBase controller = mController.get();
        if (controller == null) {
            if (DEBUG) {
                Log.d(TAG, "onConnected after MediaController2.close()");
            }
            return;
        }
        List<MediaItem2> itemList = null;
        if (itemBundleList != null) {
            itemList = new ArrayList<>();
            for (int i = 0; i < itemBundleList.size(); i++) {
                MediaItem2 item = MediaItem2.fromBundle(itemBundleList.get(i));
                if (item != null) {
                    itemList.add(item);
                }
            }
        }
        controller.onConnectedNotLocked(sessionBinder,
                SessionCommandGroup2.fromBundle(commandGroup),
                playerState, MediaItem2.fromBundle(currentItem), positionEventTimeMs, positionMs,
                playbackSpeed, bufferedPositionMs, PlaybackInfo.fromBundle(playbackInfo),
                repeatMode, shuffleMode, itemList, sessionActivity);
    }

    @Override
    public void onDisconnected() {
        final MediaController2ImplBase controller = mController.get();
        if (controller == null) {
            if (DEBUG) {
                Log.d(TAG, "onDisconnected after MediaController2.close()");
            }
            return;
        }
        controller.getInstance().close();
    }

    @Override
    public void onCustomLayoutChanged(List<Bundle> commandButtonlist) {
        if (commandButtonlist == null) {
            Log.w(TAG, "onCustomLayoutChanged(): Ignoring null commandButtonlist");
            return;
        }
        final MediaController2ImplBase controller;
        try {
            controller = getController();
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
            return;
        }
        if (controller == null) {
            // TODO(jaewan): Revisit here. Could be a bug
            return;
        }
        List<CommandButton> layout = new ArrayList<>();
        for (int i = 0; i < commandButtonlist.size(); i++) {
            CommandButton button = CommandButton.fromBundle(commandButtonlist.get(i));
            if (button != null) {
                layout.add(button);
            }
        }
        controller.onCustomLayoutChanged(layout);
    }

    @Override
    public void onAllowedCommandsChanged(Bundle commandsBundle) {
        final MediaController2ImplBase controller;
        try {
            controller = getController();
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
            return;
        }
        if (controller == null) {
            // TODO(jaewan): Revisit here. Could be a bug
            return;
        }
        SessionCommandGroup2 commands = SessionCommandGroup2.fromBundle(commandsBundle);
        if (commands == null) {
            Log.w(TAG, "onAllowedCommandsChanged(): Ignoring null commands");
            return;
        }
        controller.onAllowedCommandsChanged(commands);
    }

    @Override
    public void onCustomCommand(Bundle commandBundle, Bundle args, ResultReceiver receiver) {
        final MediaController2ImplBase controller;
        try {
            controller = getController();
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
            return;
        }
        SessionCommand2 command = SessionCommand2.fromBundle(commandBundle);
        if (command == null) {
            Log.w(TAG, "onCustomCommand(): Ignoring null command");
            return;
        }
        controller.onCustomCommand(command, args, receiver);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // MediaBrowser specific
    ////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onGetLibraryRootDone(final Bundle rootHints, final String rootMediaId,
            final Bundle rootExtra) throws RuntimeException {
        final MediaBrowser2 browser;
        try {
            browser = getBrowser();
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
            return;
        }
        if (browser == null) {
            return;
        }
        browser.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                browser.getCallback().onGetLibraryRootDone(
                        browser, rootHints, rootMediaId, rootExtra);
            }
        });
    }

    @Override
    public void onGetItemDone(final String mediaId, final Bundle itemBundle)
            throws RuntimeException {
        if (mediaId == null) {
            Log.w(TAG, "onGetItemDone(): Ignoring null mediaId");
            return;
        }
        final MediaBrowser2 browser;
        try {
            browser = getBrowser();
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
            return;
        }
        if (browser == null) {
            return;
        }
        browser.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                browser.getCallback().onGetItemDone(
                        browser, mediaId, MediaItem2.fromBundle(itemBundle));
            }
        });
    }

    @Override
    public void onGetChildrenDone(final String parentId, final int page, final int pageSize,
            final List<Bundle> itemBundleList, final Bundle extras) throws RuntimeException {
        if (parentId == null) {
            Log.w(TAG, "onGetChildrenDone(): Ignoring null parentId");
            return;
        }
        final MediaBrowser2 browser;
        try {
            browser = getBrowser();
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
            return;
        }
        if (browser == null) {
            return;
        }

        browser.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                browser.getCallback().onGetChildrenDone(browser, parentId, page, pageSize,
                        MediaUtils2.fromMediaItem2BundleList(itemBundleList), extras);
            }
        });
    }

    @Override
    public void onSearchResultChanged(final String query, final int itemCount, final Bundle extras)
            throws RuntimeException {
        if (TextUtils.isEmpty(query)) {
            Log.w(TAG, "onSearchResultChanged(): Ignoring empty query");
            return;
        }
        final MediaBrowser2 browser;
        try {
            browser = getBrowser();
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
            return;
        }
        if (browser == null) {
            return;
        }
        browser.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                browser.getCallback().onSearchResultChanged(browser, query, itemCount, extras);
            }
        });
    }

    @Override
    public void onGetSearchResultDone(final String query, final int page, final int pageSize,
            final List<Bundle> itemBundleList, final Bundle extras) throws RuntimeException {
        if (TextUtils.isEmpty(query)) {
            Log.w(TAG, "onGetSearchResultDone(): Ignoring empty query");
            return;
        }
        final MediaBrowser2 browser;
        try {
            browser = getBrowser();
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
            return;
        }
        if (browser == null) {
            // TODO(jaewan): Revisit here. Could be a bug
            return;
        }

        browser.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                browser.getCallback().onGetSearchResultDone(browser, query, page, pageSize,
                        MediaUtils2.fromMediaItem2BundleList(itemBundleList), extras);
            }
        });
    }

    @Override
    public void onChildrenChanged(final String parentId, final int itemCount, final Bundle extras) {
        if (parentId == null) {
            Log.w(TAG, "onChildrenChanged(): Ignoring null parentId");
            return;
        }
        final MediaBrowser2 browser;
        try {
            browser = getBrowser();
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
            return;
        }
        if (browser == null) {
            return;
        }
        browser.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                browser.getCallback().onChildrenChanged(browser, parentId, itemCount, extras);
            }
        });
    }

    public void destroy() {
        mController.clear();
    }

    private MediaController2ImplBase getController() throws IllegalStateException {
        final MediaController2ImplBase controller = mController.get();
        if (controller == null) {
            throw new IllegalStateException("Controller is released");
        }
        return controller;
    }

    private MediaBrowser2 getBrowser() throws IllegalStateException {
        final MediaController2ImplBase controller = getController();
        if (controller.getInstance() instanceof MediaBrowser2) {
            return (MediaBrowser2) controller.getInstance();
        }
        return null;
    }
}
