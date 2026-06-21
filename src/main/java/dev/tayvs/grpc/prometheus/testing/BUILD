load("@rules_java//java:defs.bzl", "java_library")

package(default_visibility = ["//visibility:public"])

# gRPC test service implementation only — no prometheus dependency.
#
# The version-specific RegistryHelper / PrometheusRegistryHelper utilities used to live here.
# They have been moved next to the integration tests that use them (one copy per
# src/test/.../integration/<module> directory), since each is compiled against a different
# Prometheus client version.
java_library(
    name = "testing",
    testonly = 1,
    srcs = ["HelloServiceImpl.java"],
    deps = [
        "//src/main/proto:hello_java_proto",
        "//src/main/proto:hello_proto_grpc",
        "//third_party/grpc",
    ],
)
