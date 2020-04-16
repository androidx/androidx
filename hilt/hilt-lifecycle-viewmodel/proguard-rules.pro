# Keep class names of Hilt injected ViewModels since their name are used as a multibinding map key.
-keepclasseswithmembernames class * extends androidx.lifecycle.ViewModel {
    @androidx.hilt.lifecycle.ViewModelInject
    <init>(...);
}