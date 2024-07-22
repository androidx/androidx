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

package androidx.window.embedding

import android.app.Activity
import android.os.IBinder
import androidx.annotation.RestrictTo
import androidx.window.RequiresWindowSdkExtension
import androidx.window.WindowSdkExtensions
import androidx.window.extensions.embedding.SplitInfo.Token

/** Describes a split pair of two containers with activities. */
@Suppress("Deprecation") // To compat with device with version 3 and 4.
class SplitInfo
private constructor(
    /** The [ActivityStack] representing the primary split container. */
    val primaryActivityStack: ActivityStack,
    /** The [ActivityStack] representing the secondary split container. */
    val secondaryActivityStack: ActivityStack,
    /** The [SplitAttributes] of this split pair. */
    val splitAttributes: SplitAttributes,
    @Deprecated(
        message = "Use [token] instead",
        replaceWith =
            ReplaceWith(
                expression = "SplitInfo.token",
                imports = arrayOf("androidx.window.embedding.SplitInfo"),
            )
    )
    private val binder: IBinder?,
    /** A token uniquely identifying this `SplitInfo`. */
    private val token: Token?,
) {
    @RequiresWindowSdkExtension(5)
    internal constructor(
        primaryActivityStack: ActivityStack,
        secondaryActivityStack: ActivityStack,
        splitAttributes: SplitAttributes,
        token: Token,
    ) : this(primaryActivityStack, secondaryActivityStack, splitAttributes, binder = null, token)

    /** Creates SplitInfo for [WindowSdkExtensions.extensionVersion] 3 and 4. */
    @RequiresWindowSdkExtension(3)
    internal constructor(
        primaryActivityStack: ActivityStack,
        secondaryActivityStack: ActivityStack,
        splitAttributes: SplitAttributes,
        binder: IBinder,
    ) : this(
        primaryActivityStack,
        secondaryActivityStack,
        splitAttributes,
        binder,
        token = null,
    ) {
        WindowSdkExtensions.getInstance().requireExtensionVersion(3..4)
    }

    /**
     * Creates SplitInfo ONLY for testing.
     *
     * @param primaryActivityStack the [ActivityStack] representing the primary split container.
     * @param secondaryActivityStack the [ActivityStack] representing the secondary split container.
     * @param splitAttributes the [SplitAttributes] of this split pair.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(
        primaryActivityStack: ActivityStack,
        secondaryActivityStack: ActivityStack,
        splitAttributes: SplitAttributes,
    ) : this(
        primaryActivityStack,
        secondaryActivityStack,
        splitAttributes,
        binder = null,
        token = null,
    )

    @RequiresWindowSdkExtension(3)
    internal fun getBinder(): IBinder = let {
        WindowSdkExtensions.getInstance().requireExtensionVersion(3..4)
        requireNotNull(binder)
    }

    @RequiresWindowSdkExtension(5)
    internal fun getToken(): Token = let {
        WindowSdkExtensions.getInstance().requireExtensionVersion(5)
        requireNotNull(token)
    }

    /**
     * Whether the [primaryActivityStack] or the [secondaryActivityStack] in this [SplitInfo]
     * contains the [activity].
     */
    operator fun contains(activity: Activity): Boolean {
        return primaryActivityStack.contains(activity) || secondaryActivityStack.contains(activity)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SplitInfo) return false

        if (primaryActivityStack != other.primaryActivityStack) return false
        if (secondaryActivityStack != other.secondaryActivityStack) return false
        if (splitAttributes != other.splitAttributes) return false
        if (token != other.token) return false
        if (binder != other.binder) return false

        return true
    }

    override fun hashCode(): Int {
        var result = primaryActivityStack.hashCode()
        result = 31 * result + secondaryActivityStack.hashCode()
        result = 31 * result + splitAttributes.hashCode()
        result = 31 * result + token.hashCode()
        result = 31 * result + binder.hashCode()
        return result
    }

    override fun toString(): String {
        return buildString {
            append("SplitInfo:{")
            append("primaryActivityStack=$primaryActivityStack, ")
            append("secondaryActivityStack=$secondaryActivityStack, ")
            append("splitAttributes=$splitAttributes, ")
            if (token != null) {
                append("token=$token")
            }
            if (binder != null) {
                append("binder=$binder")
            }
            append("}")
        }
    }
}
