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

package androidx.xr.scenecore.openxr

import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.openxr.testing.FakeSceneCoreOpenXrNative
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class OpenXrTransactionTest {

    private lateinit var fakeNative: FakeSceneCoreOpenXrNative

    @Before
    fun setUp() {
        fakeNative = FakeSceneCoreOpenXrNative()
        fakeNative.init(100L, 200L, 300L)
    }

    @Test
    fun isAvailable_validHandle_returnsTrue() {
        val tx = fakeNative.openTransaction()
        assertThat(tx.isAvailable).isTrue()
        tx.close()
    }

    @Test
    fun isAvailable_invalidHandle_returnsFalse() {
        val tx = OpenXrTransaction(fakeNative, INVALID_HANDLE)
        assertThat(tx.isAvailable).isFalse()
        tx.close()
    }

    @Test
    fun setTransform_invalidTransactionHandle_marksStagingErrorAndCommitReturnsFalse() {
        val tx = OpenXrTransaction(fakeNative, INVALID_HANDLE)
        val entity = fakeNative.createSceneEntity()
        tx.setTransform(entity, Pose(), Vector3.One)

        val success = tx.commit()
        assertThat(success).isFalse()
        tx.close()
    }

    @Test
    fun setParent_invalidTransactionHandle_marksStagingErrorAndCommitReturnsFalse() {
        val tx = OpenXrTransaction(fakeNative, INVALID_HANDLE)
        val child = fakeNative.createSceneEntity()
        val parent = fakeNative.createSceneEntity()
        tx.setParent(child, parent)

        val success = tx.commit()
        assertThat(success).isFalse()
        tx.close()
    }

    @Test
    fun setTransform_invalidEntityHandle_throwsIllegalArgumentException() {
        val tx = fakeNative.openTransaction()
        assertThrows(IllegalArgumentException::class.java) {
            tx.setTransform(INVALID_HANDLE, Pose(), Vector3(1f, 1f, 1f))
        }
        tx.close()
    }

    @Test
    fun setParent_invalidEntityHandle_throwsIllegalArgumentException() {
        val tx = fakeNative.openTransaction()
        assertThrows(IllegalArgumentException::class.java) { tx.setParent(INVALID_HANDLE, 100L) }
        tx.close()
    }

    @Test
    fun setTransform_stagesTransform() {
        val entity = fakeNative.createSceneEntity()
        val pose = Pose(Vector3(1f, 2f, 3f))
        val scale = Vector3(2f, 3f, 4f)

        fakeNative.openTransaction().use { tx ->
            tx.setTransform(entity, pose, scale)
            val success = tx.commit()
            assertThat(success).isTrue()
        }

        assertThat(fakeNative.entityTransforms[entity]).isEqualTo(Pair(pose, scale))
    }

    @Test
    fun commit_appliesTransformsAndParentsAtomically() {
        val entity1 = fakeNative.createSceneEntity()
        val entity2 = fakeNative.createSceneEntity()
        val parent = fakeNative.createSceneEntity()

        val pose1 = Pose(Vector3(1f, 2f, 3f))
        val scale1 = Vector3(1f, 1f, 1f)
        val pose2 = Pose(Vector3(4f, 5f, 6f))
        val scale2 = Vector3(2f, 2f, 2f)

        fakeNative.openTransaction().use { tx ->
            tx.setTransform(entity1, pose1, scale1)
            tx.setTransform(entity2, pose2, scale2)
            tx.setParent(entity1, parent)
            tx.setParent(entity2, parent)

            // Before commit, mutations should NOT be applied yet
            assertThat(fakeNative.entityTransforms[entity1]).isNull()
            assertThat(fakeNative.entityTransforms[entity2]).isNull()
            assertThat(fakeNative.entityParents[entity1]).isNull()
            assertThat(fakeNative.entityParents[entity2]).isNull()

            val success = tx.commit()
            assertThat(success).isTrue()
        }

        // After commit, all mutations are applied
        assertThat(fakeNative.entityTransforms[entity1]).isEqualTo(Pair(pose1, scale1))
        assertThat(fakeNative.entityTransforms[entity2]).isEqualTo(Pair(pose2, scale2))
        assertThat(fakeNative.entityParents[entity1]).isEqualTo(parent)
        assertThat(fakeNative.entityParents[entity2]).isEqualTo(parent)
    }

    @Test
    fun commit_withStagingError_cancelsTransactionAndReturnsFalse() {
        val entity = fakeNative.createSceneEntity()
        val tx = fakeNative.openTransaction()

        // Simulate a native failure by removing the open transaction record before staging
        fakeNative.openTransactions.remove(tx.transactionHandle)
        tx.setTransform(entity, Pose(), Vector3(1f, 1f, 1f))

        val success = tx.commit()
        assertThat(success).isFalse()
        tx.close()
    }

    @Test
    fun commit_whenSubmitFails_cancelsTransactionAndReturnsFalse() {
        val tx = fakeNative.openTransaction()
        // Close the native handle to cause submit to fail
        fakeNative.openTransactions.remove(tx.transactionHandle)

        val success = tx.commit()
        assertThat(success).isFalse()
        assertThat(fakeNative.cancelledTransactions).contains(tx.transactionHandle)
    }

    @Test
    fun setParent_withNull_removesParentOnCommit() {
        val child = fakeNative.createSceneEntity()
        val parent = fakeNative.createSceneEntity()

        fakeNative.openTransaction().use { tx ->
            tx.setParent(child, parent)
            tx.commit()
        }
        assertThat(fakeNative.entityParents[child]).isEqualTo(parent)

        fakeNative.openTransaction().use { tx ->
            tx.setParent(child, null)
            tx.commit()
        }
        assertThat(fakeNative.entityParents[child]).isNull()
    }

    @Test
    fun setParent_withInvalidHandle_removesParentOnCommit() {
        val child = fakeNative.createSceneEntity()
        val parent = fakeNative.createSceneEntity()

        fakeNative.openTransaction().use { tx ->
            tx.setParent(child, parent)
            tx.commit()
        }
        assertThat(fakeNative.entityParents[child]).isEqualTo(parent)

        fakeNative.openTransaction().use { tx ->
            tx.setParent(child, INVALID_HANDLE)
            tx.commit()
        }
        assertThat(fakeNative.entityParents[child]).isNull()
    }

    @Test
    fun close_withoutCommit_cancelsTransaction() {
        val entity = fakeNative.createSceneEntity()
        val pose = Pose(Vector3(1f, 2f, 3f))
        val scale = Vector3(1f, 1f, 1f)

        val tx = fakeNative.openTransaction()
        tx.setTransform(entity, pose, scale)
        tx.close()

        // Mutations should NOT be applied
        assertThat(fakeNative.entityTransforms[entity]).isNull()
        assertThat(fakeNative.cancelledTransactions).contains(tx.transactionHandle)
    }

    @Test
    fun close_afterCommit_doesNotCancelTransaction() {
        val entity = fakeNative.createSceneEntity()
        val tx = fakeNative.openTransaction()
        tx.setTransform(entity, Pose(), Vector3(1f, 1f, 1f))
        val commitSuccess = tx.commit()
        assertThat(commitSuccess).isTrue()

        tx.close()
        assertThat(fakeNative.cancelledTransactions).doesNotContain(tx.transactionHandle)
    }

    @Test
    fun close_calledMultipleTimes_isIdempotent() {
        val entity = fakeNative.createSceneEntity()
        val tx = fakeNative.openTransaction()
        tx.setTransform(entity, Pose(), Vector3(1f, 1f, 1f))

        tx.close()
        val cancelCountAfterFirstClose =
            fakeNative.cancelledTransactions.count { it == tx.transactionHandle }
        assertThat(cancelCountAfterFirstClose).isEqualTo(1)

        tx.close()
        val cancelCountAfterSecondClose =
            fakeNative.cancelledTransactions.count { it == tx.transactionHandle }
        assertThat(cancelCountAfterSecondClose).isEqualTo(1)
    }

    @Test
    fun operations_afterClosed_throwIllegalStateException() {
        val tx = fakeNative.openTransaction()
        tx.commit()

        assertThrows(IllegalStateException::class.java) {
            tx.setTransform(1L, Pose(), Vector3(1f, 1f, 1f))
        }
        assertThrows(IllegalStateException::class.java) { tx.setParent(1L, 2L) }
        assertThrows(IllegalStateException::class.java) { tx.commit() }
    }
}
