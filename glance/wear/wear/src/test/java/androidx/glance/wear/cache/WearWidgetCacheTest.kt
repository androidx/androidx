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

package androidx.glance.wear.cache

import androidx.datastore.core.DataStoreFactory
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.WearWidgetParams
import androidx.glance.wear.core.WidgetInstanceId
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class WearWidgetCacheTest {

    @get:Rule val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()
    private lateinit var cacheUnderTest: WearWidgetCache

    @Before
    fun setUp() {
        val dataStore =
            DataStoreFactory.create(
                serializer = WearWidgetCacheSerializer,
                produceFile = { tmpFolder.newFile(DATASTORE_FILE_NAME) },
            )
        cacheUnderTest = WearWidgetCache(dataStore)
    }

    @Test
    fun setAndGetWidgetParams_restoresValue() = runTest {
        val params =
            WearWidgetParams(
                instanceId = INSTANCE_ID_1,
                containerType = ContainerInfo.CONTAINER_TYPE_LARGE,
                widthDp = 100f,
                heightDp = 200f,
                horizontalPaddingDp = 10f,
                verticalPaddingDp = 20f,
                cornerRadiusDp = 5f,
            )

        cacheUnderTest.update { setWidgetParams(params) }
        val restoredParams =
            cacheUnderTest.getWidgetParams(ContainerInfo.CONTAINER_TYPE_LARGE, INSTANCE_ID_1)

        assertThat(restoredParams).isEqualTo(params)
    }

    @Test
    fun setAndGetWidgetParams_withMultipleTypes_restoresValues() = runTest {
        val params1 =
            WearWidgetParams(
                instanceId = INSTANCE_ID_1,
                containerType = ContainerInfo.CONTAINER_TYPE_LARGE,
                widthDp = 300f,
                heightDp = 400f,
                horizontalPaddingDp = 30f,
                verticalPaddingDp = 40f,
                cornerRadiusDp = 15f,
            )
        val params2 =
            WearWidgetParams(
                instanceId = INSTANCE_ID_2,
                containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 100f,
                heightDp = 200f,
                horizontalPaddingDp = 10f,
                verticalPaddingDp = 20f,
                cornerRadiusDp = 5f,
            )

        cacheUnderTest.update {
            setWidgetParams(params1)
            setWidgetParams(params2)
        }
        val restoredParams1 =
            cacheUnderTest.getWidgetParams(ContainerInfo.CONTAINER_TYPE_LARGE, INSTANCE_ID_1)
        val restoredParams2 =
            cacheUnderTest.getWidgetParams(ContainerInfo.CONTAINER_TYPE_SMALL, INSTANCE_ID_2)

        assertThat(restoredParams1).isEqualTo(params1)
        assertThat(restoredParams2).isEqualTo(params2)
    }

    @Test
    fun setAndGetWidgetParams_withExistingType_overwritesValue() = runTest {
        val params1 =
            WearWidgetParams(
                instanceId = INSTANCE_ID_1,
                containerType = ContainerInfo.CONTAINER_TYPE_LARGE,
                widthDp = 100f,
                heightDp = 200f,
                horizontalPaddingDp = 10f,
                verticalPaddingDp = 20f,
                cornerRadiusDp = 5f,
            )
        val params2 =
            WearWidgetParams(
                instanceId = INSTANCE_ID_1,
                containerType = ContainerInfo.CONTAINER_TYPE_LARGE,
                widthDp = 300f,
                heightDp = 400f,
                horizontalPaddingDp = 30f,
                verticalPaddingDp = 40f,
                cornerRadiusDp = 15f,
            )

        cacheUnderTest.update { setWidgetParams(params1) }
        assertThat(
                cacheUnderTest.getWidgetParams(ContainerInfo.CONTAINER_TYPE_LARGE, INSTANCE_ID_1)
            )
            .isEqualTo(params1)

        cacheUnderTest.update { setWidgetParams(params2) }
        assertThat(
                cacheUnderTest.getWidgetParams(ContainerInfo.CONTAINER_TYPE_LARGE, INSTANCE_ID_1)
            )
            .isEqualTo(params2)
    }

    @Test
    fun getWidgetParams_cacheMiss_throwsException() = runTest {
        assertFailsWith<WearWidgetCache.WidgetCacheMissException> {
            cacheUnderTest.getWidgetParams(ContainerInfo.CONTAINER_TYPE_LARGE, INSTANCE_ID_1)
        }
    }

    @Test
    fun setAndGetContainerTypeForInstance_restoresValue() = runTest {
        cacheUnderTest.update {
            setContainerTypeForInstance(INSTANCE_ID_1, ContainerInfo.CONTAINER_TYPE_LARGE)
        }

        val readType = cacheUnderTest.getContainerTypeForInstance(INSTANCE_ID_1)

        assertThat(readType).isEqualTo(ContainerInfo.CONTAINER_TYPE_LARGE)
    }

    @Test
    fun setAndGetContainerTypeForInstance_withMultipleIds_restoresValues() = runTest {
        cacheUnderTest.update {
            setContainerTypeForInstance(INSTANCE_ID_1, ContainerInfo.CONTAINER_TYPE_LARGE)
            setContainerTypeForInstance(INSTANCE_ID_2, ContainerInfo.CONTAINER_TYPE_SMALL)
        }

        val readType1 = cacheUnderTest.getContainerTypeForInstance(INSTANCE_ID_1)
        val readType2 = cacheUnderTest.getContainerTypeForInstance(INSTANCE_ID_2)

        assertThat(readType1).isEqualTo(ContainerInfo.CONTAINER_TYPE_LARGE)
        assertThat(readType2).isEqualTo(ContainerInfo.CONTAINER_TYPE_SMALL)
    }

    @Test
    fun setAndGetContainerTypeForInstance_withExistingId_overwritesValue() = runTest {
        cacheUnderTest.update {
            setContainerTypeForInstance(INSTANCE_ID_1, ContainerInfo.CONTAINER_TYPE_LARGE)
        }
        assertThat(cacheUnderTest.getContainerTypeForInstance(INSTANCE_ID_1))
            .isEqualTo(ContainerInfo.CONTAINER_TYPE_LARGE)

        cacheUnderTest.update {
            setContainerTypeForInstance(INSTANCE_ID_1, ContainerInfo.CONTAINER_TYPE_SMALL)
        }
        assertThat(cacheUnderTest.getContainerTypeForInstance(INSTANCE_ID_1))
            .isEqualTo(ContainerInfo.CONTAINER_TYPE_SMALL)
    }

    @Test
    fun getContainerTypeForInstance_cacheMiss_throwsException() = runTest {
        assertFailsWith<WearWidgetCache.WidgetCacheMissException> {
            cacheUnderTest.getContainerTypeForInstance(INSTANCE_ID_1)
        }
    }

    private companion object {
        const val DATASTORE_FILE_NAME = "test_file.pb"
        val INSTANCE_ID_1 = WidgetInstanceId("namespace1", 1)
        val INSTANCE_ID_2 = WidgetInstanceId("namespace2", 2)
    }
}
