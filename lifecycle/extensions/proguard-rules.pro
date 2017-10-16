-keep class * extends android.arch.lifecycle.ViewModel {
    <init>();
}

-keep class * extends android.arch.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}