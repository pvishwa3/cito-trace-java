package datadog.trace.api.iast.telemetry;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class IastTelemetryCollector {

  private static final Logger LOG = LoggerFactory.getLogger(IastTelemetryCollector.class);

  private IastTelemetryCollector() {}

  /** Do not delete as this method is used from call sites instrumentation */
  public static void inc(final String metric) {
    inc(metric, null);
  }

  /** Do not delete as this method is used from call sites instrumentation */
  public static void inc(final String metric, final String tag) {
    final Metric iastMetric = IastMetrics.valueOf(metric);
    add(iastMetric, 1, tag);
  }

  public static void add(final Metric metric, final long value, final String tag) {
    try {
      if (metric instanceof TaggedMetric) {
        ((TaggedMetric) metric).add(value, tag);
      } else {
        ((SingleMetric) metric).add(value);
      }
    } catch (final Throwable e) {
      LOG.error("Failed to add metric " + metric, e);
    }
  }

  public interface Metric {
    String getName();

    String getType();

    boolean isCommon();
  }

  public interface SingleMetric extends Metric {
    void add(long value);

    List<Point> drain();
  }

  public interface TaggedMetric extends Metric {
    String getTagName();

    void add(long value, String tag);

    Map<String, List<Point>> drain();
  }

  public static class Point {
    private final long timestamp;
    private final long value;

    public Point(final long value) {
      this(System.currentTimeMillis(), value);
    }

    public Point(final long timestamp, final long value) {
      this.timestamp = timestamp;
      this.value = value;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public long getValue() {
      return value;
    }
  }
}
