package com.upload.service.impl;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Created by macbookproritena on 1/24/19.
 */
//@ThreadSafe
class AtomicBigDecimal {
    private final Object lock = new Object();
    //@GuardedBy("lock")
    private BigDecimal decimal;

    AtomicBigDecimal(BigDecimal decimal) {
        this.decimal = Objects.requireNonNull(decimal, "Null input");
    }

    BigDecimal get() {
        synchronized (lock) {
            return decimal;
        }
    }

    void increment() {
        synchronized (lock) {
            decimal = decimal.add(BigDecimal.ONE);
        }
    }

    void decrement() {
        synchronized (lock) {
            decimal = decimal.subtract(BigDecimal.ONE);
        }
    }

    void reset() {
        synchronized (lock) {
            decimal = BigDecimal.ZERO;
        }
    }

}
