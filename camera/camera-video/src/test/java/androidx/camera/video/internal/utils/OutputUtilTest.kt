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

package androidx.camera.video.internal.utils

import android.content.ContentResolver
import android.content.Context
import androidx.camera.video.internal.utils.FakeMediaContentProvider.Companion.AUTHORITY
import androidx.camera.video.internal.utils.FakeMediaContentProvider.Companion.DATA_COLUMN
import androidx.camera.video.internal.utils.FakeMediaContentProvider.Companion.ENTRY_URI
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class OutputUtilTest {

    @get:Rule
    val temporaryFolder =
        TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    private val resolver: ContentResolver =
        ApplicationProvider.getApplicationContext<Context>().contentResolver
    private lateinit var provider: FakeMediaContentProvider

    @Before
    fun setUp() {
        provider = Robolectric.setupContentProvider(FakeMediaContentProvider::class.java, AUTHORITY)
    }

    @Test
    fun getAbsolutePathFromUri_returnsPath() {
        val path = File(temporaryFolder.root, "video.mp4").absolutePath
        provider.dataPath = path

        assertThat(OutputUtil.getAbsolutePathFromUri(resolver, ENTRY_URI, DATA_COLUMN))
            .isEqualTo(path)
    }

    @Test
    fun getAbsolutePathFromUri_emptyCursor_returnsNull() {
        provider.dataPath = null

        assertThat(OutputUtil.getAbsolutePathFromUri(resolver, ENTRY_URI, DATA_COLUMN)).isNull()
    }

    @Test
    fun getAbsolutePathFromUri_nullCursor_returnsNull() {
        provider.returnNullCursor = true

        assertThat(OutputUtil.getAbsolutePathFromUri(resolver, ENTRY_URI, DATA_COLUMN)).isNull()
    }

    @Test
    fun createParentFolder_createsMissingParentFolder() {
        val file = File(temporaryFolder.root, "missing/video.mp4")

        assertThat(OutputUtil.createParentFolder(file)).isTrue()
        assertThat(requireNotNull(file.parentFile).isDirectory).isTrue()
    }

    @Test
    fun createParentFolder_parentIsFile_returnsFalse() {
        val parent = temporaryFolder.newFile("not_a_folder")

        assertThat(OutputUtil.createParentFolder(File(parent, "video.mp4"))).isFalse()
    }
}
