package dev.tayvs.grpc.prometheus.testing;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.ClassicHistogramBuckets;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.HistogramSnapshot;
import io.prometheus.metrics.model.snapshots.Label;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Testing utilities for {@link PrometheusRegistry} (prometheus-metrics 1.x API).
 * Provides the same logical API as {@link RegistryHelper} via compatibility wrappers.
 */
public class PrometheusRegistryHelper {

    public static final class FamilySamples {
        public final List<FakeSample> samples;

        FamilySamples(List<FakeSample> samples) {
            this.samples = samples;
        }
    }

    public static final class FakeSample {
        public final String name;
        public final List<String> labelNames;
        public final List<String> labelValues;
        public final double value;

        FakeSample(String name, List<String> labelNames, List<String> labelValues, double value) {
            this.name = name;
            this.labelNames = labelNames;
            this.labelValues = labelValues;
            this.value = value;
        }
    }

    public static Optional<FamilySamples> findRecordedMetric(String name, PrometheusRegistry registry) {
        MetricSnapshots snapshots = registry.scrape();
        for (MetricSnapshot snapshot : snapshots) {
            if (snapshot.getMetadata().getName().equals(name)) {
                return Optional.of(toFamilySamples(name, snapshot));
            }
        }
        return Optional.empty();
    }

    public static FamilySamples findRecordedMetricOrThrow(String name, PrometheusRegistry registry) {
        return findRecordedMetric(name, registry)
                .orElseThrow(() -> new IllegalArgumentException("Could not find metric with name: " + name));
    }

    public static List<String> findRecordedMetricNamesOrThrow(String name, PrometheusRegistry registry) {
        return findRecordedMetricOrThrow(name, registry).samples.stream()
                .map(s -> s.name)
                .collect(Collectors.toList());
    }

    public static int countSamples(String metricName, String sampleName, PrometheusRegistry registry) {
        FamilySamples family = findRecordedMetricOrThrow(metricName, registry);
        int count = 0;
        for (FakeSample s : family.samples) {
            if (s.name.equals(sampleName)) {
                count++;
            }
        }
        return count;
    }

    private static FamilySamples toFamilySamples(String baseName, MetricSnapshot snapshot) {
        List<FakeSample> samples = new ArrayList<>();
        if (snapshot instanceof CounterSnapshot) {
            for (CounterSnapshot.CounterDataPointSnapshot dp : ((CounterSnapshot) snapshot).getDataPoints()) {
                List<String> names = toLabelNames(dp.getLabels());
                List<String> values = toLabelValues(dp.getLabels());
                samples.add(new FakeSample(baseName + "_total", names, values, dp.getValue()));
                samples.add(new FakeSample(baseName + "_created", names, values,
                        dp.getCreatedTimestampMillis() / 1000.0));
            }
        } else if (snapshot instanceof HistogramSnapshot) {
            for (HistogramSnapshot.HistogramDataPointSnapshot dp : ((HistogramSnapshot) snapshot).getDataPoints()) {
                List<String> names = toLabelNames(dp.getLabels());
                List<String> values = toLabelValues(dp.getLabels());
                if (dp.hasClassicHistogramData()) {
                    ClassicHistogramBuckets buckets = dp.getClassicBuckets();
                    for (int i = 0; i < buckets.size(); i++) {
                        List<String> bucketNames = new ArrayList<>(names);
                        bucketNames.add("le");
                        List<String> bucketValues = new ArrayList<>(values);
                        double ub = buckets.getUpperBound(i);
                        bucketValues.add(ub == Double.POSITIVE_INFINITY ? "+Inf" : String.valueOf(ub));
                        samples.add(new FakeSample(baseName + "_bucket", bucketNames, bucketValues,
                                (double) buckets.getCount(i)));
                    }
                }
                samples.add(new FakeSample(baseName + "_sum", names, values, dp.getSum()));
                samples.add(new FakeSample(baseName + "_count", names, values, (double) dp.getCount()));
                samples.add(new FakeSample(baseName + "_created", names, values,
                        dp.getCreatedTimestampMillis() / 1000.0));
            }
        }
        return new FamilySamples(samples);
    }

    private static List<String> toLabelNames(Labels labels) {
        List<String> result = new ArrayList<>();
        for (Label label : labels) {
            result.add(label.getName());
        }
        return result;
    }

    private static List<String> toLabelValues(Labels labels) {
        List<String> result = new ArrayList<>();
        for (Label label : labels) {
            result.add(label.getValue());
        }
        return result;
    }
}
