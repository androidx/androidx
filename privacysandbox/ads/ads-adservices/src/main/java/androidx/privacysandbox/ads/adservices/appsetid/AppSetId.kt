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

package androidx.privacysandbox.ads.adservices.appsetid

/**
 * A unique, per-device, per developer-account user-resettable ID for non-monetizing advertising
 * use cases.
 *
 * Represents the appSetID and scope of this appSetId from the
 * [AppSetIdManager#getAppSetId()] API. The scope of the ID can be per app or per developer account
 * associated with the user. AppSetId is used for analytics, spam detection, frequency capping and
 * fraud prevention use cases, on a given device, that one may need to correlate usage or actions
 * across a set of apps owned by an organization.
 *
 * @param id The appSetID.
 * @param scope The scope of the ID. Can be AppSetId.SCOPE_APP or AppSetId.SCOPE_DEVELOPER.
 */
class AppSetId public constructor(
    val id: String,
    val scope: Int
) {
    init {
        require(scope == SCOPE_APP || scope == SCOPE_DEVELOPER) { "Scope undefined." }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppSetId) return false
        return this.id == other.id &&
            this.scope == other.scope
    }

    override fun hashCode(): Int {
        var hash = id.hashCode()
        hash = 31 * hash + scope.hashCode()
        return hash
    }

    override fun toString(): String {
        var scopeStr = if (scope == 1) "SCOPE_APP" else "SCOPE_DEVELOPER"
        return "AppSetId: id=$id, scope=$scopeStr"
    }

    companion object {
        /**
         * The appSetId is scoped to an app. All apps on a device will have a different appSetId.
         */
        public const val SCOPE_APP = 1

        /**
         * The appSetId is scoped to a developer account on an app store. All apps from the same
         * developer on a device will have the same developer scoped appSetId.
         */
        public const val SCOPE_DEVELOPER = 2
    }
}
