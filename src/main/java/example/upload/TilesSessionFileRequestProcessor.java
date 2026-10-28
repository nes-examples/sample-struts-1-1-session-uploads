package example.upload;

import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.TilesRequestProcessor;

/**
 * {@link SessionFileRequestProcessor} for applications that use the Tiles plug-in. The plug-in
 * stops Struts from starting when {@code processorClass} names a {@code RequestProcessor} subclass
 * that doesn't extend {@code TilesRequestProcessor}.
 */
public class TilesSessionFileRequestProcessor extends TilesRequestProcessor {

    protected void processPopulate(HttpServletRequest request, HttpServletResponse response,
            ActionForm form, ActionMapping mapping) throws ServletException {
        Map<String, StoredFile> before = SessionFiles.before(form, mapping);
        super.processPopulate(request, response, form, mapping);
        SessionFiles.after(request, form, mapping, before);
    }
}
