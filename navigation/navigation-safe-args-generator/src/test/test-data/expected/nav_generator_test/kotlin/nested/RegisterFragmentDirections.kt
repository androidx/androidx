package foo.flavor.account

import androidx.`annotation`.CheckResult
import androidx.navigation.NavDirections
import foo.LoginDirections

public class RegisterFragmentDirections private constructor() {
  public companion object {
    @CheckResult
    public fun actionDone(): NavDirections = LoginDirections.actionDone()
  }
}
