/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core.properties;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(JUnit4.class)
public class PropertyJavaTest {

    @Test
    public void noArgConstructor_reasonableDefaultValues() {
        Property<StringValue> prop = new Property<>();

        assertThat(prop.isSupported()).isTrue();
        assertThat(prop.isRequiredForExecution()).isFalse();
        assertThat(prop.shouldMatchPossibleValues()).isFalse();
        assertThat(prop.getPossibleValues()).isEmpty();
    }

    @Test
    public void fullConstructor_returnsAllValues() {
        Property<StringValue> prop =
                new Property<>(
                        Arrays.asList(new StringValue("test")),
                        /** isRequiredForExecution= */
                        true,
                        /** shouldMatchPossibleValues= */
                        true);

        assertThat(prop.isSupported()).isTrue();
        assertThat(prop.isRequiredForExecution()).isTrue();
        assertThat(prop.shouldMatchPossibleValues()).isTrue();
        assertThat(prop.getPossibleValues()).containsExactly(new StringValue("test"));
    }

    @Test
    public void supplierConstructor_returnsValues() {
        ArrayList<StringValue> mutableValues = new ArrayList<>();
        Property<StringValue> prop = new Property<>(() -> mutableValues);

        assertThat(prop.shouldMatchPossibleValues()).isFalse();
        assertThat(prop.getPossibleValues()).isEmpty();

        // Mutate list
        mutableValues.add(new StringValue("test"));

        assertThat(prop.getPossibleValues()).containsExactly(new StringValue("test"));
    }

    @Test
    public void staticUnsupportedMethod_returnsSensibleValues() {
        Property<StringValue> prop = Property.unsupported();

        assertThat(prop.isSupported()).isFalse();
        assertThat(prop.isRequiredForExecution()).isFalse();
        assertThat(prop.shouldMatchPossibleValues()).isFalse();
        assertThat(prop.getPossibleValues()).isEmpty();
    }
}
