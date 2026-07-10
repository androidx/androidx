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

package androidx.compose.ui.autofill

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("Deprecation")
@RunWith(JUnit4::class)
class AndroidAutofillTypeTest {

    @Test
    fun emailAddress() {
        assertThat(AutofillType.EmailAddress.androidType).isEqualTo("emailAddress")
    }

    @Test
    fun username() {
        assertThat(AutofillType.Username.androidType).isEqualTo("username")
    }

    @Test
    fun password() {
        assertThat(AutofillType.Password.androidType).isEqualTo("password")
    }

    @Test
    fun newUsername() {
        assertThat(AutofillType.NewUsername.androidType).isEqualTo("newUsername")
    }

    @Test
    fun newPassword() {
        assertThat(AutofillType.NewPassword.androidType).isEqualTo("newPassword")
    }

    @Test
    fun postalAddress() {
        assertThat(AutofillType.PostalAddress.androidType).isEqualTo("postalAddress")
    }

    @Test
    fun postalCode() {
        assertThat(AutofillType.PostalCode.androidType).isEqualTo("postalCode")
    }

    @Test
    fun creditCardNumber() {
        assertThat(AutofillType.CreditCardNumber.androidType).isEqualTo("creditCardNumber")
    }

    @Test
    fun creditCardSecurityCode() {
        assertThat(AutofillType.CreditCardSecurityCode.androidType)
            .isEqualTo("creditCardSecurityCode")
    }

    @Test
    fun creditCardExpirationDate() {
        assertThat(AutofillType.CreditCardExpirationDate.androidType)
            .isEqualTo("creditCardExpirationDate")
    }

    @Test
    fun creditCardExpirationMonth() {
        assertThat(AutofillType.CreditCardExpirationMonth.androidType)
            .isEqualTo("creditCardExpirationMonth")
    }

    @Test
    fun creditCardExpirationYear() {
        assertThat(AutofillType.CreditCardExpirationYear.androidType)
            .isEqualTo("creditCardExpirationYear")
    }

    @Test
    fun creditCardExpirationDay() {
        assertThat(AutofillType.CreditCardExpirationDay.androidType)
            .isEqualTo("creditCardExpirationDay")
    }

    @Test
    fun addressCountry() {
        assertThat(AutofillType.AddressCountry.androidType).isEqualTo("addressCountry")
    }

    @Test
    fun addressRegion() {
        assertThat(AutofillType.AddressRegion.androidType).isEqualTo("addressRegion")
    }

    @Test
    fun addressLocality() {
        assertThat(AutofillType.AddressLocality.androidType).isEqualTo("addressLocality")
    }

    @Test
    fun addressStreet() {
        assertThat(AutofillType.AddressStreet.androidType).isEqualTo("streetAddress")
    }

    @Test
    fun addressAuxiliaryDetails() {
        assertThat(AutofillType.AddressAuxiliaryDetails.androidType).isEqualTo("extendedAddress")
    }

    @Test
    fun postalCodeExtended() {
        assertThat(AutofillType.PostalCodeExtended.androidType).isEqualTo("extendedPostalCode")
    }

    @Test
    fun personFullName() {
        assertThat(AutofillType.PersonFullName.androidType).isEqualTo("personName")
    }

    @Test
    fun personFirstName() {
        assertThat(AutofillType.PersonFirstName.androidType).isEqualTo("personGivenName")
    }

    @Test
    fun personLastName() {
        assertThat(AutofillType.PersonLastName.androidType).isEqualTo("personFamilyName")
    }

    @Test
    fun personMiddleName() {
        assertThat(AutofillType.PersonMiddleName.androidType).isEqualTo("personMiddleName")
    }

    @Test
    fun personMiddleInitial() {
        assertThat(AutofillType.PersonMiddleInitial.androidType).isEqualTo("personMiddleInitial")
    }

    @Test
    fun personNamePrefix() {
        assertThat(AutofillType.PersonNamePrefix.androidType).isEqualTo("personNamePrefix")
    }

    @Test
    fun personNameSuffix() {
        assertThat(AutofillType.PersonNameSuffix.androidType).isEqualTo("personNameSuffix")
    }

    @Test
    fun phoneNumber() {
        assertThat(AutofillType.PhoneNumber.androidType).isEqualTo("phoneNumber")
    }

    @Test
    fun phoneNumberDevice() {
        assertThat(AutofillType.PhoneNumberDevice.androidType).isEqualTo("phoneNumberDevice")
    }

    @Test
    fun phoneCountryCode() {
        assertThat(AutofillType.PhoneCountryCode.androidType).isEqualTo("phoneCountryCode")
    }

    @Test
    fun phoneNumberNational() {
        assertThat(AutofillType.PhoneNumberNational.androidType).isEqualTo("phoneNational")
    }

    @Test
    fun gender() {
        assertThat(AutofillType.Gender.androidType).isEqualTo("gender")
    }

    @Test
    fun birthDateFull() {
        assertThat(AutofillType.BirthDateFull.androidType).isEqualTo("birthDateFull")
    }

    @Test
    fun birthDateDay() {
        assertThat(AutofillType.BirthDateDay.androidType).isEqualTo("birthDateDay")
    }

    @Test
    fun birthDateMonth() {
        assertThat(AutofillType.BirthDateMonth.androidType).isEqualTo("birthDateMonth")
    }

    @Test
    fun birthDateYear() {
        assertThat(AutofillType.BirthDateYear.androidType).isEqualTo("birthDateYear")
    }

    @Test
    fun smsOTPCode() {
        assertThat(AutofillType.SmsOtpCode.androidType).isEqualTo("smsOTPCode")
    }
}
