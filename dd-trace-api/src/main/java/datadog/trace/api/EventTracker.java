package datadog.trace.api;

import datadog.trace.api.DDTags;
import datadog.trace.api.internal.InternalTracer;
import datadog.trace.api.internal.TraceSegment;

import java.util.Map;

public class EventTracker {

    private final InternalTracer tracer;

    EventTracker(InternalTracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Method allowing to track whether a user login was successful or not. A user login event
     * is made of a user id, along and an optional key-value map of metadata of string types only.
     *
     * @param userId    user id used for login
     * @param success   flag indicates login success
     * @param metadata  custom metadata data represented as key/value map
     */
    public void trackLoginEvent(String userId, boolean success, Map<String, String> metadata) {
        if (tracer == null) {
            return;
        }

        TraceSegment segment = this.tracer.getTraceSegment();
        if (segment == null) {
            return;
        }

        if (success) {
            segment.setTagTop("appsec.events.users.login.success.track", true);
            segment.setTagTop("usr.id", userId);
            metadata.forEach((k, v) -> {
                segment.setTagTop("appsec.events.users.login.success."+k, v);
            });
        } else {
            segment.setTagTop("appsec.events.users.login.failure.track", true);
            segment.setTagTop("appsec.events.users.login.failure.usr.id", userId);
            metadata.forEach((k, v) -> {
                segment.setTagTop("appsec.events.users.login.failure."+k, v);
            });
        }
        segment.setTagTop(DDTags.MANUAL_KEEP, true);
    }

    /**
     * Method allowing to track custom events. A custom event is made of an event name along with
     * an optional key-value map of metadata of string types only
     *
     * @param eventName     name of the custom event
     * @param metadata      custom metadata data represented as key/value map
     */
    public void trackCustomEvent(String eventName, Map<String, String> metadata) {
        if (tracer == null) {
            return;
        }

        TraceSegment segment = this.tracer.getTraceSegment();
        if (segment == null) {
            return;
        }

        segment.setTagTop("appsec.events." + eventName + ".track", true);
        metadata.forEach((k, v) -> {
            segment.setTagTop("appsec.events." + eventName + "."+k, v);
        });
        segment.setTagTop(DDTags.MANUAL_KEEP, true);
    }
}
