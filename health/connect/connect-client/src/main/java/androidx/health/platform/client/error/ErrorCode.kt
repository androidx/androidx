/*
 * Copyright (C) 2022 The Android Open Source Project
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
    ErrorCode.CHANGES_TOKEN_OUTDATED,
    ErrorCode.TRANSACTION_TOO_LARGE,
)
@Retention(AnnotationRetention.SOURCE)
@RestrictTo(RestrictTo.Scope.LIBRARY)
public annotation class ErrorCode {
    public companion object {
        /** Health Platform is not installed. */
        public const val PROVIDER_NOT_INSTALLED: Int = 1

        /** Health Platform is installed, but disabled. */
        public const val PROVIDER_NOT_ENABLED: Int = 2

        /**
         * Health Platform needs to be updated (client requires newer version of a particular API
         * method).
         */
        public const val PROVIDER_NEEDS_UPDATE: Int = 3

        /** The calling application is trying to access data without required authorization. */
        public const val NO_PERMISSION: Int = 4

        /**
         * Calling application is trying to modify data it doesn't own, i.e. the data was inserted
         * by another app into Health Platform.
         */
        public const val INVALID_OWNERSHIP: Int = 10000

        /** Calling application is not allowed to access Health Platform. */
        public const val NOT_ALLOWED: Int = 10001

        /** Requested permission list can't be empty. */
        public const val EMPTY_PERMISSION_LIST: Int = 10002

        /** Calling application is trying to request a permission it has not declared. */
        public const val PERMISSION_NOT_DECLARED: Int = 10003

        /**
         * Calling application is trying to request permissions without having a valid rationale
         * Activity declared to explain the use of permissions.
         */
        public const val INVALID_PERMISSION_RATIONALE_DECLARATION: Int = 10004

        /** Requested data UID is invalid and could not be found. */
        public const val INVALID_UID: Int = 10005

        /** Internal database error in Health Platform. */
        public const val DATABASE_ERROR: Int = 10006

        /** Some Internal error which will not get resolved even when client retry. */
        public const val INTERNAL_ERROR: Int = 10007

        /**
         * Calling application is using a changes token that indicates some changes were cleaned
         * after its last sync and before this call.
         */
        public const val CHANGES_TOKEN_OUTDATED: Int = 10008

        /** Remote end failed to deliver response, likely due to parcel too large. */
        public const val TRANSACTION_TOO_LARGE: Int = 10010
    }
}
