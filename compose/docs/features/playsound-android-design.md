# PlaySound API Design Document

**Sean McQuillan, Compose, Mar 18, 2026**

## Changes

- Renamed APIs to singular form (#5.1, #5.2, #5.3)
- Removed explicit focus opt-out design (#3.11)
- Promoted interfaces/Locals to public API (#1.1, #1.2)
- Removed `FakeSoundEffect` testing vendor (#1.6)
- Added feature flags to foundation and ui for total rollback (#7.1)
- Promoted `LocalSoundEffect` to public, to read from foundation. Non-null, no-ops default.
- Do not play sounds on *entry* from view focus callbacks, as ViewRootImpl already played sounds.
- Do go through `View.playSound` to hit non-public Window based sound feature in `ViewRootImpl`.

## Feature Flags
- **`ComposeFoundationFlags.isInteractionSoundEffectOnClickEnabled`**: Toggle-off clickable sounds
- **`AndroidComposeUiFlags.isInteractionSoundEffectsEnabled`**: Toggle-off focus sounds



## API Purpose and Goals
- Expose Android-specific `playSound` behavior related to focus change and clicks as the default Compose behavior. Other platforms do not have this feature (see tab).
- Allow developers to opt-out of automatic sounds for clicks on specific components. *Note: During implementation, it was determined that an opt-out for focus sounds is not required.*
- The system is built around a `SoundEffect` interface for abstracting playback capabilities.

## Background: View.playSoundEffect
`View.playSoundEffect` is an API on views that can be used to play a subset of the sounds that are available through `AudioManager`. These are the sounds it supports:

- `SoundEffectConstants.CLICK`
- `SoundEffectConstants.NAVIGATION_LEFT`
- `SoundEffectConstants.NAVIGATION_UP`
- `SoundEffectConstants.NAVIGATION_RIGHT`
- `SoundEffectConstants.NAVIGATION_DOWN`
- `SoundEffectConstants.NAVIGATION_REPEAT_LEFT`
- `SoundEffectConstants.NAVIGATION_REPEAT_UP`
- `SoundEffectConstants.NAVIGATION_REPEAT_RIGHT`
- `SoundEffectConstants.NAVIGATION_REPEAT_DOWN`

In the Android View system, `playSoundEffect` is invoked exactly when handling the interaction, ensuring sound effects are properly timed and correctly mapped:

1. **Clicks**: Invoked inside `performClickInternal()` (for touch, key, and accessibility actions) exactly before `OnClickListener.onClick()` is called.
2. **Focus Navigation (DPAD)**: Invoked inside `performFocusNavigation()` right after successfully moving focus to the new view.
3. **Focus Navigation (Clusters)**: Invoked inside `performKeyboardGroupNavigation()` right after successfully navigating clusters via `TAB` key events.

## Critical User Journeys
Give developers control over implicit `playSound` calls from clicks. This is expected to be a very low traffic API with one known use case.

### 1. Disabling playSound because I don't want the sounds
The developer would like to disable `playSound` for a specific component because they have their own sounds, or because playing sounds are wrong (e.g. go/msds-service-dd or a dial tone on a dialer).
This API is the same as `setSoundEffectsDisabled` (API 1) on View and is the only known use case for app-configuration on Android.

```kotlin
@Composable
fun Example(modifier: Modifier) {
   SoundEffectOnInteraction(enabled = false) {
       Button() { /* ... */ }
   }
}
```

> **Tip:** Focus opt-out was removed as the implementation is automatically handled by the Android View system on navigation.

## Existing Concepts Leveraged
- `CompositionLocal`
- Scope configuration & composable content

## New Concepts Introduced
- `SoundEffect` - The interface for playing interaction sounds.
- `LocalSoundEffect` - The CompositionLocal for accessing the sound player.
- `SoundEffectOnInteraction` - The composable for configuring interaction sound behavior.

### Dependency Layering
- **ACV (AndroidComposeView)** -> Installs `CompositionLocal` (`LocalSoundEffect`) which wraps `AudioManager.playSoundEffect()`.
- **SoundEffectOnInteraction** -> Wraps `LocalSoundEffect` with a delegating implementation that respects the `enabled` boolean state, dropping sound calls if `false`.
- **Clickable** -> Resolves `LocalSoundEffect` and invokes sound playback explicitly to ensure zero-allocation fast paths during gestures and key events.

## API Specification

### SoundEffect Interface
```kotlin
/**
 * Interface representing the capability to play sound effects on user interaction.
 *
 * This is used primarily to play click sounds from `commonMain` modifiers like `clickable`.
 */
interface SoundEffect {

    /**
     * Plays a click sound effect.
     *
     * This method triggers the standard click sound effect (if enabled by the system and the
     * component).
     */
    fun playClickSound()
}

```

### LocalSoundEffect CompositionLocal
```kotlin
/**
 * The CompositionLocal to provide platform sound effects.
 *
 * This is used to trigger sounds on user interaction, like clicks. To enable, disable, or customize
 * sound interaction scopes, utilize `SoundEffectOnInteraction`.
 *
 * @sample androidx.compose.ui.samples.InteractionSoundSamples
 * @see SoundEffect
 */
val LocalSoundEffect =
    staticCompositionLocalOf<SoundEffect> {
        object : SoundEffect {
            override fun playClickSound() {
                // This platform does not support sound, so sound effects are a no-op
            }
        }
    }
```

> **Tip:** `SoundEffect` and `LocalSoundEffect` were promoted to public to allow reading from foundation

### SoundEffectOnInteraction
```kotlin
/**
 * Configure whether sound effects are played for interactions (clicks) in the provided [content].
 *
 * @param enabled true if sound effects should be played on user interactions, false to silence
 *   them.
 * @param content The composable subtree to wrap.
 */
@Composable
fun SoundEffectOnInteraction(enabled: Boolean, content: @Composable () -> Unit) {
    ...
}
```

## Platform to compose mappings

### Focus sounds
| Call in `ViewRootImpl.java`                                                                                                                                                                                                          | Meaning | `AndroidComposeView.android.kt` Lines |
|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| :--- | :--- |
| [`playSoundEffect(SoundEffectConstants.getConstantForFocusDirection(direction, isFastScrolling))`](https://cs.android.com/android/platform/frameworks/base/+/1cdfff555f4a21f71ccc978290e2e212e2f8b168:core/java/android/view/ViewRootImpl.java;l=7956) | DPAD Key Event Navigation | [3586](https://cs.android.com/androidx/platform/frameworks/support/+/13a54b4024e510d42bad02daef8e817f9d207960:compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidComposeView.android.kt;l=3586), [3598](https://cs.android.com/androidx/platform/frameworks/support/+/13a54b4024e510d42bad02daef8e817f9d207960:compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidComposeView.android.kt;l=3598), [3627](https://cs.android.com/androidx/platform/frameworks/support/+/13a54b4024e510d42bad02daef8e817f9d207960:compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidComposeView.android.kt;l=3627), [3643](https://cs.android.com/androidx/platform/frameworks/support/+/13a54b4024e510d42bad02daef8e817f9d207960:compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidComposeView.android.kt;l=3643) |
| [`playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction))`](https://cs.android.com/android/platform/frameworks/base/+/1cdfff555f4a21f71ccc978290e2e212e2f8b168:core/java/android/view/ViewRootImpl.java;l=8019)                   | Keyboard Navigation (Tab, Cluster) | [1175](https://cs.android.com/androidx/platform/frameworks/support/+/13a54b4024e510d42bad02daef8e817f9d207960:compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidComposeView.android.kt;l=1175), [1239](https://cs.android.com/androidx/platform/frameworks/support/+/13a54b4024e510d42bad02daef8e817f9d207960:compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidComposeView.android.kt;l=1239), [1258](https://cs.android.com/androidx/platform/frameworks/support/+/13a54b4024e510d42bad02daef8e817f9d207960:compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidComposeView.android.kt;l=1258), [1324](https://cs.android.com/androidx/platform/frameworks/support/+/13a54b4024e510d42bad02daef8e817f9d207960:compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidComposeView.android.kt;l=1324), [1368](https://cs.android.com/androidx/platform/frameworks/support/+/13a54b4024e510d42bad02daef8e817f9d207960:compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidComposeView.android.kt;l=1368) |

### Click sounds


| Call in `View.java` | Meaning | `Clickable.kt` Lines | `Clickable.kt` Method |
| :--- | :--- | :--- | :--- |
| [`performClickInternal()`](https://cs.android.com/android/platform/frameworks/base/+/1cdfff555f4a21f71ccc978290e2e212e2f8b168:core/java/android/view/View.java;l=16102) | Accessibility Click Action | [2009](https://cs.android.com/androidx/platform/frameworks/support/+/13a54b4024e510d42bad02daef8e817f9d207960:compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/Clickable.kt;l=2009) | `AbstractClickableNode.applySemantics` |
| [`performClickInternal()`](https://cs.android.com/android/platform/frameworks/base/+/1cdfff555f4a21f71ccc978290e2e212e2f8b168:core/java/android/view/View.java;l=17402) | Key Event Click | [1518](https://cs.android.com/androidx/platform/frameworks/support/+/13a54b4024e510d42bad02daef8e817f9d207960:compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/Clickable.kt;l=1518), [1555](https://cs.android.com/androidx/platform/frameworks/support/+/13a54b4024e510d42bad02daef8e817f9d207960:compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/Clickable.kt;l=1555) | `onClickKeyUpEvent`, `onClickKeyDownEvent` |
| [`performClickInternal()`](https://cs.android.com/android/platform/frameworks/base/+/1cdfff555f4a21f71ccc978290e2e212e2f8b168:core/java/android/view/View.java;l=18130) | Touch Event Click | [945](https://cs.android.com/androidx/platform/frameworks/support/+/13a54b4024e510d42bad02daef8e817f9d207960:compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/Clickable.kt;l=945), [955](https://cs.android.com/androidx/platform/frameworks/support/+/13a54b4024e510d42bad02daef8e817f9d207960:compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/Clickable.kt;l=955), [1281](https://cs.android.com/androidx/platform/frameworks/support/+/13a54b4024e510d42bad02daef8e817f9d207960:compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/Clickable.kt;l=1281), [1313](https://cs.android.com/androidx/platform/frameworks/support/+/13a54b4024e510d42bad02daef8e817f9d207960:compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/Clickable.kt;l=1313) | `handleUpEvent`, `pointerInputNode` `onTap` |
| [`performClickInternal()`](https://cs.android.com/android/platform/frameworks/base/+/1cdfff555f4a21f71ccc978290e2e212e2f8b168:core/java/android/view/View.java;l=31517) | Single Tap Runnable | [1290](https://cs.android.com/androidx/platform/frameworks/support/+/13a54b4024e510d42bad02daef8e817f9d207960:compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/Clickable.kt;l=1290), [1298](https://cs.android.com/androidx/platform/frameworks/support/+/13a54b4024e510d42bad02daef8e817f9d207960:compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/Clickable.kt;l=1298), [1321](https://cs.android.com/androidx/platform/frameworks/support/+/13a54b4024e510d42bad02daef8e817f9d207960:compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/Clickable.kt;l=1321), [1330](https://cs.android.com/androidx/platform/frameworks/support/+/13a54b4024e510d42bad02daef8e817f9d207960:compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/Clickable.kt;l=1330) | `handleUpEvent`, `pointerInputNode` `onTap` |

## Testability Specification

We do not vend fakes. Public interfaces are provided.

### Sound Effects

To test, override `LocalSoundEffect` with a custom `SoundEffect` implementation:

```kotlin
val mockSoundEffect = object : SoundEffect {
    var playClickSoundCalled = 0
    override fun playClickSound() { playClickSoundCalled++ }
}

rule.setContent {
    CompositionLocalProvider(LocalSoundEffect provides mockSoundEffect) {
        Box(Modifier.clickable { })
    }
}
```

## Appendix A: Alternatives Considered (optional)

### Modifier
**Pros:**
- Kept strictly local to the element rather than a hierarchical property.

**Cons:**
- Wiring this up with `ModifierLocal` is possible but produces a fragile-ordering to the caller like already exists with focus, this is not a great API.
- This is expected to be an almost-never trafficked API. A big bold march to the left makes this API rude, but loud, and very easy to understand.

### Parameter to `clickable`
**Pros:**
- Directly solves the issue at the source where interaction events exist.
- Discoverability is better.

**Cons:**
- This would solve it but it's HUGE.
- It's also an incredibly low traffic API and honestly it's not worth this much churn.

### Previous design doc: Sound Effects in Compose
**Pros:**
- Potentially more flexible and robust.

**Cons:**
- The proposed system is bigger than the known use cases actually demand.
