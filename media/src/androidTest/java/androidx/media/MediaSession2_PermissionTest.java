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

import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYBACK_PAUSE;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYBACK_PLAY;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYBACK_RESET;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYBACK_SEEK_TO;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_ADD_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_REMOVE_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_REPLACE_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SET_LIST;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SET_LIST_METADATA;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_NEXT_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_PREV_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_PLAYLIST_SKIP_TO_PLAYLIST_ITEM;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_FAST_FORWARD;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_SEARCH;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_URI;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_URI;
import static androidx.media.SessionCommand2.COMMAND_CODE_SESSION_REWIND;
import static androidx.media.SessionCommand2.COMMAND_CODE_SET_VOLUME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.annotation.NonNull;
import androidx.media.MediaSession2.ControllerInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests whether {@link MediaSession2} receives commands that hasn't allowed.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
@Ignore
public class MediaSession2_PermissionTest extends MediaSession2TestBase {
    private static final String SESSION_ID = "MediaSession2Test_permission";

    private MockPlayer mPlayer;
    private MediaSession2 mSession;
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

    private MediaSession2 createSessionWithAllowedActions(final SessionCommandGroup2 commands) {
        mPlayer = new MockPlayer(0);
        mCallback = new MySessionCallback() {
            @Override
            public SessionCommandGroup2 onConnect(MediaSession2 session,
                    ControllerInfo controller) {
                if (Process.myUid() != controller.getUid()) {
                    return null;
                }
                return commands == null ? new SessionCommandGroup2() : commands;
            }
        };
        if (mSession != null) {
            mSession.close();
        }
        mSession = new MediaSession2.Builder(mContext).setPlayer(mPlayer).setId(SESSION_ID)
                .setSessionCallback(sHandlerExecutor, mCallback).build();
        return mSession;
    }

    private SessionCommandGroup2 createCommandGroupWith(int commandCode) {
        SessionCommandGroup2 commands = new SessionCommandGroup2();
        commands.addCommand(new SessionCommand2(commandCode));
        return commands;
    }

    private SessionCommandGroup2 createCommandGroupWithout(int commandCode) {
        SessionCommandGroup2 commands = new SessionCommandGroup2();
        commands.addAllPredefinedCommands();
        commands.removeCommand(new SessionCommand2(commandCode));
        return commands;
    }

