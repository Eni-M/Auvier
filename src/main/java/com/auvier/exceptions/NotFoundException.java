package com.auvier.exceptions;

import lombok.Data;
// import org.springframework.http.HttpStatus;

@Data
public class NotFoundException extends RuntimeException {
  //  private HttpStatus httpStatus = HttpStatus.NOT_FOUND;
    public NotFoundException(String message) {
        super(message);
    }
}
