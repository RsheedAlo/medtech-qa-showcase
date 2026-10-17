package com.medtechqa.api;

import com.medtechqa.domain.AuditEntry;
import com.medtechqa.domain.SurgicalCase;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory storage and lookup. The rules themselves live in {@link SurgicalCase}. */
@Service
public class CaseService {

    private final Map<String, SurgicalCase> cases = new ConcurrentHashMap<>();
    private final Clock clock = Clock.systemUTC();

    public SurgicalCase create(String patientId, String interventionType, String user) {
        SurgicalCase created = SurgicalCase.create(UUID.randomUUID().toString(), patientId, interventionType, user, clock);
        cases.put(created.getId(), created);
        return created;
    }

    public SurgicalCase get(String id) {
        SurgicalCase found = cases.get(id);
        if (found == null) {
            throw new CaseNotFoundException(id);
        }
        return found;
    }

    public List<SurgicalCase> list() {
        return cases.values().stream()
                .sorted(Comparator.comparing(c -> c.getAuditTrail().get(0).timestamp()))
                .toList();
    }

    public void addFile(String id, String fileName, long sizeBytes, String user) {
        get(id).addFile(fileName, sizeBytes, user);
    }

    public void submit(String id, String user) {
        get(id).submitForReview(user);
    }

    public void approve(String id, String user, String confirmedPatientId) {
        get(id).approve(user, confirmedPatientId);
    }

    public List<AuditEntry> auditTrail(String id) {
        return get(id).getAuditTrail();
    }
}
