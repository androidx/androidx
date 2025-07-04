package foo.flavor.account

import androidx.`annotation`.CheckResult
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import foo.LoginDirections
import foo.R

public class LoginFragmentDirections private constructor() {
  public companion object {
    @CheckResult
    public fun register(): NavDirections = ActionOnlyNavDirections(R.id.register)

    @CheckResult
    public fun actionDone(): NavDirections = LoginDirections.actionDone()
  }
}
