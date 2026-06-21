load("@rules_jvm_external//:defs.bzl", "java_export")

# Maven coordinates shared by every published artifact.
#
# GROUP is the verified Maven Central (Sonatype) namespace for GitHub user `tayvs`.
# VERSION carries the `-SNAPSHOT` suffix so these publish to the snapshot repository;
# drop the suffix to cut a release.
GROUP = "io.github.tayvs"

VERSION = "1.0.0-SNAPSHOT"

# To deploy to the local maven repo:
# > bazel run --define "maven_repo=file://$HOME/.m2/repository" //:maven_export_lib.publish
#
# To publish a snapshot to Maven Central (Sonatype):
# > bazel run --define "maven_repo=https://central.sonatype.com/repository/maven-snapshots/" \
#       --define "gpg_sign=true" --define "maven_user=$SONATYPE_USER" \
#       --define "maven_password=$SONATYPE_PASSWORD" //:maven_export_lib.publish

java_export(
    name = "maven_export_lib",
    # Exclude classes from these targets from being packaged into the jar itself.
    deploy_env = [
        "//third_party/grpc",
    ],
    maven_coordinates = "%s:java-grpc-prometheus:%s" % (GROUP, VERSION),
    pom_template = "//:pom_template.xml",
    # Make sure these show up in the dependencies of the resulting POM.
    runtime_deps = [
        "//src/main/java/dev/tayvs/grpc/prometheus",
        "//third_party/grpc",
    ],
)

# Provider module: prometheus simpleclient 0.9.x (legacy).
java_export(
    name = "maven_export_simpleclient_legacy",
    # Exclude classes from these targets from being packaged into the jar itself;
    # they are published separately and pulled in transitively via the POM.
    deploy_env = [
        ":maven_export_lib",
        "//third_party/grpc",
        "@maven_0_9//:io_prometheus_simpleclient",
    ],
    maven_coordinates = "%s:java-grpc-prometheus-simpleclient-legacy:%s" % (GROUP, VERSION),
    pom_template = "//:pom_template.xml",
    # Make sure these show up in the dependencies of the resulting POM.
    runtime_deps = [
        ":maven_export_lib",
        "//src/main/java/dev/tayvs/grpc/simple_client_0_9:prometheus",
        "//third_party/grpc",
        "@maven_0_9//:io_prometheus_simpleclient",
    ],
)

# Provider module: prometheus simpleclient 0.16.x.
java_export(
    name = "maven_export_simpleclient",
    deploy_env = [
        ":maven_export_lib",
        "//third_party/grpc",
        "@maven_0_16//:io_prometheus_simpleclient",
    ],
    maven_coordinates = "%s:java-grpc-prometheus-simpleclient:%s" % (GROUP, VERSION),
    pom_template = "//:pom_template.xml",
    runtime_deps = [
        ":maven_export_lib",
        "//src/main/java/dev/tayvs/grpc/simple_client_0_16:prometheus",
        "//third_party/grpc",
        "@maven_0_16//:io_prometheus_simpleclient",
    ],
)

# Provider module: prometheus-metrics-core 1.6+ (1.8.x).
# Skip javadoc generation: rules_jvm_external runs javadoc over the transitive
# source set, and the prometheus-metrics-core source jar references
# io.prometheus.metrics.annotations.StableApi, which is absent from the javadoc
# classpath and breaks the build. A stub javadoc jar is emitted instead.
java_export(
    name = "maven_export_metrics",
    tags = ["no-javadocs"],
    deploy_env = [
        ":maven_export_lib",
        "//third_party/grpc",
        "@maven_1_x//:io_prometheus_prometheus_metrics_core",
        "@maven_1_x//:io_prometheus_prometheus_metrics_model",
    ],
    maven_coordinates = "%s:java-grpc-prometheus-metrics:%s" % (GROUP, VERSION),
    pom_template = "//:pom_template.xml",
    runtime_deps = [
        ":maven_export_lib",
        "//src/main/java/dev/tayvs/grpc/prometheus_metrics_1:prometheus",
        "//third_party/grpc",
        "@maven_1_x//:io_prometheus_prometheus_metrics_core",
        "@maven_1_x//:io_prometheus_prometheus_metrics_model",
    ],
)

# Provider module: prometheus-metrics-core 1.5.x (pre-1.6 name validation).
# Skips javadoc generation for the same reason as the 1.x metrics module above.
java_export(
    name = "maven_export_metrics_legacy",
    tags = ["no-javadocs"],
    deploy_env = [
        ":maven_export_lib",
        "//third_party/grpc",
        "@maven_1_5//:io_prometheus_prometheus_metrics_core",
        "@maven_1_5//:io_prometheus_prometheus_metrics_model",
    ],
    maven_coordinates = "%s:java-grpc-prometheus-metrics-legacy:%s" % (GROUP, VERSION),
    pom_template = "//:pom_template.xml",
    runtime_deps = [
        ":maven_export_lib",
        "//src/main/java/dev/tayvs/grpc/prometheus_metrics_1_5:prometheus",
        "//third_party/grpc",
        "@maven_1_5//:io_prometheus_prometheus_metrics_core",
        "@maven_1_5//:io_prometheus_prometheus_metrics_model",
    ],
)
