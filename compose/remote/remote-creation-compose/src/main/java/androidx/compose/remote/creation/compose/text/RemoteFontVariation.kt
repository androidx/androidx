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

package androidx.compose.remote.creation.compose.text

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.runtime.Immutable

/** Font variation settings for variable fonts, with axis values as [RemoteFloat]s. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteFontVariation {
    /**
     * A single variable font axis setting, such as `Setting("wght", weight)`.
     *
     * Constant values are compared numerically, so `0f` and `-0f` are equal, as for `Float` values
     * of `androidx.compose.ui.text.font.FontVariation.Setting`.
     *
     * @param axisName The four-character OpenType axis tag, such as `wght` or `wdth`.
     * @param value The value of the axis, which can be a remote expression.
     */
    @Immutable
    public class Setting(public val axisName: String, public val value: RemoteFloat) {
        init {
            require(axisName.length == 4) {
                "Name must be exactly four characters. Actual: '$axisName'"
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Setting) return false
            if (axisName != other.axisName) return false
            val constantValue = value.constantValueOrNull
            val otherConstantValue = other.value.constantValueOrNull
            if (constantValue != null && otherConstantValue != null) {
                return constantValue == otherConstantValue
            }
            return value.isStructurallyEqual(other.value)
        }

        override fun hashCode(): Int {
            val constantValue = value.constantValueOrNull
            val valueHashCode =
                if (constantValue != null) {
                    (if (constantValue == 0f) 0f else constantValue).hashCode()
                } else {
                    value.cacheKey.hashCode()
                }
            return 31 * axisName.hashCode() + valueHashCode
        }

        override fun toString(): String =
            "RemoteFontVariation.Setting(axisName='$axisName', " +
                "value=${value.constantValueOrNull ?: value})"
    }

    /**
     * An ordered list of [Setting]s to apply to a variable font.
     *
     * @param settings The settings to apply, which must be unique by [Setting.axisName].
     */
    @Immutable
    public class Settings(settings: List<Setting>) {
        public val settings: List<Setting> = List(settings.size) { settings[it] }

        /**
         * Creates [Settings] from the given [settings], which must be unique by [Setting.axisName].
         */
        public constructor(vararg settings: Setting) : this(settings.asList())

        init {
            for (i in settings.indices) {
                val axisName = settings[i].axisName
                for (j in i + 1 until settings.size) {
                    require(axisName != settings[j].axisName) {
                        "'$axisName' must be unique. Actual $this"
                    }
                }
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Settings) return false
            if (settings.size != other.settings.size) return false
            for (i in settings.indices) {
                if (settings[i] != other.settings[i]) return false
            }
            return true
        }

        override fun hashCode(): Int {
            var result = 1
            for (i in settings.indices) {
                result = 31 * result + settings[i].hashCode()
            }
            return result
        }

        override fun toString(): String = buildString {
            append("RemoteFontVariation.Settings(settings=[")
            for (i in settings.indices) {
                if (i > 0) append(", ")
                append(settings[i])
            }
            append("])")
        }
    }
}
