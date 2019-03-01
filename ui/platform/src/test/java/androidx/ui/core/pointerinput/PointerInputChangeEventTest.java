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
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import androidx.test.filters.SmallTest;
import androidx.ui.engine.geometry.Offset;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

// TODO(shepshapard): Write the following tests when functionality is done.
// consumeDownChange_noChangeOccurred_throwsException
// consumeDownChange_alreadyConsumed_throwsException
// consumePositionChange_noChangeOccurred_throwsException
// consumePositionChange_changeOverConsumed_throwsException
// consumePositionChange_consumedInWrongDirection_throwsException

@SmallTest
@RunWith(JUnit4.class)
public class PointerInputChangeEventTest {

    @Test
    public void changedToDown_didNotChange_returnsFalse() {
        PointerInputChange pointerInputChange1 =
                createPointerInputChange(0, 0, false, 0, 0, false, 0, 0, false);
        PointerInputChange pointerInputChange2 =
                createPointerInputChange(0, 0, false, 0, 0, true, 0, 0, false);
        PointerInputChange pointerInputChange3 =
                createPointerInputChange(0, 0, true, 0, 0, true, 0, 0, false);

        assertThat(PointerInputChangeEventKt.changedToDown(pointerInputChange1), is(false));
        assertThat(PointerInputChangeEventKt.changedToDown(pointerInputChange2), is(false));
        assertThat(PointerInputChangeEventKt.changedToDown(pointerInputChange3), is(false));
    }

