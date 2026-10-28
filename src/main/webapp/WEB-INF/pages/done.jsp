<%@ page contentType="text/html; charset=UTF-8" import="example.demo.FileSummary" %>
<%@ include file="top.jspf" %>
<% FileSummary file = (FileSummary) request.getAttribute("file"); %>
<h1>3. Confirmed</h1>
<% if (file == null) { %>
<p class="error" id="missing">No file reached this step. The session didn't hold the upload.</p>
<% } else { %>
<p class="ok">Uploaded on <%= DemoInfo.escape(request.getAttribute("uploadedOn")) %>,
confirmed on <%= DemoInfo.escape(DemoInfo.node()) %>.</p>
<% } %>
<table id="file"<% if (file != null) { %> data-name="<%= DemoInfo.escape(file.getFileName()) %>"
       data-size="<%= file.getSize() %>" data-sha256="<%= file.getSha256() %>"<% } %>>
  <tr><th>Title</th><td><%= DemoInfo.escape(request.getAttribute("title")) %></td></tr>
  <tr><th>Note</th><td><%= DemoInfo.escape(request.getAttribute("note")) %></td></tr>
<% if (file != null) { %>
  <tr><th>File</th><td><%= DemoInfo.escape(file.getFileName()) %></td></tr>
  <tr><th>Size</th><td><%= file.getSize() %> bytes</td></tr>
  <tr><th>SHA-256</th><td><code><%= file.getSha256() %></code></td></tr>
  <tr><th>Held on the form as</th><td><code><%= DemoInfo.escape(file.getHeldAs()) %></code></td></tr>
<% } %>
</table>
<p><a href="<%= request.getContextPath() %>/start.do">Upload another file</a></p>
<%@ include file="bottom.jspf" %>
