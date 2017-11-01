-keepattributes *Annotation*

-keepclassmembers enum android.arch.lifecycle.Lifecycle$Event {
    <fields>;
}

-keep class * implements android.arch.lifecycle.LifecycleObserver {
}

-keep class * implements android.arch.lifecycle.GeneratedAdapter {
    <init>(...);
}

-keepclassmembers class ** {
    @android.arch.lifecycle.OnLifecycleEvent *;
}