package com.saintstream.common;

import org.springframework.http.HttpStatus;

/** İstifadəçi səhvi (məs. yanlış parametr) üçün atılır; GlobalExceptionHandler onu JSON cavaba çevirir. */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
