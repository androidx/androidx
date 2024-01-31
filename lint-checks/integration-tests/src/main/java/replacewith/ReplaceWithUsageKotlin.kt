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

@file:Suppress("unused", "UNUSED_PARAMETER")

package replacewith

import android.view.View

class ReplaceWithUsageKotlin {

    /**
     * Constructor.
     *
     * @deprecated Use [java.lang.StringBuffer#StringBuffer(String)] instead.
     */
    @Deprecated(
        message = "Use [java.lang.StringBuffer#StringBuffer(String)] instead.",
        replaceWith = ReplaceWith("StringBuffer(param)", "java.lang.StringBuffer")
    )
    constructor(param: String) {
        // Stub.
    }

    /**
     * Constructor.
     *
     * @deprecated Use [ReplaceWithUsageKotlin#obtain(int)] instead.
     */
    @Deprecated(
        message = "Use [ReplaceWithUsageKotlin#obtain(Int)] instead.",
        replaceWith = ReplaceWith("ReplaceWithUsageKotlin.obtain(param)")
    )
    constructor(param: Int) {
        // Stub.
    }

    /**
     * Constructor.
     */
    constructor() {
        // Stub.
    }

    inner class InnerClass {
        /**
         * Constructor.
         *
         * @deprecated Use [InnerClass#InnerClass()] instead.
         */

        @Deprecated("Use [InnerClass#InnerClass()] instead.", ReplaceWith("InnerClass()"))
        constructor(param: String) {
            // Stub.
        }

        /**
         * Constructor.
         */
        constructor() {
            // Stub.
        }
    }

    companion object {
        /**
         * Calls the method on the object.
         *
         * @param obj The object on which to call the method.
         */
        @Deprecated("Use [Object#toString()] directly.", ReplaceWith("obj.toString()"))
        @JvmStatic
        fun toString(obj: Any) {
            obj.toString()
        }

        /**
         * Returns a new object.
         */
        @JvmStatic
        fun obtain(param: Int): ReplaceWithUsageKotlin {
            return ReplaceWithUsageKotlin()
        }

        /**
         * String constant.
         */
        @Deprecated(
            message = "Use {@link View#AUTOFILL_HINT_NAME} directly.",
            ReplaceWith(expression = "View.AUTOFILL_HINT_NAME")
        )
        @JvmStatic
        val AUTOFILL_HINT_NAME = View.AUTOFILL_HINT_NAME
    }
}
