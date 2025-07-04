/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.core

import android.os.Build
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.CameraInternal
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeCameraFactory
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ExecutionException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CameraSelectorTest {

    private val mRearId = "0"
    private val mFrontId = "1"
    private val mExternalId = "2"
    private val mRearRotation = 0
    private val mFrontRotation = 90
    private val mExternalRotation = 270
    private val mCameras = LinkedHashSet<CameraInternal>()
    private val mCameraInfos = mutableListOf<CameraInfo>()

    private lateinit var mRearCamera: CameraInternal
    private lateinit var mFrontCamera: CameraInternal
    private lateinit var mExternalCamera: CameraInternal

    @Before
    @Throws(ExecutionException::class, InterruptedException::class)
    public fun setUp() {
        val cameraFactory = FakeCameraFactory()
        mRearCamera =
            FakeCamera(
                mRearId,
                Mockito.mock(CameraControlInternal::class.java),
                FakeCameraInfoInternal(mRearId, mRearRotation, CameraSelector.LENS_FACING_BACK),
            )
        cameraFactory.insertCamera(CameraSelector.LENS_FACING_BACK, mRearId) { mRearCamera }
        mCameras.add(mRearCamera)
        mCameraInfos.add(mRearCamera.cameraInfo)

        mFrontCamera =
            FakeCamera(
                mFrontId,
                Mockito.mock(CameraControlInternal::class.java),
                FakeCameraInfoInternal(mFrontId, mFrontRotation, CameraSelector.LENS_FACING_FRONT),
            )
        cameraFactory.insertCamera(CameraSelector.LENS_FACING_FRONT, mFrontId) { mFrontCamera }
        mCameras.add(mFrontCamera)
        mCameraInfos.add(mFrontCamera.cameraInfo)

        mExternalCamera =
            FakeCamera(
                mExternalId,
                Mockito.mock(CameraControlInternal::class.java),
                FakeCameraInfoInternal(
                    mExternalId,
                    mExternalRotation,
                    CameraSelector.LENS_FACING_EXTERNAL,
                ),
            )
        cameraFactory.insertCamera(CameraSelector.LENS_FACING_EXTERNAL, mExternalId) {
            mExternalCamera
        }
        mCameras.add(mExternalCamera)
        mCameraInfos.add(mExternalCamera.cameraInfo)
    }

    @Test
    public fun canSelectWithLensFacing() {
        val cameraSelectorBuilder = CameraSelector.Builder()
        cameraSelectorBuilder.requireLensFacing(CameraSelector.LENS_FACING_BACK)
        assertThat(cameraSelectorBuilder.build().select(mCameras)).isEqualTo(mRearCamera)
    }

    @Test(expected = IllegalArgumentException::class)
    public fun exception_ifNoAvailableCamera() {
        val cameraSelectorBuilder = CameraSelector.Builder()
        cameraSelectorBuilder
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
        cameraSelectorBuilder.build().select(mCameras)
    }

    @Test
    public fun canGetLensFacing() {
        val cameraSelectorBuilder = CameraSelector.Builder()
        cameraSelectorBuilder.requireLensFacing(CameraSelector.LENS_FACING_BACK)
        assertThat(cameraSelectorBuilder.build().lensFacing)
            .isEqualTo(CameraSelector.LENS_FACING_BACK)
    }

    @Test(expected = IllegalStateException::class)
    public fun exception_ifGetLensFacingConflicted() {
        val cameraSelectorBuilder = CameraSelector.Builder()
        cameraSelectorBuilder
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
        cameraSelectorBuilder.build().lensFacing
    }

    @Test
    public fun canAppendFilters() {
        val filter0 = Mockito.mock(CameraFilter::class.java)
        val filter1 = Mockito.mock(CameraFilter::class.java)
        val filter2 = Mockito.mock(CameraFilter::class.java)
        val cameraSelector =
            CameraSelector.Builder()
                .addCameraFilter(filter0)
                .addCameraFilter(filter1)
                .addCameraFilter(filter2)
                .build()
        assertThat(cameraSelector.cameraFilterSet).containsAtLeast(filter0, filter1, filter2)
    }

    @Test
    public fun canSelectDefaultBackCamera() {
        assertThat(CameraSelector.DEFAULT_BACK_CAMERA.select(mCameras)).isEqualTo(mRearCamera)
    }

    @Test
    public fun canSelectDefaultFrontCamera() {
        assertThat(CameraSelector.DEFAULT_FRONT_CAMERA.select(mCameras)).isEqualTo(mFrontCamera)
    }

    @Test
    public fun canSelectWithCameraFilter() {
        val filter = CameraFilter { cameraInfos: List<CameraInfo> ->
            val output: MutableList<CameraInfo> = ArrayList()
            for (cameraInfo in cameraInfos) {
                if (cameraInfo.sensorRotationDegrees == mFrontRotation) {
                    output.add(cameraInfo)
                }
            }
            output
        }
        val cameraSelector = CameraSelector.Builder().addCameraFilter(filter).build()
        assertThat(cameraSelector.select(mCameras)).isEqualTo(mFrontCamera)
    }

    @Test(expected = IllegalArgumentException::class)
    public fun exception_extraOutputCamera() {
        val cameraSelectorBuilder = CameraSelector.Builder()
        cameraSelectorBuilder.addCameraFilter {
            val result: MutableList<CameraInfo> = ArrayList()
            // Add an extra camera to output.
            result.add(FakeCameraInfoInternal())
            result
        }
        cameraSelectorBuilder.build().select(mCameras)
    }

    @Test(expected = UnsupportedOperationException::class)
    public fun exception_extraInputCamera() {
        val cameraSelectorBuilder = CameraSelector.Builder()
        cameraSelectorBuilder.addCameraFilter { cameraInfos: MutableList<CameraInfo> ->
            val cameraInfo: CameraInfo = FakeCameraInfoInternal()
            // Add an extra camera to input.
            cameraInfos.add(cameraInfo)
            cameraInfos
        }
        // Should throw an exception if the input is modified.
        cameraSelectorBuilder.build().select(mCameras)
    }

    @Test
    public fun canFilterCameraInfos() {
        val cameraInfos = mCameras.map { camera -> camera.cameraInfo }
        val backCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        val filteredCameraInfos = backCameraSelector.filter(cameraInfos)
        assertThat(filteredCameraInfos).isEqualTo(listOf(mRearCamera.cameraInfo))
    }

    @Test
    public fun canFilterCameraInfosWithEmptyResult() {
        val cameraInfos = listOf(mFrontCamera.cameraInfo)
        val backCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        val filteredCameraInfos = backCameraSelector.filter(cameraInfos)
        assertThat(filteredCameraInfos).isEmpty()
    }

    @Test(expected = IllegalArgumentException::class)
    fun ofIdentifier_throwsException_whenNoIdentifierProvided() {
        CameraSelector.of()
    }

    @Test
    fun ofIdentifier_selectsSingleCamera() {
        val rearId = (mRearCamera.cameraInfo as CameraInfoInternal).cameraIdentifier
        val selector = CameraSelector.of(rearId)
        val filtered = selector.filter(mCameraInfos)
        assertThat(filtered).containsExactly(mRearCamera.cameraInfo)
    }

    @Test
    fun ofIdentifier_prioritizesIdentifiersInOrder() {
        val rearId = (mRearCamera.cameraInfo as CameraInfoInternal).cameraIdentifier
        val frontId = (mFrontCamera.cameraInfo as CameraInfoInternal).cameraIdentifier
        val externalId = (mExternalCamera.cameraInfo as CameraInfoInternal).cameraIdentifier

        // Prioritize: External -> Front -> Rear
        val selector = CameraSelector.of(externalId, frontId, rearId)
        val filtered = selector.filter(mCameraInfos)

        assertThat(filtered)
            .containsExactly(
                mExternalCamera.cameraInfo,
                mFrontCamera.cameraInfo,
                mRearCamera.cameraInfo,
            )
            .inOrder()
    }

    @Test
    fun ofIdentifier_prioritizesIdentifiersInReversedOrder() {
        val rearId = (mRearCamera.cameraInfo as CameraInfoInternal).cameraIdentifier
        val frontId = (mFrontCamera.cameraInfo as CameraInfoInternal).cameraIdentifier
        val externalId = (mExternalCamera.cameraInfo as CameraInfoInternal).cameraIdentifier

        // Prioritize: Rear -> Front -> External
        val selector = CameraSelector.of(rearId, frontId, externalId)
        val filtered = selector.filter(mCameraInfos)

        assertThat(filtered)
            .containsExactly(
                mRearCamera.cameraInfo,
                mFrontCamera.cameraInfo,
                mExternalCamera.cameraInfo,
            )
            .inOrder()
    }

    @Test
    fun ofIdentifier_ignoresNonExistentIdentifier() {
        val rearId = (mRearCamera.cameraInfo as CameraInfoInternal).cameraIdentifier
        // Create an identifier that does not correspond to any available camera.
        val fakeId = CameraIdentifier.create("fake-id")

        val selector = CameraSelector.of(fakeId, rearId)
        val filtered = selector.filter(mCameraInfos)

        // Should correctly filter out the fake ID and only return the valid one.
        assertThat(filtered).containsExactly(mRearCamera.cameraInfo)
    }

    @Test
    fun ofIdentifier_returnsEmpty_whenOnlyNonExistentIdentifierIsUsed() {
        val fakeId = CameraIdentifier.create("fake-id")
        val selector = CameraSelector.of(fakeId)
        val filtered = selector.filter(mCameraInfos)
        assertThat(filtered).isEmpty()
    }

    @Test
    fun ofIdentifier_handlesDuplicateIdentifiers() {
        val rearId = (mRearCamera.cameraInfo as CameraInfoInternal).cameraIdentifier
        val frontId = (mFrontCamera.cameraInfo as CameraInfoInternal).cameraIdentifier

        // Provide duplicates in the priority list.
        val selector = CameraSelector.of(rearId, frontId, rearId)
        val filtered = selector.filter(mCameraInfos)

        // The result should not contain duplicates and should respect the first-seen priority.
        assertThat(filtered)
            .containsExactly(mRearCamera.cameraInfo, mFrontCamera.cameraInfo)
            .inOrder()
    }
}
