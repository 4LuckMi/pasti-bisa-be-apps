package com.pasti_bisa.backend.modules.operations.application;

import java.util.Optional;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public interface JobQueue {
    record Job(UUID id,String type,JsonNode payload,UUID token) {}
    Optional<Job> claim();
    void succeeded(Job job);
    void failed(Job job,Exception exception);
    void requireLease(Job job);
}
