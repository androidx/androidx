/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.input

import android.app.RemoteInput
import android.view.inputmethod.EditorInfo.IME_ACTION_DONE
import android.view.inputmethod.EditorInfo.IME_ACTION_GO
import android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
import android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
import android.view.inputmethod.EditorInfo.IME_ACTION_SEND
import androidx.wear.input.WearableRemoteInputExtender.Companion.INPUT_ACTION_TYPE_DONE
import androidx.wear.input.WearableRemoteInputExtender.Companion.INPUT_ACTION_TYPE_GO
import androidx.wear.input.WearableRemoteInputExtender.Companion.INPUT_ACTION_TYPE_SEARCH
import androidx.wear.input.WearableRemoteInputExtender.Companion.INPUT_ACTION_TYPE_SEND
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(WearInputTestRunner::class)
class WearableRemoteInputExtenderTest {
    @Test
    fun testDisallowEmoji_setTrue() {
        val remoteInput = RemoteInput.Builder("resultKey")
            .wearableExtender {
                disallowEmoji()
            }.build()

        assertTrue(
            remoteInput.extras.getBoolean(WearableRemoteInputExtender.EXTRA_DISALLOW_EMOJI)
        )
        // Test that input action type is not set.
        assertEquals(
            -1, remoteInput.extras.getInt(WearableRemoteInputExtender.EXTRA_INPUT_ACTION_TYPE, -1)
        )
    }

    @Test
    fun testDisallowEmoji_notSet() {
        val remoteInput: RemoteInput = RemoteInput.Builder("resultKey")
            .wearableExtender {
                // empty
            }.build()

        assertFalse(
            remoteInput.extras.getBoolean(WearableRemoteInputExtender.EXTRA_DISALLOW_EMOJI)
        )
        // Test that input action type is not set.
        assertEquals(
            -1, remoteInput.extras.getInt(WearableRemoteInputExtender.EXTRA_INPUT_ACTION_TYPE, -1)
        )
    }

    @Test
    fun testSetInputActionType() {
        for ((ime, iat) in imeToActionTypeMap) {
            val remoteInput = RemoteInput.Builder("resultKey")
                .wearableExtender {
                    setInputActionType(ime)
                }.build()

            assertEquals(
                iat,
                remoteInput.extras.getInt(WearableRemoteInputExtender.EXTRA_INPUT_ACTION_TYPE),
            )

            // Test that disallowing emoji is not set.
            assertFalse(
                remoteInput.extras.getBoolean(WearableRemoteInputExtender.EXTRA_DISALLOW_EMOJI)
            )
        }
    }

    @Test
    fun testDisallowEmoji_SetInputActionType() {
        val remoteInput = RemoteInput.Builder("resultKey")
            .wearableExtender {
                disallowEmoji()
                setInputActionType(IME_ACTION_GO)
            }.build()

        assertTrue(
            remoteInput.extras.getBoolean(WearableRemoteInputExtender.EXTRA_DISALLOW_EMOJI)
        )
        assertEquals(
            INPUT_ACTION_TYPE_GO,
            remoteInput.extras.getInt(WearableRemoteInputExtender.EXTRA_INPUT_ACTION_TYPE)
        )
    }

    companion object {
        val imeToActionTypeMap = hashMapOf(
            IME_ACTION_SEND to INPUT_ACTION_TYPE_SEND,
            IME_ACTION_SEARCH to INPUT_ACTION_TYPE_SEARCH,
            IME_ACTION_DONE to INPUT_ACTION_TYPE_DONE,
            IME_ACTION_GO to INPUT_ACTION_TYPE_GO,
            IME_ACTION_NEXT to INPUT_ACTION_TYPE_SEND // other value
        )
    }
}