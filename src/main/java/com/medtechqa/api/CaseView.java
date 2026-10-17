package com.medtechqa.api;

import com.medtechqa.domain.CaseStatus;
import com.medtechqa.domain.InterventionType;
import com.medtechqa.domain.PlanningFile;
import com.medtechqa.domain.SurgicalCase;

import java.util.List;

/** JSON shape of a case. Keeps the domain class free of serialization concerns. */
public record CaseView(String id, String patientId, InterventionType interventionType, CaseStatus status,
                       String createdBy, String approvedBy, List<PlanningFile> files) {

    public static CaseView of(SurgicalCase c) {
        return new CaseView(c.getId(), c.getPatientId(), c.getInterventionType(), c.getStatus(),
                c.getCreatedBy(), c.getApprovedBy(), c.getFiles());
    }
}
