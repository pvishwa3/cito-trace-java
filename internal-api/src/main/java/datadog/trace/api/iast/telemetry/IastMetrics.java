package datadog.trace.api.iast.telemetry;

import datadog.trace.api.iast.telemetry.IastTelemetryCollector.Metric;
import datadog.trace.api.iast.telemetry.IastTelemetryCollector.Point;
import datadog.trace.api.iast.telemetry.IastTelemetryCollector.SingleMetric;
import datadog.trace.api.iast.telemetry.IastTelemetryCollector.TaggedMetric;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class IastMetrics {

  public static final SingleMetric INSTRUMENTED_PROPAGATION =
      new SingleMetricImpl("instrumented.propagation", true, new ConflatedMetric());
  public static final TaggedMetric INSTRUMENTED_SOURCE =
      new TaggedMetricImpl("instrumented.source", true, Tag.SOURCE_TYPE, ConflatedMetric::new);
  public static final TaggedMetric INSTRUMENTED_SINK =
      new TaggedMetricImpl("instrumented.sink", true, Tag.VULNERABILITY_TYPE, ConflatedMetric::new);
  public static final SingleMetric INSTRUMENTATION_TIME =
      new SingleMetricImpl("instrumentation.time", true, new ConflatedMetric());

  public static final SingleMetric EXECUTED_PROPAGATION =
      new SingleMetricImpl("executed.propagation", true, new ConflatedMetric());
  public static final TaggedMetric EXECUTED_SOURCE =
      new TaggedMetricImpl("executed.source", true, Tag.SOURCE_TYPE, ConflatedMetric::new);
  public static final TaggedMetric EXECUTED_SINK =
      new TaggedMetricImpl("executed.sink", true, Tag.VULNERABILITY_TYPE, ConflatedMetric::new);
  public static final SingleMetric EXECUTED_TAINTED =
      new SingleMetricImpl("executed.tainted", true, new ConflatedMetric());
  public static final SingleMetric EXECUTION_TIME =
      new SingleMetricImpl("execution.time", true, new ConflatedMetric());

  public static final SingleMetric REQUEST_TAINTED =
      new SingleMetricImpl("request.tainted", true, new AggregatedMetric());
  public static final SingleMetric TAINTED_FLAT_MODE =
      new SingleMetricImpl("tainted.flat.mode", false, new ConflatedMetric());

  private static final Map<String, Metric> METRICS =
      Stream.of(
              INSTRUMENTED_PROPAGATION,
              INSTRUMENTED_SOURCE,
              INSTRUMENTED_SINK,
              INSTRUMENTATION_TIME,
              EXECUTED_PROPAGATION,
              EXECUTED_SOURCE,
              EXECUTED_SINK,
              EXECUTED_TAINTED,
              EXECUTION_TIME,
              REQUEST_TAINTED,
              TAINTED_FLAT_MODE)
          .collect(Collectors.toMap(Metric::getName, Function.identity()));

  public static Collection<Metric> values() {
    return METRICS.values();
  }

  public static Metric valueOf(final String metric) {
    return METRICS.get(metric);
  }

  private IastMetrics() {}

  public enum Tag {
    VULNERABILITY_TYPE("vulnerability_type"),
    SOURCE_TYPE("source_type");

    private final String name;

    Tag(final String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public String getTagValue(final String value) {
      return String.format("%s:%s", name, value);
    }
  }

  private interface MetricHandler {
    void add(long value);

    List<Point> drain();
  }

  private static class ConflatedMetric implements MetricHandler {

    private final AtomicLong value = new AtomicLong(0);

    @Override
    public void add(final long value) {
      this.value.addAndGet(value);
    }

    @Override
    public List<Point> drain() {
      final long current = value.getAndSet(0);
      return current == 0 ? Collections.emptyList() : Collections.singletonList(new Point(current));
    }
  }

  private static class AggregatedMetric implements MetricHandler {

    private static final int DEFAULT_MAX_QUEUE_SIZE = 1000;

    private final Queue<Point> queue;
    private final AtomicInteger available;
    private final int maxQueueSize;

    public AggregatedMetric() {
      this(DEFAULT_MAX_QUEUE_SIZE);
    }

    public AggregatedMetric(final int maxQueueSize) {
      this.maxQueueSize = maxQueueSize;
      queue = new ConcurrentLinkedQueue<>();
      available = new AtomicInteger(maxQueueSize);
    }

    @Override
    public void add(final long value) {
      if (available.getAndUpdate(x -> x > 0 ? x - 1 : x) > 0) {
        this.queue.add(new Point(value));
      }
    }

    @Override
    public List<Point> drain() {
      final List<Point> current = new ArrayList<>(queue);
      queue.clear();
      available.set(maxQueueSize);
      return current;
    }
  }

  private abstract static class MetricImpl implements Metric {
    private final String name;
    private final boolean common;

    MetricImpl(final String name, final boolean common) {
      this.name = name;
      this.common = common;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getType() {
      return "COUNT";
    }

    @Override
    public boolean isCommon() {
      return common;
    }

    @Override
    public String toString() {
      return "MetricImpl{" + "name='" + name + '\'' + '}';
    }
  }

  private static class SingleMetricImpl extends MetricImpl implements SingleMetric {

    private final MetricHandler handler;

    SingleMetricImpl(final String name, final boolean common, final MetricHandler handler) {
      super(name, common);
      this.handler = handler;
    }

    @Override
    public void add(final long value) {
      handler.add(value);
    }

    @Override
    public List<Point> drain() {
      return handler.drain();
    }
  }

  private static class TaggedMetricImpl extends MetricImpl implements TaggedMetric {

    private final Tag tag;
    private final Supplier<MetricHandler> supplier;
    /**
     * This map is bounded by the cardinality of the metric tags, so far we have: {@link
     * datadog.trace.api.iast.model.VulnerabilityTypes} and {@link
     * datadog.trace.api.iast.model.SourceTypes}
     */
    private final Map<String, MetricHandler> handlers = new ConcurrentHashMap<>();

    TaggedMetricImpl(
        final String name,
        final boolean common,
        final Tag tag,
        final Supplier<MetricHandler> supplier) {
      super(name, common);
      this.tag = tag;
      this.supplier = supplier;
    }

    @Override
    public String getTagName() {
      return tag.getName();
    }

    @Override
    public void add(final long value, final String tag) {
      MetricHandler handler = handlers.get(tag);
      if (handler == null) {
        handler = supplier.get();
        handlers.put(tag, handler); // acceptable loss here
      }
      handler.add(value);
    }

    @Override
    public Map<String, List<Point>> drain() {
      final Map<String, List<Point>> result = new HashMap<>(handlers.size());
      for (Map.Entry<String, MetricHandler> entry : handlers.entrySet()) {
        final String tagValue = tag.getTagValue(entry.getKey());
        result.put(tagValue, entry.getValue().drain());
      }
      return result;
    }
  }
}
