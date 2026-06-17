load("@rules_java//java:defs.bzl", "java_library")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "testing",
    licenses = ["permissive"],
    exports = [
        "@io_grpc_grpc_java//core:inprocess",
        "@maven//:com_google_truth_truth",
        "@maven//:org_junit_jupiter_junit_jupiter_api",
        "@maven//:org_mockito_mockito_core",
    ],
)
