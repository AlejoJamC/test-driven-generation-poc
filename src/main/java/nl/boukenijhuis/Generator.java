package nl.boukenijhuis;

import nl.boukenijhuis.assistants.AIAssistant;
import nl.boukenijhuis.assistants.llama2.Llama2;
import nl.boukenijhuis.dto.CodeContainer;
import nl.boukenijhuis.dto.InputContainer;
import nl.boukenijhuis.dto.PreviousRunContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static nl.boukenijhuis.Utils.addToClassLoader;
import static nl.boukenijhuis.Utils.compileFiles;
import static nl.boukenijhuis.Utils.createTemporaryFile;

// TODO rename project to test-driven-generator?
public class Generator {

    private static final Logger LOG = LogManager.getLogger();

    public static void main(String[] args) {
        try {
            // read the properties
            Properties properties = new Properties();
            properties.load(Generator.class.getResourceAsStream("/test-driven-generation.properties"));

            // start a generator and inject an AI assistant
            new Generator().run(new Llama2(properties), new TestRunner(), args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // TODO implement debug logging

    public void run(AIAssistant assistant, TestRunner testRunner, String[] args) throws IOException {

        // sanitize and provide default inputs
        InputContainer inputContainer = InputContainer.build(args);

        // object for storing test information
        TestRunner.TestInfo testInfo = null;
        int externalAttempts = 0;
        // TODO get from properties, static global properties object?
        int externalMaxRetries = 5;
        Path destinationFilePath = null;
        Path solutionFilePath = null;
        PreviousRunContainer previousRunContainer = new PreviousRunContainer();

        do {
            LOG.info("External attempt: {}", ++externalAttempts);

            // get solution filename and content
            CodeContainer codeContainer = callAssistant(assistant, inputContainer, previousRunContainer);

            // create the solution file in the temp directory
            solutionFilePath = createTemporaryFile(inputContainer, codeContainer);

            // copy the test file to the temp directory
            Path testFileNamePath = inputContainer.getInputFile().getFileName();
            String packageDirectories = codeContainer.getPackageName().replace(".", "/");
            destinationFilePath = inputContainer.getOutputDirectory().resolve(packageDirectories).resolve(testFileNamePath);
            Files.copy(inputContainer.getInputFile(), destinationFilePath, REPLACE_EXISTING);

            // compile the solution file and the test source file
            var compilationContainer = compileFiles(solutionFilePath, destinationFilePath);
            if (!compilationContainer.compilationSuccessful()) {
                // give the error to the AI assistant
                previousRunContainer = createPreviousRunContainer(compilationContainer.errorMessage());
                continue;
            }

            // add all compiled Java files to class loader
            addToClassLoader(inputContainer.getOutputDirectory());

            // run the test
            testInfo = testRunner.runTestFile(inputContainer);
            LOG.info("Tests found: {}, succeeded: {}", testInfo.found(), testInfo.succeeded());

            // if failing tests, provide the error to the AI assistant (and get new content)
            if (solutionNotFound(testInfo)) {
                // give the error to the AI assistant
                previousRunContainer = createPreviousRunContainer(testInfo.errorOutput());
            }

        } while (solutionNotFound(testInfo) && externalAttempts <= externalMaxRetries - 1);

        if (solutionNotFound(testInfo)) {
            LOG.info("No solution found.");
        } else {
            LOG.info("Solution found: {}", solutionFilePath);
        }
    }

    private PreviousRunContainer createPreviousRunContainer(String error) {
        LOG.debug(error);
        return new PreviousRunContainer(error);
    }

    private boolean solutionNotFound(TestRunner.TestInfo testInfo) {
        return testInfo == null || testInfo.succeeded() != testInfo.found();
    }


    private static CodeContainer callAssistant(AIAssistant assistant, InputContainer inputContainer, PreviousRunContainer previousRunContainer) {
        CodeContainer response;
        try {
            response = assistant.call(inputContainer.getInputFile(), previousRunContainer);
//            LOG.debug("CodeContainer: {}", response);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return response;
    }
}
