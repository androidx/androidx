/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.player.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Color;

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Platform;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.core.operations.layout.ClickModifierOperation;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.LayoutComponent;
import androidx.compose.remote.core.operations.layout.TouchCancelModifierOperation;
import androidx.compose.remote.core.operations.layout.TouchDownModifierOperation;
import androidx.compose.remote.core.operations.layout.TouchUpModifierOperation;
import androidx.compose.remote.core.operations.layout.managers.BoxLayout;
import androidx.compose.remote.core.operations.layout.managers.CollapsiblePriority;
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout;
import androidx.compose.remote.core.operations.layout.managers.RowLayout;
import androidx.compose.remote.core.operations.layout.modifiers.ComponentModifiers;
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.ModifierOperation;
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.actions.HostAction;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.modifiers.RoundedRectShape;
import androidx.compose.remote.creation.platform.AndroidxPlatformServices;
import androidx.test.filters.SdkSuppress;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

@SdkSuppress(minSdkVersion = 26) // b/437958945
@RunWith(androidx.test.ext.junit.runners.AndroidJUnit4.class)
public class LayoutTest {

    private static final boolean GENERATE_GOLD_FILES = false;

    @Rule public TestName name = new TestName();

    int mTw = 1000;
    int mTh = 1000;

    Platform mPlatform = new AndroidxPlatformServices();

    class SnapShot {
        float mTime = 0f;
        String mContent = "";

        SnapShot(float time, String content) {
            this.mTime = time;
            this.mContent = content;
        }
    }

