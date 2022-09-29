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

package androidx.camera.video;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.auto.value.AutoValue;

/** A fake implementation of {@link OutputOptions}. */
// Java is used because @AutoValue is required.
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class FakeOutputOptions extends OutputOptions {

    private FakeOutputOptions(@NonNull FakeOutputOptionsInternal fakeOutputOptionsInternal) {
        super(fakeOutputOptionsInternal);
    }

    /** The builder of the {@link FakeOutputOptions} object. */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public static final class Builder extends OutputOptions.Builder<FakeOutputOptions, Builder> {

        /** Creates a builder of the {@link FakeOutputOptions}. */
        public Builder() {
            super(new AutoValue_FakeOutputOptions_FakeOutputOptionsInternal.Builder());
        }

        /** Builds the {@link FakeOutputOptions} instance. */
        @Override
        @NonNull
        public FakeOutputOptions build() {
            return new FakeOutputOptions(
                    ((FakeOutputOptionsInternal.Builder) mRootInternalBuilder).build());
        }
    }

    @AutoValue
    abstract static class FakeOutputOptionsInternal extends OutputOptions.OutputOptionsInternal {

        @AutoValue.Builder
        abstract static class Builder extends OutputOptions.OutputOptionsInternal.Builder<Builder> {

            @SuppressWarnings("NullableProblems") // Nullable problem in AutoValue generated class
            @Override
            @NonNull
            abstract FakeOutputOptionsInternal build();
        }
    }
}
