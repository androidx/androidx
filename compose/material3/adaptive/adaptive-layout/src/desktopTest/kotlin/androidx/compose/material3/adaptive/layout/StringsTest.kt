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

package androidx.compose.material3.adaptive.layout

import androidx.compose.material3.adaptive.l10n.translationFor
import androidx.compose.material3.adaptive.layout.internal.Strings
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test

class StringsTest {
    @Test
    fun allStringsHaveTranslations() {
        val companionClass = Strings.Companion::class.java
        val companionInstance = Strings::class.java.getDeclaredField("Companion").apply { isAccessible = true }.get(null)
        val constructor = Strings::class.java.getDeclaredConstructor(Int::class.javaPrimitiveType)
        constructor.isAccessible = true

        assertTrue(companionClass.declaredMethods.size > 0, "No methods found in Strings.Companion")

        for (method in companionClass.declaredMethods) {
            method.isAccessible = true

            if (method.returnType == Int::class.javaPrimitiveType && method.parameterCount == 0) {
                val name = method.name
                val value = method.invoke(companionInstance) as Int
                val obj = constructor.newInstance(value) as Strings
                // check only the default locale,
                // as the translations for other locales are generated the same way by the script
                assertNotNull(
                    translationFor("")!![obj],
                    "Strings.$name doesn't have translation. Please add it to build.gradle (search stringByResourceName) and rerun ./gradlew updateTranslations"
                )
            }
        }
    }
}