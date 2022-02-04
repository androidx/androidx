/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.appsearch.utils;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class DateTimeFormatValidatorTest {
    @Test
    public void testValidateISO8601Date_validDate_returnsTrue() {
        assertThat(DateTimeFormatValidator.validateISO8601Date("2022-01-01")).isTrue();
    }

    @Test
    public void testValidateISO8601Date_invalidDate_returnsFalse() {
        assertThat(DateTimeFormatValidator.validateISO8601Date("2022:01:01")).isFalse();
    }

    @Test
    public void testValidateISO8601Date_notExactMatch_returnsFalse() {
        assertThat(DateTimeFormatValidator.validateISO8601Date("2022-01-01T00:00:00"))
                .isFalse();
    }

    @Test
    public void testValidateISO8601DateTime_validDate_returnsTrue() {
        assertThat(DateTimeFormatValidator.validateISO8601DateTime("2022-01-01T00:00:00"))
                .isTrue();
    }

    @Test
    public void testValidateISO8601DateTime_validDateWithoutSeconds_returnsTrue() {
        assertThat(DateTimeFormatValidator.validateISO8601DateTime("2022-01-01T00:00"))
                .isTrue();
    }

    @Test
    public void testValidateISO8601DateTime_invalidDate_returnsFalse() {
        assertThat(DateTimeFormatValidator.validateISO8601DateTime("2022:01:01T00-00-00"))
                .isFalse();
    }

    @Test
    public void testValidateISO8601DateTime_notExactMatch_returnsFalse() {
        assertThat(DateTimeFormatValidator.validateISO8601DateTime("2022-01-01T00:00:00.000"))
                .isFalse();
    }
}
