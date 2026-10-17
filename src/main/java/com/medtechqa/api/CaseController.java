package com.medtechqa.api;

import com.medtechqa.domain.AuditEntry;
import com.medtechqa.domain.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

/**
 * The acting user comes in the X-User header. There is no authentication (documented simplification, RISK-02).
 */
@RestController
@RequestMapping("/api/cases")
public class CaseController {

    static final String USER_HEADER = "X-User";

    private final CaseService service;

    public CaseController(CaseService service) {
        this.service = service;
    }

    public record CreateCaseRequest(String patientId, String interventionType) {
    }

    public record ApproveRequest(String confirmPatientId) {
    }

    @PostMapping
    public ResponseEntity<CaseView> create(@RequestHeader(value = USER_HEADER, required = false) String user,
                                           @RequestBody(required = false) CreateCaseRequest request) {
        if (request == null) {
            throw new DomainException.InvalidInput("Anfrage-Body fehlt.");
        }
        CaseView created = CaseView.of(service.create(request.patientId(), request.interventionType(), user));
        return ResponseEntity.created(URI.create("/api/cases/" + created.id())).body(created);
    }

    @GetMapping
    public List<CaseView> list() {
        return service.list().stream().map(CaseView::of).toList();
    }

    @GetMapping("/{id}")
    public CaseView get(@PathVariable String id) {
        return CaseView.of(service.get(id));
    }

    @PostMapping("/{id}/files")
    @ResponseStatus(HttpStatus.CREATED)
    public CaseView upload(@PathVariable String id,
                           @RequestHeader(value = USER_HEADER, required = false) String user,
                           @RequestPart("file") MultipartFile file) {
        service.addFile(id, file.getOriginalFilename(), file.getSize(), user);
        return CaseView.of(service.get(id));
    }

    @PostMapping("/{id}/submit")
    public CaseView submit(@PathVariable String id,
                           @RequestHeader(value = USER_HEADER, required = false) String user) {
        service.submit(id, user);
        return CaseView.of(service.get(id));
    }

    @PostMapping("/{id}/approve")
    public CaseView approve(@PathVariable String id,
                            @RequestHeader(value = USER_HEADER, required = false) String user,
                            @RequestBody(required = false) ApproveRequest request) {
        service.approve(id, user, request == null ? null : request.confirmPatientId());
        return CaseView.of(service.get(id));
    }

    @GetMapping("/{id}/audit")
    public List<AuditEntry> audit(@PathVariable String id) {
        return service.auditTrail(id);
    }
}
