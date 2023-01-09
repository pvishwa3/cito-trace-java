package datadog.trace.instrumentation.jdbc;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQLCommenter {
  private String serviceVersion;
  private String serviceName;
  private String dbInstance;
  private String traceId;
  private String spanId;
  private Integer samplingPriority;
  private String serviceEnv;

  // This encodes version 1.
  public static final String W3C_CONTEXT_VERSION = "00";
  private static final String UTF8 = StandardCharsets.UTF_8.toString();
  private static final Logger log = LoggerFactory.getLogger(SQLCommenter.class);

  public SQLCommenter() {}

  public static Builder newBuilder() {
    return new Builder();
  }

  public static Builder newBuilder(SQLCommenter copy) {
    return copy == null
        ? new Builder()
        : (new Builder())
            .withServiceVersion(copy.serviceVersion)
            .withServiceName(copy.serviceName)
            .withTraceContext(copy.traceId, copy.spanId, copy.samplingPriority)
            .withServiceEnv(copy.serviceEnv)
            .withDbInstance(copy.dbInstance);
  }

  // TODO: make this more efficient
  @Nullable
  private String traceParent() {
    // A sample:
    //    traceparent='00-a22901f654b534675439f71fbe43783d-7fde95452aa72253-01'
    return this.traceId == null
        ? null
        : String.format(
            "%s-%s-%s-%02X", W3C_CONTEXT_VERSION, this.traceId, this.spanId, this.samplingPriority);
  }

  public String augmentSQLStatement(final String sqlStmt) {
    if (sqlStmt == null || sqlStmt.isEmpty()) {
      return sqlStmt;
    }

    // If the SQL already has a comment, just return it.
    if (hasSQLComment(sqlStmt)) {
      return sqlStmt;
    }
    String commentStr = toString();
    if (commentStr.isEmpty()) {
      return sqlStmt;
    }
    // Otherwise, now insert the fields and format.
    return String.format("%s /*%s*/", sqlStmt, commentStr);
  }

  // TODO: copy pasta from open telemetry.. should be more efficient
  public String toString() {
    SortedMap<String, Object> skvp = this.sortedKeyValuePairs();
    ArrayList<String> keyValuePairsList = new ArrayList();
    Iterator var3 = skvp.entrySet().iterator();

    while (var3.hasNext()) {
      Map.Entry<String, Object> entry = (Map.Entry) var3.next();
      Object value = entry.getValue();
      if (this.isBlank(value)) {
        try {
          String valueStr = String.format("%s", value);
          String keyValuePairString =
              String.format("%s='%s'", urlEncode(entry.getKey()), urlEncode(valueStr));
          keyValuePairsList.add(keyValuePairString);
        } catch (Exception var8) {
          //          logger.log(Level.WARNING, "Exception when encoding State", var8);
          // TODO: close the state here or just keep going?
        }
      }
    }

    return String.join(",", keyValuePairsList);
  }

  private static String urlEncode(String s) throws Exception {
    return URLEncoder.encode(s, UTF8);
  }

  private boolean isBlank(Object obj) {
    if (obj == null) {
      return true;
    }
    if (obj instanceof String) {
      return obj == "";
    }
    if (obj instanceof Number) {
      Number number = (Number) obj;
      return number.doubleValue() == 0.0;
    }
    return false;
  }

  private boolean hasSQLComment(String stmt) {
    return stmt != null && !stmt.isEmpty() && (stmt.contains("--") || stmt.contains("/*"));
  }

  private SortedMap<String, Object> sortedKeyValuePairs() {
    SortedMap<String, Object> sortedMap = new TreeMap<>();
    sortedMap.put("ddps", this.serviceName);
    sortedMap.put("dddbs", this.dbInstance);
    sortedMap.put("dde", this.serviceEnv);
    sortedMap.put("ddpv", this.serviceVersion);
    sortedMap.put("traceparent", this.traceParent());
    return sortedMap;
  }

  public static class Builder {
    private String serviceVersion;
    private String serviceName;
    private String dbInstance;
    private String serviceEnv;
    private String traceID;
    private String spanID;
    private Integer samplingPriority;

    private Builder() {}

    public Builder withServiceVersion(final String version) {
      this.serviceVersion = version;
      return this;
    }

    public Builder withServiceName(final String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder withDbInstance(final String dbInstance) {
      this.dbInstance = dbInstance;
      return this;
    }

    public Builder withTraceContext(
        final String traceID, final String spanID, final Integer samplingPriority) {
      this.traceID = traceID;
      this.spanID = spanID;
      this.samplingPriority = samplingPriority;
      return this;
    }

    public Builder withServiceEnv(final String env) {
      this.serviceEnv = env;
      return this;
    }

    public SQLCommenter build() {
      SQLCommenter commenter = new SQLCommenter();
      commenter.serviceVersion = this.serviceVersion;
      commenter.serviceName = this.serviceName;
      commenter.dbInstance = this.dbInstance;
      commenter.serviceEnv = this.serviceEnv;
      commenter.traceId = this.traceID;
      commenter.spanId = this.spanID;
      commenter.samplingPriority = this.samplingPriority;
      return commenter;
    }
  }
}
