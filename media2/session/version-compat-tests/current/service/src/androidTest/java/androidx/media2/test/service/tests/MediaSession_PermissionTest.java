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
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_PLAY_FROM_SEARCH;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_PLAY_FROM_URI;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_PREPARE_FROM_URI;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_REWIND;
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
import android.os.Build;
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
import androidx.test.filters.SdkSuppress;

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
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
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
            public SessionCommandGroup onConnect(MediaSession session,
                    ControllerInfo controller) {
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
    public void testPlay() throws InterruptedException {
        prepareLooper();
        testOnCommandRequest(COMMAND_CODE_PLAYER_PLAY, new PermissionTestTask() {
            @Override
            public void run(RemoteMediaController controller) {
                controller.play();
            }
        });
    }

    @Test
    public void testPause() throws InterruptedException {
        prepareLooper();
        testOnCommandRequest(COMMAND_CODE_PLAYER_PAUSE, new PermissionTestTask() {
            @Override
            public void run(RemoteMediaController controller) {
                controller.pause();
            }
        });
    }

    @Test
    public void testSeekTo() throws InterruptedException {
        prepareLooper();
        final long position = 10;
        testOnCommandRequest(COMMAND_CODE_PLAYER_SEEK_TO, new PermissionTestTask() {
            @Override
            public void run(RemoteMediaController controller) {
                controller.seekTo(position);
            }
        });
    }

    @Test
    public void testSkipToNext() throws InterruptedException {
        prepareLooper();
        testOnCommandRequest(COMMAND_CODE_PLAYER_SKIP_TO_NEXT_PLAYLIST_ITEM,
                new PermissionTestTask() {
                    @Override
                    public void run(RemoteMediaController controller) {
                        controller.skipToNextItem();
                    }
                });
    }

    @Test
    public void testSkipToPrevious() throws InterruptedException {
        prepareLooper();
        testOnCommandRequest(COMMAND_CODE_PLAYER_SKIP_TO_PREVIOUS_PLAYLIST_ITEM,
                new PermissionTestTask() {
                    @Override
                    public void run(RemoteMediaController controller) {
                        controller.skipToPreviousItem();
                    }
                });
    }

    @Test
    public void testSkipToPlaylistItem() throws InterruptedException {
        prepareLooper();
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
    public void testSetPlaylist() throws InterruptedException {
        prepareLooper();
        final List<String> list = MediaTestUtils.createMediaIds(2);
        testOnCommandRequest(COMMAND_CODE_PLAYER_SET_PLAYLIST, new PermissionTestTask() {
            @Override
            public void run(RemoteMediaController controller) {
                controller.setPlaylist(list, null);
            }
        });
    }

    @Test
    public void testSetMediaItem() throws InterruptedException {
        prepareLooper();
        final String testMediaId = "testSetMediaItem";
        testOnCommandRequest(COMMAND_CODE_PLAYER_SET_MEDIA_ITEM, new PermissionTestTask() {
            @Override
            public void run(RemoteMediaController controller) {
                controller.setMediaItem(testMediaId);
            }
        });
    }

    @Test
    public void testUpdatePlaylistMetadata() throws InterruptedException {
        prepareLooper();
        testOnCommandRequest(COMMAND_CODE_PLAYER_UPDATE_LIST_METADATA,
                new PermissionTestTask() {
                    @Override
                    public void run(RemoteMediaController controller) {
                        controller.updatePlaylistMetadata(null);
                    }
                });
    }

    @Test
    public void testAddPlaylistItem() throws InterruptedException {
        prepareLooper();
        final String testMediaId = "testAddPlaylistItem";
        testOnCommandRequest(COMMAND_CODE_PLAYER_ADD_PLAYLIST_ITEM, new PermissionTestTask() {
            @Override
            public void run(RemoteMediaController controller) {
                controller.addPlaylistItem(0, testMediaId);
            }
        });
    }

    @Test
    public void testRemovePlaylistItem() throws InterruptedException {
        prepareLooper();
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
    public void testReplacePlaylistItem() throws InterruptedException {
        prepareLooper();
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
    public void testSetVolume() throws InterruptedException {
        prepareLooper();
        testOnCommandRequest(COMMAND_CODE_VOLUME_SET_VOLUME, new PermissionTestTask() {
            @Override
            public void run(RemoteMediaController controller) {
                controller.setVolumeTo(0, 0);
            }
        });
    }

    @Test
    public void testAdjustVolume() throws InterruptedException {
        prepareLooper();
        testOnCommandRequest(COMMAND_CODE_VOLUME_ADJUST_VOLUME, new PermissionTestTask() {
            @Override
            public void run(RemoteMediaController controller) {
                controller.adjustVolume(0, 0);
            }
        });
    }

    @Test
    public void testFastForward() throws InterruptedException {
        prepareLooper();
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
    public void testRewind() throws InterruptedException {
        prepareLooper();
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
    public void testSkipForward() throws InterruptedException {
        prepareLooper();
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
    public void testSkipBackward() throws InterruptedException {
        prepareLooper();
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
    public void testPlayFromMediaId() throws InterruptedException {
        prepareLooper();
        final String mediaId = "testPlayFromMediaId";
        createSessionWithAllowedActions(
                createCommandGroupWith(COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID));
        createRemoteController(mSession.getToken()).playFromMediaId(mediaId, null);

        assertTrue(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mCallback.mOnPlayFromMediaIdCalled);
        assertEquals(mediaId, mCallback.mMediaId);
        assertNull(mCallback.mExtras);

        createSessionWithAllowedActions(
                createCommandGroupWithout(COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID));
        createRemoteController(mSession.getToken()).playFromMediaId(mediaId, null);
        assertFalse(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertFalse(mCallback.mOnPlayFromMediaIdCalled);
    }

    @Test
    public void testPlayFromUri() throws InterruptedException {
        prepareLooper();
        final Uri uri = Uri.parse("play://from.uri");
        createSessionWithAllowedActions(
                createCommandGroupWith(COMMAND_CODE_SESSION_PLAY_FROM_URI));
        createRemoteController(mSession.getToken()).playFromUri(uri, null);

        assertTrue(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mCallback.mOnPlayFromUriCalled);
        assertEquals(uri, mCallback.mUri);
        assertNull(mCallback.mExtras);

        createSessionWithAllowedActions(
                createCommandGroupWithout(COMMAND_CODE_SESSION_PLAY_FROM_URI));
        createRemoteController(mSession.getToken()).playFromUri(uri, null);
        assertFalse(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertFalse(mCallback.mOnPlayFromUriCalled);
    }

    @Test
    public void testPlayFromSearch() throws InterruptedException {
        prepareLooper();
        final String query = "testPlayFromSearch";
        createSessionWithAllowedActions(
                createCommandGroupWith(COMMAND_CODE_SESSION_PLAY_FROM_SEARCH));
        createRemoteController(mSession.getToken()).playFromSearch(query, null);

        assertTrue(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mCallback.mOnPlayFromSearchCalled);
        assertEquals(query, mCallback.mQuery);
        assertNull(mCallback.mExtras);

        createSessionWithAllowedActions(
                createCommandGroupWithout(COMMAND_CODE_SESSION_PLAY_FROM_SEARCH));
        createRemoteController(mSession.getToken()).playFromSearch(query, null);
        assertFalse(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertFalse(mCallback.mOnPlayFromSearchCalled);
    }

    @Test
    public void testPrepareFromMediaId() throws InterruptedException {
        prepareLooper();
        final String mediaId = "testPrepareFromMediaId";
        createSessionWithAllowedActions(
                createCommandGroupWith(COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID));
        createRemoteController(mSession.getToken()).prepareFromMediaId(mediaId, null);

        assertTrue(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mCallback.mOnPrepareFromMediaIdCalled);
        assertEquals(mediaId, mCallback.mMediaId);
        assertNull(mCallback.mExtras);

        createSessionWithAllowedActions(
                createCommandGroupWithout(COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID));
        createRemoteController(mSession.getToken()).prepareFromMediaId(mediaId, null);
        assertFalse(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertFalse(mCallback.mOnPrepareFromMediaIdCalled);
    }

    @Test
    public void testPrepareFromUri() throws InterruptedException {
        prepareLooper();
        final Uri uri = Uri.parse("prepare://from.uri");
        createSessionWithAllowedActions(
                createCommandGroupWith(COMMAND_CODE_SESSION_PREPARE_FROM_URI));
        createRemoteController(mSession.getToken()).prepareFromUri(uri, null);

        assertTrue(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mCallback.mOnPrepareFromUriCalled);
        assertEquals(uri, mCallback.mUri);
        assertNull(mCallback.mExtras);

        createSessionWithAllowedActions(
                createCommandGroupWithout(COMMAND_CODE_SESSION_PREPARE_FROM_URI));
        createRemoteController(mSession.getToken()).prepareFromUri(uri, null);
        assertFalse(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertFalse(mCallback.mOnPrepareFromUriCalled);
    }

    @Test
    public void testPrepareFromSearch() throws InterruptedException {
        prepareLooper();
        final String query = "testPrepareFromSearch";
        createSessionWithAllowedActions(
                createCommandGroupWith(COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH));
        createRemoteController(mSession.getToken()).prepareFromSearch(query, null);

        assertTrue(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mCallback.mOnPrepareFromSearchCalled);
        assertEquals(query, mCallback.mQuery);
        assertNull(mCallback.mExtras);

        createSessionWithAllowedActions(
                createCommandGroupWithout(COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH));
        createRemoteController(mSession.getToken()).prepareFromSearch(query, null);
        assertFalse(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertFalse(mCallback.mOnPrepareFromSearchCalled);
    }

    @Test
    public void testSetRating() throws InterruptedException {
        prepareLooper();
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
    public void testChangingPermissionWithSetAllowedCommands() throws InterruptedException {
        prepareLooper();
        final String query = "testChangingPermissionWithSetAllowedCommands";
        createSessionWithAllowedActions(
                createCommandGroupWith(COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH));
        RemoteMediaController controller = createRemoteController(mSession.getToken());

        controller.prepareFromSearch(query, null);
        assertTrue(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mCallback.mOnPrepareFromSearchCalled);
        assertEquals(query, mCallback.mQuery);
        assertNull(mCallback.mExtras);
        mCallback.reset();

        // Change allowed commands.
        mSession.setAllowedCommands(getTestControllerInfo(),
                createCommandGroupWithout(COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH));
        Thread.sleep(TIMEOUT_MS);

        controller.prepareFromSearch(query, null);
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
        public String mQuery;
        public Uri mUri;
        public Bundle mExtras;
        public Rating mRating;

        public boolean mOnCommandRequestCalled;
        public boolean mOnPlayFromMediaIdCalled;
        public boolean mOnPlayFromSearchCalled;
        public boolean mOnPlayFromUriCalled;
        public boolean mOnPrepareFromMediaIdCalled;
        public boolean mOnPrepareFromSearchCalled;
        public boolean mOnPrepareFromUriCalled;
        public boolean mOnFastForwardCalled;
        public boolean mOnRewindCalled;
        public boolean mOnSkipForwardCalled;
        public boolean mOnSkipBackwardCalled;
        public boolean mOnSetRatingCalled;


        public MySessionCallback() {
            mCountDownLatch = new CountDownLatch(1);
        }

        public void reset() {
            mCountDownLatch = new CountDownLatch(1);

            mCommand = null;
            mMediaId = null;
            mQuery = null;
            mUri = null;
            mExtras = null;

            mOnCommandRequestCalled = false;
            mOnPlayFromMediaIdCalled = false;
            mOnPlayFromSearchCalled = false;
            mOnPlayFromUriCalled = false;
            mOnPrepareFromMediaIdCalled = false;
            mOnPrepareFromSearchCalled = false;
            mOnPrepareFromUriCalled = false;
            mOnFastForwardCalled = false;
            mOnRewindCalled = false;
            mOnSetRatingCalled = false;
        }

        @Override
        public int onCommandRequest(MediaSession session, ControllerInfo controller,
                SessionCommand command) {
            assertTrue(TextUtils.equals(CLIENT_PACKAGE_NAME, controller.getPackageName()));
            mOnCommandRequestCalled = true;
            mCommand = command;
            mCountDownLatch.countDown();
            return super.onCommandRequest(session, controller, command);
        }

        @Override
        public int onFastForward(MediaSession session, ControllerInfo controller) {
            assertTrue(TextUtils.equals(CLIENT_PACKAGE_NAME, controller.getPackageName()));
            mOnFastForwardCalled = true;
            mCountDownLatch.countDown();
            return RESULT_SUCCESS;
        }

        @Override
        public int onRewind(MediaSession session, ControllerInfo controller) {
            assertTrue(TextUtils.equals(CLIENT_PACKAGE_NAME, controller.getPackageName()));
            mOnRewindCalled = true;
            mCountDownLatch.countDown();
            return RESULT_SUCCESS;
        }

        @Override
        public int onSkipForward(MediaSession session, ControllerInfo controller) {
            assertTrue(TextUtils.equals(CLIENT_PACKAGE_NAME, controller.getPackageName()));
            mOnSkipForwardCalled = true;
            mCountDownLatch.countDown();
            return RESULT_SUCCESS;
        }

        @Override
        public int onSkipBackward(MediaSession session, ControllerInfo controller) {
            assertTrue(TextUtils.equals(CLIENT_PACKAGE_NAME, controller.getPackageName()));
            mOnSkipBackwardCalled = true;
            mCountDownLatch.countDown();
            return RESULT_SUCCESS;
        }

        @Override
        public int onPlayFromMediaId(MediaSession session, ControllerInfo controller,
                String mediaId, Bundle extras) {
            assertTrue(TextUtils.equals(CLIENT_PACKAGE_NAME, controller.getPackageName()));
            mOnPlayFromMediaIdCalled = true;
            mMediaId = mediaId;
            mExtras = extras;
            mCountDownLatch.countDown();
            return RESULT_SUCCESS;
        }

        @Override
        public int onPlayFromSearch(MediaSession session, ControllerInfo controller,
                String query, Bundle extras) {
            assertTrue(TextUtils.equals(CLIENT_PACKAGE_NAME, controller.getPackageName()));
            mOnPlayFromSearchCalled = true;
            mQuery = query;
            mExtras = extras;
            mCountDownLatch.countDown();
            return RESULT_SUCCESS;
        }

        @Override
        public int onPlayFromUri(MediaSession session, ControllerInfo controller,
                Uri uri, Bundle extras) {
            assertTrue(TextUtils.equals(CLIENT_PACKAGE_NAME, controller.getPackageName()));
            mOnPlayFromUriCalled = true;
            mUri = uri;
            mExtras = extras;
            mCountDownLatch.countDown();
            return RESULT_SUCCESS;
        }

        @Override
        public int onPrepareFromMediaId(MediaSession session, ControllerInfo controller,
                String mediaId, Bundle extras) {
            assertTrue(TextUtils.equals(CLIENT_PACKAGE_NAME, controller.getPackageName()));
            mOnPrepareFromMediaIdCalled = true;
            mMediaId = mediaId;
            mExtras = extras;
            mCountDownLatch.countDown();
            return RESULT_SUCCESS;
        }

        @Override
        public int onPrepareFromSearch(MediaSession session, ControllerInfo controller,
                String query, Bundle extras) {
            assertTrue(TextUtils.equals(CLIENT_PACKAGE_NAME, controller.getPackageName()));
            mOnPrepareFromSearchCalled = true;
            mQuery = query;
            mExtras = extras;
            mCountDownLatch.countDown();
            return RESULT_SUCCESS;
        }

        @Override
        public int onPrepareFromUri(MediaSession session, ControllerInfo controller,
                Uri uri, Bundle extras) {
            assertTrue(TextUtils.equals(CLIENT_PACKAGE_NAME, controller.getPackageName()));
            mOnPrepareFromUriCalled = true;
            mUri = uri;
            mExtras = extras;
            mCountDownLatch.countDown();
            return RESULT_SUCCESS;
        }

        @Override
        public int onSetRating(MediaSession session, ControllerInfo controller,
                String mediaId, Rating rating) {
            assertTrue(TextUtils.equals(CLIENT_PACKAGE_NAME, controller.getPackageName()));
            mOnSetRatingCalled = true;
            mMediaId = mediaId;
            mRating = rating;
            mCountDownLatch.countDown();
            return RESULT_SUCCESS;
        }
    }
}
