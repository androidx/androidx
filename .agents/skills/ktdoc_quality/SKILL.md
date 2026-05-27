---
name: ktdoc_quality
description: Ensure KDocs are high quality, concise, active, and explicitly linked to API components.
---

# KDoc Quality Skill

This skill enforces strict standards for Kotlin documentation (KDoc) in AndroidX. It eliminates verbose technical language (jargon and over-explanation) in favor of direct, active, and linked documentation.

---

## 1. Top Directive: Show, Don't Tell (No Vague References)

Never use plain text for APIs, classes, or parameters if a link is possible.
*   **Always Link**: Use `[ClassName]`, `[methodName]`, or `[parameterName]` for in-scope symbols.
*   **Fully Qualified Links**: For out-of-scope symbols, use fully qualified paths: `[package.path.ClassName]`. Do not add imports for links.
*   **No Vague Tasks**: Do not tell the user to configure something without linking to the configuration class/property.

---

## 2. Orwell's Rules for KDoc (Strict Enforcement)

Apply these rules ruthlessly. Any violation is a documentation bug.

### Rule 1: Cut the Filler
Ban introductory and explanatory fluff. Start immediately with the action.
*   **Banned Phrases**: "A class that...", "Used to...", "Allows the developer to...", "Is responsible for...", "Provides control over...".
*   *Before*: "A class used to represent the keyboard options." (9 words)
*   *After*: "Keyboard configuration options for TextFields." (5 words)

### Rule 2: Ban Verbose Technical Jargon (Simplicity)
Use the shortest, most common word. If you use a long or jargon word when a short one works, it is a bug.
*   **Banned Jargon/Verbose Words** -> **Simple Replacements**:
    *   *Utilize* / *Facilitate* -> **Use** / **Help**
    *   *Represent* / *Informs* -> **Show** / **Tell**
    *   *Execute* / *Perform* -> **Run** / **Do**
    *   *Programmatically control* -> **Control**

### Rule 3: Use Active Voice Only
Start every summary sentence with an active verb. Never use passive voice.
*   *Passive*: "Custom actions are triggered when..."
*   *Active*: "Triggers custom actions when..."

### Rule 4: Rule of 15 Words
Keep the summary sentence (the first line) under 15 words. If it is longer, it is too verbose. Cut it.

---

## 3. The Flow: Dual-Audience Structure

Write for both the **Novice** (needs simple language/examples) and the **Pro** (needs raw specs/links).

1.  **Summary Line (Novice)**: One short, active sentence (<15 words) explaining what it does.
2.  **Visual Example (Both)**: Link to a sample using `@sample`, or show a 2-3 line inline code block.
3.  **Raw Specs/Notes (Pro)**: Bullet points of technical details. Do not write a narrative.
    *   *Good*: "* Note: Maps to Android [android.view.inputmethod.EditorInfo.TYPE_TEXT_VARIATION_URI]."
    *   *Bad*: "This type corresponds to the Android platform's EditorInfo class constant for URI variations, which informs the system to optimize..." (too wordy).
4.  **Block Tags (Pro)**: `@param`, `@return` (start with lowercase, no trailing period).

---

## 4. Style Guide "Before & After" Examples

Study these examples to understand the target style:

### Example 1: Class Header
*   **VERBOSE (Bad)**:
    ```kotlin
    /**
     * Provides manual, programmatic control over the system's software keyboard (IME).
     *
     * Developers can obtain an instance of this interface using the local composition local
     * `LocalSoftwareKeyboardController.current`. It is commonly utilized in specialized forms to
     * programmatically hide the soft keyboard (e.g. when a background network submit completes, or when
     * clicking custom done/search keyboard actions).
     */
    ```
*   **DIRECT (Good)**:
    ```kotlin
    /**
     * Controls the software keyboard.
     *
     * Obtain an instance using [LocalSoftwareKeyboardController]. Commonly used to hide the
     * keyboard (e.g., after submitting data to a network, or when executing custom actions).
     */
    ```

