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

package androidx.work

import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.work.impl.model.WorkTypeConverters
import androidx.work.worker.TestWorker
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import org.junit.Test
import org.junit.runner.RunWith

// TODO(b/209145335): Combine with WorkSpecDaoTest when it is converted to Kotlin
@RunWith(AndroidJUnit4::class)
class WorkConstrainsDaoTest : DatabaseTest() {
    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 31)
    fun readWithNetworkRequestWithInvalidCapability() {
        val workRequest =
            OneTimeWorkRequest.Builder(TestWorker::class.java)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkRequest(
                            NetworkRequest.Builder()
                                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                                .build(),
                            NetworkType.CONNECTED
                        )
                        .build()
                )
                .build()

        val workSpec = workRequest.workSpec
        mDatabase.workSpecDao().insertWorkSpec(workSpec)

        val currentBlob =
            WorkTypeConverters.fromNetworkRequest(workSpec.constraints.requiredNetworkRequestCompat)
        val newBlob =
            ByteBuffer.allocate(currentBlob.size + 4)
                .apply {
                    currentBlob.forEach { put(it) } // Copy current blob content
                    put(5, (currentBlob[5] + 4).toByte()) // Update blob size
                    put(17, (currentBlob[17] + 1).toByte()) // Update capabilities size
                    putInt(37) // Add a new invalid capability.
                }
                .array()

        // Update the blob in work spec row include the new invalid capability
        mDatabase
            .compileStatement("UPDATE workspec SET required_network_request = ? WHERE id = ?")
            .use { stmt ->
                stmt.bindBlob(1, newBlob)
                stmt.bindString(2, workSpec.id)
                assertThat(stmt.executeUpdateDelete()).isEqualTo(1)
            }

        // Reading a work spec with an invalid capability should not cause an issue.
        val newWorkSpec = checkNotNull(mDatabase.workSpecDao().getWorkSpec(workRequest.stringId))
        assertThat(workSpec.constraints.requiredNetworkRequest!!.capabilities)
            .isEqualTo(newWorkSpec.constraints.requiredNetworkRequest!!.capabilities)
    }
}
