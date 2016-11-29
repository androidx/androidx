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

package com.android.support.room.log

import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.tools.Diagnostic.Kind.ERROR
import javax.tools.Diagnostic.Kind.NOTE

class RLog(val processingEnv : ProcessingEnvironment) {
    fun d(element : Element, msg : String, vararg args : Any) {
        processingEnv.messager.printMessage(NOTE, msg.format(args), element)
    }

    fun d(msg : String, vararg args : Any) {
        processingEnv.messager.printMessage(NOTE, msg.format(args))
    }

    fun e(element : Element, msg : String, vararg args : Any) {
        processingEnv.messager.printMessage(ERROR, msg.format(args), element)
    }
}