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

package com.android.tools.build.jetifier.processor

import java.io.File

/**
 * Result of the transformation done by the [Processor].
 *
 * @param librariesMap map from original library to its remapped version (created by Jetifier).
 * The value can be null in case the file was not modified and 'copyUnmodifiedLibsAlso' was set to
 * false.
 * @param numberOfLibsModified total number of libraries that were modified by Jetifier.
 */
data class TransformationResult(val librariesMap: Map<File, File?>, val numberOfLibsModified: Int)