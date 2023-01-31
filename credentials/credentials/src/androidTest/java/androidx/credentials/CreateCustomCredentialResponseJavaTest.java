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

package androidx.credentials;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.os.Bundle;

import org.junit.Test;

public class CreateCustomCredentialResponseJavaTest {
    @Test
    public void constructor_nullType_throws() {
        assertThrows("Expected null type to throw NPE",
                NullPointerException.class,
                () -> new CreateCustomCredentialResponse(null, new Bundle())
        );
    }

    @Test
    public void constructor_nullBundle_throws() {
        assertThrows("Expected null bundle to throw NPE",
                NullPointerException.class,
                () -> new CreateCustomCredentialResponse("T", null)
        );
    }

    @Test
    public void constructor_emptyType_throws() {
        assertThrows("Expected empty type to throw IAE",
                IllegalArgumentException.class,
                () -> new CreateCustomCredentialResponse("", new Bundle())
        );
    }

    @Test
    public void constructor_nonEmptyTypeNonNullBundle_success() {
        new CreateCustomCredentialResponse("T", new Bundle());
    }

    @Test
    public void getter_frameworkProperties() {
        String expectedType = "TYPE";
        Bundle expectedBundle = new Bundle();
        expectedBundle.putString("Test", "Test");

        CreateCustomCredentialResponse option = new CreateCustomCredentialResponse(expectedType,
                expectedBundle);

        assertThat(option.getType()).isEqualTo(expectedType);
        assertThat(TestUtilsKt.equals(option.getData(), expectedBundle)).isTrue();
    }
}
