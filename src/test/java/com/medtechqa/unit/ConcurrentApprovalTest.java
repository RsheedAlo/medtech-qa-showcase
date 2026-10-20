package com.medtechqa.unit;

import com.medtechqa.domain.AuditAction;
import com.medtechqa.domain.CaseStatus;
import com.medtechqa.domain.DomainException;
import com.medtechqa.domain.SurgicalCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcurrentApprovalTest {

    // Origin: AI proposal KI-11 (see docs/AI-TESTING.md), adopted after review.
    // No sleeps: a latch lets both threads start at the same moment; the result is the same for every order.
    @Test
    @Tag("REQ-004")
    @Tag("REQ-008")
    @DisplayName("REQ-004 REQ-008 two simultaneous approvals: exactly one succeeds, one APPROVED entry")
    void onlyOneOfTwoSimultaneousApprovalsSucceeds() throws Exception {
        SurgicalCase c = Fixtures.inReview();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (String reviewer : List.of("chirurg1", "chirurg2")) {
                Callable<Boolean> approval = () -> {
                    start.await();
                    try {
                        c.approve(reviewer, Fixtures.PATIENT_ID);
                        return true;
                    } catch (DomainException.StateConflict e) {
                        return false;
                    }
                };
                results.add(pool.submit(approval));
            }
            start.countDown();

            int successes = 0;
            for (Future<Boolean> result : results) {
                if (result.get(10, TimeUnit.SECONDS)) {
                    successes++;
                }
            }

            assertEquals(1, successes);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(CaseStatus.APPROVED, c.getStatus());
        assertEquals(1, c.getAuditTrail().stream().filter(e -> e.action() == AuditAction.APPROVED).count());
        assertTrue(List.of("chirurg1", "chirurg2").contains(c.getApprovedBy()));
    }
}
