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
// TODO(b/333102566): When Autofill goes live for Compose,
//  these classes will need to be made public.
internal actual class ContentType private actual constructor(contentHint: String) {
    internal actual companion object {
        /**
         * Indicates that the associated component can be autofilled with an email address.
         */
        actual val EmailAddress: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a username.
         */
        actual val Username: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a password.
         */
        actual val Password: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be interpreted as a newly created username for
         * save/update.
         */
        actual val NewUsername: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be interpreted as a newly created password for
         * save/update.
         */
        actual val NewPassword: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a postal address.
         */
        actual val PostalAddress: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a postal code.
         */
        actual val PostalCode: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a credit card number.
         */
        actual val CreditCardNumber: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a credit card security code.
         */
        actual val CreditCardSecurityCode: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a credit card expiration date.
         */
        actual val CreditCardExpirationDate: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a credit card expiration
         * month.
         */
        actual val CreditCardExpirationMonth: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a credit card expiration
         * year.
         */
        actual val CreditCardExpirationYear: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a credit card expiration day.
         */
        actual val CreditCardExpirationDay: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a country name/code.
         */
        actual val AddressCountry: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a region/state.
         */
        actual val AddressRegion: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with an address locality
         * (city/town).
         */
        actual val AddressLocality: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a street address.
         */
        actual val AddressStreet: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with auxiliary address details.
         */
        actual val AddressAuxiliaryDetails: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with an extended ZIP/POSTAL code.
         *
         * Example: In forms that split the U.S. ZIP+4 Code with nine digits 99999-9999 into two
         * fields annotate the delivery route code with this hint.
         */
        actual val PostalCodeExtended: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a person's full name.
         *
         */
        actual val PersonFullName: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a person's first/given name.
         */
        actual val PersonFirstName: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a person's last/family name.
         */
        actual val PersonLastName: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a person's middle name.
         */
        actual val PersonMiddleName: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a person's middle initial.
         */
        actual val PersonMiddleInitial: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a person's name prefix.
         */
        actual val PersonNamePrefix: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a person's name suffix.
         */
        actual val PersonNameSuffix: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a phone number with
         * country code.
         *
         * Example: +1 123-456-7890
         */
        actual val PhoneNumber: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with the current device's phone number
         * usually for Sign Up / OTP flows.
         */
        actual val PhoneNumberDevice: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a phone number's country code.
         */
        actual val PhoneCountryCode: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a phone number without
         * country code.
         */
        actual val PhoneNumberNational: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a gender.
         */
        actual val Gender: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a full birth date.
         */
        actual val BirthDateFull: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a birth day(of the month).
         */
        actual val BirthDateDay: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a birth day(of the month).
         */
        actual val BirthDateMonth: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a birth year.
         */
        actual val BirthDateYear: ContentType
            get() = TODO("Not yet implemented")

        /**
         * Indicates that the associated component can be autofilled with a SMS One Time Password (OTP).
         */
        actual val SmsOtpCode: ContentType
            get() = TODO("Not yet implemented")

        internal actual fun from(value: String): ContentType {
            TODO("Not yet implemented")
        }

    }

}