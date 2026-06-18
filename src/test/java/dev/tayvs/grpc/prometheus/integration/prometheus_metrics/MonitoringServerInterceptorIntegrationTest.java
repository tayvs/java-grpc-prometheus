// Copyright 2016 Dino Wernli. All Rights Reserved. See LICENSE for licensing terms.

package dev.tayvs.grpc.prometheus.integration.prometheus_metrics;

import static com.google.common.truth.Truth.assertThat;

import com.github.dinowernli.proto.grpc.prometheus.HelloProto.HelloRequest;
import com.github.dinowernli.proto.grpc.prometheus.HelloProto.HelloResponse;
import com.github.dinowernli.proto.grpc.prometheus.HelloServiceGrpc;
import com.github.dinowernli.proto.grpc.prometheus.HelloServiceGrpc.HelloServiceBlockingStub;
import com.github.dinowernli.proto.grpc.prometheus.HelloServiceGrpc.HelloServiceStub;
import com.google.common.collect.ImmutableList;
import dev.tayvs.grpc.prometheus.Provider;
import io.grpc.Channel;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.StreamRecorder;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import dev.tayvs.grpc.prometheus.Configuration;
import dev.tayvs.grpc.prometheus.MonitoringServerInterceptor;
import dev.tayvs.grpc.prometheus.testing.HelloServiceImpl;
import dev.tayvs.grpc.prometheus.testing.PrometheusRegistryHelper;
import dev.tayvs.grpc.prometheus.testing.PrometheusRegistryHelper.FamilySamples;
import dev.tayvs.grpc.prometheus.testing.PrometheusRegistryHelper.FakeSample;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integrations tests which make sure that if a service is started with a {@link
 * MonitoringServerInterceptor}, then all Prometheus metrics get recorded correctly. Uses the
 * prometheus_metrics (prometheus-metrics-core 1.x) implementation.
 *
 * <p>Note: metric names in this module omit the grpc_server_ prefix because the prometheus-metrics
 * 1.x builder does not support namespace/subsystem.
 */
public class MonitoringServerInterceptorIntegrationTest {
  private static final String grpcServerName = "grpc-server";
  private static final String RECIPIENT = "Dave";
  private static final HelloRequest REQUEST =
      HelloRequest.newBuilder().setRecipient(RECIPIENT).build();

  private static final Configuration CHEAP_METRICS = Configuration.cheapMetricsOnly();
  private static final Configuration ALL_METRICS = Configuration.allMetrics();

  private PrometheusRegistry prometheusRegistry;
  private Server grpcServer;

  @Before
  public void setUp() {
    prometheusRegistry = new PrometheusRegistry();
  }

  @After
  public void tearDown() throws Exception {
    grpcServer.shutdown().awaitTermination();
  }

  @Test
  public void unaryRpcMetrics() throws Throwable {
    startGrpcServer(CHEAP_METRICS);
    createGrpcBlockingStub().sayHello(REQUEST);

    assertThat(findRecordedMetricOrThrow("started").samples).hasSize(2);
    assertThat(findRecordedMetricNamesOrThrow("started")).contains("started_total");
    assertThat(findRecordedMetricNamesOrThrow("started")).contains("started_created");

    assertThat(findRecordedMetricOrThrow("msg_received").samples).isEmpty();
    assertThat(findRecordedMetricOrThrow("msg_sent").samples).isEmpty();

    FamilySamples handled = findRecordedMetricOrThrow("handled");
    assertThat(handled.samples).hasSize(2);
    FakeSample totalSample = getSample(handled, "handled_total");
    assertThat(totalSample.labelValues)
        .containsExactly(
            "UNARY",
            HelloServiceImpl.SERVICE_NAME,
            HelloServiceImpl.UNARY_METHOD_NAME,
            "OK",
            "OK"); // TODO: These are the "code" and "grpc_code" labels which are currently
    // duplicated. "code" should be deprecated in a future release.
    assertThat(totalSample.value).isWithin(0).of(1);
  }

