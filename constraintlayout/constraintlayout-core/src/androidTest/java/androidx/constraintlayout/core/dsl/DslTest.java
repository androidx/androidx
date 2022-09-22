/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.core.dsl;

import static org.junit.Assert.assertEquals;

import androidx.constraintlayout.core.parser.CLParser;
import androidx.constraintlayout.core.parser.CLParsingException;
import androidx.constraintlayout.core.state.CorePixelDp;
import androidx.constraintlayout.core.state.TransitionParser;

import org.junit.Test;

public class DslTest {
    //  test structures
    static CorePixelDp dipToDip = new CorePixelDp() {

        @Override
        public float toPixels(float dp) {
            return dp;
        }
    };
    static androidx.constraintlayout.core.state.Transition transitionState =
            new androidx.constraintlayout.core.state.Transition();


    @Test
    public void testTransition01() {
        MotionScene motionScene = new MotionScene();
        motionScene.addTransition(new Transition("start", "end"));
        System.out.println(motionScene);
        String exp = "{\n"
                + "Transitions:{\n"
                + "default:{\n"
                + "from:'start',\n"
                + "to:'end',\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());
    }

    @Test
    public void testTransition02() {
        MotionScene motionScene = new MotionScene();
        motionScene.addTransition(new Transition("expand", "start", "end"));
        System.out.println(motionScene);
        String exp = "{\n"
                + "Transitions:{\n"
                + "expand:{\n"
                + "from:'start',\n"
                + "to:'end',\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());
    }

    @Test
    public void testOnSwipe01() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        Transition transition = new Transition("expand", "start", "end");
        transition.setOnSwipe(new OnSwipe());
        motionScene.addTransition(transition);

