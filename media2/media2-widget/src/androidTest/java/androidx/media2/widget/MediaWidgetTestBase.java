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

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.GuardedBy;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.UriMediaItem;
import androidx.media2.player.MediaPlayer;
import androidx.media2.session.MediaController;
import androidx.media2.session.MediaSession;
import androidx.media2.widget.test.R;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MediaWidgetTestBase extends MediaTestBase {
    static final String PLAYER_TYPE_MEDIA_CONTROLLER = "MediaController";
    static final String PLAYER_TYPE_MEDIA_PLAYER = "MediaPlayer";

    // Expected success time
    // Increased timeout to pass on old devices (ex. Nexus4 API 17)
    static final int WAIT_TIME_MS = 2000;

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private List<SessionPlayer> mPlayers = new ArrayList<>();
    @GuardedBy("mLock")
    private List<MediaSession> mSessions = new ArrayList<>();
    @GuardedBy("mLock")
    private List<MediaController> mControllers = new ArrayList<>();

    Context mContext;
    Executor mMainHandlerExecutor;
    Executor mSessionCallbackExecutor;

    @Before
    public void setupWidgetTest() {
        mContext = ApplicationProvider.getApplicationContext();
        mMainHandlerExecutor = ContextCompat.getMainExecutor(mContext);
        mSessionCallbackExecutor = Executors.newFixedThreadPool(1);
    }

    static void checkAttachedToWindow(View view) throws Exception {
        if (!ViewCompat.isAttachedToWindow(view)) {
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
        Uri testVideoUri = getResourceUri(R.raw.testvideo_with_2_subtitle_tracks);
        return createTestMediaItem(testVideoUri);
    }

    MediaItem createTestMediaItem(Uri uri) {
        return createTestMediaItem(uri, "defaultMediaId");
    }

    MediaItem createTestMediaItem(Uri uri, String mediaId) {
        MediaMetadata metadata = new MediaMetadata.Builder()
                .putText(MediaMetadata.METADATA_KEY_MEDIA_ID, mediaId)
                .build();
        return new UriMediaItem.Builder(uri)
                .setMetadata(metadata)
                .build();
    }

    List<MediaItem> createTestPlaylist() {
        List<MediaItem> list = new ArrayList<>();
        list.add(createTestMediaItem(getResourceUri(R.raw.test_file_scheme_video), "id_1"));
        list.add(createTestMediaItem(getResourceUri(R.raw.test_music), "id_2"));
        list.add(createTestMediaItem(getResourceUri(R.raw.testvideo_with_2_subtitle_tracks),
                "id_3"));
        return list;
    }

    Uri getResourceUri(@IdRes int resId) {
        return Uri.parse("android.resource://" + mContext.getPackageName() + "/" + resId);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    PlayerWrapper createPlayerWrapperOfController(@NonNull PlayerWrapper.PlayerCallback callback,
            @Nullable MediaItem item, @Nullable List<MediaItem> playlist) {
        SessionPlayer player = new MediaPlayer(mContext);
        MediaSession session = new MediaSession.Builder(mContext, player)
                .setId(UUID.randomUUID().toString())
                .setSessionCallback(mSessionCallbackExecutor, new MediaSession.SessionCallback() {})
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
        } else if (playlist != null) {
            player.setPlaylist(playlist, null);
            player.prepare();
        }
        return wrapper;
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    PlayerWrapper createPlayerWrapperOfPlayer(@NonNull PlayerWrapper.PlayerCallback callback,
            @Nullable MediaItem item, @Nullable List<MediaItem> playlist) {
        SessionPlayer player = new MediaPlayer(mContext);
        synchronized (mLock) {
            mPlayers.add(player);
        }
        PlayerWrapper wrapper = new PlayerWrapper(player, mMainHandlerExecutor, callback);
        wrapper.attachCallback();
        if (item != null) {
            player.setMediaItem(item);
            player.prepare();
        } else if (playlist != null) {
            player.setPlaylist(playlist, null);
            player.prepare();
        }
        return wrapper;
    }

    PlayerWrapper createPlayerWrapperOfType(@NonNull PlayerWrapper.PlayerCallback callback,
            @Nullable MediaItem item, @Nullable List<MediaItem> playlist,
            @NonNull String playerType) {
        if (PLAYER_TYPE_MEDIA_CONTROLLER.equals(playerType)) {
            return createPlayerWrapperOfController(callback, item, playlist);
        } else if (PLAYER_TYPE_MEDIA_PLAYER.equals(playerType)) {
            return createPlayerWrapperOfPlayer(callback, item, playlist);
        } else {
            throw new IllegalArgumentException("unknown playerType " + playerType);
        }
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

    static class DefaultPlayerCallback extends PlayerWrapper.PlayerCallback {
        volatile CountDownLatch mItemLatch = new CountDownLatch(1);
        CountDownLatch mPausedLatch = new CountDownLatch(1);
        CountDownLatch mPlayingLatch = new CountDownLatch(1);
        String mPrevId = "placeholderId";

        @Override
        void onCurrentMediaItemChanged(@NonNull PlayerWrapper player,
                @Nullable MediaItem item) {
            if (item != null && !TextUtils.equals(mPrevId, item.getMediaId())) {
                mPrevId = item.getMediaId();
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
