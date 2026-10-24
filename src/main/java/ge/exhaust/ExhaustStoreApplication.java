package ge.exhaust;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExhaustStoreApplication {
    public static void main(String[] args) {
        // Make a fresh clone runnable without any database setup.
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Path.of("data"));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not create the data directory", e);
        }
        // Windows JDK socket pipes can fail when TEMP contains a DOS short-name
        // alias. Use the existing project data directory for this JVM's sockets.
        if (System.getProperty("os.name", "").startsWith("Windows")
                && System.getProperty("jdk.net.unixdomain.tmpdir") == null) {
            System.setProperty("jdk.net.unixdomain.tmpdir", "data");
        }
        SpringApplication.run(ExhaustStoreApplication.class, args);
    }
}
