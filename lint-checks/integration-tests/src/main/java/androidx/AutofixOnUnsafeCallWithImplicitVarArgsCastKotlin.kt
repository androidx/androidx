/*
 * Copyright 2025 The Android Open Source Project
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
package androidx

import android.widget.BaseAdapter
import androidx.annotation.RequiresApi
import java.nio.CharBuffer

/**
 * Contains unsafe calls to a method with a variable number of arguments which are implicitly cast.
 */
@Suppress("unused")
class AutofixOnUnsafeCallWithImplicitVarArgsCastKotlin {
    /** Calls the vararg method with no args. */
    @RequiresApi(27)
    fun callVarArgsMethodNoArgs(adapter: BaseAdapter) {
        adapter.setAutofillOptions()
    }

    /** Calls the vararg method with one args. */
    @RequiresApi(27)
    fun callVarArgsMethodOneArg(adapter: BaseAdapter, vararg: CharBuffer?) {
        adapter.setAutofillOptions(vararg)
    }

    /** Calls the vararg method with multiple args. */
    @RequiresApi(27)
    fun callVarArgsMethodManyArgs(
        adapter: BaseAdapter,
        vararg1: CharBuffer?,
        vararg2: CharBuffer?,
        vararg3: CharBuffer?,
    ) {
        adapter.setAutofillOptions(vararg1, vararg2, vararg3)
    }
}
