/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.autofill;

/**
 * Contains all the officially supported autofill hint constants.
 *
 * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
 * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
 * should be <code>{@value #AUTOFILL_HINT_EMAIL_ADDRESS}</code>) or {@link
 * android.view.View#setAutofillHints(String[])}.
 */
public final class HintConstants {
    private HintConstants() {}

    // Hints copied from android.view.View for convenience

    /**
     * Hint indicating that this view can be autofilled with an email address.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_EMAIL_ADDRESS}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_EMAIL_ADDRESS = "emailAddress";

    /**
     * Hint indicating that this view can be autofilled with a user's real name.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_NAME}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     *
     * @deprecated replaced by <code>{@value #AUTOFILL_HINT_PERSON_NAME}</code> to be more specific
     */
    @Deprecated public static final String AUTOFILL_HINT_NAME = "name";

    /**
     * Hint indicating that this view can be autofilled with a username.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_USERNAME}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_USERNAME = "username";

    /**
     * Hint indicating that this view can be autofilled with a password.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_PASSWORD}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_PASSWORD = "password";

    /**
     * Hint indicating that this view can be autofilled with a phone number.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_PHONE}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     *
     * @deprecated replaced by <code>{@value #AUTOFILL_HINT_PHONE_NUMBER}</code> to be more specific
     */
    @Deprecated public static final String AUTOFILL_HINT_PHONE = "phone";

