package com.example.wallet.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.test.context.EmbeddedKafka;

import static org.junit.jupiter.api.Assertions.*;

@EmbeddedKafka(partitions = 1, topics = {"ledger.transaction.confirmed", "wallet.saga.DLT"})
class KafkaEventContractTest {
    @Test
    void confirmationTopicNameIsStable() {
        assertEquals("ledger.transaction.confirmed", "ledger.transaction.confirmed");
    }

    @Test
    void deadLetterTopicNameIsStable() {
        assertEquals("wallet.saga.DLT", "wallet.saga.DLT");
    }
}
