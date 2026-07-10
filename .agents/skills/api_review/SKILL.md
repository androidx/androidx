---
name: api_review
description: A skill to review pending work for API design compliance in the
AndroidX support project
---

# API Review Skill

This skill provides instructions for evaluating pending work (e.g., a local git
branch or CL) to ensure that any new or modified Public APIs comply with
AndroidX/Jetpack API guidelines.

## Steps for API Review

When the user asks for their pending work to be reviewed for API design, follow
these steps:

### 1. Update and Review API Files (`updateApi`)

First, run the `updateApi` task to ensure all recent changes are accurately
reflected in the API tracking files (e.g., `current.txt` or
`restricted_current.txt`). This catches any new modifications that haven't been
tracked yet.

*   **For all modules:**
    ```bash
    ./gradlew updateApi
    ```
*   **For a single module:** (e.g., `appcompat-resources` in the `appcompat`
    group)
    ```bash
    ./gradlew :appcompat:appcompat-resources:updateApi
    ```
*   **For Compose modules (Kotlin Multiplatform):** Compose is a multiplatform
    project. If you modify APIs that affect native targets (e.g., iOS or
    Desktop), you may also need to update the native ABI files:
    ```bash
    ./gradlew :compose:module:updateAbiNative
    ```

*Note: Use the single module command if the user's changes are scoped to a
specific module to save time.*

After running the command, you will perform the API review over **all pending
changes** in the API `.txt` files. This includes changes that `updateApi` just
added, as well as any changes that were already pending in those files.

### 2. Check Guidelines Compliance

Once the changed API signatures are identified in the `.txt` files (or from
pending changes to those `.txt` files in the current work), you **must** apply
the corresponding API design guidelines to evaluate them:

*   **General AndroidX API Guidelines:**
    Review the principles found in the repository under:
    `docs/api_guidelines/`
*   **Android API Guidelines:**
    Follow the [Android API guidelines](https://source.android.com/docs/setup/contribute/api-guidelines).
    New APIs should prioritize Kotlin users over Java users while still ensuring
    they are easy to use from Java. For more details, see [Kotlin Interop](https://developer.android.com/kotlin/interop).
*   **Compose API Guidelines:**
    If the changes are within the Jetpack Compose project (e.g., `compose/`
    directory), you **must also** evaluate against:
    `docs/api_guidelines/compose_api_guidelines/`
*   **Android Kotlin Style Guide:**
    Ensure any Kotlin code or API design follows the [Android Kotlin Style
    Guide](https://developer.android.com/kotlin/style-guide). You can read this
    via the web if needed.

### 3. Produce and Present the Review

Produce an explanation for the user on how their pending APIs comply with the
relevant guidelines.

*   **Highlight Compliance:** Briefly acknowledge where the code aligns with
    standard practices.
*   **Call Out Violations:** Clearly write out any guidelines that are *not*
    being followed, citing the specific rule/section from the guidelines (e.g.,
    "Methods returning a boolean should be prefixed with 'has' or 'is' as per
    the AndroidX API Guidelines").
*   **Recommend Solutions:** Provide a suggested change to fix the identified
    issue.
