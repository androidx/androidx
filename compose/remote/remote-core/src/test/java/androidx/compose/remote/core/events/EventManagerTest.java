/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.core.events;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.operations.FloatExpression;
import androidx.compose.remote.core.operations.ShaderData;
import androidx.compose.remote.core.operations.utilities.ArrayAccess;
import androidx.compose.remote.core.operations.utilities.DataMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

public class EventManagerTest {
    private static final int TYPE1 = 100;
    private static final int TYPE2 = 200;
    private static final int TYPE3 = 300;

    private final EventManager mEventManager = new EventManager();
    private final RemoteContext mContext = new TestRemoteContext();
    private final CoreDocument mDoc = new CoreDocument();

    @Test
    public void noRouterNoHandler() {
        // Arrange.
        Event event = new Event(TYPE1, 0, new float[0]);

        // Act.
        int result = mEventManager.dispatchEvent(mContext, mDoc, event);

        // Assert.
        assertThat(result).isEqualTo(EventManager.STATUS_UNHANDLED);
    }

    @Test
    public void noHandler() {
        // Arrange.
        mEventManager.registerRouter(TYPE1, new TestEventRouter());
        Event event = new Event(TYPE1, 0, new float[0]);

        // Act.
        int result = mEventManager.dispatchEvent(mContext, mDoc, event);

        // Assert.
        assertThat(result).isEqualTo(EventManager.STATUS_UNHANDLED);
    }

    @Test
    public void noRouter() {
        // Arrange.
        mEventManager.registerHandler(new TestHandler(TYPE1));
        Event event = new Event(TYPE1, 0, new float[0]);

        // Act.
        int result = mEventManager.dispatchEvent(mContext, mDoc, event);

        // Assert.
        assertThat(result).isEqualTo(EventManager.STATUS_UNHANDLED);
    }

    @Test
    public void doesNotRouteUnregisteredEventType() {
        // Arrange.
        TestEventRouter router = new TestEventRouter();
        TestHandler handler = new TestHandler(TYPE2);
        mEventManager.registerRouter(TYPE1, router);
        mEventManager.registerHandler(handler);
        Event event = new Event(TYPE2, 0, new float[0]);

        // Act.
        int result = mEventManager.dispatchEvent(mContext, mDoc, event);

        // Assert.
        assertThat(result).isEqualTo(EventManager.STATUS_UNHANDLED);
        assertThat(router.mRouteEventCount).isEqualTo(0);
        assertThat(router.mRegisterHandlerCount).isEqualTo(0);
        assertThat(handler.mMatchesEventCount).isEqualTo(1);
    }

    @Test
    public void routesForRegisteredType() {
        // Arrange.
        TestEventRouter router = new TestEventRouter();
        TestHandler handler = new TestHandler(TYPE1);
        mEventManager.registerRouter(TYPE1, router);
        mEventManager.registerHandler(handler);
        Event event = new Event(TYPE1, 0, new float[0]);

        // Act.
        int result = mEventManager.dispatchEvent(mContext, mDoc, event);

        // Assert.
        assertThat(result).isEqualTo(TestEventRouter.EVENT_ROUTED);
        assertThat(router.mRouteEventCount).isEqualTo(1);
        assertThat(router.mRegisterHandlerCount).isEqualTo(1);
        assertThat(handler.mMatchesEventCount).isEqualTo(1);
    }

    @Test
    public void doesNotRouteUnregisteredEventTypeWithRegisteredHandler() {
        // Arrange.
        TestEventRouter router = new TestEventRouter();
        TestHandler handler = new TestHandler(TYPE1);
        mEventManager.registerRouter(TYPE1, router);
        mEventManager.registerHandler(handler);
        Event event = new Event(TYPE2, 0, new float[0]);

        // Act.
        int result = mEventManager.dispatchEvent(mContext, mDoc, event);

        // Assert.
        assertThat(result).isEqualTo(EventManager.STATUS_UNHANDLED);
        assertThat(router.mRouteEventCount).isEqualTo(0);
        assertThat(router.mRegisterHandlerCount).isEqualTo(1);
        assertThat(handler.mMatchesEventCount).isEqualTo(1);
    }

