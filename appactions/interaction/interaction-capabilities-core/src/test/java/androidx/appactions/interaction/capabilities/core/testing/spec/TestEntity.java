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

package androidx.appactions.interaction.capabilities.core.testing.spec;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf;

import com.google.auto.value.AutoValue;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;

/** A test class for capability value. */
@AutoValue
public abstract class TestEntity {

    public static Builder newBuilder() {
        return new AutoValue_TestEntity.Builder();
    }

    public abstract Optional<String> getName();

    public abstract Optional<Duration> getDuration();

    public abstract Optional<ZonedDateTime> getZonedDateTime();

    public abstract Optional<TestEnum> getEnum();

    public abstract Optional<TestEntity> getEntity();

    public enum TestEnum {
        VALUE_1("value_1"),
        VALUE_2("value_2");

        private final String mStringValue;

        TestEnum(String stringValue) {
            this.mStringValue = stringValue;
        }

        @NonNull
        @Override
        public String toString() {
            return mStringValue;
        }
    }

    @AutoValue.Builder
    public abstract static class Builder implements BuilderOf<TestEntity> {

        public abstract Builder setName(String name);

        public abstract Builder setDuration(Duration duration);

        public abstract Builder setZonedDateTime(ZonedDateTime date);

        public abstract Builder setEnum(TestEnum enumValue);

        public abstract Builder setEntity(TestEntity entity);
    }
}
