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

/**
 * Contains regex patterns used to detect Autofill hints from text. These patterns are based on
 * common field labels and attributes.
 */
private val NAME_PATTERNS =
    mapOf(
        // Name
        HintConstants.AUTOFILL_HINT_PERSON_NAME_GIVEN to
            Regex("""\b((first|given)(.*name)?|fname|initials)\b"""),
        HintConstants.AUTOFILL_HINT_PERSON_NAME_MIDDLE to
            Regex("""\b(middle.*name|mname|middle)\b"""),
        HintConstants.AUTOFILL_HINT_PERSON_NAME_FAMILY to
            Regex("""\b((last|family|sur|second)(.*name)?|lname|surname)\b"""),
        HintConstants.AUTOFILL_HINT_PERSON_NAME to
            Regex(
                """\b(name|(full|your|customer|bill|ship|contact).?name|contact.?person|receiver)\b"""
            ),
    )

private val CONTACT_PATTERNS =
    mapOf(
        HintConstants.AUTOFILL_HINT_EMAIL_ADDRESS to Regex("""\b(e.?mail)\b"""),
        HintConstants.AUTOFILL_HINT_PHONE_NUMBER to
            Regex("""\b(phone|mobile|contact.?(number|num|no))\b"""),
        HintConstants.AUTOFILL_HINT_PHONE_COUNTRY_CODE to
            Regex("""\b(country.*code|ccode|cc|phone.*code|user.*phone.*code)\b"""),
    )

private val ADDRESS_PATTERNS =
    mapOf(
        HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_APT_NUMBER to
            Regex("""\b(?:apartment|flat|suite|apt|room\s+(number|num|no))\b"""),
        HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS to
            Regex(
                """\b((?<!(e\s)?mail\s)(address|addr)(.?line)?\d*|street|house.?name|(?:shipping|billing).?address)\b"""
            ),
        HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_DEPENDENT_LOCALITY to
            Regex("""\b(area|locality|street|colony|sector|village|district|neighbo(u)?rhood)\b"""),
        HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_LOCALITY to Regex("""\b(city|town|suburb)\b"""),
        HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_REGION to
            Regex(
                """\b((?<!(united|hist|history).?)state|region|province|county|principality)\b"""
            ),
        HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_COUNTRY to Regex("""\b(country|nation)\b"""),
        HintConstants.AUTOFILL_HINT_POSTAL_CODE to
            Regex("""\b(postal|post.*code|pcode|(pin|zip).?code)\b"""),
        HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS to
            Regex(
                """\b((full|complete|mailing|postal|home|delivery|shipping|billing).?address|(?<!(e\s)?mail\s)address)\b"""
            ),
    )

private val BIRTH_AND_GENDER_PATTERNS =
    mapOf(
        HintConstants.AUTOFILL_HINT_BIRTH_DATE_FULL to
            Regex("""\b(?:date.?of.?birth|dob|birth.?date)\b"""),
        HintConstants.AUTOFILL_HINT_BIRTH_DATE_DAY to Regex("""\b(birth.?day|day.?of.?birth)\b"""),
        HintConstants.AUTOFILL_HINT_BIRTH_DATE_MONTH to
            Regex("""\b(birth.?month|month.?of.?birth)\b"""),
        HintConstants.AUTOFILL_HINT_BIRTH_DATE_YEAR to
            Regex("""\b(birth.?year|year.?of.?birth)\b"""),
        HintConstants.AUTOFILL_HINT_GENDER to Regex("""\b(?:gender|sex)\b"""),
    )

internal val AutofillHintPatterns =
    NAME_PATTERNS + CONTACT_PATTERNS + ADDRESS_PATTERNS + BIRTH_AND_GENDER_PATTERNS
