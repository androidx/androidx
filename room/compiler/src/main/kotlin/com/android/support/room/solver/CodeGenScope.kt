/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.room.solver

import com.google.common.annotations.VisibleForTesting
import com.squareup.javapoet.CodeBlock

/**
 * Defines a code generation scope where we can provide temporary variables, global variables etc
 */
class CodeGenScope {
    private var tmpVarIndex = 0
    private var builder : CodeBlock.Builder? = null
    companion object {
        const val TMP_VAR_PREFIX = "_tmp"
        @VisibleForTesting
        fun _tmpVar(index:Int) = "${TMP_VAR_PREFIX}_$index"
    }

    fun builder() : CodeBlock.Builder {
        if (builder == null) {
            builder = CodeBlock.builder()
        }
        return builder!!
    }

    fun getTmpVar() : String {
        return _tmpVar(tmpVarIndex ++)
    }

    fun generate() = builder().build().toString()
}