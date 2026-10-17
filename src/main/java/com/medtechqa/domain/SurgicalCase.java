package com.medtechqa.domain;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A surgical planning case with its workflow rules. Framework-free on purpose,
 * so every rule can be unit-tested without Spring.
 */
public class SurgicalCase {

    public static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB = 10_485_760 byte
    public static final Set<String> ALLOWED_EXTENSIONS = Set.of("stl", "pdf", "png");

    private static final Pattern PATIENT_ID = Pattern.compile("P-[0-9]{6}");

    private final String id;
    private final String patientId;
    private final InterventionType interventionType;
    private final String createdBy;
    private final Clock clock;
    private final List<PlanningFile> files = new ArrayList<>();
    private final List<AuditEntry> auditTrail = new ArrayList<>();
    private CaseStatus status = CaseStatus.DRAFT;
    private String approvedBy;

    private SurgicalCase(String id, String patientId, InterventionType type, String createdBy, Clock clock) {
        this.id = id;
        this.patientId = patientId;
        this.interventionType = type;
        this.createdBy = createdBy;
        this.clock = clock;
    }

    /** REQ-001. Validates all input before anything is created. */
    public static SurgicalCase create(String id, String patientId, String interventionType, String user, Clock clock) {
        String actor = requireUser(user);
        if (patientId == null || !PATIENT_ID.matcher(patientId).matches()) {
            throw new DomainException.InvalidInput(
                    "Patient-ID muss das Format 'P-' gefolgt von genau 6 Ziffern haben, z. B. P-004711.");
        }
        InterventionType type = parseInterventionType(interventionType);
        SurgicalCase c = new SurgicalCase(id, patientId, type, actor, clock);
        c.log(actor, AuditAction.CASE_CREATED, "Fall angelegt (" + type + ")");
        return c;
    }

    /** REQ-002, REQ-003, REQ-007. Rejected uploads change nothing and are not logged. */
    public synchronized void addFile(String fileName, long sizeBytes, String user) {
        String actor = requireUser(user);
        if (status == CaseStatus.APPROVED) {
            throw new DomainException.StateConflict("Der Fall ist freigegeben und kann nicht mehr verändert werden.");
        }
        String name = baseName(fileName);
        String extension = extensionOf(name);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new DomainException.UnsupportedFileType(
                    "Dateityp nicht erlaubt. Erlaubt sind nur .stl, .pdf und .png.");
        }
        if (sizeBytes <= 0) {
            throw new DomainException.InvalidInput("Leere Dateien werden nicht angenommen.");
        }
        if (sizeBytes > MAX_FILE_SIZE_BYTES) {
            throw new DomainException.FileTooLarge(
                    "Datei zu groß. Das Limit liegt bei 10 MB (" + MAX_FILE_SIZE_BYTES + " Byte).");
        }
        files.add(new PlanningFile(name, sizeBytes));
        log(actor, AuditAction.FILE_UPLOADED, name + " (" + sizeBytes + " Byte)");
    }

    /** REQ-004. DRAFT -> IN_REVIEW. */
    public synchronized void submitForReview(String user) {
        String actor = requireUser(user);
        if (status != CaseStatus.DRAFT) {
            throw new DomainException.StateConflict(
                    "Nur ein Fall im Status DRAFT kann eingereicht werden (aktuell: " + status + ").");
        }
        status = CaseStatus.IN_REVIEW;
        log(actor, AuditAction.SUBMITTED_FOR_REVIEW, "DRAFT -> IN_REVIEW");
    }

    /**
     * REQ-004, REQ-005, REQ-006, REQ-010. All conditions must hold at once.
     * Every rejection is written to the audit trail with its reason (REQ-008),
     * except a missing user: then nobody can be named in the log.
     */
    public synchronized void approve(String user, String confirmedPatientId) {
        String actor = requireUser(user);
        if (status != CaseStatus.IN_REVIEW) {
            throw reject(actor, new DomainException.StateConflict(
                    "Freigabe nur im Status IN_REVIEW möglich (aktuell: " + status + ")."));
        }
        if (sameUser(actor, createdBy)) {
            throw reject(actor, new DomainException.NotPermitted(
                    "Vier-Augen-Prinzip: Der Ersteller darf den Fall nicht selbst freigeben."));
        }
        if (files.isEmpty()) {
            throw reject(actor, new DomainException.StateConflict(
                    "Freigabe nicht möglich: Der Fall hat keine Planungsdatei."));
        }
        if (!patientId.equals(confirmedPatientId)) {
            throw reject(actor, new DomainException.StateConflict(
                    "Freigabe nicht möglich: Die bestätigte Patient-ID fehlt oder stimmt nicht mit dem Fall überein."));
        }
        status = CaseStatus.APPROVED;
        approvedBy = actor;
        log(actor, AuditAction.APPROVED, "IN_REVIEW -> APPROVED");
    }

    public String getId() {
        return id;
    }

    public String getPatientId() {
        return patientId;
    }

    public InterventionType getInterventionType() {
        return interventionType;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public synchronized String getApprovedBy() {
        return approvedBy;
    }

    public synchronized CaseStatus getStatus() {
        return status;
    }

    /** Copy, so callers cannot change the stored list (REQ-009). */
    public synchronized List<PlanningFile> getFiles() {
        return List.copyOf(files);
    }

    /** Copy, so callers cannot change the stored list (REQ-009). */
    public synchronized List<AuditEntry> getAuditTrail() {
        return List.copyOf(auditTrail);
    }

    private DomainException reject(String actor, DomainException exception) {
        log(actor, AuditAction.APPROVAL_REJECTED, exception.getMessage());
        return exception;
    }

    private synchronized void log(String actor, AuditAction action, String details) {
        auditTrail.add(new AuditEntry(clock.instant(), actor, action, details));
    }

    private static String requireUser(String user) {
        if (user == null || user.isBlank()) {
            throw new DomainException.InvalidInput("Benutzer fehlt. Es muss angegeben sein, wer handelt.");
        }
        return user.trim();
    }

    private static boolean sameUser(String a, String b) {
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private static InterventionType parseInterventionType(String value) {
        if (value != null) {
            for (InterventionType t : InterventionType.values()) {
                if (t.name().equals(value)) {
                    return t;
                }
            }
        }
        throw new DomainException.InvalidInput("Eingriffstyp ungültig. Erlaubt sind KNEE, HIP und CRANIAL.");
    }

    /** Drops any path part, so a name like "..\\..\\x.stl" is stored as "x.stl". */
    private static String baseName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new DomainException.InvalidInput("Dateiname fehlt.");
        }
        String name = fileName.replace('\\', '/');
        return name.substring(name.lastIndexOf('/') + 1);
    }

    /** Only the last extension counts: "plan.stl.exe" has the extension "exe". */
    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
