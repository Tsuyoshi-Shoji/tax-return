import os
from pathlib import Path
import subprocess
import tempfile
import unittest


DEPLOY_SCRIPT = Path(__file__).resolve().parents[1] / "deploy.sh"


class DeployServiceTest(unittest.TestCase):
    def run_step(self, step, **environment):
        script = DEPLOY_SCRIPT.read_text()
        section = script.split(f'log_section "Step {step}:', 1)[1]
        section = section.split("\n", 1)[1].split(
            "# ============================================================", 1)[0]
        with tempfile.TemporaryDirectory() as directory:
            log = Path(directory) / "deploy.log"
            result = subprocess.run(
                ["bash", "-c", """
set -euo pipefail
log() { printf '%s\\n' "$*"; }
log_error() { printf 'ERROR: %s\\n' "$*"; }
systemctl() {
  printf 'systemctl %s\\n' "$*"
  case "$1" in
    stop) printf 'stop diagnostic\\n' >&2; return "$STOP_STATUS" ;;
    start) printf 'start diagnostic\\n' >&2; return "$START_STATUS" ;;
    is-active) return "$ACTIVE_STATUS" ;;
    *) exit 99 ;;
  esac
}
pgrep() { return "$PROCESS_STATUS"; }
sleep() { :; }
""" + section + "\nprintf 'STEP_COMPLETED\\n'\n"],
                env={
                    **os.environ, "DEPLOY_LOG": str(log),
                    "STOP_STATUS": "0", "START_STATUS": "0",
                    "ACTIVE_STATUS": "0", "PROCESS_STATUS": "1",
                    **environment,
                },
                capture_output=True, text=True, timeout=5,
            )
            return result, log.read_text() if log.exists() else ""

    def test_stop_uses_systemd_even_when_no_tomcat_process_exists(self):
        result, log = self.run_step(4)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("systemctl stop tomcat.service", result.stdout)
        self.assertIn("stop diagnostic", result.stdout)
        self.assertIn("stop diagnostic", log)
        self.assertIn("STEP_COMPLETED", result.stdout)

    def test_stop_failure_is_visible_and_aborts(self):
        result, log = self.run_step(4, STOP_STATUS="1")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("stop diagnostic", result.stdout)
        self.assertIn("stop diagnostic", log)
        self.assertNotIn("STEP_COMPLETED", result.stdout)

    def test_remaining_process_aborts_before_war_cleanup(self):
        result, _ = self.run_step(4, PROCESS_STATUS="0")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Tomcat did not stop cleanly", result.stdout)
        self.assertNotIn("STEP_COMPLETED", result.stdout)

    def test_start_checks_service_state(self):
        result, log = self.run_step(7)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("systemctl start tomcat.service", result.stdout)
        self.assertIn("systemctl is-active --quiet tomcat.service", result.stdout)
        self.assertIn("start diagnostic", result.stdout)
        self.assertIn("start diagnostic", log)
        self.assertIn("STEP_COMPLETED", result.stdout)

    def test_start_failure_is_visible_and_aborts(self):
        result, log = self.run_step(7, START_STATUS="1")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("start diagnostic", result.stdout)
        self.assertIn("start diagnostic", log)
        self.assertNotIn("STEP_COMPLETED", result.stdout)

    def test_inactive_service_aborts_before_health_check(self):
        result, _ = self.run_step(7, ACTIVE_STATUS="3")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Tomcat service is not active after startup", result.stdout)
        self.assertNotIn("STEP_COMPLETED", result.stdout)


if __name__ == "__main__":
    unittest.main()