  @Test
  public void clientStreamRpcMetrics() throws Throwable {
    startGrpcServer(CHEAP_METRICS);
    StreamRecorder<HelloResponse> streamRecorder = StreamRecorder.create();
    StreamObserver<HelloRequest> requestStream =
        createGrpcStub().sayHelloClientStream(streamRecorder);
    requestStream.onNext(REQUEST);
    requestStream.onNext(REQUEST);
    requestStream.onNext(REQUEST);

    // Not a blocking stub, so we need to wait.
    streamRecorder.awaitCompletion();

    assertThat(findRecordedMetricOrThrow("started").samples).hasSize(2);
    assertThat(findRecordedMetricNamesOrThrow("started")).contains("started_total");
    assertThat(findRecordedMetricNamesOrThrow("started")).contains("started_created");

    assertThat(findRecordedMetricOrThrow("msg_received").samples).hasSize(2);
    assertThat(findRecordedMetricNamesOrThrow("msg_received")).contains("msg_received_total");
    assertThat(findRecordedMetricNamesOrThrow("msg_received")).contains("msg_received_created");

    assertThat(findRecordedMetricOrThrow("msg_sent").samples).isEmpty();

    FamilySamples handled = findRecordedMetricOrThrow("handled");
    assertThat(handled.samples).hasSize(2);
    FakeSample totalSample = getSample(handled, "handled_total");
    assertThat(totalSample.labelValues)
        .containsExactly(
            "CLIENT_STREAMING",
            HelloServiceImpl.SERVICE_NAME,
            HelloServiceImpl.CLIENT_STREAM_METHOD_NAME,
            "OK", // TODO: These are the "code" and "grpc_code" labels which are currently
            // duplicated. "code" should be deprecated in a future release.
            "OK");
    assertThat(totalSample.value).isWithin(0).of(1);
  }

  @Test
  public void serverStreamRpcMetrics() throws Throwable {
    startGrpcServer(CHEAP_METRICS);
    ImmutableList<HelloResponse> responses =
        ImmutableList.copyOf(createGrpcBlockingStub().sayHelloServerStream(REQUEST));

    assertThat(findRecordedMetricOrThrow("started").samples).hasSize(2);
    assertThat(findRecordedMetricNamesOrThrow("started")).contains("started_total");
    assertThat(findRecordedMetricNamesOrThrow("started")).contains("started_created");

    assertThat(findRecordedMetricOrThrow("msg_received").samples).isEmpty();

    assertThat(findRecordedMetricOrThrow("msg_sent").samples).hasSize(2);
    assertThat(findRecordedMetricNamesOrThrow("msg_sent")).contains("msg_sent_total");
    assertThat(findRecordedMetricNamesOrThrow("msg_sent")).contains("msg_sent_created");

    FamilySamples handled = findRecordedMetricOrThrow("handled");
    assertThat(handled.samples).hasSize(2);
    FakeSample totalSample = getSample(handled, "handled_total");
    assertThat(totalSample.labelValues)
        .containsExactly(
            "SERVER_STREAMING",
            HelloServiceImpl.SERVICE_NAME,
            HelloServiceImpl.SERVER_STREAM_METHOD_NAME,
            "OK", // TODO: These are the "code" and "grpc_code" labels which are currently
            // duplicated. "code" should be deprecated in a future release.
            "OK");
    assertThat(totalSample.value).isWithin(0).of(1);

    FamilySamples messagesSent = findRecordedMetricOrThrow("msg_sent");
    totalSample = getSample(messagesSent, "msg_sent_total");
    assertThat(totalSample.labelValues)
        .containsExactly(
            "SERVER_STREAMING",
            HelloServiceImpl.SERVICE_NAME,
            HelloServiceImpl.SERVER_STREAM_METHOD_NAME);
    assertThat(totalSample.value).isWithin(0).of(responses.size());
  }

  @Test
  public void bidiStreamRpcMetrics() throws Throwable {
    startGrpcServer(CHEAP_METRICS);
    StreamRecorder<HelloResponse> streamRecorder = StreamRecorder.create();
    StreamObserver<HelloRequest> requestStream =
        createGrpcStub().sayHelloBidiStream(streamRecorder);
    requestStream.onNext(REQUEST);
    requestStream.onNext(REQUEST);
    requestStream.onCompleted();

    // Not a blocking stub, so we need to wait.
    streamRecorder.awaitCompletion();

    assertThat(findRecordedMetricOrThrow("started").samples).hasSize(2);
    assertThat(findRecordedMetricNamesOrThrow("started")).contains("started_total");
    assertThat(findRecordedMetricNamesOrThrow("started")).contains("started_created");

    assertThat(findRecordedMetricOrThrow("msg_received").samples).hasSize(2);
    assertThat(findRecordedMetricNamesOrThrow("msg_received")).contains("msg_received_total");
    assertThat(findRecordedMetricNamesOrThrow("msg_received")).contains("msg_received_created");

    assertThat(findRecordedMetricOrThrow("msg_sent").samples).hasSize(2);
    assertThat(findRecordedMetricNamesOrThrow("msg_sent")).contains("msg_sent_total");
    assertThat(findRecordedMetricNamesOrThrow("msg_sent")).contains("msg_sent_created");

    FamilySamples handled = findRecordedMetricOrThrow("handled");
    assertThat(handled.samples).hasSize(2);
    FakeSample totalSample = getSample(handled, "handled_total");
    assertThat(totalSample.labelValues)
        .containsExactly(
            "BIDI_STREAMING",
            HelloServiceImpl.SERVICE_NAME,
            HelloServiceImpl.BIDI_STREAM_METHOD_NAME,
            "OK", // TODO: These are the "code" and "grpc_code" labels which are currently
            // duplicated. "code" should be deprecated in a future release.
            "OK");
    assertThat(totalSample.value).isWithin(0).of(1);
  }

