package uk.gov.hmcts.reform.dev.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.dev.model.Case;
import uk.gov.hmcts.reform.dev.repository.CaseRepository;

import java.time.LocalDateTime;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.springframework.http.ResponseEntity.ok;

/**
 * Controller for managing cases.
 * Provides endpoints to create, retrieve and delete {@link uk.gov.hmcts.reform.dev.model.Case}.
 */
@RestController
@RequestMapping("/case")
@Slf4j
public class CaseController {

    private final CaseRepository caseRepository;

    public CaseController(CaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    @GetMapping(value = "/getAllCases", produces = "application/json")
    public ResponseEntity<Iterable<Case>> getAllCases() {
        Iterable<Case> caseList = caseRepository.findAllByOrderByIdAsc();
        if (isEmpty(caseList)) {
            log.info("No cases found");
            return ResponseEntity.ok(emptyList());
        }
        return ok(caseList);
    }

    /**
     * Add a new case.
     * @param newCase the case to be added
     * @return ResponseEntity with the created case
     */
    @PostMapping(value = "/addCase")
    public ResponseEntity<Case> addCase(@RequestBody Case newCase) {
        if (isEmpty(newCase) || isEmpty(newCase.getCaseNumber())
            || isEmpty(newCase.getTitle())) {
            return ResponseEntity.badRequest().build();
        }
        if (isEmpty(newCase.getCreatedDate())) {
            newCase.setCreatedDate(LocalDateTime.now());
        }
        Case createdCase = caseRepository.save(newCase);
        return ok(createdCase);
    }

    /**
     * Get a case by its ID.
     * @param id the ID of the case
     * @return ResponseEntity with the case or not found if it does not exist
     */
    @GetMapping(value = "/{id}", produces = "application/json")
    public ResponseEntity<Case> getCaseById(@PathVariable int id) {
        Case existingCase = caseRepository.findCaseById(id);
        return ok(existingCase);
    }

    /**
     * Delete a case by its ID.
     * @param id the ID of the case to be deleted
     * @return ResponseEntity with no content if successful
     */
    @DeleteMapping(value = "/{id}", produces = "application/json")
    public ResponseEntity<Case> deleteCaseById(@PathVariable int id) {
        caseRepository.deleteById(id);
        return ok(null);
    }

    @GetMapping(value = "/searchCases", produces = "application/json")
    public ResponseEntity<Iterable<Case>> searchCases(@RequestParam (required = false) Integer caseNumber,
                                                      @RequestParam(required = false) String title) {
        Iterable<Case> caseList;
        if (isEmpty(caseNumber) && isEmpty(title)) {
            log.info("No search term provided, returning all cases");
            caseList = caseRepository.findAllByOrderByIdAsc();
        } else if (!isEmpty(caseNumber) && !isEmpty(title)) {
            caseList = caseRepository.findCasesByCaseNumberAndTitleContainingIgnoreCase(caseNumber, title);
        } else {
            caseList = isEmpty(caseNumber)
                ? caseRepository.findCasesByTitleContainingIgnoreCase(title)
                : caseRepository.findCasesByCaseNumber(caseNumber);
        }
        if (isEmpty(caseList)) {
            log.info("No cases found for the given search criteria");
            return ResponseEntity.ok(emptyList());
        }
        return ok(caseList);
    }

}
