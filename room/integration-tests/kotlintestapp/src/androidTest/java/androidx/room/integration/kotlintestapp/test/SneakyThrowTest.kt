/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.room.integration.kotlintestapp.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import java.util.concurrent.Callable
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class SneakyThrowTest : TestDatabaseTest() {

    @Test
    fun testCheckedException() {
        try {
            database.runInTransaction(
                Callable<String> {
                    val json = JSONObject()
                    json.getString("key") // method declares that it throws JSONException
                }
            )
            fail("runInTransaction should have thrown an exception")
        } catch (ex: JSONException) {
            // no-op on purpose
        }
    }
}
