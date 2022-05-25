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
package androidx.core.app

import android.app.Notification
import android.os.Build
import android.os.Bundle
import org.junit.Assert.assertEquals
import java.util.TreeSet

internal object NotificationTester {
    @JvmStatic
    fun assertNotificationEquals(n1: Notification?, n2: Notification?) {
        if (n1 == null || n2 == null) {
            assertEquals(n1, n2)
            return
        }
        assertEquals(n1.toString(), n2.toString())
        assertBundleEquals("Notification.extras", n2.extras, n2.extras)
    }

    @Suppress("DEPRECATION")
    private fun assertBundleEquals(keyPath: String, b1: Bundle, b2: Bundle) {
        assertEquals("Keys @ $keyPath", TreeSet(b1.keySet()), TreeSet(b2.keySet()))
        for (key in b1.keySet()) {
            assertValueEquals("$keyPath/$key", b1[key], b2[key])
        }
    }

    private fun assertValueEquals(keyPath: String, v1: Any?, v2: Any?) {
        if (v1 == null || v2 == null) {
            assertEquals("Values @$keyPath", v1, v2)
            return
        }
        assertEquals("Classes @$keyPath", v1.javaClass, v2.javaClass)
        if (v1 is Bundle && v2 is Bundle) {
            assertBundleEquals(keyPath, v1, v2)
            return
        }
        if (Build.VERSION.SDK_INT >= 28) {
            if (v1 is android.app.Person && v2 is android.app.Person) {
                val b1 = Person.fromAndroidPerson(v1).toBundle()
                val b2 = Person.fromAndroidPerson(v2).toBundle()
                assertBundleEquals(keyPath, b1, b2)
                return
            }
        }
        if (v1 is Array<*> && v2 is Array<*>) {
            assertEquals("Array lengths @$keyPath", v1.size, v2.size)
            for (i in v1.indices) {
                assertValueEquals("$keyPath[$i]", v1[i], v2[i])
            }
            return
        }
        if (v1 is ArrayList<*> && v2 is ArrayList<*>) {
            assertEquals("ArrayList sizes @$keyPath", v1.size, v2.size)
            for (i in v1.indices) {
                assertValueEquals("$keyPath[$i]", v1[i], v2[i])
            }
            return
        }
        assertEquals("Values @$keyPath", v1, v2)
    }
}