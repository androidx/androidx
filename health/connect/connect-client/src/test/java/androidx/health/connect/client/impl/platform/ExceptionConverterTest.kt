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

@file:SuppressLint("NewApi")

package androidx.health.connect.client.impl.platform

import android.annotation.SuppressLint
import android.health.connect.HealthConnectException
import android.os.Build
import androidx.health.connect.client.exceptions.RateLimitExceededException
import androidx.health.connect.client.exceptions.RecordTooLargeException
import androidx.health.connect.client.exceptions.RequestTooLargeException
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class ExceptionConverterTest {

    @Test
    fun toKtException_rateLimitExceeded_returnsRateLimitExceededException() {
        val platformException =
            mockHealthConnectException(
                HealthConnectException.ERROR_RATE_LIMIT_EXCEEDED,
                "API call quota exceeded",
            )

        val result = platformException.toKtException()

        assertThat(result).isInstanceOf(RateLimitExceededException::class.java)
        assertThat(result.message).isEqualTo("API call quota exceeded")
        assertThat(result.cause).isSameInstanceAs(platformException)
    }

    @Test
    fun toKtException_requestTooLargeErrorCode_returnsRequestTooLargeException() {
        val platformException =
            mockHealthConnectException(
                ERROR_REQUEST_TOO_LARGE,
                "Request too large",
            )

        val result = platformException.toKtException()

        assertThat(result).isInstanceOf(RequestTooLargeException::class.java)
        assertThat(result.message).isEqualTo("Request too large")
        assertThat(result.cause).isSameInstanceAs(platformException)
    }

    @Test
    fun toKtException_recordTooLargeErrorCode_returnsRecordTooLargeException() {
        val platformException =
            mockHealthConnectException(
                ERROR_RECORD_TOO_LARGE,
                "Record too large",
            )

        val result = platformException.toKtException()

        assertThat(result).isInstanceOf(RecordTooLargeException::class.java)
        assertThat(result.message).isEqualTo("Record too large")
        assertThat(result.cause).isSameInstanceAs(platformException)
    }

    @Test
    fun toKtException_legacyChunkSizeMessageFallback_returnsRequestTooLargeException() {
        val message = "Records chunk size exceeded the max chunk limit: 5000000, was: 5000001"
        val platformException =
            mockHealthConnectException(
                HealthConnectException.ERROR_RATE_LIMIT_EXCEEDED,
                message,
            )

        val result = platformException.toKtException()

        assertThat(result).isInstanceOf(RequestTooLargeException::class.java)
        assertThat(result.message).isEqualTo(message)
        assertThat(result.cause).isSameInstanceAs(platformException)
    }

    @Test
    fun toKtException_legacyRecordSizeMessageFallback_returnsRecordTooLargeException() {
        val message = "Record size exceeded the single record size limit: 1000000, was: 1000001"
        val platformException =
            mockHealthConnectException(
                HealthConnectException.ERROR_RATE_LIMIT_EXCEEDED,
                message,
            )

        val result = platformException.toKtException()

        assertThat(result).isInstanceOf(RecordTooLargeException::class.java)
        assertThat(result.message).isEqualTo(message)
        assertThat(result.cause).isSameInstanceAs(platformException)
    }

    private fun mockHealthConnectException(
        errorCode: Int,
        message: String?,
    ): HealthConnectException {
        val exception = mock(HealthConnectException::class.java)
        `when`(exception.errorCode).thenReturn(errorCode)
        `when`(exception.message).thenReturn(message)
        return exception
    }
}
