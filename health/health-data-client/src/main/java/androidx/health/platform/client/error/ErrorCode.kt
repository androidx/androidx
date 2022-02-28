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

package androidx.health.platform.client.error

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/** List of error codes returned by Health Platform, used in [ErrorStatus]. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@IntDef(
    ErrorCode.PROVIDER_NOT_INSTALLED,
    ErrorCode.PROVIDER_NOT_ENABLED,
    ErrorCode.PROVIDER_NEEDS_UPDATE,
    ErrorCode.NO_PERMISSION,
    ErrorCode.INVALID_OWNERSHIP,
    ErrorCode.NOT_ALLOWED,
    ErrorCode.EMPTY_PERMISSION_LIST,
    ErrorCode.PERMISSION_NOT_DECLARED,
    ErrorCode.INVALID_PERMISSION_RATIONALE_DECLARATION,
    ErrorCode.INVALID_UID,
    ErrorCode.DATABASE_ERROR,
    ErrorCode.INTERNAL_ERROR,
    ErrorCode.CHANGES_TOKEN_OUTDATED
)
annotation class ErrorCode {
    companion object {
        /** Health Platform is not installed. */
        const val PROVIDER_NOT_INSTALLED = 1

        /** Health Platform is installed, but disabled. */
        const val PROVIDER_NOT_ENABLED = 2

        /**
         * Health Platform needs to be updated (client requires newer version of a particular API
         * method).
         */
        const val PROVIDER_NEEDS_UPDATE = 3

        /**
         * The calling application is trying to access data without required authorization.
         */
        const val NO_PERMISSION = 4

        /**
         * Calling application is trying to modify data it doesn't own, i.e. the data was inserted by
         * another app into Health Platform.
         */
        const val INVALID_OWNERSHIP = 10000

        /** Calling application is not allowed to access Health Platform. */
        const val NOT_ALLOWED = 10001

        /** Requested permission list can't be empty. */
        const val EMPTY_PERMISSION_LIST = 10002

        /** Calling application is trying to request a permission it has not declared. */
        const val PERMISSION_NOT_DECLARED = 10003

        /**
         * Calling application is trying to request permissions without having a valid rationale
         * Activity declared to explain the use of permissions.
         */
        const val INVALID_PERMISSION_RATIONALE_DECLARATION = 10004

        /** Requested data UID is invalid and could not be found. */
        const val INVALID_UID = 10005

        /** Internal database error in Health Platform. */
        const val DATABASE_ERROR = 10006

        /** Some Internal error which will not get resolved even when client retry. */
        const val INTERNAL_ERROR = 10007

        /**
         * Calling application is using a changes token that indicates some changes were cleaned after
         * its last sync and before this call.
         */
        const val CHANGES_TOKEN_OUTDATED = 10008
    }
}