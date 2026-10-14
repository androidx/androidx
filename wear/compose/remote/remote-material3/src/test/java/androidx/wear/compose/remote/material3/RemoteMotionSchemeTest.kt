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

package androidx.wear.compose.remote.material3

import android.content.Context
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.RemoteAnimationSpec
import androidx.compose.remote.creation.compose.state.remoteSpring
import androidx.compose.runtime.Composable
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.remote.material3.samples.RemoteCheckboxButtonSample
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RemoteMotionSchemeTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun standard_scheme_matches_wear_motion_scheme() {
        val scheme = RemoteMotionScheme.standard()

        assertEquals(remoteSpring(stiffness = 500f, dampingRatio = 1f), scheme.defaultSpatialSpec())
        assertEquals(remoteSpring(stiffness = 1400f, dampingRatio = 1f), scheme.fastSpatialSpec())
        assertEquals(remoteSpring(stiffness = 260f, dampingRatio = 1f), scheme.slowSpatialSpec())
        assertEquals(remoteSpring(stiffness = 500f, dampingRatio = 1f), scheme.defaultEffectsSpec())
        assertEquals(remoteSpring(stiffness = 1400f, dampingRatio = 1f), scheme.fastEffectsSpec())
        assertEquals(remoteSpring(stiffness = 260f, dampingRatio = 1f), scheme.slowEffectsSpec())
    }

    @Test
    fun expressive_scheme_matches_wear_motion_scheme() {
        val scheme = RemoteMotionScheme.expressive()

        assertEquals(
            remoteSpring(stiffness = 350f, dampingRatio = 0.75f),
            scheme.defaultSpatialSpec(),
        )
        assertEquals(remoteSpring(stiffness = 800f, dampingRatio = 0.7f), scheme.fastSpatialSpec())
        assertEquals(remoteSpring(stiffness = 200f, dampingRatio = 0.8f), scheme.slowSpatialSpec())
        assertEquals(remoteSpring(stiffness = 500f, dampingRatio = 1f), scheme.defaultEffectsSpec())
        assertEquals(remoteSpring(stiffness = 1400f, dampingRatio = 1f), scheme.fastEffectsSpec())
        assertEquals(remoteSpring(stiffness = 260f, dampingRatio = 1f), scheme.slowEffectsSpec())
    }

    @Test
    fun theme_provides_standard_scheme_by_default() {
        var motionScheme: RemoteMotionScheme? = null

        capture { RemoteMaterialTheme { motionScheme = RemoteMaterialTheme.motionScheme } }

        assertSame(RemoteMotionScheme.standard(), motionScheme)
    }

    @Test
    fun theme_provides_given_scheme() {
        var motionScheme: RemoteMotionScheme? = null

        capture {
            RemoteMaterialTheme(motionScheme = RemoteMotionScheme.expressive()) {
                motionScheme = RemoteMaterialTheme.motionScheme
            }
        }

        assertSame(RemoteMotionScheme.expressive(), motionScheme)
    }

    @Test
    fun nested_theme_inherits_scheme() {
        var motionScheme: RemoteMotionScheme? = null

        capture {
            RemoteMaterialTheme(motionScheme = RemoteMotionScheme.expressive()) {
                RemoteMaterialTheme { motionScheme = RemoteMaterialTheme.motionScheme }
            }
        }

        assertSame(RemoteMotionScheme.expressive(), motionScheme)
    }

    @Test
    fun selection_control_animates_with_theme_scheme() {
        val slowScheme =
            object : RemoteMotionScheme by RemoteMotionScheme.standard() {
                override fun fastEffectsSpec(): RemoteAnimationSpec = slowEffectsSpec()
            }

        val defaultDocument = capture { RemoteMaterialTheme { RemoteCheckboxButtonSample() } }
        val standardDocument = capture {
            RemoteMaterialTheme(motionScheme = RemoteMotionScheme.standard()) {
                RemoteCheckboxButtonSample()
            }
        }
        val slowDocument = capture {
            RemoteMaterialTheme(motionScheme = slowScheme) { RemoteCheckboxButtonSample() }
        }

        assertArrayEquals(defaultDocument, standardDocument)
        assertFalse(defaultDocument.contentEquals(slowDocument))
    }

    private fun capture(content: @Composable @RemoteComposable () -> Unit): ByteArray =
        runBlocking {
            captureSingleRemoteDocument(context = context, content = content).bytes
        }
}
