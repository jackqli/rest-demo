package com.example.restdemo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class Dags {
  private List<Dag> dags;
}

@Data
class Dag {
  @JsonProperty("dag_id")
  private String dagId;
  @JsonProperty("is_active")
  private Boolean active;
  @JsonProperty("schedule_interval")
  private ScheduleInterval scheduleInterval;
  @JsonProperty("timezone")
  private String timezone;
}
@Data
class ScheduleInterval {
  @JsonProperty("__type")
  private String type;
  private String value;
}
