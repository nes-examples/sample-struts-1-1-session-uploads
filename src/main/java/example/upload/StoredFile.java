package example.upload;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.apache.struts.upload.FormFile;

/**
 * An uploaded file held in memory, so that a session-scoped form can keep it between requests.
 *
 * <p>The {@code FormFile} that Struts puts on a form wraps a Commons FileUpload item. Its temporary
 * file only lasts for the request, and the object can't be serialized, so it can't travel with a
 * session to another cluster node. This class copies the name, content type and bytes instead.
 */
public class StoredFile implements FormFile, Serializable {

    private static final long serialVersionUID = 1L;

    private String fileName;
    private String contentType;
    private byte[] data;

    public StoredFile(String fileName, String contentType, byte[] data) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.data = data != null ? data : new byte[0];
    }

    /** Copies an upload's name, content type and bytes. */
    public static StoredFile copyOf(FormFile upload) throws IOException {
        return new StoredFile(upload.getFileName(), upload.getContentType(), upload.getFileData());
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getFileSize() {
        return data.length;
    }

    /** Ignored: the size always comes from the stored bytes. */
    public void setFileSize(int fileSize) {
    }

    public byte[] getFileData() {
        return data;
    }

    public InputStream getInputStream() {
        return new ByteArrayInputStream(data);
    }

    /** Releases the bytes. */
    public void destroy() {
        data = new byte[0];
    }
}
