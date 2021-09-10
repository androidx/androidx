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

import org.junit.Test
import java.util.ConcurrentModificationException
import kotlin.test.fail

class ArrayMapThreadedTest {
    var map: ArrayMap<String, String>? = ArrayMap()

    /**
     * Attempt to generate a ConcurrentModificationException in ArrayMap.
     *
     * ArrayMap is explicitly documented to be non-thread-safe, yet it's easy to accidentally screw
     * this up; ArrayMap should (in the spirit of the core Java collection types) make an effort to
     * catch this and throw ConcurrentModificationException instead of crashing somewhere in its
     * internals.
     */
    @Test
    fun testConcurrentModificationException() {
        val testLenMs = 5000
        Thread {
            var i = 0
            while (map != null) {
                try {
                    map?.put("key $i", "B_DONT_DO_THAT")
                } catch (e: ArrayIndexOutOfBoundsException) {
                    fail(cause = e)
                } catch (e: ClassCastException) {
                    fail(cause = e)
                } catch (e: ConcurrentModificationException) {
                    println("[successfully caught CME at put #$i, size=${map?.size}]")
                }
            }
        }.start()
        for (i in 0 until testLenMs / 100) {
            try {
                Thread.sleep(100)
                map?.clear()
            } catch (e: InterruptedException) {
            } catch (e: ArrayIndexOutOfBoundsException) {
                fail(cause = e)
            } catch (e: ClassCastException) {
                fail(cause = e)
            } catch (e: ConcurrentModificationException) {
                println("[successfully caught CME at clear #$i size=${map?.size}]")
            }
        }
        map = null // will stop other thread
    }
}