  @Test
  public void noHistogramIfDisabled() throws Throwable {
    startGrpcServer(CHEAP_METRICS);
    createGrpcBlockingStub().sayHello(REQUEST);
    assertThat(
            PrometheusRegistryHelper.findRecordedMetric(
                    "handled_latency_seconds", prometheusRegistry)
                .isPresent())
        .isFalse();
  }

  @Test
  public void addsHistogramIfEnabled() throws Throwable {
    startGrpcServer(ALL_METRICS);
    createGrpcBlockingStub().sayHello(REQUEST);

    FamilySamples latency = findRecordedMetricOrThrow("handled_latency_seconds");
    assertThat(latency.samples.size()).isGreaterThan(0);
  }

  @Test
  public void overridesHistogramBuckets() throws Throwable {
    double[] buckets = new double[] {0.1, 0.2, 0.8};
    startGrpcServer(ALL_METRICS.withLatencyBuckets(buckets));
    createGrpcBlockingStub().sayHello(REQUEST);

    long expectedNum = buckets.length + 1; // Our two buckets and the Inf buckets.
    assertThat(countSamples("handled_latency_seconds", "handled_latency_seconds_bucket"))
        .isEqualTo(expectedNum);

    FakeSample sample =
        getSample(
            findRecordedMetricOrThrow("handled_latency_seconds"), "handled_latency_seconds_bucket");

    assertThat(sample.labelNames).containsExactly("grpc_type", "grpc_service", "grpc_method", "le");
  }

  @Test
  public void addsStatusCodeLabel() throws Throwable {
    double[] buckets = new double[] {8.0, 9.0, 10.0};
    startGrpcServer(ALL_METRICS.withCodeLabelInLatencyHistogram().withLatencyBuckets(buckets));
    createGrpcBlockingStub().sayHello(REQUEST);

    FakeSample sample =
        getSample(
            findRecordedMetricOrThrow("handled_latency_seconds"), "handled_latency_seconds_bucket");

    assertThat(sample.labelNames)
        .containsExactly("grpc_type", "grpc_service", "grpc_method", "grpc_code", "le");
    assertThat(sample.labelValues)
        .containsExactly(
            "UNARY",
            HelloServiceImpl.SERVICE_NAME,
            HelloServiceImpl.UNARY_METHOD_NAME,
            "OK",
            "8.0");
  }

  @Test
  public void recordsMultipleCalls() throws Throwable {
    startGrpcServer(CHEAP_METRICS);

    createGrpcBlockingStub().sayHello(REQUEST);
    createGrpcBlockingStub().sayHello(REQUEST);
    createGrpcBlockingStub().sayHello(REQUEST);

    StreamRecorder<HelloResponse> streamRecorder = StreamRecorder.create();
    StreamObserver<HelloRequest> requestStream =
        createGrpcStub().sayHelloBidiStream(streamRecorder);
    requestStream.onNext(REQUEST);
    requestStream.onNext(REQUEST);
    requestStream.onCompleted();
    streamRecorder.awaitCompletion();

    assertThat(findRecordedMetricOrThrow("started").samples).hasSize(4);
    assertThat(findRecordedMetricOrThrow("handled").samples).hasSize(4);
  }

