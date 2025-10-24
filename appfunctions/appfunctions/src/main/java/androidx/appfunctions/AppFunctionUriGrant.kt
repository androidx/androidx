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

package androidx.appfunctions

import android.content.Context
import android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import android.net.Uri
import androidx.annotation.IntDef
import java.util.Objects

/**
 * Represents a [Uri] for which temporary access permission is to be granted to the caller of an
 * AppFunction execution.
 *
 * This class encapsulates a [uri] along with the specific access [modeFlags] (e.g.,
 * [FLAG_GRANT_READ_URI_PERMISSION]) that define the type of temporary access to be granted for that
 * URI.
 *
 * Using this class in the [ExecuteAppFunctionResponse.Success.returnValue] is equivalent to calling
 * [Context.grantUriPermission] for the agent that is executing the function.
 *
 * To succeed, the content provider owning the Uri must have set the `grantUriPermissions` attribute
 * in its manifest or included the `<grant-uri-permissions>` tag.
 *
 * @see ExecuteAppFunctionResponse.Success.returnValue
 * @see FLAG_GRANT_READ_URI_PERMISSION
 * @see FLAG_GRANT_WRITE_URI_PERMISSION
 * @see FLAG_GRANT_PREFIX_URI_PERMISSION
 * @see FLAG_GRANT_PERSISTABLE_URI_PERMISSION
 */
@AppFunctionSerializable
public class AppFunctionUriGrant(
    /** The [Uri] to be granted. */
    public val uri: Uri,
    /**
     * The access mode flags.
     *
     * This value must include at least one of [FLAG_GRANT_READ_URI_PERMISSION] or
     * [FLAG_GRANT_WRITE_URI_PERMISSION]. It may optionally also include
     * [FLAG_GRANT_PREFIX_URI_PERMISSION] or [FLAG_GRANT_PERSISTABLE_URI_PERMISSION].
     */
    @GrantUriMode public val modeFlags: Int,
) {
    init {
        require(isAccessUriMode(modeFlags)) {
            ("Must set either FLAG_GRANT_READ_URI_PERMISSION or " +
                "FLAG_GRANT_WRITE_URI_PERMISSION to specify the access mode")
        }
    }

    @IntDef(
        flag = true,
        value =
            [
                FLAG_GRANT_READ_URI_PERMISSION,
                FLAG_GRANT_WRITE_URI_PERMISSION,
                FLAG_GRANT_PREFIX_URI_PERMISSION,
                FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
            ],
    )
    @Retention(AnnotationRetention.SOURCE)
    internal annotation class GrantUriMode

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionUriGrant) return false

        return this.uri == other.uri && this.modeFlags == other.modeFlags
    }

    override fun hashCode(): Int {
        return Objects.hash(uri, modeFlags)
    }

    override fun toString(): String {
        return "AppFunctionUriGrant(uri=$uri, modeFlags=$modeFlags)"
    }

    private companion object {
        private fun isAccessUriMode(modeFlags: Int): Boolean {
            return (modeFlags and
                (FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION)) != 0
        }
    }
}
