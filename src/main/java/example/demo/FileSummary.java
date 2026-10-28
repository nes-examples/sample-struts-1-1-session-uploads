package example.demo;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.struts.upload.FormFile;

/** What the pages show about a file: its name, type, size, SHA-256 and the class holding it. */
public class FileSummary {

    private final String fileName;
    private final String contentType;
    private final int size;
    private final String sha256;
    private final String heldAs;

    public FileSummary(FormFile file) throws IOException {
        this.fileName = file.getFileName();
        this.contentType = file.getContentType();
        byte[] data = file.getFileData();
        this.size = data.length;
        this.sha256 = sha256(data);
        this.heldAs = file.getClass().getName();
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public int getSize() {
        return size;
    }

    public String getSha256() {
        return sha256;
    }

    public String getHeldAs() {
        return heldAs;
    }

    private static String sha256(byte[] data) {
        try {
            StringBuilder hex = new StringBuilder();
            for (byte b : MessageDigest.getInstance("SHA-256").digest(data)) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
