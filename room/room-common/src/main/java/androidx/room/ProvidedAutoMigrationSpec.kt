/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an auto migration spec that will be provided to Room at runtime.
 * <p>
 * An instance of a class annotated with this annotation has to be provided to Room using
 * {@code Room.databaseBuilder.addAutoMigrationSpec(AutoMigrationSpec)}. Room will verify that
 * the spec is provided in the builder configuration and if not, an
 * {@link IllegalArgumentException} will be thrown.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface ProvidedAutoMigrationSpec {
}
