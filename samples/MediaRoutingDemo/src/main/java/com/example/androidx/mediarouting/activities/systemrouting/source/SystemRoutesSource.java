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

package com.example.androidx.mediarouting.activities.systemrouting.source;

import androidx.annotation.NonNull;

import com.example.androidx.mediarouting.activities.systemrouting.SystemRouteItem;

import java.util.List;

/**
 * Abstracts different route sources.
 */
public interface SystemRoutesSource {

    /**
     * Fetches system routes and returns a list of {@link SystemRouteItem}.
     */
    @NonNull
    List<SystemRouteItem> fetchRoutes();

}
