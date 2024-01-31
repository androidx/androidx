/*
 * Copyright 2024 The Android Open Source Project
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

package replacewith;

import android.view.View;

import androidx.annotation.ReplaceWith;

@SuppressWarnings({"unused", "UnknownNullness", "InnerClassMayBeStatic",
        "InstantiationOfUtilityClass", "ClassCanBeStatic", "PrivateConstructorForUtilityClass"})
public class ReplaceWithUsageJava {
    /**
     * Calls the method on the object.
     *
     * @param obj The object on which to call the method.
     * @deprecated Use {@link Object#toString()} directly.
     */
    @Deprecated
    @ReplaceWith(expression = "obj.toString()")
    public static void toString(Object obj) {
        // Stub.
    }

    /**
     * Calls the method on the object.
     *
     * @param obj The object on which to call the method.
     * @deprecated Use {@link Object#toString()} directly.
     */
    @Deprecated
    @ReplaceWith(expression = "obj.toString()", imports = "androidx.annotation.Deprecated")
    public static void toStringWithImport(Object obj) {
        // Stub.
    }

    /**
     * Calls the method on the object.
     *
     * @param obj The object on which to call the method.
     * @deprecated Use {@link Object#toString()} directly.
     */
    @Deprecated
    @ReplaceWith(expression = "obj.toString()",
            imports = {"androidx.annotation.Deprecated", "androidx.annotation.NonNull"})
    public static void toStringWithImports(Object obj) {
        // Stub.
    }

    /**
     * Returns a new object.
     */
    public static ReplaceWithUsageJava obtain(int param) {
        return new ReplaceWithUsageJava();
    }

    /**
     * String constant.
     *
     * @deprecated Use {@link View#AUTOFILL_HINT_NAME} directly.
     */
    @Deprecated
    @ReplaceWith(expression = "View.AUTOFILL_HINT_NAME")
    public static final String AUTOFILL_HINT_NAME = View.AUTOFILL_HINT_NAME;

    /**
     * Constructor.
     *
     * @deprecated Use {@link StringBuffer#StringBuffer(String)} instead.
     */
    @Deprecated
    @ReplaceWith(expression = "StringBuffer(param)")
    public ReplaceWithUsageJava(String param) {
        // Stub.
    }

    /**
     * Constructor.
     *
     * @deprecated Use {@link ReplaceWithUsageJava#obtain(int)} instead.
     */
    @Deprecated
    @ReplaceWith(expression = "ReplaceWithUsageJava.newInstance(param)")
    public ReplaceWithUsageJava(int param) {
        // Stub.
    }

    /**
     * Constructor.
     */
    public ReplaceWithUsageJava() {
        // Stub.
    }

    class InnerClass {
        /**
         * Constructor.
         *
         * @deprecated Use {@link InnerClass#InnerClass()} instead.
         */
        @Deprecated
        @ReplaceWith(expression = "InnerClass()")
        InnerClass(String param) {
            // Stub.
        }

        /**
         * Constructor.
         */
        InnerClass() {
            // Stub.
        }
    }
}
