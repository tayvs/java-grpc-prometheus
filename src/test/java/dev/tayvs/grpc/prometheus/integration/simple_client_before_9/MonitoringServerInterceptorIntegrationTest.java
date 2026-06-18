// Copyright 2016 Dino Wernli. All Rights Reserved. See LICENSE for licensing terms.

package dev.tayvs.grpc.prometheus.integration.simple_client_before_9;

import com.github.dinowernli.proto.grpc.prometheus.HelloProto.HelloRequest;
import com.github.dinowernli.proto.grpc.prometheus.HelloProto.HelloResponse;
import com.github.dinowernli.proto.grpc.prometheus.HelloServiceGrpc;
import com.github.dinowernli.proto.grpc.prometheus.HelloServiceGrpc.HelloServiceBlockingStub;
import com.github.dinowernli.proto.grpc.prometheus.HelloServiceGrpc.HelloServiceStub;
import com.google.common.collect.ImmutableList;
import dev.tayvs.grpc.prometheus.Configuration;
import dev.tayvs.grpc.prometheus.MonitoringServerInterceptor;
import dev.tayvs.grpc.prometheus.Provider;
import dev.tayvs.grpc.prometheus.testing.HelloServiceImpl;
import dev.tayvs.grpc.prometheus.testing.RegistryHelper;
import io.grpc.Channel;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.StreamRecorder;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.CollectorRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Integrations tests for the simple_client_after_10 module (simpleclient 0.9.0). In simpleclient
 * 0.9.0: counters have no _total suffix and no _created samples. Each counter family has one sample
 * per label combination.
 */
public class MonitoringServerInterceptorIntegrationTest {
  private static final String grpcServerName = "grpc-server";
  private static final String RECIPIENT = "Dave";
  private static final HelloRequest REQUEST =
      HelloRequest.newBuilder().setRecipient(RECIPIENT).build();

  private static final Configuration CHEAP_METRICS = Configuration.cheapMetricsOnly();
  private static final Configuration ALL_METRICS = Configuration.allMetrics();

  private CollectorRegistry collectorRegistry;
  private Server grpcServer;

  @Before
  public void setUp() {
    collectorRegistry = new CollectorRegistry();
  }

  @After
  public void tearDown() throws Exception {
    grpcServer.shutdown().awaitTermination();
  }

  @Test
  public void unaryRpcMetrics() throws Throwable {
    startGrpcServer(CHEAP_METRICS);
    createGrpcBlockingStub().sayHello(REQUEST);

    assertThat(findRecordedMetricOrThrow("grpc_server_started").samples).hasSize(1);
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_started"))
        .contains("grpc_server_started");

    assertThat(findRecordedMetricOrThrow("grpc_server_msg_received").samples).isEmpty();
    assertThat(findRecordedMetricOrThrow("grpc_server_msg_sent").samples).isEmpty();

    MetricFamilySamples handled = findRecordedMetricOrThrow("grpc_server_handled");
    assertThat(handled.samples).hasSize(1);
    Sample totalSample = getSample(handled, "grpc_server_handled");
    assertThat(totalSample.labelValues)
        .containsExactly(
            "UNARY",
            HelloServiceImpl.SERVICE_NAME,
            HelloServiceImpl.UNARY_METHOD_NAME,
            "OK",
            "OK"); // TODO: "code" and "grpc_code" are duplicated; "code" should be deprecated.
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

    assertThat(findRecordedMetricOrThrow("grpc_server_started").samples).hasSize(1);
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_started"))
        .contains("grpc_server_started");

    assertThat(findRecordedMetricOrThrow("grpc_server_msg_received").samples).hasSize(1);
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_msg_received"))
        .contains("grpc_server_msg_received");

    assertThat(findRecordedMetricOrThrow("grpc_server_msg_sent").samples).isEmpty();

    MetricFamilySamples handled = findRecordedMetricOrThrow("grpc_server_handled");
    assertThat(handled.samples).hasSize(1);
    Sample totalSample = getSample(handled, "grpc_server_handled");
    assertThat(totalSample.labelValues)
        .containsExactly(
            "CLIENT_STREAMING",
            HelloServiceImpl.SERVICE_NAME,
            HelloServiceImpl.CLIENT_STREAM_METHOD_NAME,
            "OK",
            "OK"); // TODO: "code" and "grpc_code" are duplicated; "code" should be deprecated.
    assertThat(totalSample.value).isWithin(0).of(1);
  }

