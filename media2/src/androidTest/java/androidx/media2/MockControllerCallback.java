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

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import android.os.Bundle;

import androidx.annotation.CallSuper;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.media2.MediaController.ControllerCallback;
import androidx.media2.MediaSession.CommandButton;
import androidx.media2.MediaSessionTestBase.TestControllerCallbackInterface;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Mock {@link MediaController.ControllerCallback} that implements
 * {@link TestControllerCallbackInterface}
 */
public class MockControllerCallback extends MediaController.ControllerCallback
        implements TestControllerCallbackInterface {

    public final ControllerCallback mCallbackProxy;
    public final CountDownLatch connectLatch = new CountDownLatch(1);
    public final CountDownLatch disconnectLatch = new CountDownLatch(1);
    @GuardedBy("this")
    private Runnable mOnCustomCommandRunnable;

    MockControllerCallback(@NonNull ControllerCallback callbackProxy) {
        if (callbackProxy == null) {
            throw new IllegalArgumentException("Callback proxy shouldn't be null. Test bug");
        }
        mCallbackProxy = callbackProxy;
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
    public MediaController.ControllerResult onCustomCommand(MediaController controller,
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
    public void setRunnableForOnCustomCommand(Runnable runnable) {
        synchronized (this) {
            mOnCustomCommandRunnable = runnable;
        }
    }
}
