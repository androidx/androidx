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

package androidx.core.telecom.test

import android.content.ComponentName
import android.net.Uri
import android.os.Build.VERSION_CODES
import android.os.Process
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import androidx.core.os.BundleCompat
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.internal.utils.Utils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
@SmallTest
class UtilsTest {

    private val mPhoneAccountHandle =
        PhoneAccountHandle(
            ComponentName("androidx.core.telecom.test", "FakeClass"),
            "FakeId",
            Process.myUserHandle(),
        )

    @Test
    fun testToVideoProfileState() {
        // Audio Call -> STATE_AUDIO_ONLY
        assertThat(Utils.toVideoProfileState(CallAttributesCompat.CALL_TYPE_AUDIO_CALL))
            .isEqualTo(VideoProfile.STATE_AUDIO_ONLY)

        // Video Call -> STATE_BIDIRECTIONAL
        assertThat(Utils.toVideoProfileState(CallAttributesCompat.CALL_TYPE_VIDEO_CALL))
            .isEqualTo(VideoProfile.STATE_BIDIRECTIONAL)

        // Invalid -> STATE_AUDIO_ONLY (default fallback)
        assertThat(Utils.toVideoProfileState(-1)).isEqualTo(VideoProfile.STATE_AUDIO_ONLY)
    }

    @Test
    fun testToCallTypeCompat() {
        // STATE_AUDIO_ONLY -> CALL_TYPE_AUDIO_CALL
        assertThat(Utils.toCallTypeCompat(VideoProfile.STATE_AUDIO_ONLY))
            .isEqualTo(CallAttributesCompat.CALL_TYPE_AUDIO_CALL)

        // STATE_BIDIRECTIONAL -> CALL_TYPE_VIDEO_CALL
        assertThat(Utils.toCallTypeCompat(VideoProfile.STATE_BIDIRECTIONAL))
            .isEqualTo(CallAttributesCompat.CALL_TYPE_VIDEO_CALL)

        // STATE_TX_ENABLED (1) collides with CALL_TYPE_AUDIO_CALL (1).
        // We prioritize CALL_TYPE_AUDIO_CALL (1) to prevent audio calls from routing to speaker.
        assertThat(Utils.toCallTypeCompat(VideoProfile.STATE_TX_ENABLED))
            .isEqualTo(CallAttributesCompat.CALL_TYPE_AUDIO_CALL)

        // STATE_RX_ENABLED -> CALL_TYPE_VIDEO_CALL (contains video)
        assertThat(Utils.toCallTypeCompat(VideoProfile.STATE_RX_ENABLED))
            .isEqualTo(CallAttributesCompat.CALL_TYPE_VIDEO_CALL)
    }

    @Test
    fun testGetBundleWithPhoneAccountHandle_OutgoingAudio() {
        val attributes =
            CallAttributesCompat(
                "DisplayName",
                Uri.parse("tel:123456"),
                CallAttributesCompat.DIRECTION_OUTGOING,
                CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
            )

        val bundle = Utils.getBundleWithPhoneAccountHandle(attributes, mPhoneAccountHandle)

        assertThat(
                BundleCompat.getParcelable(
                    bundle,
                    TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                    PhoneAccountHandle::class.java,
                )
            )
            .isEqualTo(mPhoneAccountHandle)
        assertThat(bundle.getInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE))
            .isEqualTo(VideoProfile.STATE_AUDIO_ONLY)
        assertThat(bundle.containsKey(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS)).isFalse()
        assertThat(bundle.containsKey(TelecomManager.EXTRA_INCOMING_VIDEO_STATE)).isFalse()
    }

    @Test
    fun testGetBundleWithPhoneAccountHandle_OutgoingVideo() {
        val attributes =
            CallAttributesCompat(
                "DisplayName",
                Uri.parse("tel:123456"),
                CallAttributesCompat.DIRECTION_OUTGOING,
                CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
            )

        val bundle = Utils.getBundleWithPhoneAccountHandle(attributes, mPhoneAccountHandle)

        assertThat(
                BundleCompat.getParcelable(
                    bundle,
                    TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                    PhoneAccountHandle::class.java,
                )
            )
            .isEqualTo(mPhoneAccountHandle)
        assertThat(bundle.getInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE))
            .isEqualTo(VideoProfile.STATE_BIDIRECTIONAL)
        assertThat(bundle.containsKey(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS)).isFalse()
        assertThat(bundle.containsKey(TelecomManager.EXTRA_INCOMING_VIDEO_STATE)).isFalse()
    }

    @Test
    fun testGetBundleWithPhoneAccountHandle_IncomingAudio() {
        val address = Uri.parse("tel:123456")
        val attributes =
            CallAttributesCompat(
                "DisplayName",
                address,
                CallAttributesCompat.DIRECTION_INCOMING,
                CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
            )

        val bundle = Utils.getBundleWithPhoneAccountHandle(attributes, mPhoneAccountHandle)

        assertThat(
                BundleCompat.getParcelable(
                    bundle,
                    TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                    PhoneAccountHandle::class.java,
                )
            )
            .isEqualTo(mPhoneAccountHandle)
        assertThat(
                BundleCompat.getParcelable(
                    bundle,
                    TelecomManager.EXTRA_INCOMING_CALL_ADDRESS,
                    Uri::class.java,
                )
            )
            .isEqualTo(address)
        assertThat(bundle.getInt(TelecomManager.EXTRA_INCOMING_VIDEO_STATE))
            .isEqualTo(VideoProfile.STATE_AUDIO_ONLY)
        assertThat(bundle.containsKey(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE)).isFalse()
    }

    @Test
    fun testGetBundleWithPhoneAccountHandle_IncomingVideo() {
        val address = Uri.parse("tel:123456")
        val attributes =
            CallAttributesCompat(
                "DisplayName",
                address,
                CallAttributesCompat.DIRECTION_INCOMING,
                CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
            )

        val bundle = Utils.getBundleWithPhoneAccountHandle(attributes, mPhoneAccountHandle)

        assertThat(
                BundleCompat.getParcelable(
                    bundle,
                    TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                    PhoneAccountHandle::class.java,
                )
            )
            .isEqualTo(mPhoneAccountHandle)
        assertThat(
                BundleCompat.getParcelable(
                    bundle,
                    TelecomManager.EXTRA_INCOMING_CALL_ADDRESS,
                    Uri::class.java,
                )
            )
            .isEqualTo(address)
        assertThat(bundle.getInt(TelecomManager.EXTRA_INCOMING_VIDEO_STATE))
            .isEqualTo(VideoProfile.STATE_BIDIRECTIONAL)
        assertThat(bundle.containsKey(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE)).isFalse()
    }
}
