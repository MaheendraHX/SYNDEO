
package com.syndeo.backend.exception;

public class InvalidAccountTypeException extends RuntimeException {

    public InvalidAccountTypeException() {
        super("The selected account type is not supported.");
    }
}
