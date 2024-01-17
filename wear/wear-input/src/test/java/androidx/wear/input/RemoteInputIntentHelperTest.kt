/*
 * Copyright 2020 The Android Open Source Project
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
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(WearInputTestRunner::class)
class RemoteInputIntentHelperTest {
    private val remoteInputs: List<RemoteInput> = listOf(
        RemoteInput.Builder("ri1").build(), RemoteInput.Builder("ri2").build()
    )

    @Test
    fun testCreateIntentWithRemoteInputAction() {
        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
        assertTrue(RemoteInputIntentHelper.isActionRemoteInput(intent))
    }

    @Test
    fun testCreateIntentWithoutRemoteInputAction() {
        val intent = Intent()
        assertFalse(RemoteInputIntentHelper.isActionRemoteInput(intent))
    }

    @Test
    fun testHasRemoteInputs() {
        val intent = Intent()
        RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
        assertTrue(RemoteInputIntentHelper.hasRemoteInputsExtra(intent))
    }

    @Test
    fun testGetRemoteInputs() {
        val intent = Intent()
        RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
        assertEquals(remoteInputs, RemoteInputIntentHelper.getRemoteInputsExtra(intent))
    }

    @Test
    fun testGetTitle() {
        val intent = Intent()
        val title = "Test Title"
        RemoteInputIntentHelper.putTitleExtra(intent, title)
        assertEquals(title, RemoteInputIntentHelper.getTitleExtra(intent))
    }

    @Test
    fun testGetCancelLabel() {
        val intent = Intent()
        val title = "Test Cancel Label"
        RemoteInputIntentHelper.putCancelLabelExtra(intent, title)
        assertEquals(title, RemoteInputIntentHelper.getCancelLabelExtra(intent))
    }

    @Test
    fun testGetConfirmationLabel() {
        val intent = Intent()
        val title = "Test Confirmation Label"
        RemoteInputIntentHelper.putConfirmLabelExtra(intent, title)
        assertEquals(title, RemoteInputIntentHelper.getConfirmLabelExtra(intent))
    }

    @Test
    fun testGetInProgressLabel() {
        val intent = Intent()
        val title = "Test In Progress Label"
        RemoteInputIntentHelper.putInProgressLabelExtra(intent, title)
        assertEquals(title, RemoteInputIntentHelper.getInProgressLabelExtra(intent))
    }

    @Test
    fun testGetSmartReplyContextFromIntent() {
        val intent = Intent()
        val smartReplyContext = listOf<CharSequence>("Test Reply 1", "Test Reply 2")
        RemoteInputIntentHelper.putSmartReplyContextExtra(intent, smartReplyContext)
        assertEquals(smartReplyContext, RemoteInputIntentHelper.getSmartReplyContextExtra(intent))
    }
}
