package io.github.nebulaal.fwdata;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
class ApiErrors {
    record Message(String message) {}

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<Message> duplicate() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new Message("名称或账号已存在，请换一个"));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
                       MethodArgumentTypeMismatchException.class})
    ResponseEntity<Message> invalid() {
        return ResponseEntity.badRequest().body(new Message("请检查必填项、字段格式和长度"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Message> status(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(new Message(error.getReason()));
    }
}
