package io.hhplus.tdd;

import io.hhplus.tdd.exception.MaxChargeAmountException;
import io.hhplus.tdd.exception.MinChargeAmountException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class ApiControllerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(MinChargeAmountException.class)
    public ResponseEntity<ErrorResponse> handleMinCharge(MinChargeAmountException ex) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("MIN_CHARGE_ERROR", "최소 충전 금액은 100원입니다"));
    }

    @ExceptionHandler(MaxChargeAmountException.class)
    public ResponseEntity<ErrorResponse> handleMaxCharge(MaxChargeAmountException ex) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("MAX_CHARGE_ERROR", "최대 충전 금액은 100만원입니다"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("ILLEGAL_ARGUMENT", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity
                .status(409)  // Conflict
                .body(new ErrorResponse("INSUFFICIENT_BALANCE", ex.getMessage()));
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return ResponseEntity.status(500).body(new ErrorResponse("500", "에러가 발생했습니다."));
    }
}
