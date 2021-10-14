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

package androidx.media2.test.service.tests;

import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_ADD_PLAYLIST_ITEM;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_PAUSE;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_PLAY;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_REMOVE_PLAYLIST_ITEM;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_REPLACE_PLAYLIST_ITEM;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_SEEK_TO;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_SET_MEDIA_ITEM;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_SET_PLAYLIST;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_SKIP_TO_NEXT_PLAYLIST_ITEM;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_SKIP_TO_PLAYLIST_ITEM;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_SKIP_TO_PREVIOUS_PLAYLIST_ITEM;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_UPDATE_LIST_METADATA;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_FAST_FORWARD;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_REWIND;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_SET_MEDIA_URI;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_SET_RATING;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_SKIP_BACKWARD;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_SKIP_FORWARD;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_VOLUME_ADJUST_VOLUME;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_VOLUME_SET_VOLUME;
import static androidx.media2.session.SessionResult.RESULT_SUCCESS;
import static androidx.media2.test.common.CommonConstants.CLIENT_PACKAGE_NAME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.media2.common.MediaItem;
import androidx.media2.common.Rating;
import androidx.media2.session.MediaSession;
import androidx.media2.session.MediaSession.ControllerInfo;
import androidx.media2.session.SessionCommand;
import androidx.media2.session.SessionCommandGroup;
import androidx.media2.session.StarRating;
import androidx.media2.test.service.MediaTestUtils;
import androidx.media2.test.service.MockPlayer;
import androidx.media2.test.service.RemoteMediaController;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests whether {@link MediaSession} receives commands that hasn't allowed.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MediaSession_PermissionTest extends MediaSessionTestBase {
    private static final String SESSION_ID = "MediaSessionTest_permission";

    private MockPlayer mPlayer;
    private MediaSession mSession;
    private MySessionCallback mCallback;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        if (mSession != null) {
            mSession.close();
            mSession = null;
        }
        mPlayer = null;
        mCallback = null;
    }

    private MediaSession createSessionWithAllowedActions(final SessionCommandGroup commands) {
        mPlayer = new MockPlayer(1);
        mCallback = new MySessionCallback() {
            @Override
            public SessionCommandGroup onConnect(@NonNull MediaSession session,
                    @NonNull ControllerInfo controller) {
                if (!TextUtils.equals(CLIENT_PACKAGE_NAME, controller.getPackageName())) {
                    return null;
                }
                return commands == null ? new SessionCommandGroup() : commands;
            }
        };
        if (mSession != null) {
            mSession.close();
        }
        mSession = new MediaSession.Builder(mContext, mPlayer).setId(SESSION_ID)
                .setSessionCallback(sHandlerExecutor, mCallback).build();
        return mSession;
    }

    private SessionCommandGroup createCommandGroupWith(int commandCode) {
        SessionCommandGroup commands = new SessionCommandGroup.Builder()
                .addCommand(new SessionCommand(commandCode))
                .build();
        return commands;
    }

    private SessionCommandGroup createCommandGroupWithout(int commandCode) {
        SessionCommandGroup commands = new SessionCommandGroup.Builder()
                .addAllPredefinedCommands(SessionCommand.COMMAND_VERSION_1)
                .removeCommand(new SessionCommand(commandCode))
                .build();
        return commands;
    }

    private void testOnCommandRequest(int commandCode, PermissionTestTask runnable)
            throws InterruptedException {
        createSessionWithAllowedActions(createCommandGroupWith(commandCode));
        runnable.run(createRemoteController(mSession.getToken()));

        assertTrue(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mCallback.mOnCommandRequestCalled);
        assertEquals(commandCode, mCallback.mCommand.getCommandCode());

        createSessionWithAllowedActions(createCommandGroupWithout(commandCode));
        runnable.run(createRemoteController(mSession.getToken()));

        assertFalse(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertFalse(mCallback.mOnCommandRequestCalled);
    }

    @Test
    public void play() throws InterruptedException {
        testOnCommandRequest(COMMAND_CODE_PLAYER_PLAY, new PermissionTestTask() {
            @Override
            public void run(RemoteMediaController controller) {
                controller.play();
            }
        });
    }

    @Test
    public void pause() throws InterruptedException {
        testOnCommandRequest(COMMAND_CODE_PLAYER_PAUSE, new PermissionTestTask() {
            @Override
            public void run(RemoteMediaController controller) {
                controller.pause();
            }
        });
    }

    @Test
    public void seekTo() throws InterruptedException {
        final long position = 10;
        testOnCommandRequest(COMMAND_CODE_PLAYER_SEEK_TO, new PermissionTestTask() {
            @Override
            public void run(RemoteMediaController controller) {
                controller.seekTo(position);
            }
        });
    }

    @Test
    public void skipToNext() throws InterruptedException {
        testOnCommandRequest(COMMAND_CODE_PLAYER_SKIP_TO_NEXT_PLAYLIST_ITEM,
                new PermissionTestTask() {
                    @Override
                    public void run(RemoteMediaController controller) {
                        controller.skipToNextItem();
                    }
                });
    }

    @Test
    public void skipToPrevious() throws InterruptedException {
        testOnCommandRequest(COMMAND_CODE_PLAYER_SKIP_TO_PREVIOUS_PLAYLIST_ITEM,
                new PermissionTestTask() {
                    @Override
                    public void run(RemoteMediaController controller) {
                        controller.skipToPreviousItem();
                    }
                });
    }

    @Test
    public void skipToPlaylistItem() throws InterruptedException {
        testOnCommandRequest(
                COMMAND_CODE_PLAYER_SKIP_TO_PLAYLIST_ITEM,
                new PermissionTestTask() {
                    @Override
                    public void run(RemoteMediaController controller) {
                        controller.skipToPlaylistItem(0);
                    }
                });
    }

    @Test
    public void setPlaylist() throws InterruptedException {
        final List<String> list = MediaTestUtils.createMediaIds(2);
        testOnCommandRequest(COMMAND_CODE_PLAYER_SET_PLAYLIST, new PermissionTestTask() {
            @Override
            public void run(RemoteMediaController controller) {
                controller.setPlaylist(list, null);
            }
        });
    }

    @Test
    public void setMediaItem() throws InterruptedException {
        final String testMediaId = "testSetMediaItem";
        testOnCommandRequest(COMMAND_CODE_PLAYER_SET_MEDIA_ITEM, new PermissionTestTask() {
            @Override
            public void run(RemoteMediaController controller) {
                controller.setMediaItem(testMediaId);
            }
        });
    }

    @Test
    public void updatePlaylistMetadata() throws InterruptedException {
        testOnCommandRequest(COMMAND_CODE_PLAYER_UPDATE_LIST_METADATA,
                new PermissionTestTask() {
                    @Override
                    public void run(RemoteMediaController controller) {
                        controller.updatePlaylistMetadata(null);
                    }
                });
    }

    @Test
    public void addPlaylistItem() throws InterruptedException {
        final String testMediaId = "testAddPlaylistItem";
        testOnCommandRequest(COMMAND_CODE_PLAYER_ADD_PLAYLIST_ITEM, new PermissionTestTask() {
            @Override
            public void run(RemoteMediaController controller) {
                controller.addPlaylistItem(0, testMediaId);
            }
        });
    }

    @Test
    public void removePlaylistItem() throws InterruptedException {
        final MediaItem testItem = MediaTestUtils.createMediaItemWithMetadata();
        testOnCommandRequest(COMMAND_CODE_PLAYER_REMOVE_PLAYLIST_ITEM,
                new PermissionTestTask() {
                    @Override
                    public void run(RemoteMediaController controller) {
                        controller.removePlaylistItem(0);
                    }
                });
    }

    @Test
    public void replacePlaylistItem() throws InterruptedException {
        final String testMediaId = "testReplacePlaylistItem";
        testOnCommandRequest(COMMAND_CODE_PLAYER_REPLACE_PLAYLIST_ITEM,
                new PermissionTestTask() {
                    @Override
                    public void run(RemoteMediaController controller) {
                        controller.replacePlaylistItem(0, testMediaId);
                    }
                });
    }

    @Test
    public void setVolume() throws InterruptedException {
        testOnCommandRequest(COMMAND_CODE_VOLUME_SET_VOLUME, new PermissionTestTask() {
            @Override
            public void run(RemoteMediaController controller) {
                controller.setVolumeTo(0, 0);
            }
        });
    }

    @Test
    public void adjustVolume() throws InterruptedException {
        testOnCommandRequest(COMMAND_CODE_VOLUME_ADJUST_VOLUME, new PermissionTestTask() {
            @Override
            public void run(RemoteMediaController controller) {
                controller.adjustVolume(0, 0);
            }
        });
    }

    @Test
    public void fastForward() throws InterruptedException {
        createSessionWithAllowedActions(
                createCommandGroupWith(COMMAND_CODE_SESSION_FAST_FORWARD));
        createRemoteController(mSession.getToken()).fastForward();

        assertTrue(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mCallback.mOnFastForwardCalled);

        createSessionWithAllowedActions(
                createCommandGroupWithout(COMMAND_CODE_SESSION_FAST_FORWARD));
        createRemoteController(mSession.getToken()).fastForward();
        assertFalse(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertFalse(mCallback.mOnFastForwardCalled);
    }

    @Test
    public void rewind() throws InterruptedException {
        createSessionWithAllowedActions(
                createCommandGroupWith(COMMAND_CODE_SESSION_REWIND));
        createRemoteController(mSession.getToken()).rewind();

        assertTrue(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mCallback.mOnRewindCalled);

        createSessionWithAllowedActions(
                createCommandGroupWithout(COMMAND_CODE_SESSION_REWIND));
        createRemoteController(mSession.getToken()).rewind();
        assertFalse(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertFalse(mCallback.mOnRewindCalled);
    }

    @Test
    public void skipForward() throws InterruptedException {
        createSessionWithAllowedActions(
                createCommandGroupWith(COMMAND_CODE_SESSION_SKIP_FORWARD));
        createRemoteController(mSession.getToken()).skipForward();

        assertTrue(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mCallback.mOnSkipForwardCalled);

        createSessionWithAllowedActions(
                createCommandGroupWithout(COMMAND_CODE_SESSION_SKIP_FORWARD));
        createRemoteController(mSession.getToken()).skipForward();
        assertFalse(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertFalse(mCallback.mOnSkipForwardCalled);
    }

    @Test
    public void skipBackward() throws InterruptedException {
        createSessionWithAllowedActions(
                createCommandGroupWith(COMMAND_CODE_SESSION_SKIP_BACKWARD));
        createRemoteController(mSession.getToken()).skipBackward();

        assertTrue(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mCallback.mOnSkipBackwardCalled);

        createSessionWithAllowedActions(
                createCommandGroupWithout(COMMAND_CODE_SESSION_SKIP_BACKWARD));
        createRemoteController(mSession.getToken()).skipBackward();
        assertFalse(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertFalse(mCallback.mOnSkipBackwardCalled);
    }

    @Test
    public void setMediaUri() throws InterruptedException {
        final Uri uri = Uri.parse("media://uri");
        createSessionWithAllowedActions(
                createCommandGroupWith(COMMAND_CODE_SESSION_SET_MEDIA_URI));
        createRemoteController(mSession.getToken()).setMediaUri(uri, null);

        assertTrue(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mCallback.mOnSetMediaUriCalled);
        assertEquals(uri, mCallback.mUri);
        assertNull(mCallback.mExtras);

        createSessionWithAllowedActions(
                createCommandGroupWithout(COMMAND_CODE_SESSION_SET_MEDIA_URI));
        createRemoteController(mSession.getToken()).setMediaUri(uri, null);
        assertFalse(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertFalse(mCallback.mOnSetMediaUriCalled);
    }

    @Test
    public void setRating() throws InterruptedException {
        final String mediaId = "testSetRating";
        final Rating rating = new StarRating(5, 3.5f);
        createSessionWithAllowedActions(
                createCommandGroupWith(COMMAND_CODE_SESSION_SET_RATING));
        createRemoteController(mSession.getToken()).setRating(mediaId, rating);

        assertTrue(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mCallback.mOnSetRatingCalled);
        assertEquals(mediaId, mCallback.mMediaId);
        assertEquals(rating, mCallback.mRating);

        createSessionWithAllowedActions(
                createCommandGroupWithout(COMMAND_CODE_SESSION_SET_RATING));
        createRemoteController(mSession.getToken()).setRating(mediaId, rating);
        assertFalse(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertFalse(mCallback.mOnSetRatingCalled);
    }

    @Test
    public void changingPermissionWithSetAllowedCommands() throws InterruptedException {
        final String mediaId = "testSetRating";
        final Rating rating = new StarRating(5, 3.5f);
        createSessionWithAllowedActions(
                createCommandGroupWith(COMMAND_CODE_SESSION_SET_RATING));
        RemoteMediaController controller = createRemoteController(mSession.getToken());
        controller.setRating(mediaId, rating);

        assertTrue(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mCallback.mOnSetRatingCalled);
        assertEquals(mediaId, mCallback.mMediaId);
        assertEquals(rating, mCallback.mRating);
        mCallback.reset();

        // Change allowed commands.
        mSession.setAllowedCommands(getTestControllerInfo(),
                createCommandGroupWithout(COMMAND_CODE_SESSION_SET_RATING));

        controller.setRating(mediaId, rating);
        assertFalse(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private ControllerInfo getTestControllerInfo() {
        List<ControllerInfo> controllers = mSession.getConnectedControllers();
        assertNotNull(controllers);
        for (int i = 0; i < controllers.size(); i++) {
            if (TextUtils.equals(CLIENT_PACKAGE_NAME, controllers.get(i).getPackageName())) {
                return controllers.get(i);
            }
        }
        fail("Failed to get test controller info");
        return null;
    }

    @FunctionalInterface
    private interface PermissionTestTask {
        void run(@NonNull RemoteMediaController controller);
    }

    public class MySessionCallback extends MediaSession.SessionCallback {
        public CountDownLatch mCountDownLatch;

        public SessionCommand mCommand;
        public String mMediaId;
        public Uri mUri;
        public Bundle mExtras;
        public Rating mRating;

        public boolean mOnCommandRequestCalled;
        public boolean mOnFastForwardCalled;
        public boolean mOnRewindCalled;
        public boolean mOnSkipForwardCalled;
        public boolean mOnSkipBackwardCalled;
        public boolean mOnSetMediaUriCalled;
        public boolean mOnSetRatingCalled;


        public MySessionCallback() {
            mCountDownLatch = new CountDownLatch(1);
        }

        public void reset() {
            mCountDownLatch = new CountDownLatch(1);

            mCommand = null;
            mMediaId = null;
            mUri = null;
            mExtras = null;

            mOnCommandRequestCalled = false;
            mOnFastForwardCalled = false;
            mOnRewindCalled = false;
            mOnSetMediaUriCalled = false;
            mOnSetRatingCalled = false;
        }

        @Override
        public int onCommandRequest(@NonNull MediaSession session,
                @NonNull ControllerInfo controller, @NonNull SessionCommand command) {
            assertTrue(TextUtils.equals(CLIENT_PACKAGE_NAME, controller.getPackageName()));
            mOnCommandRequestCalled = true;
            mCommand = command;
            mCountDownLatch.countDown();
            return super.onCommandRequest(session, controller, command);
        }

        @Override
        public int onFastForward(@NonNull MediaSession session,
                @NonNull ControllerInfo controller) {
            assertTrue(TextUtils.equals(CLIENT_PACKAGE_NAME, controller.getPackageName()));
            mOnFastForwardCalled = true;
            mCountDownLatch.countDown();
            return RESULT_SUCCESS;
        }

        @Override
        public int onRewind(@NonNull MediaSession session, @NonNull ControllerInfo controller) {
            assertTrue(TextUtils.equals(CLIENT_PACKAGE_NAME, controller.getPackageName()));
            mOnRewindCalled = true;
            mCountDownLatch.countDown();
            return RESULT_SUCCESS;
        }

        @Override
        public int onSkipForward(@NonNull MediaSession session,
                @NonNull ControllerInfo controller) {
            assertTrue(TextUtils.equals(CLIENT_PACKAGE_NAME, controller.getPackageName()));
            mOnSkipForwardCalled = true;
            mCountDownLatch.countDown();
            return RESULT_SUCCESS;
        }

        @Override
        public int onSkipBackward(@NonNull MediaSession session,
                @NonNull ControllerInfo controller) {
            assertTrue(TextUtils.equals(CLIENT_PACKAGE_NAME, controller.getPackageName()));
            mOnSkipBackwardCalled = true;
            mCountDownLatch.countDown();
            return RESULT_SUCCESS;
        }

        @Override
        public int onSetMediaUri(@NonNull MediaSession session,
                @NonNull ControllerInfo controller, @NonNull Uri uri, Bundle extras) {
            assertTrue(TextUtils.equals(CLIENT_PACKAGE_NAME, controller.getPackageName()));
            mOnSetMediaUriCalled = true;
            mUri = uri;
            mExtras = extras;
            mCountDownLatch.countDown();
            return RESULT_SUCCESS;
        }

        @Override
        public int onSetRating(@NonNull MediaSession session, @NonNull ControllerInfo controller,
                @NonNull String mediaId, @NonNull Rating rating) {
            assertTrue(TextUtils.equals(CLIENT_PACKAGE_NAME, controller.getPackageName()));
            mOnSetRatingCalled = true;
            mMediaId = mediaId;
            mRating = rating;
            mCountDownLatch.countDown();
            return RESULT_SUCCESS;
        }
    }
}
