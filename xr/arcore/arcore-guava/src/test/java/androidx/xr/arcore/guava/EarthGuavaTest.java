/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.arcore.guava;

import static androidx.xr.arcore.guava.GuavaEarth.createAnchorOnSurfaceAsync;

import static com.google.common.truth.Truth.assertThat;

import static kotlinx.coroutines.test.TestCoroutineDispatchersKt.StandardTestDispatcher;

import static org.junit.Assert.fail;

import androidx.activity.ComponentActivity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.xr.arcore.Anchor;
import androidx.xr.arcore.AnchorCreateIllegalState;
import androidx.xr.arcore.AnchorCreateNotAuthorized;
import androidx.xr.arcore.AnchorCreateResourcesExhausted;
import androidx.xr.arcore.AnchorCreateResult;
import androidx.xr.arcore.AnchorCreateSuccess;
import androidx.xr.arcore.AnchorCreateUnsupportedLocation;
import androidx.xr.arcore.Earth;
import androidx.xr.arcore.XrResourcesManager;
import androidx.xr.runtime.Session;
import androidx.xr.runtime.SessionCreateSuccess;
import androidx.xr.runtime.internal.AnchorNotAuthorizedException;
import androidx.xr.runtime.internal.AnchorResourcesExhaustedException;
import androidx.xr.runtime.internal.AnchorUnsupportedLocationException;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.testing.FakePerceptionManager;
import androidx.xr.runtime.testing.FakeRuntimeEarth;

import com.google.common.util.concurrent.ListenableFuture;

import kotlinx.coroutines.test.TestDispatcher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
public class EarthGuavaTest {
    private static final double LATITUDE = 10.0;
    private static final double LONGITUDE = 20.0;
    private static final double ALTITUDE_ABOVE_SURFACE = 5.0;
    private static final Quaternion EUS_QUATERNION = Quaternion.Identity;
    private Session mSession;
    private TestDispatcher mTestDispatcher;
    private XrResourcesManager mXrResourcesManager;
    private FakeRuntimeEarth mRuntimeEarth;

    @Before
    public void setUp() {
        mXrResourcesManager = new XrResourcesManager();
        mRuntimeEarth = new FakeRuntimeEarth(androidx.xr.runtime.internal.Earth.State.STOPPED);
    }

    @Test
    public void createAnchorOnSurface_success_returnsSuccessResultWithAnchor() {
        createTestSessionAndRunTest(() -> {
            Earth underTest = new Earth(mRuntimeEarth, mXrResourcesManager);
            FakePerceptionManager fakePerceptionManager =
                    (FakePerceptionManager) mSession.getRuntime().getPerceptionManager();
            androidx.xr.runtime.internal.Anchor fakeAnchor =
                    fakePerceptionManager.createAnchor(Pose.Identity);
            mRuntimeEarth.setNextAnchor(fakeAnchor);

            ListenableFuture<AnchorCreateResult> resultFuture =
                    createAnchorOnSurfaceAsync(underTest, mSession,
                            LATITUDE,
                            LONGITUDE,
                            ALTITUDE_ABOVE_SURFACE,
                            EUS_QUATERNION,
                            Earth.Surface.TERRAIN);
            mTestDispatcher.getScheduler().advanceUntilIdle();
            try {
                AnchorCreateResult result = resultFuture.get();
                assertThat(result).isInstanceOf(AnchorCreateSuccess.class);

                AnchorCreateSuccess successResult = (AnchorCreateSuccess) result;
                assertThat(successResult.getAnchor().getRuntimeAnchor()).isEqualTo(fakeAnchor);

                Anchor firstAnchor = (Anchor) mXrResourcesManager.getUpdatables().get(0);
                assertThat(firstAnchor.getRuntimeAnchor()).isEqualTo(fakeAnchor);
            } catch (Exception e) {
                throw new RuntimeException();
            }
        });
    }

    @Test
    public void createAnchorOnSurfaceAsync_illegalState_returnsIllegalStateResult() {
        createTestSessionAndRunTest(() -> {
            Earth underTest = new Earth(mRuntimeEarth, mXrResourcesManager);

            ListenableFuture<AnchorCreateResult> resultFuture =
                    createAnchorOnSurfaceAsync(underTest, mSession,
                            LATITUDE,
                            LONGITUDE,
                            ALTITUDE_ABOVE_SURFACE,
                            EUS_QUATERNION,
                            Earth.Surface.TERRAIN);
            mTestDispatcher.getScheduler().advanceUntilIdle();
            try {
                AnchorCreateResult result = resultFuture.get();

                assertThat(result).isInstanceOf(AnchorCreateIllegalState.class);
            } catch (Exception e) {
                throw new RuntimeException();
            }
        });
    }

