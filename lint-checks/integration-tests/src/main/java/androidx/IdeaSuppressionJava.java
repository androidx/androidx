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

package androidx;

/**
 * Java usage of inline suppression.
 */
@SuppressWarnings("unused")
public class IdeaSuppressionJava {

    /**
     * Call to a deprecated method with an inline suppression.
     */
    public void callDeprecatedMethod() {
        //noinspection deprecation
        deprecatedMethod();

        notDeprecatedMethod();
    }

    /**
     * Thie method is deprecated.
     *
     * @deprecated Replaced with {@link #notDeprecatedMethod()}
     */
    @Deprecated
    public void deprecatedMethod() {}

    /**
     * This method is not deprecated.
     */
    public void notDeprecatedMethod() {}

}
