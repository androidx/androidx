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
@file:JvmName("TestSplitInfo")

package androidx.window.testing.embedding

import android.os.Binder
import androidx.window.embedding.ActivityStack
import androidx.window.embedding.SplitAttributes
import androidx.window.embedding.SplitInfo

/**
 * Creates a [SplitInfo] instance for testing.
 *
 * It is suggested to construct [primaryActivityStack] and [secondActivityStack] by
 * [TestActivityStack], and [splitAttributes] by [SplitAttributes.Builder] APIs
 *
 * @param primaryActivityStack The primary [ActivityStack] with an empty [ActivityStack] as the
 *     default value.
 * @param secondActivityStack The secondary [ActivityStack] with an empty [ActivityStack] as the
 *     default value.
 * @param splitAttributes The current [SplitAttributes] for this test split, which defaults to
 *     split equally with layout direction [SplitAttributes.LayoutDirection.LOCALE].
 * @return A [SplitInfo] instance for testing
 */
@Suppress("FunctionName")
@JvmName("createTestSplitInfo")
@JvmOverloads
fun TestSplitInfo(
    primaryActivityStack: ActivityStack = TestActivityStack(),
    secondActivityStack: ActivityStack = TestActivityStack(),
    splitAttributes: SplitAttributes = SplitAttributes.Builder().build(),
): SplitInfo = SplitInfo(
    primaryActivityStack,
    secondActivityStack,
    splitAttributes,
    TEST_SPLIT_INFO_TOKEN
)

private val TEST_SPLIT_INFO_TOKEN = Binder()