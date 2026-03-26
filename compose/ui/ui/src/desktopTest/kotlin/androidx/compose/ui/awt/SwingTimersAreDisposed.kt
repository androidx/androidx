/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.awt

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.ImageComposeScene
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.DelayQueue
import java.util.concurrent.TimeUnit
import java.util.function.Predicate
import javax.swing.Timer
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import org.jetbrains.annotations.NonNls

class SwingTimersAreDisposed {

    @Test
    fun test() {
        runBlocking(Dispatchers.Swing) {
            val composeScene = ImageComposeScene(600, 600)
            try {
                composeScene.setContent {
                    BasicText("")
                }
                composeScene.render(0L)
            } finally {
                composeScene.close()
            }
        }
    }

    @AfterTest
    fun checkJavaSwingTimersAreDisposed() {
        val timerQueueClass = Class.forName("javax.swing.TimerQueue")
        val sharedInstance = timerQueueClass.getMethod("sharedInstance")
        sharedInstance.isAccessible = true
        val timerQueue = sharedInstance.invoke(null)
        val delayQueue = getField(timerQueueClass, timerQueue, DelayQueue::class.java, "queue")
        val timer = delayQueue?.peek()
        if (timer != null) {
            val delay = timer.getDelay(TimeUnit.MILLISECONDS)
            var text = "(delayed for ${delay}ms)"
            val getTimer = getDeclaredMethod(timer.javaClass, "getTimer")!!
            val swingTimer = getTimer.invoke(timer) as Timer
            text = "Timer (listeners: ${listOf(*swingTimer.actionListeners)}) $text"
            try {
                throw AssertionError("Not disposed javax.swing.Timer: $text; queue: $timerQueue")
            } finally {
                swingTimer.stop()
            }
        }
    }
}

private fun processInterfaces(
    interfaces: Array<Class<*>>,
    visited: MutableSet<in Class<*>?>,
    checker: Predicate<in Field?>,
): Field? {
    for (anInterface in interfaces) {
        if (!visited.add(anInterface)) {
            continue
        }

        for (field in anInterface.declaredFields) {
            if (checker.test(field)) {
                field.isAccessible = true
                return field
            }
        }

        val field = processInterfaces(anInterface.interfaces, visited, checker)
        if (field != null) {
            return field
        }
    }
    return null
}

private fun makeAccessible(method: Method): Method {
    method.isAccessible = true
    return method
}

fun getDeclaredMethod(aClass: Class<*>, name: String, vararg parameters: Class<*>): Method? {
    return try {
        makeAccessible(aClass.getDeclaredMethod(name, *parameters))
    } catch (_: NoSuchMethodException) {
        null
    }
}

private fun findFieldInHierarchy(
    rootClass: Class<*>,
    checker: Predicate<in Field?>,
): Field? {
    var aClass: Class<*>? = rootClass
    while (aClass != null) {
        for (field in aClass.declaredFields) {
            if (checker.test(field)) {
                field.isAccessible = true
                return field
            }
        }
        aClass = aClass.superclass
    }

    // ok, let's check interfaces
    return processInterfaces(rootClass.interfaces, HashSet(), checker)
}


private fun findAssignableField(clazz: Class<*>, fieldType: Class<*>?, fieldName: @NonNls String): Field {
    val result = findFieldInHierarchy(clazz, Predicate { field: Field? -> fieldName == field!!.name && (fieldType == null || fieldType.isAssignableFrom(field.type)) })
    if (result != null) {
        return result
    }
    throw NoSuchFieldException("Class: $clazz fieldName: $fieldName fieldType: $fieldType")
}

private fun <T> getField(objectClass: Class<*>, `object`: Any?, fieldType: Class<T>?, fieldName: String): T? {
    @Suppress("UNCHECKED_CAST")
    return findAssignableField(objectClass, fieldType as Class<*>?, fieldName).get(`object`) as T?
}
