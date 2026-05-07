# Document Structure

A RemoteCompose document is stored as a flat list of binary-encoded operations. To be displayed or processed, these operations are typically inflated into a tree structure.

## Operation Order

While many operations can appear in various orders, a well-formed document generally follows this sequence:

1.  **Header**: Must be the very first operation. It contains versioning and initial document dimensions.
2.  **Data Operations**: Operations that define reusable data, such as `TextData`, `BitmapData`, `FontData`, or `FloatConstant`. These must be defined before they are referenced by ID in other operations.
3.  **Macro Definitions**: `MacroDefine` operations must appear before any `MacroCall` that uses them.
4.  **Layout Tree**: The visual structure of the document, typically starting with a `RootLayoutComponent`.

## Nesting Rules and Containers

Operations that implement the `Container` interface can have children. The nesting is defined by the order in the flat list: an operation following a `Container` is its child, until a matching `ContainerEnd` is encountered.

### LayoutManagers (Box, Column, Row, etc.)

Most layoutable components (subclasses of `LayoutManager`) follow a specific nesting structure:

1.  **Modifiers**: Operations immediately following the `LayoutManager` (before `LayoutContent`) are treated as its modifiers (e.g., `WidthModifier`, `PaddingModifier`, `BackgroundModifier`).
2.  **Specialized Modifiers**: Some modifiers, like `CanvasOperations` (used by `drawWithContent`), act as containers for internal drawing operations.
3.  **Content**: To define the UI children of a `LayoutManager`, a `LayoutContent` operation (opcode 201) must be used.

### RootLayoutComponent

The `RootLayoutComponent` is the top-level entry point of the layout tree.
- It is a `Container`.
- Its children are directly UI components or data operations.
- It **does not** use a `LayoutContent` child to wrap its components.
- It must be closed with a `ContainerEnd`.

**Correct Nesting Example:**

| Flat Operation List | Nesting Level | Role |
| :--- | :--- | :--- |
| `Header` | 0 | Document Info |
| `TextData` [ID: 10] | 0 | Reusable String |
| `RootLayoutComponent` | 0 | Start of UI |
| &nbsp;&nbsp;`BoxLayout` | 1 | Child of Root |
| &nbsp;&nbsp;&nbsp;&nbsp;`PaddingModifier` | 2 | Modifier for BoxLayout |
| &nbsp;&nbsp;&nbsp;&nbsp;`LayoutContent` | 2 | Start of BoxLayout children |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`TextLayout` [Ref: 10] | 3 | Child of BoxLayout |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`LayoutContent` | 4 | Start of TextLayout children |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`ContainerEnd` | 4 | End of TextLayout content |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`ContainerEnd` | 3 | End of TextLayout |
| &nbsp;&nbsp;&nbsp;&nbsp;`ContainerEnd` | 2 | End of BoxLayout content |
| &nbsp;&nbsp;`ContainerEnd` | 1 | End of BoxLayout |
| `ContainerEnd` | 0 | End of Root |

### Specialized Containers

-   **CoreText**: As a `LayoutManager`, it also requires a `LayoutContent` if it has children (though it often has none, it still needs the content start/end pair if following the standard `LayoutManager` pattern in the writer).
-   **Macros**: `MacroDefine` and `MacroCall` are both `Container`s.
    -   `MacroDefine` contains the template operations.
    -   `MacroCall` contains `MacroBlock` operations (which are also `Container`s) providing arguments to the template.

## Summary of Rules

-   Every `Container` must have a matching `ContainerEnd`.
-   `LayoutManager` children must be wrapped in a `LayoutContent` block.
-   Modifiers must come *before* the `LayoutContent` block.
-   Data must be defined before it is used.
-   Macros must be defined before they are called.
