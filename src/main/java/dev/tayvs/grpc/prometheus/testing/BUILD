load("@rules_java//java:defs.bzl", "java_library")

package(default_visibility = ["//visibility:public"])

# gRPC test service implementation only — no prometheus dependency.
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

# RegistryHelper compiled against simpleclient 0.9.0 (used by simple_client_before_9).
java_library(
    name = "testing_simple_client_before_9",
    testonly = 1,
    srcs = ["RegistryHelper.java"],
    exports = [
        "@maven_0_9//:io_prometheus_simpleclient",
    ],
    deps = [
        "@maven_0_9//:io_prometheus_simpleclient",
    ],
)

# RegistryHelper compiled against simpleclient 0.16.0 (used by simple_client_after_10).
java_library(
    name = "testing_simple_client_after_10",
    testonly = 1,
    srcs = ["RegistryHelper.java"],
    exports = [
        "@maven_0_16//:io_prometheus_simpleclient",
    ],
    deps = [
        "@maven_0_16//:io_prometheus_simpleclient",
    ],
)

# PrometheusRegistryHelper for prometheus-metrics 1.x (used by prometheus_metrics).
java_library(
    name = "testing_prometheus_metrics",
    testonly = 1,
    srcs = ["PrometheusRegistryHelper.java"],
    deps = [
        "@maven_1_0//:io_prometheus_prometheus_metrics_core",
        "@maven_1_0//:io_prometheus_prometheus_metrics_model",
    ],
)
