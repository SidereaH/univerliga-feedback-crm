package com.univerliga.crm.service;

import com.univerliga.crm.dto.CreatePersonRequest;
import com.univerliga.crm.dto.UpdateIdentityRequest;
import com.univerliga.crm.dto.UpdatePersonRequest;
import com.univerliga.crm.error.ApiExceptionFactory;
import com.univerliga.crm.model.IdentityStatus;
import com.univerliga.crm.model.PersonEntity;
import com.univerliga.crm.model.PersonRole;
import com.univerliga.crm.repository.PersonRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;
    private final OutboxService outboxService;

    @Transactional(readOnly = true)
    public Page<PersonEntity> search(String query, String departmentId, String teamId, Boolean active, Pageable pageable) {
        String normalized = query == null ? null : query.trim().toLowerCase(Locale.ROOT);
        boolean hasQuery = normalized != null && !normalized.isBlank();
        String queryPattern = hasQuery ? "%" + normalized + "%" : "%";
        return personRepository.search(hasQuery, queryPattern, departmentId, teamId, active, pageable);
    }

    @Transactional(readOnly = true)
    public PersonEntity getById(String personId) {
        return personRepository.findById(personId)
                .orElseThrow(() -> ApiExceptionFactory.notFound("Person not found: " + personId));
    }

    @Transactional
    public PersonEntity create(CreatePersonRequest request) {
        PersonEntity person = new PersonEntity();
        person.setId("p_" + UUID.randomUUID());
        person.setDisplayName(request.displayName());
        person.setEmail(normalizeEmail(request.email()));
        person.setDepartmentId(request.departmentId());
        person.setTeamId(request.teamId());
        person.setRole(request.role());
        person.setActive(true);
        person.setIdentityStatus(IdentityStatus.PENDING);

        try {
            personRepository.save(person);
        } catch (DataIntegrityViolationException ex) {
            throw ApiExceptionFactory.conflict("Duplicate email");
        }

        outboxService.enqueue("Person", person.getId(), "PersonCreated", "crm.person.created", toPersonEventPayload(person));
        return person;
    }

    @Transactional
    public PersonEntity patch(String personId, UpdatePersonRequest request) {
        PersonEntity person = getById(personId);
        boolean wasActive = person.isActive();

        if (request.displayName() != null) {
            person.setDisplayName(request.displayName());
        }
        if (request.departmentId() != null) {
            person.setDepartmentId(request.departmentId());
        }
        if (request.teamId() != null) {
            person.setTeamId(request.teamId());
        }
        if (request.role() != null) {
            person.setRole(request.role());
        }
        if (request.active() != null) {
            person.setActive(request.active());
        }

        if (wasActive && !person.isActive()) {
            person.setIdentityStatus(IdentityStatus.DEPROVISIONED);
        }

        personRepository.save(person);

        if (wasActive && !person.isActive()) {
            outboxService.enqueue("Person", person.getId(), "PersonDeactivated", "crm.person.deactivated", toPersonEventPayload(person));
        } else {
            outboxService.enqueue("Person", person.getId(), "PersonUpdated", "crm.person.updated", toPersonEventPayload(person));
        }

        return person;
    }

    @Transactional
    public void softDelete(String personId) {
        PersonEntity person = getById(personId);
        person.setActive(false);
        person.setIdentityStatus(IdentityStatus.DEPROVISIONED);
        personRepository.save(person);
        outboxService.enqueue("Person", person.getId(), "PersonDeactivated", "crm.person.deactivated", toPersonEventPayload(person));
    }

    @Transactional
    public PersonEntity updateIdentity(String personId, UpdateIdentityRequest request) {
        PersonEntity person = getById(personId);
        person.setIdentityStatus(request.identityStatus());
        person.setKeycloakUserId(request.keycloakUserId());
        person.setLastIdentityError(request.lastIdentityError());
        return personRepository.save(person);
    }

    @Transactional(readOnly = true)
    public void ensureActivePerson(String personId) {
        if (!personRepository.existsByIdAndActiveTrue(personId)) {
            throw ApiExceptionFactory.badRequest("Person is missing or inactive: " + personId);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private Map<String, Object> toPersonEventPayload(PersonEntity person) {
        return Map.of(
                "personId", person.getId(),
                "username", usernameFromEmail(person.getEmail()),
                "email", person.getEmail(),
                "displayName", person.getDisplayName(),
                "departmentId", person.getDepartmentId(),
                "teamId", person.getTeamId(),
                "roles", List.of(toRealmRole(person.getRole())),
                "enabled", person.isActive()
        );
    }

    private String toRealmRole(PersonRole role) {
        return switch (role) {
            case EMPLOYEE -> "ROLE_EMPLOYEE";
            case MANAGER -> "ROLE_MANAGER";
            case HR -> "ROLE_HR";
            case ADMIN -> "ROLE_ADMIN";
        };
    }

    private String usernameFromEmail(String email) {
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
