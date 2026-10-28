package example.demo;

import org.apache.struts.action.ActionForm;
import org.apache.struts.upload.FormFile;

/** The two-step upload form. It lives in the session between the upload and the confirmation. */
public class UploadForm extends ActionForm {

    private static final long serialVersionUID = 1L;

    private String title;
    private String note;
    private String uploadedOn;
    private FormFile attachment;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    /** The node that served the upload, sent back by a hidden field on the review page. */
    public String getUploadedOn() {
        return uploadedOn;
    }

    public void setUploadedOn(String uploadedOn) {
        this.uploadedOn = uploadedOn;
    }

    public FormFile getAttachment() {
        return attachment;
    }

    public void setAttachment(FormFile attachment) {
        this.attachment = attachment;
    }
}
