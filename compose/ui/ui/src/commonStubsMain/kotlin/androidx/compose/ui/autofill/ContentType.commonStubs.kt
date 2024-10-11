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

import androidx.compose.ui.implementedInJetBrainsFork

actual class ContentType private actual constructor(contentHint: String) {
    actual companion object {
        actual val EmailAddress: ContentType = implementedInJetBrainsFork()
        actual val Username: ContentType = implementedInJetBrainsFork()
        actual val Password: ContentType = implementedInJetBrainsFork()
        actual val NewUsername: ContentType = implementedInJetBrainsFork()
        actual val NewPassword: ContentType = implementedInJetBrainsFork()
        actual val PostalAddress: ContentType = implementedInJetBrainsFork()
        actual val PostalCode: ContentType = implementedInJetBrainsFork()
        actual val CreditCardNumber: ContentType = implementedInJetBrainsFork()
        actual val CreditCardSecurityCode: ContentType = implementedInJetBrainsFork()
        actual val CreditCardExpirationDate: ContentType = implementedInJetBrainsFork()
        actual val CreditCardExpirationMonth: ContentType = implementedInJetBrainsFork()
        actual val CreditCardExpirationYear: ContentType = implementedInJetBrainsFork()
        actual val CreditCardExpirationDay: ContentType = implementedInJetBrainsFork()
        actual val AddressCountry: ContentType = implementedInJetBrainsFork()
        actual val AddressRegion: ContentType = implementedInJetBrainsFork()
        actual val AddressLocality: ContentType = implementedInJetBrainsFork()
        actual val AddressStreet: ContentType = implementedInJetBrainsFork()
        actual val AddressAuxiliaryDetails: ContentType = implementedInJetBrainsFork()
        actual val PostalCodeExtended: ContentType = implementedInJetBrainsFork()
        actual val PersonFullName: ContentType = implementedInJetBrainsFork()
        actual val PersonFirstName: ContentType = implementedInJetBrainsFork()
        actual val PersonLastName: ContentType = implementedInJetBrainsFork()
        actual val PersonMiddleName: ContentType = implementedInJetBrainsFork()
        actual val PersonMiddleInitial: ContentType = implementedInJetBrainsFork()
        actual val PersonNamePrefix: ContentType = implementedInJetBrainsFork()
        actual val PersonNameSuffix: ContentType = implementedInJetBrainsFork()
        actual val PhoneNumber: ContentType = implementedInJetBrainsFork()
        actual val PhoneNumberDevice: ContentType = implementedInJetBrainsFork()
        actual val PhoneCountryCode: ContentType = implementedInJetBrainsFork()
        actual val PhoneNumberNational: ContentType = implementedInJetBrainsFork()
        actual val Gender: ContentType = implementedInJetBrainsFork()
        actual val BirthDateFull: ContentType = implementedInJetBrainsFork()
        actual val BirthDateDay: ContentType = implementedInJetBrainsFork()
        actual val BirthDateMonth: ContentType = implementedInJetBrainsFork()
        actual val BirthDateYear: ContentType = implementedInJetBrainsFork()
        actual val SmsOtpCode: ContentType = implementedInJetBrainsFork()
    }
}