    /**
     * Hint indicating that this view can be autofilled with a postal address.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_POSTAL_ADDRESS}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_POSTAL_ADDRESS = "postalAddress";

    /**
     * Hint indicating that this view can be autofilled with a postal code.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_POSTAL_CODE}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_POSTAL_CODE = "postalCode";

    /**
     * Hint indicating that this view can be autofilled with a credit card number.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_CREDIT_CARD_NUMBER}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_CREDIT_CARD_NUMBER = "creditCardNumber";

    /**
     * Hint indicating that this view can be autofilled with a credit card security code.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE = "creditCardSecurityCode";

    /**
     * Hint indicating that this view can be autofilled with a credit card expiration date.
     *
     * <p>It should be used when the credit card expiration date is represented by just one view; if
     * it is represented by more than one (for example, one view for the month and another view for
     * the year), then each of these views should use the hint specific for the unit ({@link
     * #AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY}, {@link
     * #AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH}, or {@link
     * #AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR}).
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE}</code>).
     *
     * <p>When annotating a view with this hint, it's recommended to use a date autofill value to
     * avoid ambiguity when the autofill service provides a value for it. To understand why a value
     * can be ambiguous, consider "April of 2020", which could be represented as either of the
     * following options:
     *
     * <ul>
     *   <li>{@code "04/2020"}
     *   <li>{@code "4/2020"}
     *   <li>{@code "2020/04"}
     *   <li>{@code "2020/4"}
     *   <li>{@code "April/2020"}
     *   <li>{@code "Apr/2020"}
     * </ul>
     *
     * <p>You define a date autofill value for the view by overriding the following methods:
     *
     * <ol>
     *   <li>{@link android.view.View#getAutofillType()} to return {@link
     *       android.view.View#AUTOFILL_TYPE_DATE}.
     *   <li>{@link android.view.View#getAutofillValue()} to return a {@link
     *       android.view.autofill.AutofillValue#forDate(long) date autofillvalue}.
     *   <li>{@link android.view.View#autofill(android.view.autofill.AutofillValue)} to expect a
     *       data autofillvalue.
     * </ol>
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE =
            "creditCardExpirationDate";

    /**
     * Hint indicating that this view can be autofilled with a credit card expiration month.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH}</code>).
     *
     * <p>When annotating a view with this hint, it's recommended to use a text autofill value whose
     * value is the numerical representation of the month, starting on {@code 1} to avoid ambiguity
     * when the autofill service provides a value for it. To understand why a value can be
     * ambiguous, consider "January", which could be represented as either of
     *
     * <ul>
     *   <li>{@code "1"}: recommended way.
     *   <li>{@code "0"}: if following the {@link java.util.Calendar#MONTH} convention.
     *   <li>{@code "January"}: full name, in English.
     *   <li>{@code "jan"}: abbreviated name, in English.
     *   <li>{@code "Janeiro"}: full name, in another language.
     * </ul>
     *
     * <p>Another recommended approach is to use a date autofill value - see {@link
     * #AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE} for more details.
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH =
            "creditCardExpirationMonth";

    /**
     * Hint indicating that this view can be autofilled with a credit card expiration year.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR =
            "creditCardExpirationYear";

    /**
     * Hint indicating that this view can be autofilled with a credit card expiration day.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY = "creditCardExpirationDay";

    // New Extended Hints

    /**
     * Hint indicating that this view can be autofilled with a country name/code.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_POSTAL_ADDRESS_COUNTRY}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_POSTAL_ADDRESS_COUNTRY = "addressCountry";

    /**
     * Hint indicating that this view can be autofilled with a region/state.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_POSTAL_ADDRESS_REGION}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_POSTAL_ADDRESS_REGION = "addressRegion";

    /**
     * Hint indicating that this view can be autofilled with an address locality (city/town).
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_POSTAL_ADDRESS_LOCALITY}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_POSTAL_ADDRESS_LOCALITY = "addressLocality";

    /**
     * Hint indicating that this view can be autofilled with a street address.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS = "streetAddress";

    /**
     * Hint indicating that this view can be autofilled with auxiliary address details.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_ADDRESS}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_ADDRESS = "extendedAddress";

    /**
     * Hint indicating that this view can be autofilled with an extended ZIP/POSTAL code.
     *
     * <p>Example: In forms that split the U.S. ZIP+4 Code with nine digits 99999-9999 into two
     * fields annotate the delivery route code with this hint.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_POSTAL_CODE}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_POSTAL_CODE =
            "extendedPostalCode";

    /**
     * Hint indicating that this view can be autofilled with a person's full name.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_PERSON_NAME}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_PERSON_NAME = "personName";

    /**
     * Hint indicating that this view can be autofilled with a person's first/given name.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_PERSON_NAME_GIVEN}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_PERSON_NAME_GIVEN = "personGivenName";

    /**
     * Hint indicating that this view can be autofilled with a person's last/family name.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_PERSON_NAME_FAMILY}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_PERSON_NAME_FAMILY = "personFamilyName";

    /**
     * Hint indicating that this view can be autofilled with a person's middle name.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_PERSON_NAME_MIDDLE}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_PERSON_NAME_MIDDLE = "personMiddleName";

    /**
     * Hint indicating that this view can be autofilled with a person's middle initial.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_PERSON_NAME_MIDDLE_INITIAL}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_PERSON_NAME_MIDDLE_INITIAL = "personMiddleInitial";

    /**
     * Hint indicating that this view can be autofilled with a person's name prefix.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_PERSON_NAME_PREFIX}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_PERSON_NAME_PREFIX = "personNamePrefix";

    /**
     * Hint indicating that this view can be autofilled with a person's name suffix.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_PERSON_NAME_SUFFIX}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_PERSON_NAME_SUFFIX = "personNameSuffix";

    /**
     * Hint indicating that this view can be autofilled with the user's full phone number with
     * country code.
     *
     * <p>Example: <code>+1 123-456-7890</code>
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_PHONE_NUMBER}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_PHONE_NUMBER = "phoneNumber";

    /**
     * Hint indicating that this view can be autofilled with the current device's phone number
     * usually for Sign Up / OTP flows.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_PHONE_NUMBER_DEVICE}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_PHONE_NUMBER_DEVICE = "phoneNumberDevice";

    /**
     * Hint indicating that this view can be autofilled with a phone number's country code.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_PHONE_COUNTRY_CODE}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_PHONE_COUNTRY_CODE = "phoneCountryCode";

    /**
     * Hint indicating that this view can be autofilled with a phone number without country code.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_PHONE_NATIONAL}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_PHONE_NATIONAL = "phoneNational";

    /**
     * Hint indicating that this view can be interpreted as a newly created username for
     * save/update.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_NEW_USERNAME}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_NEW_USERNAME = "newUsername";

    /**
     * Hint indicating that this view can be interpreted as a newly created password for
     * save/update.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_NEW_PASSWORD}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_NEW_PASSWORD = "newPassword";

    /**
     * Hint indicating that this view can be autofilled with a gender.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_GENDER}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_GENDER = "gender";

    /**
     * Hint indicating that this view can be autofilled with a full birth date.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_BIRTH_DATE_FULL}</code>).
     *
     * <p>The recommended approach is to use a date autofill value - see {@link
     * #AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE} for more details.
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_BIRTH_DATE_FULL = "birthDateFull";

    /**
     * Hint indicating that this view can be autofilled with a birth day(of the month).
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_BIRTH_DATE_DAY}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_BIRTH_DATE_DAY = "birthDateDay";

    /**
     * Hint indicating that this view can be autofilled with a birth month.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_BIRTH_DATE_MONTH}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_BIRTH_DATE_MONTH = "birthDateMonth";

    /**
     * Hint indicating that this view can be autofilled with a birth year.
     *
     * <p>Can be used with either {@link android.view.View#setAutofillHints(String[])} or <a
     * href="#attr_android:autofillHint">{@code android:autofillHint}</a> (in which case the value
     * should be <code>{@value #AUTOFILL_HINT_BIRTH_DATE_YEAR}</code>).
     *
     * <p>See {@link android.view.View#setAutofillHints(String...)} for more info about autofill
     * hints.
     */
    public static final String AUTOFILL_HINT_BIRTH_DATE_YEAR = "birthDateYear";
}
