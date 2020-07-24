/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe.impl

import android.content.Context
import android.os.Build
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.testing.CameraPipeRobolectricTestRunner
import androidx.camera.camera2.pipe.testing.FakeGraphProcessor
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@SmallTest
@RunWith(CameraPipeRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CameraGraphImplTest {
    private val graphProcessor = FakeGraphProcessor()
    private lateinit var impl: CameraGraphImpl

    @Before
    fun setUp() {
        val config = CameraGraph.Config(
            camera = CameraId("0"),
            streams = listOf(),
            defaultTemplate = RequestTemplate(0)
        )
        val context = ApplicationProvider.getApplicationContext() as Context
        impl = CameraGraphImpl(context, config, graphProcessor, StreamMap())
    }

    @Test
    fun createCameraGraphImpl() {
        assertThat(impl).isNotNull()
    }

    @Test
    fun testAcquireSession() = runBlocking {
        val session = impl.acquireSession()
        assertThat(session).isNotNull()
    }

    @Test
    fun testAcquireSessionOrNull() {
        val session = impl.acquireSessionOrNull()
        assertThat(session).isNotNull()
    }

    @Test
    fun testAcquireSessionOrNullAfterAcquireSession() = runBlocking {
        val session = impl.acquireSession()
        assertThat(session).isNotNull()

        // Since a session is already active, an attempt to acquire another session will fail.
        val session1 = impl.acquireSessionOrNull()
        assertThat(session1).isNull()

        // Closing an active session should allow a new session instance to be created.
        session.close()

        val session2 = impl.acquireSessionOrNull()
        assertThat(session2).isNotNull()
    }

    @Test
    fun sessionSubmitsRequestsToGraphProcessor() {
        val session = checkNotNull(impl.acquireSessionOrNull())
        val request = Request(listOf())
        session.submit(request)

        assertThat(graphProcessor.requestQueue).contains(listOf(request))
    }

    @Test
    fun sessionSetsRepeatingRequestOnGraphProcessor() {
        val session = checkNotNull(impl.acquireSessionOrNull())
        val request = Request(listOf())
        session.setRepeating(request)

        assertThat(graphProcessor.repeatingRequest).isSameInstanceAs(request)
    }

    @Test
    fun sessionAbortsRequestOnGraphProcessor() {
        val session = checkNotNull(impl.acquireSessionOrNull())
        val request = Request(listOf())
        session.submit(request)
        session.abort()

        assertThat(graphProcessor.requestQueue).isEmpty()
    }

    @Test
    fun closingSessionDoesNotCloseGraphProcessor() {
        val session = impl.acquireSessionOrNull()
        checkNotNull(session).close()

        assertThat(graphProcessor.closed).isFalse()
    }

    @Test
    fun closingCameraGraphClosesGraphProcessor() {
        impl.close()
        assertThat(graphProcessor.closed).isTrue()
    }

    @Test
    fun stoppingCameraGraphStopsGraphProcessor() {
        assertThat(graphProcessor.active).isFalse()
        impl.start()
        assertThat(graphProcessor.active).isTrue()
        impl.stop()
        assertThat(graphProcessor.active).isFalse()
        impl.start()
        assertThat(graphProcessor.active).isTrue()
        impl.close()
        assertThat(graphProcessor.closed).isTrue()
        assertThat(graphProcessor.active).isFalse()
    }
}