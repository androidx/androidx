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

package androidx.work.datatransfer

import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class UserInitiatedTaskRequestTest {

    @Test
    fun testDefaultNetworkConstraints() {
        val request = UserInitiatedTaskRequest(MyTask::class.java)
        val networkRequest = NetworkRequest.Builder()
                                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                                .build()
        assertEquals(request.constraints.networkRequest, networkRequest)

        val networkRequest2 = NetworkRequest.Builder()
                                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                                .build()
        assertNotEquals(request.constraints.networkRequest, networkRequest2)
    }

    @Test
    fun testCustomNetworkConstraints() {
        val request = UserInitiatedTaskRequest(MyTask::class.java,
            Constraints(NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                .build()
            )
        )
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            .build()
        assertEquals(request.constraints.networkRequest, networkRequest)

        val networkRequest2 = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        assertNotEquals(request.constraints.networkRequest, networkRequest2)
    }

    @Test
    fun testTags() {
        val taskClassName = "androidx.work.datatransfer.UserInitiatedTaskRequestTest\$MyTask"
        var request = UserInitiatedTaskRequest(MyTask::class.java)
        assertEquals(1, request.tags.size)
        assertEquals(taskClassName, request.tags.get(0))

        request = UserInitiatedTaskRequest(MyTask::class.java, _tags = mutableListOf("test"))
        assertEquals(2, request.tags.size)
        assertTrue(request.tags.contains("test"))

        request = UserInitiatedTaskRequest(MyTask::class.java,
                                           _tags = mutableListOf("test", "test2"))
        assertEquals(3, request.tags.size)
        assertTrue(request.tags.contains(taskClassName))
        assertTrue(request.tags.contains("test2"))
        assertTrue(request.tags.contains("test"))
    }

    @Test
    fun testDefaultTransferInfo() {
        val request = UserInitiatedTaskRequest(MyTask::class.java)
        assertNull(request.transferInfo)
    }

    @Test
    fun testCustomTransferInfo() {
        var request = UserInitiatedTaskRequest(MyTask::class.java,
            _transferInfo = TransferInfo(estimatedDownloadBytes = 1000L))
        val transferInfo = TransferInfo(0L, 1000L)
        assertEquals(request.transferInfo, transferInfo)

        request = UserInitiatedTaskRequest(MyTask::class.java,
            _transferInfo = TransferInfo(estimatedUploadBytes = 1000L))
        val transferInfo2 = TransferInfo(1000L, 0L)
        assertEquals(request.transferInfo, transferInfo2)
        assertNotEquals(request.transferInfo, transferInfo)

        request = UserInitiatedTaskRequest(MyTask::class.java,
            _transferInfo = TransferInfo(2000L, 20L))
        val transferInfo3 = TransferInfo(2000L, 20L)
        assertEquals(request.transferInfo, transferInfo3)
    }

    private class MyTask : UserInitiatedTask(
        "test_task",
        ApplicationProvider.getApplicationContext()
    ) {
        override suspend fun performTask() {
            // test stub
        }
    }
}