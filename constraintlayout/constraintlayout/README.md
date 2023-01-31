# ConstraintLayout 🗜️📏

![core](https://github.com/androidx/constraintlayout/workflows/core/badge.svg) [![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)

<img src="https://img.shields.io/badge/stable-2.1.1-blue"/><img src="https://img.shields.io/badge/compose-1.0.0--rc01-blue"/>


ConstraintLayout is a layout manager for Android which allows you to position and size widgets in a flexible way. It's available for both the Android view system and Jetpack Compose.

This repository contains the core Java engine, Android library, validation tools, and experiments.

[Android Reference Docs](https://developer.android.com/reference/androidx/constraintlayout/widget/ConstraintLayout)

Have a question that isn't answered here? Try StackOverflow for [ConstraintLayout](https://stackoverflow.com/questions/tagged/android-constraintlayout) or [MotionLayout](https://stackoverflow.com/questions/tagged/android-motionlayout).

## Using ConstraintLayout

### ⬇️ Installation

Add the Gradle dependency:

You need to make sure you have the Google repository included in the `build.gradle` file in the root of your project:

```gradle
repositories {
    google()
}
```

Next add a dependency in the `build.gradle` file of your Gradle module.

If using ConstraintLayout with the Android View system, add:

```gradle
dependencies {

    implementation("androidx.constraintlayout:constraintlayout:2.1.1")

}
```

### 🎒🥾 Requirements

- AndroidX (Your `gradle.properties` **must** include `android.useAndroidX=true`)
- Min SDK 14+
- Java 8+

### ✨🤩📱 Key Features

Hello World

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    >

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Hello World!"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintBottom_toBottomOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

📐 [Aspect Ratio](https://developer.android.com/reference/androidx/constraintlayout/widget/ConstraintLayout#ratio) defines one dimension of a widget as a ratio of the other one. If both `width` and `height` are set to `0dp` the system sets the largest dimensions that satisfy all constraints while maintaining the aspect ratio.

```xml
<ImageView
    android:id="@+id/image_1"
    android:layout_width="0dp"
    android:layout_height="0dp"
    app:layout_constraintDimensionRatio="1:1"
    app:layout_constraintEnd_toEndOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintTop_toTopOf="parent"
    app:layout_constraintBottom_toBottomOf="parent"
    tools:src="@tools:sample/avatars" />
```

⛓️ [Chains](https://developer.android.com/reference/androidx/constraintlayout/widget/ConstraintLayout#Chains) provide group-like behavior in a single axis (horizontally or vertically). The other axis can be constrained independently.

🦮 [Guidelines](https://developer.android.com/reference/androidx/constraintlayout/widget/Guideline) allow reactive layout behavior with fixed or percentage based positioning for multiple widgets.

```xml
<androidx.constraintlayout.widget.Guideline
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:id="@+id/guideline"
    app:layout_constraintGuide_begin="100dp"
    android:orientation="vertical"/>
```

🚧 [Barrier](https://developer.android.com/reference/androidx/constraintlayout/widget/Barrier) references multiple widgets to create a virtual guideline based on the most extreme widget on the specified side.

```xml
<androidx.constraintlayout.widget.Barrier
    android:id="@+id/barrier"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:barrierDirection="start"
    app:constraint_referenced_ids="button1,button2" />
```

☂️ [Group](https://developer.android.com/reference/androidx/constraintlayout/widget/Group) controls the visibility of a set of referenced widgets.

```xml
<androidx.constraintlayout.widget.Group
    android:id="@+id/group"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:visibility="visible"
    app:constraint_referenced_ids="button4,button9" />
```

💫 [MotionLayout](https://developer.android.com/reference/androidx/constraintlayout/motion/widget/MotionLayout) a subclass of ConstraintLayout that supports transitions between constraint sets defined in MotionScenes. See [projects/MotionLayoutExperiments](projects/MotionLayoutExperiments) for examples.

🌊 [Flow](https://developer.android.com/reference/androidx/constraintlayout/helper/widget/Flow) is a VirtualLayout that allows positioning of referenced widgets horizontally or vertically similar to a Chain. If the referenced elements do not fit within the given bounds it has the ability to wrap them and create multiple chains. See [projects/CalculatorExperiments](projects/CalculatorExperiments) for examples.

🌀 [CircularFlow](https://developer.android.com/reference/androidx/constraintlayout/helper/widget/CircularFlow) is a VirtualLayout that easily organize objects in a circular pattern. See [projects/CarouselExperiments](projects/CarouselExperiments) for basic examples and [projects/MotionLayoutVerification](projects/MotionLayoutVerification) for examples with MotionLayout.
```xml
<androidx.constraintlayout.helper.widget.CircularFlow
   android:id="@+id/circularFlow"
   android:layout_width="match_parent"
   android:layout_height="match_parent"
   app:circularflow_angles="0,40,80,120"
   app:circularflow_radiusInDP="90,100,110,120"
   app:circularflow_viewCenter="@+id/view1"
   app:constraint_referenced_ids="view2,view3,view4,view5" />
```

## 📚👩‍🏫 Learning Materials

- [Build a Responsive UI with ConstraintLayout](https://developer.android.com/training/constraint-layout)
- [ConstraintLayout Codelab](https://codelabs.developers.google.com/codelabs/constraint-layout/index.html#0)
- Introduction to MotionLayout [Part I](https://medium.com/google-developers/introduction-to-motionlayout-part-i-29208674b10d) | [Part II](https://medium.com/google-developers/introduction-to-motionlayout-part-ii-a31acc084f59) | [Part III](https://medium.com/google-developers/introduction-to-motionlayout-part-iii-47cd64d51a5) | [Part IV](https://medium.com/google-developers/defining-motion-paths-in-motionlayout-6095b874d37)
- [MotionLayout Codelab](https://codelabs.developers.google.com/codelabs/motion-layout#0)

## 💻 Authors

- **John Hoford** : MotionLayout ([jafu888](https://github.com/jafu888))
- **Nicolas Roard** : ConstraintLayout ([camaelon](https://github.com/camaelon))

See also the list of [contributors](https://github.com/androidx/constraintlayout/graphs/contributors) who participated in this project.

## 🔖 License

This project is licensed under the Apache 2.0 License - see the [LICENSE](LICENSE) file for details
