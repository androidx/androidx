/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.tiles;

import static androidx.wear.tiles.EventBuilders.TileInteractionEvent.ENTER;
import static androidx.wear.tiles.EventBuilders.TileInteractionEvent.LEAVE;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.tiles.proto.EventProto;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
public class EventBuildersTest {
    @Test
    public void canBuildBasicTileEnterEvent() {
        long epochMillis = 12345;
        int tileId = 1;
        EventBuilders.TileInteractionEvent interactionEvent =
                new EventBuilders.TileInteractionEvent.Builder(tileId, ENTER)
                        .setTimestamp(Instant.ofEpochMilli(epochMillis))
                        .build();

        EventProto.TileInteractionEvent eventProto =
                EventProto.TileInteractionEvent.newBuilder()
                        .setEnter(EventProto.TileEnter.newBuilder().build())
                        .setTileId(tileId)
                        .setTimestampEpochMillis(epochMillis)
                        .build();

        assertThat(interactionEvent.toProto()).isEqualTo(eventProto);
        assertThat(interactionEvent.getEventType()).isEqualTo(ENTER);
        assertThat(interactionEvent.getTimestamp().toEpochMilli()).isEqualTo(epochMillis);
        assertThat(interactionEvent.getTileId()).isEqualTo(tileId);
    }

    @Test
    public void canBuildBasicTileLeaveEvent() {
        long epochMillis = 12345;
        int tileId = 1;
        EventBuilders.TileInteractionEvent interactionEvent =
                new EventBuilders.TileInteractionEvent.Builder(tileId, LEAVE)
                        .setTimestamp(Instant.ofEpochMilli(epochMillis))
                        .build();

        EventProto.TileInteractionEvent eventProto =
                EventProto.TileInteractionEvent.newBuilder()
                        .setLeave(EventProto.TileLeave.newBuilder().build())
                        .setTileId(tileId)
                        .setTimestampEpochMillis(epochMillis)
                        .build();

        assertThat(interactionEvent.toProto()).isEqualTo(eventProto);
        assertThat(interactionEvent.getEventType()).isEqualTo(LEAVE);
        assertThat(interactionEvent.getTimestamp().toEpochMilli()).isEqualTo(epochMillis);
        assertThat(interactionEvent.getTileId()).isEqualTo(tileId);
    }

    @Test
    public void defaultTimestampIsSetWhenCreatingTheBuilderInstance() {
        long timestamp1 = 12345L;
        long timestamp2 = 54321L;
        AtomicLong epochMillis = new AtomicLong(timestamp1);
        EventBuilders.TileInteractionEvent.Builder builder =
                new EventBuilders.TileInteractionEvent.Builder(
                        () -> Instant.ofEpochMilli(epochMillis.get()).toEpochMilli(), 1, LEAVE);

        epochMillis.set(timestamp2);
        EventBuilders.TileInteractionEvent interactionEvent = builder.build();

        assertThat(interactionEvent.toProto().getTimestampEpochMillis()).isEqualTo(timestamp1);
    }
}