  @Test
  public void recordsHeadersAsLabels() throws Throwable {
    startGrpcServer(ALL_METRICS.withLabelHeaders(Arrays.asList("header-1", "header-2")));
    Metadata metadata = new Metadata();
    metadata.put(Metadata.Key.of("header-1", Metadata.ASCII_STRING_MARSHALLER), "value1");
    metadata.put(Metadata.Key.of("header-2", Metadata.ASCII_STRING_MARSHALLER), "value2");
    StreamRecorder<HelloResponse> streamRecorder = StreamRecorder.create();
    StreamObserver<HelloRequest> requestStream =
        createGrpcStub()
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .sayHelloBidiStream(streamRecorder);
    requestStream.onNext(REQUEST);
    requestStream.onNext(REQUEST);
    requestStream.onCompleted();

    // Not a blocking stub, so we need to wait.
    streamRecorder.awaitCompletion();

    assertThat(findRecordedMetricOrThrow("started").samples).hasSize(2);
    assertThat(findRecordedMetricNamesOrThrow("started")).contains("started_total");
    assertThat(findRecordedMetricNamesOrThrow("started")).contains("started_created");
    FakeSample sample = getSample(findRecordedMetricOrThrow("started"), "started_total");
    assertThat(sample.labelNames)
        .containsExactly("grpc_type", "grpc_service", "grpc_method", "header_1", "header_2");
    assertThat(sample.labelValues)
        .containsExactly(
            "BIDI_STREAMING",
            HelloServiceImpl.SERVICE_NAME,
            HelloServiceImpl.BIDI_STREAM_METHOD_NAME,
            "value1",
            "value2");

    assertThat(findRecordedMetricOrThrow("msg_received").samples).hasSize(2);
    assertThat(findRecordedMetricNamesOrThrow("msg_received")).contains("msg_received_total");
    assertThat(findRecordedMetricNamesOrThrow("msg_received")).contains("msg_received_created");
    sample = getSample(findRecordedMetricOrThrow("msg_received"), "msg_received_total");
    assertThat(sample.labelNames)
        .containsExactly("grpc_type", "grpc_service", "grpc_method", "header_1", "header_2");
    assertThat(sample.labelValues)
        .containsExactly(
            "BIDI_STREAMING",
            HelloServiceImpl.SERVICE_NAME,
            HelloServiceImpl.BIDI_STREAM_METHOD_NAME,
            "value1",
            "value2");

    assertThat(findRecordedMetricOrThrow("msg_sent").samples).hasSize(2);
    assertThat(findRecordedMetricNamesOrThrow("msg_sent")).contains("msg_sent_total");
    assertThat(findRecordedMetricNamesOrThrow("msg_sent")).contains("msg_sent_created");
    sample = getSample(findRecordedMetricOrThrow("msg_sent"), "msg_sent_total");
    assertThat(sample.labelNames)
        .containsExactly("grpc_type", "grpc_service", "grpc_method", "header_1", "header_2");
    assertThat(sample.labelValues)
        .containsExactly(
            "BIDI_STREAMING",
            HelloServiceImpl.SERVICE_NAME,
            HelloServiceImpl.BIDI_STREAM_METHOD_NAME,
            "value1",
            "value2");

    FamilySamples handled = findRecordedMetricOrThrow("handled");
    assertThat(handled.samples).hasSize(2);
    sample = getSample(handled, "handled_total");
    assertThat(sample.labelNames)
        .containsExactly(
            "grpc_type",
            "grpc_service",
            "grpc_method",
            "code",
            "grpc_code",
            "header_1",
            "header_2");
    assertThat(sample.labelValues)
        .containsExactly(
            "BIDI_STREAMING",
            HelloServiceImpl.SERVICE_NAME,
            HelloServiceImpl.BIDI_STREAM_METHOD_NAME,
            "OK", // TODO: These are the "code" and "grpc_code" labels which are currently
            // duplicated. "code" should be deprecated in a future release.
            "OK",
            "value1",
            "value2");
    assertThat(sample.value).isWithin(0).of(1);
  }

  private void startGrpcServer(Configuration monitoringConfig) {
    MonitoringServerInterceptor interceptor =
        MonitoringServerInterceptor.create(new Provider(monitoringConfig, prometheusRegistry));
    grpcServer =
        InProcessServerBuilder.forName(grpcServerName)
            .addService(
                ServerInterceptors.intercept(new HelloServiceImpl().bindService(), interceptor))
            .build();
    try {
      grpcServer.start();
    } catch (IOException e) {
      throw new RuntimeException("Exception while running grpc server", e);
    }
  }

  private FamilySamples findRecordedMetricOrThrow(String name) {
    return PrometheusRegistryHelper.findRecordedMetricOrThrow(name, prometheusRegistry);
  }

  private List<String> findRecordedMetricNamesOrThrow(String name) {
    return PrometheusRegistryHelper.findRecordedMetricNamesOrThrow(name, prometheusRegistry);
  }

  private HelloServiceBlockingStub createGrpcBlockingStub() {
    return HelloServiceGrpc.newBlockingStub(createGrpcChannel());
  }

  private int countSamples(String metricName, String sampleName) {
    return PrometheusRegistryHelper.countSamples(metricName, sampleName, prometheusRegistry);
  }

  private HelloServiceStub createGrpcStub() {
    return HelloServiceGrpc.newStub(createGrpcChannel());
  }

  private Channel createGrpcChannel() {
    return InProcessChannelBuilder.forName(grpcServerName).usePlaintext().build();
  }

  private static FakeSample getSample(FamilySamples family, String sampleName) {
    return family.samples.stream().filter(s -> s.name.equals(sampleName)).findFirst().get();
  }
}