    private void testOnCommandRequest(int commandCode, PermissionTestRunnable runnable)
            throws InterruptedException {
        createSessionWithAllowedActions(createCommandGroupWith(commandCode));
        runnable.run(createController(mSession.getToken()));

        assertTrue(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mCallback.mOnCommandRequestCalled);
        assertEquals(commandCode, mCallback.mCommand.getCommandCode());

        createSessionWithAllowedActions(createCommandGroupWithout(commandCode));
        runnable.run(createController(mSession.getToken()));

        assertFalse(mCallback.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertFalse(mCallback.mOnCommandRequestCalled);
    }

    @Test
    public void testPlay() throws InterruptedException {
        testOnCommandRequest(COMMAND_CODE_PLAYBACK_PLAY, new PermissionTestRunnable() {
            @Override
            public void run(MediaController2 controller) {
                controller.play();
            }
        });
    }

    @Test
    public void testPause() throws InterruptedException {
        testOnCommandRequest(COMMAND_CODE_PLAYBACK_PAUSE, new PermissionTestRunnable() {
            @Override
            public void run(MediaController2 controller) {
                controller.pause();
            }
        });
    }

    @Test
    public void testReset() throws InterruptedException {
        testOnCommandRequest(COMMAND_CODE_PLAYBACK_RESET, new PermissionTestRunnable() {
            @Override
            public void run(MediaController2 controller) {
                controller.reset();
            }
        });
    }

    @Test
    public void testFastForward() throws InterruptedException {
        testOnCommandRequest(COMMAND_CODE_SESSION_FAST_FORWARD, new PermissionTestRunnable() {
            @Override
            public void run(MediaController2 controller) {
                controller.fastForward();
            }
        });
    }

    @Test
    public void testRewind() throws InterruptedException {
        testOnCommandRequest(COMMAND_CODE_SESSION_REWIND, new PermissionTestRunnable() {
            @Override
            public void run(MediaController2 controller) {
                controller.rewind();
            }
        });
    }

    @Test
    public void testSeekTo() throws InterruptedException {
        final long position = 10;
        testOnCommandRequest(COMMAND_CODE_PLAYBACK_SEEK_TO, new PermissionTestRunnable() {
            @Override
            public void run(MediaController2 controller) {
                controller.seekTo(position);
            }
        });
    }

    @Test
    public void testSkipToNext() throws InterruptedException {
        testOnCommandRequest(COMMAND_CODE_PLAYLIST_SKIP_NEXT_ITEM, new PermissionTestRunnable() {
            @Override
            public void run(MediaController2 controller) {
                controller.skipToNextItem();
            }
        });
    }

    @Test
    public void testSkipToPrevious() throws InterruptedException {
        testOnCommandRequest(COMMAND_CODE_PLAYLIST_SKIP_PREV_ITEM, new PermissionTestRunnable() {
            @Override
            public void run(MediaController2 controller) {
                controller.skipToPreviousItem();
            }
        });
    }

    @Test
    public void testSkipToPlaylistItem() throws InterruptedException {
        final MediaItem2 testItem = TestUtils.createMediaItemWithMetadata();
        testOnCommandRequest(
                COMMAND_CODE_PLAYLIST_SKIP_TO_PLAYLIST_ITEM,
                new PermissionTestRunnable() {
                    @Override
                    public void run(MediaController2 controller) {
                        controller.skipToPlaylistItem(testItem);
                    }
                });
    }

    @Test
    public void testSetPlaylist() throws InterruptedException {
        final List<MediaItem2> list = TestUtils.createPlaylist(2);
        testOnCommandRequest(COMMAND_CODE_PLAYLIST_SET_LIST, new PermissionTestRunnable() {
            @Override
            public void run(MediaController2 controller) {
                controller.setPlaylist(list, null);
            }
        });
    }

    @Test
    public void testUpdatePlaylistMetadata() throws InterruptedException {
        testOnCommandRequest(COMMAND_CODE_PLAYLIST_SET_LIST_METADATA, new PermissionTestRunnable() {
            @Override
            public void run(MediaController2 controller) {
                controller.updatePlaylistMetadata(null);
            }
        });
    }

    @Test
    public void testAddPlaylistItem() throws InterruptedException {
        final MediaItem2 testItem = TestUtils.createMediaItemWithMetadata();
        testOnCommandRequest(COMMAND_CODE_PLAYLIST_ADD_ITEM, new PermissionTestRunnable() {
            @Override
            public void run(MediaController2 controller) {
                controller.addPlaylistItem(0, testItem);
            }
        });
    }

    @Test
    public void testRemovePlaylistItem() throws InterruptedException {
        final MediaItem2 testItem = TestUtils.createMediaItemWithMetadata();
        testOnCommandRequest(COMMAND_CODE_PLAYLIST_REMOVE_ITEM, new PermissionTestRunnable() {
            @Override
            public void run(MediaController2 controller) {
                controller.removePlaylistItem(testItem);
            }
        });
    }

    @Test
    public void testReplacePlaylistItem() throws InterruptedException {
        final MediaItem2 testItem = TestUtils.createMediaItemWithMetadata();
        testOnCommandRequest(COMMAND_CODE_PLAYLIST_REPLACE_ITEM, new PermissionTestRunnable() {
            @Override
            public void run(MediaController2 controller) {
                controller.replacePlaylistItem(0, testItem);
            }
        });
    }

    @Test
    public void testSetVolume() throws InterruptedException {
        testOnCommandRequest(COMMAND_CODE_SET_VOLUME, new PermissionTestRunnable() {
            @Override
            public void run(MediaController2 controller) {
                controller.setVolumeTo(0, 0);
            }
        });
    }

    @Test
    public void testPlayFromMediaId() throws InterruptedException {
        final String mediaId = "testPlayFromMediaId";
        createSessionWithAllowedActions(
                createCommandGroupWith(COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID));
        createController(mSession.getToken()).playFromMediaId(mediaId, null);

        assertTrue(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mCallback.mOnPlayFromMediaIdCalled);
        assertEquals(mediaId, mCallback.mMediaId);
        assertNull(mCallback.mExtras);

        createSessionWithAllowedActions(
                createCommandGroupWithout(COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID));
        createController(mSession.getToken()).playFromMediaId(mediaId, null);
        assertFalse(mCallback.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertFalse(mCallback.mOnPlayFromMediaIdCalled);
    }

    @Test
    public void testPlayFromUri() throws InterruptedException {
        final Uri uri = Uri.parse("play://from.uri");
        createSessionWithAllowedActions(
                createCommandGroupWith(COMMAND_CODE_SESSION_PLAY_FROM_URI));
        createController(mSession.getToken()).playFromUri(uri, null);

        assertTrue(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mCallback.mOnPlayFromUriCalled);
        assertEquals(uri, mCallback.mUri);
        assertNull(mCallback.mExtras);

        createSessionWithAllowedActions(
                createCommandGroupWithout(COMMAND_CODE_SESSION_PLAY_FROM_URI));
        createController(mSession.getToken()).playFromUri(uri, null);
        assertFalse(mCallback.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertFalse(mCallback.mOnPlayFromUriCalled);
    }

    @Test
    public void testPlayFromSearch() throws InterruptedException {
        final String query = "testPlayFromSearch";
        createSessionWithAllowedActions(
                createCommandGroupWith(COMMAND_CODE_SESSION_PLAY_FROM_SEARCH));
        createController(mSession.getToken()).playFromSearch(query, null);

        assertTrue(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mCallback.mOnPlayFromSearchCalled);
        assertEquals(query, mCallback.mQuery);
        assertNull(mCallback.mExtras);

        createSessionWithAllowedActions(
                createCommandGroupWithout(COMMAND_CODE_SESSION_PLAY_FROM_SEARCH));
        createController(mSession.getToken()).playFromSearch(query, null);
        assertFalse(mCallback.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertFalse(mCallback.mOnPlayFromSearchCalled);
    }

    @Test
    public void testPrepareFromMediaId() throws InterruptedException {
        final String mediaId = "testPrepareFromMediaId";
        createSessionWithAllowedActions(
                createCommandGroupWith(COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID));
        createController(mSession.getToken()).prepareFromMediaId(mediaId, null);

        assertTrue(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mCallback.mOnPrepareFromMediaIdCalled);
        assertEquals(mediaId, mCallback.mMediaId);
        assertNull(mCallback.mExtras);

        createSessionWithAllowedActions(
                createCommandGroupWithout(COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID));
        createController(mSession.getToken()).prepareFromMediaId(mediaId, null);
        assertFalse(mCallback.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertFalse(mCallback.mOnPrepareFromMediaIdCalled);
    }

    @Test
    public void testPrepareFromUri() throws InterruptedException {
        final Uri uri = Uri.parse("prepare://from.uri");
        createSessionWithAllowedActions(
                createCommandGroupWith(COMMAND_CODE_SESSION_PREPARE_FROM_URI));
        createController(mSession.getToken()).prepareFromUri(uri, null);

        assertTrue(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mCallback.mOnPrepareFromUriCalled);
        assertEquals(uri, mCallback.mUri);
        assertNull(mCallback.mExtras);

        createSessionWithAllowedActions(
                createCommandGroupWithout(COMMAND_CODE_SESSION_PREPARE_FROM_URI));
        createController(mSession.getToken()).prepareFromUri(uri, null);
        assertFalse(mCallback.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertFalse(mCallback.mOnPrepareFromUriCalled);
    }

    @Test
    public void testPrepareFromSearch() throws InterruptedException {
        final String query = "testPrepareFromSearch";
        createSessionWithAllowedActions(
                createCommandGroupWith(COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH));
        createController(mSession.getToken()).prepareFromSearch(query, null);

        assertTrue(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mCallback.mOnPrepareFromSearchCalled);
        assertEquals(query, mCallback.mQuery);
        assertNull(mCallback.mExtras);

        createSessionWithAllowedActions(
                createCommandGroupWithout(COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH));
        createController(mSession.getToken()).prepareFromSearch(query, null);
        assertFalse(mCallback.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertFalse(mCallback.mOnPrepareFromSearchCalled);
    }

    @Test
    public void testChangingPermissionWithSetAllowedCommands() throws InterruptedException {
        final String query = "testChangingPermissionWithSetAllowedCommands";
        createSessionWithAllowedActions(
                createCommandGroupWith(COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH));

        ControllerCallbackForPermissionChange controllerCallback =
                new ControllerCallbackForPermissionChange();
        MediaController2 controller =
                createController(mSession.getToken(), true, controllerCallback);

        controller.prepareFromSearch(query, null);
        assertTrue(mCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mCallback.mOnPrepareFromSearchCalled);
        assertEquals(query, mCallback.mQuery);
        assertNull(mCallback.mExtras);
        mCallback.reset();

        // Change allowed commands.
        mSession.setAllowedCommands(getTestControllerInfo(),
                createCommandGroupWithout(COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH));
        assertTrue(controllerCallback.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        controller.prepareFromSearch(query, null);
        assertFalse(mCallback.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    private ControllerInfo getTestControllerInfo() {
        List<ControllerInfo> controllers = mSession.getConnectedControllers();
        assertNotNull(controllers);
        for (int i = 0; i < controllers.size(); i++) {
            if (Process.myUid() == controllers.get(i).getUid()) {
                return controllers.get(i);
            }
        }
        fail("Failed to get test controller info");
        return null;
    }

    @FunctionalInterface
    private interface PermissionTestRunnable {
        void run(@NonNull MediaController2 controller);
    }

    public class MySessionCallback extends MediaSession2.SessionCallback {
        public CountDownLatch mCountDownLatch;

        public SessionCommand2 mCommand;
        public String mMediaId;
        public String mQuery;
        public Uri mUri;
        public Bundle mExtras;

        public boolean mOnCommandRequestCalled;
        public boolean mOnPlayFromMediaIdCalled;
        public boolean mOnPlayFromSearchCalled;
        public boolean mOnPlayFromUriCalled;
        public boolean mOnPrepareFromMediaIdCalled;
        public boolean mOnPrepareFromSearchCalled;
        public boolean mOnPrepareFromUriCalled;


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
        }

        @Override
        public boolean onCommandRequest(MediaSession2 session, ControllerInfo controller,
                SessionCommand2 command) {
            assertEquals(Process.myUid(), controller.getUid());
            mOnCommandRequestCalled = true;
            mCommand = command;
            mCountDownLatch.countDown();
            return super.onCommandRequest(session, controller, command);
        }

        @Override
        public void onPlayFromMediaId(MediaSession2 session, ControllerInfo controller,
                String mediaId, Bundle extras) {
            assertEquals(Process.myUid(), controller.getUid());
            mOnPlayFromMediaIdCalled = true;
            mMediaId = mediaId;
            mExtras = extras;
            mCountDownLatch.countDown();
        }

        @Override
        public void onPlayFromSearch(MediaSession2 session, ControllerInfo controller,
                String query, Bundle extras) {
            assertEquals(Process.myUid(), controller.getUid());
            mOnPlayFromSearchCalled = true;
            mQuery = query;
            mExtras = extras;
            mCountDownLatch.countDown();
        }

        @Override
        public void onPlayFromUri(MediaSession2 session, ControllerInfo controller,
                Uri uri, Bundle extras) {
            assertEquals(Process.myUid(), controller.getUid());
            mOnPlayFromUriCalled = true;
            mUri = uri;
            mExtras = extras;
            mCountDownLatch.countDown();
        }

        @Override
        public void onPrepareFromMediaId(MediaSession2 session, ControllerInfo controller,
                String mediaId, Bundle extras) {
            assertEquals(Process.myUid(), controller.getUid());
            mOnPrepareFromMediaIdCalled = true;
            mMediaId = mediaId;
            mExtras = extras;
            mCountDownLatch.countDown();
        }

        @Override
        public void onPrepareFromSearch(MediaSession2 session, ControllerInfo controller,
                String query, Bundle extras) {
            assertEquals(Process.myUid(), controller.getUid());
            mOnPrepareFromSearchCalled = true;
            mQuery = query;
            mExtras = extras;
            mCountDownLatch.countDown();
        }

        @Override
        public void onPrepareFromUri(MediaSession2 session, ControllerInfo controller,
                Uri uri, Bundle extras) {
            assertEquals(Process.myUid(), controller.getUid());
            mOnPrepareFromUriCalled = true;
            mUri = uri;
            mExtras = extras;
            mCountDownLatch.countDown();
        }

        // TODO(jaewan): Add permission test for setRating()
    }

    public class ControllerCallbackForPermissionChange extends MediaController2.ControllerCallback {
        public CountDownLatch mCountDownLatch = new CountDownLatch(1);

        @Override
        public void onAllowedCommandsChanged(MediaController2 controller,
                SessionCommandGroup2 commands) {
            mCountDownLatch.countDown();
        }
    }
}
