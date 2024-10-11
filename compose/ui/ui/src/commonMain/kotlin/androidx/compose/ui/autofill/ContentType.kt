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
 * associated with this type. If the [ContentType] is not specified, the autofill services have to
 * use heuristics to determine the right value to use while autofilling the corresponding field.
 */
expect class ContentType private constructor(contentHint: String) {
    companion object {
        /** Indicates that the associated component can be autofilled with an email address. */
        val EmailAddress: ContentType

        /** Indicates that the associated component can be autofilled with a username. */
        val Username: ContentType

        /** Indicates that the associated component can be autofilled with a password. */
        val Password: ContentType

        /**
         * Indicates that the associated component can be interpreted as a newly created username
         * for save/update.
         */
        val NewUsername: ContentType

        /**
         * Indicates that the associated component can be interpreted as a newly created password
         * for save/update.
         */
        val NewPassword: ContentType

        /** Indicates that the associated component can be autofilled with a postal address. */
        val PostalAddress: ContentType

        /** Indicates that the associated component can be autofilled with a postal code. */
        val PostalCode: ContentType

        /** Indicates that the associated component can be autofilled with a credit card number. */
        val CreditCardNumber: ContentType

        /**
         * Indicates that the associated component can be autofilled with a credit card security
         * code.
         */
        val CreditCardSecurityCode: ContentType

        /**
         * Indicates that the associated component can be autofilled with a credit card expiration
         * date.
         */
        val CreditCardExpirationDate: ContentType

        /**
         * Indicates that the associated component can be autofilled with a credit card expiration
         * month.
         */
        val CreditCardExpirationMonth: ContentType

        /**
         * Indicates that the associated component can be autofilled with a credit card expiration
         * year.
         */
        val CreditCardExpirationYear: ContentType

        /**
         * Indicates that the associated component can be autofilled with a credit card expiration
         * day.
         */
        val CreditCardExpirationDay: ContentType

        /** Indicates that the associated component can be autofilled with a country name/code. */
        val AddressCountry: ContentType

        /** Indicates that the associated component can be autofilled with a region/state. */
        val AddressRegion: ContentType

        /**
         * Indicates that the associated component can be autofilled with an address locality
         * (city/town).
         */
        val AddressLocality: ContentType

        /** Indicates that the associated component can be autofilled with a street address. */
        val AddressStreet: ContentType

        /**
         * Indicates that the associated component can be autofilled with auxiliary address details.
         */
        val AddressAuxiliaryDetails: ContentType

        /**
         * Indicates that the associated component can be autofilled with an extended ZIP/POSTAL
         * code.
         *
         * Example: In forms that split the U.S. ZIP+4 Code with nine digits 99999-9999 into two
         * fields annotate the delivery route code with this hint.
         */
        val PostalCodeExtended: ContentType

        /** Indicates that the associated component can be autofilled with a person's full name. */
        val PersonFullName: ContentType

        /**
         * Indicates that the associated component can be autofilled with a person's first/given
         * name.
         */
        val PersonFirstName: ContentType

        /**
         * Indicates that the associated component can be autofilled with a person's last/family
         * name.
         */
        val PersonLastName: ContentType

        /**
         * Indicates that the associated component can be autofilled with a person's middle name.
         */
        val PersonMiddleName: ContentType

        /**
         * Indicates that the associated component can be autofilled with a person's middle initial.
         */
        val PersonMiddleInitial: ContentType

        /**
         * Indicates that the associated component can be autofilled with a person's name prefix.
         */
        val PersonNamePrefix: ContentType

        /**
         * Indicates that the associated component can be autofilled with a person's name suffix.
         */
        val PersonNameSuffix: ContentType

        /**
         * Indicates that the associated component can be autofilled with a phone number with
         * country code.
         *
         * Example: +1 123-456-7890
         */
        val PhoneNumber: ContentType

        /**
         * Indicates that the associated component can be autofilled with the current device's phone
         * number usually for Sign Up / OTP flows.
         */
        val PhoneNumberDevice: ContentType

        /**
         * Indicates that the associated component can be autofilled with a phone number's country
         * code.
         */
        val PhoneCountryCode: ContentType

        /**
         * Indicates that the associated component can be autofilled with a phone number without
         * country code.
         */
        val PhoneNumberNational: ContentType

        /** Indicates that the associated component can be autofilled with a gender. */
        val Gender: ContentType

        /** Indicates that the associated component can be autofilled with a full birth date. */
        val BirthDateFull: ContentType

        /**
         * Indicates that the associated component can be autofilled with a birth day(of the month).
         */
        val BirthDateDay: ContentType

        /** Indicates that the associated component can be autofilled with a birth month. */
        val BirthDateMonth: ContentType

        /** Indicates that the associated component can be autofilled with a birth year. */
        val BirthDateYear: ContentType

        /**
         * Indicates that the associated component can be autofilled with a SMS One Time Password
         * (OTP).
         */
        val SmsOtpCode: ContentType
    }
}
