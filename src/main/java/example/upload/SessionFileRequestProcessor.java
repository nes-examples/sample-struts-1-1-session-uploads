package example.upload;

import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.RequestProcessor;

/**
 * Keeps uploads on session-scoped forms as {@link StoredFile}s, so they survive later requests and
 * can move between cluster nodes with the session. Configure it on the controller:
 *
 * <pre>
 * &lt;controller processorClass="example.upload.SessionFileRequestProcessor"/&gt;
 * </pre>
 *
 * <p>With the Tiles plug-in, use {@link TilesSessionFileRequestProcessor} instead.
 */
public class SessionFileRequestProcessor extends RequestProcessor {

    protected void processPopulate(HttpServletRequest request, HttpServletResponse response,
            ActionForm form, ActionMapping mapping) throws ServletException {
        Map<String, StoredFile> before = SessionFiles.before(form, mapping);
        super.processPopulate(request, response, form, mapping);
        SessionFiles.after(request, form, mapping, before);
    }
}
