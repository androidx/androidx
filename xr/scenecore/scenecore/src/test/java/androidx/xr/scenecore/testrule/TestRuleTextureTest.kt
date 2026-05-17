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

package androidx.xr.scenecore.testrule

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.Texture
import androidx.xr.scenecore.testing.SceneCoreTestRule
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class TestRuleTextureTest {
    @Rule @JvmField val scenecoreTestRule = SceneCoreTestRule()
    private lateinit var session: Session
    private lateinit var activity: ComponentActivity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
        val testDispatcher = StandardTestDispatcher()
        val result =
            Session.create(activity, testDispatcher, lifecycleOwner = activity as LifecycleOwner)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun create_withAbsolutePath_throwsIllegalArgumentException() {
        runBlocking {
            val absolutePath = Paths.get("/absolute/path/to/texture.png")
            val exception =
                assertFailsWith<IllegalArgumentException> { Texture.create(session, absolutePath) }
            assertThat(exception.message).contains("expects a path relative to `assets/`")
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun create_withRelativePath_returnsTextureWithCorrectAssetName() {
        runBlocking {
            val texturePath = Paths.get("test.png")
            val texture = Texture.create(session, texturePath)
            val testTexture = scenecoreTestRule.createTester(texture)

            assertIs<Texture>(texture)
            assertThat(testTexture.path).isEqualTo(texturePath)
        }
    }
}
