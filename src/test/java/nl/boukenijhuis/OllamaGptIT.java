package nl.boukenijhuis;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import nl.boukenijhuis.assistants.ollama.Ollama;
import nl.boukenijhuis.dto.CodeContainer;
import nl.boukenijhuis.dto.PreviousRunContainer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertEquals;

@WireMockTest(httpPort = 8089)
public class OllamaGptIT extends IntegrationTest {

    private final String RETRY_SCENARIO = "retry";
    private String SECOND_REPLY = "second";

    @Test
    public void integrationTest() throws IOException, InterruptedException {
        // first reply
        String path = "/api/generate";
        stubFor(post(path)
                .inScenario(RETRY_SCENARIO)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(ok(readFile("stub/ollama/stub_without_code.json")))
                .willSetStateTo(SECOND_REPLY));

        // second reply
        stubFor(post(path)
                .inScenario(RETRY_SCENARIO)
                .whenScenarioStateIs(SECOND_REPLY)
                .willReturn(ok(readFile("stub/ollama/stub_with_working_code.json"))));

        Properties properties = new Properties();
        properties.setProperty("ollama.server", "http://localhost:8089");
        properties.setProperty("ollama.url", path);
        properties.setProperty("ollama.timeout", "30");
        var aiAssistant = new Ollama(properties);

        CodeContainer response = aiAssistant.call(Path.of("src", "test", "resources", "input", "PrimeNumberGeneratorTest.java"), new PreviousRunContainer());
        assertEquals(readFile("expected/ollama/PrimeNumberGenerator.java"), response.getContent());
        assertEquals("PrimeNumberGenerator.java", response.getFileName());
        assertEquals(2, response.getAttempts());
        assertEquals(List.of(4, 5, 6), aiAssistant.getContext());
    }

    @Test
    public void checkForTestsBeingRun() throws IOException, InterruptedException {

        stubFor(post("/api/generate").willReturn(ok(readFile("stub/ollama/stub_with_working_code_container_code.json"))));

        Properties properties = new Properties();
        properties.setProperty("ollama.server", "http://localhost:8089");
        properties.setProperty("ollama.url", "/api/generate");
        properties.setProperty("ollama.timeout", "30");

        Path tempDirectory = Files.createTempDirectory("test");
        String inputFile = "src/test/resources/input/CodeContainerTest.java";
        String[] args = {inputFile, tempDirectory.toString()};
        TestRunner testRunner = new TestRunner();
        new Generator().run(new Ollama(properties), testRunner, args);

        // check the output of the testrunner
        TestRunner.TestInfo latestTestInfo = testRunner.getLatestTestInfo();
        assertEquals(2, latestTestInfo.found());
        assertEquals(0, latestTestInfo.succeeded());
    }

}
