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

package sample.optin;

/**
 * Class which is stable but has experimental members.
 */
class AnnotatedJavaMembers {
    @ExperimentalJavaAnnotation
    public static final int FIELD_STATIC = -1;

    private int mFieldWithSetMarker;

    @ExperimentalJavaAnnotation
    public static int methodStatic() {
        return -1;
    }

    @ExperimentalJavaAnnotation
    public int field = -1;

    @ExperimentalJavaAnnotation
    public int method() {
        return -1;
    }

    @ExperimentalJavaAnnotation
    public int getAccessor() {
        return -1;
    }

    public int getFieldWithSetMarker() {
        return mFieldWithSetMarker;
    }

    @ExperimentalJavaAnnotation
    public void setFieldWithSetMarker(int value) {
        mFieldWithSetMarker = value;
    }
}
