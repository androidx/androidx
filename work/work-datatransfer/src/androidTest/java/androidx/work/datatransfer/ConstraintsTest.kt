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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ConstraintsTest {

    @Test
    fun testDefaultNetworkRequirements() {
        val constraints1 = Constraints(getDefaultNetworkRequest())
        val constraints2 = Constraints()
        assertEquals(constraints1, constraints2)

        val constraints3 = Constraints(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                .build()
        )
        assertNotEquals(constraints2, constraints3)
    }

    @Test
    fun testEquals() {
        val constraints1 = Constraints(getDefaultNetworkRequest())
        val constraints2 = Constraints(getDefaultNetworkRequest())
        assertEquals(constraints1, constraints2)

        val constraints3 = Constraints(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                .build()
        )
        assertNotEquals(constraints2, constraints3)
    }

    @Test
    fun testCopyFrom() {
        val constraints1 = Constraints(getDefaultNetworkRequest())
        val constraints2 = Constraints.copyFrom(constraints1)
        assertEquals(constraints1, constraints2)

        val constraints3 = Constraints(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                .build()
        )
        assertNotEquals(constraints2, constraints3)
    }

    private fun getDefaultNetworkRequest(): NetworkRequest {
        return NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
    }
}
