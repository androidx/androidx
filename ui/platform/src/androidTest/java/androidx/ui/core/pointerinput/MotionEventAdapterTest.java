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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import android.view.MotionEvent;

import androidx.test.filters.SmallTest;
import androidx.ui.core.Timestamp;
import androidx.ui.core.Timestamps;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SmallTest
@RunWith(JUnit4.class)
public class MotionEventAdapterTest {

    private static final Timestamp EXPECTED_TIMESTAMP = Timestamps.millisecondsToTimestamp(2894L);

    @Test
    public void toPointerInputEvent_1pointerActionDown_convertsCorrectly() {
        MotionEvent.PointerProperties[] pointerProperties =
                new MotionEvent.PointerProperties[]{createPointerProperties(8290)};
        MotionEvent.PointerCoords[] pointerCoords =
                new MotionEvent.PointerCoords[]{createPointerCoords(2967, 5928)};
        MotionEvent motionEvent = createMotionEvent(2894, MotionEvent.ACTION_DOWN, 1, 0,
                pointerProperties, pointerCoords);

        PointerInputEvent actual = MotionEventAdapterKt.toPointerInputEvent(motionEvent);

        assertThat(actual.getTimeStamp(), is(EXPECTED_TIMESTAMP));
        assertThat(actual.getPointers().size(), is(1));
        assertPointerInputEventData(actual.getPointers().get(0), 8290, true, 2967, 5928);
    }

    @Test
    public void toPointerInputEvent_1pointerActionMove_convertsCorrectly() {
        MotionEvent.PointerProperties[] pointerProperties =
                new MotionEvent.PointerProperties[]{createPointerProperties(8290)};
        MotionEvent.PointerCoords[] pointerCoords =
                new MotionEvent.PointerCoords[]{createPointerCoords(2967, 5928)};
        MotionEvent motionEvent = createMotionEvent(2894, MotionEvent.ACTION_MOVE, 1, 0,
                pointerProperties, pointerCoords);

        PointerInputEvent actual = MotionEventAdapterKt.toPointerInputEvent(motionEvent);

        assertThat(actual.getTimeStamp(), is(EXPECTED_TIMESTAMP));
        assertThat(actual.getPointers().size(), is(1));
        assertPointerInputEventData(actual.getPointers().get(0), 8290, true, 2967, 5928);
    }

    @Test
    public void toPointerInputEvent_1pointerActionUp_convertsCorrectly() {
        MotionEvent.PointerProperties[] pointerProperties =
                new MotionEvent.PointerProperties[]{createPointerProperties(8290)};
        MotionEvent.PointerCoords[] pointerCoords =
                new MotionEvent.PointerCoords[]{createPointerCoords(2967, 5928)};
        MotionEvent motionEvent = createMotionEvent(2894, MotionEvent.ACTION_UP, 1, 0,
                pointerProperties, pointerCoords);

        PointerInputEvent actual = MotionEventAdapterKt.toPointerInputEvent(motionEvent);

        assertThat(actual.getTimeStamp(), is(EXPECTED_TIMESTAMP));
        assertThat(actual.getPointers().size(), is(1));
        assertPointerInputEventData(actual.getPointers().get(0), 8290, false, 2967, 5928);
    }


    @Test
    public void toPointerInputEvent_2pointers1stPointerActionPointerDown_convertsCorrectly() {
        MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[]{
                createPointerProperties(8290),
                createPointerProperties(1516)
        };
        MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[]{
                createPointerCoords(2967, 5928),
                createPointerCoords(1942, 6729)
        };
        MotionEvent motionEvent = createMotionEvent(2894, MotionEvent.ACTION_POINTER_DOWN, 2, 0,
                pointerProperties, pointerCoords);

        PointerInputEvent actual = MotionEventAdapterKt.toPointerInputEvent(motionEvent);

        assertThat(actual.getTimeStamp(), is(EXPECTED_TIMESTAMP));
        assertThat(actual.getPointers().size(), is(2));
        assertPointerInputEventData(actual.getPointers().get(0), 8290, true, 2967, 5928);
        assertPointerInputEventData(actual.getPointers().get(1), 1516, true, 1942, 6729);
    }

