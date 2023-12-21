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

package androidx.camera.extensions.internal.util

import androidx.annotation.RequiresApi
import androidx.camera.extensions.impl.ExtensionVersionImpl
import androidx.camera.extensions.internal.ExtensionVersion
import java.lang.reflect.Field
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

/**
 * Util functions for extensions related robolectric test
 */
@RequiresApi(21)
object ExtensionsTestUtil {

    /**
     * Resets the field of the specified class
     */
    @JvmStatic
    fun resetSingleton(clazz: Class<*>, fieldName: String) {
        val instance: Field
        try {
            instance = clazz.getDeclaredField(fieldName)
            instance.isAccessible = true
            instance[null] = null
        } catch (e: Exception) {
            throw RuntimeException()
        }
    }

    /**
     * Sets vendor library extension version to the specified value.
     */
    @JvmStatic
    fun setTestApiVersion(testString: String) {
        setTestApiVersionAndAdvancedExtender(testString, false)
    }

    /**
     * Sets vendor library extension version to the specified value.
     */
    @JvmStatic
    fun setTestApiVersionAndAdvancedExtender(
        testString: String,
        isAdvancedExtenderImplemented: Boolean
    ) {
        val mockExtensionVersionImpl = Mockito.mock(
            ExtensionVersionImpl::class.java
        )
        Mockito.`when`(mockExtensionVersionImpl.checkApiVersion(ArgumentMatchers.anyString()))
            .thenReturn(testString)
        Mockito.`when`(mockExtensionVersionImpl.isAdvancedExtenderImplemented())
            .thenReturn(isAdvancedExtenderImplemented)
        var vendorExtenderVersioningClass: Class<*>? = null
        for (clazz in ExtensionVersion::class.java.declaredClasses) {
            if (clazz.simpleName == "VendorExtenderVersioning") {
                vendorExtenderVersioningClass = clazz
                break
            }
        }
        val field = vendorExtenderVersioningClass!!.getDeclaredField("sImpl")
        field.isAccessible = true
        field[null] = mockExtensionVersionImpl
    }
}
