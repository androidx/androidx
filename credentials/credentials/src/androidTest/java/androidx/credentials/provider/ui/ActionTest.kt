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
import androidx.credentials.provider.Action
import androidx.credentials.provider.Action.Companion.fromSlice
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
class ActionTest {
    private val mContext = ApplicationProvider.getApplicationContext<Context>()
    private val mIntent = Intent()
    private val mPendingIntent = PendingIntent.getActivity(mContext, 0, mIntent,
        PendingIntent.FLAG_IMMUTABLE)

    @Test
    fun constructor_success() {
        val action = Action(TITLE, mPendingIntent, SUBTITLE)

        assertNotNull(action)
        assertThat(TITLE == action.title)
        assertThat(SUBTITLE == action.subtitle)
        assertThat(mPendingIntent === action.pendingIntent)
    }

    @Test
    fun constructor_emptyTitle_throwsIllegalArgumentException() {
        Assert.assertThrows(
            "Expected empty title to throw IllegalArgumentException",
            IllegalArgumentException::class.java
        ) { Action("", mPendingIntent, SUBTITLE) }
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun fromSlice_success() {
        val originalAction = Action(TITLE, mPendingIntent, SUBTITLE)
        val slice = Action.toSlice(originalAction)

        val fromSlice = fromSlice(slice)

        assertNotNull(fromSlice)
        fromSlice?.let {
            assertThat(fromSlice.title).isEqualTo(TITLE)
            assertThat(fromSlice.subtitle).isEqualTo(SUBTITLE)
            assertThat(fromSlice.pendingIntent).isEqualTo(mPendingIntent)
        }
    }

    companion object {
        private val TITLE: CharSequence = "title"
        private val SUBTITLE: CharSequence = "subtitle"
    }
}
