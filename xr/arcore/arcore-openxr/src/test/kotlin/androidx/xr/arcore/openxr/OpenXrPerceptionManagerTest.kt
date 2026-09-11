/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.arcore.openxr

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.runtime.SpatialAnnotationId
import androidx.xr.arcore.runtime.SpatialAnnotationImageFormat
import androidx.xr.arcore.runtime.SpatialAnnotationQuadAlignment
import androidx.xr.runtime.ExperimentalSpatialAnnotationsApi
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Quad
import androidx.xr.runtime.math.Vector2
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalSpatialAnnotationsApi::class)
@RunWith(AndroidJUnit4::class)
// TODO(b/560290369): Failures yield clear assertion messages rather than IllegalStateException.
class OpenXrPerceptionManagerTest {

    private lateinit var underTest: OpenXrPerceptionManager
    private lateinit var timeSource: OpenXrTimeSource

    @Before
    fun setUp() {
        timeSource = OpenXrTimeSource()
        underTest = OpenXrPerceptionManager(timeSource)
    }

    @Test
    fun updateSpatialAnnotations_noConfigs_doesNothing() {
        underTest.updateSpatialAnnotations(1000L)

        assertThat(underTest.trackables).isEmpty()
        assertThat(underTest.xrResources.trackablesMap).isEmpty()
        assertThat(underTest.xrResources.updatables).isEmpty()
    }

    @Test
    fun updateSpatialAnnotations_addsSpatialAnnotationToTrackablesAndUpdatables() {
        val id = SpatialAnnotationId.fromString("test_annotation")
        val handle = 42L
        underTest.xrResources.addAnnotationHandle(id, handle, SpatialAnnotationQuadAlignment.SCREEN)

        underTest.updateSpatialAnnotations(1000L)
        val trackable = underTest.xrResources.trackablesMap[handle] as? OpenXrSpatialAnnotation

        assertThat(trackable).isNotNull()
        assertThat(trackable?.id).isEqualTo(id)
        assertThat(trackable?.alignment).isEqualTo(SpatialAnnotationQuadAlignment.SCREEN)
        assertThat(underTest.xrResources.updatables).contains(trackable as Updatable)
    }

    @Test
    fun updateSpatialAnnotations_alreadyTracked_doesNotDuplicateTrackable() {
        val id = SpatialAnnotationId.fromString("test_annotation")
        val handle = 42L
        underTest.xrResources.addAnnotationHandle(id, handle, SpatialAnnotationQuadAlignment.SCREEN)

        underTest.updateSpatialAnnotations(1000L)
        val firstTrackable = underTest.xrResources.trackablesMap[handle]

        underTest.updateSpatialAnnotations(2000L)
        val secondTrackable = underTest.xrResources.trackablesMap[handle]

        assertThat(secondTrackable).isSameInstanceAs(firstTrackable)
        assertThat(underTest.xrResources.updatables.filter { it === firstTrackable }).hasSize(1)
    }

    @Test
    fun updateSpatialAnnotations_multipleAnnotations_addsAllTrackables() {
        val id1 = SpatialAnnotationId.fromString("annotation_1")
        val id2 = SpatialAnnotationId.fromString("annotation_2")
        underTest.xrResources.addAnnotationHandle(id1, 101L, SpatialAnnotationQuadAlignment.SCREEN)
        underTest.xrResources.addAnnotationHandle(id2, 102L, SpatialAnnotationQuadAlignment.OBJECT)

        underTest.updateSpatialAnnotations(1000L)

        assertThat(underTest.xrResources.trackablesMap).containsKey(101L)
        assertThat(underTest.xrResources.trackablesMap).containsKey(102L)

        val trackable1 = underTest.xrResources.trackablesMap[101L] as? OpenXrSpatialAnnotation
        val trackable2 = underTest.xrResources.trackablesMap[102L] as? OpenXrSpatialAnnotation

        assertThat(trackable1?.alignment).isEqualTo(SpatialAnnotationQuadAlignment.SCREEN)
        assertThat(trackable2?.alignment).isEqualTo(SpatialAnnotationQuadAlignment.OBJECT)
        assertThat(underTest.xrResources.updatables).contains(trackable1 as Updatable)
        assertThat(underTest.xrResources.updatables).contains(trackable2 as Updatable)
    }

