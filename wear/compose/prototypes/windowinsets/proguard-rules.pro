# Preserve WearApiVersionHelper and its methods for reflection and runtime API checks
-keep class androidx.wear.utils.WearApiVersionHelper {
    public static boolean isApiVersionAtLeast(java.lang.String);
    public static final java.lang.String WEAR_CINNAMON_BUN_2;
}

# Preserve WearSettings method for reflection
-keep class com.google.wear.settings.WearSettings {
    public static boolean isStatusBarEnabled(android.content.Context);
}

