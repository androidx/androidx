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

package androidx.window.core

import android.content.ComponentName

/**
 * A class to hold simple information from [android.content.ComponentName] like the package and
 * the class name.
 */
internal class ActivityComponentInfo(
    val packageName: String,
    val className: String
) {

    constructor(componentName: ComponentName) : this(
        componentName.packageName,
        componentName.className
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ActivityComponentInfo

        if (packageName != other.packageName) return false
        if (className != other.className) return false

        return true
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + className.hashCode()
        return result
    }

    override fun toString(): String {
        return "ClassInfo { packageName: $packageName, className: $className }"
    }
}
