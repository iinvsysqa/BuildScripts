package build_script;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.wc.*;
import java.io.*;
import java.time.LocalDateTime;
import java.util.Map;

public class ApkBuilder {

    public static void main(String[] args) {
        String svnRepoURL = "http://192.168.10.2:82/svn/iinvsys_sw/staging/Apps/szephyr_dev_team_staging"; // SVN repository URL
        String svnRepoURLCommonFile = "http://192.168.10.2:82/svn/iinvsys_sw/staging/Application_Configuration_Modules";	
        String svnUser = "kirupakaranp"; // SVN username
        String svnPassword = "K!rk&4dA"; // SVN password
        String localProjectPath = "C://Users/Invcuser_106/Desktop/apk_build"; // Path to your local Android project
        String localProjectPathCommon = "C://Users/Invcuser_106/Desktop/apk_build/Application_Configuration_Modules";
        String buildVariant = "Release";                             // Change to "debug" if needed
        System.out.println("Build started at: " + LocalDateTime.now());

        // Checkout or update files from SVN
        if (checkoutFromSVN(svnRepoURL, svnUser, svnPassword, localProjectPath)&& checkoutFromSVN(svnRepoURLCommonFile, svnUser, svnPassword, localProjectPathCommon)) {
            System.out.println("Successfully checked out files from SVN at: " + LocalDateTime.now());

            // Build the APK using Gradle commands
            if (buildApk(localProjectPath, buildVariant)) {
                System.out.println("APK built successfully at: " + LocalDateTime.now());
            } else {
                System.out.println("Failed to build the APK at: " + LocalDateTime.now());
            }
        } else {
            System.out.println("Failed to checkout files from SVN at: " + LocalDateTime.now());
        }

        System.out.println("Build process ended at: " + LocalDateTime.now() + "\n");
    }

    // Function to checkout or update files from the SVN repository
    private static boolean checkoutFromSVN(String svnRepoURL, String svnUser, String svnPassword, String localPath) {
        boolean success = false;
        System.out.println("SVN Checkout is in Progress.......");

        try {
            // Initialize SVN repository URL and authentication manager
            SVNURL repositoryURL = SVNURL.parseURIEncoded(svnRepoURL);
            SVNClientManager clientManager = SVNClientManager.newInstance();
            clientManager.setAuthenticationManager(new BasicAuthenticationManager(svnUser, svnPassword));

            // Checkout the repository or update if already present
            File workingCopy = new File(localPath);
            if (!workingCopy.exists()) {
                workingCopy.mkdirs();
            }

            // Checkout using the updated SVNKit 1.9+ API
            
            SVNUpdateClient updateClient = clientManager.getUpdateClient();
            SVNRevision revision = SVNRevision.HEAD;  // Checkout the latest revision
            long revisionNumber = updateClient.doCheckout(repositoryURL, workingCopy, revision, revision, true);

            System.out.println("Checked out revision: " + revisionNumber);
            success = true;
        } catch (SVNException e) {
            e.printStackTrace();
        }

        return success;
    }
  
    // Function to build APK using Gradle
    private static boolean buildApk(String projectPath, String buildVariant) {
    	
        ProcessBuilder processBuilder = new ProcessBuilder();

        // Set the directory to where gradlew.bat is located (the android subdirectory)
        File gradleDir = new File(projectPath, "android");  // path to the 'android' subdirectory
        processBuilder.directory(gradleDir);  // Set the working directory for gradle commands
        
        System.out.println("Working directory: " + gradleDir.getAbsolutePath());  // Print working directory

        // Path to gradlew.bat inside the android directory
        String gradleCommand = gradleDir.getAbsolutePath() + "\\gradlew.bat";
        String cleanCommand = "clean";         // Clean the previous build

        // Check if gradlew.bat exists in the directory
        File gradlewBat = new File(gradleDir, "gradlew.bat");
        if (!gradlewBat.exists()) {
            System.err.println("gradlew.bat not found in directory: " + gradleDir.getAbsolutePath());
            return false;
        }

        System.out.println("Gradle command: " + gradleCommand);  // Print gradle command
        
        try {
        	  processBuilder.command("C:\\Users\\Invcuser_106\\Desktop\\apk_build\\android\\gradlew.bat", cleanCommand);
              Process process = processBuilder.start();
              int exitCode = process.waitFor();

              if (exitCode != 0) {
                  System.out.println("Gradle clean failed at: " + LocalDateTime.now());
                  return false;
              }
        	
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.out.println("Error during Clean Gradle Clean at: " + LocalDateTime.now());
            return false;
        }
        // Clean the previous build
      

        try {
           
            System.out.println("buildVariant: "+ buildVariant);

            // Build the APK (use assembleRelease or assembleDebug)
            processBuilder.command(gradleCommand, "assemble" + buildVariant);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("APK build completed successfully at: " + LocalDateTime.now());
                return true;
            } else {
                System.out.println("Gradle build failed at: " + LocalDateTime.now());
                return false;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.out.println("Error during APK build at: " + LocalDateTime.now());
            return false;
        }
    }

}
