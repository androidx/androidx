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

package androidx.privacysandbox.ui.core

import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.window.InputTransferToken
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

/**
 * A class containing values that will be constant for the lifetime of a
 * [SandboxedUiAdapter.Session].
 */
public class SessionData
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    /**
     * The input token of the window hosting this session.
     *
     * This value will be used when [Build.VERSION.SDK_INT] is equal to
     * [Build.VERSION_CODES.UPSIDE_DOWN_CAKE].
     */
    public val windowInputToken: IBinder?,
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    /**
     * The input transfer token of the window hosting this session.
     *
     * This will be non-null when [Build.VERSION.SDK_INT] is greater than
     * [Build.VERSION_CODES.UPSIDE_DOWN_CAKE].
     */
    public val inputTransferToken: InputTransferToken?,
) {
    public constructor() : this(null, null)

    public companion object {
        private const val KEY_WINDOW_INPUT_TOKEN = "windowInputToken"
        private const val KEY_INPUT_TRANSFER_TOKEN = "inputTransferToken"

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun toBundle(sessionData: SessionData): Bundle {
            val bundle = Bundle()
            sessionData.windowInputToken?.let { bundle.putBinder(KEY_WINDOW_INPUT_TOKEN, it) }
            CompatImpl.addInputTransferTokenToBundle(sessionData, bundle)
            return bundle
        }

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun fromBundle(bundle: Bundle): SessionData {
            val windowInputToken = bundle.getBinder(KEY_WINDOW_INPUT_TOKEN)
            val inputTransferToken = CompatImpl.deriveInputTransferToken(bundle)
            return SessionData(windowInputToken, inputTransferToken)
        }
    }

    override fun toString(): String =
        "SessionData(windowInputToken=$windowInputToken, inputTransferToken=$inputTransferToken)"

    override fun hashCode(): Int {
        var result = windowInputToken.hashCode()
        result += 31 * inputTransferToken.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SessionData) return false

        return windowInputToken == other.windowInputToken &&
            inputTransferToken == other.inputTransferToken
    }

    private object CompatImpl {

        fun deriveInputTransferToken(bundle: Bundle): InputTransferToken? {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                return Api35PlusImpl.deriveInputTransferToken(bundle)
            } else {
                return null
            }
        }

        fun addInputTransferTokenToBundle(sessionData: SessionData, bundle: Bundle) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                Api35PlusImpl.addInputTransferTokenToBundle(sessionData, bundle)
            }
        }

        @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        private object Api35PlusImpl {
            fun deriveInputTransferToken(bundle: Bundle): InputTransferToken? {
                return bundle.getParcelable(
                    KEY_INPUT_TRANSFER_TOKEN,
                    InputTransferToken::class.java,
                )
            }

            fun addInputTransferTokenToBundle(sessionData: SessionData, bundle: Bundle) {
                bundle.putParcelable(KEY_INPUT_TRANSFER_TOKEN, sessionData.inputTransferToken)
            }
        }
    }
}
