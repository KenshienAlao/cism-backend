package com.cism.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.cism.backend.dto.common.Api;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ExistException.class)
    public ResponseEntity<Api<Object>> existException(ExistException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(Api.error(ex.getMessage(), ex.getCode(), null));
    }

    @ExceptionHandler(BadrequestException.class)
    public ResponseEntity<Api<Object>> badrequestException(BadrequestException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Api.error(ex.getMessage(), ex.getCode(), ex.getData()));
    }

    @ExceptionHandler(NotfoundException.class)
    public ResponseEntity<Api<Object>> notFoundException(NotfoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Api.error(ex.getMessage(), ex.getCode(), null));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Api<Object>> unauthorizedException(UnauthorizedException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(Api.error(ex.getMessage(), ex.getCode(), null));
    }
}
