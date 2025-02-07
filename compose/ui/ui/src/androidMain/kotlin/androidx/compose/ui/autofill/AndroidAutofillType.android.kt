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
 * corresponding to the current [AutofillType].
 */
@Suppress("Deprecation")
internal val AutofillType.androidType: String
    get() {
        val androidAutofillType = androidAutofillTypes[this]
        requireNotNull(androidAutofillType) { "Unsupported autofill type" }
        return androidAutofillType
    }

/** Maps each [AutofillType] to one of the autofill hints in [androidx.autofill.HintConstants] */
@Suppress("Deprecation")
private val androidAutofillTypes: HashMap<AutofillType, String> =
    hashMapOf(
        AutofillType.EmailAddress to AUTOFILL_HINT_EMAIL_ADDRESS,
        AutofillType.Username to AUTOFILL_HINT_USERNAME,
        AutofillType.Password to AUTOFILL_HINT_PASSWORD,
        AutofillType.NewUsername to AUTOFILL_HINT_NEW_USERNAME,
        AutofillType.NewPassword to AUTOFILL_HINT_NEW_PASSWORD,
        AutofillType.PostalAddress to AUTOFILL_HINT_POSTAL_ADDRESS,
        AutofillType.PostalCode to AUTOFILL_HINT_POSTAL_CODE,
        AutofillType.CreditCardNumber to AUTOFILL_HINT_CREDIT_CARD_NUMBER,
        AutofillType.CreditCardSecurityCode to AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE,
        AutofillType.CreditCardExpirationDate to AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE,
        AutofillType.CreditCardExpirationMonth to AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH,
        AutofillType.CreditCardExpirationYear to AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR,
        AutofillType.CreditCardExpirationDay to AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY,
        AutofillType.AddressCountry to AUTOFILL_HINT_POSTAL_ADDRESS_COUNTRY,
        AutofillType.AddressRegion to AUTOFILL_HINT_POSTAL_ADDRESS_REGION,
        AutofillType.AddressLocality to AUTOFILL_HINT_POSTAL_ADDRESS_LOCALITY,
        AutofillType.AddressStreet to AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS,
        AutofillType.AddressAuxiliaryDetails to AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_ADDRESS,
        AutofillType.PostalCodeExtended to AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_POSTAL_CODE,
        AutofillType.PersonFullName to AUTOFILL_HINT_PERSON_NAME,
        AutofillType.PersonFirstName to AUTOFILL_HINT_PERSON_NAME_GIVEN,
        AutofillType.PersonLastName to AUTOFILL_HINT_PERSON_NAME_FAMILY,
        AutofillType.PersonMiddleName to AUTOFILL_HINT_PERSON_NAME_MIDDLE,
        AutofillType.PersonMiddleInitial to AUTOFILL_HINT_PERSON_NAME_MIDDLE_INITIAL,
        AutofillType.PersonNamePrefix to AUTOFILL_HINT_PERSON_NAME_PREFIX,
        AutofillType.PersonNameSuffix to AUTOFILL_HINT_PERSON_NAME_SUFFIX,
        AutofillType.PhoneNumber to AUTOFILL_HINT_PHONE_NUMBER,
        AutofillType.PhoneNumberDevice to AUTOFILL_HINT_PHONE_NUMBER_DEVICE,
        AutofillType.PhoneCountryCode to AUTOFILL_HINT_PHONE_COUNTRY_CODE,
        AutofillType.PhoneNumberNational to AUTOFILL_HINT_PHONE_NATIONAL,
        AutofillType.Gender to AUTOFILL_HINT_GENDER,
        AutofillType.BirthDateFull to AUTOFILL_HINT_BIRTH_DATE_FULL,
        AutofillType.BirthDateDay to AUTOFILL_HINT_BIRTH_DATE_DAY,
        AutofillType.BirthDateMonth to AUTOFILL_HINT_BIRTH_DATE_MONTH,
        AutofillType.BirthDateYear to AUTOFILL_HINT_BIRTH_DATE_YEAR,
        AutofillType.SmsOtpCode to AUTOFILL_HINT_SMS_OTP
    )