    @Test
    public void createAnchorOnSurfaceAsync_resourceExhausted_returnsResourcesExhaustedResult() {
        createTestSessionAndRunTest(() -> {
            Earth underTest = new Earth(mRuntimeEarth, mXrResourcesManager);
            mRuntimeEarth.setNextException(new AnchorResourcesExhaustedException());

            ListenableFuture<AnchorCreateResult> resultFuture =
                    createAnchorOnSurfaceAsync(underTest, mSession,
                            LATITUDE,
                            LONGITUDE,
                            ALTITUDE_ABOVE_SURFACE,
                            EUS_QUATERNION,
                            Earth.Surface.TERRAIN);
            mTestDispatcher.getScheduler().advanceUntilIdle();
            try {
                AnchorCreateResult result = resultFuture.get();

                assertThat(result).isInstanceOf(AnchorCreateResourcesExhausted.class);
            } catch (Exception e) {
                throw new RuntimeException();
            }
        });
    }

    @Test
    public void createAnchorOnSurfaceAsync_notAuthorized_returnsNotAuthorizedResult() {
        createTestSessionAndRunTest(() -> {
            Earth underTest = new Earth(mRuntimeEarth, mXrResourcesManager);
            mRuntimeEarth.setNextException(new AnchorNotAuthorizedException());

            ListenableFuture<AnchorCreateResult> resultFuture =
                    createAnchorOnSurfaceAsync(underTest, mSession,
                            LATITUDE,
                            LONGITUDE,
                            ALTITUDE_ABOVE_SURFACE,
                            EUS_QUATERNION,
                            Earth.Surface.TERRAIN);
            mTestDispatcher.getScheduler().advanceUntilIdle();
            try {
                AnchorCreateResult result = resultFuture.get();

                assertThat(result).isInstanceOf(AnchorCreateNotAuthorized.class);
            } catch (Exception e) {
                throw new RuntimeException();
            }
        });
    }

    @Test
    public void createAnchorOnSurfaceAsync_unsupportedLocation_returnsUnsupportedLocationResult() {
        createTestSessionAndRunTest(() -> {
            Earth underTest = new Earth(mRuntimeEarth, mXrResourcesManager);
            mRuntimeEarth.setNextException(new AnchorUnsupportedLocationException());

            ListenableFuture<AnchorCreateResult> resultFuture =
                    createAnchorOnSurfaceAsync(underTest, mSession,
                            LATITUDE,
                            LONGITUDE,
                            ALTITUDE_ABOVE_SURFACE,
                            EUS_QUATERNION,
                            Earth.Surface.TERRAIN);
            mTestDispatcher.getScheduler().advanceUntilIdle();
            try {
                AnchorCreateResult result = resultFuture.get();

                assertThat(result).isInstanceOf(AnchorCreateUnsupportedLocation.class);
            } catch (Exception e) {
                throw new RuntimeException();
            }
        });
    }

    @Test
    public void createAnchorOnSurfaceAsync_invalidLatitude_throwsIllegalArgumentException() {
        createTestSessionAndRunTest(() -> {
            Earth underTest = new Earth(mRuntimeEarth, mXrResourcesManager);
            mRuntimeEarth.setNextException(
                    new IllegalArgumentException("Invalid latitude provided."));

            ExecutionException outerException = null;
            try {
                ListenableFuture<AnchorCreateResult> resultFuture = createAnchorOnSurfaceAsync(
                        underTest, mSession, 90.0, LONGITUDE,
                        ALTITUDE_ABOVE_SURFACE,
                        EUS_QUATERNION,
                        Earth.Surface.TERRAIN);
                mTestDispatcher.getScheduler().advanceUntilIdle();
                resultFuture.get();
                fail("Invalid latitude provided.");
            } catch (ExecutionException e) {
                outerException = e;
            } catch (Exception e) {
                throw new RuntimeException();
            }

            assertThat(outerException).isNotNull();
            assertThat(outerException.getCause())
                    .isInstanceOf(IllegalArgumentException.class);
        });
    }

    private void createTestSessionAndRunTest(Runnable testBody) {
        try (ActivityScenario<ComponentActivity> scenario = ActivityScenario.launch(
                ComponentActivity.class)) {
            scenario.onActivity(activity -> {
                mTestDispatcher = StandardTestDispatcher(/* scheduler= */ null, /* name= */ null);
                mSession = ((SessionCreateSuccess) Session.create(activity,
                        mTestDispatcher)).getSession();

                try {
                    testBody.run();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}

