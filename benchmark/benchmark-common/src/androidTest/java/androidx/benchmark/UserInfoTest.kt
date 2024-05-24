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

package androidx.benchmark

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import java.io.DataInputStream
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = android.os.Build.VERSION_CODES.R)
class UserInfoTest {

    @Before
    fun setup() {
        UserInfo.Overrides.currentUserId = null
    }

    @After
    fun tearDown() {
        UserInfo.Overrides.currentUserId = null
    }

    @Test
    fun currentUserIdEqualsWhoAmI() {
        assertEquals(
            expected = UserInfo.currentUserId,
            actual = getCurrentUserId(),
            message = "currentUserId different from whoami user."
        )
    }

    /**
     * Executes the command `whoami`. The output of the command is in the format
     * `u<user-id>_a<app-id>`. After executing the command, the `user-id` is extracted and returned.
     */
    private fun getCurrentUserId(): Int {
        val output =
            try {
                DataInputStream(Runtime.getRuntime().exec("whoami").inputStream)
                    .bufferedReader()
                    .use { it.readText() }
            } catch (e: Exception) {
                Log.e(BenchmarkState.TAG, "Error running `whoami` command.", e)
                ""
            }
        val userId =
            try {
                output.split("_")[0].substring(1).toInt()
            } catch (e: Exception) {
                Log.e(BenchmarkState.TAG, "Error parsing user id. Current user: `$output`.", e)
                -1
            }
        return userId
    }
}
