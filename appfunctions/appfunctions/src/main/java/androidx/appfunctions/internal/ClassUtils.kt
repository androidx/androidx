/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions.internal

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

/**
 * Finds the implementation of the given [Class].
 *
 * @param suffix the suffix used with the [Class] name to identify the implementation class. Then,
 *   use the `_Impl` as suffix. Default value is `_Impl`.
 * @return the implementation instance.
 * @throws RuntimeException if unable to find the implementation class.
 */
@RequiresApi(Build.VERSION_CODES.S)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T : Any> Class<T>.findImpl(prefix: String, suffix: String): T {
    val fullPackage = this.packageName
    val name = this.canonicalName
    requireNotNull(name)

    val postPackageName = name.substring(fullPackage.length + 1)
    val implName = "$prefix$postPackageName$suffix"
    return try {
        val fullClassName = "$fullPackage.$implName"
        @Suppress("UNCHECKED_CAST")
        val aClass = Class.forName(fullClassName, true, this.classLoader) as Class<T>
        aClass.getDeclaredConstructor().newInstance()
    } catch (e: ClassNotFoundException) {
        throw RuntimeException(
            "Cannot find implementation for ${this.canonicalName}. $implName does not " +
                "exist. Is AppFunction annotation processor correctly configured?",
            e,
        )
    } catch (e: IllegalAccessException) {
        throw RuntimeException("Cannot access the constructor ${this.canonicalName}", e)
    } catch (e: InstantiationException) {
        throw RuntimeException("Failed to create an instance of ${this.canonicalName}", e)
    }
}
