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
package androidx.credentials.provider.ui

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.credentials.provider.AuthenticationAction
import androidx.credentials.provider.AuthenticationAction.Companion.fromSlice
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class AuthenticationActionTest {
    private val mContext = ApplicationProvider.getApplicationContext<Context>()
    private val mIntent = Intent()
    private val mPendingIntent = PendingIntent.getActivity(mContext, 0, mIntent,
        PendingIntent.FLAG_IMMUTABLE)

    @Test
    fun build_success() {
        val action = AuthenticationAction.Builder(TITLE, mPendingIntent).build()

        assertThat(mPendingIntent).isEqualTo(action.pendingIntent)
    }

    @Test
    fun constructor_success() {
        val action = AuthenticationAction(TITLE, mPendingIntent)

        assertThat(mPendingIntent).isEqualTo(action.pendingIntent)
    }

    @Test
    fun constructor_emptyTitle_throwsIllegalArgumentException() {
        Assert.assertThrows(
            "Expected empty title to throw IAE",
            IllegalArgumentException::class.java
        ) { AuthenticationAction("", mPendingIntent) }
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun fromSlice_success() {
        val originalAction = AuthenticationAction(TITLE, mPendingIntent)
        val slice = AuthenticationAction.toSlice(originalAction)

        val fromSlice = fromSlice(slice)

        assertNotNull(fromSlice)
        fromSlice?.let {
            assertNotNull(fromSlice.pendingIntent)
            assertThat(fromSlice.pendingIntent).isEqualTo(mPendingIntent)
        }
    }

    companion object {
        private val TITLE: CharSequence = "title"
    }
}
