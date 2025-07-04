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

package androidx.xr.projected.permissions

/**
 * Class representing data for requesting permissions from a Projected activity.
 *
 * @param permissions The permissions to request.
 * @param rationale The rationale for the permission request. If null, the request will be presented
 *   to the user immediately. If not null, the rationale will be presented to the user first and the
 *   permission request will only be triggered after the user accepts the rationale. See
 *   [ProjectedPermissionsResultContract] for details.
 */
public class ProjectedPermissionsRequestParams(
    public val permissions: List<String>,
    public val rationale: String?,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProjectedPermissionsRequestParams) return false

        if (permissions != other.permissions) return false
        if (rationale != other.rationale) return false

        return true
    }

    override fun hashCode(): Int {
        var result = permissions.hashCode()
        result = 31 * result + (rationale?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ProjectedPermissionsRequestParams(permissions=${permissions.joinToString()}, rationale=$rationale)"
    }
}
