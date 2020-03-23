package name.nkonev.spring.cloud.gateway;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryOptionsBuilderCustomizer;
import org.springframework.cloud.sleuth.instrument.messaging.TraceMessagingAutoConfiguration;
import org.springframework.cloud.sleuth.instrument.messaging.TraceSpringIntegrationAutoConfiguration;
import org.springframework.cloud.sleuth.instrument.rpc.TraceRpcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.tools.agent.ReactorDebugAgent;

import java.time.Duration;
import java.util.Arrays;

@SpringBootApplication(exclude = {TraceMessagingAutoConfiguration.class, TraceSpringIntegrationAutoConfiguration.class, TraceRpcAutoConfiguration.class})
@RestController
@EnableR2dbcRepositories(basePackages = "name.nkonev.spring.cloud.gateway")
public class GatewayApplication {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayApplication.class);

    public static void main(String[] args) {
        // https://projectreactor.io/docs/core/release/reference/#reactor-tools-debug
        ReactorDebugAgent.init();
        SpringApplication.run(GatewayApplication.class, args);
    }

    @GetMapping("/public/hello")
    public Mono<String> hello() {
        return Mono.just("Hello, Spring!");
    }

    @GetMapping("/self/hello")
    public Mono<String> selfHello() {
        return Mono.just("Hello, Spring!");
    }

    @Bean
    public ConnectionFactoryOptionsBuilderCustomizer setConnectTimeout() {
        // PostgresqlConnectionFactoryProvider
        // ConnectionFactoryOptions
        return builder -> builder.option(ConnectionFactoryOptions.CONNECT_TIMEOUT, Duration.ofSeconds(10));
    }

    @Bean
    public CommandLineRunner demo(CustomerRepository repository) {

        return (args) -> {
            // save a few customers
            repository.saveAll(Arrays.asList(new Customer("Jack", "Bauer"),
                    new Customer("Chloe", "O'Brian"),
                    new Customer("Kim", "Bauer"),
                    new Customer("David", "Palmer"),
                    new Customer("Michelle", "Dessler")))
                    .blockLast(Duration.ofSeconds(10));

            // fetch all customers
            LOGGER.info("Customers found with findAll():");
            LOGGER.info("-------------------------------");
            repository.findAll().doOnNext(customer -> {
                LOGGER.info(customer.toString());
            }).blockLast(Duration.ofSeconds(10));

            LOGGER.info("");

            // fetch an individual customer by ID
            repository.findById(1L).doOnNext(customer -> {
                LOGGER.info("Customer found with findById(1L):");
                LOGGER.info("--------------------------------");
                LOGGER.info(customer.toString());
                LOGGER.info("");
            }).block(Duration.ofSeconds(10));


            // fetch customers by last name
            LOGGER.info("Customer found with findByLastName('Bauer'):");
            LOGGER.info("--------------------------------------------");
            repository.findByLastName("Bauer").doOnNext(bauer -> {
                LOGGER.info(bauer.toString());
            }).blockLast(Duration.ofSeconds(10));;
            LOGGER.info("");
        };
    }
}