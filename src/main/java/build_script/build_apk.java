package build_script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;

public class build_apk {

    // Method to run a command and capture output
    private static void runCommand(String command[], String workingDirectory) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(workingDirectory));  // Set the working directory

        try {
            Process process = processBuilder.start();  // Start the process
            int exitCode = process.waitFor();  // Wait for the process to finish
            
            // Capture and print the standard output of the command
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                	System.out.println(process.getInputStream() + ": " + line);  // Print the output
                }
            }
            
            // Capture and print the error output (if any)
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                
                while ((line = reader.readLine()) != null) {
                    System.out.println(process.getErrorStream()+ ": " + line);  // Print in real-time
                }
            }

            // Check if the command was successful
            if (exitCode != 0) {
                System.err.println("Command failed with exit code: " + exitCode);
            } else {
                System.out.println("Command executed successfully.");
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String gradleProjectPath = "C:\\Users\\Invcuser_106\\Desktop\\apk_build\\android";  // Replace with your actual project path
        String gradleWrapperPath = gradleProjectPath + "\\gradlew.bat";

        // Check if gradlew.bat exists before proceeding
        File gradleWrapper = new File(gradleWrapperPath);
        if (!gradleWrapper.exists()) {
            System.err.println("Error: gradlew.bat not found in the project directory.");
            System.err.println("Please ensure that the Gradle wrapper is correctly set up.");
            return;  // Exit the program as we cannot proceed without gradlew.bat
        }

        // Clean the project using gradle.bat clean
        System.out.println("Running gradle clean...");
        runCommand(new String[]{"cmd", "/c", "Java ", "-version"}, gradleProjectPath);
        
        System.out.println("Running gradle clean...");
        runCommand(new String[]{"cmd", "/c", "gradlew", "clean"}, gradleProjectPath);

        // Assemble the release version of the project using gradle assembleRelease
        System.out.println("Running gradle assembleRelease...");
        runCommand(new String[]{"cmd", "/c", "gradlew", "assembleRelease"}, gradleProjectPath);

    }
}
