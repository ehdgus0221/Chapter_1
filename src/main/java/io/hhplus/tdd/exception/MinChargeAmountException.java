package io.hhplus.tdd.exception;

public class MinChargeAmountException extends IllegalArgumentException{
    public MinChargeAmountException() {
        super("최소 충전 금액은 100원입니다.");
    }
}
