package uk.gov.hmcts.reform.dev.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.dev.controllers.CaseController;
import uk.gov.hmcts.reform.dev.model.Case;
import uk.gov.hmcts.reform.dev.repository.CaseRepository;

import java.util.Arrays;
import java.util.Collections;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CaseControllerTest {

    private CaseRepository caseRepository;
    private CaseController controller;

    @BeforeEach
    void setUp() {
        caseRepository = mock(CaseRepository.class);
        controller = new CaseController(caseRepository);
    }

    @Test
    void getAllCases_returnsCases() {
        Case exampleCaseOne = Case.builder()
            .id(1)
            .title("Case1")
            .build();
        Case exampleCaseTwo = Case.builder()
            .id(2)
            .title("Case2")
            .build();

        when(caseRepository.findAllByOrderByIdAsc()).thenReturn(Arrays.asList(exampleCaseOne, exampleCaseTwo));
        ResponseEntity<Iterable<Case>> response = controller.getAllCases();
        assertThat(response.getBody()).containsExactly(exampleCaseOne, exampleCaseTwo);
    }

    @Test
    void getAllCases_noCases() {
        when(caseRepository.findAll()).thenReturn(emptyList());
        ResponseEntity<Iterable<Case>> response = controller.getAllCases();
        assertThat(response.getBody()).isNull();

    }

    @Test
    void addCase_savesCase() {
        Case caseOne = Case.builder()
            .id(1)
            .title("Case1")
            .caseNumber(12345)
            .build();

        controller.addCase(caseOne);

        verify(caseRepository, times(1)).save(caseOne);
    }

    @Test
    void addCase_invalidCase_returnsBadRequest() {
        Case invalidCase = Case.builder().build(); // Missing required fields
        ResponseEntity<Case> response = controller.addCase(invalidCase);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void getCaseById_returnsCase() {
        Case exampleCaseOne = Case.builder()
            .id(1)
            .title("Case1")
            .build();
        when(caseRepository.findCaseById(1)).thenReturn(exampleCaseOne);

        ResponseEntity<Case> response = controller.getCaseById(1);

        assertThat(response.getBody()).isEqualTo(exampleCaseOne);
    }

    @Test
    void deleteCaseById_deletesCase() {
        ResponseEntity<Case> response = controller.deleteCaseById(1);

        verify(caseRepository).deleteById(1);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void searchCases_withCaseNumberAndTitle_returnsFilteredCases() {
        Case exampleCaseOne = Case.builder()
            .id(1)
            .title("Case1")
            .caseNumber(12345)
            .build();

        when(caseRepository.findCasesByCaseNumberAndTitleContainingIgnoreCase(12345, "Case1"))
            .thenReturn(Collections.singletonList(exampleCaseOne));

        ResponseEntity<Iterable<Case>> response = controller.searchCases(12345, "Case1");

        assertThat(response.getBody()).containsExactly(exampleCaseOne);
    }

    @Test
    void searchCases_withOnlyCaseNumber_returnsFilteredCases() {
        Case exampleCaseOne = Case.builder()
            .id(1)
            .title("Case1")
            .caseNumber(12345)
            .build();

        when(caseRepository.findCasesByCaseNumber(12345))
            .thenReturn(Collections.singletonList(exampleCaseOne));

        ResponseEntity<Iterable<Case>> response = controller.searchCases(12345, null);

        assertThat(response.getBody()).containsExactly(exampleCaseOne);
    }

    @Test
    void searchCases_withOnlyTitle_returnsFilteredCases() {
        Case exampleCaseOne = Case.builder()
            .id(1)
            .title("Case1")
            .caseNumber(12345)
            .build();

        when(caseRepository.findCasesByTitleContainingIgnoreCase("Case1"))
            .thenReturn(Collections.singletonList(exampleCaseOne));

        ResponseEntity<Iterable<Case>> response = controller.searchCases(null, "Case1");

        assertThat(response.getBody()).containsExactly(exampleCaseOne);
    }

    @Test
    void searchCases_noSearchCriteria_returnsAllCases() {
        Case exampleCaseOne = Case.builder()
            .id(1)
            .title("Case1")
            .caseNumber(12345)
            .build();
        Case exampleCaseTwo = Case.builder()
            .id(2)
            .title("Case2")
            .caseNumber(67890)
            .build();

        when(caseRepository.findAllByOrderByIdAsc()).thenReturn(Arrays.asList(exampleCaseOne, exampleCaseTwo));

        ResponseEntity<Iterable<Case>> response = controller.searchCases(null, null);

        assertThat(response.getBody()).containsExactly(exampleCaseOne, exampleCaseTwo);
    }

    @Test
    void searchCases_noResults_returnsEmptyList() {
        when(caseRepository.findCasesByCaseNumberAndTitleContainingIgnoreCase(12345, "NonExistent"))
            .thenReturn(emptyList());

        ResponseEntity<Iterable<Case>> response = controller.searchCases(12345, "NonExistent");

        assertThat(response.getBody()).isEmpty();
    }

}
