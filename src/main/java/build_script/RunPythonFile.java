package build_script;


	import java.io.BufferedReader;
	import java.io.IOException;
	import java.io.InputStreamReader;

	public class RunPythonFile {

	    public static void runPythonScript(String pythonFilePath) {
	        // Build the command to execute the Python script
	        String[] command = {
	            "python",    // The command to run the Python interpreter
	            pythonFilePath  // The path to the Python script
	        };
	        
	        System.out.println("Script execution started:  ");
	        ProcessBuilder processBuilder = new ProcessBuilder(command);

	        try {
	            // Start the process to execute the Python script
	            Process process = processBuilder.start();

	            // Get the standard output from the script and print it
	            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
	                String line;
	                while ((line = reader.readLine()) != null) {
	                    System.out.println(line);  // Print the output of the Python script
	                }
	            }

	            // Get the error output (if any) and print it
	            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
	                String line;
	                while ((line = reader.readLine()) != null) {
	                    System.err.println(line);  // Print error output (if any)
	                }
	            }

	            // Wait for the Python process to finish and check the exit code
	            int exitCode = process.waitFor();
	            if (exitCode == 0) {
	                System.out.println("Python script executed successfully.");
	            } else {
	                System.err.println("Python script execution failed with exit code: " + exitCode);
	            }

	        } catch (IOException | InterruptedException e) {
	            System.err.println("Error occurred while executing the Python script: " + e.getMessage());
	            e.printStackTrace();
	        }
	    }

	    public static void main(String[] args) {
	        // Replace with the path to your Python file
	        String pythonFilePath = "C:\\Users\\Invcuser_106\\Desktop\\apk_build\\android\\script_new.py";

	        // Run the Python script
	        runPythonScript(pythonFilePath);
	    }
}