    String loadFileFromRaw(Context context, String fileName) {
        try {
            int resourceId =
                    context.getResources().getIdentifier(fileName, "raw", context.getPackageName());
            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    context.getResources().openRawResource(resourceId)));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
            reader.close();
            return stringBuilder.toString();
        } catch (IOException e) {
            return "";
        }
    }

    ArrayList<SnapShot> loadSnapshotsFromRaw(Context context, String fileName) {
        ArrayList<SnapShot> snapshots = new ArrayList<>();
        try {
            int resourceId =
                    context.getResources().getIdentifier(fileName, "raw", context.getPackageName());
            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    context.getResources().openRawResource(resourceId)));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("SNAPSHOT")) {
                    float time = Float.parseFloat(line.substring("SNAPSHOT =".length()));
                    snapshots.add(new SnapShot(time, stringBuilder.toString()));
                    stringBuilder = new StringBuilder();
                } else {
                    stringBuilder.append(line);
                    stringBuilder.append("\n");
                }
            }
            reader.close();
        } catch (IOException e) {
        }
        return snapshots;
    }

    void baseTest(RemoteComposeWriter writer, int tw, int th) {
        byte[] buffer = writer.buffer();
        int bufferSize = writer.bufferSize();
        RemoteComposeDocument doc =
                new RemoteComposeDocument(new ByteArrayInputStream(buffer, 0, bufferSize));

        DebugPlayerContext debugContext = new DebugPlayerContext();
        debugContext.mWidth = tw;
        debugContext.mHeight = th;
        doc.initializeContext(debugContext);
        doc.paint(debugContext, Theme.UNSPECIFIED);
        String resLayout = doc.getDocument().getRootLayoutComponent().displayHierarchy();

        String testName = name.getMethodName();
        String fileName = "layout_" + testName.substring("test".length()).toLowerCase();
        if (GENERATE_GOLD_FILES) {
            writeToFile(resLayout, fileName);
        } else {
            String expected = loadFileFromRaw(CtsTest.sAppContext, fileName);
            System.out.println("Found:\n" + resLayout);
            assertEquals("did not match ", expected, resLayout);
        }
    }

    void writeToFile(String content, String fileName) {
        File file = new File(CtsTest.sAppContext.getFilesDir(), fileName + ".layout");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.write(content);
            osw.flush();
            osw.close();
            fos.close();
        } catch (Exception e) {
            // handle exception
        }
    }

    void baseTestResize(RemoteComposeWriter writer, int tw1, int th1, int tw2, int th2) {
        byte[] buffer = writer.buffer();
        int bufferSize = writer.bufferSize();
        RemoteComposeDocument doc =
                new RemoteComposeDocument(new ByteArrayInputStream(buffer, 0, bufferSize));

        DebugPlayerContext debugContext = new DebugPlayerContext();
        debugContext.setAnimationEnabled(false);
        debugContext.setDensity(1f);
        debugContext.mWidth = tw1;
        debugContext.mHeight = th1;
        doc.initializeContext(debugContext);
        doc.paint(debugContext, Theme.UNSPECIFIED);
        debugContext.mWidth = tw2;
        debugContext.mHeight = th2;
        doc.paint(debugContext, Theme.UNSPECIFIED);
        String resLayout = doc.getDocument().getRootLayoutComponent().displayHierarchy();

        String testName = name.getMethodName();
        String fileName = "layout_" + testName.substring("test".length()).toLowerCase();
        if (GENERATE_GOLD_FILES) {
            writeToFile(resLayout, fileName);
        } else {
            String expected = loadFileFromRaw(CtsTest.sAppContext, fileName);
            System.out.println("Found:\n" + resLayout);
            assertEquals("did not match ", expected, resLayout);
        }
    }

    class TestComponentOperation {
        int mComponentId;
        CoreDocument mDocument;
        RemoteContext mContext;

        TestComponentOperation(int id) {
            mComponentId = id;
        }

        public int getComponentId() {
            return mComponentId;
        }

        public void apply(Component component) {}
    }

    class TestComponentResizeOperation extends TestComponentOperation {
        int mWidth = -1;
        int mHeight = -1;

        TestComponentResizeOperation(int id) {
            super(id);
        }

        public TestComponentOperation setWidth(int width) {
            mWidth = width;
            return this;
        }

        public TestComponentOperation setHeight(int height) {
            mHeight = height;
            return this;
        }

        public TestComponentOperation setSize(int width, int height) {
            mWidth = width;
            mHeight = height;
            return this;
        }

        @Override
        public void apply(Component component) {
            ArrayList<Operation> ops = component.getList();
            for (int i = 0; i < ops.size(); i++) {
                Operation op = ops.get(i);
                if (op instanceof ComponentModifiers) {
                    ArrayList<ModifierOperation> mods = ((ComponentModifiers) op).getList();
                    for (int j = 0; j < mods.size(); j++) {
                        ModifierOperation m = mods.get(j);
                        if (mWidth != -1 && m instanceof WidthModifierOperation) {
                            ((WidthModifierOperation) m).setValue(mWidth);
                        }
                        if (mHeight != -1 && m instanceof HeightModifierOperation) {
                            ((HeightModifierOperation) m).setValue(mHeight);
                        }
                    }
                }
            }
            component.invalidateMeasure();
        }
    }

    class TestComponentClickOperation extends TestComponentOperation {

        TestComponentClickOperation(int id) {
            super(id);
        }

        @Override
        public void apply(Component component) {
            ArrayList<Operation> ops = component.getList();
            for (int i = 0; i < ops.size(); i++) {
                Operation op = ops.get(i);
                if (op instanceof ComponentModifiers) {
                    ArrayList<ModifierOperation> mods = ((ComponentModifiers) op).getList();
                    for (int j = 0; j < mods.size(); j++) {
                        ModifierOperation m = mods.get(j);
                        if (m instanceof ClickModifierOperation) {
                            ((ClickModifierOperation) m)
                                    .onClick(mContext, mDocument, component, 10f, 10f);
                        }
                    }
                }
            }
            component.invalidateMeasure();
        }
    }

    class TestComponentTouchDownOperation extends TestComponentOperation {
        float mX;
        float mY;

        TestComponentTouchDownOperation(int id, float x, float y) {
            super(id);
            mX = x;
            mY = y;
        }

        @Override
        public void apply(Component component) {
            ArrayList<Operation> ops = component.getList();
            for (int i = 0; i < ops.size(); i++) {
                Operation op = ops.get(i);
                if (op instanceof ComponentModifiers) {
                    ArrayList<ModifierOperation> mods = ((ComponentModifiers) op).getList();
                    for (int j = 0; j < mods.size(); j++) {
                        ModifierOperation m = mods.get(j);
                        if (m instanceof TouchDownModifierOperation) {
                            ((TouchDownModifierOperation) m)
                                    .onTouchDown(mContext, mDocument, component, mX, mY);
                        }
                    }
                }
            }
            component.invalidateMeasure();
        }
    }

    class TestComponentTouchUpOperation extends TestComponentOperation {
        float mX;
        float mY;

        TestComponentTouchUpOperation(int id, float x, float y) {
            super(id);
            mX = x;
            mY = y;
        }

        @Override
        public void apply(Component component) {
            ArrayList<Operation> ops = component.getList();
            for (int i = 0; i < ops.size(); i++) {
                Operation op = ops.get(i);
                if (op instanceof ComponentModifiers) {
                    ArrayList<ModifierOperation> mods = ((ComponentModifiers) op).getList();
                    for (int j = 0; j < mods.size(); j++) {
                        ModifierOperation m = mods.get(j);
                        if (m instanceof TouchUpModifierOperation) {
                            ((TouchUpModifierOperation) m)
                                    .onTouchUp(mContext, mDocument, component, mX, mY, 0, 0);
                        }
                    }
                }
            }
            component.invalidateMeasure();
        }
    }

    class TestComponentTouchCancelOperation extends TestComponentOperation {
        float mX;
        float mY;

        TestComponentTouchCancelOperation(int id, float x, float y) {
            super(id);
            mX = x;
            mY = y;
        }

        @Override
        public void apply(Component component) {
            ArrayList<Operation> ops = component.getList();
            for (int i = 0; i < ops.size(); i++) {
                Operation op = ops.get(i);
                if (op instanceof ComponentModifiers) {
                    ArrayList<ModifierOperation> mods = ((ComponentModifiers) op).getList();
                    for (int j = 0; j < mods.size(); j++) {
                        ModifierOperation m = mods.get(j);
                        if (m instanceof TouchCancelModifierOperation) {
                            ((TouchCancelModifierOperation) m)
                                    .onTouchCancel(mContext, mDocument, component, mX, mY);
                        }
                    }
                }
            }
            component.invalidateMeasure();
        }
    }

    class TestComponentHorizontalWeightOperation extends TestComponentOperation {
        float mWeight = Float.NaN;

        TestComponentHorizontalWeightOperation(int id, float weight) {
            super(id);
            mWeight = weight;
        }

        @Override
        public void apply(Component component) {
            if (component instanceof LayoutComponent) {
                LayoutComponent lc = (LayoutComponent) component;
                lc.getWidthModifier().setValue(mWeight);
            }
            component.invalidateMeasure();
        }
    }

    class TestComponentVerticalWeightOperation extends TestComponentOperation {
        float mWeight = Float.NaN;

        TestComponentVerticalWeightOperation(int id, float weight) {
            super(id);
            mWeight = weight;
        }

        @Override
        public void apply(Component component) {
            if (component instanceof LayoutComponent) {
                LayoutComponent lc = (LayoutComponent) component;
                lc.getHeightModifier().setValue(mWeight);
            }
            component.invalidateMeasure();
        }
    }

    class TestComponentNeedsMeasure extends TestComponentOperation {
        boolean mNeedsMeasureCheck = false;

        TestComponentNeedsMeasure(int id, boolean needsMeasureCheck) {
            super(id);
            mNeedsMeasureCheck = needsMeasureCheck;
        }

        @Override
        public void apply(Component component) {
            assertEquals(
                    "for component " + component.getComponentId(),
                    mNeedsMeasureCheck,
                    component.mNeedsMeasure);
        }
    }

    class TestRootNeedsMeasure extends TestComponentNeedsMeasure {
        TestRootNeedsMeasure(int id, boolean needsMeasureCheck) {
            super(id, needsMeasureCheck);
        }

        @Override
        public void apply(Component component) {
            try {
                super.apply(component.getRoot());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    class TestComponentNeedsRepaint extends TestComponentOperation {
        boolean mNeedsRepaintCheck = false;

        TestComponentNeedsRepaint(int id, boolean needsRepaintCheck) {
            super(id);
            mNeedsRepaintCheck = needsRepaintCheck;
        }

        @Override
        public void apply(Component component) {
            assertEquals(
                    "for component " + component.getComponentId(),
                    mNeedsRepaintCheck,
                    component.mNeedsRepaint);
        }
    }

    class TestRootNeedsRepaint extends TestComponentNeedsRepaint {
        TestRootNeedsRepaint(int id, boolean needsRepaintCheck) {
            super(id, needsRepaintCheck);
        }

        @Override
        public void apply(Component component) {
            try {
                super.apply(component.getRoot());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    RemoteContext baseTestComponent(
            RemoteComposeWriter writer, int tw1, int th1, ArrayList<TestComponentOperation> ops) {
        return baseTestComponent(writer, tw1, th1, ops, new ArrayList<>());
    }

    RemoteContext baseTestComponent(
            RemoteComposeWriter writer,
            int tw1,
            int th1,
            ArrayList<TestComponentOperation> ops,
            ArrayList<TestComponentOperation> postPaintOps) {
        byte[] buffer = writer.buffer();
        int bufferSize = writer.bufferSize();
        RemoteComposeDocument doc =
                new RemoteComposeDocument(new ByteArrayInputStream(buffer, 0, bufferSize));

        DebugPlayerContext debugContext = new DebugPlayerContext();
        debugContext.setAnimationEnabled(false);
        debugContext.setDensity(1f);
        debugContext.mWidth = tw1;
        debugContext.mHeight = th1;
        doc.initializeContext(debugContext);
        doc.paint(debugContext, Theme.UNSPECIFIED);
        for (TestComponentOperation op : ops) {
            Component component = doc.getComponent(op.getComponentId());
            assertTrue("no available component for " + op.getComponentId(), component != null);
            op.mContext = debugContext;
            op.mDocument = doc.getDocument();
            op.apply(component);
        }
        doc.paint(debugContext, Theme.UNSPECIFIED);
        for (TestComponentOperation op : postPaintOps) {
            Component component = doc.getComponent(op.getComponentId());
            assertTrue("no available component for " + op.getComponentId(), component != null);
            op.mContext = debugContext;
            op.mDocument = doc.getDocument();
            op.apply(component);
        }

        String resLayout = doc.getDocument().getRootLayoutComponent().displayHierarchy();

        String testName = name.getMethodName();
        String fileName = "layout_" + testName.substring("test".length()).toLowerCase();
        if (GENERATE_GOLD_FILES) {
            writeToFile(resLayout, fileName);
        } else {
            String expected = loadFileFromRaw(CtsTest.sAppContext, fileName);
            System.out.println("Found:\n" + resLayout);
            assertEquals("did not match ", expected, resLayout);
        }
        return debugContext;
    }

    static class AnimationSnapshot {
        int mWidth;
        int mHeight;
        long mTime;

        AnimationSnapshot(int w, int h, long time) {
            this.mWidth = w;
            this.mHeight = h;
            this.mTime = time;
        }

        int getWidth() {
            return mWidth;
        }

        int getHeight() {
            return mHeight;
        }

        long getTime() {
            return mTime;
        }
    }

    void baseTestAnimatedResize(
            RemoteComposeWriter writer, ArrayList<AnimationSnapshot> snapshots) {
        baseTestAnimatedResize(writer, snapshots, true);
    }

    void baseTestAnimatedResize(
            RemoteComposeWriter writer,
            ArrayList<AnimationSnapshot> snapshots,
            boolean animationEnabled) {
        String testName = name.getMethodName();
        String fileName = "layout_" + testName.substring("test".length()).toLowerCase();

        byte[] buffer = writer.buffer();
        int bufferSize = writer.bufferSize();
        RemoteComposeDocument doc =
                new RemoteComposeDocument(new ByteArrayInputStream(buffer, 0, bufferSize));

        DebugPlayerContext debugContext = new DebugPlayerContext();
        debugContext.setAnimationEnabled(animationEnabled);
        debugContext.setDensity(1f);
        doc.initializeContext(debugContext);

        if (GENERATE_GOLD_FILES) {
            StringBuilder result = new StringBuilder();
            for (AnimationSnapshot snapshot : snapshots) {
                debugContext.mWidth = snapshot.getWidth();
                debugContext.mHeight = snapshot.getHeight();
                debugContext.currentTime = snapshot.getTime();
                doc.paint(debugContext, Theme.UNSPECIFIED);
                String resLayout = doc.getDocument().getRootLayoutComponent().displayHierarchy();
                result.append(resLayout);
                result.append("SNAPSHOT = " + snapshot.getTime() + "\n");
            }
            writeToFile(result.toString(), fileName);
        } else {
            ArrayList<SnapShot> targetSnapshots =
                    loadSnapshotsFromRaw(CtsTest.sAppContext, fileName);
            int index = 0;
            for (AnimationSnapshot snapshot : snapshots) {
                debugContext.mWidth = snapshot.getWidth();
                debugContext.mHeight = snapshot.getHeight();
                debugContext.currentTime = snapshot.getTime();
                doc.paint(debugContext, Theme.UNSPECIFIED);
                String resLayout = doc.getDocument().getRootLayoutComponent().displayHierarchy();
                SnapShot targetSnapshot = targetSnapshots.get(index);
                String expected = targetSnapshot.mContent;
                assertEquals("did not match snapshot " + index, expected, resLayout);
                index++;
            }
        }
    }

    @Test
    public void testColumn1() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier().fillMaxSize(),
                            ColumnLayout.CENTER,
                            ColumnLayout.CENTER,
                            () -> {
                                writer.box(new RecordingModifier().size(100).background(Color.RED));
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRow1() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier().fillMaxSize(),
                            RowLayout.CENTER,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(new RecordingModifier().size(100).background(Color.RED));
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRow2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier(),
                            RowLayout.CENTER,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(new RecordingModifier().size(100).background(Color.RED));
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testBox1() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(new RecordingModifier().padding(8).size(100).background(Color.RED));
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testBox2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(new RecordingModifier().size(100).padding(8).background(Color.RED));
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testResize1() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier().fillMaxSize().padding(8).background(Color.RED));
                });
        baseTestResize(writer, mTw, mTh, 1200, 1300);
    }

    @Test
    public void testResize2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier().fillMaxSize(),
                            ColumnLayout.CENTER,
                            ColumnLayout.CENTER,
                            () -> {
                                writer.box(new RecordingModifier().size(100).background(Color.RED));
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.BLUE));
                            });
                });
        baseTestResize(writer, mTw, mTh, 1200, 1300);
    }

    @Test
    public void testResize_anim1() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier().fillMaxSize().padding(8).background(Color.RED));
                });
        ArrayList<AnimationSnapshot> snapshots = new ArrayList<>();
        snapshots.add(new AnimationSnapshot(mTw, mTh, 0));
        snapshots.add(new AnimationSnapshot(1200, 1300, 0));
        snapshots.add(new AnimationSnapshot(1200, 1300, 150));
        snapshots.add(new AnimationSnapshot(1200, 1300, 300));
        baseTestAnimatedResize(writer, snapshots);
    }

    @Test
    public void testResize_anim2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier().fillMaxSize(),
                            ColumnLayout.CENTER,
                            ColumnLayout.CENTER,
                            () -> {
                                writer.box(new RecordingModifier().size(100).background(Color.RED));
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.BLUE));
                            });
                });
        ArrayList<AnimationSnapshot> snapshots = new ArrayList<>();
        snapshots.add(new AnimationSnapshot(mTw, mTh, 0));
        snapshots.add(new AnimationSnapshot(1200, 1300, 0));
        snapshots.add(new AnimationSnapshot(1200, 1300, 150));
        snapshots.add(new AnimationSnapshot(1200, 1300, 300));
        baseTestAnimatedResize(writer, snapshots);
    }

    @Test
    public void testResize_anim3() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier().fillMaxSize(),
                            ColumnLayout.CENTER,
                            ColumnLayout.CENTER,
                            () -> {
                                writer.box(new RecordingModifier().size(100).background(Color.RED));
                                writer.row(
                                        new RecordingModifier().fillMaxWidth(),
                                        RowLayout.CENTER,
                                        RowLayout.CENTER,
                                        () -> {
                                            writer.box(
                                                    new RecordingModifier()
                                                            .size(50)
                                                            .background(Color.YELLOW));
                                            writer.box(
                                                    new RecordingModifier()
                                                            .size(75)
                                                            .background(Color.YELLOW));
                                            writer.box(
                                                    new RecordingModifier()
                                                            .size(50)
                                                            .background(Color.YELLOW));
                                        });
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.BLUE));
                            });
                });
        ArrayList<AnimationSnapshot> snapshots = new ArrayList<>();
        snapshots.add(new AnimationSnapshot(mTw, mTh, 0));
        snapshots.add(new AnimationSnapshot(1200, 1300, 0));
        snapshots.add(new AnimationSnapshot(1200, 1300, 150));
        snapshots.add(new AnimationSnapshot(1200, 1300, 300));
        baseTestAnimatedResize(writer, snapshots);
    }

    @Test
    public void testResize_collapsibleRow() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.collapsibleRow(
                            new RecordingModifier()
                                    .fillMaxSize()
                                    .padding(8)
                                    .background(Color.RED)
                                    .componentId(42),
                            ColumnLayout.START,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.YELLOW));
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.BLUE));
                            });
                });

        ArrayList<AnimationSnapshot> snapshots = new ArrayList<>();
        snapshots.add(new AnimationSnapshot(mTw, mTh, 0));
        snapshots.add(new AnimationSnapshot(200, mTh, 500));
        snapshots.add(new AnimationSnapshot(1200, mTh, 1000));
        baseTestAnimatedResize(writer, snapshots, false);
    }

    @Test
    public void testResize_collapsibleRow2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.collapsibleRow(
                            new RecordingModifier()
                                    .fillMaxHeight()
                                    .padding(8)
                                    .background(Color.RED)
                                    .componentId(42),
                            ColumnLayout.START,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.YELLOW));
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.BLUE));
                            });
                });

        ArrayList<AnimationSnapshot> snapshots = new ArrayList<>();
        snapshots.add(new AnimationSnapshot(mTw, mTh, 0));
        snapshots.add(new AnimationSnapshot(200, mTh, 500));
        snapshots.add(new AnimationSnapshot(1200, mTh, 1000));
        baseTestAnimatedResize(writer, snapshots, false);
    }

    @Test
    public void testResize_collapsiblePriorityRow() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.collapsibleRow(
                            new RecordingModifier()
                                    .fillMaxHeight()
                                    .padding(8)
                                    .background(Color.RED)
                                    .componentId(42),
                            ColumnLayout.START,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(100)
                                                .background(Color.YELLOW)
                                                .collapsiblePriority(
                                                        CollapsiblePriority.HORIZONTAL, 0f));
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.BLUE));
                            });
                });

        ArrayList<AnimationSnapshot> snapshots = new ArrayList<>();
        snapshots.add(new AnimationSnapshot(mTw, mTh, 0));
        snapshots.add(new AnimationSnapshot(250, mTh, 500));
        snapshots.add(new AnimationSnapshot(1200, mTh, 1000));
        baseTestAnimatedResize(writer, snapshots, false);
    }

    @Test
    public void testResize_collapsibleColumn() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.collapsibleColumn(
                            new RecordingModifier()
                                    .fillMaxSize()
                                    .padding(8)
                                    .background(Color.RED)
                                    .componentId(42),
                            ColumnLayout.START,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.YELLOW));
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.BLUE));
                            });
                });

        ArrayList<AnimationSnapshot> snapshots = new ArrayList<>();
        snapshots.add(new AnimationSnapshot(mTw, mTh, 0));
        snapshots.add(new AnimationSnapshot(mTw, 200, 500));
        snapshots.add(new AnimationSnapshot(mTw, 1200, 1000));
        baseTestAnimatedResize(writer, snapshots, false);
    }

    @Test
    public void testResize_collapsibleColumn2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.collapsibleColumn(
                            new RecordingModifier()
                                    .fillMaxWidth()
                                    .padding(8)
                                    .background(Color.RED)
                                    .componentId(42),
                            ColumnLayout.START,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.YELLOW));
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.BLUE));
                            });
                });

        ArrayList<AnimationSnapshot> snapshots = new ArrayList<>();
        snapshots.add(new AnimationSnapshot(mTw, mTh, 0));
        snapshots.add(new AnimationSnapshot(mTw, 200, 500));
        snapshots.add(new AnimationSnapshot(mTw, 1200, 1000));
        baseTestAnimatedResize(writer, snapshots, false);
    }

    @Test
    public void testResize_collapsiblePriorityColumn() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.collapsibleColumn(
                            new RecordingModifier()
                                    .fillMaxWidth()
                                    .padding(8)
                                    .background(Color.RED)
                                    .componentId(42),
                            ColumnLayout.START,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(100)
                                                .background(Color.YELLOW)
                                                .collapsiblePriority(
                                                        CollapsiblePriority.VERTICAL, 0f));
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.BLUE));
                            });
                });

        ArrayList<AnimationSnapshot> snapshots = new ArrayList<>();
        snapshots.add(new AnimationSnapshot(mTw, mTh, 0));
        snapshots.add(new AnimationSnapshot(mTw, 250, 500));
        snapshots.add(new AnimationSnapshot(mTw, 1200, 1000));
        baseTestAnimatedResize(writer, snapshots, false);
    }

    @Test
    public void testResize_collapsibleRowConstrained() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.collapsibleRow(
                            new RecordingModifier()
                                    .fillMaxSize()
                                    .padding(8)
                                    .background(Color.RED)
                                    .componentId(42),
                            ColumnLayout.START,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .height(100)
                                                .horizontalWeight(1f)
                                                .widthIn(100, Float.MAX_VALUE)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.BLUE));
                            });
                });

        ArrayList<AnimationSnapshot> snapshots = new ArrayList<>();
        snapshots.add(new AnimationSnapshot(mTw, mTh, 0));
        snapshots.add(new AnimationSnapshot(150, mTh, 500));
        snapshots.add(new AnimationSnapshot(1200, mTh, 1000));
        baseTestAnimatedResize(writer, snapshots, false);
    }

    @Test
    public void testResize_collapsibleColumnConstrained() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.collapsibleColumn(
                            new RecordingModifier()
                                    .fillMaxSize()
                                    .padding(8)
                                    .background(Color.RED)
                                    .componentId(42),
                            ColumnLayout.START,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .width(100)
                                                .verticalWeight(1f)
                                                .heightIn(100, Float.MAX_VALUE)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier().size(100).background(Color.BLUE));
                            });
                });

        ArrayList<AnimationSnapshot> snapshots = new ArrayList<>();
        snapshots.add(new AnimationSnapshot(mTw, mTh, 0));
        snapshots.add(new AnimationSnapshot(mTw, 150, 500));
        snapshots.add(new AnimationSnapshot(mTw, 1200, 1000));
        baseTestAnimatedResize(writer, snapshots, false);
    }

    @Test
    public void testFillMaxWidth1() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier()
                                    .fillMaxWidth()
                                    .height(100)
                                    .background(Color.RED));
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testFillMaxWidth2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier()
                                    .fillMaxWidth()
                                    .height(100)
                                    .background(Color.RED));
                });
        baseTestResize(writer, mTw, mTh, 1200, 1300);
    }

    @Test
    public void testFillMaxHeight1() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier()
                                    .fillMaxHeight()
                                    .width(100)
                                    .background(Color.RED));
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testFillMaxHeight2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier()
                                    .fillMaxHeight()
                                    .width(100)
                                    .background(Color.RED));
                });
        baseTestResize(writer, mTw, mTh, 1200, 1300);
    }

    @Test
    public void testFillMaxSize1() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(new RecordingModifier().fillMaxSize().background(Color.RED));
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testFillMaxSize2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(new RecordingModifier().fillMaxSize().background(Color.RED));
                });
        baseTestResize(writer, mTw, mTh, 1200, 1300);
    }

    @Test
    public void testSize1() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(new RecordingModifier().size(100).background(Color.RED));
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testSize2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(new RecordingModifier().size(100, 80).background(Color.RED));
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testWidthHeight() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier().width(100).height(150).background(Color.RED));
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testBoxWrap1() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier().background(Color.RED),
                            BoxLayout.CENTER,
                            BoxLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testBoxWrap2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier().background(Color.RED),
                            BoxLayout.CENTER,
                            BoxLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .padding(8)
                                                .size(100, 80)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testBoxWrap3() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier().background(Color.RED),
                            BoxLayout.CENTER,
                            BoxLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .padding(8)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testBoxWrap4() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier().background(Color.RED),
                            BoxLayout.CENTER,
                            BoxLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnWrap1() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier().background(Color.RED),
                            ColumnLayout.CENTER,
                            ColumnLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnWrap2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier().background(Color.RED),
                            ColumnLayout.CENTER,
                            ColumnLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .padding(8)
                                                .size(100, 80)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnWrap3() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier().background(Color.RED),
                            ColumnLayout.CENTER,
                            ColumnLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .padding(8)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnWrap4() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier().background(Color.RED),
                            ColumnLayout.CENTER,
                            ColumnLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnWrap5() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier().background(Color.RED).padding(8),
                            ColumnLayout.CENTER,
                            ColumnLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnWrap6() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier().background(Color.RED).padding(8, 9, 10, 11),
                            ColumnLayout.CENTER,
                            ColumnLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowWrap1() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier().background(Color.RED),
                            RowLayout.CENTER,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowWrap2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier().background(Color.RED),
                            RowLayout.CENTER,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .padding(8)
                                                .size(100, 80)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowWrap3() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier().background(Color.RED),
                            RowLayout.CENTER,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .padding(8)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowWrap4() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier().background(Color.RED),
                            RowLayout.CENTER,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowWrap5() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier().background(Color.RED).padding(8),
                            RowLayout.CENTER,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowWrap6() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier().background(Color.RED).padding(8, 9, 10, 11),
                            RowLayout.CENTER,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowPadding1() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier()
                                    .fillMaxWidth()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            RowLayout.START,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .padding(8)
                                                .size(64, 64)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .padding(8)
                                                .fillMaxWidth()
                                                .height(70)
                                                .background(Color.GREEN));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowPadding2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier()
                                    .fillMaxWidth()
                                    .background(Color.YELLOW)
                                    .padding(40),
                            RowLayout.START,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .padding(8)
                                                .size(85, 85)
                                                .background(Color.RED),
                                        BoxLayout.START,
                                        BoxLayout.CENTER,
                                        () -> {
                                            writer.box(
                                                    new RecordingModifier()
                                                            .padding(8)
                                                            .fillMaxWidth()
                                                            .height(70)
                                                            .background(Color.GREEN));
                                        });
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnPadding1() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier()
                                    .fillMaxHeight()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            ColumnLayout.CENTER,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .padding(8)
                                                .size(64, 64)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .padding(8)
                                                .fillMaxHeight()
                                                .width(70)
                                                .background(Color.GREEN));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnPadding2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier()
                                    .fillMaxHeight()
                                    .background(Color.YELLOW)
                                    .padding(40),
                            ColumnLayout.CENTER,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .padding(8)
                                                .size(85, 85)
                                                .background(Color.RED),
                                        BoxLayout.START,
                                        BoxLayout.CENTER,
                                        () -> {
                                            writer.box(
                                                    new RecordingModifier()
                                                            .padding(8)
                                                            .fillMaxHeight()
                                                            .width(70)
                                                            .background(Color.GREEN));
                                        });
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowArrange1() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier()
                                    .fillMaxWidth()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            RowLayout.START,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowArrange2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier()
                                    .fillMaxWidth()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            RowLayout.CENTER,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowArrange3() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier()
                                    .fillMaxWidth()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            RowLayout.END,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowArrange4() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier()
                                    .fillMaxWidth()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            RowLayout.SPACE_AROUND,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowArrange5() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier()
                                    .fillMaxWidth()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            RowLayout.SPACE_BETWEEN,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowArrange6() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier()
                                    .fillMaxWidth()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            RowLayout.SPACE_EVENLY,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnArrange1() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier()
                                    .fillMaxHeight()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            ColumnLayout.CENTER,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnArrange2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier()
                                    .fillMaxHeight()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            ColumnLayout.CENTER,
                            ColumnLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnArrange3() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier()
                                    .fillMaxHeight()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            ColumnLayout.CENTER,
                            ColumnLayout.BOTTOM,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnArrange4() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier()
                                    .fillMaxHeight()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            ColumnLayout.CENTER,
                            ColumnLayout.SPACE_AROUND,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnArrange5() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier()
                                    .fillMaxHeight()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            ColumnLayout.CENTER,
                            ColumnLayout.SPACE_BETWEEN,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnArrange6() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier()
                                    .fillMaxHeight()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            ColumnLayout.CENTER,
                            ColumnLayout.SPACE_EVENLY,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testBoxArrange1() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier().fillMaxSize().background(Color.RED).padding(8),
                            BoxLayout.START,
                            BoxLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 80)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testBoxArrange2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier().fillMaxSize().background(Color.RED).padding(8),
                            BoxLayout.CENTER,
                            BoxLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 80)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testBoxArrange3() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier().fillMaxSize().background(Color.RED).padding(8),
                            BoxLayout.END,
                            BoxLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 80)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testBoxArrange4() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier().fillMaxSize().background(Color.RED).padding(8),
                            BoxLayout.START,
                            BoxLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 80)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testBoxArrange5() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier().fillMaxSize().background(Color.RED).padding(8),
                            BoxLayout.CENTER,
                            BoxLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 80)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testBoxArrange6() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier().fillMaxSize().background(Color.RED).padding(8),
                            BoxLayout.END,
                            BoxLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 80)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testBoxArrange7() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier().fillMaxSize().background(Color.RED).padding(8),
                            BoxLayout.START,
                            BoxLayout.BOTTOM,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 80)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testBoxArrange8() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier().fillMaxSize().background(Color.RED).padding(8),
                            BoxLayout.CENTER,
                            BoxLayout.BOTTOM,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 80)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testBoxArrange9() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier().fillMaxSize().background(Color.RED).padding(8),
                            BoxLayout.END,
                            BoxLayout.BOTTOM,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 80)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowSpacedBy1() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier()
                                    .fillMaxWidth()
                                    .spacedBy(11)
                                    .background(Color.YELLOW)
                                    .padding(8),
                            RowLayout.START,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowSpacedBy2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier()
                                    .spacedBy((float) 11)
                                    .background(Color.YELLOW)
                                    .padding(8),
                            RowLayout.START,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnSpacedBy1() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier()
                                    .fillMaxHeight()
                                    .spacedBy(11)
                                    .background(Color.YELLOW)
                                    .padding(8),
                            ColumnLayout.CENTER,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnSpacedBy2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier()
                                    .spacedBy((float) 11)
                                    .background(Color.YELLOW)
                                    .padding(8),
                            ColumnLayout.CENTER,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowWeight1() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier()
                                    .fillMaxWidth()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            RowLayout.START,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .horizontalWeight(1f)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowWeight2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier()
                                    .fillMaxWidth()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            RowLayout.START,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .height(90)
                                                .horizontalWeight(1f)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowWeight3() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier()
                                    .fillMaxWidth()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            RowLayout.START,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .height(80)
                                                .horizontalWeight(1f)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .height(90)
                                                .horizontalWeight(1f)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowWeight4() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier()
                                    .fillMaxWidth()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            RowLayout.START,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .height(80)
                                                .horizontalWeight(1f)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .height(90)
                                                .horizontalWeight(1f)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .height(100)
                                                .horizontalWeight(1f)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowWeight5() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier()
                                    .fillMaxWidth()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            RowLayout.START,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .height(80)
                                                .horizontalWeight(1f)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .componentId(42)
                                                .height(90)
                                                .horizontalWeight(1f)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .height(100)
                                                .horizontalWeight(1f)
                                                .background(Color.BLUE));
                            });
                });
        ArrayList<TestComponentOperation> ops = new ArrayList<>();
        ops.add(new TestComponentHorizontalWeightOperation(42, 2f));
        baseTestComponent(writer, mTw, mTh, ops);
    }

    @Test
    public void testRowWeight6() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier()
                                    .fillMaxWidth()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            RowLayout.START,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(new RecordingModifier().size(80).background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .componentId(42)
                                                .height(90)
                                                .horizontalWeight(1f)
                                                .background(Color.GREEN),
                                        BoxLayout.CENTER,
                                        BoxLayout.CENTER,
                                        () -> {
                                            writer.box(
                                                    new RecordingModifier()
                                                            .size(80)
                                                            .background(Color.RED));
                                        });
                                writer.box(new RecordingModifier().size(80).background(Color.BLUE));
                            });
                });
        ArrayList<TestComponentOperation> ops = new ArrayList<>();
        ops.add(new TestComponentHorizontalWeightOperation(42, 2f));
        baseTestComponent(writer, mTw, mTh, ops);
    }

    @Test
    public void testRowWeight1w() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier().background(Color.YELLOW).padding(8),
                            RowLayout.START,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .horizontalWeight(1f)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowWeight2w() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier().background(Color.YELLOW).padding(8),
                            RowLayout.START,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .height(90)
                                                .horizontalWeight(1f)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowWeight3w() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier().background(Color.YELLOW).padding(8),
                            RowLayout.START,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .height(80)
                                                .horizontalWeight(1f)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .height(90)
                                                .horizontalWeight(1f)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowWeight4w() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier().background(Color.YELLOW).padding(8),
                            RowLayout.START,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .height(80)
                                                .horizontalWeight(1f)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .height(90)
                                                .horizontalWeight(1f)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .height(100)
                                                .horizontalWeight(1f)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testRowWeight5w() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier().background(Color.YELLOW).padding(8),
                            RowLayout.START,
                            RowLayout.CENTER,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .height(80)
                                                .horizontalWeight(1f)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .componentId(42)
                                                .height(90)
                                                .horizontalWeight(1f)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .height(100)
                                                .horizontalWeight(1f)
                                                .background(Color.BLUE));
                            });
                });
        ArrayList<TestComponentOperation> ops = new ArrayList<>();
        ops.add(new TestComponentHorizontalWeightOperation(42, 2f));
        baseTestComponent(writer, mTw, mTh, ops);
    }

    @Test
    public void testColumnWeight1() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier()
                                    .fillMaxHeight()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            ColumnLayout.CENTER,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .verticalWeight(1f)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnWeight2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier()
                                    .fillMaxHeight()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            ColumnLayout.CENTER,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .width(110)
                                                .verticalWeight(1f)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnWeight3() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier()
                                    .fillMaxHeight()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            ColumnLayout.CENTER,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .width(100)
                                                .verticalWeight(1f)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .width(110)
                                                .verticalWeight(1f)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnWeight4() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier()
                                    .fillMaxHeight()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            ColumnLayout.CENTER,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .width(100)
                                                .verticalWeight(1f)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .width(110)
                                                .verticalWeight(1f)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .width(120)
                                                .verticalWeight(1f)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnWeight5() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier()
                                    .fillMaxHeight()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            ColumnLayout.CENTER,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .width(100)
                                                .verticalWeight(1f)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .componentId(42)
                                                .width(110)
                                                .verticalWeight(1f)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .width(120)
                                                .verticalWeight(1f)
                                                .background(Color.BLUE));
                            });
                });
        ArrayList<TestComponentOperation> ops = new ArrayList<>();
        ops.add(new TestComponentVerticalWeightOperation(42, 2f));
        baseTestComponent(writer, mTw, mTh, ops);
    }

    @Test
    public void testColumnWeight6() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier()
                                    .fillMaxHeight()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            ColumnLayout.CENTER,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(new RecordingModifier().size(80).background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .componentId(42)
                                                .width(100)
                                                .verticalWeight(1f)
                                                .background(Color.GREEN),
                                        BoxLayout.CENTER,
                                        BoxLayout.CENTER,
                                        () -> {
                                            writer.box(
                                                    new RecordingModifier()
                                                            .size(80)
                                                            .background(Color.RED));
                                        });
                                writer.box(new RecordingModifier().size(80).background(Color.BLUE));
                            });
                });
        ArrayList<TestComponentOperation> ops = new ArrayList<>();
        ops.add(new TestComponentVerticalWeightOperation(42, 2f));
        baseTestComponent(writer, mTw, mTh, ops);
    }

    @Test
    public void testColumnWeight1w() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier().background(Color.YELLOW).padding(8),
                            ColumnLayout.CENTER,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .size(110, 90)
                                                .verticalWeight(1f)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnWeight2w() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier().background(Color.YELLOW).padding(8),
                            ColumnLayout.CENTER,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .size(100, 80)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .width(110)
                                                .verticalWeight(1f)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnWeight3w() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier().background(Color.YELLOW).padding(8),
                            ColumnLayout.CENTER,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .width(100)
                                                .verticalWeight(1f)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .width(110)
                                                .verticalWeight(1f)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .size(120, 100)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnWeight4w() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier().background(Color.YELLOW).padding(8),
                            ColumnLayout.CENTER,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .width(100)
                                                .verticalWeight(1f)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .width(110)
                                                .verticalWeight(1f)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .width(120)
                                                .verticalWeight(1f)
                                                .background(Color.BLUE));
                            });
                });
        baseTest(writer, mTw, mTh);
    }

    @Test
    public void testColumnWeight5w() {
        RemoteComposeWriter writer = new RemoteComposeWriter(mTw, mTh, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier().background(Color.YELLOW).padding(8),
                            ColumnLayout.CENTER,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .width(100)
                                                .verticalWeight(1f)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .componentId(42)
                                                .width(110)
                                                .verticalWeight(1f)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .width(120)
                                                .verticalWeight(1f)
                                                .background(Color.BLUE));
                            });
                });
        ArrayList<TestComponentOperation> ops = new ArrayList<>();
        ops.add(new TestComponentVerticalWeightOperation(42, 2f));
        baseTestComponent(writer, mTw, mTh, ops);
    }

    @Test
    public void testBoxClip1() {
        RemoteComposeWriter writer = new RemoteComposeWriter(200, 200, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier().background(Color.YELLOW).padding(8),
                            BoxLayout.START,
                            BoxLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .componentId(42)
                                                .size(100, 80)
                                                .clip(new RoundedRectShape(16, 16, 16, 16))
                                                .background(Color.RED));
                            });
                });
        baseTest(writer, 200, 200);
    }

    @Test
    public void testBoxClip2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(200, 200, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier().background(Color.YELLOW).padding(8),
                            BoxLayout.START,
                            BoxLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .componentId(42)
                                                .size(100, 80)
                                                .clip(new RoundedRectShape(16, 16, 16, 16))
                                                .background(Color.RED));
                            });
                });
        ArrayList<TestComponentOperation> ops = new ArrayList<>();
        ops.add(new TestComponentResizeOperation(42).setSize(180, 160));
        baseTestComponent(writer, 200, 200, ops);
    }

    @Test
    public void testBoxClip3() {
        RemoteComposeWriter writer = new RemoteComposeWriter(200, 200, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier().background(Color.YELLOW).padding(8),
                            BoxLayout.START,
                            BoxLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .componentId(42)
                                                .size(180, 160)
                                                .clip(new RoundedRectShape(16, 16, 16, 16))
                                                .background(Color.RED));
                            });
                });
        baseTest(writer, 200, 200);
    }

    @Test
    public void testBoxClip4() {
        RemoteComposeWriter writer = new RemoteComposeWriter(190, 170, "Layout", mPlatform);
        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier().background(Color.YELLOW).padding(8),
                            BoxLayout.START,
                            BoxLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .componentId(42)
                                                .size(180, 160)
                                                .clip(new RoundedRectShape(16, 16, 16, 16))
                                                .background(Color.RED));
                            });
                });
        baseTest(writer, 190, 170);
    }

    @Test
    public void testInvalidate1() {
        RemoteComposeWriter writer = new RemoteComposeWriter(200, 200, "Layout", mPlatform);

        writer.root(
                () -> {
                    writer.column(
                            new RecordingModifier()
                                    .componentId(1)
                                    .fillMaxHeight()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            ColumnLayout.CENTER,
                            ColumnLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .componentId(2)
                                                .size(100)
                                                .background(Color.RED));
                                writer.box(
                                        new RecordingModifier()
                                                .componentId(3)
                                                .size(110)
                                                .background(Color.GREEN));
                                writer.box(
                                        new RecordingModifier()
                                                .componentId(4)
                                                .size(120)
                                                .background(Color.BLUE));
                            });
                });
        ArrayList<TestComponentOperation> ops1 = new ArrayList<>();
        ops1.add(new TestRootNeedsMeasure(1, false));
        ops1.add(new TestComponentNeedsMeasure(1, false));
        ops1.add(new TestComponentNeedsMeasure(2, false));
        ops1.add(new TestComponentNeedsMeasure(3, false));
        ops1.add(new TestComponentNeedsMeasure(4, false));
        ops1.add(new TestComponentResizeOperation(3).setSize(180, 160));
        ops1.add(new TestRootNeedsMeasure(1, true));
        ops1.add(new TestComponentNeedsMeasure(1, true));
        ops1.add(new TestComponentNeedsMeasure(2, false));
        ops1.add(new TestComponentNeedsMeasure(3, true));
        ops1.add(new TestComponentNeedsMeasure(4, false));
        ArrayList<TestComponentOperation> ops2 = new ArrayList<>();
        ops2.add(new TestRootNeedsMeasure(1, false));
        ops2.add(new TestComponentNeedsMeasure(1, false));
        ops2.add(new TestComponentNeedsMeasure(2, false));
        ops2.add(new TestComponentNeedsMeasure(3, false));
        ops2.add(new TestComponentNeedsMeasure(4, false));
        baseTestComponent(writer, 200, 200, ops1, ops2);
    }

    @Test
    public void testInvalidate2() {
        RemoteComposeWriter writer = new RemoteComposeWriter(400, 400, "Layout", mPlatform);

        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier()
                                    .componentId(1)
                                    .fillMaxHeight()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            RowLayout.CENTER,
                            RowLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .componentId(2)
                                                .size(100)
                                                .background(Color.RED));
                                writer.column(
                                        new RecordingModifier()
                                                .componentId(3)
                                                .size(110)
                                                .background(Color.GREEN),
                                        ColumnLayout.CENTER,
                                        ColumnLayout.CENTER,
                                        () -> {
                                            writer.box(
                                                    new RecordingModifier()
                                                            .componentId(4)
                                                            .size(10)
                                                            .background(Color.RED));
                                            writer.box(
                                                    new RecordingModifier()
                                                            .componentId(5)
                                                            .size(10)
                                                            .background(Color.RED));
                                        });
                                writer.box(
                                        new RecordingModifier()
                                                .componentId(6)
                                                .size(120)
                                                .verticalWeight(1f)
                                                .background(Color.BLUE));
                            });
                });
        ArrayList<TestComponentOperation> ops1 = new ArrayList<>();
        ops1.add(new TestRootNeedsMeasure(1, false));
        ops1.add(new TestComponentNeedsMeasure(1, false));
        ops1.add(new TestComponentNeedsMeasure(2, false));
        ops1.add(new TestComponentNeedsMeasure(3, false));
        ops1.add(new TestComponentNeedsMeasure(4, false));
        ops1.add(new TestComponentNeedsMeasure(5, false));
        ops1.add(new TestComponentNeedsMeasure(6, false));
        ops1.add(new TestComponentResizeOperation(4).setSize(180, 160));
        ops1.add(new TestRootNeedsMeasure(1, true));
        ops1.add(new TestComponentNeedsMeasure(1, true));
        ops1.add(new TestComponentNeedsMeasure(2, false));
        ops1.add(new TestComponentNeedsMeasure(3, true));
        ops1.add(new TestComponentNeedsMeasure(4, true));
        ops1.add(new TestComponentNeedsMeasure(5, false));
        ops1.add(new TestComponentNeedsMeasure(6, false));
        ArrayList<TestComponentOperation> ops2 = new ArrayList<>();
        ops2.add(new TestRootNeedsMeasure(1, false));
        ops2.add(new TestComponentNeedsMeasure(1, false));
        ops2.add(new TestComponentNeedsMeasure(2, false));
        ops2.add(new TestComponentNeedsMeasure(3, false));
        ops2.add(new TestComponentNeedsMeasure(4, false));
        ops2.add(new TestComponentNeedsMeasure(5, false));
        ops2.add(new TestComponentNeedsMeasure(6, false));
        baseTestComponent(writer, 400, 400, ops1, ops2);
    }

    @Test
    public void testClick() {
        RemoteComposeWriter writer = new RemoteComposeWriter(400, 400, "Layout", mPlatform);
        int actionId = 1234;
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier()
                                    .componentId(1)
                                    .fillMaxHeight()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            RowLayout.CENTER,
                            RowLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .componentId(2)
                                                .size(100)
                                                .background(Color.RED)
                                                .onClick(new HostAction(actionId)));
                            });
                });
        ArrayList<TestComponentOperation> ops = new ArrayList<>();
        ops.add(new TestComponentClickOperation(2));
        DebugPlayerContext context = (DebugPlayerContext) baseTestComponent(writer, 400, 400, ops);
        assertEquals("Test click action", actionId, context.getLastAction());
    }

    @Test
    public void testTouchDown() {
        RemoteComposeWriter writer = new RemoteComposeWriter(400, 400, "Layout", mPlatform);
        int actionId = 1234;
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier()
                                    .componentId(1)
                                    .fillMaxHeight()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            RowLayout.CENTER,
                            RowLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .componentId(2)
                                                .size(100)
                                                .background(Color.RED)
                                                .onTouchDown(new HostAction(actionId)));
                            });
                });
        ArrayList<TestComponentOperation> ops = new ArrayList<>();
        ops.add(new TestComponentTouchDownOperation(2, 50f, 50f));
        DebugPlayerContext context = (DebugPlayerContext) baseTestComponent(writer, 400, 400, ops);
        assertEquals("Test click action", actionId, context.getLastAction());
    }

    @Test
    public void testTouchDownUp() {
        RemoteComposeWriter writer = new RemoteComposeWriter(400, 400, "Layout", mPlatform);
        int actionIdDown = 1234;
        int actionIdUp = 5678;
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier()
                                    .componentId(1)
                                    .fillMaxHeight()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            RowLayout.CENTER,
                            RowLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .componentId(2)
                                                .size(100)
                                                .background(Color.RED)
                                                .onTouchDown(new HostAction(actionIdDown))
                                                .onTouchUp(new HostAction(actionIdUp)));
                            });
                });
        ArrayList<TestComponentOperation> ops = new ArrayList<>();
        ops.add(new TestComponentTouchDownOperation(2, 50f, 50f));
        ops.add(new TestComponentTouchUpOperation(2, 50f, 50f));
        DebugPlayerContext context = (DebugPlayerContext) baseTestComponent(writer, 400, 400, ops);
        assertEquals("Test click action", actionIdUp, context.getLastAction());
    }

    @Test
    public void testTouchDownUpCancel() {
        RemoteComposeWriter writer = new RemoteComposeWriter(400, 400, "Layout", mPlatform);
        int actionIdDown = 1234;
        int actionIdUp = 5678;
        int actionIdCancel = 9012;
        writer.root(
                () -> {
                    writer.row(
                            new RecordingModifier()
                                    .componentId(1)
                                    .fillMaxHeight()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            RowLayout.CENTER,
                            RowLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .componentId(2)
                                                .size(100)
                                                .background(Color.RED)
                                                .onTouchDown(new HostAction(actionIdDown))
                                                .onTouchUp(new HostAction(actionIdUp))
                                                .onTouchCancel(new HostAction(actionIdCancel)));
                            });
                });
        ArrayList<TestComponentOperation> ops = new ArrayList<>();
        ops.add(new TestComponentTouchDownOperation(2, 50f, 50f));
        ops.add(new TestComponentTouchUpOperation(2, 50f, 50f));
        ops.add(new TestComponentTouchCancelOperation(2, 50f, 50f));
        DebugPlayerContext context = (DebugPlayerContext) baseTestComponent(writer, 400, 400, ops);
        assertEquals("Test click action", actionIdCancel, context.getLastAction());
    }
}
