package foo.flavor;

import androidx.annotation.NonNull;
import foo.SettingsDirections;

public class SettingsFragmentDirections {
    private SettingsFragmentDirections() {
    }

    @NonNull
    public static SettingsDirections.Main main() {
        return SettingsDirections.main();
    }

    @NonNull
    public static SettingsDirections.Exit exit() {
        return SettingsDirections.exit();
    }
}