  @Test
  public void serverStreamRpcMetrics() throws Throwable {
    startGrpcServer(CHEAP_METRICS);
    ImmutableList<HelloResponse> responses =
        ImmutableList.copyOf(createGrpcBlockingStub().sayHelloServerStream(REQUEST));

    assertThat(findRecordedMetricOrThrow("grpc_server_started").samples).hasSize(1);
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_started"))
        .contains("grpc_server_started");

    assertThat(findRecordedMetricOrThrow("grpc_server_msg_received").samples).isEmpty();

    assertThat(findRecordedMetricOrThrow("grpc_server_msg_sent").samples).hasSize(1);
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_msg_sent"))
        .contains("grpc_server_msg_sent");

    MetricFamilySamples handled = findRecordedMetricOrThrow("grpc_server_handled");
    assertThat(handled.samples).hasSize(1);
    Sample totalSample = getSample(handled, "grpc_server_handled");
    assertThat(totalSample.labelValues)
        .containsExactly(
            "SERVER_STREAMING",
            HelloServiceImpl.SERVICE_NAME,
            HelloServiceImpl.SERVER_STREAM_METHOD_NAME,
            "OK",
            "OK"); // TODO: "code" and "grpc_code" are duplicated; "code" should be deprecated.
    assertThat(totalSample.value).isWithin(0).of(1);

    MetricFamilySamples messagesSent = findRecordedMetricOrThrow("grpc_server_msg_sent");
    totalSample = getSample(messagesSent, "grpc_server_msg_sent");
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

    assertThat(findRecordedMetricOrThrow("grpc_server_started").samples).hasSize(1);
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_started"))
        .contains("grpc_server_started");

    assertThat(findRecordedMetricOrThrow("grpc_server_msg_received").samples).hasSize(1);
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_msg_received"))
        .contains("grpc_server_msg_received");

    assertThat(findRecordedMetricOrThrow("grpc_server_msg_sent").samples).hasSize(1);
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_msg_sent"))
        .contains("grpc_server_msg_sent");

    MetricFamilySamples handled = findRecordedMetricOrThrow("grpc_server_handled");
    assertThat(handled.samples).hasSize(1);
    Sample totalSample = getSample(handled, "grpc_server_handled");
    assertThat(totalSample.labelValues)
        .containsExactly(
            "BIDI_STREAMING",
            HelloServiceImpl.SERVICE_NAME,
            HelloServiceImpl.BIDI_STREAM_METHOD_NAME,
            "OK",
            "OK"); // TODO: "code" and "grpc_code" are duplicated; "code" should be deprecated.
    assertThat(totalSample.value).isWithin(0).of(1);
  }

  @Test
  public void noHistogramIfDisabled() throws Throwable {
    startGrpcServer(CHEAP_METRICS);
    createGrpcBlockingStub().sayHello(REQUEST);
    assertThat(
            RegistryHelper.findRecordedMetric(
                    "grpc_server_handled_latency_seconds", collectorRegistry)
                .isPresent())
        .isFalse();
  }

  @Test
  public void addsHistogramIfEnabled() throws Throwable {
    startGrpcServer(ALL_METRICS);
    createGrpcBlockingStub().sayHello(REQUEST);

    MetricFamilySamples latency = findRecordedMetricOrThrow("grpc_server_handled_latency_seconds");
    assertThat(latency.samples.size()).isGreaterThan(0);
  }

  @Test
  public void overridesHistogramBuckets() throws Throwable {
    double[] buckets = new double[] {0.1, 0.2, 0.8};
    startGrpcServer(ALL_METRICS.withLatencyBuckets(buckets));
    createGrpcBlockingStub().sayHello(REQUEST);

    long expectedNum = buckets.length + 1; // Our buckets plus the +Inf bucket.
    assertThat(
            countSamples(
                "grpc_server_handled_latency_seconds",
                "grpc_server_handled_latency_seconds_bucket"))
        .isEqualTo(expectedNum);

    Sample sample =
        getSample(
            findRecordedMetricOrThrow("grpc_server_handled_latency_seconds"),
            "grpc_server_handled_latency_seconds_bucket");

    assertThat(sample.labelNames).containsExactly("grpc_type", "grpc_service", "grpc_method", "le");
  }