    @Test
    public void routesMultipleEventsForRegisteredType() {
        // Arrange.
        TestEventRouter router = new TestEventRouter();
        TestHandler handler = new TestHandler(TYPE1);
        mEventManager.registerRouter(TYPE1, router);
        mEventManager.registerHandler(handler);
        Event event1 = new Event(TYPE1, 0, new float[0]);
        Event event2 = new Event(TYPE1, 0, new float[0]);

        // Act.
        int result1 = mEventManager.dispatchEvent(mContext, mDoc, event1);
        int result2 = mEventManager.dispatchEvent(mContext, mDoc, event2);

        // Assert.
        assertThat(result1).isEqualTo(TestEventRouter.EVENT_ROUTED);
        assertThat(result2).isEqualTo(TestEventRouter.EVENT_ROUTED);
        assertThat(router.mRouteEventCount).isEqualTo(2);
        assertThat(router.mRegisterHandlerCount).isEqualTo(1);
        assertThat(handler.mMatchesEventCount).isEqualTo(1);
    }

    @Test
    public void routesMultipleEventsForDifferentRegisteredTypes() {
        // Arrange.
        TestEventRouter router1 = new TestEventRouter();
        TestEventRouter router2 = new TestEventRouter();
        mEventManager.registerRouter(TYPE1, router1);
        mEventManager.registerRouter(TYPE2, router2);
        mEventManager.registerHandler(new TestHandler(TYPE1));
        mEventManager.registerHandler(new TestHandler(TYPE2));
        mEventManager.registerHandler(new TestHandler(TYPE3));
        Event event1 = new Event(TYPE1, 0, new float[0]);
        Event event2 = new Event(TYPE1, 0, new float[0]);
        Event event3 = new Event(TYPE2, 0, new float[0]);
        Event event4 = new Event(TYPE2, 0, new float[0]);
        Event event5 = new Event(TYPE3, 0, new float[0]);
        Event event6 = new Event(TYPE3, 0, new float[0]);

        // Act.
        int result1 = mEventManager.dispatchEvent(mContext, mDoc, event1);
        int result2 = mEventManager.dispatchEvent(mContext, mDoc, event2);
        int result3 = mEventManager.dispatchEvent(mContext, mDoc, event3);
        int result4 = mEventManager.dispatchEvent(mContext, mDoc, event4);
        int result5 = mEventManager.dispatchEvent(mContext, mDoc, event5);
        int result6 = mEventManager.dispatchEvent(mContext, mDoc, event6);

        // Assert.
        assertThat(result1).isEqualTo(TestEventRouter.EVENT_ROUTED);
        assertThat(result2).isEqualTo(TestEventRouter.EVENT_ROUTED);
        assertThat(result3).isEqualTo(TestEventRouter.EVENT_ROUTED);
        assertThat(result4).isEqualTo(TestEventRouter.EVENT_ROUTED);
        assertThat(result5).isEqualTo(EventManager.STATUS_UNHANDLED);
        assertThat(result6).isEqualTo(EventManager.STATUS_UNHANDLED);
        assertThat(router1.mRouteEventCount).isEqualTo(2);
        assertThat(router2.mRouteEventCount).isEqualTo(2);
        assertThat(router1.mRegisterHandlerCount).isEqualTo(1);
        assertThat(router2.mRegisterHandlerCount).isEqualTo(1);
    }

    @Test
    public void preRegisteredHandlerIsRegistered_whenMatchingRouterIsAdded() {
        // Arrange.
        TestEventRouter router = new TestEventRouter();
        TestHandler handler = new TestHandler(TYPE1);
        mEventManager.registerHandler(handler);

        // Act.
        mEventManager.registerRouter(TYPE1, router);

        // Assert.
        assertThat(router.mRegisterHandlerCount).isEqualTo(1);
    }

    @Test
    public void preRegisteredHandlerIsNotRegistered_whenNonMatchingRouterIsAdded() {
        // Arrange.
        TestEventRouter router = new TestEventRouter();
        TestHandler handler = new TestHandler(TYPE2);
        mEventManager.registerHandler(handler);

        // Act.
        mEventManager.registerRouter(TYPE1, router);

        // Assert.
        assertThat(router.mRegisterHandlerCount).isEqualTo(0);
    }

    @Test
    public void registerRouter_throwsException_whenRouterForEventTypeAlreadyExists() {
        // Arrange.
        TestEventRouter router1 = new TestEventRouter();
        TestEventRouter router2 = new TestEventRouter();
        mEventManager.registerRouter(TYPE1, router1);

        // Act.
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                mEventManager.registerRouter(TYPE1, router2)
        );

