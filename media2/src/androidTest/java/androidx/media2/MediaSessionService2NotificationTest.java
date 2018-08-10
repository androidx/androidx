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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.media2.MediaLibraryService2.MediaLibrarySession.MediaLibrarySessionCallback;
import androidx.media2.MediaSession2.ControllerInfo;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

/**
 * Manual test of {@link MediaSessionService2} for showing/removing notification when the
 * playback is started/ended.
 * <p>
 * This test is a manual test, which means the one who runs this test should keep looking at the
 * device and check whether the notification is shown/removed.
 */
@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
public class MediaSessionService2NotificationTest extends MediaSession2TestBase {
    private static final long NOTIFICATION_SHOW_TIME_MS = 15000;

    MediaSession2 mSession;
    MockPlayer mPlayer;
    MockPlaylistAgent mPlaylistAgent;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mPlayer = new MockPlayer(true);
        mPlaylistAgent = new MockPlaylistAgent();
        TestServiceRegistry.getInstance().setHandler(sHandler);
    }

    @After
    public void cleanUp() throws Exception {
        super.cleanUp();
    }

    @Test
    public void testNotification() throws InterruptedException {
        prepareLooper();
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaLibrarySessionCallback sessionCallback = new MediaLibrarySessionCallback() {
            @Override
            public SessionCommandGroup2 onConnect(@NonNull MediaSession2 session,
                    @NonNull ControllerInfo controller) {
                if (Process.myUid() == controller.getUid()) {
                    mSession = session;
                    // Change the player and playlist agent with ours.
                    session.updatePlayerConnector(mPlayer, mPlaylistAgent);
                    latch.countDown();
                }
                return super.onConnect(session, controller);
            }
        };
        TestServiceRegistry.getInstance().setSessionCallback(sessionCallback);

        // Create a controller to start the service.
        MediaController2 controller =
                createController(TestUtils.getServiceToken(mContext, MockMediaSessionService2.ID));

        // Set current media item.
        final String mediaId = "testMediaId";
        Bitmap albumArt = BitmapFactory.decodeResource(mContext.getResources(),
                androidx.media2.test.R.drawable.big_buck_bunny);
        MediaMetadata2 metadata = new MediaMetadata2.Builder()
                        .putText(MediaMetadata2.METADATA_KEY_MEDIA_ID, mediaId)
                        .putText(MediaMetadata2.METADATA_KEY_DISPLAY_TITLE, "Test Song Name")
                        .putText(MediaMetadata2.METADATA_KEY_ARTIST, "Test Artist Name")
                        .putBitmap(MediaMetadata2.METADATA_KEY_ALBUM_ART, albumArt)
                        .build();
        mPlaylistAgent.mCurrentMediaItem = new MediaItem2.Builder(MediaItem2.FLAG_PLAYABLE)
                        .setMediaId(mediaId)
                        .setMetadata(metadata)
                        .build();

        // Notification should be shown. Clicking play/pause button will change the player state.
        // When playing, the notification will not be removed by swiping horizontally.
        // When paused, the notification can be swiped away.
        mPlayer.notifyPlaybackState(MediaPlayerConnector.PLAYER_STATE_PLAYING);
        Thread.sleep(NOTIFICATION_SHOW_TIME_MS);
    }
}
