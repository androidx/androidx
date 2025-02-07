/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.scenecore.impl.perception;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.app.Activity;
import android.os.IBinder;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.xr.scenecore.impl.perception.exceptions.FailedToInitializeException;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/** This tests the Java/JNI interface of the perception library, with a fake native library. */
@LargeTest
@RunWith(AndroidJUnit4.class)
public final class PerceptionLibraryTest {
    @Rule
    public final ActivityScenarioRule<Activity> activityScenarioRule =
            new ActivityScenarioRule<>(Activity.class);

    IBinder mSharedAnchorToken = Mockito.mock(IBinder.class);

    private static final String TEST_NATIVE_LIBRARY_NAME = "fake_perception_library_jni";

    @PerceptionLibraryConstants.OpenXrSpaceType
    private static final int OPEN_XR_REFERENCE_SPACE_TYPE = 1;

    Activity mActivity;
    PerceptionLibrary mPerceptionLibrary = new PerceptionLibrary();
    FakeScheduledExecutorService mExecutor = new FakeScheduledExecutorService();
    PerceptionLibraryTestHelper mTestHelper = new PerceptionLibraryTestHelper();

    @Before
    public void setUp() {
        PerceptionLibrary.loadLibraryAsync(TEST_NATIVE_LIBRARY_NAME);
        activityScenarioRule.getScenario().onActivity(activity -> mActivity = activity);
    }

    @After
    public void tearDown() {
        mTestHelper.reset();
    }