    @Test
    public void toPointerInputEvent_2pointers2ndPointerActionPointerDown_convertsCorrectly() {
        MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[]{
                createPointerProperties(8290),
                createPointerProperties(1516)
        };
        MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[]{
                createPointerCoords(2967, 5928),
                createPointerCoords(1942, 6729)
        };
        MotionEvent motionEvent = createMotionEvent(2894, MotionEvent.ACTION_POINTER_DOWN, 2, 1,
                pointerProperties, pointerCoords);

        PointerInputEvent actual = MotionEventAdapterKt.toPointerInputEvent(motionEvent);

        assertThat(actual.getTimeStamp(), is(EXPECTED_TIMESTAMP));
        assertThat(actual.getPointers().size(), is(2));
        assertPointerInputEventData(actual.getPointers().get(0), 8290, true, 2967, 5928);
        assertPointerInputEventData(actual.getPointers().get(1), 1516, true, 1942, 6729);
    }

    @Test
    public void toPointerInputEvent_3pointers1stPointerActionPointerDown_convertsCorrectly() {
        MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[]{
                createPointerProperties(8290),
                createPointerProperties(1516),
                createPointerProperties(9285)
        };
        MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[]{
                createPointerCoords(2967, 5928),
                createPointerCoords(1942, 6729),
                createPointerCoords(6206, 1098)
        };
        MotionEvent motionEvent = createMotionEvent(2894, MotionEvent.ACTION_POINTER_DOWN, 3, 0,
                pointerProperties, pointerCoords);

        PointerInputEvent actual = MotionEventAdapterKt.toPointerInputEvent(motionEvent);

        assertThat(actual.getTimeStamp(), is(EXPECTED_TIMESTAMP));
        assertThat(actual.getPointers().size(), is(3));
        assertPointerInputEventData(actual.getPointers().get(0), 8290, true, 2967, 5928);
        assertPointerInputEventData(actual.getPointers().get(1), 1516, true, 1942, 6729);
        assertPointerInputEventData(actual.getPointers().get(2), 9285, true, 6206, 1098);
    }

    @Test
    public void toPointerInputEvent_3pointers2ndPointerActionPointerDown_convertsCorrectly() {
        MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[]{
                createPointerProperties(8290),
                createPointerProperties(1516),
                createPointerProperties(9285)
        };
        MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[]{
                createPointerCoords(2967, 5928),
                createPointerCoords(1942, 6729),
                createPointerCoords(6206, 1098)
        };
        MotionEvent motionEvent = createMotionEvent(2894, MotionEvent.ACTION_POINTER_DOWN, 3, 2,
                pointerProperties, pointerCoords);

        PointerInputEvent actual = MotionEventAdapterKt.toPointerInputEvent(motionEvent);

        assertThat(actual.getTimeStamp(), is(EXPECTED_TIMESTAMP));
        assertThat(actual.getPointers().size(), is(3));
        assertPointerInputEventData(actual.getPointers().get(0), 8290, true, 2967, 5928);
        assertPointerInputEventData(actual.getPointers().get(1), 1516, true, 1942, 6729);
        assertPointerInputEventData(actual.getPointers().get(2), 9285, true, 6206, 1098);
    }

    @Test
    public void toPointerInputEvent_3pointers3rdPointerActionPointerDown_convertsCorrectly() {
        MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[]{
                createPointerProperties(8290),
                createPointerProperties(1516),
                createPointerProperties(9285)
        };
        MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[]{
                createPointerCoords(2967, 5928),
                createPointerCoords(1942, 6729),
                createPointerCoords(6206, 1098)
        };
        MotionEvent motionEvent = createMotionEvent(2894, MotionEvent.ACTION_POINTER_DOWN, 3, 2,
                pointerProperties, pointerCoords);

        PointerInputEvent actual = MotionEventAdapterKt.toPointerInputEvent(motionEvent);

        assertThat(actual.getTimeStamp(), is(EXPECTED_TIMESTAMP));
        assertThat(actual.getPointers().size(), is(3));
        assertPointerInputEventData(actual.getPointers().get(0), 8290, true, 2967, 5928);
        assertPointerInputEventData(actual.getPointers().get(1), 1516, true, 1942, 6729);
        assertPointerInputEventData(actual.getPointers().get(2), 9285, true, 6206, 1098);
    }

