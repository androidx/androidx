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

package androidx.kruth

import com.google.common.base.Optional
import com.google.common.collect.Multimap
import com.google.common.collect.Multiset
import java.math.BigDecimal

fun assertThat(actual: Class<*>): ClassSubject =
    ClassSubject(actual)

fun <T : Any> assertThat(actual: Optional<T>?): GuavaOptionalSubject<T> =
    GuavaOptionalSubject(actual)

fun assertThat(actual: BigDecimal): BigDecimalSubject =
    BigDecimalSubject(actual)

fun <T> assertThat(actual: Multiset<T>): MultisetSubject<T> =
    MultisetSubject(actual = actual)

fun <K, V> assertThat(actual: Multimap<K, V>): MultimapSubject<K, V> =
    MultimapSubject(actual = actual)
