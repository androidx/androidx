/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.telecom

import android.net.Uri
import android.telecom.PhoneAccountHandle
import androidx.core.telecom.internal.utils.BuildVersionAdapter

class TestUtils {

    companion object {

        internal val mV2Build = object : BuildVersionAdapter {
            override fun hasPlatformV2Apis(): Boolean {
                return true
            }

            override fun hasInvalidBuildVersion(): Boolean {
                return false
            }
        }

        internal val mBackwardsCompatBuild = object : BuildVersionAdapter {
            override fun hasPlatformV2Apis(): Boolean {
                return false
            }

            override fun hasInvalidBuildVersion(): Boolean {
                return false
            }
        }

        internal val mInvalidBuild = object : BuildVersionAdapter {
            override fun hasPlatformV2Apis(): Boolean {
                return false
            }

            override fun hasInvalidBuildVersion(): Boolean {
                return true
            }
        }

        val TEST_CALL_ATTRIB_NAME = "Elon Musk"
        val TEST_CALL_ATTRIB_NUMBER = Uri.parse("tel:6506959001")

        fun createCallAttributes(
            callDirection: Int,
            phoneAccountHandle: PhoneAccountHandle,
            callType: Int? = CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
        ): CallAttributesCompat {

            val attributes: CallAttributesCompat = if (callType != null) {
                CallAttributesCompat(
                    TEST_CALL_ATTRIB_NAME,
                    TEST_CALL_ATTRIB_NUMBER,
                    callDirection, callType
                )
            } else {
                CallAttributesCompat(
                    TEST_CALL_ATTRIB_NAME,
                    TEST_CALL_ATTRIB_NUMBER,
                    callDirection
                )
            }

            attributes.mHandle = phoneAccountHandle

            return attributes
        }
    }
}