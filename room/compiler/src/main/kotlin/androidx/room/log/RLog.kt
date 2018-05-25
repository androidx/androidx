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

@file:Suppress("unused")

package androidx.room.log

import androidx.room.vo.Warning
import java.util.UnknownFormatConversionException
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.tools.Diagnostic
import javax.tools.Diagnostic.Kind.ERROR
import javax.tools.Diagnostic.Kind.NOTE
import javax.tools.Diagnostic.Kind.WARNING

class RLog(val messager: Messager, val suppressedWarnings: Set<Warning>,
           val defaultElement: Element?) {
    private fun String.safeFormat(vararg args: Any): String {
        try {
            return format(args)
        } catch (ex: UnknownFormatConversionException) {
            // the input string might be from random source in which case we rather print the
            // msg as is instead of crashing while reporting an error.
            return this
        }
    }

    fun d(element: Element, msg: String, vararg args: Any) {
        messager.printMessage(NOTE, msg.safeFormat(args), element)
    }

    fun d(msg: String, vararg args: Any) {
        messager.printMessage(NOTE, msg.safeFormat(args))
    }

    fun e(element: Element, msg: String, vararg args: Any) {
        messager.printMessage(ERROR, msg.safeFormat(args), element)
    }

    fun e(msg: String, vararg args: Any) {
        messager.printMessage(ERROR, msg.safeFormat(args), defaultElement)
    }

    fun w(warning: Warning, element: Element? = null, msg: String, vararg args: Any) {
        if (suppressedWarnings.contains(warning)) {
            return
        }
        messager.printMessage(WARNING, msg.safeFormat(args),
                element ?: defaultElement)
    }

    fun w(warning: Warning, msg: String, vararg args: Any) {
        if (suppressedWarnings.contains(warning)) {
            return
        }
        messager.printMessage(WARNING, msg.safeFormat(args), defaultElement)
    }

    interface Messager {
        fun printMessage(kind: Diagnostic.Kind, msg: String, element: Element? = null)
    }

    class ProcessingEnvMessager(val processingEnv: ProcessingEnvironment) : Messager {
        override fun printMessage(kind: Diagnostic.Kind, msg: String, element: Element?) {
            processingEnv.messager.printMessage(kind, msg, element)
        }
    }

    class CollectingMessager : Messager {
        private val messages = mutableMapOf<Diagnostic.Kind, MutableList<Pair<String, Element?>>> ()
        override fun printMessage(kind: Diagnostic.Kind, msg: String, element: Element?) {
            messages.getOrPut(kind, {
                arrayListOf()
            }).add(Pair(msg, element))
        }

        fun hasErrors() = messages.containsKey(Diagnostic.Kind.ERROR)

        fun writeTo(env: ProcessingEnvironment) {
            messages.forEach { pair ->
                val kind = pair.key
                pair.value.forEach {
                    env.messager.printMessage(kind, it.first, it.second)
                }
            }
        }
    }
}
