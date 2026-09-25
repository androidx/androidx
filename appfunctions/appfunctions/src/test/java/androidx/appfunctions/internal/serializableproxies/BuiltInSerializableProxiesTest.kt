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

package androidx.appfunctions.internal.serializableproxies

import android.net.Uri
import androidx.appfunctions.AppFunctionData
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 33)
class BuiltInSerializableProxiesTest {

    @Test
    fun supportedProxyClasses_containsExpectedTypes() {
        assertThat(BuiltInSerializableProxies.supportedProxyClasses)
            .containsExactly(
                LocalDate::class.java,
                LocalTime::class.java,
                LocalDateTime::class.java,
                Uri::class.java,
                Instant::class.java,
                ZoneId::class.java,
            )
    }

    @Test
    fun isSupportedProxy_returnsTrueForSupportedTypes() {
        assertThat(BuiltInSerializableProxies.isSupportedProxy(LocalDate::class.java)).isTrue()
        assertThat(BuiltInSerializableProxies.isSupportedProxy(LocalTime::class.java)).isTrue()
        assertThat(BuiltInSerializableProxies.isSupportedProxy(LocalDateTime::class.java)).isTrue()
        assertThat(BuiltInSerializableProxies.isSupportedProxy(Uri::class.java)).isTrue()
        assertThat(BuiltInSerializableProxies.isSupportedProxy(Instant::class.java)).isTrue()
        assertThat(BuiltInSerializableProxies.isSupportedProxy(ZoneId::class.java)).isTrue()
    }

    @Test
    fun isSupportedProxy_returnsFalseForUnsupportedTypes() {
        assertThat(BuiltInSerializableProxies.isSupportedProxy(String::class.java)).isFalse()
        assertThat(BuiltInSerializableProxies.isSupportedProxy(Int::class.java)).isFalse()
        assertThat(BuiltInSerializableProxies.isSupportedProxy(Any::class.java)).isFalse()
    }

    @Test
    fun getFactory_returnsNonNullFactoryForSupportedTypes() {
        assertThat(BuiltInSerializableProxies.getFactory(LocalDate::class.java)).isNotNull()
        assertThat(BuiltInSerializableProxies.getFactory(LocalTime::class.java)).isNotNull()
        assertThat(BuiltInSerializableProxies.getFactory(LocalDateTime::class.java)).isNotNull()
        assertThat(BuiltInSerializableProxies.getFactory(Uri::class.java)).isNotNull()
        assertThat(BuiltInSerializableProxies.getFactory(Instant::class.java)).isNotNull()
        assertThat(BuiltInSerializableProxies.getFactory(ZoneId::class.java)).isNotNull()
    }

    @Test
    fun getFactory_returnsNullForUnsupportedTypes() {
        assertThat(BuiltInSerializableProxies.getFactory(String::class.java)).isNull()
    }

    @Test
    fun appFunctionData_serializeAndDeserialize_localDate() {
        val original = LocalDate.of(2026, 9, 22)
        val data = AppFunctionData.serialize(original, LocalDate::class.java)

        assertThat(data.getInt("year")).isEqualTo(2026)
        assertThat(data.getInt("month")).isEqualTo(9)
        assertThat(data.getInt("dayOfMonth")).isEqualTo(22)

        val deserialized = data.deserialize(LocalDate::class.java)
        assertThat(deserialized).isEqualTo(original)
    }

    @Test
    fun appFunctionData_serializeAndDeserialize_localTime() {
        val original = LocalTime.of(14, 30, 45, 123456789)
        val data = AppFunctionData.serialize(original, LocalTime::class.java)

        assertThat(data.getInt("hour")).isEqualTo(14)
        assertThat(data.getInt("minute")).isEqualTo(30)
        assertThat(data.getInt("second")).isEqualTo(45)
        assertThat(data.getInt("nanoOfSecond")).isEqualTo(123456789)

        val deserialized = data.deserialize(LocalTime::class.java)
        assertThat(deserialized).isEqualTo(original)
    }

    @Test
    fun appFunctionData_serializeAndDeserialize_localDateTime() {
        val original = LocalDateTime.of(2026, 9, 22, 14, 30, 45, 123456789)
        val data = AppFunctionData.serialize(original, LocalDateTime::class.java)

        val deserialized = data.deserialize(LocalDateTime::class.java)
        assertThat(deserialized).isEqualTo(original)
    }

    @Test
    fun appFunctionData_serializeAndDeserialize_uri() {
        val original = Uri.parse("https://android.example.com/appfunctions")
        val data = AppFunctionData.serialize(original, Uri::class.java)

        assertThat(data.getString("uri")).isEqualTo("https://android.example.com/appfunctions")

        val deserialized = data.deserialize(Uri::class.java)
        assertThat(deserialized).isEqualTo(original)
    }

    @Test
    fun appFunctionData_serializeAndDeserialize_instant() {
        val original = Instant.ofEpochMilli(1700000000000L)
        val data = AppFunctionData.serialize(original, Instant::class.java)

        val deserialized = data.deserialize(Instant::class.java)
        assertThat(deserialized).isEqualTo(original)
    }

    @Test
    fun appFunctionData_serializeAndDeserialize_zoneId() {
        val original = ZoneId.of("America/Los_Angeles")
        val data = AppFunctionData.serialize(original, ZoneId::class.java)

        val deserialized = data.deserialize(ZoneId::class.java)
        assertThat(deserialized).isEqualTo(original)
    }
}