    @Test
    public void toPointerInputEvent_2pointersActionMove_convertsCorrectly() {
        MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[]{
                createPointerProperties(8290),
                createPointerProperties(1516)
        };
        MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[]{
                createPointerCoords(2967, 5928),
                createPointerCoords(1942, 6729)
        };
        MotionEvent motionEvent = createMotionEvent(2894, MotionEvent.ACTION_MOVE, 2, 0,
                pointerProperties, pointerCoords);

        PointerInputEvent actual = MotionEventAdapterKt.toPointerInputEvent(motionEvent);

        assertThat(actual.getTimeStamp(), is(EXPECTED_TIMESTAMP));
        assertThat(actual.getPointers().size(), is(2));
        assertPointerInputEventData(actual.getPointers().get(0), 8290, true, 2967, 5928);
        assertPointerInputEventData(actual.getPointers().get(1), 1516, true, 1942, 6729);
    }

    @Test
    public void toPointerInputEvent_2pointers1stPointerActionPointerUP_convertsCorrectly() {
        MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[]{
                createPointerProperties(8290),
                createPointerProperties(1516)
        };
        MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[]{
                createPointerCoords(2967, 5928),
                createPointerCoords(1942, 6729)
        };
        MotionEvent motionEvent = createMotionEvent(2894, MotionEvent.ACTION_POINTER_UP, 2, 0,
                pointerProperties, pointerCoords);

        PointerInputEvent actual = MotionEventAdapterKt.toPointerInputEvent(motionEvent);

        assertThat(actual.getTimeStamp(), is(EXPECTED_TIMESTAMP));
        assertThat(actual.getPointers().size(), is(2));
        assertPointerInputEventData(actual.getPointers().get(0), 8290, false, 2967, 5928);
        assertPointerInputEventData(actual.getPointers().get(1), 1516, true, 1942, 6729);
    }

    @Test
    public void toPointerInputEvent_2pointers2ndPointerActionPointerUp_convertsCorrectly() {
        MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[]{
                createPointerProperties(8290),
                createPointerProperties(1516)
        };
        MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[]{
                createPointerCoords(2967, 5928),
                createPointerCoords(1942, 6729)
        };
        MotionEvent motionEvent = createMotionEvent(2894, MotionEvent.ACTION_POINTER_UP, 2, 1,
                pointerProperties, pointerCoords);

        PointerInputEvent actual = MotionEventAdapterKt.toPointerInputEvent(motionEvent);

        assertThat(actual.getTimeStamp(), is(EXPECTED_TIMESTAMP));
        assertThat(actual.getPointers().size(), is(2));
        assertPointerInputEventData(actual.getPointers().get(0), 8290, true, 2967, 5928);
        assertPointerInputEventData(actual.getPointers().get(1), 1516, false, 1942, 6729);
    }

    @Test
    public void toPointerInputEvent_3pointers1stPointerActionPointerUp_convertsCorrectly() {
        MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[]{
                createPointerProperties(8290),
                createPointerProperties(1516),
                createPointerProperties(9285)
        };
        MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[]{
                createPointerCoords(2967, 5928),
                createPointerCoords(1942, 6729),
                createPointerCoords(6206, 1098)
        };
        MotionEvent motionEvent = createMotionEvent(2894, MotionEvent.ACTION_POINTER_UP, 3, 0,
                pointerProperties, pointerCoords);

        PointerInputEvent actual = MotionEventAdapterKt.toPointerInputEvent(motionEvent);

        assertThat(actual.getTimeStamp(), is(EXPECTED_TIMESTAMP));
        assertThat(actual.getPointers().size(), is(3));
        assertPointerInputEventData(actual.getPointers().get(0), 8290, false, 2967, 5928);
        assertPointerInputEventData(actual.getPointers().get(1), 1516, true, 1942, 6729);
        assertPointerInputEventData(actual.getPointers().get(2), 9285, true, 6206, 1098);
    }

