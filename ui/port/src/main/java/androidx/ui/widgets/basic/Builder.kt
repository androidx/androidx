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

package androidx.ui.widgets.basic

import androidx.ui.foundation.Key
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.StatelessWidget
import androidx.ui.widgets.framework.Widget
import androidx.ui.widgets.framework.WidgetBuilder

/**
 * A platonic widget that calls a closure to obtain its child widget.
 *
 * Creates a widget that delegates its build to a callback.
 *
 * The [builder] argument must not be null.
 */
class Builder(val builder: WidgetBuilder, key: Key? = null) : StatelessWidget(key = key) {
    override fun build(context: BuildContext): Widget = build(context)
}