package com.sunxin.knowledge.common.id;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class TimeBasedIdGenerator implements IdGenerator {

    private static final int SEQUENCE_BITS = 20;

    private final AtomicLong lastId = new AtomicLong(System.currentTimeMillis() << SEQUENCE_BITS);

    @Override
    public long nextId() {
        while (true) {
            long candidate = System.currentTimeMillis() << SEQUENCE_BITS;
            long previous = lastId.get();
            long next = Math.max(candidate, previous + 1);
            if (lastId.compareAndSet(previous, next)) {
                return next;
            }
        }
    }
}
