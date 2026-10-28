package example.demo;

import java.io.File;
import java.net.InetAddress;
import java.security.CodeSource;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.config.ModuleConfig;

/** What the page header shows: which node served the request and what it's running. */
public final class DemoInfo {

    private DemoInfo() {
    }

    public static String node() {
        String name = System.getenv("NODE_NAME");
        if (name != null && name.length() > 0) {
            return name;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    public static String strutsJar() {
        return jarOf(ActionServlet.class);
    }

    public static String fileUploadJar() {
        try {
            return jarOf(Class.forName("org.apache.commons.fileupload.FileUploadBase"));
        } catch (ClassNotFoundException e) {
            return "not on the classpath";
        }
    }

    public static String processorClass(ServletContext application) {
        ModuleConfig config = (ModuleConfig) application.getAttribute(Globals.MODULE_KEY);
        return config == null ? "unknown" : config.getControllerConfig().getProcessorClass();
    }

    /** True when this copy of the app uses SessionFileRequestProcessor. */
    public static boolean usesSessionFiles(ServletContext application) {
        return processorClass(application).endsWith("SessionFileRequestProcessor");
    }

    public static String sessionId(HttpSession session) {
        String id = session.getId();
        return id.length() > 8 ? id.substring(0, 8) + "..." : id;
    }

    public static String escape(Object value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (char c : value.toString().toCharArray()) {
            switch (c) {
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '&': out.append("&amp;"); break;
                case '"': out.append("&quot;"); break;
                case '\'': out.append("&#39;"); break;
                default: out.append(c);
            }
        }
        return out.toString();
    }

    private static String jarOf(Class<?> type) {
        CodeSource source = type.getProtectionDomain().getCodeSource();
        return source == null ? "unknown" : new File(source.getLocation().getPath()).getName();
    }
}
