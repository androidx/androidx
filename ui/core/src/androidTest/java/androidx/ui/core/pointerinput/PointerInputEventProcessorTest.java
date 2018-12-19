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

import static androidx.ui.core.pointerinput.PointerEventPass.INITIAL_DOWN;
import static androidx.ui.core.pointerinput.PointerEventPass.POST_DOWN;
import static androidx.ui.core.pointerinput.PointerEventPass.POST_UP;
import static androidx.ui.core.pointerinput.PointerEventPass.PRE_DOWN;
import static androidx.ui.core.pointerinput.PointerEventPass.PRE_UP;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import android.content.Context;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.ui.core.Dimension;
import androidx.ui.core.DimensionKt;
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

// TODO(shepshapard): Write hit test tests related to siblings (once the functionality has been
// written).

// TODO(shepshapard): Write the following pointer input dispatch path tests:
// down, move, up, on 2, hits all 5 passes
// 2 down, hits for each individually (TODO, this will change)

// TODO(shepshapard): Write tests that verify that PointerInput data is offset to be relative
// to each PointerInputNode bounds as it is passed through the hierarchy (once this
// functionality is written).

// TODO(shepshapard): These tests shouldn't require Android to run, but currently do given Crane
// currently relies on Android Context.
@SmallTest
@RunWith(JUnit4.class)
public class PointerInputEventProcessorTest {

