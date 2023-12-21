/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.credentials.provider

import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.os.BuildCompat
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.equals
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@RequiresApi(34)
class BeginGetPasswordOptionTest {
    companion object {
        private const val BUNDLE_ID_KEY =
            "android.service.credentials.BeginGetCredentialOption.BUNDLE_ID_KEY"
        private const val BUNDLE_ID = "id"
    }
    @Test
    fun getter_frameworkProperties() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        val expectedAllowedUserIds: Set<String> = setOf("id1", "id2", "id3")
        val bundle = Bundle()
        bundle.putStringArrayList(
            GetPasswordOption.BUNDLE_KEY_ALLOWED_USER_IDS,
            ArrayList(expectedAllowedUserIds)
        )

        val option = BeginGetPasswordOption(expectedAllowedUserIds, bundle, BUNDLE_ID)

        bundle.putString(BUNDLE_ID_KEY, BUNDLE_ID)
        assertThat(option.type).isEqualTo(PasswordCredential.TYPE_PASSWORD_CREDENTIAL)
        assertThat(equals(option.candidateQueryData, bundle)).isTrue()
        assertThat(option.allowedUserIds).containsExactlyElementsIn(expectedAllowedUserIds)
    }

    // TODO ("Add framework conversion, createFrom tests")
}