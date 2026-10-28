<%@ page contentType="text/html; charset=UTF-8" %>
<%@ include file="top.jspf" %>
<h1>1. Upload a file</h1>
<% if (request.getAttribute("error") != null) { %>
<p class="error" id="error"><%= DemoInfo.escape(request.getAttribute("error")) %></p>
<% } %>
<form action="upload.do" method="post" enctype="multipart/form-data">
  <label>Title <input type="text" name="title"></label>
  <label>File (up to 5 MB) <input type="file" name="attachment"></label>
  <button type="submit">Upload</button>
</form>
<p>The upload goes into a session-scoped form. The next step reads it back from the session,
probably on the other node.</p>
<%@ include file="bottom.jspf" %>
