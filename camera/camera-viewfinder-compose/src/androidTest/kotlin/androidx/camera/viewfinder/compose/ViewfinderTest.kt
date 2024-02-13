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

package androidx.camera.viewfinder.compose

import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.viewfinder.surface.ImplementationMode
import androidx.camera.viewfinder.surface.TransformationInfo
import androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ViewfinderTest {
    @get:Rule
    val rule = createComposeRule()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    @Test
    fun canRetrievePerformanceSurface() = runBlocking {
        assertCanRetrieveSurface(implementationMode = ImplementationMode.PERFORMANCE)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    @Test
    fun canRetrieveCompatibleSurface() = runBlocking {
        assertCanRetrieveSurface(implementationMode = ImplementationMode.COMPATIBLE)
    }

    @RequiresApi(Build.VERSION_CODES.M) // Needed for Surface.lockHardwareCanvas()
    private suspend fun assertCanRetrieveSurface(implementationMode: ImplementationMode) {
        val surfaceDeferred = CompletableDeferred<Surface>()
        val surfaceRequest = ViewfinderSurfaceRequest.Builder(TEST_RESOLUTION).build()
        rule.setContent {
            Viewfinder(
                modifier = Modifier.size(TEST_VIEWFINDER_SIZE),
                surfaceRequest = surfaceRequest,
                transformationInfo = TEST_TRANSFORMATION_INFO,
                implementationMode = implementationMode
            )

            LaunchedEffect(Unit) {
                surfaceDeferred.complete(surfaceRequest.getSurface())
            }
        }

        val surface = surfaceDeferred.await()
        surface.lockHardwareCanvas().apply {
            try {
                assertThat(Size(width, height)).isEqualTo(TEST_RESOLUTION)
            } finally {
                surface.unlockCanvasAndPost(this)
                surfaceRequest.markSurfaceSafeToRelease()
            }
        }
    }

    companion object {
        val TEST_VIEWFINDER_SIZE = DpSize(360.dp, 640.dp)
        val TEST_RESOLUTION = Size(1080, 1920)
        val TEST_TRANSFORMATION_INFO = TransformationInfo(
            sourceRotation = 0,
            cropRectLeft = 0,
            cropRectRight = TEST_RESOLUTION.width,
            cropRectTop = 0,
            cropRectBottom = TEST_RESOLUTION.height,
            shouldMirror = false
        )
    }
}
