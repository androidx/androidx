package foo.flavor.account

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import foo.LoginDirections
import foo.R

public class LoginFragmentDirections private constructor() {
  public companion object {
    public fun register(): NavDirections = ActionOnlyNavDirections(R.id.register)

    public fun actionDone(): NavDirections = LoginDirections.actionDone()
  }
}