        System.out.println(motionScene);
        String exp = "{\n"
                + "Transitions:{\n"
                + "expand:{\n"
                + "from:'start',\n"
                + "to:'end',\n"
                + "OnSwipe:{\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        TransitionParser.parse(CLParser.parse(transition.toString()), transitionState, dipToDip);
    }

    @Test
    public void testOnSwipe02() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        Transition transition = new Transition("expand", "start", "end");
        transition.setOnSwipe(new OnSwipe("button", OnSwipe.Side.RIGHT, OnSwipe.Drag.RIGHT));
        motionScene.addTransition(transition);

        System.out.println(motionScene);
        String exp = "{\n"
                + "Transitions:{\n"
                + "expand:{\n"
                + "from:'start',\n"
                + "to:'end',\n"
                + "OnSwipe:{\n"
                + "anchor:'button',\n"
                + "direction:'right',\n"
                + "side:'right',\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";

        assertEquals(exp, motionScene.toString());

        TransitionParser.parse(CLParser.parse(transition.toString()), transitionState, dipToDip);
    }

    @Test
    public void testOnKeyPosition01() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        Transition transition = new Transition("expand", "start", "end");
        transition.setOnSwipe(new OnSwipe("button", OnSwipe.Side.RIGHT, OnSwipe.Drag.RIGHT));
        KeyPosition kp = new KeyPosition("button", 32);
        kp.setPercentX(0.5f);
        transition.setKeyFrames(kp);
        motionScene.addTransition(transition);

        System.out.println(motionScene);
        String exp = "{\n"
                + "Transitions:{\n"
                + "expand:{\n"
                + "from:'start',\n"
                + "to:'end',\n"
                + "OnSwipe:{\n"
                + "anchor:'button',\n"
                + "direction:'right',\n"
                + "side:'right',\n"
                + "},\n"
                + "keyFrames:{\n"
                + "KeyPositions:{\n"
                + "target:'button',\n"
                + "frame:32,\n"
                + "type:'CARTESIAN',\n"
                + "percentX:0.5,\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  Transitions: {\n"
                + "    expand: {\n"
                + "      from: 'start',\n"
                + "      to: 'end',\n"
                + "      OnSwipe: { anchor: 'button', direction: 'right', side: 'right' },\n"
                + "      keyFrames: {\n"
                + "        KeyPositions: {\n"
                + "          target:           'button',\n"
                + "          frame:           32,\n"
                + "          type:           'CARTESIAN',\n"
                + "          percentX:           0.5\n"
                + "        }\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(transition.toString()), transitionState, dipToDip);
    }

    @Test
    public void testOnKeyPositions0() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        Transition transition = new Transition("expand", "start", "end");
        transition.setOnSwipe(new OnSwipe("button", OnSwipe.Side.RIGHT, OnSwipe.Drag.RIGHT));
        KeyPositions kp = new KeyPositions(2,"button1","button2");

        transition.setKeyFrames(kp);
        motionScene.addTransition(transition);

        System.out.println(motionScene);
        String exp = "{\n"
                + "Transitions:{\n"
                + "expand:{\n"
                + "from:'start',\n"
                + "to:'end',\n"
                + "OnSwipe:{\n"
                + "anchor:'button',\n"
                + "direction:'right',\n"
                + "side:'right',\n"
                + "},\n"
                + "keyFrames:{\n"
                + "KeyPositions:{\n"
                + "target:['button1','button2'],\n"
                + "frame:[33, 66],\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  Transitions: {\n"
                + "    expand: {\n"
                + "      from: 'start',\n"
                + "      to: 'end',\n"
                + "      OnSwipe: { anchor: 'button', direction: 'right', side: 'right' },\n"
                + "      keyFrames: { KeyPositions: { target: ['button1', 'button2'], frame: [33,"
                + " 66] } }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(transition.toString()), transitionState, dipToDip);
    }

    @Test
    public void testAnchor01() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        ConstraintSet constraintSet = new ConstraintSet("start");
        Constraint constraint = new Constraint("a");
        Constraint constraint2 = new Constraint("b");
        constraintSet.add(constraint);
        constraint.linkToLeft(constraint2.getLeft());
        motionScene.addConstraintSet(constraintSet);
        System.out.println(motionScene);
        String exp = "{\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "a:{\n"
                + "left:['b','left'],\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      a: { left: ['b', 'left'] }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }

    @Test
    public void testAnchor02() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        ConstraintSet constraintSet = new ConstraintSet("start");
        Constraint constraint = new Constraint("a");
        Constraint constraint2 = new Constraint("b");
        constraintSet.add(constraint);
        constraint.linkToLeft(constraint2.getLeft(), 15);
        motionScene.addConstraintSet(constraintSet);
        System.out.println(motionScene);
        String exp = "{\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "a:{\n"
                + "left:['b','left',15],\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      a: { left: ['b', 'left', 15] }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }

    @Test
    public void testAnchor03() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        ConstraintSet constraintSet = new ConstraintSet("start");
        Constraint constraint = new Constraint("a");
        Constraint constraint2 = new Constraint("b");
        Constraint constraint3 = new Constraint("c");
        Constraint constraint4 = new Constraint("d");
        constraintSet.add(constraint);
        constraint.linkToLeft(constraint2.getRight(), 5, 10);
        constraint.linkToTop(constraint3.getBottom(),0, 15);
        constraint.linkToBaseline(constraint4.getBaseline());
        motionScene.addConstraintSet(constraintSet);
        System.out.println(motionScene);
        String exp = "{\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "a:{\n"
                + "left:['b','right',5,10],\n"
                + "top:['c','bottom',0,15],\n"
                + "baseline:['d','baseline'],\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      a: {\n"
                + "        left: ['b', 'right', 5, 10],\n"
                + "        top: ['c', 'bottom', 0, 15],\n"
                + "        baseline: ['d', 'baseline']\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }

    @Test
    public void testConstraint01() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        ConstraintSet constraintSet = new ConstraintSet("start");
        Constraint constraint = new Constraint("a");
        constraint.setHeight(0);
        constraint.setWidth(40);
        constraint.setDimensionRatio("1:1");
        constraintSet.add(constraint);
        motionScene.addConstraintSet(constraintSet);
        System.out.println(motionScene);
        String exp = "{\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "a:{\n"
                + "width:40,\n"
                + "height:0,\n"
                + "dimensionRatio:'1:1',\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      a: { width: 40, height: 0, dimensionRatio: '1:1' }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }

    @Test
    public void testConstraint02() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        ConstraintSet constraintSet = new ConstraintSet("start");
        Constraint constraint = new Constraint("a");
        constraint.setWidthPercent(50);
        constraint.setHeightPercent(60);
        constraint.setHorizontalBias(0.3f);
        constraint.setVerticalBias(0.2f);
        constraint.setCircleConstraint("parent");
        constraint.setCircleRadius(10);
        constraint.setVerticalWeight(2.1f);
        constraint.setHorizontalWeight(1f);
        constraintSet.add(constraint);
        motionScene.addConstraintSet(constraintSet);
        System.out.println(motionScene);
        String exp = "{\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "a:{\n"
                + "horizontalBias:0.3,\n"
                + "verticalBias:0.2,\n"
                + "circular:['parent',0,10],\n"
                + "verticalWeight:2.1,\n"
                + "horizontalWeight:1.0,\n"
                + "width:'50%',\n"
                + "height:'60%',\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      a: {\n"
                + "        horizontalBias: 0.3,\n"
                + "        verticalBias: 0.2,\n"
                + "        circular: ['parent', 0, 10],\n"
                + "        verticalWeight: 2.1,\n"
                + "        horizontalWeight: 1,\n"
                + "        width: '50%',\n"
                + "        height: '60%'\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }

    @Test
    public void testConstraint03() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        ConstraintSet constraintSet = new ConstraintSet("start");
        Constraint constraint = new Constraint("a");
        constraint.setWidthDefault(Constraint.Behaviour.WRAP);
        constraint.setHeightDefault(Constraint.Behaviour.SPREAD);
        constraint.setWidthMax(30);
        constraint.setCircleConstraint("parent");
        constraint.setCircleAngle(10);
        constraint.setReferenceIds(new String[] {"a", "b", "c"});
        Constraint constraint2 = new Constraint("b");
        constraint2.setHorizontalChainStyle(Constraint.ChainMode.SPREAD_INSIDE);
        constraint2.setVerticalChainStyle(Constraint.ChainMode.PACKED);
        constraint2.setConstrainedWidth(true);
        constraint2.setConstrainedHeight(true);
        constraintSet.add(constraint);
        constraintSet.add(constraint2);
        motionScene.addConstraintSet(constraintSet);
        System.out.println(motionScene);
        String exp = "{\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "a:{\n"
                + "circular:['parent',10.0],\n"
                + "width:{value:'wrap',max:30},\n"
                + "height:'spread',\n"
                + "referenceIds:['a','b','c'],\n"
                + "},\n"
                + "b:{\n"
                + "horizontalChainStyle:'spread_inside',\n"
                + "verticalChainStyle:'packed',\n"
                + "constrainedWidth:true,\n"
                + "constrainedHeight:true,\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      a: {\n"
                + "        circular: ['parent', 10],\n"
                + "        width: { value: 'wrap', max: 30 },\n"
                + "        height: 'spread',\n"
                + "        referenceIds: ['a', 'b', 'c']\n"
                + "      },\n"
                + "      b: {\n"
                + "        horizontalChainStyle: 'spread_inside',\n"
                + "        verticalChainStyle: 'packed',\n"
                + "        constrainedWidth: true,\n"
                + "        constrainedHeight: true\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }

    @Test
    public void testConstraintSet01() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        ConstraintSet constraintSet1 = new ConstraintSet("start");
        ConstraintSet constraintSet2 = new ConstraintSet("end");
        Constraint constraint1 = new Constraint("a");
        Constraint constraint2 = new Constraint("b");
        Constraint constraint3 = new Constraint("c");
        constraintSet1.add(constraint1);
        constraintSet1.add(constraint2);
        constraintSet2.add(constraint3);
        motionScene.addConstraintSet(constraintSet1);
        motionScene.addConstraintSet(constraintSet2);
        System.out.println(motionScene);
        String exp = "{\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "a:{\n"
                + "},\n"
                + "b:{\n"
                + "},\n"
                + "},\n"
                + "end:{\n"
                + "c:{\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      a: {  },\n"
                + "      b: {  }\n"
                + "    },\n"
                + "    end: {\n"
                + "      c: {  }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }

    @Test
    public void testConstraintSet02() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        ConstraintSet constraintSet1 = new ConstraintSet("start");
        ConstraintSet constraintSet2 = new ConstraintSet("end");
        Constraint constraint1 = new Constraint("a");
        Constraint constraint2 = new Constraint("b");
        Constraint constraint3 = new Constraint("a");
        Constraint constraint4 = new Constraint("b");
        constraint1.setWidth(50);
        constraint1.setHeight(60);
        constraint2.setWidth(30);
        constraint2.setHeight(0);
        constraint2.setDimensionRatio("1:1");
        constraint1.linkToLeft(constraint2.getLeft(), 10);
        constraint1.linkToRight(constraint2.getRight(), 0, 15);
        constraintSet1.add(constraint1);
        constraint3.setHeightPercent(40);
        constraint3.setWidthPercent(30);
        constraint4.setHeight(20);
        constraint4.setHeight(30);
        constraint4.setWidthDefault(Constraint.Behaviour.SPREAD);
        constraint4.setHeightDefault(Constraint.Behaviour.WRAP);
        constraint4.setHeightMax(100);
        constraint4.linkToTop(constraint3.getTop(), 5, 10);
        constraint4.linkToBottom(constraint3.getBottom());
        constraintSet1.add(constraint2);
        constraintSet2.add(constraint3);
        constraintSet2.add(constraint4);
        motionScene.addConstraintSet(constraintSet1);
        motionScene.addConstraintSet(constraintSet2);
        System.out.println(motionScene);
        String exp = "{\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "a:{\n"
                + "left:['b','left',10],\n"
                + "right:['b','right',0,15],\n"
                + "width:50,\n"
                + "height:60,\n"
                + "},\n"
                + "b:{\n"
                + "width:30,\n"
                + "height:0,\n"
                + "dimensionRatio:'1:1',\n"
                + "},\n"
                + "},\n"
                + "end:{\n"
                + "a:{\n"
                + "width:'30%',\n"
                + "height:'40%',\n"
                + "},\n"
                + "b:{\n"
                + "top:['a','top',5,10],\n"
                + "bottom:['a','bottom'],\n"
                + "height:30,\n"
                + "width:'spread',\n"
                + "height:{value:'wrap',max:100},\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      a: {\n"
                + "        left: ['b', 'left', 10],\n"
                + "        right: ['b', 'right', 0, 15],\n"
                + "        width: 50,\n"
                + "        height: 60\n"
                + "      },\n"
                + "      b: { width: 30, height: 0, dimensionRatio: '1:1' }\n"
                + "    },\n"
                + "    end: {\n"
                + "      a: { width: '30%', height: '40%' },\n"
                + "      b: {\n"
                + "        top: ['a', 'top', 5, 10],\n"
                + "        bottom: ['a', 'bottom'],\n"
                + "        height: 30,\n"
                + "        width: 'spread',\n"
                + "        height: { value: 'wrap', max: 100 }\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }

    @Test
    public void testHelper01() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        ConstraintSet constraintSet = new ConstraintSet("start");
        Helper helper = new Helper("barrier1",
                new Helper.HelperType("barrier"), "margin:10,contains:[['a1', 1, 2],'b1']");
        constraintSet.add(helper);
        motionScene.addConstraintSet(constraintSet);
        System.out.println(motionScene);
        String exp = "{\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "barrier1:{\n"
                + "type:'barrier',\n"
                + "margin:10,\n"
                + "contains:[['a1',1,2],'b1'],\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      barrier1: { type: 'barrier', margin: 10, contains: [['a1', 1, 2], 'b1'] "
                + "}\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }

    @Test
    public void testHelper02() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        ConstraintSet constraintSet = new ConstraintSet("start");
        Helper helper1 = new Helper("grid1",
                new Helper.HelperType("grid"), "row:3, column:5");
        constraintSet.add(helper1);
        Helper helper2 = new Helper("vchain", new Helper.HelperType("vChain"),
                "style:'spread',top:['a1',1 , 2], bottom:['b1', 10], contains:['c1','d1']");
        constraintSet.add(helper2);
        motionScene.addConstraintSet(constraintSet);
        System.out.println(motionScene);
        String exp = "{\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "grid1:{\n"
                + "type:'grid',\n"
                + "column:5,\n"
                + "row:3,\n"
                + "},\n"
                + "vchain:{\n"
                + "type:'vChain',\n"
                + "contains:['c1','d1'],\n"
                + "top:['a1',1,2],\n"
                + "bottom:['b1',10],\n"
                + "style:'spread',\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      grid1: { type: 'grid', column: 5, row: 3 },\n"
                + "      vchain: {\n"
                + "        type: 'vChain',\n"
                + "        contains: ['c1', 'd1'],\n"
                + "        top: ['a1', 1, 2],\n"
                + "        bottom: ['b1', 10],\n"
                + "        style: 'spread'\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }

    @Test
    public void testVGuideline01() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        ConstraintSet constraintSet = new ConstraintSet("start");
        VGuideline guideline1 = new VGuideline("g1");
        constraintSet.add(guideline1);
        VGuideline guideline2 = new VGuideline("g2", "start:10");
        constraintSet.add(guideline2);
        VGuideline guideline3 = new VGuideline("g3", "percent:0.5");
        constraintSet.add(guideline3);
        motionScene.addConstraintSet(constraintSet);
        System.out.println(motionScene);
        String exp = "{\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "g1:{\n"
                + "type:'vGuideline',\n"
                + "},\n"
                + "g2:{\n"
                + "type:'vGuideline',\n"
                + "start:10,\n"
                + "},\n"
                + "g3:{\n"
                + "type:'vGuideline',\n"
                + "percent:0.5,\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      g1: { type: 'vGuideline' },\n"
                + "      g2: { type: 'vGuideline', start: 10 },\n"
                + "      g3: { type: 'vGuideline', percent: 0.5 }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }

    @Test
    public void testVGuideline02() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        ConstraintSet constraintSet = new ConstraintSet("start");
        VGuideline guideline1 = new VGuideline("g1");
        guideline1.setEnd(20);
        constraintSet.add(guideline1);
        VGuideline guideline2 = new VGuideline("g2", "start:10");
        guideline2.setStart(40);
        constraintSet.add(guideline2);
        VGuideline guideline3 = new VGuideline("g3", "percent:0.5");
        guideline3.setPercent(0.75f);
        constraintSet.add(guideline3);
        motionScene.addConstraintSet(constraintSet);
        System.out.println(motionScene);
        String exp = "{\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "g1:{\n"
                + "type:'vGuideline',\n"
                + "end:20,\n"
                + "},\n"
                + "g2:{\n"
                + "type:'vGuideline',\n"
                + "start:40,\n"
                + "},\n"
                + "g3:{\n"
                + "type:'vGuideline',\n"
                + "percent:0.75,\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      g1: { type: 'vGuideline', end: 20 },\n"
                + "      g2: { type: 'vGuideline', start: 40 },\n"
                + "      g3: { type: 'vGuideline', percent: 0.75 }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }

    @Test
    public void testHGuideline01() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        ConstraintSet constraintSet = new ConstraintSet("start");
        HGuideline guideline1 = new HGuideline("g1");
        constraintSet.add(guideline1);
        HGuideline guideline2 = new HGuideline("g2", "start:10");
        constraintSet.add(guideline2);
        HGuideline guideline3 = new HGuideline("g3", "percent:0.5");
        constraintSet.add(guideline3);
        motionScene.addConstraintSet(constraintSet);
        System.out.println(motionScene);
        String exp = "{\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "g1:{\n"
                + "type:'hGuideline',\n"
                + "},\n"
                + "g2:{\n"
                + "type:'hGuideline',\n"
                + "start:10,\n"
                + "},\n"
                + "g3:{\n"
                + "type:'hGuideline',\n"
                + "percent:0.5,\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      g1: { type: 'hGuideline' },\n"
                + "      g2: { type: 'hGuideline', start: 10 },\n"
                + "      g3: { type: 'hGuideline', percent: 0.5 }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }

    @Test
    public void testHGuideline02() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        ConstraintSet constraintSet = new ConstraintSet("start");
        HGuideline guideline1 = new HGuideline("g1");
        constraintSet.add(guideline1);
        guideline1.setEnd(30);
        HGuideline guideline2 = new HGuideline("g2", "start:10");
        constraintSet.add(guideline2);
        guideline2.setStart(50);
        HGuideline guideline3 = new HGuideline("g3", "percent:0.5");
        guideline3.setPercent(0.25f);
        constraintSet.add(guideline3);
        motionScene.addConstraintSet(constraintSet);
        System.out.println(motionScene);
        String exp = "{\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "g1:{\n"
                + "type:'hGuideline',\n"
                + "end:30,\n"
                + "},\n"
                + "g2:{\n"
                + "type:'hGuideline',\n"
                + "start:50,\n"
                + "},\n"
                + "g3:{\n"
                + "type:'hGuideline',\n"
                + "percent:0.25,\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      g1: { type: 'hGuideline', end: 30 },\n"
                + "      g2: { type: 'hGuideline', start: 50 },\n"
                + "      g3: { type: 'hGuideline', percent: 0.25 }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }

    @Test
    public void testVChain01() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        ConstraintSet constraintSet = new ConstraintSet("start");
        VChain chain1 = new VChain("chain1");
        constraintSet.add(chain1);
        VChain chain2 = new VChain("chain2", "style:'spread'");
        constraintSet.add(chain2);
        VChain chain3 = new VChain("chain3", "style:'packed', top:['a1',1,2]");
        constraintSet.add(chain3);
        motionScene.addConstraintSet(constraintSet);
        System.out.println(motionScene);
        String exp = "{\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "chain1:{\n"
                + "type:'vChain',\n"
                + "},\n"
                + "chain2:{\n"
                + "type:'vChain',\n"
                + "style:'spread',\n"
                + "},\n"
                + "chain3:{\n"
                + "type:'vChain',\n"
                + "top:['a1',1,2],\n"
                + "style:'packed',\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      chain1: { type: 'vChain' },\n"
                + "      chain2: { type: 'vChain', style: 'spread' },\n"
                + "      chain3: { type: 'vChain', top: ['a1', 1, 2], style: 'packed' }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }

    @Test
    public void testVChain02() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        ConstraintSet constraintSet = new ConstraintSet("start");
        VChain chain1 = new VChain("chain1");
        chain1.setStyle(Chain.Style.SPREAD_INSIDE);
        constraintSet.add(chain1);
        VChain chain2 = new VChain("chain2", "style:spread");
        chain2.setStyle(Chain.Style.PACKED);
        chain2.linkToBaseline(new Constraint("c1").getBaseline());
        constraintSet.add(chain2);
        VChain chain3 = new VChain("chain3", "style:packed, top:['a1',1,2]");
        chain3.setStyle(Chain.Style.SPREAD);
        chain3.linkToTop(new Constraint("c2").getBottom(), 10);
        chain3.linkToBottom(new Constraint("c3").getTop(), 25, 50);
        constraintSet.add(chain3);
        motionScene.addConstraintSet(constraintSet);
        System.out.println(motionScene);
        String exp = "{\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "chain1:{\n"
                + "type:'vChain',\n"
                + "style:'spread_inside',\n"
                + "},\n"
                + "chain2:{\n"
                + "type:'vChain',\n"
                + "style:'packed',\n"
                + "baseline:['c1','baseline'],\n"
                + "},\n"
                + "chain3:{\n"
                + "type:'vChain',\n"
                + "top:['c2','bottom',10],\n"
                + "bottom:['c3','top',25,50],\n"
                + "style:'spread',\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      chain1: { type: 'vChain', style: 'spread_inside' },\n"
                + "      chain2: { type: 'vChain', style: 'packed', baseline: ['c1', 'baseline'] "
                + "},\n"
                + "      chain3: {\n"
                + "        type: 'vChain',\n"
                + "        top: ['c2', 'bottom', 10],\n"
                + "        bottom: ['c3', 'top', 25, 50],\n"
                + "        style: 'spread'\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }

    @Test
    public void testVChain03() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        ConstraintSet constraintSet = new ConstraintSet("start");
        VChain chain1 = new VChain("chain1");
        chain1.addReference("['a1']");
        chain1.addReference(new Ref("b1", 30, 50, 20));
        chain1.addReference(new Ref("c1"));
        chain1.addReference("['d1', 10]");
        constraintSet.add(chain1);
        VChain chain2 = new VChain("chain2",
                "contains:['a1', ['b1', 10, 15,20,,], ['c1', 25, 35  ,,,]]]");
        Ref ref2 = new Ref("ref2");
        ref2.setPreMargin(50);
        ref2.setWeight(20);
        chain2.addReference(ref2);
        constraintSet.add(chain2);
        VChain chain3 = new VChain("chain3");
        Ref ref3 = new Ref("ref3");
        ref3.setWeight(75);
        chain3.addReference(ref3);
        constraintSet.add(chain3);
        motionScene.addConstraintSet(constraintSet);
        System.out.println(motionScene);
        String exp = "{\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "chain1:{\n"
                + "type:'vChain',\n"
                + "contains:['a1',['b1',30.0,50.0,20.0],'c1',['d1',10.0],],\n"
                + "},\n"
                + "chain2:{\n"
                + "type:'vChain',\n"
                + "contains:['a1',['b1',10.0,15.0,20.0],['c1',25.0,35.0],['ref2',20.0,50.0],],\n"
                + "},\n"
                + "chain3:{\n"
                + "type:'vChain',\n"
                + "contains:[['ref3',75.0],],\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      chain1: {\n"
                + "        type: 'vChain',\n"
                + "        contains: ['a1', ['b1', 30, 50, 20], 'c1', ['d1', 10]]\n"
                + "      },\n"
                + "      chain2: {\n"
                + "        type: 'vChain',\n"
                + "        contains: ['a1', ['b1', 10, 15, 20], ['c1', 25, 35], ['ref2', 20, 50]]\n"
                + "      },\n"
                + "      chain3: { type: 'vChain', contains: [['ref3', 75]] }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }

    @Test
    public void testHChain01() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        ConstraintSet constraintSet = new ConstraintSet("start");
        HChain chain1 = new HChain("chain1");
        constraintSet.add(chain1);
        HChain chain2 = new HChain("chain2", "style:'spread'");
        constraintSet.add(chain2);
        HChain chain3 = new HChain("chain3", "style:'packed', start:['a1',1,2]");
        constraintSet.add(chain3);
        motionScene.addConstraintSet(constraintSet);
        System.out.println(motionScene);
        String exp = "{\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "chain1:{\n"
                + "type:'hChain',\n"
                + "},\n"
                + "chain2:{\n"
                + "type:'hChain',\n"
                + "style:'spread',\n"
                + "},\n"
                + "chain3:{\n"
                + "type:'hChain',\n"
                + "start:['a1',1,2],\n"
                + "style:'packed',\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      chain1: { type: 'hChain' },\n"
                + "      chain2: { type: 'hChain', style: 'spread' },\n"
                + "      chain3: { type: 'hChain', start: ['a1', 1, 2], style: 'packed' }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }

    @Test
    public void testHChain02() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        ConstraintSet constraintSet = new ConstraintSet("start");
        HChain chain1 = new HChain("chain1");
        chain1.setStyle(Chain.Style.PACKED);
        chain1.linkToStart(new Constraint("c1").getEnd(), 10, 25);
        chain1.linkToEnd(new Constraint("c2").getStart());
        constraintSet.add(chain1);
        HChain chain2 = new HChain("chain2", "style:'spread'");
        chain2.setStyle(Chain.Style.SPREAD_INSIDE);
        chain2.linkToLeft(new Constraint("c3").getLeft(), 5);
        chain2.linkToRight(new Constraint("c4").getRight(), 25, 40);
        constraintSet.add(chain2);
        HChain chain3 = new HChain("chain3", "style:'packed', "
                + "start:['a1',1,2], end:['b1',3,4]");
        chain3.linkToStart(new Constraint("a2").getStart(), 10, 20);
        chain3.linkToEnd(new Constraint("b2").getEnd(), 30, 40);
        constraintSet.add(chain3);
        motionScene.addConstraintSet(constraintSet);
        System.out.println(motionScene);
        String exp = "{\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "chain1:{\n"
                + "type:'hChain',\n"
                + "start:['c1','end',10,25],\n"
                + "style:'packed',\n"
                + "end:['c2','start'],\n"
                + "},\n"
                + "chain2:{\n"
                + "type:'hChain',\n"
                + "left:['c3','left',5],\n"
                + "style:'spread_inside',\n"
                + "right:['c4','right',25,40],\n"
                + "},\n"
                + "chain3:{\n"
                + "type:'hChain',\n"
                + "start:['a2','start',10,20],\n"
                + "style:'packed',\n"
                + "end:['b2','end',30,40],\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      chain1: {\n"
                + "        type: 'hChain',\n"
                + "        start: ['c1', 'end', 10, 25],\n"
                + "        style: 'packed',\n"
                + "        end: ['c2', 'start']\n"
                + "      },\n"
                + "      chain2: {\n"
                + "        type: 'hChain',\n"
                + "        left: ['c3', 'left', 5],\n"
                + "        style: 'spread_inside',\n"
                + "        right: ['c4', 'right', 25, 40]\n"
                + "      },\n"
                + "      chain3: {\n"
                + "        type: 'hChain',\n"
                + "        start: ['a2', 'start', 10, 20],\n"
                + "        style: 'packed',\n"
                + "        end: ['b2', 'end', 30, 40]\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }

    @Test
    public void testHChain03() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        ConstraintSet constraintSet = new ConstraintSet("start");
        HChain chain1 = new HChain("chain1");
        chain1.addReference("['a1', 10, 20, 30]");
        chain1.addReference(new Ref("b1"));
        chain1.addReference("'c1', 50");
        constraintSet.add(chain1);
        HChain chain2 = new HChain("chain2", "contains:[['a1', 10, 15,20,,], 'a2']");
        Ref ref1 = new Ref("a3");
        ref1.setPostMargin(100);
        chain2.addReference(ref1);
        constraintSet.add(chain2);
        motionScene.addConstraintSet(constraintSet);
        System.out.println(motionScene);
        String exp = "{\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "chain1:{\n"
                + "type:'hChain',\n"
                + "contains:[['a1',10.0,20.0,30.0],'b1',['c1',50.0],],\n"
                + "},\n"
                + "chain2:{\n"
                + "type:'hChain',\n"
                + "contains:[['a1',10.0,15.0,20.0],'a2',['a3',0.0,0.0,100.0],],\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      chain1: { type: 'hChain', contains: [['a1', 10, 20, 30], 'b1', ['c1', "
                + "50]] },\n"
                + "      chain2: {\n"
                + "        type: 'hChain',\n"
                + "        contains: [['a1', 10, 15, 20], 'a2', ['a3', 0, 0, 100]]\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }

    @Test
    public void testBarrier01() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        ConstraintSet constraintSet = new ConstraintSet("start");
        Barrier barrier1 = new Barrier("barrier1",
                "direction:'bottom', margin:10,contains:[['a1', 1, 2 ],'b1']");
        constraintSet.add(barrier1);
        motionScene.addConstraintSet(constraintSet);
        System.out.println(motionScene);
        String exp = "{\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "barrier1:{\n"
                + "type:'barrier',\n"
                + "margin:10,\n"
                + "contains:[['a1',1,2],'b1'],\n"
                + "direction:'bottom',\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      barrier1: {\n"
                + "        type: 'barrier',\n"
                + "        margin: 10,\n"
                + "        contains: [['a1', 1, 2], 'b1'],\n"
                + "        direction: 'bottom'\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }

    @Test
    public void testBarrier02() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        ConstraintSet constraintSet = new ConstraintSet("start");
        Barrier barrier1 = new Barrier("barrier1",
                "direction:bottom, margin:10,contains:[['a1', 1, 2],'b1']");
        barrier1.addReference(new Ref("c1", 10, 15));
        barrier1.setMargin(25);
        barrier1.setDirection(Constraint.Side.TOP);
        constraintSet.add(barrier1);
        Barrier barrier2 = new Barrier("barrier2");
        barrier2.addReference(new Ref("a1"));
        barrier2.addReference(new Ref("b1", 10, 15, 25));
        barrier2.setDirection(Constraint.Side.START);
        barrier2.setMargin(15);
        constraintSet.add(barrier2);
        motionScene.addConstraintSet(constraintSet);
        System.out.println(motionScene);
        String exp = "{\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "barrier1:{\n"
                + "type:'barrier',\n"
                + "margin:25,\n"
                + "contains:[['a1',1.0,2.0],'b1',['c1',10.0,15.0],],\n"
                + "direction:'top',\n"
                + "},\n"
                + "barrier2:{\n"
                + "type:'barrier',\n"
                + "contains:['a1',['b1',10.0,15.0,25.0],],\n"
                + "margin:15,\n"
                + "direction:'start',\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      barrier1: {\n"
                + "        type: 'barrier',\n"
                + "        margin: 25,\n"
                + "        contains: [['a1', 1, 2], 'b1', ['c1', 10, 15]],\n"
                + "        direction: 'top'\n"
                + "      },\n"
                + "      barrier2: {\n"
                + "        type: 'barrier',\n"
                + "        contains: ['a1', ['b1', 10, 15, 25]],\n"
                + "        margin: 15,\n"
                + "        direction: 'start'\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }

    @Test
    public void testDemo01() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        motionScene.addTransition(new Transition("start", "end"));
        ConstraintSet cs1 = new ConstraintSet("start");
        ConstraintSet cs2 = new ConstraintSet("end");
        Constraint c1 = new Constraint("id1");
        Constraint c2 = new Constraint("id1");
        c1.linkToStart(Constraint.PARENT.getStart(), 16);
        c1.linkToBottom(Constraint.PARENT.getBottom(), 16);
        c1.setWidth(40);
        c1.setHeight(40);
        cs1.add(c1);
        c2.linkToEnd(Constraint.PARENT.getEnd(), 16);
        c2.linkToTop(Constraint.PARENT.getTop(), 16);
        c2.setWidth(100);
        c2.setHeight(100);
        cs2.add(c2);
        motionScene.addConstraintSet(cs1);
        motionScene.addConstraintSet(cs2);
        System.out.println(motionScene);
        String exp = "{\n"
                + "Transitions:{\n"
                + "default:{\n"
                + "from:'start',\n"
                + "to:'end',\n"
                + "},\n"
                + "},\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "id1:{\n"
                + "bottom:['parent','bottom',16],\n"
                + "start:['parent','start',16],\n"
                + "width:40,\n"
                + "height:40,\n"
                + "},\n"
                + "},\n"
                + "end:{\n"
                + "id1:{\n"
                + "top:['parent','top',16],\n"
                + "end:['parent','end',16],\n"
                + "width:100,\n"
                + "height:100,\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  Transitions: {\n"
                + "    default: {\n"
                + "      from: 'start',\n"
                + "      to: 'end'\n"
                + "    }\n"
                + "  },\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      id1: {\n"
                + "        bottom: ['parent', 'bottom', 16],\n"
                + "        start: ['parent', 'start', 16],\n"
                + "        width: 40,\n"
                + "        height: 40\n"
                + "      }\n"
                + "    },\n"
                + "    end: {\n"
                + "      id1: {\n"
                + "        top: ['parent', 'top', 16],\n"
                + "        end: ['parent', 'end', 16],\n"
                + "        width: 100,\n"
                + "        height: 100\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }

    @Test
    public void testDemo02() throws CLParsingException {
        MotionScene motionScene = new MotionScene();
        motionScene.addTransition(new Transition("start", "end"));
        ConstraintSet cs1 = new ConstraintSet("start");
        ConstraintSet cs2 = new ConstraintSet("end");
        Constraint c1 = new Constraint("id1");
        Constraint c2 = new Constraint("id2");
        Constraint c3 = new Constraint("id3");
        HChain chain1 = new HChain("chain1", "contains:['id1','id2','id3'], "
                + "start:['parent', 'start', 10], end:['parent', 'end', 10]");
        c1.setWidth(40);
        c1.setHeight(40);
        c2.setWidth(40);
        c2.setHeight(40);
        c3.setWidth(40);
        c3.setHeight(40);
        cs1.add(c1);
        cs1.add(c2);
        cs1.add(c3);
        cs1.add(chain1);

        Constraint c4 = new Constraint("id1");
        Constraint c5 = new Constraint("id2");
        Constraint c6 = new Constraint("id3");
        VChain chain2 = new VChain("chain2", "contains:['id1','id2','id3'], "
                + "top:['parent', 'top', 10], bottom:['parent', 'bottom', 10]");
        c4.setWidth(50);
        c4.setHeight(50);
        c5.setWidth(60);
        c5.setHeight(60);
        c6.setWidth(70);
        c6.setHeight(70);
        cs2.add(c4);
        cs2.add(c5);
        cs2.add(c6);
        cs2.add(chain2);
        motionScene.addConstraintSet(cs1);
        motionScene.addConstraintSet(cs2);
        System.out.println(motionScene);
        String exp = "{\n"
                + "Transitions:{\n"
                + "default:{\n"
                + "from:'start',\n"
                + "to:'end',\n"
                + "},\n"
                + "},\n"
                + "ConstraintSets:{\n"
                + "start:{\n"
                + "id1:{\n"
                + "width:40,\n"
                + "height:40,\n"
                + "},\n"
                + "id2:{\n"
                + "width:40,\n"
                + "height:40,\n"
                + "},\n"
                + "id3:{\n"
                + "width:40,\n"
                + "height:40,\n"
                + "},\n"
                + "chain1:{\n"
                + "type:'hChain',\n"
                + "contains:['id1','id2','id3'],\n"
                + "start:['parent','start',10],\n"
                + "end:['parent','end',10],\n"
                + "},\n"
                + "},\n"
                + "end:{\n"
                + "id1:{\n"
                + "width:50,\n"
                + "height:50,\n"
                + "},\n"
                + "id2:{\n"
                + "width:60,\n"
                + "height:60,\n"
                + "},\n"
                + "id3:{\n"
                + "width:70,\n"
                + "height:70,\n"
                + "},\n"
                + "chain2:{\n"
                + "type:'vChain',\n"
                + "contains:['id1','id2','id3'],\n"
                + "top:['parent','top',10],\n"
                + "bottom:['parent','bottom',10],\n"
                + "},\n"
                + "},\n"
                + "},\n"
                + "}\n";
        assertEquals(exp, motionScene.toString());

        String formattedJson = CLParser.parse(motionScene.toString()).toFormattedJSON();
        String formatExp = "{\n"
                + "  Transitions: {\n"
                + "    default: {\n"
                + "      from: 'start',\n"
                + "      to: 'end'\n"
                + "    }\n"
                + "  },\n"
                + "  ConstraintSets: {\n"
                + "    start: {\n"
                + "      id1: { width: 40, height: 40 },\n"
                + "      id2: { width: 40, height: 40 },\n"
                + "      id3: { width: 40, height: 40 },\n"
                + "      chain1: {\n"
                + "        type: 'hChain',\n"
                + "        contains: ['id1', 'id2', 'id3'],\n"
                + "        start: ['parent', 'start', 10],\n"
                + "        end: ['parent', 'end', 10]\n"
                + "      }\n"
                + "    },\n"
                + "    end: {\n"
                + "      id1: { width: 50, height: 50 },\n"
                + "      id2: { width: 60, height: 60 },\n"
                + "      id3: { width: 70, height: 70 },\n"
                + "      chain2: {\n"
                + "        type: 'vChain',\n"
                + "        contains: ['id1', 'id2', 'id3'],\n"
                + "        top: ['parent', 'top', 10],\n"
                + "        bottom: ['parent', 'bottom', 10]\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        assertEquals(formatExp, formattedJson);

        TransitionParser.parse(CLParser.parse(motionScene.toString()), transitionState, dipToDip);
    }
}
