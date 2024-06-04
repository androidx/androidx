/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.security.state

import java.time.Instant
import java.util.Date
import java.util.Objects

/** Represents information about an available update for a component. */
public class UpdateInfo(
    public val uri: String,
    public val component: String,
    public val securityPatchLevel: String,
    public val publishedDate: Date
) {
    public override fun toString(): String =
        "UpdateInfo(" +
            "uri=$uri, component=$component, SPL=$securityPatchLevel, date=$publishedDate)"

    public override fun equals(other: Any?): Boolean =
        other is UpdateInfo &&
            uri == other.uri &&
            component == other.component &&
            securityPatchLevel == other.securityPatchLevel &&
            publishedDate == other.publishedDate

    public override fun hashCode(): Int =
        Objects.hash(uri, component, securityPatchLevel, publishedDate)

    public class Builder {
        @set:JvmSynthetic private var uri: String = ""
        @set:JvmSynthetic private var component: String = ""
        @set:JvmSynthetic private var securityPatchLevel: String = ""
        @set:JvmSynthetic private var publishedDate: Date = Date.from(Instant.now())

        public fun setUri(uri: String): Builder = apply { this.uri = uri }

        public fun setComponent(component: String): Builder = apply { this.component = component }

        public fun setSecurityPatchLevel(securityPatchLevel: String): Builder = apply {
            this.securityPatchLevel = securityPatchLevel
        }

        public fun setPublishedDate(publishedDate: Date): Builder = apply {
            this.publishedDate = publishedDate
        }

        public fun build(): UpdateInfo =
            UpdateInfo(uri, component, securityPatchLevel, publishedDate)
    }
}

@JvmSynthetic
public fun UpdateInfo(initializer: UpdateInfo.Builder.() -> Unit): UpdateInfo {
    return UpdateInfo.Builder().apply(initializer).build()
}
