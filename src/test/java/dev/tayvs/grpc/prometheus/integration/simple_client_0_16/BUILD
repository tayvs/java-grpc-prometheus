load("@rules_java//java:defs.bzl", "java_test")

java_test(
    name = "server_test",
    size = "small",
    srcs = [
        "MonitoringServerInterceptorIntegrationTest.java",
        "RegistryHelper.java",
    ],
    test_class = "dev.tayvs.grpc.prometheus.integration.simple_client_0_16.MonitoringServerInterceptorIntegrationTest",
    deps = [
        "//src/main/java/dev/tayvs/grpc/prometheus",
        "//src/main/java/dev/tayvs/grpc/prometheus/testing",
        "//src/main/java/dev/tayvs/grpc/simple_client_0_16:prometheus",
        "//src/main/proto:hello_java_proto",
        "//src/main/proto:hello_proto_grpc",
        "//third_party/grpc",
        "//third_party/grpc:grpc_testing",
        "//third_party/guava",
        "//third_party/testing",
        "@maven_0_16//:io_prometheus_simpleclient",
    ],
)

java_test(
    name = "client_test",
    size = "small",
    srcs = [
        "MonitoringClientInterceptorIntegrationTest.java",
        "RegistryHelper.java",
    ],
    test_class = "dev.tayvs.grpc.prometheus.integration.simple_client_0_16.MonitoringClientInterceptorIntegrationTest",
    deps = [
        "//src/main/java/dev/tayvs/grpc/prometheus",
        "//src/main/java/dev/tayvs/grpc/prometheus/testing",
        "//src/main/java/dev/tayvs/grpc/simple_client_0_16:prometheus",
        "//src/main/proto:hello_java_proto",
        "//src/main/proto:hello_proto_grpc",
        "//third_party/grpc",
        "//third_party/grpc:grpc_testing",
        "//third_party/guava",
        "//third_party/testing",
        "@maven_0_16//:io_prometheus_simpleclient",
    ],
)
