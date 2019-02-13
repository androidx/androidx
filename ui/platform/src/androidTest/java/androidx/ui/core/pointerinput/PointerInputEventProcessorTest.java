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

package androidx.ui.core.pointerinput;

import static androidx.ui.core.pointerinput.PointerEventPass.InitialDown;
import static androidx.ui.core.pointerinput.PointerEventPass.PostDown;
import static androidx.ui.core.pointerinput.PointerEventPass.PostUp;
import static androidx.ui.core.pointerinput.PointerEventPass.PreDown;
import static androidx.ui.core.pointerinput.PointerEventPass.PreUp;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.ui.core.Density;
import androidx.ui.core.DensityKt;
import androidx.ui.core.Dimension;
import androidx.ui.core.Duration;
import androidx.ui.core.LayoutNode;
import androidx.ui.core.PointerInputNode;
import androidx.ui.engine.geometry.Offset;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import kotlin.Triple;
import kotlin.jvm.functions.Function2;

// TODO(shepshapard): Write the following PointerInputEvent to PointerInputChangeEvent tests
// 2 down, 2 move, 2 up, converted correctly
// 3 down, 3 move, 3 up, converted correctly
// down, up, down, up, converted correctly
// 2 down, 1 up, same down, both up, converted correctly
// 2 down, 1 up, new down, both up, converted correctly
// new is up, throws exception

// TODO(shepshapard): Write the following hit testing tests
// 2 down, one hits, target receives correct event
// 2 down, one moves in, one out, 2 up, target receives correct event stream
// down, up, receives down and up
// down, move, up, receives all 3
// down, up, then down and misses, target receives down and up
// down, misses, moves in bounds, up, target does not receive event
// down, hits, moves out of bounds, up, target receives all events

// TODO(shepshapard): Write the following offset testing tests
// 2 Directly Nested PointerInputNodes inside offset LayoutNode, offset is correct
// PointerInputNode inside 3 nested parents, offset is correct
// 2 LayoutNodes in between PointerInputNodes, offset is correct
// 3 simultaneous moves, offsets are correct

// TODO(shepshapard): Write hit test tests related to siblings (once the functionality has been
// written).

// TODO(shepshapard): Write the following pointer input dispatch path tests:
// down, move, up, on 2, hits all 5 passes
// 2 down, hits for each individually (TODO, this will change)

// TODO(shepshapard): These tests shouldn't require Android to run, but currently do given Crane
// currently relies on Android Context.
@SmallTest
@RunWith(JUnit4.class)
public class PointerInputEventProcessorTest {

    private Density mDensity;
    private List<Triple<PointerInputNode, PointerEventPass, PointerInputChange>> mTrackerList;

    @Before
    public void setup() {
        mDensity = DensityKt.Density(InstrumentationRegistry.getContext());
        mTrackerList = new ArrayList<>();
    }

