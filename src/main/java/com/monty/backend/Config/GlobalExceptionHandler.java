//package com.monty.backend.Config;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.authentication.BadCredentialsException;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<Map<String,String>> handleValidation(MethodArgumentNotValidException ex) {
//        String details = ex.getBindingResult().getFieldErrors().stream()
//                .map(e -> e.getField()+": "+e.getDefaultMessage())
//                .collect(Collectors.joining("; "));
//        return ResponseEntity
//                .badRequest()
//                .body(Map.of(
//                        "error",   "Validation Failed",
//                        "message", details
//                ));
//    }
//
//    @ExceptionHandler(BadCredentialsException.class)
//    public ResponseEntity<Map<String,String>> handleBadCreds(BadCredentialsException ex) {
//        return ResponseEntity
//                .status(HttpStatus.UNAUTHORIZED)
//                .body(Map.of(
//                        "error",   "Invalid Credentials",
//                        "message", ex.getMessage()
//                ));
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<Map<String,String>> handleAll(Exception ex) {
//        return ResponseEntity
//                .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(Map.of(
//                        "error",   "Server Error",
//                        "message", ex.getMessage()
//                ));
//    }
//}
//
