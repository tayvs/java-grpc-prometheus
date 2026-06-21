load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

GRPC_JAVA_VERSION = "1.81.0"

http_archive(
    name = "io_grpc_grpc_java",
    strip_prefix = "grpc-java-%s" % GRPC_JAVA_VERSION,
    url = "https://github.com/grpc/grpc-java/archive/v%s.zip" % GRPC_JAVA_VERSION,
)

load("@io_grpc_grpc_java//:repositories.bzl", "IO_GRPC_GRPC_JAVA_ARTIFACTS", "IO_GRPC_GRPC_JAVA_OVERRIDE_TARGETS", "grpc_java_repositories")

RULES_JVM_EXTERNAL_TAG = "6.6"

http_archive(
    name = "rules_jvm_external",
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    urls = ["https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG],
)

load("@rules_jvm_external//:repositories.bzl", "rules_jvm_external_deps")

rules_jvm_external_deps()

load("@rules_jvm_external//:setup.bzl", "rules_jvm_external_setup")

rules_jvm_external_setup()

load("@rules_jvm_external//:defs.bzl", "maven_install")

SIMPLE_CLIENT_0_9 = [
    "io.prometheus:simpleclient:0.9.0",
]

SIMPLE_CLIENT_0_16 = [
    "io.prometheus:simpleclient:0.16.0",
]

PROMETHEUS_CLIENT_1_5 = [
    "io.prometheus:prometheus-metrics-core:1.5.0",
]

PROMETHEUS_CLIENT_1_x = [
    "io.prometheus:prometheus-metrics-core:1.8.0",
]

MAVEN_ARTIFACTS = [
    "com.google.cloud:google-cloud-core:2.71.0",
    "com.google.cloud:google-cloud-storage:2.69.0",
    "com.google.truth:truth:1.4.5",
    "io.grpc:grpc-api:%s" % GRPC_JAVA_VERSION,
    "io.grpc:grpc-stub:%s" % GRPC_JAVA_VERSION,
    "org.junit.jupiter:junit-jupiter-api:6.1.0",
    "org.mockito:mockito-core:5.23.0",
]

maven_install(
    name = "maven",
    artifacts = MAVEN_ARTIFACTS + IO_GRPC_GRPC_JAVA_ARTIFACTS,
    fetch_sources = True,
    override_targets = IO_GRPC_GRPC_JAVA_OVERRIDE_TARGETS,
    repositories = [
        "https://maven.google.com",
        "https://repo1.maven.org/maven2",
    ],
)

maven_install(
    name = "maven_0_16",
    artifacts = SIMPLE_CLIENT_0_16,
    fetch_sources = True,
    repositories = [
        "https://maven.google.com",
        "https://repo1.maven.org/maven2",
    ],
)

maven_install(
    name = "maven_0_9",
    artifacts = SIMPLE_CLIENT_0_9,
    fetch_sources = True,
    repositories = [
        "https://maven.google.com",
        "https://repo1.maven.org/maven2",
    ],
)

maven_install(
    name = "maven_1_5",
    artifacts = PROMETHEUS_CLIENT_1_5,
    fetch_sources = True,
    repositories = [
        "https://maven.google.com",
        "https://repo1.maven.org/maven2",
    ],
)

maven_install(
    name = "maven_1_x",
    artifacts = PROMETHEUS_CLIENT_1_x,
    fetch_sources = True,
    repositories = [
        "https://maven.google.com",
        "https://repo1.maven.org/maven2",
    ],
)

# Run grpc_java_repositories after maven_install to ensure the
# maven_install-selected dependencies are used.
grpc_java_repositories()

load("@com_google_protobuf//:protobuf_deps.bzl", "protobuf_deps")

protobuf_deps()

load("@rules_python//python:repositories.bzl", "py_repositories")

py_repositories()