    @Test
    fun stopSpatialAnnotationTracking_emptyListAndNoConfigs_doesNothing() {
        underTest.stopSpatialAnnotationTracking(emptyList())

        assertThat(underTest.xrResources.annotationConfigs).isEmpty()
        assertThat(underTest.xrResources.trackablesMap).isEmpty()
        assertThat(underTest.xrResources.updatables).isEmpty()
    }

    @Test
    fun stopSpatialAnnotationTracking_unknownId_doesNothing() {
        underTest.stopSpatialAnnotationTracking(listOf(SpatialAnnotationId.fromString("unknown")))

        assertThat(underTest.xrResources.annotationConfigs).isEmpty()
    }

    @Test
    fun stopSpatialAnnotationTracking_removesTrackableAndUpdatable() {
        val id = SpatialAnnotationId.fromString("test_annotation")
        val handle = 42L
        underTest.xrResources.addAnnotationHandle(id, handle, SpatialAnnotationQuadAlignment.SCREEN)
        underTest.updateSpatialAnnotations(1000L)
        val trackable = underTest.xrResources.trackablesMap[handle]
        checkNotNull(trackable)
        check(underTest.xrResources.updatables.contains(trackable as Updatable))

        underTest.stopSpatialAnnotationTracking(listOf(id))

        assertThat(underTest.xrResources.annotationConfigs).doesNotContainKey(id)
        assertThat(underTest.xrResources.trackablesMap).doesNotContainKey(handle)
        assertThat(underTest.xrResources.updatables).doesNotContain(trackable as Updatable)
    }

    @Test
    fun stopSpatialAnnotationTracking_emptyList_stopsAllAnnotations() {
        val id1 = SpatialAnnotationId.fromString("annotation_1")
        val id2 = SpatialAnnotationId.fromString("annotation_2")
        underTest.xrResources.addAnnotationHandle(id1, 101L, SpatialAnnotationQuadAlignment.SCREEN)
        underTest.xrResources.addAnnotationHandle(id2, 102L, SpatialAnnotationQuadAlignment.OBJECT)
        underTest.updateSpatialAnnotations(1000L)
        check(underTest.xrResources.trackablesMap.size == 2)
        check(underTest.xrResources.updatables.size == 2)

        underTest.stopSpatialAnnotationTracking(emptyList())

        assertThat(underTest.xrResources.annotationConfigs).isEmpty()
        assertThat(underTest.xrResources.trackablesMap).isEmpty()
        assertThat(underTest.xrResources.updatables).isEmpty()
    }

    @Test
    fun clear_clearsSpatialAnnotationsAndResources() {
        val id = SpatialAnnotationId.fromString("test_annotation")
        underTest.xrResources.addAnnotationHandle(id, 42L, SpatialAnnotationQuadAlignment.SCREEN)
        underTest.updateSpatialAnnotations(1000L)

        underTest.clear()

        assertThat(underTest.xrResources.annotationConfigs).isEmpty()
        assertThat(underTest.xrResources.trackablesMap).isEmpty()
        assertThat(underTest.xrResources.updatables).isEmpty()
    }

    @Test
    fun updateSpatialAnnotations_nullAlignment_addsTrackableWithNullAlignment() {
        val id = SpatialAnnotationId.fromString("test_annotation")
        val handle = 42L
        underTest.xrResources.addAnnotationHandle(id, handle, alignment = null)

        underTest.updateSpatialAnnotations(1000L)

        val trackable = underTest.xrResources.trackablesMap[handle] as? OpenXrSpatialAnnotation
        assertThat(trackable).isNotNull()
        assertThat(trackable?.alignment).isNull()
    }

