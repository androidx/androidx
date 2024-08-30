# Workaround for https://issuetracker.google.com/issues/346808608
#
# `androidx.lifecycle.compose.LocalLifecycleOwner` will reflectively lookup for
# `androidx.compose.ui.platform.LocalLifecycleOwner` to ensure backward compatibility
# when using Lifecycle 2.8+ with Compose 1.6.
#
# We need to keep the getter if the code using this is included.
#
# We need to suppress `ShrinkerUnresolvedReference` because the `LocalComposition` is in a
# different module.
#
#noinspection ShrinkerUnresolvedReference
-if public class androidx.compose.ui.platform.AndroidCompositionLocals_androidKt {
    public static *** getLocalLifecycleOwner();
}
-keep public class androidx.compose.ui.platform.AndroidCompositionLocals_androidKt {
    public static *** getLocalLifecycleOwner();
}
