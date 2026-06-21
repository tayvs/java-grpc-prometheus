load("@rules_java//java:defs.bzl", "java_library")

package(default_visibility = ["//visibility:public"])

# Compile/runtime dependency for the published library.
#
# We export the `original_*` aliases instead of the plain
# `@maven//:io_grpc_grpc_*` targets on purpose. WORKSPACE passes
# `override_targets = IO_GRPC_GRPC_JAVA_OVERRIDE_TARGETS` to maven_install,
# which rewrites `@maven//:io_grpc_grpc_api` into an alias to the *source*
# build `@io_grpc_grpc_java//api`. That source target carries no
# `maven_coordinates`, so java_export's POM generator can't emit it as a
# `<dependency>` and instead descends into grpc's own transitive deps,
# leaking guava, jsr305, error_prone, failureaccess and animal-sniffer into
# every published POM while omitting grpc-api/grpc-stub themselves.
#
# rules_jvm_external keeps the real Maven jvm_import (with coordinates) under
# the `original_` prefix. Exporting those makes the POM list exactly
# io.grpc:grpc-api and io.grpc:grpc-stub and stop there. The source-built grpc
# is still used by tests via :grpc_testing below.
java_library(
    name = "grpc",
    licenses = ["permissive"],
    exports = [
        "@maven//:original_io_grpc_grpc_api",
        "@maven//:original_io_grpc_grpc_stub",
    ],
)

java_library(
    name = "grpc_testing",
    testonly = 1,
    licenses = ["permissive"],
    exports = [
        "@io_grpc_grpc_java//testing",
    ],
)
