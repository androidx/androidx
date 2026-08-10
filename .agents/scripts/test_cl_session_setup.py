#!/usr/bin/env python3
import io
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

import common
from common import WorkspaceContext, HostEnvironment
import cl_session_setup as setup_script


class FakeWorkspaceProvider(common.WorkspaceProvider):
    """In-memory fake provider for zero-mock testing."""
    def __init__(self, should_succeed: bool = True, exists: bool = False):
        self.should_succeed = should_succeed
        self.exists = exists
        self.created_workspaces: list[tuple[str, str]] = []
        self.synced_refs: list[tuple[str, Path, str]] = []
        self.cleaned_up: list[str] = []
        self.ensured_ready: list[str] = []

    def get_workspace_parent(self, ctx: WorkspaceContext) -> Path:
        if ctx.is_cog_workspace:
            return Path(f"/google/cog/cloud/{ctx.env.user}/{ctx.cl_name}")
        return ctx.checkout_root / ".worktrees" / ctx.cl_name

    def get_cl_dir(self, ctx: WorkspaceContext) -> Path:
        if ctx.is_cog_workspace:
            return self.get_workspace_parent(ctx) / "android" / "frameworks" / "support"
        return self.get_workspace_parent(ctx) / "frameworks" / "support"

    def workspace_exists(self, ctx: WorkspaceContext) -> bool:
        return self.exists

    def create_workspace(self, ctx: WorkspaceContext, base_ref: str = common.DEFAULT_TARGET_BRANCH) -> bool:
        if self.should_succeed:
            self.created_workspaces.append((ctx.cl_name, base_ref))
            self.exists = True
        return self.should_succeed

    def sync_workspace_to_ref(self, ctx: WorkspaceContext, source_path: Path, target_ref: str, clean: bool = False) -> bool:
        if self.should_succeed:
            self.synced_refs.append((ctx.cl_name, source_path, target_ref, clean))
        return self.should_succeed

    def sync_workspace_to_source(self, ctx: WorkspaceContext, source_path: Path, strip_change_id: bool = False) -> bool:
        if self.should_succeed:
            self.synced_refs.append((ctx.cl_name, source_path, "SOURCE_HEAD"))
        return self.should_succeed

    def cleanup_workspace(self, ctx: WorkspaceContext) -> None:
        self.cleaned_up.append(ctx.cl_name)
        self.exists = False

    def ensure_workspace_ready(self, ctx: WorkspaceContext) -> None:
        self.ensured_ready.append(ctx.cl_name)


def make_test_env(root: str = "/fake/checkout") -> HostEnvironment:
    return HostEnvironment(is_cog_supported=False, user="testuser", checkout_root=Path(root))


class FailingCogWorkspaceProvider(common.CogWorkspaceProvider):
    """Subclass Fake for testing CoG creation failure without monkey-patching."""
    def workspace_exists(self, ctx: WorkspaceContext) -> bool:
        return False

    def create_workspace(self, ctx: WorkspaceContext, base_ref: str = common.DEFAULT_TARGET_BRANCH) -> bool:
        return False


