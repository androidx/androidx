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

package androidx.credentials.agesignals.exceptions

import android.os.Bundle
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.credentials.agesignals.internal.toJetpackGetAgeRangeException

/** Signals that an age range request failed. */
public abstract class GetAgeRangeException
@JvmOverloads
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public open val type: String,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val errorMessage: CharSequence? = null,
) : Exception(errorMessage?.toString()) {

    internal companion object {
        private const val TAG = "GetAgeRangeException"

        @VisibleForTesting
        internal const val EXTRA_GET_AGE_RANGE_EXCEPTION_TYPE =
            "androidx.credentials.agesignals.extra.GET_AGE_RANGE_EXCEPTION_TYPE"

        @VisibleForTesting
        internal const val EXTRA_GET_AGE_RANGE_EXCEPTION_MESSAGE =
            "androidx.credentials.agesignals.extra.GET_AGE_RANGE_EXCEPTION_MESSAGE"

        /**
         * Helper method to convert the given [ex] to a parcelable [Bundle], in case the instance
         * needs to be sent across a process. Consumers of this method should use [fromBundle] to
         * reconstruct the class instance back from the bundle returned here.
         */
        internal fun asBundle(ex: GetAgeRangeException): Bundle {
            val bundle = Bundle()
            bundle.putString(EXTRA_GET_AGE_RANGE_EXCEPTION_TYPE, ex.type)
            ex.errorMessage?.let {
                bundle.putCharSequence(EXTRA_GET_AGE_RANGE_EXCEPTION_MESSAGE, it)
            }
            return bundle
        }

        /**
         * Helper method to convert a [Bundle] retrieved through [asBundle], back to an instance of
         * [GetAgeRangeException].
         *
         * A [bundle] that does not carry a recognizable exception type is reported as a
         * [GetAgeRangeUnknownException] rather than failing, because the presence of an exception
         * entry already tells us the request failed; only the specific cause is unavailable.
         */
        internal fun fromBundle(bundle: Bundle): GetAgeRangeException {
            val msg = bundle.getCharSequence(EXTRA_GET_AGE_RANGE_EXCEPTION_MESSAGE)
            val type = bundle.getString(EXTRA_GET_AGE_RANGE_EXCEPTION_TYPE)
            if (type == null) {
                Log.w(
                    TAG,
                    "Exception bundle is missing $EXTRA_GET_AGE_RANGE_EXCEPTION_TYPE; " +
                        "reporting the failure as unknown.",
                )
                return GetAgeRangeUnknownException(msg)
            }
            return toJetpackGetAgeRangeException(type, msg)
        }
    }
}
