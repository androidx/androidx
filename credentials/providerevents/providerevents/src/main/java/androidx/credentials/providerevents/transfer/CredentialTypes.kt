/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.credentials.providerevents.transfer

/**
 * The credential types that can be transferred as part of the FIDO Credential Exchange Format (CXF)
 * protocol. More about these types can found
 * [here](https://fidoalliance.org/specs/cx/cxf-v1.0-rd-20250313.html#credentialtype). The constants
 * defined in this file list the frequently used credential types in CXF, and are not intended to be
 * the exhaustive list.
 */
public object CredentialTypes {
    /** A username/password login credential */
    public const val CREDENTIAL_TYPE_BASIC_AUTH: String = "basic-auth"
    /** A public key credential */
    public const val CREDENTIAL_TYPE_PUBLIC_KEY: String = "passkey"
    /** An address credential provides information for autofilling address forms. */
    public const val CREDENTIAL_TYPE_ADDRESS: String = "address"
    /**
     * A APIKey credential contains information to interact with an Application’s Programming
     * Interface (API).
     */
    public const val CREDENTIAL_TYPE_API_KEY: String = "api-key"
    /** A CreditCard credential contains information about a credit or debit card. */
    public const val CREDENTIAL_TYPE_CREDIT_CARD: String = "credit-card"
    /**
     * This credential allows the user to add additional information to the item organized as a
     * grouping that MAY have a label. If the exporting provider allows custom fields to be added to
     * items but does not have a grouping concept, it SHOULD use this object without setting the
     * label or id fields.
     */
    public const val CREDENTIAL_TYPE_CUSTOM_FIELDS: String = "custom-fields"
    /** A DriversLicense credential contains information about a person’s driver’s license. */
    public const val CREDENTIAL_TYPE_DRIVERS_LICENSE: String = "drivers-license"
    /**
     * A file credential acts as a placeholder to an arbitrary binary file holding its associated
     * metadata.
     */
    public const val CREDENTIAL_TYPE_FILE: String = "file"
    /**
     * A GeneratedPassword credential type represents a credential consisting of a machine-generated
     * password.
     */
    public const val CREDENTIAL_TYPE_GENERATED_PASSWORD: String = "generated-password"
    /**
     * An IdentityDocument credential is for any document, card, or number identifying a person or
     * entity. Examples include national ID cards, Social Security Numbers (SSN), Tax Identification
     * Numbers (TIN), health insurance cards, or Value-Added Tax (VAT) numbers.
     */
    public const val CREDENTIAL_TYPE_IDENTITY_DOCUMENT: String = "identity-document"
    /**
     * An ItemReference credential is a pointer to another Item, denoting that the two items MAY be
     * logically linked together.
     */
    public const val CREDENTIAL_TYPE_ITEM_REFERENCE: String = "item-reference"
    /** A note credential is a user-defined value encoded as a UTF-8 string */
    public const val CREDENTIAL_TYPE_NOTE: String = "note"
    /** A Passport credential contains the details of a person’s passport. */
    public const val CREDENTIAL_TYPE_PASSPORT: String = "passport"
    /** A PersonName credential represents a person’s name. */
    public const val CREDENTIAL_TYPE_PERSON_NAME: String = "person-name"
    /** An SSHKey credential represents an SSH (Secure Shell) key pair. */
    public const val CREDENTIAL_TYPE_SSH_KEY: String = "ssh-key"
    /** A TOTP is a time-based one-time password */
    public const val CREDENTIAL_TYPE_TOTP: String = "totp"
    /** A wifi credential provides the necessary information to connect to a Wi-Fi network. */
    public const val CREDENTIAL_TYPE_WIFI: String = "wifi"
}
