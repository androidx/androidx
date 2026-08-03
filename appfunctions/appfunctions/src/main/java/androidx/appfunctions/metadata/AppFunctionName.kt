/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appfunctions.metadata

import android.os.Build
import androidx.annotation.RequiresApi

/** Globally unique identifier for an app function. */
public class AppFunctionName
constructor(
    /** The package name of the Android app which contains the app function. */
    public val packageName: String,
    /** The identifier of the app function within the [packageName]. */
    public val functionIdentifier: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppFunctionName

        if (packageName != other.packageName) return false
        if (functionIdentifier != other.functionIdentifier) return false

        return true
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + functionIdentifier.hashCode()
        return result
    }

    override fun toString(): String {
        return "AppFunctionName(packageName='$packageName', functionIdentifier='$functionIdentifier')"
    }

    internal companion object {
        internal fun fromQualifiedId(qualifiedFunctionId: String): AppFunctionName {
            val parts = qualifiedFunctionId.split('/', limit = 2)

            require(parts.size == 2 && parts[1].isNotEmpty()) {
                "Incorrect app function id format."
            }

            return AppFunctionName(parts[0], parts[1])
        }

        @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
        internal fun fromPlatformAppFunctionName(
            platformAppFunctionName: android.app.appfunctions.AppFunctionName
        ): AppFunctionName {
            return AppFunctionName(
                packageName = platformAppFunctionName.packageName,
                functionIdentifier = platformAppFunctionName.functionIdentifier,
            )
        }
    }
}
