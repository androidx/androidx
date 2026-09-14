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

package androidx.credentials.agesignals.exceptions

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class GetAgeRangeExceptionTest {

    @Test
    fun interruptedException_typeMessageAndInheritance() {
        val exception = GetAgeRangeInterruptedException("Operation interrupted")
        assertThat(exception).isInstanceOf(GetAgeRangeException::class.java)
        assertThat(exception.type)
            .isEqualTo(GetAgeRangeInterruptedException.TYPE_GET_AGE_RANGE_INTERRUPTED_EXCEPTION)
        assertThat(exception.message).isEqualTo("Operation interrupted")
        assertThat(exception.errorMessage).isEqualTo("Operation interrupted")

        val noMsgException = GetAgeRangeInterruptedException()
        assertThat(noMsgException).isInstanceOf(GetAgeRangeException::class.java)
        assertThat(noMsgException.type)
            .isEqualTo(GetAgeRangeInterruptedException.TYPE_GET_AGE_RANGE_INTERRUPTED_EXCEPTION)
        assertThat(noMsgException.message).isNull()
        assertThat(noMsgException.errorMessage).isNull()
    }

    @Test
    fun unknownException_typeMessageAndInheritance() {
        val exception = GetAgeRangeUnknownException("Unknown failure")
        assertThat(exception).isInstanceOf(GetAgeRangeException::class.java)
        assertThat(exception.type)
            .isEqualTo(GetAgeRangeUnknownException.TYPE_GET_AGE_RANGE_UNKNOWN_EXCEPTION)
        assertThat(exception.message).isEqualTo("Unknown failure")
        assertThat(exception.errorMessage).isEqualTo("Unknown failure")

        val noMsgException = GetAgeRangeUnknownException()
        assertThat(noMsgException).isInstanceOf(GetAgeRangeException::class.java)
        assertThat(noMsgException.type)
            .isEqualTo(GetAgeRangeUnknownException.TYPE_GET_AGE_RANGE_UNKNOWN_EXCEPTION)
        assertThat(noMsgException.message).isNull()
        assertThat(noMsgException.errorMessage).isNull()
    }

    @Test
    fun providerConfigurationException_typeMessageAndInheritance() {
        val exception = GetAgeRangeProviderConfigurationException("Provider not found")
        assertThat(exception).isInstanceOf(GetAgeRangeException::class.java)
        assertThat(exception.type)
            .isEqualTo(
                GetAgeRangeProviderConfigurationException
                    .TYPE_GET_AGE_RANGE_PROVIDER_CONFIGURATION_EXCEPTION
            )
        assertThat(exception.message).isEqualTo("Provider not found")
        assertThat(exception.errorMessage).isEqualTo("Provider not found")

        val noMsgException = GetAgeRangeProviderConfigurationException()
        assertThat(noMsgException).isInstanceOf(GetAgeRangeException::class.java)
        assertThat(noMsgException.type)
            .isEqualTo(
                GetAgeRangeProviderConfigurationException
                    .TYPE_GET_AGE_RANGE_PROVIDER_CONFIGURATION_EXCEPTION
            )
        assertThat(noMsgException.message).isNull()
        assertThat(noMsgException.errorMessage).isNull()
    }

    @Test
    fun unavailableException_typeMessageAndInheritance() {
        val exception = GetAgeRangeUnavailableException("Age range unavailable")
        assertThat(exception).isInstanceOf(GetAgeRangeException::class.java)
        assertThat(exception.type)
            .isEqualTo(GetAgeRangeUnavailableException.TYPE_GET_AGE_RANGE_UNAVAILABLE_EXCEPTION)
        assertThat(exception.message).isEqualTo("Age range unavailable")
        assertThat(exception.errorMessage).isEqualTo("Age range unavailable")

        val noMsgException = GetAgeRangeUnavailableException()
        assertThat(noMsgException).isInstanceOf(GetAgeRangeException::class.java)
        assertThat(noMsgException.type)
            .isEqualTo(GetAgeRangeUnavailableException.TYPE_GET_AGE_RANGE_UNAVAILABLE_EXCEPTION)
        assertThat(noMsgException.message).isNull()
        assertThat(noMsgException.errorMessage).isNull()
    }

    @Test
    fun baseException_typeMessageAndInheritance() {
        val exception = object : GetAgeRangeException("custom_type", "custom error") {}
        assertThat(exception).isInstanceOf(GetAgeRangeException::class.java)
        assertThat(exception).isInstanceOf(Exception::class.java)
        assertThat(exception.type).isEqualTo("custom_type")
        assertThat(exception.message).isEqualTo("custom error")
        assertThat(exception.errorMessage).isEqualTo("custom error")

        val noMsgException = object : GetAgeRangeException("custom_type") {}
        assertThat(noMsgException).isInstanceOf(GetAgeRangeException::class.java)
        assertThat(noMsgException).isInstanceOf(Exception::class.java)
        assertThat(noMsgException.type).isEqualTo("custom_type")
        assertThat(noMsgException.message).isNull()
        assertThat(noMsgException.errorMessage).isNull()
    }
}
