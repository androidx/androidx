package foo.flavor;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import foo.SettingsDirections;

public class SettingsFragmentDirections {
    private SettingsFragmentDirections() {
    }

    @CheckResult
    @NonNull
    public static SettingsDirections.Main main() {
        return SettingsDirections.main();
    }

    @CheckResult
    @NonNull
    public static SettingsDirections.Exit exit() {
        return SettingsDirections.exit();
    }
}