/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.graph

import android.os.Build
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.core.TokenLockImpl
import androidx.camera.camera2.pipe.testing.FakeGraphProcessor
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class CameraGraphSessionImplTest {
    private val tokenLock = TokenLockImpl(1)

    private val graphState3A = GraphState3A()
    private val graphProcessor = FakeGraphProcessor(graphState3A = graphState3A)
    private val listener3A = Listener3A()
    private val controller3A = Controller3A(graphProcessor, graphState3A, listener3A)

    private val session = CameraGraphSessionImpl(
        tokenLock.acquireOrNull(1, 1)!!,
        graphProcessor,
        controller3A
    )

    @Test
    fun createCameraGraphSession() {
        assertThat(session).isNotNull()
    }

    @Test
    fun sessionCannotBeUsedAfterClose() {
        session.close()

        val result = assertThrows<IllegalStateException> {
            session.submit(Request(listOf()))
        }
        result.hasMessageThat().contains("submit")
    }
}