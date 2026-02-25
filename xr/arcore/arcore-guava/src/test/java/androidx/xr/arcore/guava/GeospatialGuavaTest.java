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

import static androidx.xr.arcore.guava.GuavaGeospatial.checkVpsAvailabilityAsync;
import static androidx.xr.arcore.guava.GuavaGeospatial.createAnchorOnSurfaceAsync;

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
import androidx.xr.arcore.Geospatial;
import androidx.xr.arcore.SessionExtKt;
import androidx.xr.arcore.XrResourcesManager;
import androidx.xr.arcore.runtime.AnchorNotAuthorizedException;
import androidx.xr.arcore.runtime.AnchorResourcesExhaustedException;
import androidx.xr.arcore.runtime.AnchorUnsupportedLocationException;
import androidx.xr.arcore.testing.FakePerceptionManager;
import androidx.xr.arcore.testing.FakeRuntimeGeospatial;
import androidx.xr.runtime.AnchorPersistenceMode;
import androidx.xr.runtime.CameraFacingDirection;
import androidx.xr.runtime.Config;
import androidx.xr.runtime.DepthEstimationMode;
import androidx.xr.runtime.DeviceTrackingMode;
import androidx.xr.runtime.EyeTrackingMode;
import androidx.xr.runtime.FaceTrackingMode;
import androidx.xr.runtime.GeospatialMode;
import androidx.xr.runtime.HandTrackingMode;
import androidx.xr.runtime.PlaneTrackingMode;
import androidx.xr.runtime.Session;
import androidx.xr.runtime.SessionCreateSuccess;
import androidx.xr.runtime.VpsAvailabilityAvailable;
import androidx.xr.runtime.VpsAvailabilityResult;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;

import com.google.common.util.concurrent.ListenableFuture;

import kotlinx.coroutines.test.TestDispatcher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
public class GeospatialGuavaTest {
    private static final double LATITUDE = 10.0;
    private static final double LONGITUDE = 20.0;
    private static final double ALTITUDE_ABOVE_SURFACE = 5.0;
    private static final Quaternion EUS_QUATERNION = Quaternion.Identity;
    private Session mSession;
    private TestDispatcher mTestDispatcher;
    private XrResourcesManager mXrResourcesManager;
    private FakeRuntimeGeospatial mRuntimeGeospatial;

    @Before
    public void setUp() {
        mXrResourcesManager = new XrResourcesManager();
        mRuntimeGeospatial =
                new FakeRuntimeGeospatial(androidx.xr.arcore.runtime.Geospatial.State.NOT_RUNNING);
    }

    @Test
    public void createAnchorOnSurface_success_returnsSuccessResultWithAnchor() {
        createTestSessionAndRunTest(
                () -> {
                    Geospatial underTest = new Geospatial(mRuntimeGeospatial, mXrResourcesManager);
                    FakePerceptionManager fakePerceptionManager = getFakePerceptionManager();
                    androidx.xr.arcore.runtime.Anchor fakeAnchor =
                            fakePerceptionManager.createAnchor(Pose.Identity);
                    mRuntimeGeospatial.setNextAnchor(fakeAnchor);

                    ListenableFuture<AnchorCreateResult> resultFuture =
                            createAnchorOnSurfaceAsync(
                                    underTest,
                                    mSession,
                                    LATITUDE,
                                    LONGITUDE,
                                    ALTITUDE_ABOVE_SURFACE,
                                    EUS_QUATERNION,
                                    Geospatial.Surface.TERRAIN);
                    mTestDispatcher.getScheduler().advanceUntilIdle();
                    try {
                        AnchorCreateResult result = resultFuture.get();
                        assertThat(result).isInstanceOf(AnchorCreateSuccess.class);

                        AnchorCreateSuccess successResult = (AnchorCreateSuccess) result;
                        assertThat(successResult.getAnchor().getRuntimeAnchor())
                                .isEqualTo(fakeAnchor);

                        Anchor firstAnchor = (Anchor) mXrResourcesManager.getUpdatables().get(0);
                        assertThat(firstAnchor.getRuntimeAnchor()).isEqualTo(fakeAnchor);
                    } catch (Exception e) {
                        throw new RuntimeException();
                    }
                });
    }

