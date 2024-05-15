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

/**
 * Content type information.
 *
 * Autofill services use the [ContentType] to determine what value to use to autofill fields
 * associated with this type. If the [ContentType] is not specified, the autofill services have
 * to use heuristics to determine the right value to use while autofilling the corresponding field.
 */
internal actual class ContentType private actual constructor(contentHint: String) {
    internal actual companion object {
        private val UnsupportedContentType = ContentType("")

        internal actual fun from(value: String) = UnsupportedContentType

        actual val EmailAddress = UnsupportedContentType
        actual val Username = UnsupportedContentType
        actual val Password = UnsupportedContentType
        actual val NewUsername = UnsupportedContentType
        actual val NewPassword = UnsupportedContentType
        actual val PostalAddress = UnsupportedContentType
        actual val PostalCode = UnsupportedContentType
        actual val CreditCardNumber = UnsupportedContentType
        actual val CreditCardSecurityCode = UnsupportedContentType
        actual val CreditCardExpirationDate = UnsupportedContentType
        actual val CreditCardExpirationMonth = UnsupportedContentType
        actual val CreditCardExpirationYear = UnsupportedContentType
        actual val CreditCardExpirationDay = UnsupportedContentType
        actual val AddressCountry = UnsupportedContentType
        actual val AddressRegion = UnsupportedContentType
        actual val AddressLocality = UnsupportedContentType
        actual val AddressStreet = UnsupportedContentType
        actual val AddressAuxiliaryDetails = UnsupportedContentType
        actual val PostalCodeExtended = UnsupportedContentType
        actual val PersonFullName = UnsupportedContentType
        actual val PersonFirstName = UnsupportedContentType
        actual val PersonLastName = UnsupportedContentType
        actual val PersonMiddleName = UnsupportedContentType
        actual val PersonMiddleInitial = UnsupportedContentType
        actual val PersonNamePrefix = UnsupportedContentType
        actual val PersonNameSuffix = UnsupportedContentType
        actual val PhoneNumber = UnsupportedContentType
        actual val PhoneNumberDevice = UnsupportedContentType
        actual val PhoneCountryCode = UnsupportedContentType
        actual val PhoneNumberNational = UnsupportedContentType
        actual val Gender = UnsupportedContentType
        actual val BirthDateFull = UnsupportedContentType
        actual val BirthDateDay = UnsupportedContentType
        actual val BirthDateMonth = UnsupportedContentType
        actual val BirthDateYear = UnsupportedContentType
        actual val SmsOtpCode = UnsupportedContentType
    }
}
