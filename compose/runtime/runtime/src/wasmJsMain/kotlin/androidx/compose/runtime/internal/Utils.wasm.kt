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

package androidx.compose.runtime.internal

/**
 * We intentionally use the default `instance.hashCode()` here.
 * The consequence is that the returned values can be more often not unique,
 * but it's not required for correctness (absolute uniqueness can't be guaranteed on any platform).
 * It has good performance comparing with alternatives.
 *
 * For more details have a look:
 * https://kotlinlang.slack.com/archives/G010KHY484C/p1706547846376149
 * Quote: "...we optimize for the case where hash code is unique, but it is not required for correctness"
 *
 * And here: https://jetbrains.slack.com/archives/C047QCXNLTX/p1706536405443729
 */
internal actual fun identityHashCode(instance: Any?): Int {
    if (instance == null) {
        return 0
    }
    return instance.hashCode()
}

@JsFun("""
(() => {
const memoizeIdentityHashCodeMap = new WeakMap();
let nextHash = 1;
return (obj) => {
    const res = memoizeIdentityHashCodeMap.get(obj);
    if (res === undefined) {
        const value = nextHash++;
        memoizeIdentityHashCodeMap.set(obj, value);
        return value;
    }
    return res;
}
})()"""
)
private external fun getIdentityHashCode(instance: JsAny): Int