    @Test
    public void toPointerInputEvent_3pointers2ndPointerActionPointerUp_convertsCorrectly() {
        MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[]{
                createPointerProperties(8290),
                createPointerProperties(1516),
                createPointerProperties(9285)
        };
        MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[]{
                createPointerCoords(2967, 5928),
                createPointerCoords(1942, 6729),
                createPointerCoords(6206, 1098)
        };
        MotionEvent motionEvent = createMotionEvent(2894, MotionEvent.ACTION_POINTER_UP, 3, 1,
                pointerProperties, pointerCoords);

        PointerInputEvent actual = MotionEventAdapterKt.toPointerInputEvent(motionEvent);

        assertThat(actual.getTimeStamp(), is(EXPECTED_TIMESTAMP));
        assertThat(actual.getPointers().size(), is(3));
        assertPointerInputEventData(actual.getPointers().get(0), 8290, true, 2967, 5928);
        assertPointerInputEventData(actual.getPointers().get(1), 1516, false, 1942, 6729);
        assertPointerInputEventData(actual.getPointers().get(2), 9285, true, 6206, 1098);
    }

    @Test
    public void toPointerInputEvent_3pointers3rdPointerActionPointerUp_convertsCorrectly() {
        MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[]{
                createPointerProperties(8290),
                createPointerProperties(1516),
                createPointerProperties(9285)
        };
        MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[]{
                createPointerCoords(2967, 5928),
                createPointerCoords(1942, 6729),
                createPointerCoords(6206, 1098)
        };
        MotionEvent motionEvent = createMotionEvent(2894, MotionEvent.ACTION_POINTER_UP, 3, 2,
                pointerProperties, pointerCoords);

        PointerInputEvent actual = MotionEventAdapterKt.toPointerInputEvent(motionEvent);

        assertThat(actual.getTimeStamp(), is(EXPECTED_TIMESTAMP));
        assertThat(actual.getPointers().size(), is(3));
        assertPointerInputEventData(actual.getPointers().get(0), 8290, true, 2967, 5928);
        assertPointerInputEventData(actual.getPointers().get(1), 1516, true, 1942, 6729);
        assertPointerInputEventData(actual.getPointers().get(2), 9285, false, 6206, 1098);
    }

    private MotionEvent.PointerProperties createPointerProperties(int id) {
        MotionEvent.PointerProperties pointerProperties = new MotionEvent.PointerProperties();
        pointerProperties.id = id;
        return pointerProperties;
    }

    private MotionEvent.PointerCoords createPointerCoords(float x, float y) {
        MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
        pointerCoords.x = x;
        pointerCoords.y = y;
        return pointerCoords;
    }

    private MotionEvent createMotionEvent(int eventTime, int action,
            int numPointers, int actionIndex, MotionEvent.PointerProperties[] pointerProperties,
            MotionEvent.PointerCoords[] pointerCoords) {
        return MotionEvent.obtain(0, eventTime,
                action + (actionIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                numPointers,
                pointerProperties,
                pointerCoords,
                0, 0, 0, 0, 0, 0, 0, 0);
    }

    private void assertPointerInputEventData(PointerInputEventData actual, int id, boolean isDown,
            float x, float y) {
        PointerInputData pointerInputData = actual.getPointerInputData();
        assertThat(actual.getId(), is(id));
        assertThat(pointerInputData.getDown(), is(isDown));
        assertThat(pointerInputData.getPosition().getDx(), is(x));
        assertThat(pointerInputData.getPosition().getDy(), is(y));
    }
}
