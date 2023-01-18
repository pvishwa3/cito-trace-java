package datadog.telemetry.iast;

import datadog.telemetry.TelemetryRunnable;
import datadog.telemetry.TelemetryService;
import datadog.telemetry.api.Metric;
import datadog.telemetry.api.Metric.TypeEnum;
import datadog.trace.api.iast.telemetry.IastMetrics;
import datadog.trace.api.iast.telemetry.IastTelemetryCollector;
import datadog.trace.api.iast.telemetry.IastTelemetryCollector.Point;
import datadog.trace.api.iast.telemetry.IastTelemetryCollector.SingleMetric;
import datadog.trace.api.iast.telemetry.IastTelemetryCollector.TaggedMetric;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class IastTelemetryPeriodicAction implements TelemetryRunnable.TelemetryPeriodicAction {

  /** TODO rename to "iast" once telemetry supports it */
  private static final String NAMESPACE = "appsec";

  @Override
  public void doIteration(@Nonnull final TelemetryService service) {
    for (final IastTelemetryCollector.Metric item : IastMetrics.values()) {
      if (item instanceof TaggedMetric) {
        final TaggedMetric tagged = (TaggedMetric) item;
        final Map<String, List<Point>> metrics = tagged.drain();
        for (Map.Entry<String, List<Point>> entry : metrics.entrySet()) {
          final String tag = entry.getKey();
          final List<Point> points = entry.getValue();
          if (!points.isEmpty()) {
            service.addMetric(
                asTelemetryMetric(item)
                    .points(asTelemetryPoints(entry.getValue()))
                    .addTagsItem(tag));
          }
        }
      } else {
        final List<Point> points = ((SingleMetric) item).drain();
        if (!points.isEmpty()) {
          service.addMetric(asTelemetryMetric(item).points(asTelemetryPoints(points)));
        }
      }
    }
  }

  private Metric asTelemetryMetric(final IastTelemetryCollector.Metric metric) {
    return new Metric()
        .namespace(NAMESPACE)
        .metric(metric.getName())
        .type(TypeEnum.valueOf(metric.getType()))
        .common(metric.isCommon());
  }

  private List<List<Number>> asTelemetryPoints(final List<Point> points) {
    return points.stream()
        .map(point -> Arrays.<Number>asList(point.getTimestamp(), point.getValue()))
        .collect(Collectors.toList());
  }
}
