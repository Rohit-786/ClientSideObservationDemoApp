package com.example.clientapplication.config

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import kotlin.random.Random


@Configuration(proxyBeanMethods = false)
internal class MyConfiguration {
    // IMPORTANT! To instrument RestTemplate you must inject the RestTemplateBuilder
    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        return builder.build()
    }

    @Bean
    fun myCommandLineRunner(registry: ObservationRegistry?, restTemplate: RestTemplate): CommandLineRunner? {
        val highCardinalityValues = Random // Simulates potentially large number of values
        val lowCardinalityValues: List<String> = mutableListOf("userType1", "userType2", "userType3") // Simulates low number of values
        return CommandLineRunner {
            val highCardinalityUserId: String = java.lang.String.valueOf(highCardinalityValues.nextLong(100000))
            // Example of using the Observability API manually
            // <my.observation> is a "technical" name that does not depend on the context. It will be used to name e.g. Metrics
            Observation.createNotStarted("my.observation", registry) // Low cardinality means that the number of potential values won't be big. Low cardinality entries will end up in e.g. Metrics
                    .lowCardinalityKeyValue("userType", randomUserTypePicker(lowCardinalityValues)) // High cardinality means that the number of potential values can be large. High cardinality entries will end up in e.g. Spans
                    .highCardinalityKeyValue("userId", highCardinalityUserId) // <command-line-runner> is a "contextual" name that gives more details within the provided context. It will be used to name e.g. Spans
                    .contextualName("command-line-runner") // The following lambda will be executed with an observation scope (e.g. all the MDC entries will be populated with tracing information). Also the observation will be started, stopped and if an error occurred it will be recorded on the observation
                    .observe {
                        log.info("Will send a request to the server") // Since we're in an observation scope - this log line will contain tracing MDC entries ...
                        val response = restTemplate.getForObject("http://localhost:7654/user/{userId}", String::class.java, highCardinalityUserId) // Boot's RestTemplate instrumentation creates a child span here
                        log.info("Got response [{}]", response) // ... so will this line
                    }
        }
    }
    fun randomUserTypePicker(lowCardinalityValues: List<String>): String {
        // Generate a random index to select a user type
        val randomIndex = Random.nextInt(lowCardinalityValues.size)
        // Return the randomly selected user type
        return lowCardinalityValues[randomIndex]
    }
    companion object {
        private val log: Logger = LoggerFactory.getLogger(MyConfiguration::class.java)
    }
}

