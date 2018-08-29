/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.navigation.safe.args.generator.ext

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile

const val L = "\$L"
const val N = "\$N"
const val T = "\$T"
const val S = "\$S"
const val BEGIN_STMT = "\$["
const val END_STMT = "\$]"

fun JavaFile.toClassName(): ClassName = ClassName.get(this.packageName, this.typeSpec.name)