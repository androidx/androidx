package foo.flavor;

import android.support.annotation.NonNull;
import foo.SettingsDirections;

public class SettingsFragmentDirections {
    private SettingsFragmentDirections() {
    }

    @NonNull
    public static SettingsDirections.Exit exit() {
        return SettingsDirections.exit();
    }
}