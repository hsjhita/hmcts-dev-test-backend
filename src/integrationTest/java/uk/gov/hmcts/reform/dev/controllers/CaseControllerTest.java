package uk.gov.hmcts.reform.dev.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.dev.model.Case;
import uk.gov.hmcts.reform.dev.repository.CaseRepository;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class CaseControllerTest {

    public static final String CASE_ID = "/case/{id}";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("integration-tests-db")
        .withUsername("testuser")
        .withPassword("testpass");

    @DynamicPropertySource
    static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Case exampleCaseOne;
    private Case exampleCaseTwo;

    @BeforeEach
    void setUp() {
        caseRepository.deleteAll();
        exampleCaseOne = Case.builder()
            .title("Case 1 Title")
            .caseNumber(1234)
            .description("Case 1 Description")
            .build();
        exampleCaseTwo = Case.builder()
            .title("Case 2 Title")
            .caseNumber(5678)
            .description("Case 2 Description")
            .build();
        exampleCaseOne = caseRepository.save(exampleCaseOne);
        exampleCaseTwo = caseRepository.save(exampleCaseTwo);
    }

    @AfterEach
    void tearDown() {
        caseRepository.deleteAll();
    }

    @Test
    void getAllCasesShouldReturnSortedCases() throws Exception {
        Case caseToSave = Case.builder()
            .title("Case 3 Title")
            .caseNumber(91011)
            .description("Case 3 Description")
            .build();
        Case savedCase = caseRepository.save(caseToSave);
        List<Case> expectedCases = Arrays.asList(exampleCaseOne, exampleCaseTwo, savedCase);
        expectedCases.sort(java.util.Comparator.comparingInt(Case::getId));

        mockMvc.perform(get("/case/getAllCases")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].id", is(expectedCases.get(0).getId())))
            .andExpect(jsonPath("$[0].description", is(expectedCases.get(0).getDescription())))
            .andExpect(jsonPath("$[1].id", is(expectedCases.get(1).getId())))
            .andExpect(jsonPath("$[2].id", is(expectedCases.get(2).getId())));
    }

    @Test
    void shouldReturnEmptyListWhenNoCasesExist() throws Exception {
        caseRepository.deleteAll();
        mockMvc.perform(get("/case/getAllCases")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void addCaseShouldCreateNewCase() throws Exception {
        Case newCase = Case.builder()
            .title("New Case Title")
            .caseNumber(12345)
            .description("New Case")
            .build();

        mockMvc.perform(post("/case/addCase").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newCase)))
            .andExpect(status().isOk());

        List<Case> cases = (List<Case>) caseRepository.findAll();
        assertThat(cases).hasSize(3);
        assertThat(cases).anyMatch(c -> "New Case".equals(c.getDescription()));
    }

    @Test
    void getCaseByIdShouldReturnCaseWhenExists() throws Exception {
        mockMvc.perform(get(CASE_ID, exampleCaseOne.getId()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(exampleCaseOne.getId())))
            .andExpect(jsonPath("$.description", is(exampleCaseOne.getDescription())));
    }

    @Test
    void getCaseByIdShouldReturnOkWithNullBodyWhenNotExists() throws Exception {
        int nonExistentId = 999;
        MvcResult result = mockMvc.perform(
            get(CASE_ID, nonExistentId).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(content).isBlank();
    }


    @Test
    void deleteCaseByIdShouldRemoveCase() throws Exception {
        mockMvc.perform(delete(CASE_ID, exampleCaseOne.getId()).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        assertThat(caseRepository.findCaseById(exampleCaseOne.getId())).isNull();
        assertThat(caseRepository.count()).isEqualTo(1);
    }

    @Test
    void searchCasesShouldReturnCasesByCaseNumber() throws Exception {
        mockMvc.perform(get("/case/searchCases")
                            .param("caseNumber", String.valueOf(exampleCaseOne.getCaseNumber()))
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id", is(exampleCaseOne.getId())))
            .andExpect(jsonPath("$[0].description", is(exampleCaseOne.getDescription())));
    }

    @Test
    void searchCasesShouldReturnCasesByTitle() throws Exception {
        mockMvc.perform(get("/case/searchCases")
                            .param("title", exampleCaseTwo.getTitle())
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id", is(exampleCaseTwo.getId())))
            .andExpect(jsonPath("$[0].description", is(exampleCaseTwo.getDescription())));
    }

    @Test
    void searchCasesShouldReturnAllCasesWhenNoParamsProvided() throws Exception {
        mockMvc.perform(get("/case/searchCases")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].id", is(exampleCaseOne.getId())))
            .andExpect(jsonPath("$[1].id", is(exampleCaseTwo.getId())));
    }

    @Test
    void searchCasesShouldReturnEmptyListWhenNoCasesMatch() throws Exception {
        mockMvc.perform(get("/case/searchCases")
                            .param("caseNumber", "99999")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void searchCasesShouldReturnCasesByCaseNumberAndTitle() throws Exception {
        mockMvc.perform(get("/case/searchCases")
                            .param("caseNumber", String.valueOf(exampleCaseOne.getCaseNumber()))
                            .param("title", exampleCaseOne.getTitle())
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id", is(exampleCaseOne.getId())))
            .andExpect(jsonPath("$[0].description", is(exampleCaseOne.getDescription())));
    }

}
