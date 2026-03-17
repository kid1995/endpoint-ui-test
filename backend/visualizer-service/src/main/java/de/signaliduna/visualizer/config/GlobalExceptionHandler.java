package de.signaliduna.visualizer.config;

import de.signaliduna.visualizer.model.SiErrorMessage;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<SiErrorMessage> handleConstraintViolation(ConstraintViolationException ex) {
        var error = new SiErrorMessage("Validation failed", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<SiErrorMessage> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        var error = new SiErrorMessage("Validation failed", ex.getBindingResult().toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<SiErrorMessage> handleIllegalArgument(IllegalArgumentException ex) {
        var error = new SiErrorMessage("Bad request", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<SiErrorMessage> handleRuntime(RuntimeException ex) {
        log.error("Unexpected error", ex);
        var error = new SiErrorMessage("Internal server error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
