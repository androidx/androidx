/*
 * Copyright 2021 The Android Open Source Project
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
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
public @interface JavaAnnotationWithPrimitiveArray {
    int[] intArray() default {};

    double[] doubleArray() default {};

    float[] floatArray() default {};

    char[] charArray() default {};

    byte[] byteArray() default {};

    short[] shortArray() default {};

    long[] longArray() default {};

    boolean[] booleanArray() default {};
}
