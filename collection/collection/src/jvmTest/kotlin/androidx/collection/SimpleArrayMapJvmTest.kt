/*
 * Copyright 2022 The Android Open Source Project
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

import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert
import org.junit.Test

public class SimpleArrayMapJvmTest {
    /**
     * Attempt to generate a ConcurrentModificationException in ArrayMap.
     */
    @Test
    public fun testConcurrentModificationException() {
        val map = SimpleArrayMap<String, String>()
        val done = AtomicBoolean()
        val TEST_LEN_MS = 5000
        println("Starting SimpleArrayMap concurrency test")
        Thread {
            var i = 0
            while (!done.get()) {
                try {
                    map.put(String.format(Locale.US, "key %d", i++), "B_DONT_DO_THAT")
                } catch (e: ArrayIndexOutOfBoundsException) {
                    // SimpleArrayMap is not thread safe, so lots of concurrent modifications
                    // can still cause data corruption
                    System.err.println("concurrent modification uncaught, causing indexing failure")
                    e.printStackTrace()
                } catch (e: ClassCastException) {
                    // cache corruption should not occur as it is hard to trace and one thread
                    // may corrupt the pool for all threads in the same process.
                    System.err.println("concurrent modification uncaught, causing cache corruption")
                    e.printStackTrace()
                    Assert.fail()
                } catch (_: ConcurrentModificationException) {
                }
            }
        }.start()
        for (i in 0 until TEST_LEN_MS / 100) {
            try {
                Thread.sleep(100)
                map.clear()
            } catch (_: InterruptedException) {
            } catch (e: ArrayIndexOutOfBoundsException) {
                System.err.println("concurrent modification uncaught, causing indexing failure")
            } catch (e: ClassCastException) {
                System.err.println("concurrent modification uncaught, causing cache corruption")
                Assert.fail()
            } catch (_: ConcurrentModificationException) {
            }
        }
        done.set(true)
    }
}