class TestClSessionSetup(unittest.TestCase):

    def test_process_session_request_creates_workspace(self):
        fake_provider = FakeWorkspaceProvider(should_succeed=True)
        env = make_test_env()
        input_data = {
            "conversationId": "test-conv",
            "workspacePaths": ["/fake/checkout/frameworks/support"],
            "useLocalHead": True
        }

        result = setup_script.process_session_request(input_data, provider=fake_provider, env=env)

        self.assertIn("injectSteps", result)
        self.assertEqual(len(fake_provider.created_workspaces), 1)
        self.assertEqual(fake_provider.created_workspaces[0][0], "cl_test-conv")

    def test_process_session_request_defaults_to_head(self):
        fake_provider = FakeWorkspaceProvider(should_succeed=True)
        env = make_test_env()
        input_data = {
            "conversationId": "test-conv-head",
            "workspacePaths": ["/fake/checkout/frameworks/support"]
        }

        result = setup_script.process_session_request(input_data, provider=fake_provider, env=env)

        self.assertIn("injectSteps", result)
        self.assertEqual(fake_provider.created_workspaces[0], ("cl_test-conv-head", "HEAD"))

    def test_process_session_request_fresh_upstream(self):
        fake_provider = FakeWorkspaceProvider(should_succeed=True)
        env = make_test_env()
        input_data = {
            "conversationId": "test-conv-upstream",
            "workspacePaths": ["/fake/checkout/frameworks/support"],
            "freshUpstream": True
        }

        result = setup_script.process_session_request(input_data, provider=fake_provider, env=env)

        self.assertIn("injectSteps", result)
        self.assertEqual(fake_provider.created_workspaces[0], ("cl_test-conv-upstream", "refs/heads/androidx-main"))

    def test_process_session_request_already_isolated(self):
        fake_provider = FakeWorkspaceProvider(should_succeed=True)
        env = make_test_env()
        input_data = {
            "conversationId": "test-conv",
            "workspacePaths": ["/fake/checkout/.worktrees/cl_test-conv/frameworks/support"]
        }

        result = setup_script.process_session_request(input_data, provider=fake_provider, env=env)
        self.assertEqual(result, {})
        self.assertEqual(len(fake_provider.created_workspaces), 0)
        self.assertEqual(fake_provider.ensured_ready, ["cl_test-conv"])

    def test_process_session_request_failure_returns_error(self):
        fake_provider = FakeWorkspaceProvider(should_succeed=False)
        env = make_test_env()
        input_data = {
            "conversationId": "test-conv",
            "workspacePaths": ["/fake/checkout/frameworks/support"]
        }

        result = setup_script.process_session_request(input_data, provider=fake_provider, env=env)

        self.assertIn("error", result)

    def test_sync_workspace_to_ref_delegates_to_provider(self):
        fake_provider = FakeWorkspaceProvider(should_succeed=True)
        env = make_test_env()
        with self.assertRaises(SystemExit) as cm:
            setup_script.handle_sync_subcommand(["4175276", "test-conv", "/fake/checkout"], provider=fake_provider, env=env)
        self.assertEqual(cm.exception.code, 0)
        self.assertEqual(fake_provider.synced_refs, [("cl_test-conv", Path("/fake/checkout"), "4175276", False)])

    def test_sync_workspace_to_ref_clean_flag(self):
        fake_provider = FakeWorkspaceProvider(should_succeed=True)
        env = make_test_env()
        with self.assertRaises(SystemExit) as cm:
            setup_script.handle_sync_subcommand(["--clean", "4175276", "test-conv", "/fake/checkout"], provider=fake_provider, env=env)
        self.assertEqual(cm.exception.code, 0)
        self.assertEqual(fake_provider.synced_refs, [("cl_test-conv", Path("/fake/checkout"), "4175276", True)])

    def test_cleanup_workspace_delegates_to_provider(self):
        fake_provider = FakeWorkspaceProvider(should_succeed=True)
        env = make_test_env()
        setup_script.handle_cleanup_subcommand("test-conv", provider=fake_provider, env=env)
        self.assertEqual(fake_provider.cleaned_up, ["cl_test-conv"])

    def test_main_cleanup_without_arg_prints_usage(self):
        with patch("sys.argv", ["cl_session_setup.py", "cleanup"]), \
             patch("sys.stderr", new_callable=io.StringIO) as mock_stderr:
            with self.assertRaises(SystemExit) as cm:
                setup_script.main()
            self.assertEqual(cm.exception.code, 1)
            self.assertIn("Usage: cl_session_setup.py cleanup", mock_stderr.getvalue())

    def test_process_session_request_cog_fallback_to_local(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            support_dir = Path(tmpdir) / "frameworks" / "support"
            support_dir.mkdir(parents=True)
            subprocess.run(["git", "-C", str(support_dir), "init"], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(support_dir), "commit", "--allow-empty", "-m", "init"], check=True, capture_output=True)

            env = HostEnvironment(is_cog_supported=False, user="testuser", checkout_root=Path(tmpdir))
            cog_failing_provider = FailingCogWorkspaceProvider()

            input_data = {
                "conversationId": "test-fallback",
                "workspacePaths": [str(support_dir)]
            }
            res = setup_script.process_session_request(input_data, provider=cog_failing_provider, env=env)
            self.assertIn("injectSteps", res)



    def test_process_session_request_subagent_worktree_on_cog_host(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            main_root = Path(tmpdir) / "main"
            main_root.mkdir()
            (main_root / "prebuilts").mkdir()
            (main_root / "tools").mkdir()

            subagent_dir = Path(tmpdir) / "brain" / "conv1" / ".system_generated" / "worktrees" / "subagent-A"
            subagent_dir.mkdir(parents=True)

            env = HostEnvironment(is_cog_supported=True, user="testuser", checkout_root=main_root)
            input_data = {
                "conversationId": "subagent-A",
                "workspacePaths": [str(subagent_dir)]
            }

            result = setup_script.process_session_request(input_data, provider=None, env=env)
            self.assertEqual(result, {})

            target_parent = subagent_dir.parent.parent
            self.assertTrue((target_parent / "prebuilts").is_symlink())
            self.assertEqual((target_parent / "prebuilts").resolve(), (main_root / "prebuilts").resolve())

    def test_main_invocation(self):
        input_data = {"conversationId": "test-main", "workspacePaths": []}
        stdin = io.StringIO(json.dumps(input_data))
        stdout = io.StringIO()
        environ = {"ANTIGRAVITY_CONVERSATION_ID": "test-main", "USER": "user"}

        setup_script.main(stdin=stdin, stdout=stdout, environ=environ)
        response = json.loads(stdout.getvalue())
        self.assertEqual(response, {})


if __name__ == '__main__':
    unittest.main()
