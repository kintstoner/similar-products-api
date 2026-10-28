package com.example.similarproducts.api;

import com.example.similarproducts.domain.ProductNotFoundException;
import com.example.similarproducts.domain.UpstreamUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps domain exceptions to RFC 9457 Problem Details.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProductNotFoundException.class)
    ProblemDetail handleProductNotFound(ProductNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /**
     * Outside the agreed contract (which only defines 200 and 404): without the similar ids there is nothing
     * meaningful to return, and a 503 tells the caller the failure is temporary.
     */
    @ExceptionHandler(UpstreamUnavailableException.class)
    ProblemDetail handleUpstreamUnavailable(UpstreamUnavailableException e) {
        // The client only gets a generic message, so the cause has to be in the log
        log.warn("Answering 503: {} (cause: {})", e.getMessage(), String.valueOf(e.getCause()));
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Similar products are temporarily unavailable");
    }
}
