package eggfly.kvm.converter;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class ZipUtils {
    public static void replaceSingleFileIntoZip(String zip, String file) throws IOException {
        Path myFilePath = Paths.get(file);
        Path zipFilePath = Paths.get(zip);
        String name = new File(file).getName();
        try (FileSystem fs = FileSystems.newFileSystem(zipFilePath, null)) {
            Path fileInsideZipPath = fs.getPath("/" + name);
            Files.copy(myFilePath, fileInsideZipPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void removeSignature(String zip) throws IOException {
        Path zipFilePath = Paths.get(zip);
        try (FileSystem fs = FileSystems.newFileSystem(zipFilePath, null)) {
            Path cert = fs.getPath("/META-INF/CERT.SF");
            Files.deleteIfExists(cert);
            Path rsa = fs.getPath("/META-INF/CERT.RSA");
            Files.deleteIfExists(rsa);
        }
    }
}
