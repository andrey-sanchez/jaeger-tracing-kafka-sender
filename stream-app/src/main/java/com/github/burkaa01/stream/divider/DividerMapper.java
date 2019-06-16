package com.github.burkaa01.stream.divider;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.streams.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DividerMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DividerMapper.class);

    @Value("${topics.dead-letter-topic}")
    private String deadLetterTopic;

    private final KafkaTemplate<String, Integer> deadLetterTemplate;

    public DividerMapper(KafkaTemplate<String, Integer> deadLetterTemplate) {
        this.deadLetterTemplate = deadLetterTemplate;
    }

    public KeyValue<String, Integer> map(String key, ValueWithHeaders valueWithHeaders) {
        if (valueWithHeaders == null) {
            return new KeyValue<>(null, null);
        }

        int number;
        try {
            number = Integer.parseInt(valueWithHeaders.getValue());
        } catch (NumberFormatException e) {
            return new KeyValue<>(null, null);
        }

        if (isOddNumber(number)) {
            toDeadLetterTopic(key, number, valueWithHeaders.getHeaders());
            return new KeyValue<>(null, null);
        }
        return new KeyValue<>(key, number);
    }

    private void toDeadLetterTopic(String key, int value, Headers headers) {
        LOGGER.info("{}: value={}", deadLetterTopic, value);

        ProducerRecord<String, Integer> deadLetterRecord = new ProducerRecord<>(
                deadLetterTopic,
                null,
                key,
                value,
                headers);

        deadLetterTemplate.send(deadLetterRecord);
    }

    private static boolean isOddNumber(int number) {
        return ((number % 2) != 0);
    }
}
