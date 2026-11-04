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

package androidx.a2ui.compose.runtime

/**
 * Catalog of components and functions that a UI layer provides to the A2UI Compose runtime.
 *
 * A UI layer is a library that renders A2UI components on top of this runtime with a specific UI
 * toolkit. App developers get catalogs from a UI layer and pass them to [A2uiMessageProcessor];
 * they do not implement this interface.
 *
 * **Note for UI layer implementors:** Implementations must also implement
 * [androidx.a2ui.engine.catalog.A2uiCoreCatalog] from `androidx.a2ui:a2ui-engine`, which defines
 * the catalog's components, functions, and theme schema. The runtime keeps the engine types out of
 * its public API, so [A2uiMessageProcessor] checks this requirement at runtime.
 */
public interface A2uiRuntimeCatalog
