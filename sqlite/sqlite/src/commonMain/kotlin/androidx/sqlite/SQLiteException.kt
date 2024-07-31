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

package androidx.sqlite

import androidx.annotation.RestrictTo

/**
 * An exception that indicates that something has gone wrong and a error code was produced.
 *
 * See [Result and Error codes](https://www.sqlite.org/rescode.html)
 */
public expect class SQLiteException
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(message: String) : RuntimeException
