package eggfly.kvm.converter;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class EncryptFile {
    private static final String KEY = "my_secret_key";

    public static void main(String[] args) {
        String key = KEY;
        if (args.length < 1) {
            System.out.println("Usage: java EncryptFile <file name>");
            System.exit(-1);
        }
        try {
            File aesFile = new File("encrypted.enc");
            FileInputStream fis;
            FileOutputStream fos;
            CipherInputStream cis;
            // Creation of Secret key
            int length = key.length();
            if (length > 16 && length != 16) {
                key = key.substring(0, 15);
            }
            if (length < 16 && length != 16) {
                for (int i = 0; i < 16 - length; i++) {
                    key = key + "0";
                }
            }
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
            //Creation of Cipher objects
            Cipher encrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] aByte = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            IvParameterSpec ivSpec = new IvParameterSpec(aByte);

            encrypt.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            try {
                fis = new FileInputStream(args[0]);
                cis = new CipherInputStream(fis, encrypt);
                // Write to the Encrypted file
                fos = new FileOutputStream(aesFile);
                byte[] b = new byte[8];
                int i = cis.read(b);
                while (i != -1) {
                    fos.write(b, 0, i);
                    i = cis.read(b);
                }
                fos.flush();
                fos.close();
                cis.close();
                fis.close();
            } catch (IOException err) {
                System.out.println("Cannot open file!");
                System.exit(-1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
