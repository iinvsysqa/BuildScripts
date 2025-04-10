package devicebin;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.nio.file.*;

public class AndroidBuildAutomation {

    // Configuration
    private static final String SVN_USERNAME = "kirupakaranp";
    private static final String SVN_PASSWORD = "K!rk&4dA";
    private static final String MAIN_PROJECT_URL = "http://192.168.10.2:82/svn/iinvsys_sw/staging/Apps/szephyr_dev_team_staging";
    private static final String CONFIG_MODULE_URL = "http://192.168.10.2:82/svn/iinvsys_sw/staging/Application_Configuration_Modules";
    private static final String PROJECT_ROOT = Paths.get("C:", "Users", "Invcuser_106", "Desktop", "apkbuild").toString();
    private static final String CONFIG_MODULE_PATH = Paths.get(PROJECT_ROOT, "Application_Configuration_Modules").toString();
    private static final String ANDROID_DIR = "android";
    private static final String GRADLEW_SCRIPT = System.getProperty("os.name").toLowerCase().contains("win") 
            ? "gradlew.bat" : "./gradlew";
    private static final String APK_OUTPUT_PATH = PROJECT_ROOT + File.separator + ANDROID_DIR + 
    	    File.separator + "app" + File.separator + "build" + File.separator + "outputs" + 
    	    File.separator + "apk" + File.separator + "release";
    private static final String DESTINATION_PATH = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "App";
    	private static final String BUILD_GRADLE_PATH = PROJECT_ROOT + File.separator + ANDROID_DIR + 
    	    File.separator + "app" + File.separator + "build.gradle";