### Example 2: Property Description
*   **VERBOSE (Bad)**:
    ```kotlin
    /**
     * Forces the standard keyboard to display English/Latin characters.
     *
     * **When to use it**: Ideal for usernames, system database IDs, or passcodes where you want
     * to prevent typos from non-latin layouts. *Note: Does not automatically disable
     * auto-correct; use KeyboardOptions' auto-correct setting to control suggestions.*
     */
    ```
*   **DIRECT (Good)**:
    ```kotlin
    /**
     * Forces the keyboard to display Latin characters.
     *
     * **When to use it**: Ideal for usernames, database IDs, or passcodes where you want to
     * restrict input to Latin characters.
     *
     * Note: Unlike [Uri] or [Email], this type does not automatically disable auto-correct. Use
     * [androidx.compose.foundation.text.KeyboardOptions.autoCorrectEnabled] to disable it.
     */
    ```

### Example 3: Function Parameter
*   **VERBOSE (Bad)**:
    ```kotlin
    * @param capitalization Informs the keyboard whether to automatically capitalize characters, words
    *   or sentences. Only applicable to only text based KeyboardTypes.
    ```
*   **DIRECT (Good)**:
    ```kotlin
    * @param capitalization capitalization style to apply to text-based [KeyboardType]s
    ```

---

## 5. Critical Safeguards: Don't Lose the Details

While conciseness is key, KDocs are API specifications. Do not sacrifice technical accuracy or critical behavior contracts for brevity.

### 1. Maintain Technical Contracts
Do not remove descriptions of how parameters interact, how states map to behaviors, or what data callbacks receive.
*   *Bad (Too Simplified)*: `onTextLayout: callback run when text layout changes`
*   *Good*: `onTextLayout: callback run when a new text layout is calculated. The [TextLayoutResult] parameter contains paragraph information, size, baselines, and other details.`

### 2. Define Positively (No Negative Definitions)
Explain what a class or component *does* first. Do not define it solely by what it *does not* do or how it differs from another component.
*   *Bad (Negative)*: `SpanStyle: Only allows character-level styling. For paragraph styling see ParagraphStyle.`
*   *Good (Positive)*: `SpanStyle: Styling configuration that applies at the character level (e.g. text color, font size, background color). For paragraph styling see ParagraphStyle.`

### 3. Document Units and Fallbacks
Always specify:
*   **Units**: E.g., for `letterSpacing`, specify that it uses SP or EM.
*   **Behavior**: E.g., for `baselineShift`, explain it is a vertical shift (up/down).
*   **Fallbacks**: E.g., for `fontSynthesis`, explain it falls back to bold/italic if the font family lacks the requested style.
*   **Specifications**: E.g., link to standard specs (like CSS for `fontFeatureSettings`).

### 4. Rephrase Complex Rules with Structured Examples & Diagrams
For complex validation rules (like paragraph arrangement/nesting) or pipeline data flows (like text transformation pipelines), use clear ASCII diagrams, lists, or structured examples to explain the behavior. Do not remove existing diagrams for the sake of brevity; they are critical for visual APIs.

### 5. Multiplatform & Common Code Cleanliness
When documenting multiplatform (`commonMain`) code, avoid platform-specific terminology (e.g., referring to "Android resources" in a common `ResourceFont` class) unless the API is explicitly platform-bound. Use platform-agnostic terms (e.g., "application or device resources").

### 6. Define Jargon for ESL Readers
Define text-specific jargon (like "collapsed range" or "soft wrap") in plain English. For example, explain that a collapsed range represents a cursor position (start equals end), or describe soft wrap as automatic text wrapping to fit the container.

### 7. Make Warnings Actionable with Examples
When warning about potential layout bugs (e.g., character or accent clipping in tight line heights), provide concrete language examples with tall scripts (e.g., Arabic "العَرَبِيَّةُ", Tibetan "དབུ་ཅན་", or Burmese "မြန်မာဘာသာ") to make testing instructions actionable.

