package com.matraknhash.thread;

import com.matraknhash.model.Part;

import java.util.List;

public record LowStockEvent(List<Part> parts, long timestampMillis) {
    public LowStockEvent(List<Part> parts) {
        this(parts, System.currentTimeMillis());
    }
}
