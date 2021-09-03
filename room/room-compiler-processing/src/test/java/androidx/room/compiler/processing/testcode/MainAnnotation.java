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

package androidx.room.compiler.processing.testcode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * used in compilation tests
 */
@SuppressWarnings("unused")
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MainAnnotation {
    Class<?>[] typeList();

    Class<?> singleType();

    int intMethod();

    double doubleMethodWithDefault() default 0;

    float floatMethodWithDefault() default 0;

    char charMethodWithDefault() default 0;

    byte byteMethodWithDefault() default 0;

    short shortMethodWithDefault() default 0;

    long longMethodWithDefault() default 0;

    boolean boolMethodWithDefault() default true;

    OtherAnnotation[] otherAnnotationArray() default {};

    OtherAnnotation singleOtherAnnotation();
}