    @Test
    public void process_downMoveUp_convertedCorrectly() {

        // Arrange

        List<PointerEventPass> pointerInputPassesToTrack = Collections.singletonList(PostUp);

        PointerInputNode pointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker pointerInputHandlerTracker =
                new PointerInputHandlerTracker(pointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        pointerInputNode.setPointerInputHandler(pointerInputHandlerTracker);

        LayoutNode parentLayoutNode = createLayoutNode(0, 0, 500, 500);
        parentLayoutNode.emitInsertAt(0, pointerInputNode);

        Offset offset = createPixelOffset(100, 200);
        Offset offset2 = createPixelOffset(300, 400);

        PointerInputEvent down = createPointerInputEvent(createDuration(0), 8712, offset, true);
        PointerInputEvent move = createPointerInputEvent(createDuration(0), 8712, offset2, true);
        PointerInputEvent up = createPointerInputEvent(createDuration(0), 8712, offset2, false);

        PointerInputEventProcessor pointerInputEventProcessor =
                new PointerInputEventProcessor(mDensity, parentLayoutNode);

        // Act

        pointerInputEventProcessor.process(down);
        pointerInputEventProcessor.process(move);
        pointerInputEventProcessor.process(up);

        // Assert

        assertThat(mTrackerList.size(), is(equalTo(3)));
        assertPointerInputChange(
                mTrackerList.get(0),
                pointerInputNode,
                PostUp,
                8712,
                offset,
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(1),
                pointerInputNode,
                PostUp,
                8712,
                offset2,
                true,
                offset,
                true,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(2),
                pointerInputNode,
                PostUp,
                8712,
                offset2,
                false,
                offset2,
                true,
                createConsumeData());
    }

    @Test
    public void process_downHits_targetReceives() {

        // Arrange

        List<PointerEventPass> pointerInputPassesToTrack = Collections.singletonList(PostUp);

        PointerInputNode pointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker pointerInputHandlerTracker =
                new PointerInputHandlerTracker(pointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        pointerInputNode.setPointerInputHandler(pointerInputHandlerTracker);

        LayoutNode childLayoutNode = createLayoutNode(100, 200, 301, 401);
        childLayoutNode.emitInsertAt(0, pointerInputNode);

        LayoutNode parentLayoutNode = createLayoutNode(0, 0, 500, 500);
        parentLayoutNode.emitInsertAt(0, childLayoutNode);

        Offset topLeftOffset = createPixelOffset(100, 200);
        Offset topRightOffset = createPixelOffset(300, 200);
        Offset bottomLeftOffset = createPixelOffset(100, 400);
        Offset bottomRightOffset = createPixelOffset(300, 400);

        PointerInputEvent topLeft =
                createPointerInputEvent(createDuration(0), 0, topLeftOffset, true);

        PointerInputEvent topRight =
                createPointerInputEvent(createDuration(0), 1, topRightOffset, true);

        PointerInputEvent bottomLeft =
                createPointerInputEvent(createDuration(0), 2, bottomLeftOffset, true);

        PointerInputEvent bottomRight =
                createPointerInputEvent(createDuration(0), 3, bottomRightOffset, true);

        PointerInputEventProcessor pointerInputEventProcessor = new PointerInputEventProcessor(
                mDensity, parentLayoutNode);

        // Act

        pointerInputEventProcessor.process(topLeft);
        pointerInputEventProcessor.process(topRight);
        pointerInputEventProcessor.process(bottomLeft);
        pointerInputEventProcessor.process(bottomRight);

        // Assert

        assertThat(mTrackerList.size(), is(equalTo(4)));
        assertPointerInputChange(
                mTrackerList.get(0),
                pointerInputNode,
                PostUp,
                0,
                topLeftOffset.minus(topLeftOffset),
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(1),
                pointerInputNode,
                PostUp,
                1,
                topRightOffset.minus(topLeftOffset),
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(2),
                pointerInputNode,
                PostUp,
                2,
                bottomLeftOffset.minus(topLeftOffset),
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(3),
                pointerInputNode,
                PostUp,
                3,
                bottomRightOffset.minus(topLeftOffset),
                true,
                null,
                false,
                createConsumeData());
    }

    @Test
    public void process_downMisses_targetDoesNotReceive() {

        // Arrange

        List<PointerEventPass> pointerInputPassesToTrack = Collections.singletonList(PostUp);

        PointerInputNode pointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker pointerInputHandlerTracker =
                new PointerInputHandlerTracker(pointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        pointerInputNode.setPointerInputHandler(pointerInputHandlerTracker);

        LayoutNode childLayoutNode = createLayoutNode(100, 200, 301, 401);
        childLayoutNode.emitInsertAt(0, pointerInputNode);

        LayoutNode parentLayoutNode = createLayoutNode(0, 0, 500, 500);
        parentLayoutNode.emitInsertAt(0, childLayoutNode);

        Offset topLeftToLeftOffset = createPixelOffset(99, 200);
        Offset bottomLeftToLeftOffset = createPixelOffset(99, 400);
        Offset topLeftAboveOffset = createPixelOffset(100, 199);
        Offset bottomLeftBelowOffset = createPixelOffset(100, 401);
        Offset topRightAboveOffset = createPixelOffset(300, 199);
        Offset bottomRightBelowOffset = createPixelOffset(300, 401);
        Offset topRightToRightOffset = createPixelOffset(301, 200);
        Offset bottomRightToRightOffset = createPixelOffset(301, 400);

        PointerInputEvent topLeftToLeft =
                createPointerInputEvent(createDuration(0), 0, topLeftToLeftOffset, true);

        PointerInputEvent bottomLeftToLeft =
                createPointerInputEvent(createDuration(0), 1, bottomLeftToLeftOffset, true);

        PointerInputEvent topLeftAbove =
                createPointerInputEvent(createDuration(0), 2, topLeftAboveOffset, true);

        PointerInputEvent bottomLeftBelow =
                createPointerInputEvent(createDuration(0), 3, bottomLeftBelowOffset, true);

        PointerInputEvent topRightAbove =
                createPointerInputEvent(createDuration(0), 4, topRightAboveOffset, true);

        PointerInputEvent bottomRightBelow =
                createPointerInputEvent(createDuration(0), 5, bottomRightBelowOffset, true);

        PointerInputEvent topRightToRight =
                createPointerInputEvent(createDuration(0), 6, topRightToRightOffset, true);

        PointerInputEvent bottomRightToRight =
                createPointerInputEvent(createDuration(0), 7, bottomRightToRightOffset, true);

        PointerInputEventProcessor pointerInputEventProcessor = new PointerInputEventProcessor(
                mDensity, parentLayoutNode);

        // Act

        pointerInputEventProcessor.process(topLeftToLeft);
        pointerInputEventProcessor.process(bottomLeftToLeft);
        pointerInputEventProcessor.process(topLeftAbove);
        pointerInputEventProcessor.process(bottomLeftBelow);
        pointerInputEventProcessor.process(topRightAbove);
        pointerInputEventProcessor.process(bottomRightBelow);
        pointerInputEventProcessor.process(topRightToRight);
        pointerInputEventProcessor.process(bottomRightToRight);

        // Assert

        assertThat(mTrackerList.size(), is(equalTo(0)));
    }

    @Test
    public void process_downHits3of3_All3TargetsReceive() {

        // Arrange

        List<PointerEventPass> pointerInputPassesToTrack = Collections.singletonList(PostUp);

        // Create PointerInputNodes

        PointerInputNode parentPointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker parentPointerInputHandlerTracker =
                new PointerInputHandlerTracker(parentPointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        parentPointerInputNode.setPointerInputHandler(parentPointerInputHandlerTracker);

        PointerInputNode middlePointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker middlePointerInputHandlerTracker =
                new PointerInputHandlerTracker(middlePointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        middlePointerInputNode.setPointerInputHandler(middlePointerInputHandlerTracker);

        PointerInputNode childPointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker childPointerInputHandlerTracker =
                new PointerInputHandlerTracker(childPointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        childPointerInputNode.setPointerInputHandler(childPointerInputHandlerTracker);

        // Create LayoutNodes
        LayoutNode parentLayoutNode = createLayoutNode(0, 0, 500, 500);
        LayoutNode middleLayoutNode = createLayoutNode(100, 100, 400, 400);
        LayoutNode childLayoutNode = createLayoutNode(100, 100, 200, 200);

        // Setup tree
        parentLayoutNode.emitInsertAt(0, parentPointerInputNode);
        parentPointerInputNode.emitInsertAt(0, middleLayoutNode);
        middleLayoutNode.emitInsertAt(0, middlePointerInputNode);
        middlePointerInputNode.emitInsertAt(0, childLayoutNode);
        childLayoutNode.emitInsertAt(0, childPointerInputNode);

        Offset offset = createPixelOffset(250, 250);

        PointerInputEvent down =
                createPointerInputEvent(createDuration(0), 0, offset, true);

        PointerInputEventProcessor pointerInputEventProcessor =
                new PointerInputEventProcessor(mDensity, parentLayoutNode);

        // Act

        pointerInputEventProcessor.process(down);

        // Assert

        Offset middleOffset = new Offset(
                DensityKt.toPx(middleLayoutNode.getX(), mDensity),
                DensityKt.toPx(middleLayoutNode.getY(), mDensity));

        Offset childOffset = middleOffset.plus(new Offset(
                DensityKt.toPx(childLayoutNode.getX(), mDensity),
                DensityKt.toPx(childLayoutNode.getY(), mDensity)));

        assertThat(mTrackerList.size(), is(equalTo(3)));
        assertPointerInputChange(
                mTrackerList.get(0),
                childPointerInputNode,
                PostUp,
                0,
                offset.minus(childOffset),
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(1),
                middlePointerInputNode,
                PostUp,
                0,
                offset.minus(middleOffset),
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(2),
                parentPointerInputNode,
                PostUp,
                0,
                offset,
                true,
                null,
                false,
                createConsumeData());
    }

    @Test
    public void process_downHits2Of3_onlyCorrect2TargetsReceive() {

        // Arrange

        List<PointerEventPass> pointerInputPassesToTrack = Collections.singletonList(PostUp);

        // Create PointerInputNodes

        PointerInputNode parentPointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker parentPointerInputHandlerTracker =
                new PointerInputHandlerTracker(parentPointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        parentPointerInputNode.setPointerInputHandler(parentPointerInputHandlerTracker);

        PointerInputNode middlePointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker middlePointerInputHandlerTracker =
                new PointerInputHandlerTracker(middlePointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        middlePointerInputNode.setPointerInputHandler(middlePointerInputHandlerTracker);

        PointerInputNode childPointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker childPointerInputHandlerTracker =
                new PointerInputHandlerTracker(childPointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        childPointerInputNode.setPointerInputHandler(childPointerInputHandlerTracker);

        // Create LayoutNodes
        LayoutNode parentLayoutNode = createLayoutNode(0, 0, 500, 500);
        LayoutNode middleLayoutNode = createLayoutNode(100, 100, 400, 400);
        LayoutNode childLayoutNode = createLayoutNode(100, 100, 200, 200);

        // Setup tree
        parentLayoutNode.emitInsertAt(0, parentPointerInputNode);
        parentPointerInputNode.emitInsertAt(0, middleLayoutNode);
        middleLayoutNode.emitInsertAt(0, middlePointerInputNode);
        middlePointerInputNode.emitInsertAt(0, childLayoutNode);
        childLayoutNode.emitInsertAt(0, childPointerInputNode);

        Offset offset = createPixelOffset(150, 150);

        PointerInputEvent down =
                createPointerInputEvent(createDuration(0), 0, offset, true);

        PointerInputEventProcessor pointerInputEventProcessor =
                new PointerInputEventProcessor(mDensity, parentLayoutNode);

        // Act

        pointerInputEventProcessor.process(down);

        // Assert

        Offset middleOffset = new Offset(
                DensityKt.toPx(middleLayoutNode.getX(), mDensity),
                DensityKt.toPx(middleLayoutNode.getY(), mDensity));

        assertThat(mTrackerList.size(), is(equalTo(2)));
        assertPointerInputChange(
                mTrackerList.get(0),
                middlePointerInputNode,
                PostUp,
                0,
                offset.minus(middleOffset),
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(1),
                parentPointerInputNode,
                PostUp,
                0,
                offset,
                true,
                null,
                false,
                createConsumeData());
    }

    @Test
    public void process_downHits1Of3_onlyCorrectTargetReceives() {

        // Arrange

        List<PointerEventPass> pointerInputPassesToTrack = Collections.singletonList(PostUp);

        // Create PointerInputNodes

        PointerInputNode parentPointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker parentPointerInputHandlerTracker =
                new PointerInputHandlerTracker(parentPointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        parentPointerInputNode.setPointerInputHandler(parentPointerInputHandlerTracker);

        PointerInputNode middlePointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker middlePointerInputHandlerTracker =
                new PointerInputHandlerTracker(middlePointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        middlePointerInputNode.setPointerInputHandler(middlePointerInputHandlerTracker);

        PointerInputNode childPointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker childPointerInputHandlerTracker =
                new PointerInputHandlerTracker(childPointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        childPointerInputNode.setPointerInputHandler(childPointerInputHandlerTracker);

        // Create LayoutNodes
        LayoutNode parentLayoutNode = createLayoutNode(0, 0, 500, 500);
        LayoutNode middleLayoutNode = createLayoutNode(100, 100, 400, 400);
        LayoutNode childLayoutNode = createLayoutNode(100, 100, 200, 200);

        // Setup tree
        parentLayoutNode.emitInsertAt(0, parentPointerInputNode);
        parentPointerInputNode.emitInsertAt(0, middleLayoutNode);
        middleLayoutNode.emitInsertAt(0, middlePointerInputNode);
        middlePointerInputNode.emitInsertAt(0, childLayoutNode);
        childLayoutNode.emitInsertAt(0, childPointerInputNode);

        Offset offset = createPixelOffset(50, 50);

        PointerInputEvent down =
                createPointerInputEvent(createDuration(0), 0, offset, true);

        PointerInputEventProcessor pointerInputEventProcessor =
                new PointerInputEventProcessor(mDensity, parentLayoutNode);

        // Act

        pointerInputEventProcessor.process(down);

        // Assert

        assertThat(mTrackerList.size(), is(equalTo(1)));
        assertPointerInputChange(
                mTrackerList.get(0),
                parentPointerInputNode,
                PostUp,
                0,
                offset,
                true,
                null,
                false,
                createConsumeData());
    }

    @Test
    public void process_downMoveUp_hitsAll5Passes() {

        // Arrange

        List<PointerEventPass> pointerInputPassesToTrack =
                Arrays.asList(InitialDown, PreUp, PreDown, PostUp, PostDown);

        PointerInputNode pointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker pointerInputHandlerTracker =
                new PointerInputHandlerTracker(pointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        pointerInputNode.setPointerInputHandler(pointerInputHandlerTracker);

        LayoutNode parentLayoutNode = createLayoutNode(0, 0, 500, 500);
        parentLayoutNode.emitInsertAt(0, pointerInputNode);

        Offset offset = createPixelOffset(100, 200);
        Offset offset2 = createPixelOffset(300, 400);

        PointerInputEvent down = createPointerInputEvent(createDuration(0), 8712, offset, true);
        PointerInputEvent move = createPointerInputEvent(createDuration(0), 8712, offset2, true);
        PointerInputEvent up = createPointerInputEvent(createDuration(0), 8712, offset2, false);

        PointerInputEventProcessor pointerInputEventProcessor =
                new PointerInputEventProcessor(mDensity, parentLayoutNode);

        // Act

        pointerInputEventProcessor.process(down);
        pointerInputEventProcessor.process(move);
        pointerInputEventProcessor.process(up);

        // Assert

        assertThat(mTrackerList.size(), is(equalTo(15)));
        PointerEventPass[] passes =
                new PointerEventPass[]{InitialDown, PreUp, PreDown, PostUp, PostDown};

        // For each PointerInputEvent, verify that it went through the PointerInputNode's
        // pointerInputHandler for each pass in order.
        for (int i = 0; i < 5; i++) {
            assertPointerInputChange(
                    mTrackerList.get(i),
                    pointerInputNode,
                    passes[i],
                    8712,
                    offset,
                    true,
                    null,
                    false,
                    createConsumeData());
        }
        for (int i = 0; i < 5; i++) {
            assertPointerInputChange(
                    mTrackerList.get(i + 5),
                    pointerInputNode,
                    passes[i],
                    8712,
                    offset2,
                    true,
                    offset,
                    true,
                    createConsumeData());
        }
        for (int i = 0; i < 5; i++) {
            assertPointerInputChange(
                    mTrackerList.get(i + 10),
                    pointerInputNode,
                    passes[i],
                    8712,
                    offset2,
                    false,
                    offset2,
                    true,
                    createConsumeData());
        }
    }

    @Test
    public void process_modifiedPointerInputChange_isPassedToNext() {

        // Arrange

        List<PointerEventPass> pointerInputPassesToTrack =
                Arrays.asList(InitialDown, PreUp, PreDown, PostUp, PostDown);

        PointerInputNode pointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker pointerInputHandlerTracker =
                new PointerInputHandlerTracker(pointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        // Setup the tracker to consume 20 pixels of movement if any movement exists in the
        // PointerInputChange.
        pointerInputHandlerTracker.mPointerInputChangeModifier = new PointerInputChangeModifier() {
            @Override
            public PointerInputChange modify(PointerInputChange pointerInputChange) {
                if (PointerInputChangeEventKt.positionChangedIgnoreConsumed(pointerInputChange)) {
                    return PointerInputChangeEventKt
                            .consumePositionChange(pointerInputChange, 13f, 0);
                }
                return pointerInputChange;
            }
        };
        pointerInputNode.setPointerInputHandler(pointerInputHandlerTracker);

        LayoutNode parentLayoutNode = createLayoutNode(0, 0, 500, 500);
        parentLayoutNode.emitInsertAt(0, pointerInputNode);

        Offset offset = createPixelOffset(0, 0);
        Offset offset2 = createPixelOffset(100, 0);

        PointerInputEvent down = createPointerInputEvent(createDuration(0), 8712, offset, true);
        PointerInputEvent move = createPointerInputEvent(createDuration(0), 8712, offset2, true);

        PointerInputEventProcessor pointerInputEventProcessor =
                new PointerInputEventProcessor(mDensity, parentLayoutNode);

        // Act

        pointerInputEventProcessor.process(down);
        pointerInputEventProcessor.process(move);

        // Assert

        assertThat(mTrackerList.size(), is(equalTo(10)));
        PointerEventPass[] passes =
                new PointerEventPass[]{InitialDown, PreUp, PreDown, PostUp, PostDown};

        float consumedX = 0;
        for (int i = 0; i < 5; i++) {
            assertPointerInputChange(
                    mTrackerList.get(i + 5),
                    pointerInputNode,
                    passes[i],
                    8712,
                    offset2,
                    true,
                    offset,
                    true,
                    new ConsumedData(new Offset(consumedX, 0f), false));
            consumedX += 13;
        }
    }

    @Test
    public void process_layoutNodesIncreasinglyInset_pointerInputChangeTranslatedCorrectly() {
        process_pointerInputChangeTranslatedCorrectly(
                0, 0, 100, 100,
                2, 11, 100, 100,
                23, 31, 100, 100,
                99, 99
        );
    }

    @Test
    public void process_layoutNodesIncreasinglyOutset_pointerInputChangeTranslatedCorrectly() {
        process_pointerInputChangeTranslatedCorrectly(
                0, 0, 100, 100,
                -2, -11, 100, 100,
                -23, -31, 100, 100,
                1, 1
        );
    }

    @Test
    public void process_layoutNodesNotOffset_pointerInputChangeTranslatedCorrectly() {
        process_pointerInputChangeTranslatedCorrectly(
                0, 0, 100, 100,
                0, 0, 100, 100,
                0, 0, 100, 100,
                50, 50
        );
    }

    private void process_pointerInputChangeTranslatedCorrectly(
            int pX1, int pY1, int pX2, int pY2,
            int mX1, int mY1, int mX2, int mY2,
            int cX1, int cY1, int cX2, int cY2,
            int pointerX, int pointerY) {

        // Arrange

        List<PointerEventPass> pointerInputPassesToTrack =
                Arrays.asList(InitialDown, PreUp, PreDown, PostUp, PostDown);

        // Create PointerInputNodes

        PointerInputNode parentPointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker parentPointerInputHandlerTracker =
                new PointerInputHandlerTracker(parentPointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        parentPointerInputNode.setPointerInputHandler(parentPointerInputHandlerTracker);

        PointerInputNode middlePointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker middlePointerInputHandlerTracker =
                new PointerInputHandlerTracker(middlePointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        middlePointerInputNode.setPointerInputHandler(middlePointerInputHandlerTracker);

        PointerInputNode childPointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker childPointerInputHandlerTracker =
                new PointerInputHandlerTracker(childPointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        childPointerInputNode.setPointerInputHandler(childPointerInputHandlerTracker);

        // Create LayoutNodes
        LayoutNode parentLayoutNode = createLayoutNode(pX1, pY1, pX2, pY2);
        LayoutNode middleLayoutNode = createLayoutNode(mX1, mY1, mX2, mY2);
        LayoutNode childLayoutNode = createLayoutNode(cX1, cY1, cX2, cY2);

        // Setup tree
        parentLayoutNode.emitInsertAt(0, parentPointerInputNode);
        parentPointerInputNode.emitInsertAt(0, middleLayoutNode);
        middleLayoutNode.emitInsertAt(0, middlePointerInputNode);
        middlePointerInputNode.emitInsertAt(0, childLayoutNode);
        childLayoutNode.emitInsertAt(0, childPointerInputNode);

        Offset offset = createPixelOffset(pointerX, pointerY);

        PointerInputEvent down =
                createPointerInputEvent(createDuration(0), 0, offset, true);

        PointerInputEventProcessor pointerInputEventProcessor =
                new PointerInputEventProcessor(mDensity, parentLayoutNode);

        // Act

        pointerInputEventProcessor.process(down);

        // Assert

        Offset middleOffset = new Offset(
                DensityKt.toPx(middleLayoutNode.getX(), mDensity),
                DensityKt.toPx(middleLayoutNode.getY(), mDensity));

        Offset childOffset = middleOffset.plus(new Offset(
                DensityKt.toPx(childLayoutNode.getX(), mDensity),
                DensityKt.toPx(childLayoutNode.getY(), mDensity)));

        assertThat(mTrackerList.size(), is(equalTo(15)));

        assertPointerInputChange(
                mTrackerList.get(0),
                parentPointerInputNode,
                InitialDown,
                0,
                offset,
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(1),
                middlePointerInputNode,
                InitialDown,
                0,
                offset.minus(middleOffset),
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(2),
                childPointerInputNode,
                InitialDown,
                0,
                offset.minus(childOffset),
                true,
                null,
                false,
                createConsumeData());

        assertPointerInputChange(
                mTrackerList.get(3),
                childPointerInputNode,
                PreUp,
                0,
                offset.minus(childOffset),
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(4),
                middlePointerInputNode,
                PreUp,
                0,
                offset.minus(middleOffset),
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(5),
                parentPointerInputNode,
                PreUp,
                0,
                offset,
                true,
                null,
                false,
                createConsumeData());

        assertPointerInputChange(
                mTrackerList.get(6),
                parentPointerInputNode,
                PreDown,
                0,
                offset,
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(7),
                middlePointerInputNode,
                PreDown,
                0,
                offset.minus(middleOffset),
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(8),
                childPointerInputNode,
                PreDown,
                0,
                offset.minus(childOffset),
                true,
                null,
                false,
                createConsumeData());

        assertPointerInputChange(
                mTrackerList.get(9),
                childPointerInputNode,
                PostUp,
                0,
                offset.minus(childOffset),
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(10),
                middlePointerInputNode,
                PostUp,
                0,
                offset.minus(middleOffset),
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(11),
                parentPointerInputNode,
                PostUp,
                0,
                offset,
                true,
                null,
                false,
                createConsumeData());

        assertPointerInputChange(
                mTrackerList.get(12),
                parentPointerInputNode,
                PostDown,
                0,
                offset,
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(13),
                middlePointerInputNode,
                PostDown,
                0,
                offset.minus(middleOffset),
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(14),
                childPointerInputNode,
                PostDown,
                0,
                offset.minus(childOffset),
                true,
                null,
                false,
                createConsumeData());

    }

    @Test
    public void process_downUp_pointerInputChangeTranslatedCorrectly() {

        // Arrange

        List<PointerEventPass> pointerInputPassesToTrack = Collections.singletonList(InitialDown);

        // Create PointerInputNodes

        PointerInputNode parentPointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker parentPointerInputHandlerTracker =
                new PointerInputHandlerTracker(parentPointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        parentPointerInputNode.setPointerInputHandler(parentPointerInputHandlerTracker);

        PointerInputNode middlePointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker middlePointerInputHandlerTracker =
                new PointerInputHandlerTracker(middlePointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        middlePointerInputNode.setPointerInputHandler(middlePointerInputHandlerTracker);

        PointerInputNode childPointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker childPointerInputHandlerTracker =
                new PointerInputHandlerTracker(childPointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        childPointerInputNode.setPointerInputHandler(childPointerInputHandlerTracker);

        // Create LayoutNodes
        LayoutNode parentLayoutNode = createLayoutNode(0, 0, 100, 100);
        LayoutNode middleLayoutNode = createLayoutNode(2, 11, 100, 100);
        LayoutNode childLayoutNode = createLayoutNode(23, 31, 100, 100);

        // Setup tree
        parentLayoutNode.emitInsertAt(0, parentPointerInputNode);
        parentPointerInputNode.emitInsertAt(0, middleLayoutNode);
        middleLayoutNode.emitInsertAt(0, middlePointerInputNode);
        middlePointerInputNode.emitInsertAt(0, childLayoutNode);
        childLayoutNode.emitInsertAt(0, childPointerInputNode);

        Offset offset = createPixelOffset(99, 99);

        PointerInputEvent down =
                createPointerInputEvent(createDuration(0), 0, offset, true);
        PointerInputEvent up =
                createPointerInputEvent(createDuration(1), 0, null, false);

        PointerInputEventProcessor pointerInputEventProcessor =
                new PointerInputEventProcessor(mDensity, parentLayoutNode);

        // Act

        pointerInputEventProcessor.process(down);
        pointerInputEventProcessor.process(up);

        // Assert

        Offset middleOffset = new Offset(
                DensityKt.toPx(middleLayoutNode.getX(), mDensity),
                DensityKt.toPx(middleLayoutNode.getY(), mDensity));

        Offset childOffset = middleOffset.plus(new Offset(
                DensityKt.toPx(childLayoutNode.getX(), mDensity),
                DensityKt.toPx(childLayoutNode.getY(), mDensity)));

        assertThat(mTrackerList.size(), is(equalTo(6)));
        assertPointerInputChange(
                mTrackerList.get(0),
                parentPointerInputNode,
                InitialDown,
                0,
                offset,
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(1),
                middlePointerInputNode,
                InitialDown,
                0,
                offset.minus(middleOffset),
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(2),
                childPointerInputNode,
                InitialDown,
                0,
                offset.minus(childOffset),
                true,
                null,
                false,
                createConsumeData());

        assertPointerInputChange(
                mTrackerList.get(3),
                parentPointerInputNode,
                InitialDown,
                0,
                null,
                false,
                offset,
                true,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(4),
                middlePointerInputNode,
                InitialDown,
                0,
                null,
                false,
                offset.minus(middleOffset),
                true,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(5),
                childPointerInputNode,
                InitialDown,
                0,
                null,
                false,
                offset.minus(childOffset),
                true,
                createConsumeData());
    }

    // Private helpers

    private Duration createDuration(long millis) {
        return Duration.create(0, 0, 0, 0, millis, 0);
    }

    private LayoutNode createLayoutNode(int xDp, int yDp, int xDp2, int yDp2) {
        LayoutNode layoutNode = new LayoutNode();
        layoutNode.moveTo(new Dimension(xDp), new Dimension(yDp));
        layoutNode.resize(new Dimension(xDp2 - xDp), new Dimension(yDp2 - yDp));
        return layoutNode;
    }

    private PointerInputEvent createPointerInputEvent(Duration timeStamp, int id, Offset position,
            boolean down) {
        PointerInputData pointerInputData = new PointerInputData(position, down);
        List<PointerInputEventData> pointerInputEventDatas =
                Arrays.asList(new PointerInputEventData(id, pointerInputData));
        return new PointerInputEvent(timeStamp, pointerInputEventDatas);
    }

    private Offset createPixelOffset(int xdp, int ydp) {
        return new Offset(createPixelDimension(xdp), createPixelDimension(ydp));
    }

    private float createPixelDimension(int dp) {
        return DensityKt.toPx(new Dimension(dp), mDensity);
    }

    private ConsumedData createConsumeData() {
        return new ConsumedData(Offset.Companion.getZero(), false);
    }

    private void assertPointerInputChange(
            Triple<PointerInputNode, PointerEventPass, PointerInputChange> actual,
            PointerInputNode pointerInputNode,
            PointerEventPass pass,
            int id,
            Offset currentPosition,
            boolean currentDown,
            Offset previousPosition,
            boolean previousDown,
            ConsumedData consumedData) {
        assertThat(actual.getFirst(), is(equalTo(pointerInputNode)));
        assertThat(actual.getSecond(), is(pass));

        PointerInputChange pointerInputChange = actual.getThird();

        assertThat(pointerInputChange.getId(), is(equalTo(id)));
        PointerInputData current = pointerInputChange.getCurrent();
        assertThat(current.getPosition(), is(equalTo(currentPosition)));
        assertThat(current.getDown(), is(currentDown));
        PointerInputData previous = pointerInputChange.getPrevious();
        assertThat(previous.getPosition(), is(equalTo(previousPosition)));
        assertThat(previous.getDown(), is(previousDown));
        assertThat(pointerInputChange.getConsumed(), is(equalTo(consumedData)));
    }

    private class PointerInputHandlerTracker implements
            Function2<PointerInputChange, PointerEventPass, PointerInputChange> {

        private PointerInputNode mPointerInputNode;
        private List<Triple<PointerInputNode, PointerEventPass, PointerInputChange>> mTrackerList;
        private List<PointerEventPass> mPointerEventPassesToTrack;
        public PointerInputChangeModifier mPointerInputChangeModifier;

        PointerInputHandlerTracker(PointerInputNode pointerInputNode,
                List<Triple<PointerInputNode, PointerEventPass, PointerInputChange>> trackerList,
                List<PointerEventPass> pointerEventPassesToTrack) {
            mPointerInputNode = pointerInputNode;
            mTrackerList = trackerList;
            mPointerEventPassesToTrack = pointerEventPassesToTrack;
        }

        @Override
        public PointerInputChange invoke(PointerInputChange pointerInputChange,
                PointerEventPass pointerEventPass) {
            if (mPointerEventPassesToTrack.contains(pointerEventPass)) {
                mTrackerList.add(
                        new Triple<>(mPointerInputNode, pointerEventPass, pointerInputChange));
                if (mPointerInputChangeModifier != null) {
                    pointerInputChange = mPointerInputChangeModifier.modify(pointerInputChange);
                }
            }
            return pointerInputChange;
        }
    }

    interface PointerInputChangeModifier {
        PointerInputChange modify(PointerInputChange pointerInputChange);
    }


}