    static byte[] uuidToByteArray(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    @Test
    public void initSession_success() throws Exception {
        Session session = mPerceptionLibrary.getSession();
        assertThat(session).isNull();

        mTestHelper.setCreateSessionResult(true);
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        session = sessionFuture.get();

        assertThat(session).isNotNull();
        assertThat(session).isEqualTo(mPerceptionLibrary.getSession());
        assertThat(mTestHelper.getOpenXrSessionReferenceSpaceType())
                .isEqualTo(OPEN_XR_REFERENCE_SPACE_TYPE);
    }

    @Test
    public void initSession_alreadyExists() throws Exception {
        mTestHelper.setCreateSessionResult(true);
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();
        assertThat(session).isNotNull();

        ListenableFuture<Session> session2Future =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Exception initException = assertThrows(ExecutionException.class, session2Future::get);
        assertThat(initException).hasCauseThat().isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void initSession_failure() throws Exception {
        mTestHelper.setCreateSessionResult(false);
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();

        Exception initException = assertThrows(ExecutionException.class, sessionFuture::get);
        assertThat(initException).hasCauseThat().isInstanceOf(FailedToInitializeException.class);
    }

    @Test
    public void createAnchor_success() throws Exception {
        long anchorId = 888;
        mTestHelper.setGetAnchorResult(mSharedAnchorToken, anchorId);
        mTestHelper.setCreateSessionResult(true);
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();

        Anchor anchor = session.createAnchor(0, 0, Plane.Type.ARBITRARY, Plane.Label.UNKNOWN);
        assertThat(anchor.getAnchorToken()).isEqualTo(mSharedAnchorToken);
    }

    @Test
    public void createAnchor_failureReturnsNull() throws Exception {
        mTestHelper.setGetAnchorResult(null, 0);
        mTestHelper.setCreateSessionResult(true);
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();
        assertThat(session.createAnchor(0, 0, Plane.Type.ARBITRARY, Plane.Label.UNKNOWN)).isNull();
    }

    @Test
    public void detachAnchor_success() throws Exception {
        long anchorId = 888;
        mTestHelper.setGetAnchorResult(mSharedAnchorToken, anchorId);
        mTestHelper.setCreateSessionResult(true);
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();

        Anchor anchor = session.createAnchor(0, 0, Plane.Type.ARBITRARY, Plane.Label.UNKNOWN);
        assertThat(anchor.detach()).isTrue();
    }

    @Test
    public void detachAnchor_failureReturnsNull() throws Exception {
        long anchorId = 888;
        mTestHelper.setGetAnchorResult(mSharedAnchorToken, anchorId);
        mTestHelper.setCreateSessionResult(true);
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();

        Anchor anchor = session.createAnchor(0, 0, Plane.Type.ARBITRARY, Plane.Label.UNKNOWN);

        // Reset the anchor data so that there is an error
        mTestHelper.setGetAnchorResult(null, 0);
        assertThat(anchor.detach()).isFalse();
    }

    @Test
    public void persistAnchor_failureReturnsNull() throws Exception {
        long anchorId = 27;
        mTestHelper.setGetAnchorResult(mSharedAnchorToken, anchorId);
        mTestHelper.setCreateSessionResult(true);
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();

        Anchor anchor = session.createAnchor(0, 0, Plane.Type.ARBITRARY, Plane.Label.UNKNOWN);
        assertThat(anchor.persist()).isNull();
    }

    @Test
    public void persistAnchor_success() throws Exception {
        long anchorId = 34;
        mTestHelper.setGetAnchorResult(mSharedAnchorToken, anchorId);
        mTestHelper.setCreateSessionResult(true);

        UUID uuid = UUID.randomUUID();
        mTestHelper.setAnchorUuidBytes(uuidToByteArray(uuid));
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();

        Anchor anchor = session.createAnchor(0, 0, Plane.Type.ARBITRARY, Plane.Label.UNKNOWN);
        UUID returnedUuid = anchor.persist();
        assertThat(returnedUuid).isNotNull();
        assertThat(uuid).isEquivalentAccordingToCompareTo(returnedUuid);
    }

    @Test
    public void anchor_getPersistState_successReturnsPersisted() throws Exception {
        long anchorId = 34;
        mTestHelper.setGetAnchorResult(mSharedAnchorToken, anchorId);
        mTestHelper.setCreateSessionResult(true);
        mTestHelper.setAnchorPersistState(2);
        UUID uuid = UUID.randomUUID();
        mTestHelper.setAnchorUuidBytes(uuidToByteArray(uuid));
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();

        Anchor anchor = session.createAnchor(0, 0, Plane.Type.ARBITRARY, Plane.Label.UNKNOWN);
        UUID returnedUuid = anchor.persist();
        assertThat(returnedUuid).isNotNull();
        assertThat(uuid).isEquivalentAccordingToCompareTo(returnedUuid);
        Anchor.PersistState state = anchor.getPersistState();
        assertThat(state).isEqualTo(Anchor.PersistState.PERSISTED);
    }

    @Test
    public void anchor_getPersistState_successReturnsPending() throws Exception {
        long anchorId = 34;
        mTestHelper.setGetAnchorResult(mSharedAnchorToken, anchorId);
        mTestHelper.setCreateSessionResult(true);
        mTestHelper.setAnchorPersistState(1);
        UUID uuid = UUID.randomUUID();
        mTestHelper.setAnchorUuidBytes(uuidToByteArray(uuid));
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();

        Anchor anchor = session.createAnchor(0, 0, Plane.Type.ARBITRARY, Plane.Label.UNKNOWN);
        UUID returnedUuid = anchor.persist();
        assertThat(returnedUuid).isNotNull();
        assertThat(uuid).isEquivalentAccordingToCompareTo(returnedUuid);
        Anchor.PersistState state = anchor.getPersistState();
        assertThat(state).isEqualTo(Anchor.PersistState.PERSIST_PENDING);
    }

    @Test
    public void anchor_getPersistState_successReturnsPersistNotRequested() throws Exception {
        long anchorId = 34;
        mTestHelper.setGetAnchorResult(mSharedAnchorToken, anchorId);
        mTestHelper.setCreateSessionResult(true);
        mTestHelper.setAnchorPersistState(0);
        UUID uuid = UUID.randomUUID();
        mTestHelper.setAnchorUuidBytes(uuidToByteArray(uuid));
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();

        Anchor anchor = session.createAnchor(0, 0, Plane.Type.ARBITRARY, Plane.Label.UNKNOWN);
        UUID returnedUuid = anchor.persist();
        assertThat(returnedUuid).isNotNull();
        assertThat(uuid).isEquivalentAccordingToCompareTo(returnedUuid);
        Anchor.PersistState state = anchor.getPersistState();
        assertThat(state).isEqualTo(Anchor.PersistState.PERSIST_NOT_REQUESTED);
    }

    @Test
    public void anchor_getPersistState_failsReturnsPersistNotRequested() throws Exception {
        long anchorId = 34;
        mTestHelper.setGetAnchorResult(mSharedAnchorToken, anchorId);
        mTestHelper.setCreateSessionResult(true);
        mTestHelper.setAnchorPersistState(2);
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();

        Anchor anchor = session.createAnchor(0, 0, Plane.Type.ARBITRARY, Plane.Label.UNKNOWN);
        Anchor.PersistState state = anchor.getPersistState();
        assertThat(state).isEqualTo(Anchor.PersistState.PERSIST_NOT_REQUESTED);
    }

    @Test
    public void anchor_getPersistState_failsReturnsNotValid() throws Exception {
        long anchorId = 34;
        mTestHelper.setGetAnchorResult(mSharedAnchorToken, anchorId);
        mTestHelper.setCreateSessionResult(true);
        UUID setUuid = new UUID((long) 1, (long) 2);
        mTestHelper.setAnchorUuidBytes(uuidToByteArray(setUuid));
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();

        Anchor anchor = session.createAnchor(0, 0, Plane.Type.ARBITRARY, Plane.Label.UNKNOWN);
        UUID returnedUuid = anchor.persist();
        assertThat(returnedUuid).isNotNull();
        assertThat(setUuid).isEquivalentAccordingToCompareTo(returnedUuid);

        UUID searchUuid = new UUID((long) 3, (long) 4);
        mTestHelper.setAnchorUuidBytes(uuidToByteArray(searchUuid));
        Anchor.PersistState state = anchor.getPersistState();
        assertThat(state).isEqualTo(Anchor.PersistState.NOT_VALID);
    }

    @Test
    public void session_unpersistAnchor_success() throws Exception {
        long anchorId = 34;
        mTestHelper.setGetAnchorResult(mSharedAnchorToken, anchorId);
        mTestHelper.setCreateSessionResult(true);

        UUID uuid = UUID.randomUUID();
        mTestHelper.setAnchorUuidBytes(uuidToByteArray(uuid));
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();

        Anchor anchor = session.createAnchor(0, 0, Plane.Type.ARBITRARY, Plane.Label.UNKNOWN);
        UUID returnedUuid = anchor.persist();
        assertThat(returnedUuid).isNotNull();
        assertThat(uuid).isEquivalentAccordingToCompareTo(returnedUuid);

        assertThat(session.unpersistAnchor(returnedUuid)).isTrue();
    }

    @Test
    public void session_unpersistAnchor_failureReturnsFalse_wrongUuid() throws Exception {
        mTestHelper.setCreateSessionResult(true);

        UUID setUuid = new UUID((long) 1, (long) 2);
        mTestHelper.setAnchorUuidBytes(uuidToByteArray(setUuid));
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();

        UUID searchUuid = new UUID((long) 3, (long) 4);
        assertThat(session.unpersistAnchor(searchUuid)).isFalse();
    }

    @Test
    public void session_unpersistAnchor_failureReturnsFalse_nullUuid() throws Exception {
        mTestHelper.setCreateSessionResult(true);

        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();

        assertThat(session.unpersistAnchor(null)).isFalse();
    }

    @Test
    public void session_createPersistedAnchor_success() throws Exception {
        long anchorId = 34;
        mTestHelper.setGetAnchorResult(mSharedAnchorToken, anchorId);
        mTestHelper.setCreateSessionResult(true);

        UUID uuid = UUID.randomUUID();
        mTestHelper.setAnchorUuidBytes(uuidToByteArray(uuid));
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();

        Anchor anchor = session.createAnchorFromUuid(uuid);
        assertThat(anchor).isNotNull();
        assertThat(anchor.getAnchorToken()).isEqualTo(mSharedAnchorToken);
    }

    @Test
    public void session_createPersistedAnchor_failsReturnsNull_forNullUuid() throws Exception {
        long anchorId = 34;
        mTestHelper.setGetAnchorResult(mSharedAnchorToken, anchorId);
        mTestHelper.setCreateSessionResult(true);

        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();

        Anchor anchor = session.createAnchorFromUuid(null);
        assertThat(anchor).isNull();
    }

    @Test
    public void session_createPersistedAnchor_failsReturnsNull_forWrongUuid() throws Exception {
        long anchorId = 34;
        mTestHelper.setGetAnchorResult(mSharedAnchorToken, anchorId);
        mTestHelper.setCreateSessionResult(true);

        UUID setUuid = new UUID((long) 1, (long) 2);
        mTestHelper.setAnchorUuidBytes(uuidToByteArray(setUuid));
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();

        UUID searchUuid = new UUID((long) 3, (long) 4);
        Anchor anchor = session.createAnchorFromUuid(searchUuid);
        assertThat(anchor).isNull();
    }

    @Test
    public void getHeadPose_success() throws Exception {
        Pose result = new Pose(1, 2, 3, 4, 5, 6, 7);
        mTestHelper.setGetCurrentHeadPoseResult(1, 2, 3, 4, 5, 6, 7);
        mTestHelper.setCreateSessionResult(true);
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();

        assertThat(session.getHeadPose()).isEqualTo(result);
    }

    @Test
    public void getHeadPose_failureReturnsNull() throws Exception {
        mTestHelper.setCreateSessionResult(true);
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();

        assertThat(session.getHeadPose()).isNull();
    }

    @Test
    public void getStereoViews_success() throws Exception {
        mTestHelper.setCreateSessionResult(true);
        mTestHelper.setLeftView(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
        mTestHelper.setRightView(12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22);
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();

        assertThat(session.getStereoViews())
                .isEqualTo(
                        new ViewProjections(
                                new ViewProjection(
                                        new Pose(1, 2, 3, 4, 5, 6, 7), new Fov(8, 9, 10, 11)),
                                new ViewProjection(
                                        new Pose(12, 13, 14, 15, 16, 17, 18),
                                        new Fov(19, 20, 21, 22))));
    }

    @Test
    public void getStereoViews_failureReturnsNull() throws Exception {
        mTestHelper.setCreateSessionResult(true);
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();

        assertThat(session.getStereoViews()).isNull();
    }

    @Test
    public void getPlanes_withPlanes_returnsPlanes() throws Exception {
        mTestHelper.setCreateSessionResult(true);
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();
        mTestHelper.addPlane(
                1L,
                Pose.identity(),
                0f,
                0f,
                Plane.Type.ARBITRARY.intValue,
                Plane.Label.UNKNOWN.intValue);
        mTestHelper.addPlane(
                2L,
                Pose.identity(),
                0f,
                0f,
                Plane.Type.ARBITRARY.intValue,
                Plane.Label.UNKNOWN.intValue);

        assertThat(session.getAllPlanes()).hasSize(2);
    }

    @Test
    public void getPlanes_withNoPlanes_returnsEmptyList() throws Exception {
        mTestHelper.setCreateSessionResult(true);
        ListenableFuture<Session> sessionFuture =
                mPerceptionLibrary.initSession(mActivity, OPEN_XR_REFERENCE_SPACE_TYPE, mExecutor);
        mExecutor.runNext();
        Session session = sessionFuture.get();

        assertThat(session.getAllPlanes()).isEmpty();
    }
}
