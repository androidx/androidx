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

package androidx.compose.runtime.internal

import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.Composer
import androidx.compose.runtime.Stable

@ComposeCompilerApi
@Stable
actual interface ComposableLambda :
    Function2<Composer, Int, Any?>,
    Function3<Any?, Composer, Int, Any?>,
    Function4<Any?, Any?, Composer, Int, Any?>,
    Function5<Any?, Any?, Any?, Composer, Int, Any?>,
    Function6<Any?, Any?, Any?, Any?, Composer, Int, Any?>,
    Function7<Any?, Any?, Any?, Any?, Any?, Composer, Int, Any?>,
    Function8<Any?, Any?, Any?, Any?, Any?, Any?, Composer, Int, Any?>,
    Function9<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Composer, Int, Any?>,
    Function10<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Composer, Int, Any?>,
    Function11<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Composer, Int, Any?>,
    Function13<
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Composer,
        Int,
        Int,
        Any?
    >,
    Function14<
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Composer,
        Int,
        Int,
        Any?
    >,
    Function15<
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Composer,
        Int,
        Int,
        Any?
    >,
    Function16<
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Composer,
        Int,
        Int,
        Any?
    >,
    Function17<
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Composer,
        Int,
        Int,
        Any?
    >,
    Function18<
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Composer,
        Int,
        Int,
        Any?
    >,
    Function19<
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Composer,
        Int,
        Int,
        Any?
    >,
    Function20<
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Composer,
        Int,
        Int,
        Any?
    >,
    Function21<
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Any?,
        Composer,
        Int,
        Int,
        Any?
    >