    private Context mContext;
    private List<Triple<PointerInputNode, Integer, PointerInputChange>> mTrackerList;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getContext();
        mTrackerList = new ArrayList<>();
    }

    @Test
    public void process_downMoveUp_convertedCorrectly() {

        // Arrange

        List<Integer> pointerInputPassesToTrack = Collections.singletonList(POST_UP);

        PointerInputNode pointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker pointerInputHandlerTracker =
                new PointerInputHandlerTracker(pointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        pointerInputNode.setPointerInputHandler(pointerInputHandlerTracker);

        LayoutNode parentLayoutNode = createLayoutNode(0, 0, 500, 500);
        parentLayoutNode.getChildren().add(pointerInputNode);

        Offset offset = createPixelOffset(100, 200);
        Offset offset2 = createPixelOffset(300, 400);

        PointerInputEvent down = createPointerInputEvent(createDuration(0), 8712, offset, true);
        PointerInputEvent move = createPointerInputEvent(createDuration(0), 8712, offset2, true);
        PointerInputEvent up = createPointerInputEvent(createDuration(0), 8712, offset2, false);

        PointerInputEventProcessor pointerInputEventProcessor =
                new PointerInputEventProcessor(mContext, parentLayoutNode);

        // Act

        pointerInputEventProcessor.process(down);
        pointerInputEventProcessor.process(move);
        pointerInputEventProcessor.process(up);

        // Assert

        assertThat(mTrackerList.size(), is(equalTo(3)));
        assertPointerInputChange(
                mTrackerList.get(0),
                pointerInputNode,
                POST_UP,
                8712,
                offset,
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(1),
                pointerInputNode,
                POST_UP,
                8712,
                offset2,
                true,
                offset,
                true,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(2),
                pointerInputNode,
                POST_UP,
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

        List<Integer> pointerInputPassesToTrack = Collections.singletonList(POST_UP);

        PointerInputNode pointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker pointerInputHandlerTracker =
                new PointerInputHandlerTracker(pointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        pointerInputNode.setPointerInputHandler(pointerInputHandlerTracker);

        LayoutNode childLayoutNode = createLayoutNode(100, 200, 301, 401);
        childLayoutNode.getChildren().add(pointerInputNode);

        LayoutNode parentLayoutNode = createLayoutNode(0, 0, 500, 500);
        parentLayoutNode.getChildren().add(childLayoutNode);

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
                mContext, parentLayoutNode);

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
                POST_UP,
                0,
                topLeftOffset,
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(1),
                pointerInputNode,
                POST_UP,
                1,
                topRightOffset,
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(2),
                pointerInputNode,
                POST_UP,
                2,
                bottomLeftOffset,
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(3),
                pointerInputNode,
                POST_UP,
                3,
                bottomRightOffset,
                true,
                null,
                false,
                createConsumeData());
    }

    @Test
    public void process_downMisses_targetDoesNotReceive() {

        // Arrange

        List<Integer> pointerInputPassesToTrack = Collections.singletonList(POST_UP);

        PointerInputNode pointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker pointerInputHandlerTracker =
                new PointerInputHandlerTracker(pointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        pointerInputNode.setPointerInputHandler(pointerInputHandlerTracker);

        LayoutNode childLayoutNode = createLayoutNode(100, 200, 301, 401);
        childLayoutNode.getChildren().add(pointerInputNode);

        LayoutNode parentLayoutNode = createLayoutNode(0, 0, 500, 500);
        parentLayoutNode.getChildren().add(childLayoutNode);

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
                mContext, parentLayoutNode);

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
    public void process_downHits3of3_targetReceives3() {

        // Arrange

        List<Integer> pointerInputPassesToTrack = Collections.singletonList(POST_UP);

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
        parentLayoutNode.getChildren().add(parentPointerInputNode);
        parentPointerInputNode.setChild(middleLayoutNode);
        middleLayoutNode.getChildren().add(middlePointerInputNode);
        middlePointerInputNode.setChild(childLayoutNode);
        childLayoutNode.getChildren().add(childPointerInputNode);

        Offset offset = createPixelOffset(250, 250);

        PointerInputEvent topLeft =
                createPointerInputEvent(createDuration(0), 0, offset, true);

        PointerInputEventProcessor pointerInputEventProcessor =
                new PointerInputEventProcessor(mContext, parentLayoutNode);

        // Act

        pointerInputEventProcessor.process(topLeft);

        // Assert

        assertThat(mTrackerList.size(), is(equalTo(3)));
        assertPointerInputChange(
                mTrackerList.get(0),
                childPointerInputNode,
                POST_UP,
                0,
                offset,
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(1),
                middlePointerInputNode,
                POST_UP,
                0,
                offset,
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(2),
                parentPointerInputNode,
                POST_UP,
                0,
                offset,
                true,
                null,
                false,
                createConsumeData());
    }

    @Test
    public void process_downHits2Of3_targetReceives2() {

        // Arrange

        List<Integer> pointerInputPassesToTrack = Collections.singletonList(POST_UP);

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
        parentLayoutNode.getChildren().add(parentPointerInputNode);
        parentPointerInputNode.setChild(middleLayoutNode);
        middleLayoutNode.getChildren().add(middlePointerInputNode);
        middlePointerInputNode.setChild(childLayoutNode);
        childLayoutNode.getChildren().add(childPointerInputNode);

        Offset offset = createPixelOffset(150, 150);

        PointerInputEvent topLeft =
                createPointerInputEvent(createDuration(0), 0, offset, true);

        PointerInputEventProcessor pointerInputEventProcessor =
                new PointerInputEventProcessor(mContext, parentLayoutNode);

        // Act

        pointerInputEventProcessor.process(topLeft);

        // Assert

        assertThat(mTrackerList.size(), is(equalTo(2)));
        assertPointerInputChange(
                mTrackerList.get(0),
                middlePointerInputNode,
                POST_UP,
                0,
                offset,
                true,
                null,
                false,
                createConsumeData());
        assertPointerInputChange(
                mTrackerList.get(1),
                parentPointerInputNode,
                POST_UP,
                0,
                offset,
                true,
                null,
                false,
                createConsumeData());
    }

    @Test
    public void process_downHits1Of3_targetReceives1() {

        // Arrange

        List<Integer> pointerInputPassesToTrack = Collections.singletonList(POST_UP);

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
        parentLayoutNode.getChildren().add(parentPointerInputNode);
        parentPointerInputNode.setChild(middleLayoutNode);
        middleLayoutNode.getChildren().add(middlePointerInputNode);
        middlePointerInputNode.setChild(childLayoutNode);
        childLayoutNode.getChildren().add(childPointerInputNode);

        Offset offset = createPixelOffset(50, 50);

        PointerInputEvent topLeft =
                createPointerInputEvent(createDuration(0), 0, offset, true);

        PointerInputEventProcessor pointerInputEventProcessor =
                new PointerInputEventProcessor(mContext, parentLayoutNode);

        // Act

        pointerInputEventProcessor.process(topLeft);

        // Assert

        assertThat(mTrackerList.size(), is(equalTo(1)));
        assertPointerInputChange(
                mTrackerList.get(0),
                parentPointerInputNode,
                POST_UP,
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

        List<Integer> pointerInputPassesToTrack =
                Arrays.asList(INITIAL_DOWN, PRE_UP, PRE_DOWN, POST_UP, POST_DOWN);

        PointerInputNode pointerInputNode = new PointerInputNode();
        PointerInputHandlerTracker pointerInputHandlerTracker =
                new PointerInputHandlerTracker(pointerInputNode, mTrackerList,
                        pointerInputPassesToTrack);
        pointerInputNode.setPointerInputHandler(pointerInputHandlerTracker);

        LayoutNode parentLayoutNode = createLayoutNode(0, 0, 500, 500);
        parentLayoutNode.getChildren().add(pointerInputNode);

        Offset offset = createPixelOffset(100, 200);
        Offset offset2 = createPixelOffset(300, 400);

        PointerInputEvent down = createPointerInputEvent(createDuration(0), 8712, offset, true);
        PointerInputEvent move = createPointerInputEvent(createDuration(0), 8712, offset2, true);
        PointerInputEvent up = createPointerInputEvent(createDuration(0), 8712, offset2, false);

        PointerInputEventProcessor pointerInputEventProcessor =
                new PointerInputEventProcessor(mContext, parentLayoutNode);

        // Act

        pointerInputEventProcessor.process(down);
        pointerInputEventProcessor.process(move);
        pointerInputEventProcessor.process(up);

        // Assert

        assertThat(mTrackerList.size(), is(equalTo(15)));
        Integer[] passes =
                new Integer[]{INITIAL_DOWN, PRE_UP, PRE_DOWN, POST_UP, POST_DOWN};

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

        List<Integer> pointerInputPassesToTrack =
                Arrays.asList(INITIAL_DOWN, PRE_UP, PRE_DOWN, POST_UP, POST_DOWN);

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
        parentLayoutNode.getChildren().add(pointerInputNode);

        Offset offset = createPixelOffset(0, 0);
        Offset offset2 = createPixelOffset(100, 0);

        PointerInputEvent down = createPointerInputEvent(createDuration(0), 8712, offset, true);
        PointerInputEvent move = createPointerInputEvent(createDuration(0), 8712, offset2, true);

        PointerInputEventProcessor pointerInputEventProcessor =
                new PointerInputEventProcessor(mContext, parentLayoutNode);

        // Act

        pointerInputEventProcessor.process(down);
        pointerInputEventProcessor.process(move);

        // Assert

        assertThat(mTrackerList.size(), is(equalTo(10)));
        Integer[] passes =
                new Integer[]{INITIAL_DOWN, PRE_UP, PRE_DOWN, POST_UP, POST_DOWN};

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

    // Private helpers

    private Duration createDuration(long millis) {
        return Duration.create(0, 0, 0, 0, millis, 0);
    }

    private LayoutNode createLayoutNode(int x, int y, int x2, int y2) {
        LayoutNode layoutNode = new LayoutNode();
        layoutNode.moveTo(new Dimension(x), new Dimension(y));
        layoutNode.resize(new Dimension(x2 - x), new Dimension(y2 - y));
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
        return DimensionKt.toPx(new Dimension(dp), mContext);
    }

    private ConsumedData createConsumeData() {
        return new ConsumedData(Offset.Companion.getZero(), false);
    }

    private void assertPointerInputChange(
            Triple<PointerInputNode, Integer, PointerInputChange> actual,
            PointerInputNode pointerInputNode,
            Integer pass,
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
            Function2<PointerInputChange, Integer, PointerInputChange> {

        private PointerInputNode mPointerInputNode;
        private List<Triple<PointerInputNode, Integer, PointerInputChange>> mTrackerList;
        private List<Integer> mPointerEventPassesToTrack;
        public PointerInputChangeModifier mPointerInputChangeModifier;

        PointerInputHandlerTracker(PointerInputNode pointerInputNode,
                List<Triple<PointerInputNode, Integer, PointerInputChange>> trackerList,
                List<Integer> pointerEventPassesToTrack) {
            mPointerInputNode = pointerInputNode;
            mTrackerList = trackerList;
            mPointerEventPassesToTrack = pointerEventPassesToTrack;
        }

        @Override
        public PointerInputChange invoke(PointerInputChange pointerInputChange,
                Integer pointerEventPass) {
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
