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

public actual sealed interface ContentType {
    public actual companion object {
        public actual val EmailAddress: ContentType = implementedInJetBrainsFork()
        public actual val Username: ContentType = implementedInJetBrainsFork()
        public actual val Password: ContentType = implementedInJetBrainsFork()
        public actual val NewUsername: ContentType = implementedInJetBrainsFork()
        public actual val NewPassword: ContentType = implementedInJetBrainsFork()
        public actual val PostalAddress: ContentType = implementedInJetBrainsFork()
        public actual val PostalCode: ContentType = implementedInJetBrainsFork()
        public actual val CreditCardNumber: ContentType = implementedInJetBrainsFork()
        public actual val CreditCardSecurityCode: ContentType = implementedInJetBrainsFork()
        public actual val CreditCardExpirationDate: ContentType = implementedInJetBrainsFork()
        public actual val CreditCardExpirationMonth: ContentType = implementedInJetBrainsFork()
        public actual val CreditCardExpirationYear: ContentType = implementedInJetBrainsFork()
        public actual val CreditCardExpirationDay: ContentType = implementedInJetBrainsFork()
        public actual val AddressCountry: ContentType = implementedInJetBrainsFork()
        public actual val AddressRegion: ContentType = implementedInJetBrainsFork()
        public actual val AddressLocality: ContentType = implementedInJetBrainsFork()
        public actual val AddressStreet: ContentType = implementedInJetBrainsFork()
        public actual val AddressAuxiliaryDetails: ContentType = implementedInJetBrainsFork()
        public actual val PostalCodeExtended: ContentType = implementedInJetBrainsFork()
        public actual val PersonFullName: ContentType = implementedInJetBrainsFork()
        public actual val PersonFirstName: ContentType = implementedInJetBrainsFork()
        public actual val PersonLastName: ContentType = implementedInJetBrainsFork()
        public actual val PersonMiddleName: ContentType = implementedInJetBrainsFork()
        public actual val PersonMiddleInitial: ContentType = implementedInJetBrainsFork()
        public actual val PersonNamePrefix: ContentType = implementedInJetBrainsFork()
        public actual val PersonNameSuffix: ContentType = implementedInJetBrainsFork()
        public actual val PhoneNumber: ContentType = implementedInJetBrainsFork()
        public actual val PhoneNumberDevice: ContentType = implementedInJetBrainsFork()
        public actual val PhoneCountryCode: ContentType = implementedInJetBrainsFork()
        public actual val PhoneNumberNational: ContentType = implementedInJetBrainsFork()
        public actual val Gender: ContentType = implementedInJetBrainsFork()
        public actual val BirthDateFull: ContentType = implementedInJetBrainsFork()
        public actual val BirthDateDay: ContentType = implementedInJetBrainsFork()
        public actual val BirthDateMonth: ContentType = implementedInJetBrainsFork()
        public actual val BirthDateYear: ContentType = implementedInJetBrainsFork()
        public actual val SmsOtpCode: ContentType = implementedInJetBrainsFork()
    }

    public actual operator fun plus(other: ContentType): ContentType
}
