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

package androidx.media2.widget;

import static android.content.Context.KEYGUARD_SERVICE;

import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.media2.common.MediaItem;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.UriMediaItem;
import androidx.media2.player.MediaPlayer;
import androidx.media2.session.MediaController;
import androidx.media2.session.MediaSession;
import androidx.media2.widget.test.R;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class MediaWidgetTestBase extends MediaTestBase {
    // Expected success time
    static final int WAIT_TIME_MS = 1000;

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private List<SessionPlayer> mPlayers = new ArrayList<>();
    @GuardedBy("mLock")
    private List<MediaSession> mSessions = new ArrayList<>();
    @GuardedBy("mLock")
    private List<MediaController> mControllers = new ArrayList<>();

    Context mContext;
    Executor mMainHandlerExecutor;

    @Before
    public void setupWidgetTest() {
        mContext = ApplicationProvider.getApplicationContext();
        mMainHandlerExecutor = ContextCompat.getMainExecutor(mContext);
    }

    static <T extends Activity> void setKeepScreenOn(ActivityTestRule<T> activityRule)
            throws Throwable {
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        final Activity activity = activityRule.getActivity();
        activityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= 27) {
                    activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    activity.setTurnScreenOn(true);
                    activity.setShowWhenLocked(true);
                    KeyguardManager keyguardManager = (KeyguardManager)
                            instrumentation.getTargetContext().getSystemService(KEYGUARD_SERVICE);
                    keyguardManager.requestDismissKeyguard(activity, null);
                } else {
                    activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                }
            }
        });
        instrumentation.waitForIdleSync();
    }

    static void checkAttachedToWindow(View view) throws Exception {
        if (!view.isAttachedToWindow()) {
            final CountDownLatch latch = new CountDownLatch(1);
            View.OnAttachStateChangeListener listener = new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    latch.countDown();
                }
                @Override
                public void onViewDetachedFromWindow(View v) {
                }
            };
            view.addOnAttachStateChangeListener(listener);
            assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        }
    }

    MediaItem createTestMediaItem() {
        Uri testVideoUri = Uri.parse(
                "android.resource://" + mContext.getPackageName() + "/"
                        + R.raw.testvideo_with_2_subtitle_tracks);
        return createTestMediaItem(testVideoUri);
    }

    MediaItem createTestMediaItem(Uri uri) {
        return new UriMediaItem.Builder(uri).build();
    }

    PlayerWrapper createPlayerWrapperOfController(@NonNull PlayerWrapper.PlayerCallback callback,
            @Nullable MediaItem item) {
        prepareLooper();

        SessionPlayer player = new MediaPlayer(mContext);
        MediaSession session = new MediaSession.Builder(mContext, player)
                .setId(UUID.randomUUID().toString())
                .build();
        MediaController controller = new MediaController.Builder(mContext)
                .setSessionToken(session.getToken())
                .build();
        synchronized (mLock) {
            mPlayers.add(player);
            mSessions.add(session);
            mControllers.add(controller);
        }
        PlayerWrapper wrapper = new PlayerWrapper(controller, mMainHandlerExecutor, callback);
        wrapper.attachCallback();
        if (item != null) {
            player.setMediaItem(item);
            player.prepare();
        }
        return wrapper;
    }

    PlayerWrapper createPlayerWrapperOfPlayer(@NonNull PlayerWrapper.PlayerCallback callback,
            @Nullable MediaItem item) {
        SessionPlayer player = new MediaPlayer(mContext);
        synchronized (mLock) {
            mPlayers.add(player);
        }
        PlayerWrapper wrapper = new PlayerWrapper(player, mMainHandlerExecutor, callback);
        wrapper.attachCallback();
        if (item != null) {
            player.setMediaItem(item);
            player.prepare();
        }
        return wrapper;
    }

    void closeAll() {
        synchronized (mLock) {
            for (MediaController controller : mControllers) {
                controller.close();
            }
            for (MediaSession session : mSessions) {
                session.close();
            }
            for (SessionPlayer player : mPlayers) {
                try {
                    player.close();
                } catch (Exception ex) {
                    // ignore
                }
            }
            mControllers.clear();
            mSessions.clear();
            mPlayers.clear();
        }
    }

    class DefaultPlayerCallback extends PlayerWrapper.PlayerCallback {
        CountDownLatch mItemLatch = new CountDownLatch(1);
        CountDownLatch mPausedLatch = new CountDownLatch(1);
        CountDownLatch mPlayingLatch = new CountDownLatch(1);

        @Override
        void onCurrentMediaItemChanged(@NonNull PlayerWrapper player,
                @Nullable MediaItem item) {
            if (item != null) {
                mItemLatch.countDown();
            }
        }

        @Override
        void onPlayerStateChanged(@NonNull PlayerWrapper player, int state) {
            if (state == SessionPlayer.PLAYER_STATE_PAUSED) {
                mPausedLatch.countDown();
            } else if (state == SessionPlayer.PLAYER_STATE_PLAYING) {
                mPlayingLatch.countDown();
            }
        }
    }
}
