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

package androidx.collection

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.fail

class SimpleArrayMapThreadedTest {
    /**
     * Attempt to generate a ConcurrentModificationException in ArrayMap.
     */
    @Test
    fun testConcurrentModificationException() {
        val map = SimpleArrayMap<String, String>()
        val done = AtomicBoolean()
        val TEST_LEN_MS = 5000
        println("Starting SimpleArrayMap concurrency test")
        Thread(
            Runnable() {
                fun run() {
                    var i = 0
                    while (!done.get()) {
                        try {
                            map.put("key ${i++}", "B_DONT_DO_THAT")
                        } catch (e: ArrayIndexOutOfBoundsException) {
                            // SimpleArrayMap is not thread safe, so lots of concurrent modifications
                            // can still cause data corruption
                            println("concurrent modification uncaught, causing indexing failure")
                            e.printStackTrace()
                        } catch (e: ClassCastException) {
                            // cache corruption should not occur as it is hard to trace and one thread
                            // may corrupt the pool for all threads in the same process.
                            fail("concurrent modification uncaught, causing cache corruption", e)
                        } catch (e: ConcurrentModificationException) {
                            // Ignored.
                        }
                    }
                }
            }
        ).start()

        for (i in 0 until TEST_LEN_MS / 100) {
            try {
                Thread.sleep(100)
                map.clear()
            } catch (e: InterruptedException) {
            } catch (e: ArrayIndexOutOfBoundsException) {
                fail("concurrent modification uncaught, causing indexing failure", e)
            } catch (e: ClassCastException) {
                fail("concurrent modification uncaught, causing cache corruption", e)
            } catch (e: ConcurrentModificationException) {
                // Ignored.
            }
        }
        done.set(true)
    }
}