/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.scenecore

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.runtime.RenderingRuntime
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.testing.FakeExrImageResource
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class ExrImageTest {

    private lateinit var sceneRuntime: SceneRuntime
    private lateinit var renderingRuntime: RenderingRuntime
    private lateinit var session: Session

    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        val result = Session.create(activity, testDispatcher)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
        sceneRuntime = session.sceneRuntime
        renderingRuntime = session.renderingRuntime
    }

    @Test
    fun exrImage_createFromZip_failsForExrFile() {
        runBlocking {
            @Suppress("UNUSED_VARIABLE", "NewApi")
            val exception =
                assertFailsWith<IllegalArgumentException> {
                    val unusedExrImage: ExrImage =
                        ExrImage.createFromZip(session, Paths.get("test.exr"))
                }

            assertThat(exception)
                .hasMessageThat()
                .contains("Only preprocessed skybox files with the .zip extension are supported.")
        }
    }

    @Test
    fun exrImage_createFromZip_withZipExtension_passes() {
        runBlocking {
            @Suppress("UNUSED_VARIABLE", "NewApi")
            val exrImage: ExrImage = ExrImage.createFromZip(session, Paths.get("test.zip"))

            assertIs<ExrImage>(exrImage)
            assertThat((exrImage.image as FakeExrImageResource).assetName).isEqualTo("test.zip")
        }
    }
}
