package devicebin;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Shell;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeviceBinGenerator {

    public static void main(String[] args) {
        // Configuration
        final String host = "192.168.10.2";
        final String username = "kirupakaranp";
        final String password = "K!rk&4dA";
        final int port = 6568;
        
        // Command parameters
        final String svnUrl = "http://192.168.10.2:82/svn/iinvsys_sw/trunk/testing/build_generation_scripts";
        String svnRevision = null;
        /*Enter 0 - To select Main Repository
        Enter 1 - To select Release Repository
        Enter 2 - To select Production Repository
        Enter 3 - To select VidyuRaksha EPS32 Repository*/
        // The option to select (0, 1,2 or 3). Change option based on above release Binary
        final String menuOption = "1"; 

        final String HWOption = "0";

        SSHClient ssh = new SSHClient();

        try {
            // 1. Connect with host key verification (using promiscuous verifier for testing)
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.connect(host, port);
            
            // 2. Authenticate
            ssh.authPassword(username, password);
            
            // 3. Start interactive shell session
            try (Session session = ssh.startSession()) {
                // Allocate pseudo-terminal for interactive commands
                session.allocateDefaultPTY();
                
                try (Shell shell = session.startShell()) {
                    OutputStream stdin = shell.getOutputStream();
                    InputStream stdout = shell.getInputStream();
                    
                    // Execute commands sequence
                    executeCommand(stdin, stdout, "cd devicebin");
                    
                    String svnOutput = executeAndGetOutput(stdin, stdout, 
                            "svn co " + svnUrl, 30000); // 30 second timeout for SVN
                    svnRevision = extractSvnRevisionNumber(svnOutput);
                    if (svnRevision == null) {
                        throw new RuntimeException("Failed to extract SVN revision number");
                    }
                    System.out.println("Extracted SVN revision number: " + svnRevision);
                    
                    executeCommand(stdin, stdout, "cd build_generation_scripts");
                    executeCommand(stdin, stdout, "chmod +x device_bin_generator.sh");
                    
                    // Execute the script and handle interactive prompt
                    executeInteractiveCommand(stdin, stdout, 
                        "./device_bin_generator.sh " + svnRevision +" FTP",
                        "Enter the input to select the device repository path to proceed (0 or 1 or 2):",
                        menuOption);
                    
                    sendResponse(stdin, "1");
                    Thread.sleep(2000);
                    
                    waitForPrompt(stdout,"Enter the product id to proceed (0,1,2,3,4,5):");
                    sendResponse(stdin, "1");
                    
                    waitForPrompt(stdout,"Enter the OTA Selection to proceed (0,1):");
                    Thread.sleep(2000);
                    
                    sendResponse(stdin, "1");
                    
                    Thread.sleep(2000);
                    
                    waitForPrompt(stdout,"Enter the HW Type Selection to proceed (0,1,2):");
                    sendResponse(stdin, "0");
                   
                    Thread.sleep(120000);

                    readOutput(stdout, 5000); // Wait up to 5 seconds for final output
                    
                }
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error during SSH operation: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                ssh.disconnect();
            } catch (IOException e) {
                System.err.println("Error during disconnect: " + e.getMessage());
            }
        }
    }

    private static void executeCommand(OutputStream stdin, InputStream stdout, String command) 
            throws IOException, InterruptedException {
        System.out.println("\n>>> Executing: " + command);
        stdin.write((command + "\n").getBytes());
        stdin.flush();
        readOutput(stdout, 2000); // Wait 2 seconds for output
    }

    private static void executeInteractiveCommand(OutputStream stdin, InputStream stdout, 
            String command, String prompt, String response) 
            throws IOException, InterruptedException {
        System.out.println("\n>>> Executing interactive: " + command);
        stdin.write((command + "\n").getBytes());
        stdin.flush();
        
        // Wait for prompt and respond
        waitForPrompt(stdout, prompt);
        stdin.write((response + "\n").getBytes());
        stdin.flush();
        readOutput(stdout, 2000); // Wait 2 seconds after response
    }
    
    private static String readOutput(InputStream stdout, long timeoutMillis) 
            throws IOException, InterruptedException {
        StringBuilder output = new StringBuilder();
        byte[] buffer = new byte[1024];
        long endTime = System.currentTimeMillis() + timeoutMillis;
        
        while (System.currentTimeMillis() < endTime) {
            while (stdout.available() > 0) {
                int len = stdout.read(buffer);
                String text = new String(buffer, 0, len);
                System.out.print(text);
                output.append(text);
            }
            Thread.sleep(100);
        }
        return output.toString();
    }

	/*
	 * private static void readOutput(InputStream stdout, long timeoutMillis) throws
	 * IOException, InterruptedException { byte[] buffer = new byte[1024]; long
	 * endTime = System.currentTimeMillis() + timeoutMillis;
	 * 
	 * while (System.currentTimeMillis() < endTime) { while (stdout.available() > 0)
	 * { int len = stdout.read(buffer); System.out.print(new String(buffer, 0,
	 * len)); } Thread.sleep(100); } }
	 */

    private static void waitForPrompt(InputStream stdout, String prompt) 
            throws IOException, InterruptedException {
        StringBuilder outputBuffer = new StringBuilder();
        byte[] buffer = new byte[1024];
        
        while (true) {
            while (stdout.available() > 0) {
                int len = stdout.read(buffer);
                String output = new String(buffer, 0, len);
                System.out.print(output);
                outputBuffer.append(output);
                
                if (outputBuffer.toString().contains(prompt)) {
                    return;
                }
            }
            Thread.sleep(100);
        }
    }
    
    private static String executeAndGetOutput(OutputStream stdin, InputStream stdout, 
            String command, long timeoutMillis) 
            throws IOException, InterruptedException {
        System.out.println("\n>>> Executing: " + command);
        stdin.write((command + "\n").getBytes());
        stdin.flush();
        return readOutput(stdout, timeoutMillis);
    }
    
    private static String extractSvnRevisionNumber(String output) {
        // Pattern to match "Checked out revision 16936"
        Pattern pattern = Pattern.compile("Checked out revision (\\d+)");
        Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    private static void waitAndRespond(OutputStream stdin, InputStream stdout, 
            String promptPattern, String response) 
            throws IOException, InterruptedException {
        StringBuilder outputBuffer = new StringBuilder();
        byte[] buffer = new byte[1024];
        Pattern pattern = Pattern.compile(promptPattern);
        
        System.out.println("Waiting for prompt: " + promptPattern);
        
        while (true) {
            while (stdout.available() > 0) {
                int len = stdout.read(buffer);
                String output = new String(buffer, 0, len);
                System.out.print(output); // Show real-time output
                outputBuffer.append(output);
                
                Matcher matcher = pattern.matcher(outputBuffer.toString());
                if (matcher.find()) {
                    System.out.println("Responding with: " + response);
                    stdin.write((response + "\n").getBytes());
                    stdin.flush();
                    return;
                }
            }
            Thread.sleep(100);
        }
    }
    
    private static void sendResponse(OutputStream stdin, String response) throws IOException {
        System.out.println("\nSending response: " + response);
        stdin.write((response + "\n").getBytes());
        stdin.flush();
        // Small delay to ensure the response is processed
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}