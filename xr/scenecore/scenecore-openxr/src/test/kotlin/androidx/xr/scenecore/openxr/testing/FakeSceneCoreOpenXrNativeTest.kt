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

package androidx.xr.scenecore.openxr.testing

import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.openxr.INVALID_HANDLE
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeSceneCoreOpenXrNativeTest {

    @Test
    fun init_withValidHandles_succeeds() {
        val fake = FakeSceneCoreOpenXrNative()
        val result = fake.init(xrInstanceHandle = 100L, xrSessionHandle = 200L, gipaHandle = 300L)

        assertThat(result).isTrue()
        assertThat(fake.isInitialized.get()).isTrue()
    }

    @Test
    fun init_withInvalidHandles_returnsFalse() {
        val fake = FakeSceneCoreOpenXrNative()
        assertThat(fake.init(INVALID_HANDLE, 200L, 300L)).isFalse()
        assertThat(fake.init(100L, INVALID_HANDLE, 300L)).isFalse()
        assertThat(fake.init(100L, 200L, INVALID_HANDLE)).isFalse()
        assertThat(fake.isInitialized.get()).isFalse()
    }

    @Test
    fun operationsBeforeInit_throwIllegalStateException() {
        val fake = FakeSceneCoreOpenXrNative()
        assertThrows(IllegalStateException::class.java) { fake.createSpatialContainer() }
        assertThrows(IllegalStateException::class.java) { fake.getSpatialContainerHandle() }
        assertThrows(IllegalStateException::class.java) { fake.getRootSpaceHandle() }
        assertThrows(IllegalStateException::class.java) { fake.getRootEntityHandle() }
        assertThrows(IllegalStateException::class.java) { fake.createSceneEntity() }
        assertThrows(IllegalStateException::class.java) { fake.createSceneTransaction() }
    }

    @Test
    fun createSpatialContainer_succeedsAndSetsHandles() {
        val fake = FakeSceneCoreOpenXrNative()
        fake.init(100L, 200L, 300L)
        fake.createSpatialContainer()

        assertThat(fake.isSpatialContainerCreated.get()).isTrue()
        assertThat(fake.getSpatialContainerHandle()).isEqualTo(fake.fakeSpatialContainerHandle)
        assertThat(fake.getRootSpaceHandle()).isEqualTo(fake.fakeRootSpaceHandle)
        assertThat(fake.getRootEntityHandle()).isEqualTo(fake.fakeRootEntityHandle)
    }

    @Test
    fun createSceneEntity_incrementsAndTracksEntities() {
        val fake = FakeSceneCoreOpenXrNative()
        fake.init(100L, 200L, 300L)
        val handle1 = fake.createSceneEntity()
        val handle2 = fake.createSceneEntity()

        assertThat(handle1).isNotEqualTo(INVALID_HANDLE)
        assertThat(handle2).isNotEqualTo(handle1)
        assertThat(fake.createdEntities).containsExactly(handle1, handle2).inOrder()
    }

    @Test
    fun destroySceneEntity_removesAndTracksDestroyedEntities() {
        val fake = FakeSceneCoreOpenXrNative()
        fake.init(100L, 200L, 300L)
        val handle = fake.createSceneEntity()

        val success = fake.destroySceneEntity(handle)

        assertThat(success).isTrue()
        assertThat(fake.createdEntities).doesNotContain(handle)
        assertThat(fake.destroyedEntities).containsExactly(handle)
    }

    @Test
    fun destroySceneEntity_removesParentAndChildrenRelationships() {
        val fake = FakeSceneCoreOpenXrNative()
        fake.init(100L, 200L, 300L)
        val parent = fake.createSceneEntity()
        val child = fake.createSceneEntity()

        val tx = fake.createSceneTransaction()
        fake.setTransactionParent(tx, child, parent)
        fake.submitSceneTransaction(tx)
        assertThat(fake.entityParents[child]).isEqualTo(parent)

        fake.destroySceneEntity(parent)
        assertThat(fake.entityParents[child]).isNull()
        assertThat(fake.destroyedEntities).contains(parent)
    }

    @Test
    fun sceneTransaction_stagesAndSubmitsChanges() {
        val fake = FakeSceneCoreOpenXrNative()
        fake.init(100L, 200L, 300L)
        val child = fake.createSceneEntity()
        val parent = fake.createSceneEntity()
        val pose = Pose(Vector3(1f, 2f, 3f))
        val scale = Vector3(2f, 2f, 2f)

        val txHandle = fake.createSceneTransaction()
        assertThat(fake.openTransactions).containsKey(txHandle)

        fake.setTransactionTransform(txHandle, child, pose, scale)
        fake.setTransactionParent(txHandle, child, parent)

        // Before submit
        assertThat(fake.entityTransforms[child]).isNull()
        assertThat(fake.entityParents[child]).isNull()

        val submitSuccess = fake.submitSceneTransaction(txHandle)
        assertThat(submitSuccess).isTrue()
        assertThat(fake.openTransactions).doesNotContainKey(txHandle)
        assertThat(fake.committedTransactions).contains(txHandle)

        // After submit
        assertThat(fake.entityTransforms[child]).isEqualTo(Pair(pose, scale))
        assertThat(fake.entityParents[child]).isEqualTo(parent)
    }

    @Test
    fun sceneTransaction_removeParent_unparentsEntity() {
        val fake = FakeSceneCoreOpenXrNative()
        fake.init(100L, 200L, 300L)
        val child = fake.createSceneEntity()
        val parent = fake.createSceneEntity()

        val tx1 = fake.createSceneTransaction()
        fake.setTransactionParent(tx1, child, parent)
        fake.submitSceneTransaction(tx1)
        assertThat(fake.entityParents[child]).isEqualTo(parent)

        val tx2 = fake.createSceneTransaction()
        fake.setTransactionParent(tx2, child, INVALID_HANDLE)
        fake.submitSceneTransaction(tx2)
        assertThat(fake.entityParents[child]).isNull()
    }

    @Test
    fun sceneTransaction_cancel_discardsChanges() {
        val fake = FakeSceneCoreOpenXrNative()
        fake.init(100L, 200L, 300L)
        val entity = fake.createSceneEntity()
        val txHandle = fake.createSceneTransaction()

        fake.setTransactionTransform(txHandle, entity, Pose(), Vector3(1f, 1f, 1f))
        val cancelSuccess = fake.cancelSceneTransaction(txHandle)

        assertThat(cancelSuccess).isTrue()
        assertThat(fake.openTransactions).doesNotContainKey(txHandle)
        assertThat(fake.cancelledTransactions).contains(txHandle)
        assertThat(fake.entityTransforms[entity]).isNull()
    }

    @Test
    fun openTransaction_whenTransactionUnavailable_returnsUnavailableTransaction() {
        val fake = FakeSceneCoreOpenXrNative()
        fake.init(100L, 200L, 300L)
        fake.simulateTransactionUnavailable = true

        val tx = fake.openTransaction()
        assertThat(tx.isAvailable).isFalse()
        assertThat(tx.transactionHandle).isEqualTo(INVALID_HANDLE)
        tx.close()
    }

    @Test
    fun openTransaction_afterDestroy_throwsIllegalStateException() {
        val fake = FakeSceneCoreOpenXrNative()
        fake.init(100L, 200L, 300L)
        fake.destroy()

        assertThrows(IllegalStateException::class.java) { fake.openTransaction() }
    }

    @Test
    fun shutdownAndDestroy_setsStateCorrectlyAndCancelsOpenTransactions() {
        val fake = FakeSceneCoreOpenXrNative()
        fake.init(100L, 200L, 300L)
        val tx1 = fake.createSceneTransaction()
        val tx2 = fake.createSceneTransaction()

        fake.destroy()

        assertThat(fake.isShutdown.get()).isTrue()
        assertThat(fake.isDestroyed.get()).isTrue()
        assertThat(fake.nativeScenecore).isEqualTo(INVALID_HANDLE)
        assertThat(fake.openTransactions).isEmpty()
        assertThat(fake.cancelledTransactions).containsExactly(tx1, tx2)
        assertThrows(IllegalStateException::class.java) { fake.createSceneEntity() }
        assertThrows(IllegalStateException::class.java) { fake.createSceneTransaction() }
    }
}
