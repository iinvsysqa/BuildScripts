package devicebin;

import java.io.*;
import java.util.concurrent.*;

public class RunFlashScript {
    public static void main(String[] args) {
        String desktopPath = System.getProperty("user.home") + "\\Desktop\\Flash script";
        String pythonScriptPath = desktopPath + "\\Flash_Script.py";

        try {
        	
        	String productId = "1";
            // Step 1: Start the Python process
            ProcessBuilder pb = new ProcessBuilder("python", "-u", pythonScriptPath);
            pb.directory(new File(desktopPath));
            Process process = pb.start();

            // Step 2: Get input/output streams
            OutputStream stdin = process.getOutputStream();
            InputStream stdout = process.getInputStream();
            InputStream stderr = process.getErrorStream();

            // Step 3: Define expected prompt-response pairs
            //Product ID 1 - Auto Flashing for SmaZer product
            //Product ID 2 - Auto Flashing for SmAmp_Max product
            //Product ID 3 - Auto Flashing for SZephyr product
            

            String[][] promptResponses = {
                {"Select the product ID", productId + "\n"},
                {"Select the Board Version", "5\n"},
                {"Do you want flash OTA BIN?", "N\n"},
                {"Select the Server Type", "2\n"},
                {"Select the flash type", "1\n"},
                {"Enter your last two digits", "KA\n"},
                {"Flash / Erase Selection", "1\n"},
                {"Enter Serial Port", "COM5\n"}
            };

            // Step 4: Thread to handle live output reading and prompt detection
            new Thread(() -> {
                try (
                    InputStreamReader reader = new InputStreamReader(stdout);
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin))
                ) {
                    char[] buffer = new char[1024];
                    int numRead;
                    StringBuilder accumulated = new StringBuilder();
                    int step = 0;

                    while ((numRead = reader.read(buffer)) != -1) {
                        String chunk = new String(buffer, 0, numRead);
                        System.out.print(chunk);  // Show Python output live
                        accumulated.append(chunk);

                        // Check for prompt match
                        if (step < promptResponses.length &&
                                accumulated.toString().contains(promptResponses[step][0])) {
                        	Thread.sleep(1000);
                            String response = promptResponses[step][1];
                            writer.write(response);
                            writer.flush();
                            System.out.println("\nSent input: " + response.trim());
                            step++;

                            // Reset buffer after matching to avoid duplicate matches
                            accumulated.setLength(0);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }).start();

            // Step 5: Thread to handle Python stderr (optional, but recommended)
            new Thread(() -> {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(stderr))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        System.err.println("ERROR: " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            // Step 6: Wait for process to complete (increase timeout if needed)
            boolean finished = process.waitFor(90, TimeUnit.SECONDS);
            System.out.println("Script finished: " + finished);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
