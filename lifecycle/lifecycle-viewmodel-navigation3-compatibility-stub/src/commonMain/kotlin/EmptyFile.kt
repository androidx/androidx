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

// We prefer to have no source code here, but a module can't be empty.
// We use this module to publish a dumb klib to be provided to the compilation of user projects.
// It's needed because Kotlin tries to resolve the dependencies listed in klib manifest.
// There is an intention to drop this behavior: https://youtrack.jetbrains.com/issue/KT-61096
// The actual klib is published at androidx maven coordinates in Google maven.
// This module depends on the actual klib, so the module API will be available transitively.