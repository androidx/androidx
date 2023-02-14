/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.constraintlayout.core.scout;

import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;
import androidx.constraintlayout.core.widgets.WidgetContainer;

import java.util.ArrayList;

/**
 * Main entry for the Scout Inference engine.
 * All external access should be through this class
 * TODO support Stash / merge constraints table etc.
 */
public class Scout {


    /**
     * Given a collection of widgets evaluates probability of a connection
     * and makes connections
     *
     * @param list collection of widgets to connect
     */

    /**
     * Recursive decent of widget tree inferring constraints on ConstraintWidgetContainer
     */
    public static void inferConstraints(WidgetContainer base) {
        if (base == null) {
            return;
        }
        if (base instanceof ConstraintWidgetContainer
                && ((ConstraintWidgetContainer) base).handlesInternalConstraints()) {
            return;
        }
        int preX = base.getX();
        int preY = base.getY();
        base.setX(0);
        base.setY(0);
        for (ConstraintWidget constraintWidget : base.getChildren()) {
            if (constraintWidget instanceof ConstraintWidgetContainer) {
                ConstraintWidgetContainer container = (ConstraintWidgetContainer) constraintWidget;
                if (!container.getChildren().isEmpty()) {
                    inferConstraints(container);
                }
            }
        }

        ArrayList<ConstraintWidget> list = new ArrayList<>(base.getChildren());
        list.add(0, base);

        ConstraintWidget[] widgets = list.toArray(new ConstraintWidget[list.size()]);
        ScoutWidget.computeConstraints(ScoutWidget.create(widgets));
        base.setX(preX);
        base.setY(preY);
    }

}
