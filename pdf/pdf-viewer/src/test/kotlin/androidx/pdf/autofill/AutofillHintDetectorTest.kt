/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.pdf.autofill

import androidx.autofill.HintConstants
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class AutofillHintDetectorTest {

    private lateinit var detector: AutofillHintDetector

    @Before
    fun setUp() {
        detector = DefaultAutofillHintDetector()
    }

    @Test
    fun detectHints_personNamePatterns() {
        // GIVEN NAME
        assertThat(detector.detectHints("First Name"))
            .containsExactly(
                HintConstants.AUTOFILL_HINT_PERSON_NAME_GIVEN,
                HintConstants.AUTOFILL_HINT_PERSON_NAME,
            )
        assertThat(detector.detectHints("given name"))
            .contains(HintConstants.AUTOFILL_HINT_PERSON_NAME_GIVEN)
        assertThat(detector.detectHints("fname"))
            .contains(HintConstants.AUTOFILL_HINT_PERSON_NAME_GIVEN)
        assertThat(detector.detectHints("initials"))
            .contains(HintConstants.AUTOFILL_HINT_PERSON_NAME_GIVEN)

        // MIDDLE NAME
        assertThat(detector.detectHints("Middle Name"))
            .contains(HintConstants.AUTOFILL_HINT_PERSON_NAME_MIDDLE)
        assertThat(detector.detectHints("mname"))
            .contains(HintConstants.AUTOFILL_HINT_PERSON_NAME_MIDDLE)
        assertThat(detector.detectHints("middle"))
            .contains(HintConstants.AUTOFILL_HINT_PERSON_NAME_MIDDLE)

        // FAMILY NAME
        assertThat(detector.detectHints("Last Name"))
            .contains(HintConstants.AUTOFILL_HINT_PERSON_NAME_FAMILY)
        assertThat(detector.detectHints("family name"))
            .contains(HintConstants.AUTOFILL_HINT_PERSON_NAME_FAMILY)
        assertThat(detector.detectHints("surname"))
            .contains(HintConstants.AUTOFILL_HINT_PERSON_NAME_FAMILY)
        assertThat(detector.detectHints("lname"))
            .contains(HintConstants.AUTOFILL_HINT_PERSON_NAME_FAMILY)
        assertThat(detector.detectHints("second name"))
            .contains(HintConstants.AUTOFILL_HINT_PERSON_NAME_FAMILY)

        // FULL NAME / GENERAL NAME
        assertThat(detector.detectHints("Name")).contains(HintConstants.AUTOFILL_HINT_PERSON_NAME)
        assertThat(detector.detectHints("full name"))
            .contains(HintConstants.AUTOFILL_HINT_PERSON_NAME)
        assertThat(detector.detectHints("your name"))
            .contains(HintConstants.AUTOFILL_HINT_PERSON_NAME)
        assertThat(detector.detectHints("customer name"))
            .contains(HintConstants.AUTOFILL_HINT_PERSON_NAME)
        assertThat(detector.detectHints("bill name"))
            .contains(HintConstants.AUTOFILL_HINT_PERSON_NAME)
        assertThat(detector.detectHints("billing name"))
            .contains(HintConstants.AUTOFILL_HINT_PERSON_NAME)
        assertThat(detector.detectHints("ship name"))
            .contains(HintConstants.AUTOFILL_HINT_PERSON_NAME)
        assertThat(detector.detectHints("contact name"))
            .contains(HintConstants.AUTOFILL_HINT_PERSON_NAME)
        assertThat(detector.detectHints("contact person"))
            .contains(HintConstants.AUTOFILL_HINT_PERSON_NAME)
        assertThat(detector.detectHints("receiver"))
            .contains(HintConstants.AUTOFILL_HINT_PERSON_NAME)
    }

    @Test
    fun detectHints_contactDetailsPatterns() {
        // EMAIL
        assertThat(detector.detectHints("Email"))
            .containsExactly(HintConstants.AUTOFILL_HINT_EMAIL_ADDRESS)
        assertThat(detector.detectHints("e-mail"))
            .containsExactly(HintConstants.AUTOFILL_HINT_EMAIL_ADDRESS)
        assertThat(detector.detectHints("e.mail"))
            .containsExactly(HintConstants.AUTOFILL_HINT_EMAIL_ADDRESS)

        // PHONE NUMBER
        assertThat(detector.detectHints("Phone"))
            .containsExactly(HintConstants.AUTOFILL_HINT_PHONE_NUMBER)
        assertThat(detector.detectHints("mobile"))
            .containsExactly(HintConstants.AUTOFILL_HINT_PHONE_NUMBER)
        assertThat(detector.detectHints("contact number"))
            .containsExactly(HintConstants.AUTOFILL_HINT_PHONE_NUMBER)
        assertThat(detector.detectHints("contact num"))
            .containsExactly(HintConstants.AUTOFILL_HINT_PHONE_NUMBER)
        assertThat(detector.detectHints("contact no"))
            .containsExactly(HintConstants.AUTOFILL_HINT_PHONE_NUMBER)

        // PHONE COUNTRY CODE
        assertThat(detector.detectHints("country code"))
            .contains(HintConstants.AUTOFILL_HINT_PHONE_COUNTRY_CODE)
        assertThat(detector.detectHints("ccode"))
            .containsExactly(HintConstants.AUTOFILL_HINT_PHONE_COUNTRY_CODE)
        assertThat(detector.detectHints("cc"))
            .containsExactly(HintConstants.AUTOFILL_HINT_PHONE_COUNTRY_CODE)
        assertThat(detector.detectHints("phone code"))
            .containsExactly(
                HintConstants.AUTOFILL_HINT_PHONE_COUNTRY_CODE,
                HintConstants.AUTOFILL_HINT_PHONE_NUMBER,
            )
        assertThat(detector.detectHints("user phone code"))
            .containsExactly(
                HintConstants.AUTOFILL_HINT_PHONE_NUMBER,
                HintConstants.AUTOFILL_HINT_PHONE_COUNTRY_CODE,
            )
    }

    @Test
    fun detectHints_addressPatterns() {
        // APT NUMBER
        assertThat(detector.detectHints("apartment"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_APT_NUMBER)
        assertThat(detector.detectHints("flat"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_APT_NUMBER)
        assertThat(detector.detectHints("suite"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_APT_NUMBER)
        assertThat(detector.detectHints("apt"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_APT_NUMBER)
        assertThat(detector.detectHints("room number"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_APT_NUMBER)
        assertThat(detector.detectHints("room num"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_APT_NUMBER)
        assertThat(detector.detectHints("room no"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_APT_NUMBER)

        // STREET ADDRESS
        assertThat(detector.detectHints("address"))
            .contains(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS)
        assertThat(detector.detectHints("addr"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS)
        assertThat(detector.detectHints("address line 1"))
            .contains(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS)
        assertThat(detector.detectHints("addr line 2"))
            .contains(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS)
        assertThat(detector.detectHints("street"))
            .containsExactly(
                HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS,
                HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_DEPENDENT_LOCALITY,
            )
        assertThat(detector.detectHints("houseName"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS)
        assertThat(detector.detectHints("shipping address"))
            .containsExactly(
                HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS,
                HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS,
            )
        assertThat(detector.detectHints("billing address"))
            .containsExactly(
                HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS,
                HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS,
            )

        // DEPENDENT LOCALITY
        assertThat(detector.detectHints("area"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_DEPENDENT_LOCALITY)
        assertThat(detector.detectHints("locality"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_DEPENDENT_LOCALITY)
        assertThat(detector.detectHints("colony"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_DEPENDENT_LOCALITY)
        assertThat(detector.detectHints("sector"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_DEPENDENT_LOCALITY)
        assertThat(detector.detectHints("village"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_DEPENDENT_LOCALITY)
        assertThat(detector.detectHints("district"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_DEPENDENT_LOCALITY)
        assertThat(detector.detectHints("neighborhood"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_DEPENDENT_LOCALITY)
        assertThat(detector.detectHints("neighbourhood"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_DEPENDENT_LOCALITY)

        // LOCALITY
        assertThat(detector.detectHints("city"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_LOCALITY)
        assertThat(detector.detectHints("town"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_LOCALITY)
        assertThat(detector.detectHints("suburb"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_LOCALITY)

        // REGION (STATE)
        assertThat(detector.detectHints("state"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_REGION)
        assertThat(detector.detectHints("region"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_REGION)
        assertThat(detector.detectHints("province"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_REGION)
        assertThat(detector.detectHints("county"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_REGION)
        assertThat(detector.detectHints("principality"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_REGION)

        // COUNTRY
        assertThat(detector.detectHints("country"))
            .contains(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_COUNTRY)
        assertThat(detector.detectHints("nation"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_COUNTRY)

        // POSTAL CODE
        assertThat(detector.detectHints("zip code"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_CODE)
        assertThat(detector.detectHints("postal"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_CODE)
        assertThat(detector.detectHints("post code"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_CODE)
        assertThat(detector.detectHints("postal code"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_CODE)
        assertThat(detector.detectHints("pcode"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_CODE)
        assertThat(detector.detectHints("pin code"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_CODE)
        assertThat(detector.detectHints("pincode"))
            .containsExactly(HintConstants.AUTOFILL_HINT_POSTAL_CODE)

        // POSTAL ADDRESS (FULL)
        assertThat(detector.detectHints("full address"))
            .containsExactly(
                HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS,
                HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS,
            )
        assertThat(detector.detectHints("mailing address"))
            .containsExactly(
                HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS,
                HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS,
            )
        assertThat(detector.detectHints("delivery address"))
            .containsExactly(
                HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS,
                HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS,
            )
    }

    @Test
    fun detectHints_birthDateAndGenderPatterns() {
        // BIRTH DATE FULL
        assertThat(detector.detectHints("date of birth"))
            .containsExactly(HintConstants.AUTOFILL_HINT_BIRTH_DATE_FULL)
        assertThat(detector.detectHints("dob"))
            .containsExactly(HintConstants.AUTOFILL_HINT_BIRTH_DATE_FULL)
        assertThat(detector.detectHints("birth date"))
            .containsExactly(HintConstants.AUTOFILL_HINT_BIRTH_DATE_FULL)

        // BIRTH DAY
        assertThat(detector.detectHints("birthday"))
            .containsExactly(HintConstants.AUTOFILL_HINT_BIRTH_DATE_DAY)
        assertThat(detector.detectHints("day of birth"))
            .containsExactly(HintConstants.AUTOFILL_HINT_BIRTH_DATE_DAY)

        // BIRTH MONTH
        assertThat(detector.detectHints("birth month"))
            .containsExactly(HintConstants.AUTOFILL_HINT_BIRTH_DATE_MONTH)
        assertThat(detector.detectHints("month of birth"))
            .containsExactly(HintConstants.AUTOFILL_HINT_BIRTH_DATE_MONTH)

        // BIRTH YEAR
        assertThat(detector.detectHints("birth year"))
            .containsExactly(HintConstants.AUTOFILL_HINT_BIRTH_DATE_YEAR)
        assertThat(detector.detectHints("year of birth"))
            .containsExactly(HintConstants.AUTOFILL_HINT_BIRTH_DATE_YEAR)

        // GENDER
        assertThat(detector.detectHints("gender"))
            .containsExactly(HintConstants.AUTOFILL_HINT_GENDER)
        assertThat(detector.detectHints("sex")).containsExactly(HintConstants.AUTOFILL_HINT_GENDER)
    }

    @Test
    fun detectHints_negativeLookbehinds() {
        // address should NOT match email address
        assertThat(detector.detectHints("email address"))
            .doesNotContain(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS)
        assertThat(detector.detectHints("e mail address"))
            .doesNotContain(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS)

        // state should NOT match united state or history state
        assertThat(detector.detectHints("united states"))
            .doesNotContain(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_REGION)
        assertThat(detector.detectHints("history state"))
            .doesNotContain(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_REGION)
    }

    @Test
    fun detectHints_normalization() {
        // Case sensitivity
        assertThat(detector.detectHints("FIRST NAME"))
            .contains(HintConstants.AUTOFILL_HINT_PERSON_NAME_GIVEN)

        // Special characters replaced by space
        assertThat(detector.detectHints("First-Name"))
            .contains(HintConstants.AUTOFILL_HINT_PERSON_NAME_GIVEN)
        assertThat(detector.detectHints("Email@Address"))
            .contains(HintConstants.AUTOFILL_HINT_EMAIL_ADDRESS)

        // Multiple whitespaces collapsed
        assertThat(detector.detectHints("First    Name"))
            .contains(HintConstants.AUTOFILL_HINT_PERSON_NAME_GIVEN)

        // Trimming
        assertThat(detector.detectHints("  Email  "))
            .containsExactly(HintConstants.AUTOFILL_HINT_EMAIL_ADDRESS)
    }

    @Test
    fun detectHints_noMatch() {
        assertThat(detector.detectHints("")).isEmpty()
        assertThat(detector.detectHints("   ")).isEmpty()
        assertThat(detector.detectHints("unknown_field_123")).isEmpty()
        assertThat(detector.detectHints("!@#$%^&*()")).isEmpty()
    }
}
