package com.example.restdemo;

import static com.cronutils.model.CronType.QUARTZ;
import static com.cronutils.model.CronType.UNIX;

import com.cronutils.converter.CalendarToCronTransformer;
import com.cronutils.converter.CronConverter;
import com.cronutils.converter.CronToCalendarTransformer;
import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class DemoApplicationRunner implements ApplicationRunner {

  public static final String EXAMPLE_BASH_OPERATOR = "example_bash_operator";
  private final RestTemplate restTemplate;

  public DemoApplicationRunner(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    Dags dags = restTemplate.getForObject("http://localhost:8080/api/v1/dags", Dags.class);
    Optional<Dag> any = dags.getDags().stream()
        .filter(dag -> dag.getDagId().equals(EXAMPLE_BASH_OPERATOR)).filter(dag -> dag.getActive())
        .findAny();
    if (any.isPresent()) {
      log.info("Cron express is {}", any.get().getScheduleInterval().getValue());
    }

    Dag dag = restTemplate.getForObject(String.format("http://localhost:8080/api/v1/dags/%s/details", EXAMPLE_BASH_OPERATOR), Dag.class);
    if (dag != null) {
      log.info("Timezone of dag is: {}", dag.getTimezone());

      dag.getScheduleInterval().setValue("30 11 * * 1-5");

      // Define your own cron: arbitrary fields are allowed and last field can be optional
//      CronDefinition cronDefinition =
//          CronDefinitionBuilder.defineCron()
//              .withMinutes().and()
//              .withHours().and()
//              .withDayOfMonth()
//              .supportsHash().supportsL().supportsW().and()
//              .withMonth().and()
//              .withDayOfWeek()
//              .withIntMapping(7, 0) //we support non-standard non-zero-based numbers!
//              .supportsHash().supportsL().supportsW().and()
//              .instance();
      CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(UNIX);

      CronConverter cronConverter = new CronConverter(new CronToCalendarTransformer(), new CalendarToCronTransformer());
      String utc = cronConverter.using("30 11 * * 1-5")
          .from(ZoneId.of("Europe/London"))
          .to(ZoneId.of("UTC"))
          .convert();

      log.info("UTC cron express: {}", utc);

      // Create a descriptor for a specific Locale
      CronDescriptor descriptor = CronDescriptor.instance(Locale.UK);

      // Create a parser based on provided definition
      CronParser parser = new CronParser(cronDefinition);
      Cron quartzCron = parser.parse(utc);

      String description = descriptor.describe(quartzCron);
      log.info("Cron Description: {}", description);

      // Get date for last execution
      ZonedDateTime now = ZonedDateTime.now();
      ZonedDateTime utcNow = ZonedDateTime.ofInstant(now.toInstant(), ZoneId.of("UTC"));
      ExecutionTime executionTime = ExecutionTime.forCron(quartzCron);
      Optional<ZonedDateTime> lastExecutionTime = executionTime.lastExecution(utcNow);
      log.info("Last Execution Time: {}", lastExecutionTime.get());

      ZonedDateTime zdt = ZonedDateTime.ofInstant(lastExecutionTime.get().toInstant(), ZoneId.of("UTC"));
      log.info("zdt: {}", zdt);

      // Get date for next execution
      Optional<ZonedDateTime> nextExecutionTime = executionTime.nextExecution(utcNow);
      log.info("Next Execution Time: {}", nextExecutionTime.get());
    }


  }
}
