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
package androidx.core.net

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class MailToTest {
    @Test
    fun isMailTo_withNullArgument_shouldReturnFalse() {
        assertThat(MailTo.isMailTo(null)).isFalse()
    }

    @Test
    fun isMailTo_withEmptyString_shouldReturnFalse() {
        assertThat(MailTo.isMailTo("")).isFalse()
    }

    @Test
    fun isMailTo_withHttpUrl_shouldReturnFalse() {
        assertThat(MailTo.isMailTo("http://www.google.com")).isFalse()
    }

    @Test
    fun isMailTo_withValidMailtoUris_shouldReturnTrue() {
        assertThat(MailTo.isMailTo(MAILTOURI_1)).isTrue()
        assertThat(MailTo.isMailTo(MAILTOURI_2)).isTrue()
        assertThat(MailTo.isMailTo(MAILTOURI_3)).isTrue()
        assertThat(MailTo.isMailTo(MAILTOURI_4)).isTrue()
        assertThat(MailTo.isMailTo(MAILTOURI_5)).isTrue()
        assertThat(MailTo.isMailTo(MAILTOURI_6)).isTrue()
    }

    @Test
    fun simpleMailtoUri() {
        val mailTo = MailTo.parse(MAILTOURI_1)

        assertThat(mailTo.to).isEqualTo("chris@example.com")
        assertThat(mailTo.headers).hasSize(1)
        assertThat(mailTo.body).isNull()
        assertThat(mailTo.cc).isNull()
        assertThat(mailTo.subject).isNull()
        assertThat(mailTo.toString()).isEqualTo("mailto:?to=chris%40example.com&")
    }

    @Test
    fun subjectQueryParameter() {
        val mailTo = MailTo.parse(MAILTOURI_2)

        assertThat(mailTo.headers).hasSize(2)
        assertThat(mailTo.to).isEqualTo("infobot@example.com")
        assertThat(mailTo.subject).isEqualTo("current-issue")
        assertThat(mailTo.body).isNull()
        assertThat(mailTo.cc).isNull()
        val stringUrl = mailTo.toString()
        assertThat(stringUrl).startsWith("mailto:?")
        assertThat(stringUrl).contains("to=infobot%40example.com&")
        assertThat(stringUrl).contains("subject=current-issue&")
    }

    @Test
    fun bodyQueryParameter() {
        val mailTo = MailTo.parse(MAILTOURI_3)

        assertThat(mailTo.headers).hasSize(2)
        assertThat(mailTo.to).isEqualTo("infobot@example.com")
        assertThat(mailTo.body).isEqualTo("send current-issue")
        assertThat(mailTo.cc).isNull()
        assertThat(mailTo.subject).isNull()
        val stringUrl = mailTo.toString()
        assertThat(stringUrl).startsWith("mailto:?")
        assertThat(stringUrl).contains("to=infobot%40example.com&")
        assertThat(stringUrl).contains("body=send%20current-issue&")
    }

    @Test
    fun bodyQueryParameterWithLineBreak() {
        val mailTo = MailTo.parse(MAILTOURI_4)

        assertThat(mailTo.headers).hasSize(2)
        assertThat(mailTo.to).isEqualTo("infobot@example.com")
        assertThat(mailTo.body).isEqualTo("send current-issue\r\nsend index")
        assertThat(mailTo.cc).isNull()
        assertThat(mailTo.subject).isNull()
        val stringUrl = mailTo.toString()
        assertThat(stringUrl).startsWith("mailto:?")
        assertThat(stringUrl).contains("to=infobot%40example.com&")
        assertThat(stringUrl).contains("body=send%20current-issue%0D%0Asend%20index&")
    }

    @Test
    fun ccAndBodyQueryParameters() {
        val mailTo = MailTo.parse(MAILTOURI_5)

        assertThat(mailTo.headers).hasSize(3)
        assertThat(mailTo.to).isEqualTo("joe@example.com")
        assertThat(mailTo.cc).isEqualTo("bob@example.com")
        assertThat(mailTo.body).isEqualTo("hello")
        assertThat(mailTo.subject).isNull()
        val stringUrl = mailTo.toString()
        assertThat(stringUrl).startsWith("mailto:?")
        assertThat(stringUrl).contains("cc=bob%40example.com&")
        assertThat(stringUrl).contains("body=hello&")
        assertThat(stringUrl).contains("to=joe%40example.com&")
    }

    @Test
    fun toAndCcQueryParameters() {
        val mailTo = MailTo.parse(MAILTOURI_6)

        assertThat(mailTo.headers).hasSize(3)
        assertThat(mailTo.to).isEqualTo(", joe@example.com")
        assertThat(mailTo.cc).isEqualTo("bob@example.com")
        assertThat(mailTo.body).isEqualTo("hello")
        assertThat(mailTo.subject).isNull()
        val stringUrl = mailTo.toString()
        assertThat(stringUrl).startsWith("mailto:?")
        assertThat(stringUrl).contains("cc=bob%40example.com&")
        assertThat(stringUrl).contains("body=hello&")
        assertThat(stringUrl).contains("to=%2C%20joe%40example.com&")
    }

    @Test
    fun encodedAmpersandInBody() {
        val mailTo = MailTo.parse("mailto:alice@example.com?body=a%26b")

        assertThat(mailTo.body).isEqualTo("a&b")
    }

    @Test
    fun encodedEqualSignInBody() {
        val mailTo = MailTo.parse("mailto:alice@example.com?body=a%3Db")

        assertThat(mailTo.body).isEqualTo("a=b")
    }

    @Test
    fun unencodedEqualsSignInBody() {
        // This is not a properly encoded mailto URI. But there's no good reason to drop everything
        // after the equals sign in the 'body' query parameter value.
        val mailTo = MailTo.parse("mailto:alice@example.com?body=foo=bar&subject=test")

        assertThat(mailTo.body).isEqualTo("foo=bar")
        assertThat(mailTo.subject).isEqualTo("test")
    }

    @Test
    fun encodedPercentValueInBody() {
        val mailTo = MailTo.parse("mailto:alice@example.com?body=%2525")

        assertThat(mailTo.body).isEqualTo("%25")
    }

    @Test
    fun colonInBody() {
        val mailTo = MailTo.parse("mailto:alice@example.com?body=one:two")

        assertThat(mailTo.body).isEqualTo("one:two")
    }

    @Test
    fun emailAddressAndFragment() {
        val mailTo = MailTo.parse("mailto:alice@example.com#fragment")

        assertThat(mailTo.to).isEqualTo("alice@example.com")
    }

    @Test
    fun emailAddressAndQueryAndFragment() {
        val mailTo = MailTo.parse("mailto:alice@example.com?cc=bob@example.com#fragment")

        assertThat(mailTo.to).isEqualTo("alice@example.com")
        assertThat(mailTo.cc).isEqualTo("bob@example.com")
    }

    @Test
    fun fragmentWithValueThatLooksLikeQueryPart() {
        val mailTo = MailTo.parse("mailto:#?to=alice@example.com")

        assertThat(mailTo.to).isEqualTo("")
    }

    @Test
    fun bccRecipient() {
        val mailTo = MailTo.parse("mailto:alice@example.com?BCC=joe@example.com")

        assertThat(mailTo.bcc).isEqualTo("joe@example.com")
    }

    companion object {
        private const val MAILTOURI_1 = "mailto:chris@example.com"
        private const val MAILTOURI_2 = "mailto:infobot@example.com?subject=current-issue"
        private const val MAILTOURI_3 = "mailto:infobot@example.com?body=send%20current-issue"
        private const val MAILTOURI_4 = "mailto:infobot@example.com?body=send%20current-" +
                "issue%0D%0Asend%20index" // NOTYPO
        private const val MAILTOURI_5 = "mailto:joe@example.com?cc=bob@example.com&body=hello"
        private const val MAILTOURI_6 = "mailto:?to=joe@example.com&cc=bob@example.com&body=hello"
    }
}