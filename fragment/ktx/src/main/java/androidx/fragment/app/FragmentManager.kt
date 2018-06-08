/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.fragment.app

/**
 * Run [body] in a [FragmentTransaction] which is automatically committed if it completes without
 * exception.
 *
 * One of four commit functions will be used based on the values of `now` and `allowStateLoss`:
 *
 * | `now` | `allowStateLoss` | Method                         |
 * | ----- | ---------------- | ------------------------------ |
 * | false | false            | `commit()`                     |
 * | false | true             | `commitAllowingStateLoss()`    |
 * | true  | false            | `commitNow()`                  |
 * | true  | true             | `commitNowAllowingStateLoss()` |
 */
inline fun FragmentManager.transaction(
    now: Boolean = false,
    allowStateLoss: Boolean = false,
    body: FragmentTransaction.() -> Unit
) {
    val transaction = beginTransaction()
    transaction.body()
    if (now) {
        if (allowStateLoss) {
            transaction.commitNowAllowingStateLoss()
        } else {
            transaction.commitNow()
        }
    } else {
        if (allowStateLoss) {
            transaction.commitAllowingStateLoss()
        } else {
            transaction.commit()
        }
    }
}
