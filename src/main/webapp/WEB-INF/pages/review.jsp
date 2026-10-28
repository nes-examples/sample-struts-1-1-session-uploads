<%@ page contentType="text/html; charset=UTF-8" import="example.demo.FileSummary" %>
<%@ include file="top.jspf" %>
<% FileSummary file = (FileSummary) request.getAttribute("file"); %>
<h1>2. Review</h1>
<% if (request.getAttribute("error") != null) { %>
<p class="error" id="error"><%= DemoInfo.escape(request.getAttribute("error")) %></p>
<% } %>
<table id="file" data-name="<%= DemoInfo.escape(file.getFileName()) %>"
       data-size="<%= file.getSize() %>" data-sha256="<%= file.getSha256() %>">
  <tr><th>Title</th><td><%= DemoInfo.escape(request.getAttribute("title")) %></td></tr>
  <tr><th>File</th><td><%= DemoInfo.escape(file.getFileName()) %></td></tr>
  <tr><th>Content type</th><td><%= DemoInfo.escape(file.getContentType()) %></td></tr>
  <tr><th>Size</th><td><%= file.getSize() %> bytes</td></tr>
  <tr><th>SHA-256</th><td><code><%= file.getSha256() %></code></td></tr>
  <tr><th>Held on the form as</th><td><code><%= DemoInfo.escape(file.getHeldAs()) %></code></td></tr>
</table>
<form action="confirm.do" method="post" enctype="multipart/form-data">
  <input type="hidden" name="uploadedOn" value="<%= DemoInfo.escape(DemoInfo.node()) %>">
  <label>Note <input type="text" name="note"></label>
  <label>Replace the file (optional) <input type="file" name="attachment"></label>
  <button type="submit">Confirm</button>
</form>
<p>Leave the file input empty to keep the file you uploaded. The browser still sends an empty file
part for it.</p>
<%@ include file="bottom.jspf" %>
