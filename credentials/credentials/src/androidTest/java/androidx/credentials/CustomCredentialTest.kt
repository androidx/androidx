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

package androidx.credentials

import android.os.Bundle
import androidx.credentials.Credential.Companion.createFrom
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CustomCredentialTest {
    @Test
    fun constructor_emptyType_throws() {
        Assert.assertThrows(
            "Expected empty type to throw IAE",
            IllegalArgumentException::class.java
        ) {
            CustomCredential("", Bundle())
        }
    }

    @Test
    fun constructor_nonEmptyTypeNonNullBundle_success() {
        CustomCredential("T", Bundle())
    }

    @Test
    fun getter_frameworkProperties() {
        val expectedType = "TYPE"
        val expectedBundle = Bundle()
        expectedBundle.putString("Test", "Test")
        val option = CustomCredential(expectedType, expectedBundle)
        assertThat(option.type).isEqualTo(expectedType)
        assertThat(equals(option.data, expectedBundle)).isTrue()
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun frameworkConversion_frameworkClass_success() {
        val expectedType = "TYPE"
        val expectedBundle = Bundle()
        expectedBundle.putString("Test", "Test")
        val credential = CustomCredential(expectedType, expectedBundle)

        val convertedCredential =
            createFrom(android.credentials.Credential(credential.type, credential.data))

        equals(convertedCredential, credential)
    }
}
