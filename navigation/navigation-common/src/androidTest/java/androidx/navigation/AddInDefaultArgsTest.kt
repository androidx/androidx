/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation

import android.os.Bundle
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private val stringArgument = "stringArg" to NavArgument.Builder()
    .setType(NavType.StringType)
    .setIsNullable(true)
    .build()
private val stringArgumentWithDefault = "stringArg" to NavArgument.Builder()
    .setType(NavType.StringType)
    .setDefaultValue("aaa")
    .build()
private val intArgument = "intArg" to NavArgument.Builder()
    .setType(NavType.IntType)
    .setDefaultValue(123)
    .build()

@SmallTest
@RunWith(Parameterized::class)
class AddInDefaultArgsTest(
    private val arguments: Map<String, NavArgument>,
    private val args: Bundle
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "arguments={0}, bundle={1}")
        fun data() = mutableListOf<Array<Any?>>().apply {
            arrayOf(
                // Test with an empty set of arguments
                mapOf(),
                // Test with an argument with no default value
                mapOf(stringArgument),
                // Test with arguments where only some have default values
                mapOf(stringArgument, intArgument),
                // Test with arguments that have default values
                mapOf(stringArgumentWithDefault, intArgument)
            ).forEach { arguments: Map<String, NavArgument> ->
                // Run with a null Bundle
                add(arrayOf(arguments, Bundle.EMPTY))
                // Run with a Bundle with a different argument
                add(arrayOf(arguments, Bundle().apply { putString("customArg", "custom") }))
                // Run with a Bundle with an overriding argument
                add(arrayOf(arguments, Bundle().apply { putString("stringArg", "bbb") }))
            }
        }
    }

    @Test
    fun addInDefaultArgs() {
        val destination = NoOpNavigator().createDestination()
        arguments.forEach { entry ->
            destination.addArgument(entry.key, entry.value)
        }

        val nullableArgs = if (args != Bundle.EMPTY) { args } else { null }
        val bundle = destination.addInDefaultArgs(nullableArgs)

        if (args == Bundle.EMPTY && arguments.isEmpty()) {
            assertWithMessage("Null args + null destination arguments should give a null Bundle")
                .that(bundle)
                .isNull()
        } else {
            assertThat(bundle)
                .isNotNull()
            // Assert that the args take precedence
            args.keySet()?.forEach { key ->
                assertThat(bundle!![key])
                    .isEqualTo(args[key])
            }
            // Assert that arguments with default values not in the args
            // are present in the Bundle
            arguments
                .filterKeys { !args.containsKey(it) }
                .filterValues { it.isDefaultValuePresent }
                .forEach { entry ->
                    assertThat(bundle!![entry.key])
                        .isEqualTo(entry.value.defaultValue)
                }
        }
    }
}
