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

package com.android.tools.deploy.liveedit;

import java.util.Map;

/**
 * Interface from Live Edit
 */
public interface SourceLocationAware {

    /**
     * Get the source location from a Live Edit lambda.
     *
     * @return a Map with:
     * - lambda: The original lambda class name,
     * - file: The file containing the lambda,
     * - startLine: The starting line as an int,
     * - endLine: The ending line as an int.
     */
    @SuppressWarnings("unused")
    Map<String, Object> getSourceLocationInfo();
}
