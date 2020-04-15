package eggfly.kvm.converter;

import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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


    /**
     * Size of the buffer to read/write data
     */
    private static final int BUFFER_SIZE = 4096;

    /**
     *  * Extracts a zip file specified by the zipFilePath to a directory specified by
     *  * destDirectory (will be created if does not exists)
     *  * @param zipFilePath
     *  * @param destDirectory
     *  * @throws IOException
     *  
     */
    public static void unzip(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                File dir = new File(destDirectory, entry.getName()).getParentFile();
                dir.mkdirs();
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdirs();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }

    /**
     *  * Extracts a zip entry (file entry)
     *  * @param zipIn
     *  * @param filePath
     *  * @throws IOException
     *  
     */
    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
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
