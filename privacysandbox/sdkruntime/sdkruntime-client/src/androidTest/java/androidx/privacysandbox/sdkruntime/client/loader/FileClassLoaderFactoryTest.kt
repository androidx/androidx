/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.sdkruntime.client.loader

import android.content.Context
import androidx.privacysandbox.sdkruntime.client.TestSdkConfigs
import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfig
import androidx.privacysandbox.sdkruntime.client.loader.storage.LocalSdkDexFiles
import androidx.privacysandbox.sdkruntime.client.loader.storage.LocalSdkStorage
import androidx.privacysandbox.sdkruntime.client.loader.storage.TestLocalSdkStorage
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class FileClassLoaderFactoryTest {

    private lateinit var testSdkConfig: LocalSdkConfig

    @Before
    fun setUp() {
        testSdkConfig = TestSdkConfigs.CURRENT
    }

    @Test
    fun createClassLoaderFor_whenSdkStorageReturnFiles_returnClassloaderAndNotDelegateToFallback() {
        val sdkDexFiles = extractTestSdkDexFiles()
        val fallback = TestFallbackFactory()

        val fileClassLoaderFactory = FileClassLoaderFactory(
            StubSdkStorage(result = sdkDexFiles),
            fallback
        )

        val classLoader = fileClassLoaderFactory.createClassLoaderFor(
            testSdkConfig,
            javaClass.classLoader!!.parent!!
        )

        val loadedEntryPointClass = classLoader.loadClass(testSdkConfig.entryPoint)
        assertThat(loadedEntryPointClass.classLoader).isEqualTo(classLoader)

        assertThat(fallback.loadSdkCalled).isFalse()
    }

    @Test
    fun createClassLoaderFor_whenSdkStorageReturnNull_delegateToFallback() {
        val fallback = TestFallbackFactory(testSdkConfig, javaClass.classLoader!!.parent)
        val fileClassLoaderFactory = FileClassLoaderFactory(
            StubSdkStorage(result = null),
            fallback
        )

        fileClassLoaderFactory.createClassLoaderFor(
            testSdkConfig,
            javaClass.classLoader!!.parent!!
        )

        assertThat(fallback.loadSdkCalled).isTrue()
    }

    @Test
    fun createClassLoaderFor_whenSdkStorageThrows_delegateToFallback() {
        val fallback = TestFallbackFactory(testSdkConfig, javaClass.classLoader!!.parent)
        val fileClassLoaderFactory = FileClassLoaderFactory(
            ThrowingSdkStorage(exception = Exception("Something wrong")),
            fallback
        )

        fileClassLoaderFactory.createClassLoaderFor(
            testSdkConfig,
            javaClass.classLoader!!.parent!!
        )

        assertThat(fallback.loadSdkCalled).isTrue()
    }

    private class StubSdkStorage(
        private val result: LocalSdkDexFiles?
    ) : LocalSdkStorage {
        override fun dexFilesFor(sdkConfig: LocalSdkConfig) = result
    }

    private class ThrowingSdkStorage(
        private val exception: Exception
    ) : LocalSdkStorage {
        override fun dexFilesFor(sdkConfig: LocalSdkConfig): LocalSdkDexFiles? {
            throw exception
        }
    }

    private class TestFallbackFactory(
        private val expectedSdkConfig: LocalSdkConfig? = null,
        private val expectedParent: ClassLoader? = null,
    ) : SdkLoader.ClassLoaderFactory {

        var loadSdkCalled: Boolean = false

        override fun createClassLoaderFor(
            sdkConfig: LocalSdkConfig,
            parent: ClassLoader
        ): ClassLoader {
            assertThat(sdkConfig).isEqualTo(expectedSdkConfig)
            assertThat(parent).isEqualTo(expectedParent)
            loadSdkCalled = true
            return parent
        }
    }

    private fun extractTestSdkDexFiles(): LocalSdkDexFiles {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val testStorage = TestLocalSdkStorage(
            context,
            rootFolder = File(context.cacheDir, "FileClassLoaderFactoryTest")
        )

        return testStorage.dexFilesFor(testSdkConfig)
    }
}
