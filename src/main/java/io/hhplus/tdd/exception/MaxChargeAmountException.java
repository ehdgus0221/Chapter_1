package io.hhplus.tdd.exception;

public class MaxChargeAmountException extends IllegalArgumentException{
    public MaxChargeAmountException() {
        super("최대 충전 금액은 100만원입니다.");
    }
}
