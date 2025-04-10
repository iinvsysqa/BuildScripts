package devicebin;
import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.Optional;

public class UnzipBin {

	public static void main(String[] args) {
        String sourceDirectory = "C:\\Users\\Invcuser_106\\Desktop\\Device_Bin";  // Directory containing the zip file
        String destDirectory = "C:\\Users\\Invcuser_106\\Desktop\\Device_Bin\\Release_bin";    // Where to put the bin folder
        
        try {
            // Step 1: Find any zip file in the source directory
            Optional<Path> zipFilePath = findFirstZipFile(sourceDirectory);
            
            if (zipFilePath.isPresent()) {
                System.out.println("Found zip file: " + zipFilePath.get());
                
                // Step 2: Create temp extraction directory
                Path tempExtractDir = Paths.get(destDirectory, "temp_extracted_" + System.currentTimeMillis());
                Files.createDirectories(tempExtractDir);
                
                // Step 3: Unzip the file
                unzip(zipFilePath.get().toString(), tempExtractDir.toString());
                
                // Step 4: Find and move the bin folder
                Path binFolderPath = findBinFolder(tempExtractDir.toString());
                
                if (binFolderPath != null) {
                    Path destinationPath = Paths.get(destDirectory, "bin");
                    Files.move(binFolderPath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Successfully moved bin folder to: " + destinationPath);
                } else {
                    System.err.println("No bin folder found in the extracted archive.");
                }
                
                // Step 5: Clean up
                deleteDirectory(tempExtractDir);
                System.out.println("Cleaned up temporary files.");
            } else {
                System.err.println("No zip file found in directory: " + sourceDirectory);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static Optional<Path> findFirstZipFile(String directory) throws IOException {
        return Files.list(Paths.get(directory))
                .filter(path -> path.toString().toLowerCase().endsWith(".zip"))
                .findFirst();
    }
    
    private static void unzip(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipIn.getNextEntry();
            
            while (entry != null) {
                String filePath = destDirectory + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    extractFile(zipIn, filePath);
                } else {
                    File dir = new File(filePath);
                    dir.mkdir();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }
    
    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        // Create parent directories if they don't exist
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] bytesIn = new byte[4096];
            int read;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }
    
    private static Path findBinFolder(String directory) throws IOException {
        return Files.walk(Paths.get(directory))
                .filter(path -> path.getFileName().toString().equals("bin") && Files.isDirectory(path))
                .findFirst()
                .orElse(null);
    }
    
    private static void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                .sorted((a, b) -> b.compareTo(a)) // reverse order to delete contents first
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        System.err.println("Failed to delete: " + p);
                    }
                });
        }
    }
}