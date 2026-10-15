import importlib.util
import json
import os
from pathlib import Path
import subprocess
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location(
    "ssh_ingress", Path(__file__).with_name("ssh_ingress.py"))
ingress = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ingress)


class SshIngressTest(unittest.TestCase):
    def setUp(self):
        self.environment = patch.dict(os.environ, {
            "EC2_SECURITY_GROUP_ID": "sg-123abc",
            "GITHUB_REPOSITORY": "Tsuyoshi-Shoji/tax-return",
            "GITHUB_RUN_ID": "123", "GITHUB_RUN_ATTEMPT": "2",
        })
        self.environment.start()
        self.addCleanup(self.environment.stop)

    def rule(self, expiry="1000"):
        return {
            "GroupId": "sg-123abc", "IsEgress": False, "IpProtocol": "tcp",
            "FromPort": 22, "ToPort": 22, "CidrIpv4": "8.8.8.8/32",
            "SecurityGroupRuleId": "sgr-123abc",
            "Tags": [
                {"Key": "ManagedBy", "Value": ingress.MANAGER},
                {"Key": "Repository", "Value": "Tsuyoshi-Shoji/tax-return"},
                {"Key": "RunId", "Value": "123-2"},
                {"Key": "ExpiresAt", "Value": expiry},
            ],
        }

    @patch.object(ingress.time, "time", return_value=1000)
    @patch.object(ingress, "aws", return_value={"Return": True})
    def test_open_is_ipv4_tcp22_single_address_with_one_hour_expiry(self, aws, clock):
        ingress.open_rule("8.8.8.8")
        args = aws.call_args.args
        self.assertEqual(args[2], "sg-123abc")
        permission, = json.loads(args[4])
        self.assertEqual((permission["IpProtocol"], permission["FromPort"],
                          permission["ToPort"]), ("tcp", 22, 22))
        self.assertEqual(permission["IpRanges"][0]["CidrIp"], "8.8.8.8/32")
        tags = {t["Key"]: t["Value"] for t in json.loads(args[6])[0]["Tags"]}
        self.assertEqual(tags["ExpiresAt"], "4600")
        self.assertEqual(tags["RunId"], "123-2")

    @patch.object(ingress, "aws")
    def test_invalid_or_nonpublic_ip_does_not_call_aws(self, aws):
        for address in ("0.0.0.0/0", "127.0.0.1", "10.0.0.1", "::1", "invalid"):
            with self.subTest(address=address), self.assertRaises(ValueError):
                ingress.open_rule(address)
        aws.assert_not_called()

    @patch.object(ingress, "aws")
    def test_close_filters_by_run_and_revokes_only_returned_rule_id(self, aws):
        aws.side_effect = [{"SecurityGroupRules": [self.rule()]}, {"Return": True}]
        ingress.close_rules()
        filters = json.loads(aws.call_args_list[0].args[2])
        self.assertIn({"Name": "tag:RunId", "Values": ["123-2"]}, filters)
        self.assertEqual(aws.call_args_list[1].args, (
            "revoke-security-group-ingress", "--group-id", "sg-123abc",
            "--security-group-rule-ids", "sgr-123abc"))

    @patch.object(ingress.time, "time", return_value=1000)
    @patch.object(ingress, "aws")
    def test_expired_cleanup_does_not_remove_unexpired_rule(self, aws, clock):
        aws.side_effect = [{"SecurityGroupRules": [
            self.rule("1001"), self.rule("1000")]}, {"Return": True}]
        ingress.close_rules(expired=True)
        self.assertEqual(aws.call_count, 2)

    @patch.object(ingress, "aws")
    def test_refuses_out_of_scope_rules(self, aws):
        for key, value in (("GroupId", "sg-other"), ("IsEgress", True),
                           ("ToPort", 443), ("CidrIpv4", "8.8.8.0/24"),
                           ("Tags", [])):
            rule = self.rule()
            rule[key] = value
            aws.reset_mock()
            aws.return_value = {"SecurityGroupRules": [rule]}
            with self.subTest(key=key), self.assertRaises(ValueError):
                ingress.close_rules()
            self.assertEqual(aws.call_count, 1)

    @patch.object(ingress, "aws", return_value={"SecurityGroupRules": []})
    def test_close_without_created_rule_is_noop(self, aws):
        ingress.close_rules()
        self.assertEqual(aws.call_count, 1)

    @patch.object(ingress, "aws", return_value={"Return": False})
    def test_creation_without_aws_confirmation_fails(self, aws):
        with self.assertRaises(RuntimeError):
            ingress.open_rule("8.8.8.8")

    @patch.object(ingress, "aws")
    def test_close_refuses_another_runs_rule(self, aws):
        rule = self.rule()
        next(tag for tag in rule["Tags"] if tag["Key"] == "RunId")["Value"] = "other-run"
        aws.return_value = {"SecurityGroupRules": [rule]}
        with self.assertRaises(ValueError):
            ingress.close_rules()
        self.assertEqual(aws.call_count, 1)

    @patch.object(ingress.subprocess, "run", side_effect=subprocess.CalledProcessError(
        1, ["aws"], stderr="AccessDenied"))
    def test_aws_failure_is_not_hidden(self, run):
        with self.assertRaises(subprocess.CalledProcessError):
            ingress.close_rules()


if __name__ == "__main__":
    unittest.main()
