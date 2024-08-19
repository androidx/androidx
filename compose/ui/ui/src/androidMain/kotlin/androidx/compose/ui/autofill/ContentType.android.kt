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

/**
 * Gets the Android specific [AutofillHint][android.view.ViewStructure.setAutofillHints]
 * corresponding to the [ContentType].
 */
actual class ContentType private constructor(internal val contentHints: Set<String>) {
    actual constructor(contentHint: String) : this(setOf(contentHint))

    actual companion object {
        // Define constants for predefined autofill hints
        actual val Username = ContentType(AUTOFILL_HINT_USERNAME)
        actual val Password = ContentType(AUTOFILL_HINT_PASSWORD)
        actual val EmailAddress = ContentType(AUTOFILL_HINT_EMAIL_ADDRESS)
        actual val NewUsername = ContentType(AUTOFILL_HINT_NEW_USERNAME)
        actual val NewPassword = ContentType(AUTOFILL_HINT_NEW_PASSWORD)
        actual val PostalAddress = ContentType(AUTOFILL_HINT_POSTAL_ADDRESS)
        actual val PostalCode = ContentType(AUTOFILL_HINT_POSTAL_CODE)
        actual val CreditCardNumber = ContentType(AUTOFILL_HINT_CREDIT_CARD_NUMBER)
        actual val CreditCardSecurityCode = ContentType(AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE)
        actual val CreditCardExpirationDate = ContentType(AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE)
        actual val CreditCardExpirationMonth =
            ContentType(AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH)
        actual val CreditCardExpirationYear = ContentType(AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR)
        actual val CreditCardExpirationDay = ContentType(AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY)
        actual val AddressCountry = ContentType(AUTOFILL_HINT_POSTAL_ADDRESS_COUNTRY)
        actual val AddressRegion = ContentType(AUTOFILL_HINT_POSTAL_ADDRESS_REGION)
        actual val AddressLocality = ContentType(AUTOFILL_HINT_POSTAL_ADDRESS_LOCALITY)
        actual val AddressStreet = ContentType(AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS)
        actual val AddressAuxiliaryDetails =
            ContentType(AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_ADDRESS)
        actual val PostalCodeExtended =
            ContentType(AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_POSTAL_CODE)
        actual val PersonFullName = ContentType(AUTOFILL_HINT_PERSON_NAME)
        actual val PersonFirstName = ContentType(AUTOFILL_HINT_PERSON_NAME_GIVEN)
        actual val PersonLastName = ContentType(AUTOFILL_HINT_PERSON_NAME_FAMILY)
        actual val PersonMiddleName = ContentType(AUTOFILL_HINT_PERSON_NAME_MIDDLE)
        actual val PersonMiddleInitial = ContentType(AUTOFILL_HINT_PERSON_NAME_MIDDLE_INITIAL)
        actual val PersonNamePrefix = ContentType(AUTOFILL_HINT_PERSON_NAME_PREFIX)
        actual val PersonNameSuffix = ContentType(AUTOFILL_HINT_PERSON_NAME_SUFFIX)
        actual val PhoneNumber = ContentType(AUTOFILL_HINT_PHONE_NUMBER)
        actual val PhoneNumberDevice = ContentType(AUTOFILL_HINT_PHONE_NUMBER_DEVICE)
        actual val PhoneCountryCode = ContentType(AUTOFILL_HINT_PHONE_COUNTRY_CODE)
        actual val PhoneNumberNational = ContentType(AUTOFILL_HINT_PHONE_NATIONAL)
        actual val Gender = ContentType(AUTOFILL_HINT_GENDER)
        actual val BirthDateFull = ContentType(AUTOFILL_HINT_BIRTH_DATE_FULL)
        actual val BirthDateDay = ContentType(AUTOFILL_HINT_BIRTH_DATE_DAY)
        actual val BirthDateMonth = ContentType(AUTOFILL_HINT_BIRTH_DATE_MONTH)
        actual val BirthDateYear = ContentType(AUTOFILL_HINT_BIRTH_DATE_YEAR)
        actual val SmsOtpCode = ContentType(AUTOFILL_HINT_SMS_OTP)
    }

    operator fun plus(other: ContentType): ContentType {
        val combinedValues = contentHints + other.contentHints
        return ContentType(combinedValues)
    }
}
