/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.tiles.tooling

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

@Suppress("unused")
open class A {
    open fun aOpen(): String { return "aOpen" }

    fun aFinal(): String { return "aFinal" }

    protected open fun aOpenProtected(): String { return "aOpenProtected" }

    protected fun aFinalProtected(): String { return "aFinalProtected" }

    protected open fun aOpenProtected2(): String { return "aOpenProtected2" }
}

@Suppress("unused")
class B : A() {
    override fun aOpen(): String { return "bOpen" }

    override fun aOpenProtected(): String { return "bOpenProtected" }
}

class FindMethodTest {
    @Test
    fun testFindMethod() {
        val a = A()
        assertEquals("aOpen", A::class.java.findMethod("aOpen").invoke(a))
        assertEquals("aFinal", A::class.java.findMethod("aFinal").invoke(a))
        assertEquals("aOpenProtected", A::class.java.findMethod("aOpenProtected").invoke(a))
        assertEquals("aFinalProtected", A::class.java.findMethod("aFinalProtected").invoke(a))
        try {
            A::class.java.findMethod("noSuchMethod")
            fail()
        } catch (_: NoSuchMethodException) { }

        val b = B()
        assertEquals("bOpen", B::class.java.findMethod("aOpen").invoke(b))
        assertEquals("bOpenProtected", B::class.java.findMethod("aOpenProtected").invoke(b))
        assertEquals("aFinal", B::class.java.findMethod("aFinal").invoke(b))
        assertEquals("aOpenProtected2", B::class.java.findMethod("aOpenProtected2").invoke(b))
    }
}
