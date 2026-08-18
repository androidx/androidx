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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 29)
class SceneCoreOpenXrNativeTest {

    @Test
    fun create_createsNativeInstance() {
        val wrapper = SceneCoreOpenXrNative()
        assertThat(wrapper.nativeScenecore).isNotEqualTo(INVALID_HANDLE)
        wrapper.destroy()
    }

    @Test
    fun use_destroysNativeInstance() {
        var wrapperRef: SceneCoreOpenXrNative? = null
        SceneCoreOpenXrNative().use { wrapper ->
            wrapperRef = wrapper
            assertThat(wrapper.nativeScenecore).isNotEqualTo(INVALID_HANDLE)
        }
        assertThat(wrapperRef?.nativeScenecore).isEqualTo(INVALID_HANDLE)
    }

    @Test
    fun destroy_cleansUpHandleAndSetsToZero() {
        val nativeWrapper = SceneCoreOpenXrNative()
        assertThat(nativeWrapper.nativeScenecore).isNotEqualTo(INVALID_HANDLE)
        nativeWrapper.destroy()
        assertThat(nativeWrapper.nativeScenecore).isEqualTo(INVALID_HANDLE)
    }

    @Test
    fun multipleInstances_createAndDestroy_succeeds() {
        val instance1 = SceneCoreOpenXrNative()
        val instance2 = SceneCoreOpenXrNative()
        assertThat(instance1.nativeScenecore).isNotEqualTo(INVALID_HANDLE)
        assertThat(instance2.nativeScenecore).isNotEqualTo(INVALID_HANDLE)
        instance1.destroy()
        instance2.destroy()
        assertThat(instance1.nativeScenecore).isEqualTo(INVALID_HANDLE)
        assertThat(instance2.nativeScenecore).isEqualTo(INVALID_HANDLE)
    }

    @Test
    fun initWithNullHandles_returnsFalse() {
        val nativeWrapper = SceneCoreOpenXrNative()
        val success = nativeWrapper.init(INVALID_HANDLE, INVALID_HANDLE, INVALID_HANDLE)
        assertThat(success).isFalse()
        nativeWrapper.destroy()
    }

    @Test
    fun getSpatialContainerHandle_beforeCreate_returnsZero() {
        val nativeWrapper = SceneCoreOpenXrNative()
        assertThat(nativeWrapper.getSpatialContainerHandle()).isEqualTo(INVALID_HANDLE)
        nativeWrapper.destroy()
    }

    @Test
    fun getRootSpaceHandle_beforeCreate_returnsZero() {
        val nativeWrapper = SceneCoreOpenXrNative()
        assertThat(nativeWrapper.getRootSpaceHandle()).isEqualTo(INVALID_HANDLE)
        nativeWrapper.destroy()
    }

    @Test
    fun shutdown_whenNotInitialized_isSafe() {
        val nativeWrapper = SceneCoreOpenXrNative()
        nativeWrapper.shutdown()
        nativeWrapper.destroy()
    }

    @Test
    fun methods_afterDestroy_throwIllegalStateException() {
        val nativeWrapper = SceneCoreOpenXrNative()
        nativeWrapper.destroy()
        assertThrows(IllegalStateException::class.java) { nativeWrapper.init(1L, 1L, 1L) }
        assertThrows(IllegalStateException::class.java) { nativeWrapper.createSpatialContainer() }
        assertThrows(IllegalStateException::class.java) {
            nativeWrapper.getSpatialContainerHandle()
        }
        assertThrows(IllegalStateException::class.java) { nativeWrapper.getRootSpaceHandle() }
        assertThrows(IllegalStateException::class.java) { nativeWrapper.createSceneEntity() }
        assertThrows(IllegalStateException::class.java) { nativeWrapper.destroySceneEntity(1L) }
        assertThrows(IllegalStateException::class.java) { nativeWrapper.getRootEntityHandle() }
        assertThrows(IllegalStateException::class.java) { nativeWrapper.createSceneTransaction() }
        assertThrows(IllegalStateException::class.java) {
            nativeWrapper.setTransactionTransform(1L, 2L, Pose(), Vector3(1f, 1f, 1f))
        }
        assertThrows(IllegalStateException::class.java) {
            nativeWrapper.setTransactionParent(1L, 2L, 3L)
        }
        assertThrows(IllegalStateException::class.java) { nativeWrapper.submitSceneTransaction(1L) }
        assertThrows(IllegalStateException::class.java) { nativeWrapper.cancelSceneTransaction(1L) }
        assertThrows(IllegalStateException::class.java) { nativeWrapper.openTransaction() }
    }

    @Test
    fun openTransaction_createsTransaction() {
        val nativeWrapper = SceneCoreOpenXrNative()
        val tx = nativeWrapper.openTransaction()
        assertThat(tx).isNotNull()
        assertThat(tx.isAvailable).isFalse()
        nativeWrapper.destroy()
    }

    @Test
    fun uninitializedMethods_returnSafeDefaults() {
        val nativeWrapper = SceneCoreOpenXrNative()
        assertThat(nativeWrapper.createSceneEntity()).isEqualTo(INVALID_HANDLE)
        assertThat(nativeWrapper.destroySceneEntity(1L)).isFalse()
        assertThat(nativeWrapper.getRootEntityHandle()).isEqualTo(INVALID_HANDLE)
        assertThat(nativeWrapper.createSceneTransaction()).isEqualTo(INVALID_HANDLE)
        assertThat(nativeWrapper.setTransactionTransform(1L, 2L, Pose(), Vector3(1f, 1f, 1f)))
            .isFalse()
        assertThat(nativeWrapper.setTransactionParent(1L, 2L, 3L)).isFalse()
        assertThat(nativeWrapper.submitSceneTransaction(1L)).isFalse()
        assertThat(nativeWrapper.cancelSceneTransaction(1L)).isFalse()
        nativeWrapper.destroy()
    }
}
