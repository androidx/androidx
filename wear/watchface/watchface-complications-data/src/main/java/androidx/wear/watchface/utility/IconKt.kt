/*
 * Copyright 2021 The Android Open Source Project
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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.wear.watchface.utility

import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import java.util.Objects

/** Returns true if the [Icon]s are equal. */
infix fun Icon?.iconEquals(other: Icon?): Boolean =
    this === other ||
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            IconP.equals(this, other)
        } else {
            this == other
        }

/** Creates a hash code for the [Icon]. */
fun Icon.iconHashCode(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        IconP.hashCode(this)
    } else {
        hashCode()
    }

@RequiresApi(Build.VERSION_CODES.P)
private object IconP {
    fun equals(icon: Icon?, other: Icon?): Boolean =
        (icon == null && other == null) ||
            ((icon != null && other != null) &&
                icon.type == other.type &&
                when (icon.type) {
                    Icon.TYPE_RESOURCE ->
                        icon.resId == other.resId && icon.resPackage == other.resPackage
                    Icon.TYPE_URI -> icon.uri == other.uri
                    else -> icon == other
                })

    fun hashCode(icon: Icon): Int =
        when (icon.type) {
            Icon.TYPE_RESOURCE -> Objects.hash(icon.type, icon.resId, icon.resPackage)
            Icon.TYPE_URI -> Objects.hash(icon.type, icon.uri)
            else -> hashCode()
        }
}
