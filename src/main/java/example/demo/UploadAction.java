package example.demo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;

/** Step 1: takes the upload and shows the review page. */
public class UploadAction extends Action {

    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        String limit = UploadLimits.exceeded(request);
        if (limit != null) {
            request.setAttribute("error", limit);
            return mapping.findForward("retry");
        }
        FormFile attachment = (FormFile) Forms.get(form, "attachment");
        if (attachment == null || attachment.getFileName() == null
                || attachment.getFileName().length() == 0) {
            request.setAttribute("error", "Choose a file to upload.");
            return mapping.findForward("retry");
        }
        request.setAttribute("title", Forms.get(form, "title"));
        request.setAttribute("file", new FileSummary(attachment));
        return mapping.findForward("review");
    }
}