    @Test
    public void changedToDown_changeNotConsumed_returnsTrue() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(0, 0, true, 0, 0, false, 0, 0, false);
        assertThat(PointerInputChangeEventKt.changedToDown(pointerInputChange), is(true));
    }

    @Test
    public void changedToDown_changeNotConsumed_returnsFalse() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(0, 0, true, 0, 0, false, 0, 0, true);
        assertThat(PointerInputChangeEventKt.changedToDown(pointerInputChange), is(false));
    }

    @Test
    public void changedToDownIgnoreConsumed_didNotChange_returnsFalse() {
        PointerInputChange pointerInputChange1 =
                createPointerInputChange(0, 0, false, 0, 0, false, 0, 0, false);
        PointerInputChange pointerInputChange2 =
                createPointerInputChange(0, 0, false, 0, 0, true, 0, 0, false);
        PointerInputChange pointerInputChange3 =
                createPointerInputChange(0, 0, true, 0, 0, true, 0, 0, false);

        assertThat(PointerInputChangeEventKt.changedToDownIgnoreConsumed(pointerInputChange1),
                is(false));
        assertThat(PointerInputChangeEventKt.changedToDownIgnoreConsumed(pointerInputChange2),
                is(false));
        assertThat(PointerInputChangeEventKt.changedToDownIgnoreConsumed(pointerInputChange3),
                is(false));
    }

    @Test
    public void changedToDownIgnoreConsumed_changedNotConsumed_returnsTrue() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(0, 0, true, 0, 0, false, 0, 0, false);
        assertThat(PointerInputChangeEventKt.changedToDownIgnoreConsumed(pointerInputChange),
                is(true));
    }

    @Test
    public void changedToDownIgnoreConsumed_changedConsumed_returnsTrue() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(0, 0, true, 0, 0, false, 0, 0, true);
        assertThat(PointerInputChangeEventKt.changedToDownIgnoreConsumed(pointerInputChange),
                is(true));
    }

    @Test
    public void changedToUp_didNotChange_returnsFalse() {
        PointerInputChange pointerInputChange1 =
                createPointerInputChange(0, 0, false, 0, 0, false, 0, 0, false);
        PointerInputChange pointerInputChange2 =
                createPointerInputChange(0, 0, true, 0, 0, false, 0, 0, false);
        PointerInputChange pointerInputChange3 =
                createPointerInputChange(0, 0, true, 0, 0, true, 0, 0, false);

        assertThat(PointerInputChangeEventKt.changedToUp(pointerInputChange1), is(false));
        assertThat(PointerInputChangeEventKt.changedToUp(pointerInputChange2), is(false));
        assertThat(PointerInputChangeEventKt.changedToUp(pointerInputChange3), is(false));
    }

    @Test
    public void changedToUp_changeNotConsumed_returnsTrue() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(0, 0, false, 0, 0, true, 0, 0, false);
        assertThat(PointerInputChangeEventKt.changedToUp(pointerInputChange), is(true));
    }

    @Test
    public void changedToUp_changeNotConsumed_returnsFalse() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(0, 0, false, 0, 0, true, 0, 0, true);
        assertThat(PointerInputChangeEventKt.changedToUp(pointerInputChange), is(false));
    }

    @Test
    public void changedToUpIgnoreConsumed_didNotChange_returnsFalse() {
        PointerInputChange pointerInputChange1 =
                createPointerInputChange(0, 0, false, 0, 0, false, 0, 0, false);
        PointerInputChange pointerInputChange2 =
                createPointerInputChange(0, 0, true, 0, 0, false, 0, 0, false);
        PointerInputChange pointerInputChange3 =
                createPointerInputChange(0, 0, true, 0, 0, true, 0, 0, false);

        assertThat(PointerInputChangeEventKt.changedToUpIgnoreConsumed(pointerInputChange1),
                is(false));
        assertThat(PointerInputChangeEventKt.changedToUpIgnoreConsumed(pointerInputChange2),
                is(false));
        assertThat(PointerInputChangeEventKt.changedToUpIgnoreConsumed(pointerInputChange3),
                is(false));
    }

    @Test
    public void changedToUpIgnoreConsumed_changedNotConsumed_returnsTrue() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(0, 0, false, 0, 0, true, 0, 0, false);
        assertThat(PointerInputChangeEventKt.changedToUpIgnoreConsumed(pointerInputChange),
                is(true));
    }

    @Test
    public void changedToUpIgnoreConsumed_changedConsumed_returnsTrue() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(0, 0, false, 0, 0, true, 0, 0, true);
        assertThat(PointerInputChangeEventKt.changedToUpIgnoreConsumed(pointerInputChange),
                is(true));
    }

    // TODO(shepshapard): Test more variations of positions?

    @Test
    public void positionChange_didNotChange_returnsZeroOffset() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(11, 13, true, 11, 13, true, 0, 0, false);
        assertThat(PointerInputChangeEventKt.positionChange(pointerInputChange),
                is(equalTo(new Offset(0, 0))));
    }

    @Test
    public void positionChange_changedNotConsumed_returnsFullOffset() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(8, 16, true, 2, 4, true, 0, 0, false);
        assertThat(PointerInputChangeEventKt.positionChange(pointerInputChange),
                is(equalTo(new Offset(6, 12))));
    }

    @Test
    public void positionChange_changedPartiallyConsumed_returnsRemainder() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(8, 16, true, 2, 4, true, 5, 9, false);
        assertThat(PointerInputChangeEventKt.positionChange(pointerInputChange),
                is(equalTo(new Offset(1, 3))));
    }

    @Test
    public void positionChange_changedFullConsumed_returnsZeroOffset() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(8, 16, true, 2, 4, true, 6, 12, false);
        assertThat(PointerInputChangeEventKt.positionChange(pointerInputChange),
                is(equalTo(new Offset(0, 0))));
    }

    @Test
    public void positionChangeIgnoreConsumed_didNotChange_returnsZeroOffset() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(11, 13, true, 11, 13, true, 0, 0, false);
        assertThat(PointerInputChangeEventKt.positionChangeIgnoreConsumed(pointerInputChange),
                is(equalTo(new Offset(0, 0))));
    }

    @Test
    public void positionChangeIgnoreConsumed_changedNotConsumed_returnsFullOffset() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(8, 16, true, 2, 4, true, 0, 0, false);
        assertThat(PointerInputChangeEventKt.positionChangeIgnoreConsumed(pointerInputChange),
                is(equalTo(new Offset(6, 12))));
    }

    @Test
    public void positionChangeIgnoreConsumed_changedPartiallyConsumed_returnsFullOffset() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(8, 16, true, 2, 4, true, 5, 9, false);
        assertThat(PointerInputChangeEventKt.positionChangeIgnoreConsumed(pointerInputChange),
                is(equalTo(new Offset(6, 12))));
    }

    @Test
    public void positionChangeIgnoreConsumed_changedFullConsumed_returnsFullOffset() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(8, 16, true, 2, 4, true, 6, 12, false);
        assertThat(PointerInputChangeEventKt.positionChangeIgnoreConsumed(pointerInputChange),
                is(equalTo(new Offset(6, 12))));
    }

    @Test
    public void positionChanged_didNotChange_returnsFalse() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(11, 13, true, 11, 13, true, 0, 0, false);
        assertThat(PointerInputChangeEventKt.positionChanged(pointerInputChange), is(false));
    }

    @Test
    public void positionChanged_changedNotConsumed_returnsTrue() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(8, 16, true, 2, 4, true, 0, 0, false);
        assertThat(PointerInputChangeEventKt.positionChanged(pointerInputChange), is(true));
    }

    @Test
    public void positionChanged_changedPartiallyConsumed_returnsTrue() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(8, 16, true, 2, 4, true, 5, 9, false);
        assertThat(PointerInputChangeEventKt.positionChanged(pointerInputChange), is(true));
    }

    @Test
    public void positionChanged_changedFullConsumed_returnsFalse() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(8, 16, true, 2, 4, true, 6, 12, false);
        assertThat(PointerInputChangeEventKt.positionChanged(pointerInputChange), is(false));
    }

    @Test
    public void positionChangedIgnoreConsumed_didNotChange_returnsFalse() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(11, 13, true, 11, 13, true, 0, 0, false);
        assertThat(PointerInputChangeEventKt.positionChangedIgnoreConsumed(pointerInputChange),
                is(false));
    }

    @Test
    public void positionChangedIgnoreConsumed_changedNotConsumed_returnsTrue() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(8, 16, true, 2, 4, true, 0, 0, false);
        assertThat(PointerInputChangeEventKt.positionChangedIgnoreConsumed(pointerInputChange),
                is(true));
    }

    @Test
    public void positionChangedIgnoreConsumed_changedPartiallyConsumed_returnsTrue() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(8, 16, true, 2, 4, true, 5, 9, false);
        assertThat(PointerInputChangeEventKt.positionChangedIgnoreConsumed(pointerInputChange),
                is(true));
    }

    @Test
    public void positionChangedIgnoreConsumed_changedFullConsumed_returnsTrue() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(8, 16, true, 2, 4, true, 6, 12, false);
        assertThat(PointerInputChangeEventKt.positionChangedIgnoreConsumed(pointerInputChange),
                is(true));
    }

    @Test
    public void anyPositionChangeConsumed_changedNotConsumed_returnsFalse() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(8, 16, true, 2, 4, true, 0, 0, false);
        assertThat(PointerInputChangeEventKt.anyPositionChangeConsumed(pointerInputChange),
                is(false));
    }

    @Test
    public void anyPositionChangeConsumed_changedPartiallyConsumed_returnsTrue() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(8, 16, true, 2, 4, true, 5, 9, false);
        assertThat(PointerInputChangeEventKt.anyPositionChangeConsumed(pointerInputChange),
                is(true));
    }

    @Test
    public void anyPositionChangeConsumed_changedFullConsumed_returnsTrue() {
        PointerInputChange pointerInputChange =
                createPointerInputChange(8, 16, true, 2, 4, true, 6, 12, false);
        assertThat(PointerInputChangeEventKt.anyPositionChangeConsumed(pointerInputChange),
                is(true));
    }

    @Test
    public void consumeDownChange_changeOccurred_consumes() {
        PointerInputChange pointerInputChange1 =
                createPointerInputChange(0, 0, false, 0, 0, true, 0, 0, false);
        PointerInputChange pointerInputChange2 =
                createPointerInputChange(0, 0, true, 0, 0, false, 0, 0, false);

        PointerInputChange pointerInputChange1Result =
                PointerInputChangeEventKt.consumeDownChange(pointerInputChange1);
        PointerInputChange pointerInputChange2Result =
                PointerInputChangeEventKt.consumeDownChange(pointerInputChange2);

        assertThat(pointerInputChange1Result.getConsumed().getDownChange(), is(true));
        assertThat(pointerInputChange2Result.getConsumed().getDownChange(), is(true));
    }

    @Test
    public void consumePositionChange_consumesNone_consumesCorrectly() {
        PointerInputChange pointerInputChange1 =
                createPointerInputChange(8, 16, true, 2, 4, true, 0, 0, false);

        PointerInputChange pointerInputChangeResult1 =
                PointerInputChangeEventKt.consumePositionChange(pointerInputChange1, 0, 0);

        assertThat(pointerInputChangeResult1, is(equalTo(pointerInputChange1)));
    }

    @Test
    public void consumePositionChange_consumesPart_consumes() {
        PointerInputChange pointerInputChange1 =
                createPointerInputChange(8, 16, true, 2, 4, true, 0, 0, false);

        PointerInputChange pointerInputChangeResult1 =
                PointerInputChangeEventKt.consumePositionChange(pointerInputChange1, 5, 0);
        PointerInputChange pointerInputChangeResult2 =
                PointerInputChangeEventKt.consumePositionChange(pointerInputChange1, 0, 3);
        PointerInputChange pointerInputChangeResult3 =
                PointerInputChangeEventKt.consumePositionChange(pointerInputChange1, 5, 3);

        assertThat(pointerInputChangeResult1,
                is(equalTo(createPointerInputChange(8, 16, true, 2, 4, true, 5, 0, false))));
        assertThat(pointerInputChangeResult2,
                is(equalTo(createPointerInputChange(8, 16, true, 2, 4, true, 0, 3, false))));
        assertThat(pointerInputChangeResult3,
                is(equalTo(createPointerInputChange(8, 16, true, 2, 4, true, 5, 3, false))));
    }

    @Test
    public void consumePositionChange_consumesAll_consumes() {
        PointerInputChange pointerInputChange1 =
                createPointerInputChange(8, 16, true, 2, 4, true, 0, 0, false);

        PointerInputChange pointerInputChangeResult1 =
                PointerInputChangeEventKt.consumePositionChange(pointerInputChange1, 6, 0);
        PointerInputChange pointerInputChangeResult2 =
                PointerInputChangeEventKt.consumePositionChange(pointerInputChange1, 0, 12);
        PointerInputChange pointerInputChangeResult3 =
                PointerInputChangeEventKt.consumePositionChange(pointerInputChange1, 6, 12);

        assertThat(pointerInputChangeResult1,
                is(equalTo(createPointerInputChange(8, 16, true, 2, 4, true, 6, 0, false))));
        assertThat(pointerInputChangeResult2,
                is(equalTo(createPointerInputChange(8, 16, true, 2, 4, true, 0, 12, false))));
        assertThat(pointerInputChangeResult3,
                is(equalTo(createPointerInputChange(8, 16, true, 2, 4, true, 6, 12, false))));
    }

    @Test
    public void consumePositionChange_alreadyPartiallyConsumed_consumptionAdded() {
        PointerInputChange pointerInputChange1 =
                createPointerInputChange(8, 16, true, 2, 4, true, 1, 5, false);

        PointerInputChange pointerInputChangeResult1 =
                PointerInputChangeEventKt.consumePositionChange(pointerInputChange1, 2, 0);
        PointerInputChange pointerInputChangeResult2 =
                PointerInputChangeEventKt.consumePositionChange(pointerInputChange1, 0, 3);
        PointerInputChange pointerInputChangeResult3 =
                PointerInputChangeEventKt.consumePositionChange(pointerInputChange1, 2, 3);

        assertThat(pointerInputChangeResult1,
                is(equalTo(createPointerInputChange(8, 16, true, 2, 4, true, 3, 5, false))));
        assertThat(pointerInputChangeResult2,
                is(equalTo(createPointerInputChange(8, 16, true, 2, 4, true, 1, 8, false))));
        assertThat(pointerInputChangeResult3,
                is(equalTo(createPointerInputChange(8, 16, true, 2, 4, true, 3, 8, false))));
    }

    // Private Helper

    private PointerInputChange createPointerInputChange(
            float currentX, float currentY, boolean currentDown,
            float previousX, float previousY, boolean previousDown,
            float consumedX, float consumedY, boolean consumedDown) {
        return new PointerInputChange(
                0,
                new PointerInputData(new Offset(currentX, currentY), currentDown),
                new PointerInputData(new Offset(previousX, previousY), previousDown),
                new ConsumedData(new Offset(consumedX, consumedY), consumedDown)
        );
    }
}
