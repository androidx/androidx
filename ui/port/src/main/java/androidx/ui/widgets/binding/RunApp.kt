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

package androidx.ui.widgets.binding

import androidx.ui.widgets.framework.Widget

/**
 * Inflate the given widget and attach it to the screen.
 *
 * The widget is given constraints during layout that force it to fill the
 * entire screen. If you wish to align your widget to one side of the screen
 * (e.g., the top), consider using the [Align] widget. If you wish to center
 * your widget, you can also use the [Center] widget
 *
 * Calling [runApp] again will detach the previous root widget from the screen
 * and attach the given widget in its place. The new widget tree is compared
 * against the previous widget tree and any differences are applied to the
 * underlying render tree, similar to what happens when a [StatefulWidget]
 * rebuilds after calling [State.setState].
 *
 * Initializes the binding using [WidgetsFlutterBinding] if necessary.
 *
 * See also:
 *
 * * [WidgetsBinding.attachRootWidget], which creates the root widget for the
 *   widget hierarchy.
 * * [RenderObjectToWidgetAdapter.attachToRenderTree], which creates the root
 *   element for the element hierarchy.
 * * [WidgetsBinding.handleBeginFrame], which pumps the widget pipeline to
 *   ensure the widget, element, and render trees are all built.
 */
fun runApp(app: Widget, binding: WidgetsBinding) {
    with(binding) {
        attachRootWidget(app)
        scheduleWarmUpFrame()
    }
}