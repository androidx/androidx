# Guide to Writing RemoteCompose Creation Demos

This guide describes how to write procedural drawing demos using the `remote-creation` APIs (without `@Composable`). These demos are used to test the core drawing, layout, and expression engine of RemoteCompose.

## Core Principles

- **Procedural Logic**: You are writing code that *emits* a stream of operations to a `RemoteComposeWriter`.
- **Remote Types**: Use `RemoteComposeWriter` (or `RemoteComposeWriterAndroid`) to define constants, expressions, and drawing commands.
- **Stateless Execution**: The demo function should ideally be a static method that returns a `RemoteComposeWriter`.

## Structure of a Creation Demo

A typical demo consists of:
1.  **Platform Services**: Initializing `RcPlatformServices`.
2.  **Writer Initialization**: Creating a `RemoteComposeWriterAndroid`.
3.  **Variables and Expressions**: Defining `floatConstant`, `floatExpression`, etc.
4.  **Layout Root**: Using `rc.root(...)` to start the component tree.
5.  **Layout and Drawing**: Using `box`, `column`, `row`, and `startCanvas` to structure and draw.

### Example Template (Java)

```java
import static androidx.compose.remote.core.RcProfiles.PROFILE_ANDROIDX;
import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;
import static androidx.compose.remote.creation.Rc.FloatExpression.ADD;

import android.graphics.Color;
import android.graphics.Paint;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;
import androidx.compose.remote.core.RcPlatformServices;

public class MyDemo {
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();

    public static RemoteComposeWriterAndroid createDemo() {
        // 1. Initialize Writer (width, height, name, version, profile, platform)
        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(
                500, 500, "MyDemo", 7, PROFILE_ANDROIDX, sPlatform);

        // 2. Define dynamic variables
        float time = RemoteComposeWriterAndroid.TIME_IN_CONTINUOUS_SEC;
        float speed = rc.addFloatConstant(2.0f);
        float animatedValue = rc.floatExpression(time, speed, MUL);

        // 3. Define the UI Root
        rc.root(() -> {
            rc.box(new RecordingModifier().fillMaxSize().background(Color.LTGRAY), () -> {
                rc.startCanvas(new RecordingModifier().size(200, 200));
                
                // Drawing operations
                rc.getPainter()
                  .setColor(Color.BLUE)
                  .setStyle(Paint.Style.FILL)
                  .commit();
                  
                rc.drawCircle(100, 100, 50);
                
                rc.endCanvas();
            });
        });

        return rc;
    }
}
```

## Key API Sections

### 1. Expressions (`rc.floatExpression`)
Expressions allow you to define math that runs on the player (remote side) without re-sending the whole doc.
- `rc.addFloatConstant(value)`
- `rc.floatExpression(a, b, OP)` where `OP` is from `Rc.FloatExpression` (e.g., `ADD`, `MUL`, `SIN`, `COS`, `PINGPONG`).

### 2. The Painter (`rc.getPainter()`)
The `RemotePaint` bridge allows you to set standard Android `Paint` properties which are then serialized.
- **IMPORTANT**: You must call `.commit()` after setting properties to sync them to the writer.
- Example: `rc.getPainter().setColor(Color.RED).setStrokeWidth(5).commit();`

### 3. Layout Components
- `rc.box(modifier, content)`
- `rc.column(modifier, horizontalAlign, verticalAlign, content)`
- `rc.row(modifier, horizontalAlign, verticalAlign, content)`
- `rc.startCanvas(modifier)` / `rc.endCanvas()`

### 4. Modifiers (`RecordingModifier`)
Modifiers are chained to define layout behavior.
- `.fillMaxSize()`, `.fillMaxWidth()`, `.fillMaxHeight()`
- `.width(w)`, `.height(h)`, `.size(w, h)`
- `.padding(p)`, `.background(color)`
- `.visibility(id)` (takes a remote ID)

### 5. Advanced Drawing
- **Paths**: `rc.addPathData(remotePath)`, `rc.drawPath(pathId)`.
- **Text**: `rc.createTextFromFloat(...)`, `rc.drawTextAnchored(...)`.
- **Loops**: `rc.startLoopVar(start, step, end)`, `rc.endLoop()`.

## Registration

To make a demo visible in the Player View Demos app, it must be registered in `DemosCreation.java` using `getp`:

```java
// In DemosCreation.java
getp("Category/MyDemoName", MyDemo::createDemo),
```

- `getp(path, supplier)`: Standard procedural demo.
- `getpc(path, supplier)`: Procedural demo that might need context (like `Activity`).

## Prompting for New Demos

When asking the agent to create a new demo:
1.  **Specify the Goal**: "Create a creation demo that shows [feature, e.g., path morphing]."
2.  **Mention the API**: "Use `RemoteComposeWriterAndroid` and procedural layout."
3.  **Target File**: "Add it to `integration-tests/player-view-demos/src/main/java/androidx/compose/remote/integration/view/demos/examples/`."
4.  **Registration**: "Register it in `DemosCreation.java`."