  @Test
  public void addsStatusCodeLabel() throws Throwable {
    double[] buckets = new double[] {8.0, 9.0, 10.0};
    startGrpcServer(ALL_METRICS.withCodeLabelInLatencyHistogram().withLatencyBuckets(buckets));
    createGrpcBlockingStub().sayHello(REQUEST);

    Sample sample =
        getSample(
            findRecordedMetricOrThrow("grpc_server_handled_latency_seconds"),
            "grpc_server_handled_latency_seconds_bucket");

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

    // 2 label combinations (UNARY and BIDI_STREAMING), 1 sample each in 0.9.0.
    assertThat(findRecordedMetricOrThrow("grpc_server_started").samples).hasSize(2);
    assertThat(findRecordedMetricOrThrow("grpc_server_handled").samples).hasSize(2);
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

    assertThat(findRecordedMetricOrThrow("grpc_server_started").samples).hasSize(1);
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_started"))
        .contains("grpc_server_started");
    Sample sample =
        getSample(findRecordedMetricOrThrow("grpc_server_started"), "grpc_server_started");
    assertThat(sample.labelNames)
        .containsExactly("grpc_type", "grpc_service", "grpc_method", "header_1", "header_2");
    assertThat(sample.labelValues)
        .containsExactly(
            "BIDI_STREAMING",
            HelloServiceImpl.SERVICE_NAME,
            HelloServiceImpl.BIDI_STREAM_METHOD_NAME,
            "value1",
            "value2");

    assertThat(findRecordedMetricOrThrow("grpc_server_msg_received").samples).hasSize(1);
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_msg_received"))
        .contains("grpc_server_msg_received");
    sample =
        getSample(
            findRecordedMetricOrThrow("grpc_server_msg_received"), "grpc_server_msg_received");
    assertThat(sample.labelNames)
        .containsExactly("grpc_type", "grpc_service", "grpc_method", "header_1", "header_2");
    assertThat(sample.labelValues)
        .containsExactly(
            "BIDI_STREAMING",
            HelloServiceImpl.SERVICE_NAME,
            HelloServiceImpl.BIDI_STREAM_METHOD_NAME,
            "value1",
            "value2");

    assertThat(findRecordedMetricOrThrow("grpc_server_msg_sent").samples).hasSize(1);
    assertThat(findRecordedMetricNamesOrThrow("grpc_server_msg_sent"))
        .contains("grpc_server_msg_sent");
    sample = getSample(findRecordedMetricOrThrow("grpc_server_msg_sent"), "grpc_server_msg_sent");
    assertThat(sample.labelNames)
        .containsExactly("grpc_type", "grpc_service", "grpc_method", "header_1", "header_2");
    assertThat(sample.labelValues)
        .containsExactly(
            "BIDI_STREAMING",
            HelloServiceImpl.SERVICE_NAME,
            HelloServiceImpl.BIDI_STREAM_METHOD_NAME,
            "value1",
            "value2");

    MetricFamilySamples handled = findRecordedMetricOrThrow("grpc_server_handled");
    assertThat(handled.samples).hasSize(1);
    sample = getSample(handled, "grpc_server_handled");
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
            "OK",
            "OK",
            "value1",
            "value2");
    assertThat(sample.value).isWithin(0).of(1);
  }

  private void startGrpcServer(Configuration monitoringConfig) {
    MonitoringServerInterceptor interceptor =
        MonitoringServerInterceptor.create(new Provider(monitoringConfig, collectorRegistry));
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

  private MetricFamilySamples findRecordedMetricOrThrow(String name) {
    return RegistryHelper.findRecordedMetricOrThrow(name, collectorRegistry);
  }

  private List<String> findRecordedMetricNamesOrThrow(String name) {
    return RegistryHelper.findRecordedMetricNamesOrThrow(name, collectorRegistry);
  }

  private HelloServiceBlockingStub createGrpcBlockingStub() {
    return HelloServiceGrpc.newBlockingStub(createGrpcChannel());
  }

  private int countSamples(String metricName, String sampleName) {
    return RegistryHelper.countSamples(metricName, sampleName, collectorRegistry);
  }

  private HelloServiceStub createGrpcStub() {
    return HelloServiceGrpc.newStub(createGrpcChannel());
  }

  private Channel createGrpcChannel() {
    return InProcessChannelBuilder.forName(grpcServerName).usePlaintext().build();
  }

  private static Sample getSample(MetricFamilySamples family, String sampleName) {
    return family.samples.stream().filter(s -> s.name.equals(sampleName)).findFirst().get();
  }
}
