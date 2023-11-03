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

package androidx.window.reflection

/**
 * Constants for WindowExtensions
 */
internal object WindowExtensionsConstants {
    /**
     * Constant name for package [androidx.window.extensions]
     */
    private const val WINDOW_EXTENSIONS_PACKAGE_NAME = "androidx.window.extensions"

    /**
     * Constant name for class [androidx.window.extensions.WindowExtensionsProvider] used
     * for reflection
     */
    internal const val WINDOW_EXTENSIONS_PROVIDER_CLASS =
        "$WINDOW_EXTENSIONS_PACKAGE_NAME.WindowExtensionsProvider"

    /**
     * Constant name for class [androidx.window.extensions.WindowExtensions] used for reflection
     */
    internal const val WINDOW_EXTENSIONS_CLASS =
        "$WINDOW_EXTENSIONS_PACKAGE_NAME.WindowExtensions"

    /**
     * Constant name for class [androidx.window.extensions.layout.FoldingFeature]
     * used for reflection
     */
    internal const val FOLDING_FEATURE_CLASS =
        "$WINDOW_EXTENSIONS_PACKAGE_NAME.layout.FoldingFeature"

    /**
     * Constant name for class [androidx.window.extensions.layout.WindowLayoutComponent]
     * used for reflection
     */
    internal const val WINDOW_LAYOUT_COMPONENT_CLASS =
        "$WINDOW_EXTENSIONS_PACKAGE_NAME.layout.WindowLayoutComponent"

    /**
     * Constant name for class [androidx.window.extensions.area.WindowAreaComponent]
     * used for reflection
     */
    internal const val WINDOW_AREA_COMPONENT_CLASS =
        "$WINDOW_EXTENSIONS_PACKAGE_NAME.area.WindowAreaComponent"

    /**
     * Constant name for class [androidx.window.extensions.area.ExtensionWindowAreaStatus]
     * used for reflection
     */
    internal const val EXTENSION_WINDOW_AREA_STATUS_CLASS =
        "$WINDOW_EXTENSIONS_PACKAGE_NAME.area.ExtensionWindowAreaStatus"

    /**
     * Constant name for class [androidx.window.extensions.area.ExtensionWindowAreaPresentation]
     * used for reflection
     */
    internal const val EXTENSION_WINDOW_AREA_PRESENTATION_CLASS =
        "$WINDOW_EXTENSIONS_PACKAGE_NAME.area.ExtensionWindowAreaPresentation"

    /**
     * Constant name for class [androidx.window.extensions.embedding.ActivityEmbeddingComponent]
     * used for reflection
     */
    internal const val ACTIVITY_EMBEDDING_COMPONENT_CLASS =
        "$WINDOW_EXTENSIONS_PACKAGE_NAME.embedding.ActivityEmbeddingComponent"

    /**
     * Constant name for class [androidx.window.extensions.core.util.function]
     * used for reflection
     */
    internal const val WINDOW_CONSUMER =
        "$WINDOW_EXTENSIONS_PACKAGE_NAME.core.util.function.Consumer"

    /**
     * Constant name for class [java.util.function.Consumer]
     * used for reflection
     */
    internal const val JAVA_CONSUMER = "java.util.function.Consumer"
}