        // Assert.
        assertThat(exception).hasMessageThat().isEqualTo("Existing router for this event type.");
    }

    private static class TestEventRouter implements EventRouter {

        static final int EVENT_ROUTED = 1;

        int mRouteEventCount = 0;
        int mRegisterHandlerCount = 0;

        @Override
        public void registerHandler(@NonNull EventHandler eventHandler) {
            mRegisterHandlerCount++;
        }

        @Override
        public int routeEvent(
                @NonNull RemoteContext context,
                @NonNull CoreDocument document,
                @NonNull Event event) {
            mRouteEventCount++;
            return mRegisterHandlerCount > 0 ? EVENT_ROUTED : EventManager.STATUS_UNHANDLED;
        }
    }

    private static class TestHandler implements EventHandler {

        private final int mType;

        int mMatchesEventCount = 0;

        TestHandler(int type) {
            mType = type;
        }

        @Override
        public boolean matchesEvent(int eventType) {
            mMatchesEventCount++;
            return eventType == mType;
        }

        @Override
        public int onEvent(
                @NonNull RemoteContext context,
                @NonNull CoreDocument document,
                @NonNull Event event) {
            return 0;
        }
    }

    private static class TestRemoteContext extends RemoteContext {
        @Override
        public void loadPathData(int instanceId, int winding, float @NonNull [] floatPath) {}

        @Override
        public float @Nullable [] getPathData(int instanceId) {
            return null;
        }

        @Override
        public void loadVariableName(@NonNull String varName, int varId, int varType) {}

        @Override
        public void loadColor(int id, int color) {}

        @Override
        public void setNamedColorOverride(@NonNull String colorName, int color) {}

        @Override
        public void setNamedStringOverride(@NonNull String stringName, @NonNull String value) {}

        @Override
        public void clearNamedStringOverride(@NonNull String stringName) {}

        @Override
        public void setNamedBooleanOverride(@NonNull String booleanName, boolean value) {}

        @Override
        public void clearNamedBooleanOverride(@NonNull String booleanName) {}

        @Override
        public void setNamedIntegerOverride(@NonNull String integerName, int value) {}

        @Override
        public void clearNamedIntegerOverride(@NonNull String integerName) {}

        @Override
        public void setNamedFloatOverride(@NonNull String floatName, float value) {}

        @Override
        public void clearNamedFloatOverride(@NonNull String floatName) {}

        @Override
        public void setNamedLong(@NonNull String name, long value) {}

        @Override
        public void setNamedDataOverride(@NonNull String dataName, @NonNull Object value) {}

        @Override
        public void clearNamedDataOverride(@NonNull String dataName) {}

        @Override
        public void addCollection(int id, @NonNull ArrayAccess collection) {}

        @Override
        public void putDataMap(int id, @NonNull DataMap map) {}

        @Override
        public @Nullable DataMap getDataMap(int id) {
            return null;
        }

        @Override
        public void runAction(int id, @NonNull String metadata) {}

        @Override
        public void runNamedAction(int id, @Nullable Object value) {}

        @Override
        public void putObject(int id, @NonNull Object value) {}

        @Override
        public @Nullable Object getObject(int id) {
            return null;
        }

        @Override
        public void hapticEffect(int type) {}

        @Override
        public void loadBitmap(
                int imageId,
                short encoding,
                short type,
                int width,
                int height,
                byte @NonNull [] bitmap) {}

        @Override
        public void loadText(int id, @NonNull String text) {}

        @Override
        public @Nullable String getText(int id) {
            return null;
        }

        @Override
        public void loadFloat(int id, float value) {}

        @Override
        public void overrideFloat(int id, float value) {}

        @Override
        public void loadInteger(int id, int value) {}

        @Override
        public void overrideInteger(int id, int value) {}

        @Override
        public void overrideText(int id, int valueId) {}

        @Override
        public void loadAnimatedFloat(int id, @NonNull FloatExpression animatedFloat) {}

        @Override
        public void loadShader(int id, @NonNull ShaderData value) {}

        @Override
        public float getFloat(int id) {
            return 0f;
        }

        @Override
        public int getInteger(int id) {
            return 0;
        }

        @Override
        public long getLong(int id) {
            return 0;
        }

        @Override
        public int getColor(int id) {
            return 0;
        }

        @Override
        public void listensTo(int id, @NonNull VariableSupport variableSupport) {}

        @Override
        public int updateOps() {
            return 0;
        }

        @Override
        public ShaderData getShader(int id) {
            return null;
        }

        @Override
        public void addClickArea(
                int id,
                int contentId,
                float left,
                float top,
                float right,
                float bottom,
                int metadataId) {}
    }
}
