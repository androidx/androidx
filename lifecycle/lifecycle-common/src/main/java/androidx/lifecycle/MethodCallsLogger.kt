/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.lifecycle

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public open class MethodCallsLogger {
    private val calledMethods: MutableMap<String, Int> = HashMap()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public open fun approveCall(name: String, type: Int): Boolean {
        val nullableMask = calledMethods[name]
        val mask = nullableMask ?: 0
        val wasCalled = mask and type != 0
        calledMethods[name] = mask or type
        return !wasCalled
    }
}
