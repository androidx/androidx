# Copyright (C) 2020 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Some methods in androidx.window.extensions are accessed through reflection and need to be kept.
# Failure to do so can cause bugs such as b/157286362. This could be overly broad too and should
# ideally be trimmed down to only the classes/methods that actually need to be kept. This should
# be tracked in b/165268619.
-keep class androidx.window.extensions.** { *; }
-dontwarn androidx.window.extensions.**
-keep class androidx.window.extensions.embedding.** { *; }
-dontwarn androidx.window.extensions.embedding.**

# Keep the whole library for now since there is a crash with a missing method.
# TODO(b/165268619) Make a narrow rule
-keep class androidx.window.** { *; }

# We also neep to keep sidecar.** for the same reason.
-keep class androidx.window.sidecar.** { *; }
-dontwarn androidx.window.sidecar.**

