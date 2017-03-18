# Change Log

## [23.1.0](https://android.googlesource.com/platform/frameworks/support/+/refs/heads/master/v14/preference) (2015-09-28)

**Breakage and deprecation notices:**

- EditTextPreferenceDialogFragment
  - onAddEditTextToDialogView has been removed. Any code depending on overriding this method should
    be moved to onBindDialogView.
  - The EditText view is now expected to be present in the dialog layout file with the id
    @android:id/edit, and is no longer created in code.
