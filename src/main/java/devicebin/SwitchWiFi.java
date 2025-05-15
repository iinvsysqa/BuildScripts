package devicebin;
import java.io.*;

public class SwitchWiFi {
    public static void main(String[] args) {
      //  String ssid = "TP-Link_6D38_With_Internet";         // Replace with your Wi-Fi name
       // String password = "1234512345"; // Replace with your Wi-Fi password

        String ssid = "iinvsys_Private_limited";         // Replace with your Wi-Fi name
        String password = "76364228";
        try {
            // Step 1: Create Wi-Fi profile XML
            String profileXml = generateWifiProfile(ssid, password);
            File profileFile = new File("wifi-profile.xml");
            try (FileWriter writer = new FileWriter(profileFile)) {
                writer.write(profileXml);
            }

            // Step 2: Add the Wi-Fi profile
            runCommand("netsh wlan add profile filename=\"wifi-profile.xml\"");

            // Step 3: Connect to the network
            runCommand("netsh wlan connect name=\"" + ssid + "\" ssid=\"" + ssid + "\"");
            
            Thread.sleep(3000);
            System.out.println("Connection command sent. Check system Wi-Fi status.");

            // Optional: Clean up
            profileFile.deleteOnExit();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper to run system command
    private static void runCommand(String command) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(command);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        process.waitFor();
    }

    // Helper to generate Wi-Fi profile XML
    private static String generateWifiProfile(String ssid, String password) {
        return "<?xml version=\"1.0\"?>\n" +
                "<WLANProfile xmlns=\"http://www.microsoft.com/networking/WLAN/profile/v1\">\n" +
                "    <name>" + ssid + "</name>\n" +
                "    <SSIDConfig>\n" +
                "        <SSID>\n" +
                "            <name>" + ssid + "</name>\n" +
                "        </SSID>\n" +
                "    </SSIDConfig>\n" +
                "    <connectionType>ESS</connectionType>\n" +
                "    <connectionMode>auto</connectionMode>\n" +
                "    <MSM>\n" +
                "        <security>\n" +
                "            <authEncryption>\n" +
                "                <authentication>WPA2PSK</authentication>\n" +
                "                <encryption>AES</encryption>\n" +
                "                <useOneX>false</useOneX>\n" +
                "            </authEncryption>\n" +
                "            <sharedKey>\n" +
                "                <keyType>passPhrase</keyType>\n" +
                "                <protected>false</protected>\n" +
                "                <keyMaterial>" + password + "</keyMaterial>\n" +
                "            </sharedKey>\n" +
                "        </security>\n" +
                "    </MSM>\n" +
                "</WLANProfile>";
    }
}
