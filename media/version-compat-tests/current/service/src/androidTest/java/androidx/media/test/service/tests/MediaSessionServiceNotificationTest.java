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

package androidx.media.test.service.tests;

import static android.support.mediacompat.testlib.util.IntentUtil.CLIENT_PACKAGE_NAME;

import static androidx.media.test.lib.CommonConstants.MOCK_MEDIA_SESSION_SERVICE;
import static androidx.media2.MediaMetadata.BROWSABLE_TYPE_NONE;
import static androidx.media2.MediaMetadata.METADATA_KEY_ALBUM_ART;
import static androidx.media2.MediaMetadata.METADATA_KEY_ARTIST;
import static androidx.media2.MediaMetadata.METADATA_KEY_BROWSABLE;
import static androidx.media2.MediaMetadata.METADATA_KEY_DISPLAY_TITLE;
import static androidx.media2.MediaMetadata.METADATA_KEY_MEDIA_ID;
import static androidx.media2.MediaMetadata.METADATA_KEY_PLAYABLE;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.media.test.service.MockPlayer;
import androidx.media.test.service.RemoteMediaController;
import androidx.media.test.service.TestServiceRegistry;
import androidx.media2.MediaItem;
import androidx.media2.MediaLibraryService.MediaLibrarySession.MediaLibrarySessionCallback;
import androidx.media2.MediaMetadata;
import androidx.media2.MediaSession;
import androidx.media2.MediaSession.ControllerInfo;
import androidx.media2.MediaSessionService;
import androidx.media2.SessionCommandGroup;
import androidx.media2.SessionPlayer;
import androidx.media2.SessionToken;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

/**
 * Manual test of {@link MediaSessionService} for showing/removing notification
 * when the playback is started/ended.
 * <p>
 * This test is a manual test, which means the one who runs this test should keep looking at the
 * device and check whether the notification is shown/removed.
 */
@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@Ignore("Comment out this line and manually run the test.")
public class MediaSessionServiceNotificationTest extends MediaSessionTestBase {
    private static final long NOTIFICATION_SHOW_TIME_MS = 15000;

    MediaSession mSession;
    MockPlayer mPlayer;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mPlayer = new MockPlayer(true);
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
            public SessionCommandGroup onConnect(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller) {
                if (CLIENT_PACKAGE_NAME.equals(controller.getPackageName())) {
                    mSession = session;
                    // Change the player and playlist agent with ours.
                    session.updatePlayer(mPlayer);
                    latch.countDown();
                }
                return super.onConnect(session, controller);
            }
        };
        TestServiceRegistry.getInstance().setSessionCallback(sessionCallback);

        // Create a controller to start the service.
        RemoteMediaController controller = createRemoteController(
                new SessionToken(mContext, MOCK_MEDIA_SESSION_SERVICE), true);

        // Set current media item.
        final String mediaId = "testMediaId";
        Bitmap albumArt = BitmapFactory.decodeResource(mContext.getResources(),
                android.support.mediacompat.service.R.drawable.big_buck_bunny);
        MediaMetadata metadata = new MediaMetadata.Builder()
                .putText(METADATA_KEY_MEDIA_ID, mediaId)
                .putText(METADATA_KEY_DISPLAY_TITLE, "Test Song Name")
                .putText(METADATA_KEY_ARTIST, "Test Artist Name")
                .putBitmap(METADATA_KEY_ALBUM_ART, albumArt)
                .putLong(METADATA_KEY_BROWSABLE, BROWSABLE_TYPE_NONE)
                .putLong(METADATA_KEY_PLAYABLE, 1)
                .build();
        mPlayer.mCurrentMediaItem = new MediaItem.Builder()
                        .setMetadata(metadata)
                        .build();

        // Notification should be shown. Clicking play/pause button will change the player state.
        // When playing, the notification will not be removed by swiping horizontally.
        // When paused, the notification can be swiped away.
        mPlayer.notifyPlayerStateChanged(SessionPlayer.PLAYER_STATE_PLAYING);
        Thread.sleep(NOTIFICATION_SHOW_TIME_MS);
    }
}
