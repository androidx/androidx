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

import android.os.IBinder;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@LargeTest
@RunWith(AndroidJUnit4.class)
public final class PlaneTest {
    @PerceptionLibraryConstants.OpenXrSpaceType
    private static final int OPEN_XR_REFERENCE_SPACE_TYPE = 1;

    private static final String TEST_NATIVE_LIBRARY_NAME = "fake_perception_library_jni";
    PerceptionLibraryTestHelper mTestHelper = new PerceptionLibraryTestHelper();
    IBinder mSharedAnchorToken = Mockito.mock(IBinder.class);

    @BeforeClass
    public static void beforeClass() {
        System.loadLibrary(TEST_NATIVE_LIBRARY_NAME);
    }

    @After
    public void tearDown() {
        mTestHelper.reset();
    }

    @Test
    public void getData_returnsPlaneData() {
        Pose centerPoint = new Pose(1, 2, 3, 4, 5, 6, 7);
        mTestHelper.addPlane(
                1L,
                centerPoint,
                8f,
                9f,
                Plane.Type.HORIZONTAL_DOWNWARD_FACING.intValue,
                Plane.Label.WALL.intValue);
        Plane plane = new Plane(1L, OPEN_XR_REFERENCE_SPACE_TYPE);

        assertThat(plane.getData(null))
                .isEqualTo(
                        new Plane.PlaneData(
                                centerPoint,
                                8f,
                                9f,
                                Plane.Type.HORIZONTAL_DOWNWARD_FACING.intValue,
                                Plane.Label.WALL.intValue));
    }

    @Test
    public void getDataError_returnsNull() {
        Plane plane = new Plane(1L, OPEN_XR_REFERENCE_SPACE_TYPE);
        assertThat(plane.getData(null)).isNull();
    }

    @Test
    public void createAnchor_success() {
        long anchorId = 888;
        mTestHelper.setGetAnchorResult(mSharedAnchorToken, anchorId);
        Pose centerPoint = new Pose(1, 2, 3, 4, 5, 6, 7);
        mTestHelper.addPlane(
                1L,
                centerPoint,
                8f,
                9f,
                Plane.Type.HORIZONTAL_DOWNWARD_FACING.intValue,
                Plane.Label.WALL.intValue);
        Plane plane = new Plane(1L, OPEN_XR_REFERENCE_SPACE_TYPE);
        Pose anchorPose = new Pose(7, 6, 5, 4, 3, 2, 1);

        Anchor anchor = plane.createAnchor(anchorPose, null);
        assertThat(anchor.getAnchorToken()).isEqualTo(mSharedAnchorToken);
    }

    @Test
    public void createAnchor_failureReturnsNull() throws Exception {
        Pose centerPoint = new Pose(1, 2, 3, 4, 5, 6, 7);
        mTestHelper.addPlane(
                1L,
                centerPoint,
                8f,
                9f,
                Plane.Type.HORIZONTAL_DOWNWARD_FACING.intValue,
                Plane.Label.WALL.intValue);
        Plane plane = new Plane(1L, OPEN_XR_REFERENCE_SPACE_TYPE);
        Pose anchorPose = new Pose(7, 6, 5, 4, 3, 2, 1);

        assertThat(plane.createAnchor(anchorPose, null)).isNull();
    }

    @Test
    public void getAnchors_returnsAnchors() throws Exception {
        long anchorId = 888;
        mTestHelper.setGetAnchorResult(mSharedAnchorToken, anchorId);
        Pose centerPoint = new Pose(1, 2, 3, 4, 5, 6, 7);
        mTestHelper.addPlane(
                1L,
                centerPoint,
                8f,
                9f,
                Plane.Type.HORIZONTAL_DOWNWARD_FACING.intValue,
                Plane.Label.WALL.intValue);
        Plane plane = new Plane(1L, OPEN_XR_REFERENCE_SPACE_TYPE);
        Pose anchorPose = new Pose(7, 6, 5, 4, 3, 2, 1);
        Anchor anchor = plane.createAnchor(anchorPose, null);

        assertThat(plane.getAnchors()).containsExactly(anchor);
    }
}
