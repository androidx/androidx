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

package androidx.media2.test.client.tests;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import android.os.Bundle;

import androidx.annotation.CallSuper;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.SubtitleData;
import androidx.media2.common.VideoSize;
import androidx.media2.session.MediaBrowser;
import androidx.media2.session.MediaBrowser.BrowserCallback;
import androidx.media2.session.MediaController;
import androidx.media2.session.MediaController.ControllerCallback;
import androidx.media2.session.MediaLibraryService;
import androidx.media2.session.MediaSession.CommandButton;
import androidx.media2.session.SessionCommand;
import androidx.media2.session.SessionCommandGroup;
import androidx.media2.session.SessionResult;
import androidx.media2.test.client.tests.MediaSessionTestBase.TestControllerCallbackInterface;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A proxy class for {@link BrowserCallback} which implements
 * {@link TestControllerCallbackInterface}.
 */
public class TestBrowserCallback extends BrowserCallback
        implements TestControllerCallbackInterface {

    public final ControllerCallback mCallbackProxy;
    public final CountDownLatch connectLatch = new CountDownLatch(1);
    public final CountDownLatch disconnectLatch = new CountDownLatch(1);
    @GuardedBy("this")
    private Runnable mOnCustomCommandRunnable;

    TestBrowserCallback(@Nullable ControllerCallback callbackProxy) {
        mCallbackProxy = callbackProxy == null ? new BrowserCallback() {} : callbackProxy;
    }

    @CallSuper
    @Override
    public void onConnected(MediaController controller, SessionCommandGroup commands) {
        connectLatch.countDown();
        mCallbackProxy.onConnected(controller, commands);
    }

    @CallSuper
    @Override
    public void onDisconnected(MediaController controller) {
        disconnectLatch.countDown();
        mCallbackProxy.onDisconnected(controller);
    }

    @Override
    public void waitForConnect(boolean expect) throws InterruptedException {
        if (expect) {
            assertTrue(connectLatch.await(
                    MediaSessionTestBase.TIMEOUT_MS, TimeUnit.MILLISECONDS));
        } else {
            assertFalse(connectLatch.await(
                    MediaSessionTestBase.TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public void waitForDisconnect(boolean expect) throws InterruptedException {
        if (expect) {
            assertTrue(disconnectLatch.await(
                    MediaSessionTestBase.TIMEOUT_MS, TimeUnit.MILLISECONDS));
        } else {
            assertFalse(disconnectLatch.await(
                    MediaSessionTestBase.TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public SessionResult onCustomCommand(MediaController controller,
            SessionCommand command, Bundle args) {
        synchronized (this) {
            if (mOnCustomCommandRunnable != null) {
                mOnCustomCommandRunnable.run();
            }
        }
        return mCallbackProxy.onCustomCommand(controller, command, args);
    }

    @Override
    public void onPlaybackInfoChanged(MediaController controller,
            MediaController.PlaybackInfo info) {
        mCallbackProxy.onPlaybackInfoChanged(controller, info);
    }

    @Override
    public int onSetCustomLayout(MediaController controller, List<CommandButton> layout) {
        return mCallbackProxy.onSetCustomLayout(controller, layout);
    }

    @Override
    public void onAllowedCommandsChanged(MediaController controller,
            SessionCommandGroup commands) {
        mCallbackProxy.onAllowedCommandsChanged(controller, commands);
    }

    @Override
    public void onPlayerStateChanged(MediaController controller, int state) {
        mCallbackProxy.onPlayerStateChanged(controller, state);
    }

    @Override
    public void onSeekCompleted(MediaController controller, long position) {
        mCallbackProxy.onSeekCompleted(controller, position);
    }

    @Override
    public void onPlaybackSpeedChanged(MediaController controller, float speed) {
        mCallbackProxy.onPlaybackSpeedChanged(controller, speed);
    }

    @Override
    public void onBufferingStateChanged(MediaController controller, MediaItem item,
            int state) {
        mCallbackProxy.onBufferingStateChanged(controller, item, state);
    }

    @Override
    public void onCurrentMediaItemChanged(MediaController controller, MediaItem item) {
        mCallbackProxy.onCurrentMediaItemChanged(controller, item);
    }

    @Override
    public void onPlaylistChanged(MediaController controller,
            List<MediaItem> list, MediaMetadata metadata) {
        mCallbackProxy.onPlaylistChanged(controller, list, metadata);
    }

    @Override
    public void onPlaylistMetadataChanged(MediaController controller,
            MediaMetadata metadata) {
        mCallbackProxy.onPlaylistMetadataChanged(controller, metadata);
    }

    @Override
    public void onShuffleModeChanged(MediaController controller, int shuffleMode) {
        mCallbackProxy.onShuffleModeChanged(controller, shuffleMode);
    }

    @Override
    public void onRepeatModeChanged(MediaController controller, int repeatMode) {
        mCallbackProxy.onRepeatModeChanged(controller, repeatMode);
    }

    @Override
    public void onPlaybackCompleted(MediaController controller) {
        mCallbackProxy.onPlaybackCompleted(controller);
    }

    @Override
    public void onVideoSizeChanged(@NonNull MediaController controller, @NonNull MediaItem item,
            @NonNull VideoSize videoSize) {
        mCallbackProxy.onVideoSizeChanged(controller, item, videoSize);
    }

    @Override
    public void onTrackInfoChanged(MediaController controller,
            List<SessionPlayer.TrackInfo> trackInfos) {
        mCallbackProxy.onTrackInfoChanged(controller, trackInfos);
    }

    @Override
    public void onTrackSelected(MediaController controller, SessionPlayer.TrackInfo trackInfo) {
        mCallbackProxy.onTrackSelected(controller, trackInfo);
    }

    @Override
    public void onTrackDeselected(MediaController controller, SessionPlayer.TrackInfo trackInfo) {
        mCallbackProxy.onTrackDeselected(controller, trackInfo);
    }

    @Override
    public void onSubtitleData(@NonNull MediaController controller, @NonNull MediaItem item,
            @NonNull SessionPlayer.TrackInfo track, @NonNull SubtitleData data) {
        mCallbackProxy.onSubtitleData(controller, item, track, data);
    }

    @Override
    public void onChildrenChanged(@NonNull MediaBrowser browser, @NonNull String parentId,
            int itemCount, @Nullable MediaLibraryService.LibraryParams params) {
        ((BrowserCallback) mCallbackProxy).onChildrenChanged(
                browser, parentId, itemCount, params);
    }

    @Override
    public void onSearchResultChanged(@NonNull MediaBrowser browser, @NonNull String query,
            int itemCount, @Nullable MediaLibraryService.LibraryParams params) {
        ((BrowserCallback) mCallbackProxy).onSearchResultChanged(
                browser, query, itemCount, params);
    }

    @Override
    public void setRunnableForOnCustomCommand(Runnable runnable) {
        synchronized (this) {
            mOnCustomCommandRunnable = runnable;
        }
    }
}
