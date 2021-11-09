/*
 * Copyright (C) 2020 The Android Open Source Project
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
package androidx.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Denotes that the annotated method checks if the SDK_INT API level is
 * at least the given value, and either returns it or executes the
 * given lambda in that case (or if it's a field, has the value true).
 *
 * The API level can be specified either as an API level via
 * {@link #api()}, or for preview platforms as a codename (such as "R") via
 * {@link #codename()}}, or it can be passed in to the method; in that
 * case, the parameter containing the API level or code name should
 * be specified via {@link #parameter()}, where the first parameter
 * is numbered 0.
 *
 * <p>
 * Examples:
 * <pre>
 *  // Simple version check
 *  &#64;ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
 *  public static boolean isAtLeastO() {
 *      return Build.VERSION.SDK_INT >= 26;
 *  }
 *
 *  // Required API level is passed in as first argument, and function
 *  // in second parameter is executed if SDK_INT is at least that high:
 *  &#64;ChecksSdkIntAtLeast(parameter = 0, lambda = 1)
 *  inline fun fromApi(value: Int, action: () -> Unit) {
 *      if (Build.VERSION.SDK_INT >= value) {
 *          action()
 *      }
 *  }
 *
 *  // Kotlin property:
 *  &#64;get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.GINGERBREAD)
 *  val isGingerbread: Boolean
 *     get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD
 *
 *  // Java field:
 *  &#64;ChecksSdkIntAtLeast(api = Build.VERSION_CODES.LOLLIPOP)
 *  public static final boolean SUPPORTS_LETTER_SPACING =
 *         Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
 *
 * </pre>
 */
@Documented
@Retention(CLASS)
@Target({METHOD, FIELD})
public @interface ChecksSdkIntAtLeast {
    /**
     * The API level is at least the given level
     */
    int api() default -1;

    /**
     * The API level is at least the given codename (such as "R")
     */
    String codename() default "";

    /**
     * The API level is specified in the given parameter, where the first parameter is number 0
     */
    int parameter() default -1;

    /**
     * The parameter number for a lambda that will be executed if the API level is at least
     * the value supplied via {@link #api()}, {@link #codename()} or
     * {@link #parameter()}
     */
    int lambda() default -1;
}
