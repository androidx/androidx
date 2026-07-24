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

public actual sealed interface ContentType {
    public actual companion object {
        // Define constants for predefined autofill hints
        public actual val Username: ContentType = ContentType(AUTOFILL_HINT_USERNAME)
        public actual val Password: ContentType = ContentType(AUTOFILL_HINT_PASSWORD)
        public actual val EmailAddress: ContentType = ContentType(AUTOFILL_HINT_EMAIL_ADDRESS)
        public actual val NewUsername: ContentType = ContentType(AUTOFILL_HINT_NEW_USERNAME)
        public actual val NewPassword: ContentType = ContentType(AUTOFILL_HINT_NEW_PASSWORD)
        public actual val PostalAddress: ContentType = ContentType(AUTOFILL_HINT_POSTAL_ADDRESS)
        public actual val PostalCode: ContentType = ContentType(AUTOFILL_HINT_POSTAL_CODE)
        public actual val CreditCardNumber: ContentType =
            ContentType(AUTOFILL_HINT_CREDIT_CARD_NUMBER)
        public actual val CreditCardSecurityCode: ContentType =
            ContentType(AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE)
        public actual val CreditCardExpirationDate: ContentType =
            ContentType(AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE)
        public actual val CreditCardExpirationMonth: ContentType =
            ContentType(AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH)
        public actual val CreditCardExpirationYear: ContentType =
            ContentType(AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR)
        public actual val CreditCardExpirationDay: ContentType =
            ContentType(AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY)
        public actual val AddressCountry: ContentType =
            ContentType(AUTOFILL_HINT_POSTAL_ADDRESS_COUNTRY)
        public actual val AddressRegion: ContentType =
            ContentType(AUTOFILL_HINT_POSTAL_ADDRESS_REGION)
        public actual val AddressLocality: ContentType =
            ContentType(AUTOFILL_HINT_POSTAL_ADDRESS_LOCALITY)
        public actual val AddressStreet: ContentType =
            ContentType(AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS)
        public actual val AddressAuxiliaryDetails: ContentType =
            ContentType(AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_ADDRESS)
        public actual val PostalCodeExtended: ContentType =
            ContentType(AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_POSTAL_CODE)
        public actual val PersonFullName: ContentType = ContentType(AUTOFILL_HINT_PERSON_NAME)
        public actual val PersonFirstName: ContentType =
            ContentType(AUTOFILL_HINT_PERSON_NAME_GIVEN)
        public actual val PersonLastName: ContentType =
            ContentType(AUTOFILL_HINT_PERSON_NAME_FAMILY)
        public actual val PersonMiddleName: ContentType =
            ContentType(AUTOFILL_HINT_PERSON_NAME_MIDDLE)
        public actual val PersonMiddleInitial: ContentType =
            ContentType(AUTOFILL_HINT_PERSON_NAME_MIDDLE_INITIAL)
        public actual val PersonNamePrefix: ContentType =
            ContentType(AUTOFILL_HINT_PERSON_NAME_PREFIX)
        public actual val PersonNameSuffix: ContentType =
            ContentType(AUTOFILL_HINT_PERSON_NAME_SUFFIX)
        public actual val PhoneNumber: ContentType = ContentType(AUTOFILL_HINT_PHONE_NUMBER)
        public actual val PhoneNumberDevice: ContentType =
            ContentType(AUTOFILL_HINT_PHONE_NUMBER_DEVICE)
        public actual val PhoneCountryCode: ContentType =
            ContentType(AUTOFILL_HINT_PHONE_COUNTRY_CODE)
        public actual val PhoneNumberNational: ContentType =
            ContentType(AUTOFILL_HINT_PHONE_NATIONAL)
        public actual val Gender: ContentType = ContentType(AUTOFILL_HINT_GENDER)
        public actual val BirthDateFull: ContentType = ContentType(AUTOFILL_HINT_BIRTH_DATE_FULL)
        public actual val BirthDateDay: ContentType = ContentType(AUTOFILL_HINT_BIRTH_DATE_DAY)
        public actual val BirthDateMonth: ContentType = ContentType(AUTOFILL_HINT_BIRTH_DATE_MONTH)
        public actual val BirthDateYear: ContentType = ContentType(AUTOFILL_HINT_BIRTH_DATE_YEAR)
        public actual val SmsOtpCode: ContentType = ContentType(AUTOFILL_HINT_SMS_OTP)
    }

    public actual operator fun plus(other: ContentType): ContentType
}

private class AndroidContentType(val androidAutofillHints: Set<String>) : ContentType {
    override operator fun plus(other: ContentType): ContentType {
        other as AndroidContentType
        val combinedValues = androidAutofillHints + other.androidAutofillHints
        return AndroidContentType(combinedValues)
    }
}

/**
 * Create a custom `ContentType` with [contentHint].
 *
 * This creates a `ContentType` with the parameter [contentHint] passed in. This API can be used if
 * the Autofill hint is not present in the list of `contentType`s provided by Compose.
 *
 * For example,
 * `ContentType(androidx.autofill.HintConstants.AUTOFILL_HINT_FLIGHT_CONFIRMATION_CODE)` can be used
 * to create a new flight confirmation code hint.
 */
public fun ContentType(contentHint: String): ContentType = AndroidContentType(setOf(contentHint))

internal val ContentType.contentHints: Array<String>
    get() = (this as AndroidContentType).androidAutofillHints.toTypedArray()
