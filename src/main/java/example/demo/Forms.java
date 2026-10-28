package example.demo;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.struts.action.ActionForm;

/** Reads a form property by name, so the same actions serve UploadForm and DynaActionForm. */
final class Forms {

    private Forms() {
    }

    static Object get(ActionForm form, String name) throws Exception {
        return form == null ? null : PropertyUtils.getSimpleProperty(form, name);
    }
}