    @Test
    fun stopSpatialAnnotationTracking_specificId_removesOnlyTargetAnnotation() {
        val id1 = SpatialAnnotationId.fromString("annotation_1")
        val id2 = SpatialAnnotationId.fromString("annotation_2")
        underTest.xrResources.addAnnotationHandle(id1, 101L, SpatialAnnotationQuadAlignment.SCREEN)
        underTest.xrResources.addAnnotationHandle(id2, 102L, SpatialAnnotationQuadAlignment.OBJECT)
        underTest.updateSpatialAnnotations(1000L)

        underTest.stopSpatialAnnotationTracking(listOf(id1))

        assertThat(underTest.xrResources.annotationConfigs).doesNotContainKey(id1)
        assertThat(underTest.xrResources.annotationConfigs).containsKey(id2)
        assertThat(underTest.xrResources.trackablesMap).doesNotContainKey(101L)
        assertThat(underTest.xrResources.trackablesMap).containsKey(102L)
        val trackable2 = underTest.xrResources.trackablesMap[102L] as Updatable
        assertThat(underTest.xrResources.updatables).contains(trackable2)
    }

    @Test
    fun stopSpatialAnnotationTracking_partiallyKnownIds_removesOnlyKnownId() {
        val id1 = SpatialAnnotationId.fromString("annotation_1")
        val unknownId = SpatialAnnotationId.fromString("unknown")
        underTest.xrResources.addAnnotationHandle(id1, 101L, SpatialAnnotationQuadAlignment.SCREEN)
        underTest.updateSpatialAnnotations(1000L)

        underTest.stopSpatialAnnotationTracking(listOf(id1, unknownId))

        assertThat(underTest.xrResources.annotationConfigs).doesNotContainKey(id1)
        assertThat(underTest.xrResources.trackablesMap).doesNotContainKey(101L)
    }

    @Test
    fun startSpatialAnnotationTracking_singleQuad_invokesNativeMethod() {
        val id = SpatialAnnotationId.fromString("test_annotation")
        val quad =
            Quad.fromCorners(
                upperLeft = Vector2(-1f, 1f),
                upperRight = Vector2(1f, 1f),
                lowerRight = Vector2(1f, -1f),
                lowerLeft = Vector2(-1f, -1f),
            )
        val imageBuffer = ByteBuffer.allocateDirect(100)

        underTest.startSpatialAnnotationTracking(
            imageBuffer = imageBuffer,
            imageSize = IntSize2d(10, 10),
            rowStride = 10,
            format = SpatialAnnotationImageFormat.GRAYSCALE,
            alignment = SpatialAnnotationQuadAlignment.SCREEN,
            quads = mapOf(id to quad),
            timestampNanos = 1000L,
        )
    }

    @Test
    fun startSpatialAnnotationTracking_emptyQuads_invokesNativeMethod() {
        val imageBuffer = ByteBuffer.allocateDirect(100)

        underTest.startSpatialAnnotationTracking(
            imageBuffer = imageBuffer,
            imageSize = IntSize2d(10, 10),
            rowStride = 10,
            format = SpatialAnnotationImageFormat.GRAYSCALE,
            alignment = SpatialAnnotationQuadAlignment.SCREEN,
            quads = emptyMap(),
            timestampNanos = 1000L,
        )
    }

    @Test
    fun startSpatialAnnotationTracking_multipleQuads_invokesNativeMethod() {
        val id1 = SpatialAnnotationId.fromString("annotation_1")
        val id2 = SpatialAnnotationId.fromString("annotation_2")
        val quad1 =
            Quad.fromCorners(
                upperLeft = Vector2(-1f, 1f),
                upperRight = Vector2(1f, 1f),
                lowerRight = Vector2(1f, -1f),
                lowerLeft = Vector2(-1f, -1f),
            )
        val quad2 =
            Quad.fromCorners(
                upperLeft = Vector2(-2f, 2f),
                upperRight = Vector2(2f, 2f),
                lowerRight = Vector2(2f, -2f),
                lowerLeft = Vector2(-2f, -2f),
            )
        val imageBuffer = ByteBuffer.allocateDirect(100)

        underTest.startSpatialAnnotationTracking(
            imageBuffer = imageBuffer,
            imageSize = IntSize2d(10, 10),
            rowStride = 10,
            format = SpatialAnnotationImageFormat.RGBA,
            alignment = SpatialAnnotationQuadAlignment.OBJECT,
            quads = mapOf(id1 to quad1, id2 to quad2),
            timestampNanos = 2000L,
        )
    }
}
