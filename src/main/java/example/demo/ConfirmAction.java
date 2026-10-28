package example.demo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;

/**
 * Step 2: confirms the upload. The file comes from the session-scoped form, which may have been
 * written by the other node. The form is then removed from the session.
 */
public class ConfirmAction extends Action {

    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        FormFile attachment = (FormFile) Forms.get(form, "attachment");
        request.setAttribute("title", Forms.get(form, "title"));
        if (attachment != null) {
            request.setAttribute("file", new FileSummary(attachment));
        }
        // Over a limit, Struts doesn't populate the form, so it still holds the stored file.
        // Say so instead of confirming it.
        String limit = UploadLimits.exceeded(request);
        if (limit != null && attachment != null) {
            request.setAttribute("error", limit + " Nothing changed: this is still the file you uploaded.");
            return mapping.findForward("review");
        }
        request.setAttribute("note", Forms.get(form, "note"));
        request.setAttribute("uploadedOn", Forms.get(form, "uploadedOn"));
        request.getSession().removeAttribute(mapping.getAttribute());
        return mapping.findForward("done");
    }
}
