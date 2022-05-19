package com.boutiqaat.ingestor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicLong;

public interface IngestionStreamWriter {

    String success = "success";
    String failure = "failure";
    String opening = "[";
    String closing = "\"done\": true]";
    //last model will have a ',' after it, so empty '{}' to allow JSON processing
    void ingest(OutputStream outputStream, int startPage, int pageSize, boolean showSuccess, boolean showFailure);

    default void write(PrintWriter writer, ObjectMapper mapper, Logger log, Object model) {
        try {
            writer.println(mapper.writeValueAsString(model) + ",");
        } catch (JsonProcessingException ex) {
            log.error("Object Mapper failed to write model to JSON, model {}", model);
            ex.printStackTrace();
        }
    }

    default void writeSuccess(PrintWriter writer, ObjectMapper mapper, Logger log, Object model, AtomicLong counter) {
        String successCount = "\""+success+"_"+counter+"\"";
        try {
            writer.println(successCount + ":" + mapper.writeValueAsString(model));
        } catch (JsonProcessingException ex) {
            log.error("Object Mapper failed to write model to JSON, model {}", model + ",");
            ex.printStackTrace();
        }
    }

    default void writeFailure(PrintWriter writer, ObjectMapper mapper, Logger log, Object model, AtomicLong counter) {
        String failureCount = "\""+failure+"_"+counter+"\"";
        try {
            writer.println(failureCount + ":" + mapper.writeValueAsString(model) + ",");
        } catch (JsonProcessingException ex) {
            log.error("Object Mapper failed to write model to JSON, model {}", model);
            ex.printStackTrace();
        }
    }
}
