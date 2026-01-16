package com.assignment.buyogo_backend_assignment.util;

import com.assignment.buyogo_backend_assignment.request.EventRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class EventPayloadHashUtil {

    public static String computeHash(EventRequest eventRequest){
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            String payload= String.format("%s|%s|%s|%s|%s|%s|%s",
                    eventRequest.eventId(),
                    eventRequest.eventTime(),
                    eventRequest.machineId(),
                    eventRequest.durationMs(),
                    eventRequest.defectCount(),
                    eventRequest.factoryId() != null ? eventRequest.factoryId() : "",
                    eventRequest.lineId() != null ? eventRequest.lineId() : ""
                    );

            byte[] hashBytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));

            return   bytesToHex(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 Algorithm Not Supported");
        }
    }

    private static String bytesToHex(byte[] bytes){
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString( 0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
