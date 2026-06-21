#!/usr/bin/env bash
#
# Publish the java-grpc-prometheus artifacts via rules_jvm_external `*.publish` targets.
#
# Everything below uses `: "${VAR:=default}"`, so an already-exported environment
# variable always wins over the default written here. Edit the defaults for your
# common case, or override per-run, e.g.:
#
#   MAVEN_USER=abc MAVEN_PASSWORD=xyz ./publish.sh
#   MAVEN_REPO="file://$HOME/.m2/repository" ./publish.sh           # local dry-run
#   ./publish.sh maven_export_lib maven_export_metrics             # only some targets
#
set -euo pipefail

# ----------------------------------------------------------------------------
# Config — edit defaults here, or override via the environment.
# ----------------------------------------------------------------------------

# Target Maven repo. Default: Central Portal snapshot repository.
#   - Local dry-run:  file://$HOME/.m2/repository
#   - Central snaps:  https://central.sonatype.com/repository/maven-snapshots/
: "${MAVEN_REPO:=https://central.sonatype.com/repository/maven-snapshots/}"

# Credentials (Central Portal user token: central.sonatype.com -> Account ->
# Generate User Token). Leave empty for a local file:// publish.
: "${MAVEN_USER:=}"
: "${MAVEN_PASSWORD:=}"

# GPG signing. Not required for snapshots; required for Maven Central releases.
#   GPG_SIGN=true                      -> sign using the local `gpg` binary
#   USE_IN_MEMORY_PGP_KEYS=true        -> sign without a gpg binary (CI); also set
#     PGP_SIGNING_KEY (base64 of `gpg --export-secret-keys --armor KEYID`) and
#     PGP_SIGNING_PWD (the key passphrase).
: "${GPG_SIGN:=false}"
: "${USE_IN_MEMORY_PGP_KEYS:=false}"
: "${PGP_SIGNING_KEY:=}"
: "${PGP_SIGNING_PWD:=}"

# Default set of publish targets (override by passing target names as args).
DEFAULT_TARGETS=(
  maven_export_lib
  maven_export_simpleclient_legacy
  maven_export_simpleclient
  maven_export_metrics
  maven_export_metrics_legacy
)

# ----------------------------------------------------------------------------
# Run — usually no need to edit below.
# ----------------------------------------------------------------------------

if [[ $# -gt 0 ]]; then
  TARGETS=("$@")
else
  TARGETS=("${DEFAULT_TARGETS[@]}")
fi

export MAVEN_REPO MAVEN_USER MAVEN_PASSWORD
export GPG_SIGN USE_IN_MEMORY_PGP_KEYS PGP_SIGNING_KEY PGP_SIGNING_PWD

echo "Publishing to: ${MAVEN_REPO}"
echo "Targets:       ${TARGETS[*]}"
[[ -n "${MAVEN_USER}" ]] && echo "Auth user:     ${MAVEN_USER}"
echo

for t in "${TARGETS[@]}"; do
  echo "==> //:${t}.publish"
  bazel run "//:${t}.publish"
done

echo
echo "Done."
