/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.watchface.client

import android.os.IBinder
import androidx.wear.watchface.control.IHeadlessWatchFace
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.mock

class HeadlessWatchFaceClientTest {

    private val iHeadlessWatchFace = mock<IHeadlessWatchFace>()
    private val iBinder = mock<IBinder>()

    init {
        Mockito.`when`(iHeadlessWatchFace.asBinder()).thenReturn(iBinder)
    }

    @Test
    fun renderWatchFaceToSurfaceSupported_oldApi() {
        Mockito.`when`(iHeadlessWatchFace.apiVersion).thenReturn(3)
        val client = HeadlessWatchFaceClientImpl(iHeadlessWatchFace)

        Assert.assertFalse(client.isRenderWatchFaceToSurfaceSupported)
    }

    @Test
    fun renderWatchFaceToSurfaceSupported_currentApi() {
        Mockito.`when`(iHeadlessWatchFace.apiVersion).thenReturn(IHeadlessWatchFace.API_VERSION)
        val client = HeadlessWatchFaceClientImpl(iHeadlessWatchFace)

        Assert.assertTrue(client.isRenderWatchFaceToSurfaceSupported)
    }
}