    @Test
    public void createAnchorOnSurfaceAsync_illegalState_returnsIllegalStateResult() {
        createTestSessionAndRunTest(
                () -> {
                    Geospatial underTest = new Geospatial(mRuntimeGeospatial, mXrResourcesManager);

                    ListenableFuture<AnchorCreateResult> resultFuture =
                            createAnchorOnSurfaceAsync(
                                    underTest,
                                    mSession,
                                    LATITUDE,
                                    LONGITUDE,
                                    ALTITUDE_ABOVE_SURFACE,
                                    EUS_QUATERNION,
                                    Geospatial.Surface.TERRAIN);
                    mTestDispatcher.getScheduler().advanceUntilIdle();
                    try {
                        AnchorCreateResult result = resultFuture.get();

                        assertThat(result).isInstanceOf(AnchorCreateIllegalState.class);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Test
    public void createAnchorOnSurfaceAsync_resourceExhausted_returnsResourcesExhaustedResult() {
        createTestSessionAndRunTest(
                () -> {
                    Geospatial underTest = new Geospatial(mRuntimeGeospatial, mXrResourcesManager);
                    mRuntimeGeospatial.setNextException(new AnchorResourcesExhaustedException());

                    ListenableFuture<AnchorCreateResult> resultFuture =
                            createAnchorOnSurfaceAsync(
                                    underTest,
                                    mSession,
                                    LATITUDE,
                                    LONGITUDE,
                                    ALTITUDE_ABOVE_SURFACE,
                                    EUS_QUATERNION,
                                    Geospatial.Surface.TERRAIN);
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
        createTestSessionAndRunTest(
                () -> {
                    Geospatial underTest = new Geospatial(mRuntimeGeospatial, mXrResourcesManager);
                    mRuntimeGeospatial.setNextException(new AnchorNotAuthorizedException());

                    ListenableFuture<AnchorCreateResult> resultFuture =
                            createAnchorOnSurfaceAsync(
                                    underTest,
                                    mSession,
                                    LATITUDE,
                                    LONGITUDE,
                                    ALTITUDE_ABOVE_SURFACE,
                                    EUS_QUATERNION,
                                    Geospatial.Surface.TERRAIN);
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
        createTestSessionAndRunTest(
                () -> {
                    Geospatial underTest = new Geospatial(mRuntimeGeospatial, mXrResourcesManager);
                    mRuntimeGeospatial.setNextException(new AnchorUnsupportedLocationException());

                    ListenableFuture<AnchorCreateResult> resultFuture =
                            createAnchorOnSurfaceAsync(
                                    underTest,
                                    mSession,
                                    LATITUDE,
                                    LONGITUDE,
                                    ALTITUDE_ABOVE_SURFACE,
                                    EUS_QUATERNION,
                                    Geospatial.Surface.TERRAIN);
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
        createTestSessionAndRunTest(
                () -> {
                    Geospatial underTest = new Geospatial(mRuntimeGeospatial, mXrResourcesManager);
                    mRuntimeGeospatial.setNextException(
                            new IllegalArgumentException("Invalid latitude provided."));

                    ExecutionException outerException = null;
                    try {
                        ListenableFuture<AnchorCreateResult> resultFuture =
                                createAnchorOnSurfaceAsync(
                                        underTest,
                                        mSession,
                                        90.0,
                                        LONGITUDE,
                                        ALTITUDE_ABOVE_SURFACE,
                                        EUS_QUATERNION,
                                        Geospatial.Surface.TERRAIN);
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
        try (ActivityScenario<ComponentActivity> scenario =
                ActivityScenario.launch(ComponentActivity.class)) {
            scenario.onActivity(
                    activity -> {
                        mTestDispatcher =
                                StandardTestDispatcher(/* scheduler= */ null, /* name= */ null);
                        mSession =
                                ((SessionCreateSuccess) Session.create(activity, mTestDispatcher))
                                        .getSession();
                        mXrResourcesManager.setLifecycleManager$arcore(
                                SessionExtKt.getPerceptionRuntime(mSession).getLifecycleManager());
                        mSession.configure(
                                new Config(
                                        PlaneTrackingMode.DISABLED,
                                        HandTrackingMode.DISABLED,
                                        DeviceTrackingMode.DISABLED,
                                        DepthEstimationMode.DISABLED,
                                        AnchorPersistenceMode.DISABLED,
                                        FaceTrackingMode.DISABLED,
                                        GeospatialMode.VPS_AND_GPS,
                                        java.util.Collections.emptyList(),
                                        EyeTrackingMode.DISABLED,
                                        CameraFacingDirection.WORLD));

                        try {
                            testBody.run();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    private FakePerceptionManager getFakePerceptionManager() {
        return (FakePerceptionManager)
                SessionExtKt.getPerceptionRuntime(mSession).getPerceptionManager();
    }

    @Test
    public void checkVpsAvailabilityAsync_available_returnsAvailable() {
        createTestSessionAndRunTest(
                () -> {
                    Geospatial underTest = new Geospatial(mRuntimeGeospatial, mXrResourcesManager);

                    ListenableFuture<VpsAvailabilityResult> resultFuture =
                            checkVpsAvailabilityAsync(underTest, mSession, LATITUDE, LONGITUDE);
                    mTestDispatcher.getScheduler().advanceUntilIdle();

                    try {
                        VpsAvailabilityResult result = resultFuture.get();
                        assertThat(result).isInstanceOf(VpsAvailabilityAvailable.class);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
