load("@rules_java//java:defs.bzl", "java_test")

java_test(
    name = "server_test",
    size = "small",
    srcs = ["MonitoringServerInterceptorIntegrationTest.java"],
    test_class = "dev.tayvs.grpc.prometheus.integration.prometheus_metrics.MonitoringServerInterceptorIntegrationTest",
    deps = [
        "//src/main/java/dev/tayvs/grpc/prometheus/testing",
        "//src/main/java/dev/tayvs/grpc/prometheus/testing:testing_prometheus_metrics",
        "//src/main/java/dev/tayvs/grpc/prometheus",
        "//src/main/java/dev/tayvs/grpc/prometheus_metrics:prometheus",
        "//src/main/proto:hello_java_proto",
        "//src/main/proto:hello_proto_grpc",
        "//third_party/grpc",
        "//third_party/grpc:grpc_testing",
        "//third_party/guava",
        "//third_party/testing",
        "@maven_3//:io_prometheus_prometheus_metrics_core",
        "@maven_3//:io_prometheus_prometheus_metrics_model",
    ],
)

java_test(
    name = "client_test",
    size = "small",
    srcs = ["MonitoringClientInterceptorIntegrationTest.java"],
    test_class = "dev.tayvs.grpc.prometheus.integration.prometheus_metrics.MonitoringClientInterceptorIntegrationTest",
    deps = [
        "//src/main/java/dev/tayvs/grpc/prometheus",
        "//src/main/java/dev/tayvs/grpc/prometheus/testing",
        "//src/main/java/dev/tayvs/grpc/prometheus/testing:testing_prometheus_metrics",
        "//src/main/java/dev/tayvs/grpc/prometheus_metrics:prometheus",
        "//src/main/proto:hello_java_proto",
        "//src/main/proto:hello_proto_grpc",
        "//third_party/grpc",
        "//third_party/grpc:grpc_testing",
        "//third_party/guava",
        "//third_party/testing",
        "@maven_3//:io_prometheus_prometheus_metrics_core",
        "@maven_3//:io_prometheus_prometheus_metrics_model",
    ],
)
