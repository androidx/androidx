/*
 * Copyright (C) 2021 The Android Open Source Project
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
package androidx.constraintlayout.core.cl;

import static org.junit.Assert.assertNotNull;

import androidx.constraintlayout.core.parser.CLParsingException;
import androidx.constraintlayout.core.state.ConstraintReference;
import androidx.constraintlayout.core.state.ConstraintSetParser;
import androidx.constraintlayout.core.state.State;
import androidx.constraintlayout.core.state.Transition;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;

import org.junit.Test;

public class ConstraintSetParserTest {

    @Test
    public void testSimpleConstraintSet1() throws CLParsingException {
        String jsonString = "     n"
                + "                  start: {\n"
                + "                    id1: {\n"
                + "                      width: 40, height: 40,\n"
                + "                      start:  ['parent', 'start' , 16],\n"
                + "                      bottom: ['parent', 'bottom', 16]\n"
                + "                    }\n"
                + "                  },\n";

        State state = new State();
        System.out.println(">>>>>> testSimpleConstraintSet1 <<<<<<");
        state.setDpToPixel(dp -> dp);
        ConstraintSetParser.LayoutVariables vars = new ConstraintSetParser.LayoutVariables();
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(1000, 1000);
        ConstraintWidget idWidget = new ConstraintWidget();
        idWidget.stringId = "id1";
        ConstraintSetParser.parseJSON(jsonString, state, vars);
        root.add(idWidget);

        ConstraintReference id1 = state.constraints("id1");
        assertNotNull(id1);
        state.reset();

        System.out.println(">>>>>> " + root.stringId);
        for (ConstraintWidget child : root.getChildren()) {
            System.out.println(">>>>>>  " + child.stringId);
        }
        state.apply(root);
        System.out.print("children: ");
        for (ConstraintWidget child : root.getChildren()) {
            System.out.print(" " + child.stringId);
        }
        System.out.println();
        System.out.println(">>>>>> testSimpleConstraintSet1 <<<<<<");

    }

    @Test
    public void testSimpleConstraintSet2() {
        String jsonString = "    {\n"
                + "                Header: { exportAs: 'mtest01'},\n"
                + "                \n"
                + "                ConstraintSets: {\n"
                + "                  start: {\n"
                + "                    id1: {\n"
                + "                      width: 40, height: 40,\n"
                + "                      start:  ['parent', 'start' , 16],\n"
                + "                      bottom: ['parent', 'bottom', 16]\n"
                + "                    }\n"
                + "                  },\n"
                + "                  \n"
                + "                  end: {\n"
                + "                    id1: {\n"
                + "                      width: 40, height: 40,\n"
                + "                      end: ['parent', 'end', 16],\n"
                + "                      top: ['parent', 'top', 16]\n"
                + "                    }\n"
                + "                  }\n"
                + "                },\n"
                + "                \n"
                + "                Transitions: {\n"
                + "                  default: {\n"
                + "                    from: 'start',   to: 'end',\n"
                + "                  }\n"
                + "                }\n"
                + "            }";

        Transition transition = new Transition();
        ConstraintSetParser.parseJSON(jsonString, transition, 0);
    }
}

