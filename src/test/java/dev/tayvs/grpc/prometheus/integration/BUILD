load("@rules_java//java:defs.bzl", "java_test")

java_test(
    name = "server_test",
    size = "small",
    srcs = ["MonitoringServerInterceptorIntegrationTest.java"],
    test_class = "dev.tayvs.grpc.prometheus.integration.MonitoringServerInterceptorIntegrationTest",
    deps = [
        "//src/main/java/dev/tayvs/grpc/prometheus/testing",
        #      TODO:   Target both modules
        "//src/main/java/dev/tayvs/grpc/prometheus",
        "//src/main/java/dev/tayvs/grpc/simple_client_after_10:prometheus",
        "//src/main/proto:hello_java_proto",
        "//src/main/proto:hello_proto_grpc",
        "//third_party/grpc",
        "//third_party/grpc:grpc_testing",
        "//third_party/guava",
        "//third_party/prometheus",
        "//third_party/testing",
    ],
)

java_test(
    name = "client_test",
    size = "small",
    srcs = ["MonitoringClientInterceptorIntegrationTest.java"],
    test_class = "dev.tayvs.grpc.prometheus.integration.MonitoringClientInterceptorIntegrationTest",
    deps = [
        "//src/main/java/dev/tayvs/grpc/prometheus",
        "//src/main/java/dev/tayvs/grpc/prometheus/testing",
        "//src/main/java/dev/tayvs/grpc/simple_client_after_10:prometheus",
        "//src/main/proto:hello_java_proto",
        "//src/main/proto:hello_proto_grpc",
        "//third_party/grpc",
        "//third_party/grpc:grpc_testing",
        "//third_party/guava",
        "//third_party/prometheus",
        "//third_party/testing",
    ],
)
