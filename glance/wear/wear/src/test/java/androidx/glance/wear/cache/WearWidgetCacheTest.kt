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
import androidx.glance.wear.core.WidgetInstanceId
import com.google.common.truth.Truth.assertThat
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
    fun setAndGetContainerSpec_restoresValue() = runTest {
        val spec = WidgetContainerSpec(100f, 200f)

        cacheUnderTest.update { setContainerSpec(ContainerInfo.CONTAINER_TYPE_LARGE, spec) }
        val readSpec = cacheUnderTest.getContainerSpec(ContainerInfo.CONTAINER_TYPE_LARGE)

        assertThat(readSpec).isEqualTo(spec)
    }

    @Test
    fun setAndGetContainerSpec_withMultipleTypes_restoresValues() = runTest {
        val spec1 = WidgetContainerSpec(300f, 400f)
        val spec2 = WidgetContainerSpec(100f, 200f)

        cacheUnderTest.update {
            setContainerSpec(ContainerInfo.CONTAINER_TYPE_LARGE, spec1)
            setContainerSpec(ContainerInfo.CONTAINER_TYPE_SMALL, spec2)
        }
        val readSpec1 = cacheUnderTest.getContainerSpec(ContainerInfo.CONTAINER_TYPE_LARGE)
        val readSpec2 = cacheUnderTest.getContainerSpec(ContainerInfo.CONTAINER_TYPE_SMALL)

        assertThat(readSpec1).isEqualTo(spec1)
        assertThat(readSpec2).isEqualTo(spec2)
    }

    @Test
    fun setAndGetContainerSpec_withExistingType_overwritesValue() = runTest {
        val spec1 = WidgetContainerSpec(100f, 200f)
        val spec2 = WidgetContainerSpec(300f, 400f)

        cacheUnderTest.update {
            setContainerSpec(ContainerInfo.CONTAINER_TYPE_LARGE, spec1)
            setContainerSpec(ContainerInfo.CONTAINER_TYPE_LARGE, spec2)
        }
        val readSpec = cacheUnderTest.getContainerSpec(ContainerInfo.CONTAINER_TYPE_LARGE)
        val readOtherSpec = cacheUnderTest.getContainerSpec(ContainerInfo.CONTAINER_TYPE_SMALL)

        assertThat(readSpec).isEqualTo(spec2)
        assertThat(readOtherSpec).isNull()
    }

    @Test
    fun setAndGetInstanceType_restoresValue() = runTest {
        cacheUnderTest.update { setInstanceType(INSTANCE_ID_1, ContainerInfo.CONTAINER_TYPE_LARGE) }

        val readType = cacheUnderTest.getInstanceType(INSTANCE_ID_1)

        assertThat(readType).isEqualTo(ContainerInfo.CONTAINER_TYPE_LARGE)
    }

    @Test
    fun setAndGetInstanceType_withMultipleIds_restoresValues() = runTest {
        cacheUnderTest.update {
            setInstanceType(INSTANCE_ID_1, ContainerInfo.CONTAINER_TYPE_LARGE)
            setInstanceType(INSTANCE_ID_2, ContainerInfo.CONTAINER_TYPE_SMALL)
        }

        val readType1 = cacheUnderTest.getInstanceType(INSTANCE_ID_1)
        val readType2 = cacheUnderTest.getInstanceType(INSTANCE_ID_2)

        assertThat(readType1).isEqualTo(ContainerInfo.CONTAINER_TYPE_LARGE)
        assertThat(readType2).isEqualTo(ContainerInfo.CONTAINER_TYPE_SMALL)
    }

    @Test
    fun setAndGetInstanceType_withExistingId_overwritesValue() = runTest {
        cacheUnderTest.update {
            setInstanceType(INSTANCE_ID_1, ContainerInfo.CONTAINER_TYPE_LARGE)
            setInstanceType(INSTANCE_ID_1, ContainerInfo.CONTAINER_TYPE_SMALL)
        }
        val readType1 = cacheUnderTest.getInstanceType(INSTANCE_ID_1)
        val readType2 = cacheUnderTest.getInstanceType(INSTANCE_ID_2)

        assertThat(readType1).isEqualTo(ContainerInfo.CONTAINER_TYPE_SMALL)
        assertThat(readType2).isNull()
    }

    private companion object {
        const val DATASTORE_FILE_NAME = "test_file.pb"
        val INSTANCE_ID_1 = WidgetInstanceId("namespace1", 1)
        val INSTANCE_ID_2 = WidgetInstanceId("namespace2", 2)
    }
}
