package uk.gov.hmcts.reform.dev.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.dev.model.Case;

import java.util.List;

@Repository
public interface CaseRepository extends CrudRepository<Case, Integer> {
    /**
     * Find a case by its ID.
     *
     * @param id the ID of the case
     * @return the case with the specified ID
     */
    Case findCaseById(int id);

    /**
     * Find all cases ordered by ID in ascending order.
     *
     * @return a list of cases ordered by ID
     */
    List<Case> findAllByOrderByIdAsc();

    List<Case> findCasesByCaseNumberAndTitleContainingIgnoreCase(Integer caseNumber, String title);

    List<Case> findCasesByCaseNumber(int caseNumber);
    List<Case> findCasesByTitleContainingIgnoreCase(String title);
}
