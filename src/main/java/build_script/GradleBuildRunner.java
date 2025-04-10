package build_script;
import java.io.*;
import java.time.LocalDateTime;
import java.util.Map;

public class GradleBuildRunner {

    public boolean runGradleBuild(String projectPath, String buildVariant) {

        ProcessBuilder processBuilder = new ProcessBuilder();

        // Set the directory to where gradlew.bat is located (the android subdirectory)
        File gradleDir = new File(projectPath, "android");  // path to the 'android' subdirectory
        processBuilder.directory(gradleDir);  // Set the working directory for gradle commands

        System.out.println("Working directory: " + gradleDir.getAbsolutePath());  // Print working directory
        
        String javaHome = "C:\\Program Files\\Java\\jdk-17";  // Modify this to your JDK path
        Map<String, String> environment = processBuilder.environment();
        environment.put("JAVA_HOME", javaHome); // Set the correct JDK path
        environment.put("ANDROID_HOME", "C:\\Users\\Invcuser_106\\AppData\\Local\\Android\\Sdk");
        environment.put("GRADLE_HOME", "C:\\Gradle");
        //environment.put("PATH", environment.get("PATH") + "C:\\Users\\Invcuser_106\\AppData\\Local\\Android\\Sdk\\platform-tools");

        // Print JAVA_HOME to debug if it's being set correctly
        System.out.println("Using JAVA_HOME: " + javaHome);

        // Ensure NPM dependencies are installed
        if (!installNpmDependencies(projectPath)) {
            return false;  // Exit if NPM install fails
        }

        // Path to gradlew.bat inside the android directory
        File gradlewBat = new File(gradleDir, "gradlew.bat");
        if (!gradlewBat.exists()) {
            System.err.println("gradlew.bat not found in directory: " + gradleDir.getAbsolutePath());
            return false;
        }

        // Clean the previous build
        if (!runGradleCommand(processBuilder, gradleDir, "clean")) {
            return false;
        }

        // Build the APK (use assembleRelease or assembleDebug)
        System.out.println("buildVariant: " + buildVariant);
        return runGradleCommand(processBuilder, gradleDir, "assemble" + buildVariant);
    }

    private boolean installNpmDependencies(String projectPath) {
        // Prepare the npm install command (to install react-native-geolocation-service and other dependencies)
        ProcessBuilder npmProcessBuilder = new ProcessBuilder();
        npmProcessBuilder.directory(new File(projectPath));  // Ensure we run npm install in the root of the project

        // Command to install the package with legacy-peer-deps flag
       // npmProcessBuilder.command("npm", "install", "react-native-geolocation-service", "--legacy-peer-deps");
        npmProcessBuilder.command("C:\\Program Files\\nodejs\\npm.cmd", "install", "--legacy-peer-deps");

        try {
        	
            // Start the npm install process
        	
            Process npmProcess = npmProcessBuilder.start();
            
            // Capture output and error streams for debugging
            printStream(npmProcess.getInputStream(), "NPM OUTPUT");
            printStream(npmProcess.getErrorStream(), "NPM ERROR");

            // Wait for the process to finish
            int exitCode = npmProcess.waitFor();

            // Check if npm install was successful
            if (exitCode != 0) {
                System.out.println("NPM install failed at: " + LocalDateTime.now());
                return false;
            }
            System.out.println("NPM install succeeded at: " + LocalDateTime.now());
            return true;

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.out.println("Error during NPM install at: " + LocalDateTime.now());
            return false;
        }
    }

    private boolean runGradleCommand(ProcessBuilder processBuilder, File gradleDir, String command) {

        // Prepare the gradle command
        String gradleCommand = gradleDir.getAbsolutePath() + File.separator + "gradlew.bat";
        processBuilder.command(gradleCommand, command, "--no-daemon", "--info");

        try {
            Process process = processBuilder.start();

            // Capture output and error streams for debugging
            printStream(process.getInputStream(), "OUTPUT");
            printStream(process.getErrorStream(), "ERROR");

            // Wait for the process to finish
            int exitCode = process.waitFor();

            // Check if the command was successful
            if (exitCode != 0) {
                System.out.println("Gradle command failed: " + command + " at: " + LocalDateTime.now());
                return false;
            }
            System.out.println("Gradle command succeeded: " + command + " at: " + LocalDateTime.now());
            return true;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.out.println("Error during Gradle command: " + command + " at: " + LocalDateTime.now());
            return false;
        }
    }

    private void printStream(InputStream inputStream, String streamType) throws IOException {
    	
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(streamType + ": " + line);  // Print in real-time
        }
    }

    public static void main(String[] args) {

        GradleBuildRunner buildRunner = new GradleBuildRunner();
        String projectPath = "C:\\Users\\Invcuser_106\\Desktop\\apk_build"; // Replace with actual project path
        String buildVariant = "Release"; //"Release"; Or "Debug;"
        
        boolean success = buildRunner.runGradleBuild(projectPath, buildVariant);
        if (success) {
            System.out.println("Build completed successfully.");
        } else {
            System.out.println("Build failed");
        }
    }
}
