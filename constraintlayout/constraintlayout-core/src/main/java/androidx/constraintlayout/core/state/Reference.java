/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.constraintlayout.core.state;

import androidx.constraintlayout.core.state.helpers.Facade;
import androidx.constraintlayout.core.widgets.ConstraintWidget;

public interface Reference {
    // @TODO: add description
    ConstraintWidget getConstraintWidget();

    // @TODO: add description
    void setConstraintWidget(ConstraintWidget widget);

    // @TODO: add description
    void setKey(Object key);

    // @TODO: add description
    Object getKey();

    // @TODO: add description
    void apply();

    // @TODO: add description
    Facade getFacade();
}
