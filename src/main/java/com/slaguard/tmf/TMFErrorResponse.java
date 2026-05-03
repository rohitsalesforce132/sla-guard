package com.slaguard.tmf;

import java.time.LocalDateTime;

/**
 * Standard TMF Open API error response
 */
public class TMFErrorResponse {

    public String code;
    public String reason;
    public String message;
    public String referenceError;
    public int status;
    public LocalDateTime timestamp;
    public String path;
    public String requestId;
}
