# RemoteCompose Expression Engine

The Expression Engine allows the Player to perform mathematical calculations and logic locally, enabling smooth animations and responsive UI without constant communication with the host.

## RPN (Reverse Polish Notation)
Expressions are encoded using RPN, where operands precede their operators. This allows for efficient stack-based evaluation on the player.

### Example: `(A + B) * 2`
In RemoteCompose, this is represented as: `[A_ID, B_ID, ADD, 2.0, MUL]`

## Evaluation Logic
1.  **Dependencies**: Each expression tracks which variables it depends on.
2.  **Dirty Tracking**: When a variable (like `Time` or `TouchX`) changes, all dependent expressions are marked "dirty".
3.  **Stack Evaluator**: `AnimatedFloatExpression.eval()` iterates through the RPN array:
    - Values are pushed onto a stack.
    - Operators pop their operands from the stack and push the result back.

## Supported Operators

| Type | Operators |
| :--- | :--- |
| **Arithmetic** | `ADD`, `SUB`, `MUL`, `DIV`, `MOD`, `POW` |
| **Trigonometry** | `SIN`, `COS`, `TAN`, `ASIN`, `ACOS`, `ATAN`, `ATAN2` |
| **Logic** | `EQ`, `NEQ`, `GT`, `GE`, `LT`, `LE`, `AND`, `OR`, `IFELSE` |
| **Special** | `ABS`, `MIN`, `MAX`, `CLAMP`, `RAND`, `PINGPONG`, `SQUARE`, `SQRT` |
| **System** | `VAR1`, `VAR2` (Used in loops and path expressions) |

## Variable Storage
- **`RemoteContext`**: Holds the current value of every float ID in the document.
- **Global Variables**: System-provided values updated by the player:
    - `WINDOW_WIDTH`, `WINDOW_HEIGHT`
    - `ANIMATION_TIME`, `CONTINUOUS_SEC`
    - `TOUCH_X`, `TOUCH_Y`, `TOUCH_PRESSED`
    - `ACCELEROMETER_X`, `Y`, `Z`
