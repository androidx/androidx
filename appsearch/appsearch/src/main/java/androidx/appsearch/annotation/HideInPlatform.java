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

// @exportToFramework:skipFile()
package androidx.appsearch.annotation;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;

import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to indicate that the target API should be annotated with `@Hide` in the platform
 * Android Framework codebase.
 *
 * <p>This annotation is only used as a metadata signal for the codebase sync process and is
 * stripped during export.
 */
@Target({TYPE, FIELD, METHOD, CONSTRUCTOR, PACKAGE})
@Retention(RetentionPolicy.SOURCE)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public @interface HideInPlatform {}
