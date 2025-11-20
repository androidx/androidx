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

package androidx.credentials.providerevents.exception

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.credentials.providerevents.internal.toJetpackGetException

/** Represents an error thrown during the import flow. */
public abstract class ImportCredentialsException
@JvmOverloads
internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public open val type: String,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public open val errorMessage: String? = null,
) : Exception(errorMessage) {
    public companion object {
        private const val EXTRA_IMPORT_CREDENTIALS_EXCEPTION_TYPE =
            "androidx.credentials.providerevents.extra.IMPORT_CREDENTIALS_EXCEPTION_TYPE"
        private const val EXTRA_IMPORT_CREDENTIALS_EXCEPTION_MESSAGE =
            "androidx.credentials.providerevents.extra.IMPORT_CREDENTIALS_EXCEPTION_MESSAGE"

        /**
         * Helper method to convert the given [ex] to a parcelable [Bundle], in case the instance
         * needs to be sent across a process. Consumers of this method should use [fromBundle] to
         * reconstruct the class instance back from the bundle returned here.
         */
        @JvmStatic
        public fun asBundle(ex: ImportCredentialsException): Bundle {
            val bundle = Bundle()
            bundle.putString(EXTRA_IMPORT_CREDENTIALS_EXCEPTION_TYPE, ex.type)
            ex.errorMessage?.let {
                bundle.putCharSequence(EXTRA_IMPORT_CREDENTIALS_EXCEPTION_MESSAGE, it)
            }
            return bundle
        }

        /**
         * Helper method to convert a [Bundle] retrieved through [asBundle], back to an instance of
         * [ImportCredentialsException].
         *
         * Throws [IllegalArgumentException] if the conversion fails. This means that the given
         * [bundle] does not contain a `ImportCredentialsException`. The bundle should be
         * constructed and retrieved from [asBundle] itself and never be created from scratch to
         * avoid the failure.
         */
        @JvmStatic
        public fun fromBundle(bundle: Bundle): ImportCredentialsException {
            val type =
                bundle.getString(EXTRA_IMPORT_CREDENTIALS_EXCEPTION_TYPE)
                    ?: throw IllegalArgumentException("Bundle was missing exception type.")
            val msg = bundle.getString(EXTRA_IMPORT_CREDENTIALS_EXCEPTION_MESSAGE)
            return toJetpackGetException(type, msg)
        }
    }
}
