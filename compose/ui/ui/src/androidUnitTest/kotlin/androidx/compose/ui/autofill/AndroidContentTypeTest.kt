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
        assertThat(EmailAddress.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_EMAIL_ADDRESS).contentHints)
    }

    @Test
    fun username() {
        assertThat(Username.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_USERNAME).contentHints)
    }

    @Test
    fun password() {
        assertThat(Password.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_PASSWORD).contentHints)
    }

    @Test
    fun newUsername() {
        assertThat(NewUsername.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_NEW_USERNAME).contentHints)
    }

    @Test
    fun newPassword() {
        assertThat(NewPassword.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_NEW_PASSWORD).contentHints)
    }

    @Test
    fun postalAddress() {
        assertThat(PostalAddress.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_POSTAL_ADDRESS).contentHints)
    }

    @Test
    fun postalCode() {
        assertThat(PostalCode.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_POSTAL_CODE).contentHints)
    }

    @Test
    fun creditCardNumber() {
        assertThat(CreditCardNumber.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_CREDIT_CARD_NUMBER).contentHints)
    }

    @Test
    fun creditCardSecurityCode() {
        assertThat(CreditCardSecurityCode.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE).contentHints)
    }

    @Test
    fun creditCardExpirationDate() {
        assertThat(CreditCardExpirationDate.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE).contentHints)
    }

    @Test
    fun creditCardExpirationMonth() {
        assertThat(CreditCardExpirationMonth.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH).contentHints)
    }

    @Test
    fun creditCardExpirationYear() {
        assertThat(CreditCardExpirationYear.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR).contentHints)
    }

    @Test
    fun creditCardExpirationDay() {
        assertThat(CreditCardExpirationDay.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY).contentHints)
    }

    @Test
    fun addressCountry() {
        assertThat(AddressCountry.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_POSTAL_ADDRESS_COUNTRY).contentHints)
    }

    @Test
    fun addressRegion() {
        assertThat(AddressRegion.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_POSTAL_ADDRESS_REGION).contentHints)
    }

    @Test
    fun addressLocality() {
        assertThat(AddressLocality.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_POSTAL_ADDRESS_LOCALITY).contentHints)
    }

    @Test
    fun addressStreet() {
        assertThat(AddressStreet.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS).contentHints)
    }

    @Test
    fun addressAuxiliaryDetails() {
        assertThat(AddressAuxiliaryDetails.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_ADDRESS).contentHints)
    }

    @Test
    fun postalCodeExtended() {
        assertThat(PostalCodeExtended.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_POSTAL_CODE).contentHints)
    }

    @Test
    fun personFullName() {
        assertThat(PersonFullName.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_PERSON_NAME).contentHints)
    }

    @Test
    fun personFirstName() {
        assertThat(PersonFirstName.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_PERSON_NAME_GIVEN).contentHints)
    }

    @Test
    fun personLastName() {
        assertThat(PersonLastName.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_PERSON_NAME_FAMILY).contentHints)
    }

    @Test
    fun personMiddleName() {
        assertThat(PersonMiddleName.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_PERSON_NAME_MIDDLE).contentHints)
    }

    @Test
    fun personMiddleInitial() {
        assertThat(PersonMiddleInitial.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_PERSON_NAME_MIDDLE_INITIAL).contentHints)
    }

    @Test
    fun personNamePrefix() {
        assertThat(PersonNamePrefix.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_PERSON_NAME_PREFIX).contentHints)
    }

    @Test
    fun personNameSuffix() {
        assertThat(PersonNameSuffix.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_PERSON_NAME_SUFFIX).contentHints)
    }

    @Test
    fun phoneNumber() {
        assertThat(PhoneNumber.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_PHONE_NUMBER).contentHints)
    }

    @Test
    fun phoneNumberDevice() {
        assertThat(PhoneNumberDevice.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_PHONE_NUMBER_DEVICE).contentHints)
    }

    @Test
    fun phoneCountryCode() {
        assertThat(PhoneCountryCode.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_PHONE_COUNTRY_CODE).contentHints)
    }

    @Test
    fun phoneNumberNational() {
        assertThat(PhoneNumberNational.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_PHONE_NATIONAL).contentHints)
    }

    @Test
    fun gender() {
        assertThat(Gender.contentHints).isEqualTo(ContentType(AUTOFILL_HINT_GENDER).contentHints)
    }

    @Test
    fun birthDateFull() {
        assertThat(BirthDateFull.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_BIRTH_DATE_FULL).contentHints)
    }

    @Test
    fun birthDateDay() {
        assertThat(BirthDateDay.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_BIRTH_DATE_DAY).contentHints)
    }

    @Test
    fun birthDateMonth() {
        assertThat(BirthDateMonth.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_BIRTH_DATE_MONTH).contentHints)
    }

    @Test
    fun birthDateYear() {
        assertThat(BirthDateYear.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_BIRTH_DATE_YEAR).contentHints)
    }

    @Test
    fun smsOTPCode() {
        assertThat(SmsOtpCode.contentHints)
            .isEqualTo(ContentType(AUTOFILL_HINT_SMS_OTP).contentHints)
    }
}
