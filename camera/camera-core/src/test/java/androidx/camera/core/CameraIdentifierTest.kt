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

package androidx.camera.core

import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.ForwardingCameraInfo
import androidx.camera.core.impl.Identifier
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeCameraConfig
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.internal.DoNotInstrument

private const val CAMERA_ID_0 = "0"
private const val CAMERA_ID_1 = "1"
private const val CAMERA_ID_2 = "2"
private const val PHYSICAL_CAMERA_ID_0 = "2"
private const val PHYSICAL_CAMERA_ID_1 = "3"

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@org.robolectric.annotation.Config(
    sdk = [org.robolectric.annotation.Config.ALL_SDKS],
    instrumentedPackages = ["androidx.camera.core"],
)
class CameraIdentifierTest {

    private lateinit var fakeCameraInfo0: CameraInfoInternal
    private lateinit var fakeCameraInfo1: CameraInfoInternal
    private lateinit var fakeCamera2: CameraInfoInternal
    private lateinit var fakeCamera0: FakeCamera
    private lateinit var fakeCamera1: FakeCamera

    @Before
    fun setUp() {
        val physicalInfo0 =
            FakePhysicalCameraInfo(PHYSICAL_CAMERA_ID_0, physicalCameraId = PHYSICAL_CAMERA_ID_0)
        val physicalInfo1 =
            FakePhysicalCameraInfo(PHYSICAL_CAMERA_ID_1, physicalCameraId = PHYSICAL_CAMERA_ID_1)
        fakeCameraInfo0 =
            FakePhysicalCameraInfo(
                CAMERA_ID_0,
                physicalCameraInfos = setOf(physicalInfo0, physicalInfo1),
            )
        fakeCameraInfo1 =
            FakePhysicalCameraInfo(
                CAMERA_ID_1,
                physicalCameraInfos = setOf(physicalInfo0, physicalInfo1),
            )
        fakeCamera2 = FakeCameraInfoInternal(CAMERA_ID_2)
        fakeCamera0 = FakeCamera(CAMERA_ID_0, null, fakeCameraInfo0)
        fakeCamera1 = FakeCamera(CAMERA_ID_1, null, fakeCameraInfo1)
    }

    @Test
    fun create_singleCameraId_isEquivalentToItself() {
        val id = CameraIdentifier.Factory.create(CAMERA_ID_0)
        val idCopy = CameraIdentifier.Factory.create(CAMERA_ID_0)
        assertThat(id).isEqualTo(idCopy)
        assertThat(id.hashCode()).isEqualTo(idCopy.hashCode())
    }

    @Test
    fun create_singleCameraId_differentId_isNotEquivalent() {
        val id0 = CameraIdentifier.Factory.create(CAMERA_ID_0)
        val id1 = CameraIdentifier.Factory.create(CAMERA_ID_1)
        assertThat(id0).isNotEqualTo(id1)
        // Hash codes might collide, but it's good practice to assert inequality when objects are
        // not equal.
        // If hash codes are equal, it just means it's a valid hash collision, not a bug in
        // hashCode().
        // However, if they were equal and the objects were not, that would be a bug.
        // For distinct objects, generally expect distinct hash codes, unless specifically designed
        // to collide.
    }

