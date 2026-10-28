package example.upload;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;

/**
 * Keeps uploaded files on session-scoped forms from one request to the next. A request processor
 * calls {@link #before} and {@link #after} around {@code processPopulate}.
 *
 * <p>Every {@code FormFile} property is handled: on an {@code ActionForm} subclass, any property with
 * a getter and a setter, and on a {@code DynaActionForm}, any property declared with that type.
 * Forms in request scope are left alone, because the upload is used in the same request.
 */
public final class SessionFiles {

    private SessionFiles() {
    }

    /** Remembers the files a session-scoped form holds before Struts resets and populates it. */
    public static Map<String, StoredFile> before(ActionForm form, ActionMapping mapping)
            throws ServletException {
        Map<String, StoredFile> stored = new HashMap<>();
        if (!inSession(form, mapping)) {
            return stored;
        }
        for (String name : fileProperties(form)) {
            Object value = get(form, name);
            if (value instanceof StoredFile) {
                stored.put(name, (StoredFile) value);
            }
        }
        return stored;
    }

    /**
     * Replaces each new upload on the form with a {@link StoredFile}. When the request has no new
     * file for a property (an empty file input, no file part at all, or a {@code reset()} that
     * cleared it), the file stored earlier is put back.
     *
     * <p>Then the form is stored in the session again. Struts stores a new session-scoped form
     * before it copies the request into it, so a session manager that saves an attribute when
     * {@code setAttribute} is called would otherwise keep the empty form.
     */
    public static void after(HttpServletRequest request, ActionForm form, ActionMapping mapping,
            Map<String, StoredFile> before) throws ServletException {
        if (!inSession(form, mapping)) {
            return;
        }
        for (String name : fileProperties(form)) {
            Object value = get(form, name);
            if (value instanceof StoredFile) {
                continue;
            }
            StoredFile keep = before.get(name);
            FormFile upload = (FormFile) value;
            if (upload != null) {
                try {
                    String fileName = upload.getFileName();
                    if (fileName != null && fileName.length() > 0) {
                        keep = StoredFile.copyOf(upload);
                    }
                } catch (IOException e) {
                    throw new ServletException("Could not read the upload for " + name, e);
                } finally {
                    upload.destroy();
                }
            }
            set(form, name, keep);
        }
        request.getSession().setAttribute(mapping.getAttribute(), form);
    }

    private static boolean inSession(ActionForm form, ActionMapping mapping) {
        return form != null && "session".equals(mapping.getScope());
    }

    private static List<String> fileProperties(ActionForm form) {
        List<String> names = new ArrayList<>();
        if (form instanceof DynaBean) {
            for (DynaProperty property : ((DynaBean) form).getDynaClass().getDynaProperties()) {
                if (FormFile.class.isAssignableFrom(property.getType())) {
                    names.add(property.getName());
                }
            }
            return names;
        }
        for (PropertyDescriptor property : PropertyUtils.getPropertyDescriptors(form)) {
            Class<?> type = property.getPropertyType();
            if (type != null && FormFile.class.isAssignableFrom(type)
                    && property.getReadMethod() != null && property.getWriteMethod() != null) {
                names.add(property.getName());
            }
        }
        return names;
    }

    private static Object get(ActionForm form, String name) throws ServletException {
        try {
            return PropertyUtils.getSimpleProperty(form, name);
        } catch (Exception e) {
            throw new ServletException("Could not read form property " + name, e);
        }
    }

    private static void set(ActionForm form, String name, Object value) throws ServletException {
        try {
            PropertyUtils.setSimpleProperty(form, name, value);
        } catch (Exception e) {
            throw new ServletException("Could not set form property " + name, e);
        }
    }
}
