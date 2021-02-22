package foo.flavor.account

import androidx.navigation.NavDirections
import foo.LoginDirections

public class RegisterFragmentDirections private constructor() {
  public companion object {
    public fun actionDone(): NavDirections = LoginDirections.actionDone()
  }
}
