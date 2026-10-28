package example.demo;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.upload.MultipartRequestHandler;

/**
 * Says which upload limit a request went over, from the request attributes Struts sets. When either
 * is set, Struts hasn't copied the request into the form at all.
 */
final class UploadLimits {

    /**
     * Set by NES for Apache Struts 1.1 when a text field is over {@code maxStringLen}. It sets
     * MaxLengthExceeded too. Apache Struts 1.1 has neither the limit nor this attribute.
     */
    private static final String STRING_LENGTH_EXCEEDED =
            "org.apache.struts.upload.MaxStringLengthExceeded";

    private UploadLimits() {
    }

    /** A message for the limit the request went over, or null if it went over none. */
    static String exceeded(HttpServletRequest request) {
        if (Boolean.TRUE.equals(request.getAttribute(STRING_LENGTH_EXCEEDED))) {
            return "A text field is longer than the server accepts (maxStringLen).";
        }
        if (Boolean.TRUE.equals(request.getAttribute(
                MultipartRequestHandler.ATTRIBUTE_MAX_LENGTH_EXCEEDED))) {
            return "The file is larger than the 5 MB limit.";
        }
        return null;
    }
}
