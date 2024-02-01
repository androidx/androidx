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

package androidx.wear.protolayout.expression;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import kotlin.annotation.MustBeDocumented;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates the minimum schema version the annotated type is supported at. {@link #major()} and
 * {@link #minor()} correspond to {@link VersionBuilders.VersionInfo#getMajor()} and {@link
 * VersionBuilders.VersionInfo#getMinor()} values reported by renderers/evaluators of ProtoLayout.
 *
 * <p>Note that {@link #minor()} version is usually in the form of {@code x00} such as 100, 200, ...
 */
@MustBeDocumented
@Retention(CLASS)
@Target({TYPE, METHOD, CONSTRUCTOR, FIELD})
public @interface RequiresSchemaVersion {
    int major();

    int minor();
}
