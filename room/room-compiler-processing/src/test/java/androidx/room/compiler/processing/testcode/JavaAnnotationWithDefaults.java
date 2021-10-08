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

import java.util.HashMap;
import java.util.LinkedHashMap;

public @interface JavaAnnotationWithDefaults {
    String stringVal() default "foo";
    String[] stringArrayVal() default {"x", "y"};
    Class<?> typeVal() default HashMap.class;
    Class[] typeArrayVal() default {LinkedHashMap.class};
    int intVal() default 3;
    int[] intArrayVal() default {1, 3, 5};
    JavaEnum enumVal() default JavaEnum.DEFAULT;
    JavaEnum[] enumArrayVal() default {JavaEnum.VAL1, JavaEnum.VAL2};
    OtherAnnotation otherAnnotationVal() default @OtherAnnotation("def");
    OtherAnnotation[] otherAnnotationArrayVal() default {@OtherAnnotation("v1")};
}