    public static void main(String[] args) {
        try {
            // 1. Checkout main project and config module
            svnCheckout(MAIN_PROJECT_URL, PROJECT_ROOT, SVN_USERNAME, SVN_PASSWORD);
            svnCheckout(CONFIG_MODULE_URL, CONFIG_MODULE_PATH, SVN_USERNAME, SVN_PASSWORD);
            
            // 2. Get SVN revision
            String revision = getSvnRevision();
            System.out.println("Using SVN revision: " + revision);
            
            // 3. Update versions
            updateVersionWithRevision();
           // updatePackageJsonVersion(revision);
            // 4. Install Node Modules
            installNodeModules();
            
            // 5. Run Gradle Clean and Release Build
            runGradleCleanAndRelease();
            
            // 6. Copy APK to destination
            copyApkToDestination();
            
            System.out.println("Build completed successfully!");
        } catch (Exception e) {
            System.err.println("Build failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void copyApkToDestination() throws IOException, InterruptedException {
        File apkDir = new File(APK_OUTPUT_PATH);
        File destDir = new File(DESTINATION_PATH);
        
        // Create destination directory if it doesn't exist
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        
        // Get SVN revision number
        String revision = getSvnRevision();
        
        // Find all APK files in output directory
        File[] apkFiles = apkDir.listFiles((dir, name) -> name.endsWith(".apk"));
        if (apkFiles == null || apkFiles.length == 0) {
            throw new RuntimeException("No APK files found in: " + APK_OUTPUT_PATH);
        }
        
        // Copy each APK file with revision number in filename
        for (File apk : apkFiles) {
            String originalName = apk.getName();
            
            // Create new filename with revision (e.g., app-release-16789.apk)
            String newName = originalName.replace(".apk", "-" + revision + ".apk");
            
            File dest = new File(destDir, newName);
            
            // Delete existing file if present (replace it)
            if (dest.exists()) {
                Files.delete(dest.toPath());
            }
            
            // Copy the file
            Files.copy(apk.toPath(), dest.toPath());
            System.out.println("Copied APK to: " + dest.getAbsolutePath());
            
            // Optional: Keep original filename too
            File destOriginal = new File(destDir, originalName);
            Files.copy(apk.toPath(), destOriginal.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String getSvnRevision() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("svn", "info", "--show-item", "revision", PROJECT_ROOT);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String revision = reader.readLine();
            if (revision == null || revision.trim().isEmpty()) {
                throw new RuntimeException("Failed to get SVN revision");
            }
            process.waitFor();
            return revision.trim();
        }
    }

    private static void svnCheckout(String url, String localPath, String username, String password) 
            throws IOException, InterruptedException {
        File dir = new File(localPath);
        
        if (dir.exists()) {
            System.out.println("Updating: " + localPath);
            executeCommand("svn", "update", "--username", username, "--password", password, 
                         "--non-interactive", "--trust-server-cert", localPath);
        } else {
            System.out.println("Checking out: " + url + " to " + localPath);
            executeCommand("svn", "checkout", "--username", username, "--password", password,
                         "--non-interactive", "--trust-server-cert", url, localPath);
        }
    }

    private static String findNpmPath() {
        // Try common npm locations on Windows
        String[] possiblePaths = {
            System.getenv("ProgramFiles") + "\\nodejs\\npm.cmd",
            System.getenv("ProgramFiles(x86)") + "\\nodejs\\npm.cmd",
            System.getenv("APPDATA") + "\\npm\\npm.cmd",
            "npm" // Fallback to PATH
        };
        
        for (String path : possiblePaths) {
            if (new File(path).exists()) {
                return path;
            }
        }
        return "npm"; // Final fallback
    }

    
    private static void installNodeModules() throws IOException, InterruptedException {
        File nodeModulesDir = new File(PROJECT_ROOT, "node_modules");
        String npmPath = findNpmPath();
        
        if (!nodeModulesDir.exists()) {
            System.out.println("Installing node modules with legacy peer deps...");
            try {
                // First try with --legacy-peer-deps
                executeCommandInDirectory(PROJECT_ROOT, npmPath, "install", "--legacy-peer-deps");
            } catch (Exception e) {
                System.err.println("First attempt failed, trying with --force...");
                // If that fails, try with --force
                executeCommandInDirectory(PROJECT_ROOT, npmPath, "install", "--force");
            }
        } else {
            System.out.println("Node modules already exist, skipping installation");
        }
    }
	
	  
	  private static void executeCommand(String... command) throws IOException,
	  InterruptedException { executeCommandInDirectory(null, command); }
	 
    
    private static void runGradleCleanAndRelease() throws IOException, InterruptedException {
        String androidPath = PROJECT_ROOT + File.separator + ANDROID_DIR;
        File androidDir = new File(androidPath);
        
        // Validate Android directory
        if (!androidDir.exists()) {
            throw new RuntimeException("Android directory missing: " + androidPath);
        }
        
        // Resolve gradle command
        String gradleCommand = resolveGradleCommand(androidPath);
        
        // Run build process
        System.out.println("Running Gradle clean...");
        executeCommandInDirectory(androidPath, gradleCommand, "clean");
        
        System.out.println("Running Gradle assembleRelease...");
        executeCommandInDirectory(androidPath, gradleCommand, "assembleRelease");
    }

    private static String resolveGradleCommand(String androidPath) {
        // Check for gradle wrapper
        File gradlewBat = new File(androidPath, "gradlew.bat");
        File gradlew = new File(androidPath, "gradlew");
        
        if (gradlewBat.exists()) {
            return gradlewBat.getAbsolutePath();
        }
        if (gradlew.exists()) {
            gradlew.setExecutable(true);
            return gradlew.getAbsolutePath();
        }
        
        // Fallback to system gradle
        System.out.println("Gradle wrapper not found, using system gradle");
        return "gradle";
    }

    private static void executeCommandInDirectory(String directory, String... command) 
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        if (directory != null) {
            pb.directory(new File(directory));
        }
        pb.redirectErrorStream(true);
        
        System.out.println("Executing: " + String.join(" ", command));
        Process process = pb.start();
        
        // Print command output
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed with exit code " + exitCode);
        }
    }
    
    private static void updatePackageJsonVersion(String revision) throws IOException {
        File packageJson = new File(PROJECT_ROOT, "package.json");
        String content = new String(Files.readAllBytes(packageJson.toPath()));
        
        // Update version in package.json
        String updatedContent = content.replaceAll(
            "\"version\":\\s*\"[0-9.]+\"", 
            "\"version\": \"" + revision + "\"");
        
        Files.write(packageJson.toPath(), updatedContent.getBytes());
    }
    
    private static void updateVersionWithRevision() throws IOException, InterruptedException {
        String revision = getSvnRevision();
        File buildGradle = new File(BUILD_GRADLE_PATH);
        
        if (!buildGradle.exists()) {
            throw new RuntimeException("build.gradle not found at: " + BUILD_GRADLE_PATH);
        }
        
        // Read build.gradle content
        String content = new String(Files.readAllBytes(buildGradle.toPath()));
        
        // Update versionName - replaces entire version with just the revision number
        String updatedContent = content.replaceAll(
            "versionName\\s+\"[0-9.]+\"", 
            "versionName \"" + revision + "\"");
        
        // Also update versionCode if needed (recommended)
        updatedContent = updatedContent.replaceAll(
            "versionCode\\s+[0-9]+", 
            "versionCode " + revision);
        
        // Write back to file
        Files.write(buildGradle.toPath(), updatedContent.getBytes());
        System.out.println("Updated version to SVN revision: " + revision);
    }
}