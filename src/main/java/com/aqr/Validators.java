package com.aqr;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;



public class Validators {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static HashMap getValidQueryInput(String body) throws IOException {
        HashMap input;

        // Parse the input
        try {
            input = objectMapper.readValue(body, HashMap.class);
        } catch (Exceptions e) {
            throw Exceptions.invalidInput;
        }



         return input;
    }
}