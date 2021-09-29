-keep class androidx.test.** {*;}
-keep class androidx.camera.testing.** {*;}
-keep class androidx.camera.integration.extensions.CameraExtensionsActivity {*;}

-keepclassmembers class androidx.camera.lifecycle.ProcessCameraProvider {
    ** shutdown();
}

-keepclassmembers class androidx.camera.extensions.ExtensionsManager {
    ** shutdown();
}

-keep class org.junit.** {*;}
-keep class org.hamcrest.** {*;}