    @Test
    fun create_multipleCameraIds_sameOrderSameCompatibilityId_isEquivalent() {
        val cameraIds1 =
            listOf(
                CameraIdentifier.CompositeCameraId(CAMERA_ID_0),
                CameraIdentifier.CompositeCameraId(CAMERA_ID_1),
            )
        val cameraIds2 =
            listOf(
                CameraIdentifier.CompositeCameraId(CAMERA_ID_0),
                CameraIdentifier.CompositeCameraId(CAMERA_ID_1),
            )
        val compatId = Identifier.create(Any())

        val id1 = CameraIdentifier.Factory.create(cameraIds1, compatId)
        val id2 = CameraIdentifier.Factory.create(cameraIds2, compatId)
        assertThat(id1).isEqualTo(id2)
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode())
    }

    @Test
    fun create_multipleCameraIds_differentOrderSameCompatibilityId_isNotEquivalent() {
        val cameraIds1 =
            listOf(
                CameraIdentifier.CompositeCameraId(CAMERA_ID_0),
                CameraIdentifier.CompositeCameraId(CAMERA_ID_1),
            )
        val cameraIds2 =
            listOf(
                CameraIdentifier.CompositeCameraId(CAMERA_ID_1),
                CameraIdentifier.CompositeCameraId(CAMERA_ID_0),
            ) // Different order
        val compatId = Identifier.create(Any())

        val id1 = CameraIdentifier.Factory.create(cameraIds1, compatId)
        val id2 = CameraIdentifier.Factory.create(cameraIds2, compatId)
        assertThat(id1).isNotEqualTo(id2)
        assertThat(id1.hashCode())
            .isNotEqualTo(id2.hashCode()) // Expect different hash codes for different objects
    }

    @Test
    fun create_multipleCameraIds_sameOrderDifferentCompatibilityId_isNotEquivalent() {
        val cameraIds =
            listOf(
                CameraIdentifier.CompositeCameraId(CAMERA_ID_0),
                CameraIdentifier.CompositeCameraId(CAMERA_ID_1),
            )
        val compatId1 = Identifier.create(Any())
        val compatId2 = Identifier.create(Any()) // Different compatibility ID

        val id1 = CameraIdentifier.Factory.create(cameraIds, compatId1)
        val id2 = CameraIdentifier.Factory.create(cameraIds, compatId2)
        assertThat(id1).isNotEqualTo(id2)
        assertThat(id1.hashCode())
            .isNotEqualTo(id2.hashCode()) // Expect different hash codes for different objects
    }

    @Test
    fun create_multipleCameraIds_differentCount_isNotEquivalent() {
        val cameraIds1 =
            listOf(
                CameraIdentifier.CompositeCameraId(CAMERA_ID_0),
                CameraIdentifier.CompositeCameraId(CAMERA_ID_1),
            )
        val cameraIds2 = listOf(CameraIdentifier.CompositeCameraId(CAMERA_ID_0)) // Different count
        val compatId = Identifier.create(Any())

        val id1 = CameraIdentifier.Factory.create(cameraIds1, compatId)
        val id2 = CameraIdentifier.Factory.create(cameraIds2, compatId)
        assertThat(id1).isNotEqualTo(id2)
        assertThat(id1.hashCode())
            .isNotEqualTo(id2.hashCode()) // Expect different hash codes for different objects
    }

    @Test
    fun create_multipleCameraIds_withNullCompatibilityId_isEquivalent() {
        val cameraIds =
            listOf(
                CameraIdentifier.CompositeCameraId(CAMERA_ID_0),
                CameraIdentifier.CompositeCameraId(CAMERA_ID_1),
            )
        val id1 = CameraIdentifier.Factory.create(cameraIds, null)
        val id2 = CameraIdentifier.Factory.create(cameraIds, null)
        assertThat(id1).isEqualTo(id2)
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode())
    }

    @Test
    fun create_multipleCameraIds_oneWithCompatIdOneWithout_isNotEquivalent() {
        val cameraIds =
            listOf(
                CameraIdentifier.CompositeCameraId(CAMERA_ID_0),
                CameraIdentifier.CompositeCameraId(CAMERA_ID_1),
            )
        val compatId = Identifier.create(Any())

        val id1 = CameraIdentifier.Factory.create(cameraIds, compatId)
        val id2 = CameraIdentifier.Factory.create(cameraIds, null)
        assertThat(id1).isNotEqualTo(id2)
        assertThat(id1.hashCode())
            .isNotEqualTo(id2.hashCode()) // Expect different hash codes for different objects
    }

    @Test
    fun create_emptyCameraIds_throwsIllegalArgumentException() {
        assertThrows<IllegalArgumentException> {
            CameraIdentifier.Factory.create(listOf<CameraIdentifier.CompositeCameraId>(), null)
        }
    }

    @Test
    fun create_primarySecondary_sameIdsSameCompatId_isEquivalent() {
        val compatId = Identifier.create(Any())
        val id1 = CameraIdentifier.Factory.create(CAMERA_ID_0, CAMERA_ID_1, compatId)
        val id2 = CameraIdentifier.Factory.create(CAMERA_ID_0, CAMERA_ID_1, compatId)
        assertThat(id1).isEqualTo(id2)
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode())
    }

    @Test
    fun create_primarySecondary_primaryDifferent_isNotEquivalent() {
        val compatId = Identifier.create(Any())
        val id1 = CameraIdentifier.Factory.create(CAMERA_ID_0, CAMERA_ID_1, compatId)
        val id2 =
            CameraIdentifier.Factory.create(CAMERA_ID_2, CAMERA_ID_1, compatId) // Primary different
        assertThat(id1).isNotEqualTo(id2)
        assertThat(id1.hashCode()).isNotEqualTo(id2.hashCode())
    }

    @Test
    fun create_primarySecondary_secondaryDifferent_isNotEquivalent() {
        val compatId = Identifier.create(Any())
        val id1 = CameraIdentifier.Factory.create(CAMERA_ID_0, CAMERA_ID_1, compatId)
        val id2 =
            CameraIdentifier.Factory.create(
                CAMERA_ID_0,
                CAMERA_ID_2,
                compatId,
            ) // Secondary different
        assertThat(id1).isNotEqualTo(id2)
        assertThat(id1.hashCode()).isNotEqualTo(id2.hashCode())
    }

    @Test
    fun create_primarySecondary_secondaryNullVsNonNull_isNotEquivalent() {
        val compatId = Identifier.create(Any())
        val id1 = CameraIdentifier.Factory.create(CAMERA_ID_0, CAMERA_ID_1, compatId)
        val id2 = CameraIdentifier.Factory.create(CAMERA_ID_0, null, compatId) // Secondary is null
        assertThat(id1).isNotEqualTo(id2)
        assertThat(id1.hashCode()).isNotEqualTo(id2.hashCode())
    }

    @Test
    fun fromAdapterInfos_singleCameraInfo_generatesCorrectIdentifier() {
        val primaryCameraConfig = FakeCameraConfig()
        val adapterCameraInfo = AdapterCameraInfo(fakeCameraInfo0, primaryCameraConfig)
        // According to the designed behavior of CameraIdentifier.from(), we should use the
        // compatibility id from primary AdapterCameraInfo.
        val expectedIdentifier =
            CameraIdentifier.Factory.create(CAMERA_ID_0, null, primaryCameraConfig.compatibilityId)

        val actualIdentifier = CameraIdentifier.Factory.fromAdapterInfos(adapterCameraInfo, null)
        assertThat(actualIdentifier).isEqualTo(expectedIdentifier)
    }

    @Test
    fun fromAdapterInfos_dualCameraInfo_generatesCorrectIdentifier() {
        val primaryCameraConfig = FakeCameraConfig()
        val primaryInfo = AdapterCameraInfo(fakeCameraInfo0, primaryCameraConfig)
        val secondaryInfo = AdapterCameraInfo(fakeCameraInfo1, FakeCameraConfig())
        // According to the designed behavior of CameraIdentifier.from(), we should use the
        // compatibility id from primary AdapterCameraInfo.
        val expectedIdentifier =
            CameraIdentifier.Factory.create(
                CAMERA_ID_0,
                CAMERA_ID_1,
                primaryCameraConfig.compatibilityId,
            )

        val actualIdentifier = CameraIdentifier.Factory.fromAdapterInfos(primaryInfo, secondaryInfo)
        assertThat(actualIdentifier).isEqualTo(expectedIdentifier)
    }

    @Test
    fun from_AdapterInfos_primaryWithCompatibilityId_generatesCorrectIdentifier() {
        val compatId = Identifier.create("test_compat_id")
        val primaryConfig = FakeCameraConfig(compatibilityId = compatId)
        val primaryInfo = AdapterCameraInfo(fakeCameraInfo0, primaryConfig)
        val expectedIdentifier = CameraIdentifier.Factory.create(CAMERA_ID_0, null, compatId)

        val actualIdentifier = CameraIdentifier.Factory.fromAdapterInfos(primaryInfo, null)
        assertThat(actualIdentifier).isEqualTo(expectedIdentifier)
    }

    @Test
    fun fromAdapterInfos_withPhysicalCameraId_includesPhysicalCameraIdInIdentifier() {
        val primaryConfig = FakeCameraConfig()
        val physicalCameraId = PHYSICAL_CAMERA_ID_0
        val primaryInfo = AdapterCameraInfo(fakeCameraInfo0, physicalCameraId, primaryConfig)

        val actualIdentifier = CameraIdentifier.Factory.fromAdapterInfos(primaryInfo, null)
        assertThat(actualIdentifier.cameraIds)
            .containsExactly(CameraIdentifier.CompositeCameraId(CAMERA_ID_0, physicalCameraId))
        assertThat(actualIdentifier.internalId).isEqualTo(CAMERA_ID_0)
    }

    @Test
    fun fromAdapterInfos_physicalCameraVsLogicalCamera_areNotEquivalent() {
        val primaryConfig = FakeCameraConfig()
        val logicalInfo = AdapterCameraInfo(fakeCameraInfo0, primaryConfig)
        val physicalInfo = AdapterCameraInfo(fakeCameraInfo0, PHYSICAL_CAMERA_ID_0, primaryConfig)

        val logicalIdentifier = CameraIdentifier.Factory.fromAdapterInfos(logicalInfo, null)
        val physicalIdentifier = CameraIdentifier.Factory.fromAdapterInfos(physicalInfo, null)

        assertThat(logicalIdentifier).isNotEqualTo(physicalIdentifier)
    }

    @Test
    fun fromAdapterInfos_differentPhysicalCamerasOnSameLogicalCamera_areNotEquivalent() {
        val primaryConfig = FakeCameraConfig()
        val physicalInfo1 = AdapterCameraInfo(fakeCameraInfo0, PHYSICAL_CAMERA_ID_0, primaryConfig)
        val physicalInfo2 = AdapterCameraInfo(fakeCameraInfo0, PHYSICAL_CAMERA_ID_1, primaryConfig)

        val id1 = CameraIdentifier.Factory.fromAdapterInfos(physicalInfo1, null)
        val id2 = CameraIdentifier.Factory.fromAdapterInfos(physicalInfo2, null)

        assertThat(id1).isNotEqualTo(id2)
        assertThat(id1.hashCode()).isNotEqualTo(id2.hashCode())
    }

    @Test
    fun fromAdapterInfos_dualPhysicalCameras_includesAllCameraIdsInOrder() {
        val primaryConfig = FakeCameraConfig()
        val primaryInfo = AdapterCameraInfo(fakeCameraInfo0, PHYSICAL_CAMERA_ID_0, primaryConfig)
        val secondaryInfo =
            AdapterCameraInfo(fakeCameraInfo1, PHYSICAL_CAMERA_ID_1, FakeCameraConfig())

        val actualIdentifier = CameraIdentifier.Factory.fromAdapterInfos(primaryInfo, secondaryInfo)

        assertThat(actualIdentifier.cameraIds)
            .containsExactly(
                CameraIdentifier.CompositeCameraId(CAMERA_ID_0, PHYSICAL_CAMERA_ID_0),
                CameraIdentifier.CompositeCameraId(CAMERA_ID_1, PHYSICAL_CAMERA_ID_1),
            )
            .inOrder()
    }

    @Test
    fun fromAdapterInfos_dualPhysicalCamerasOnSameLogicalCamera_preservesBothPhysicalCameraIds() {
        val primaryConfig = FakeCameraConfig()
        val primaryInfo = AdapterCameraInfo(fakeCameraInfo0, PHYSICAL_CAMERA_ID_0, primaryConfig)
        val secondaryInfo =
            AdapterCameraInfo(fakeCameraInfo0, PHYSICAL_CAMERA_ID_1, FakeCameraConfig())

        val actualIdentifier = CameraIdentifier.Factory.fromAdapterInfos(primaryInfo, secondaryInfo)

        assertThat(actualIdentifier.cameraIds)
            .containsExactly(
                CameraIdentifier.CompositeCameraId(CAMERA_ID_0, PHYSICAL_CAMERA_ID_0),
                CameraIdentifier.CompositeCameraId(CAMERA_ID_0, PHYSICAL_CAMERA_ID_1),
            )
            .inOrder()
    }

    @Test
    fun fromAdapterInfos_dualCamera_preservesLogicalToPhysicalMapping() {
        val primaryLogical = AdapterCameraInfo(fakeCameraInfo0, FakeCameraConfig())
        val primaryPhysical =
            AdapterCameraInfo(fakeCameraInfo0, PHYSICAL_CAMERA_ID_0, FakeCameraConfig())
        val secondaryLogical = AdapterCameraInfo(fakeCameraInfo1, FakeCameraConfig())
        val secondaryPhysical =
            AdapterCameraInfo(fakeCameraInfo1, PHYSICAL_CAMERA_ID_0, FakeCameraConfig())

        // primary=0 (logical), secondary=1 (physical 2)
        val id1 = CameraIdentifier.Factory.fromAdapterInfos(primaryLogical, secondaryPhysical)
        // primary=0 (physical 2), secondary=1 (logical)
        val id2 = CameraIdentifier.Factory.fromAdapterInfos(primaryPhysical, secondaryLogical)

        assertThat(id1.cameraIds)
            .containsExactly(
                CameraIdentifier.CompositeCameraId(CAMERA_ID_0),
                CameraIdentifier.CompositeCameraId(CAMERA_ID_1, PHYSICAL_CAMERA_ID_0),
            )
            .inOrder()

        assertThat(id2.cameraIds)
            .containsExactly(
                CameraIdentifier.CompositeCameraId(CAMERA_ID_0, PHYSICAL_CAMERA_ID_0),
                CameraIdentifier.CompositeCameraId(CAMERA_ID_1),
            )
            .inOrder()

        assertThat(id1).isNotEqualTo(id2)
        assertThat(id1.hashCode()).isNotEqualTo(id2.hashCode())
    }

    @Test
    fun fromAdapterInfos_dualCameraWithCompatibilityId_generatesCorrectIdentifier() {
        val compatId = Identifier.create("test_compat_id")
        val primaryConfig = FakeCameraConfig(compatibilityId = compatId)
        val primaryInfo = AdapterCameraInfo(fakeCameraInfo0, primaryConfig)
        // Secondary config does not influence compatId in the CameraIdentifier logic
        val secondaryInfo = AdapterCameraInfo(fakeCameraInfo1, FakeCameraConfig())

        val expectedIdentifier = CameraIdentifier.Factory.create(CAMERA_ID_0, CAMERA_ID_1, compatId)
        val actualIdentifier = CameraIdentifier.Factory.fromAdapterInfos(primaryInfo, secondaryInfo)
        assertThat(actualIdentifier).isEqualTo(expectedIdentifier)
    }

    @Test
    fun fromAdapterInfos_differentPrimaryCompatibilityId_isNotEquivalent() {
        val compatId1 = Identifier.create("compat1")
        val compatId2 = Identifier.create("compat2")

        val primaryInfo1 =
            AdapterCameraInfo(fakeCameraInfo0, FakeCameraConfig(compatibilityId = compatId1))
        val primaryInfo2 =
            AdapterCameraInfo(fakeCameraInfo0, FakeCameraConfig(compatibilityId = compatId2))

        val id1 = CameraIdentifier.Factory.fromAdapterInfos(primaryInfo1, null)
        val id2 = CameraIdentifier.Factory.fromAdapterInfos(primaryInfo2, null)

        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun fromAdapterInfos_identicalPrimaryAndSecondaryInfo_generatesEquivalentIdentifier() {
        val compatId = Identifier.create("shared_compat")
        val config = FakeCameraConfig(compatibilityId = compatId)
        val primaryInfo = AdapterCameraInfo(fakeCameraInfo0, config)
        val secondaryInfo =
            AdapterCameraInfo(fakeCameraInfo1, config) // Both use same config for primary

        val id1 = CameraIdentifier.Factory.fromAdapterInfos(primaryInfo, secondaryInfo)
        val id2 = CameraIdentifier.Factory.fromAdapterInfos(primaryInfo, secondaryInfo)

        assertThat(id1).isEqualTo(id2)
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode())
    }

    @Test
    fun fromAdapterInfos_dualCameraInfo_isEquivalentToManuallyCreatedAdapterId() {
        // Arrange: Simulate the creation of an identifier within CameraUseCaseAdapter.
        // This is the "ground truth" identifier that would be stored in the repository.
        val compatId = Identifier.create("some_extension")
        val adapterIdentifier = CameraIdentifier.Factory.create(CAMERA_ID_0, CAMERA_ID_1, compatId)

        // Act: Simulate the creation of an identifier in LifecycleCameraProviderImpl
        // for the repository lookup, using the .from() factory.
        val primaryConfig = FakeCameraConfig(compatibilityId = compatId)
        val primaryInfo = AdapterCameraInfo(fakeCameraInfo0, primaryConfig)
        val secondaryInfo = AdapterCameraInfo(fakeCameraInfo1, FakeCameraConfig())
        val lookupIdentifier = CameraIdentifier.Factory.fromAdapterInfos(primaryInfo, secondaryInfo)

        // Assert: The identifier created for the lookup MUST be equal to the one
        // that would have been created by the adapter. This ensures the repository key matches.
        assertThat(lookupIdentifier).isEqualTo(adapterIdentifier)
        assertThat(lookupIdentifier.hashCode()).isEqualTo(adapterIdentifier.hashCode())
    }

    @Test
    fun isOf_matchingCamera_returnsTrue() {
        val identifier = CameraIdentifier.Factory.create(CAMERA_ID_0)

        // camera.cameraInfo.getCameraIdentifier() should now return the identifier
        assertThat(identifier.isOf(fakeCamera0)).isTrue()
    }

    @Test
    fun isOf_nonMatchingCamera_returnsFalse() {
        val identifier =
            CameraIdentifier.Factory.create(CAMERA_ID_1) // This ID will not match fakeCamera0's ID

        assertThat(identifier.isOf(fakeCamera0)).isFalse()
    }

    @Test
    fun isOf_nullCamera_throwsNullPointerException() {
        val identifier = CameraIdentifier.Factory.create(CAMERA_ID_0)
        assertThrows<NullPointerException> { identifier.isOf(null as Camera) }
    }

    @Test
    fun isOf_matchingCameraInfo_returnsTrue() {
        val identifier = CameraIdentifier.Factory.create(CAMERA_ID_0)

        assertThat(identifier.isOf(fakeCameraInfo0)).isTrue()
    }

    @Test
    fun isOf_nonMatchingCameraInfo_returnsFalse() {
        val identifier = CameraIdentifier.Factory.create(CAMERA_ID_0)

        assertThat(identifier.isOf(fakeCameraInfo1)).isFalse()
    }

    @Test
    fun isOf_nullCameraInfo_throwsNullPointerException() {
        val identifier = CameraIdentifier.Factory.create(CAMERA_ID_0)
        assertThrows<NullPointerException> { identifier.isOf(null as CameraInfo) }
    }

    @Test
    fun concurrentCamera_camerasWithinHaveSingleIdentifiers() {
        // Arrange: Create a ConcurrentCamera with two individual cameras.
        val concurrentCamera = ConcurrentCamera(listOf(fakeCamera0, fakeCamera1))

        // Act: Get the identifiers from the cameras within the ConcurrentCamera.
        val identifiers = concurrentCamera.cameras.map { it.cameraInfo.cameraIdentifier }

        // Assert: Verify that each identifier is a distinct, single-ID identifier.
        assertThat(identifiers).hasSize(2)

        // Check the first camera's identifier
        val id0 = identifiers[0]!!
        assertThat(id0.cameraIds).containsExactly(CameraIdentifier.CompositeCameraId(CAMERA_ID_0))
        assertThat(id0.compatibilityId).isNull()

        // Check the second camera's identifier
        val id1 = identifiers[1]!!
        assertThat(id1.cameraIds).containsExactly(CameraIdentifier.CompositeCameraId(CAMERA_ID_1))
        assertThat(id1.compatibilityId).isNull()

        // The identifiers for the individual cameras should not be equal.
        assertThat(id0).isNotEqualTo(id1)
    }

    @Test
    fun getPhysicalCameraIds_singleId_returnsCorrectList() {
        val identifier = CameraIdentifier.Factory.create(CAMERA_ID_0)
        assertThat(identifier.cameraIds)
            .containsExactly(CameraIdentifier.CompositeCameraId(CAMERA_ID_0))
    }

    @Test
    fun getPhysicalCameraIds_multipleIds_returnsCorrectListInOrder() {
        val ids =
            listOf(
                CameraIdentifier.CompositeCameraId(CAMERA_ID_0),
                CameraIdentifier.CompositeCameraId(CAMERA_ID_1),
                CameraIdentifier.CompositeCameraId(CAMERA_ID_2),
            )
        val identifier = CameraIdentifier.Factory.create(ids, null)
        assertThat(identifier.cameraIds)
            .containsExactly(
                CameraIdentifier.CompositeCameraId(CAMERA_ID_0),
                CameraIdentifier.CompositeCameraId(CAMERA_ID_1),
                CameraIdentifier.CompositeCameraId(CAMERA_ID_2),
            )
            .inOrder()
    }

    @Test
    fun getCompatibilityId_withId_returnsCorrectId() {
        val compatId = Identifier.create(Any())
        val identifier =
            CameraIdentifier.Factory.create(
                listOf(CameraIdentifier.CompositeCameraId(CAMERA_ID_0)),
                compatId,
            )
        assertThat(identifier.compatibilityId).isEqualTo(compatId)
    }

    @Test
    fun getCompatibilityId_withoutId_returnsNull() {
        val identifier = CameraIdentifier.Factory.create(CAMERA_ID_0)
        assertThat(identifier.compatibilityId).isNull()
    }

    @Test
    fun getInternalId_singleCameraIdentifier_returnsCorrectId() {
        val identifier = CameraIdentifier.Factory.create(CAMERA_ID_0)
        assertThat(identifier.internalId).isEqualTo(CAMERA_ID_0)
    }

    @Test
    fun getInternalId_multiCameraIdentifier_throwsIllegalStateException() {
        val identifier =
            CameraIdentifier.Factory.create(
                listOf(
                    CameraIdentifier.CompositeCameraId(CAMERA_ID_0),
                    CameraIdentifier.CompositeCameraId(CAMERA_ID_1),
                ),
                null,
            )
        assertThrows<IllegalStateException> { identifier.internalId }
    }

    @Test
    fun equals_differentClass_returnsFalse() {
        val id = CameraIdentifier.Factory.create(CAMERA_ID_0)
        assertThat(id).isNotEqualTo("some_string")
    }

    @Test
    fun equals_compareToNull_returnsFalse() {
        val id = CameraIdentifier.Factory.create(CAMERA_ID_0)
        assertThat(id).isNotEqualTo(null)
    }

    @Test
    fun hashCode_sameObjects_areEqual() {
        val id1 =
            CameraIdentifier.Factory.create(
                listOf(
                    CameraIdentifier.CompositeCameraId(CAMERA_ID_0),
                    CameraIdentifier.CompositeCameraId(CAMERA_ID_1),
                ),
                Identifier.create("A"),
            )
        val id2 =
            CameraIdentifier.Factory.create(
                listOf(
                    CameraIdentifier.CompositeCameraId(CAMERA_ID_0),
                    CameraIdentifier.CompositeCameraId(CAMERA_ID_1),
                ),
                Identifier.create("A"),
            )
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode())
    }

    @Test
    fun hashCode_differentPhysicalIds_areDifferent() {
        val id1 =
            CameraIdentifier.Factory.create(
                listOf(CameraIdentifier.CompositeCameraId(CAMERA_ID_0)),
                Identifier.create("A"),
            )
        val id2 =
            CameraIdentifier.Factory.create(
                listOf(CameraIdentifier.CompositeCameraId(CAMERA_ID_1)),
                Identifier.create("A"),
            )
        assertThat(id1.hashCode()).isNotEqualTo(id2.hashCode())
    }

    @Test
    fun hashCode_differentCompatibilityIds_areDifferent() {
        val id1 =
            CameraIdentifier.Factory.create(
                listOf(CameraIdentifier.CompositeCameraId(CAMERA_ID_0)),
                Identifier.create("A"),
            )
        val id2 =
            CameraIdentifier.Factory.create(
                listOf(CameraIdentifier.CompositeCameraId(CAMERA_ID_0)),
                Identifier.create("B"),
            )
        assertThat(id1.hashCode()).isNotEqualTo(id2.hashCode())
    }

    @Test
    fun hashCode_oneNullCompatibilityIdOtherNonNull_areDifferent() {
        val id1 =
            CameraIdentifier.Factory.create(
                listOf(CameraIdentifier.CompositeCameraId(CAMERA_ID_0)),
                null,
            )
        val id2 =
            CameraIdentifier.Factory.create(
                listOf(CameraIdentifier.CompositeCameraId(CAMERA_ID_0)),
                Identifier.create("A"),
            )
        assertThat(id1.hashCode()).isNotEqualTo(id2.hashCode())
    }

    @Test
    fun toString_singleCameraId_containsId() {
        val id = CameraIdentifier.Factory.create(CAMERA_ID_0)
        assertThat(id.toString()).contains("cameraIds=$CAMERA_ID_0")
        assertThat(id.toString()).doesNotContain("compatId")
    }

    @Test
    fun toString_multipleCameraIds_containsIdsInOrder() {
        val cameraIds =
            listOf(
                CameraIdentifier.CompositeCameraId(CAMERA_ID_0),
                CameraIdentifier.CompositeCameraId(CAMERA_ID_1),
            )
        val id = CameraIdentifier.Factory.create(cameraIds, null)
        assertThat(id.toString()).contains("cameraIds=$CAMERA_ID_0,$CAMERA_ID_1")
    }

    @Test
    fun toString_withCompatibilityId_containsCompatibilityId() {
        val compatId = Identifier.create("extension_id")
        val id = CameraIdentifier.Factory.create(CAMERA_ID_0, null, compatId)
        assertThat(id.toString()).contains("compatId=$compatId")
    }

    @Test
    fun equals_differentPhysicalCameraIds_returnsFalse() {
        val id1 =
            CameraIdentifier.Factory.create(
                listOf(CameraIdentifier.CompositeCameraId(CAMERA_ID_0, PHYSICAL_CAMERA_ID_0))
            )
        val id2 =
            CameraIdentifier.Factory.create(
                listOf(CameraIdentifier.CompositeCameraId(CAMERA_ID_0, PHYSICAL_CAMERA_ID_1))
            )
        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun hashCode_differentPhysicalCameraIds_areDifferent() {
        val id1 =
            CameraIdentifier.Factory.create(
                listOf(CameraIdentifier.CompositeCameraId(CAMERA_ID_0, PHYSICAL_CAMERA_ID_0))
            )
        val id2 =
            CameraIdentifier.Factory.create(
                listOf(CameraIdentifier.CompositeCameraId(CAMERA_ID_0, PHYSICAL_CAMERA_ID_1))
            )
        assertThat(id1.hashCode()).isNotEqualTo(id2.hashCode())
    }

    @Test
    fun toString_withPhysicalCameraIds_containsPhysicalIds() {
        val id =
            CameraIdentifier.Factory.create(
                listOf(CameraIdentifier.CompositeCameraId(CAMERA_ID_0, PHYSICAL_CAMERA_ID_0))
            )
        assertThat(id.toString()).contains("$CAMERA_ID_0(physical=$PHYSICAL_CAMERA_ID_0)")
    }
}

private class FakePhysicalCameraInfo(
    cameraId: String,
    private val physicalCameraId: String? = null,
    private val physicalCameraInfos: Set<CameraInfo> = emptySet(),
) : ForwardingCameraInfo(FakeCameraInfoInternal(cameraId)) {
    override fun getCameraSelector(): CameraSelector {
        val base = super.getCameraSelector()
        return if (physicalCameraId != null) {
            CameraSelector.Builder.fromSelector(base).setPhysicalCameraId(physicalCameraId).build()
        } else {
            base
        }
    }

    override fun getPhysicalCameraInfos(): Set<CameraInfo> = physicalCameraInfos
}
