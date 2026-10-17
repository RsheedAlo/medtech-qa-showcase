package com.medtechqa.domain;

/** Metadata of an accepted planning file. The content itself is not stored (simplification). */
public record PlanningFile(String name, long sizeBytes) {
}
