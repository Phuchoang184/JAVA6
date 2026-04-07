package com.leika.shop.exception;

/**
 * Ném ngoại lệ khi đăng ký với email đã tồn tại.
 */
public class EmailAlreadyExistsException extends RuntimeException {
    
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
