/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.autofill

import androidx.autofill.HintConstants.AUTOFILL_HINT_BIRTH_DATE_DAY
import androidx.autofill.HintConstants.AUTOFILL_HINT_BIRTH_DATE_FULL
import androidx.autofill.HintConstants.AUTOFILL_HINT_BIRTH_DATE_MONTH
import androidx.autofill.HintConstants.AUTOFILL_HINT_BIRTH_DATE_YEAR
import androidx.autofill.HintConstants.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE
import androidx.autofill.HintConstants.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY
import androidx.autofill.HintConstants.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH
import androidx.autofill.HintConstants.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR
import androidx.autofill.HintConstants.AUTOFILL_HINT_CREDIT_CARD_NUMBER
import androidx.autofill.HintConstants.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE
import androidx.autofill.HintConstants.AUTOFILL_HINT_EMAIL_ADDRESS
import androidx.autofill.HintConstants.AUTOFILL_HINT_GENDER
import androidx.autofill.HintConstants.AUTOFILL_HINT_NEW_PASSWORD
import androidx.autofill.HintConstants.AUTOFILL_HINT_NEW_USERNAME
import androidx.autofill.HintConstants.AUTOFILL_HINT_PASSWORD
import androidx.autofill.HintConstants.AUTOFILL_HINT_PERSON_NAME
import androidx.autofill.HintConstants.AUTOFILL_HINT_PERSON_NAME_FAMILY
import androidx.autofill.HintConstants.AUTOFILL_HINT_PERSON_NAME_GIVEN
import androidx.autofill.HintConstants.AUTOFILL_HINT_PERSON_NAME_MIDDLE
import androidx.autofill.HintConstants.AUTOFILL_HINT_PERSON_NAME_MIDDLE_INITIAL
import androidx.autofill.HintConstants.AUTOFILL_HINT_PERSON_NAME_PREFIX
import androidx.autofill.HintConstants.AUTOFILL_HINT_PERSON_NAME_SUFFIX
import androidx.autofill.HintConstants.AUTOFILL_HINT_PHONE_COUNTRY_CODE
import androidx.autofill.HintConstants.AUTOFILL_HINT_PHONE_NATIONAL
import androidx.autofill.HintConstants.AUTOFILL_HINT_PHONE_NUMBER
import androidx.autofill.HintConstants.AUTOFILL_HINT_PHONE_NUMBER_DEVICE
import androidx.autofill.HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS
import androidx.autofill.HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_COUNTRY
import androidx.autofill.HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_ADDRESS
import androidx.autofill.HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_POSTAL_CODE
import androidx.autofill.HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_LOCALITY
import androidx.autofill.HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_REGION
import androidx.autofill.HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS
import androidx.autofill.HintConstants.AUTOFILL_HINT_POSTAL_CODE
import androidx.autofill.HintConstants.AUTOFILL_HINT_SMS_OTP
import androidx.autofill.HintConstants.AUTOFILL_HINT_USERNAME
import androidx.compose.ui.autofill.ContentType.Companion.AddressAuxiliaryDetails
import androidx.compose.ui.autofill.ContentType.Companion.AddressCountry
import androidx.compose.ui.autofill.ContentType.Companion.AddressLocality
import androidx.compose.ui.autofill.ContentType.Companion.AddressRegion
import androidx.compose.ui.autofill.ContentType.Companion.AddressStreet
import androidx.compose.ui.autofill.ContentType.Companion.BirthDateDay
import androidx.compose.ui.autofill.ContentType.Companion.BirthDateFull
import androidx.compose.ui.autofill.ContentType.Companion.BirthDateMonth
import androidx.compose.ui.autofill.ContentType.Companion.BirthDateYear
import androidx.compose.ui.autofill.ContentType.Companion.CreditCardExpirationDate
import androidx.compose.ui.autofill.ContentType.Companion.CreditCardExpirationDay
import androidx.compose.ui.autofill.ContentType.Companion.CreditCardExpirationMonth
import androidx.compose.ui.autofill.ContentType.Companion.CreditCardExpirationYear
import androidx.compose.ui.autofill.ContentType.Companion.CreditCardNumber
import androidx.compose.ui.autofill.ContentType.Companion.CreditCardSecurityCode
import androidx.compose.ui.autofill.ContentType.Companion.EmailAddress
import androidx.compose.ui.autofill.ContentType.Companion.Gender
import androidx.compose.ui.autofill.ContentType.Companion.NewPassword
import androidx.compose.ui.autofill.ContentType.Companion.NewUsername
import androidx.compose.ui.autofill.ContentType.Companion.Password
import androidx.compose.ui.autofill.ContentType.Companion.PersonFirstName
import androidx.compose.ui.autofill.ContentType.Companion.PersonFullName
import androidx.compose.ui.autofill.ContentType.Companion.PersonLastName
import androidx.compose.ui.autofill.ContentType.Companion.PersonMiddleInitial
import androidx.compose.ui.autofill.ContentType.Companion.PersonMiddleName
import androidx.compose.ui.autofill.ContentType.Companion.PersonNamePrefix
import androidx.compose.ui.autofill.ContentType.Companion.PersonNameSuffix
import androidx.compose.ui.autofill.ContentType.Companion.PhoneCountryCode
import androidx.compose.ui.autofill.ContentType.Companion.PhoneNumber
import androidx.compose.ui.autofill.ContentType.Companion.PhoneNumberDevice
import androidx.compose.ui.autofill.ContentType.Companion.PhoneNumberNational
import androidx.compose.ui.autofill.ContentType.Companion.PostalAddress
import androidx.compose.ui.autofill.ContentType.Companion.PostalCode
import androidx.compose.ui.autofill.ContentType.Companion.PostalCodeExtended
import androidx.compose.ui.autofill.ContentType.Companion.SmsOtpCode
import androidx.compose.ui.autofill.ContentType.Companion.Username
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AndroidContentTypeTest {
    @Test
    fun emailAddress() {
        assertThat(EmailAddress).isEqualTo(ContentType.from(AUTOFILL_HINT_EMAIL_ADDRESS))
    }

    @Test
    fun username() {
        assertThat(Username).isEqualTo(ContentType.from(AUTOFILL_HINT_USERNAME))
    }

    @Test
    fun password() {
        assertThat(Password).isEqualTo(ContentType.from(AUTOFILL_HINT_PASSWORD))
    }

    @Test
    fun newUsername() {
        assertThat(NewUsername).isEqualTo(ContentType.from(AUTOFILL_HINT_NEW_USERNAME))
    }

    @Test
    fun newPassword() {
        assertThat(NewPassword).isEqualTo(ContentType.from(AUTOFILL_HINT_NEW_PASSWORD))
    }

    @Test
    fun postalAddress() {
        assertThat(PostalAddress).isEqualTo(ContentType.from(AUTOFILL_HINT_POSTAL_ADDRESS))
    }

    @Test
    fun postalCode() {
        assertThat(PostalCode).isEqualTo(ContentType.from(AUTOFILL_HINT_POSTAL_CODE))
    }

    @Test
    fun creditCardNumber() {
        assertThat(CreditCardNumber).isEqualTo(ContentType.from(AUTOFILL_HINT_CREDIT_CARD_NUMBER))
    }

    @Test
    fun creditCardSecurityCode() {
        assertThat(CreditCardSecurityCode)
            .isEqualTo(ContentType.from(AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE))
    }

    @Test
    fun creditCardExpirationDate() {
        assertThat(CreditCardExpirationDate)
            .isEqualTo(ContentType.from(AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE))
    }

    @Test
    fun creditCardExpirationMonth() {
        assertThat(CreditCardExpirationMonth)
            .isEqualTo(ContentType.from(AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH))
    }

    @Test
    fun creditCardExpirationYear() {
        assertThat(CreditCardExpirationYear)
            .isEqualTo(ContentType.from(AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR))
    }

    @Test
    fun creditCardExpirationDay() {
        assertThat(CreditCardExpirationDay)
            .isEqualTo(ContentType.from(AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY))
    }

    @Test
    fun addressCountry() {
        assertThat(AddressCountry).isEqualTo(ContentType.from(AUTOFILL_HINT_POSTAL_ADDRESS_COUNTRY))
    }

    @Test
    fun addressRegion() {
        assertThat(AddressRegion).isEqualTo(ContentType.from(AUTOFILL_HINT_POSTAL_ADDRESS_REGION))
    }

    @Test
    fun addressLocality() {
        assertThat(AddressLocality)
            .isEqualTo(ContentType.from(AUTOFILL_HINT_POSTAL_ADDRESS_LOCALITY))
    }

    @Test
    fun addressStreet() {
        assertThat(AddressStreet)
            .isEqualTo(ContentType.from(AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS))
    }

    @Test
    fun addressAuxiliaryDetails() {
        assertThat(AddressAuxiliaryDetails)
            .isEqualTo(ContentType.from(AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_ADDRESS))
    }

    @Test
    fun postalCodeExtended() {
        assertThat(PostalCodeExtended)
            .isEqualTo(ContentType.from(AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_POSTAL_CODE))
    }

    @Test
    fun personFullName() {
        assertThat(PersonFullName).isEqualTo(ContentType.from(AUTOFILL_HINT_PERSON_NAME))
    }

    @Test
    fun personFirstName() {
        assertThat(PersonFirstName).isEqualTo(ContentType.from(AUTOFILL_HINT_PERSON_NAME_GIVEN))
    }

    @Test
    fun personLastName() {
        assertThat(PersonLastName).isEqualTo(ContentType.from(AUTOFILL_HINT_PERSON_NAME_FAMILY))
    }

    @Test
    fun personMiddleName() {
        assertThat(PersonMiddleName).isEqualTo(ContentType.from(AUTOFILL_HINT_PERSON_NAME_MIDDLE))
    }

    @Test
    fun personMiddleInitial() {
        assertThat(PersonMiddleInitial)
            .isEqualTo(ContentType.from(AUTOFILL_HINT_PERSON_NAME_MIDDLE_INITIAL))
    }

    @Test
    fun personNamePrefix() {
        assertThat(PersonNamePrefix).isEqualTo(ContentType.from(AUTOFILL_HINT_PERSON_NAME_PREFIX))
    }

    @Test
    fun personNameSuffix() {
        assertThat(PersonNameSuffix).isEqualTo(ContentType.from(AUTOFILL_HINT_PERSON_NAME_SUFFIX))
    }

    @Test
    fun phoneNumber() {
        assertThat(PhoneNumber).isEqualTo(ContentType.from(AUTOFILL_HINT_PHONE_NUMBER))
    }

    @Test
    fun phoneNumberDevice() {
        assertThat(PhoneNumberDevice).isEqualTo(ContentType.from(AUTOFILL_HINT_PHONE_NUMBER_DEVICE))
    }

    @Test
    fun phoneCountryCode() {
        assertThat(PhoneCountryCode).isEqualTo(ContentType.from(AUTOFILL_HINT_PHONE_COUNTRY_CODE))
    }

    @Test
    fun phoneNumberNational() {
        assertThat(PhoneNumberNational).isEqualTo(ContentType.from(AUTOFILL_HINT_PHONE_NATIONAL))
    }

    @Test
    fun gender() {
        assertThat(Gender).isEqualTo(ContentType.from(AUTOFILL_HINT_GENDER))
    }

    @Test
    fun birthDateFull() {
        assertThat(BirthDateFull).isEqualTo(ContentType.from(AUTOFILL_HINT_BIRTH_DATE_FULL))
    }

    @Test
    fun birthDateDay() {
        assertThat(BirthDateDay).isEqualTo(ContentType.from(AUTOFILL_HINT_BIRTH_DATE_DAY))
    }

    @Test
    fun birthDateMonth() {
        assertThat(BirthDateMonth).isEqualTo(ContentType.from(AUTOFILL_HINT_BIRTH_DATE_MONTH))
    }

    @Test
    fun birthDateYear() {
        assertThat(BirthDateYear).isEqualTo(ContentType.from(AUTOFILL_HINT_BIRTH_DATE_YEAR))
    }

    @Test
    fun smsOTPCode() {
        assertThat(SmsOtpCode).isEqualTo(ContentType.from(AUTOFILL_HINT_SMS_OTP))
    }
}
