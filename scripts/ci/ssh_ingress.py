#!/usr/bin/env python3
"""Manage only repository-tagged, expiring IPv4 SSH ingress rules."""

import argparse
import ipaddress
import json
import os
import re
import subprocess
import sys
import time

MANAGER = "github-actions-ssh"
TTL_SECONDS = 3600


def aws(*args):
    result = subprocess.run(
        ["aws", "ec2", *args, "--output", "json", "--no-cli-pager"],
        check=True, capture_output=True, text=True, timeout=60,
    )
    return json.loads(result.stdout) if result.stdout.strip() else {}


def settings():
    group = os.environ["EC2_SECURITY_GROUP_ID"]
    repository = os.environ["GITHUB_REPOSITORY"]
    if not re.fullmatch(r"sg-[0-9a-f]+", group):
        raise ValueError("Invalid EC2_SECURITY_GROUP_ID")
    if repository != "Tsuyoshi-Shoji/tax-return":
        raise ValueError("Unexpected deployment repository")
    return group, repository


def run_id():
    return os.environ["GITHUB_RUN_ID"] + "-" + os.environ["GITHUB_RUN_ATTEMPT"]


def open_rule(ip):
    address = ipaddress.IPv4Address(ip)
    if not address.is_global:
        raise ValueError("Runner IP must be a public IPv4 address")
    group, repository = settings()
    tags = [
        {"Key": "ManagedBy", "Value": MANAGER},
        {"Key": "Repository", "Value": repository},
        {"Key": "RunId", "Value": run_id()},
        {"Key": "ExpiresAt", "Value": str(int(time.time()) + TTL_SECONDS)},
    ]
    result = aws(
        "authorize-security-group-ingress", "--group-id", group,
        "--ip-permissions", json.dumps([{
            "IpProtocol": "tcp", "FromPort": 22, "ToPort": 22,
            "IpRanges": [{"CidrIp": str(address) + "/32",
                          "Description": MANAGER + " " + run_id()}],
        }]),
        "--tag-specifications", json.dumps([{
            "ResourceType": "security-group-rule", "Tags": tags,
        }]),
    )
    if not result.get("Return"):
        raise RuntimeError("AWS did not confirm SSH ingress creation")
    print("Created temporary SSH rule for run " + run_id(), flush=True)


def close_rules(expired=False):
    group, repository = settings()
    filters = [
        {"Name": "group-id", "Values": [group]},
        {"Name": "tag:ManagedBy", "Values": [MANAGER]},
        {"Name": "tag:Repository", "Values": [repository]},
    ]
    if not expired:
        filters.append({"Name": "tag:RunId", "Values": [run_id()]})
    rules = aws("describe-security-group-rules", "--filters", json.dumps(filters))
    now = int(time.time())
    for rule in rules["SecurityGroupRules"]:
        tags = {tag["Key"]: tag["Value"] for tag in rule.get("Tags", [])}
        # Recheck scope before deleting; never touch existing administrator rules.
        if (rule["GroupId"] != group or rule["IsEgress"]
                or rule["IpProtocol"] != "tcp"
                or rule.get("FromPort") != 22 or rule.get("ToPort") != 22
                or tags.get("ManagedBy") != MANAGER
                or tags.get("Repository") != repository
                or (not expired and tags.get("RunId") != run_id())):
            raise ValueError("Refusing to revoke an unexpected security group rule")
        network = ipaddress.IPv4Network(rule["CidrIpv4"])
        if network.prefixlen != 32:
            raise ValueError("Refusing to revoke a non-/32 rule")
        if expired and int(tags["ExpiresAt"]) > now:
            continue
        rule_id = rule["SecurityGroupRuleId"]
        result = aws("revoke-security-group-ingress", "--group-id", group,
                     "--security-group-rule-ids", rule_id)
        if not result.get("Return"):
            raise RuntimeError("AWS did not confirm SSH ingress removal")
        print("Revoked managed SSH rule " + rule_id, flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("operation", choices=["open", "close", "expired"])
    parser.add_argument("--ip")
    args = parser.parse_args()
    if args.operation == "open":
        if not args.ip:
            parser.error("open requires --ip")
        open_rule(args.ip)
    else:
        close_rules(expired=args.operation == "expired")


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError as error:
        print(error.stderr, file=sys.stderr)
        sys.exit(error.returncode)
