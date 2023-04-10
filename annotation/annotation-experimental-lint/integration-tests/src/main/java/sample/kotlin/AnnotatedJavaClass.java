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

package sample.kotlin;

/**
 * Class which is experimental.
 */
@ExperimentalJavaAnnotation
public class AnnotatedJavaClass {
    public static final int FIELD_STATIC = -1;

    /**
     * Method which is static.
     */
    public static int methodStatic() {
        return -1;
    }

    /**
     * Field which is final and dynamic.
     */
    public final int field = -1;

    /**
     * Method which is dynamic.
     */
    public int method() {
        return -1;
    }
}
