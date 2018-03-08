/*
 * Copyright (C) 2016 The Android Open Source Project
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
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * <p>Denotes that the annotated element represents a half-precision floating point
 * value. Such values are stored in short data types and can be manipulated with
 * the <code>android.util.Half</code> class. If applied to an array of short, every
 * element in the array represents a half-precision float.</p>
 *
 * <p>Example:</p>
 *
 * <pre>{@code
 * public abstract void setPosition(@HalfFloat short x, @HalfFloat short y, @HalfFloat short z);
 * }</pre>
 */
@Retention(SOURCE)
@Target({PARAMETER, METHOD, LOCAL_VARIABLE, FIELD})
public @interface HalfFloat {
}
