## Introduction

Describes steps to troubleshoot the AndroidX GitHub workflow.

## F.A.Q

### Workflows are not automatically triggered ?

- You can manually trigger a `Workflow`. To do this, you will need a GitHub personal access token. You can create a token by going to `Settings -> Developer Settings -> Personal Access Tokens`. Make sure you have the `repo` and `workflow` scopes selected when creating the token. You can then trigger the workflow by using this command.

  ``` bash
  curl -X "POST" "https://api.github.com/repos/<username>/androidx/actions/workflows/presubmit.yml/dispatches" \
     -H 'Accept: application/vnd.github.v3+json' \
     -H 'Authorization: token <access_token>' \
     -H 'Content-Type: text/plain; charset=utf-8' \
     -d $'{"ref": "<your branch name>"}'
  ```
