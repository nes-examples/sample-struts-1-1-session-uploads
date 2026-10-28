# Struts 1.1 uploads in a clustered session

A Struts 1.1 form in session scope can't keep an uploaded file. The `FormFile` that Struts puts on the form wraps a temporary file that only lasts for the request, and the object can't be serialized. On a cluster that shares sessions between nodes, saving the session fails, and the next step of the form finds no file.

This example keeps the upload with a request processor instead. After Struts populates a session-scoped form, the processor swaps each new upload for an in-memory copy that can be serialized. Struts itself doesn't change: the fix is three classes in the application and one attribute in `struts-config.xml`, plus a fourth class for applications that use Tiles.

It runs on Apache Struts 1.1, and the same build switches to the NES for Apache Struts 1.1 trial with one setting.

## What runs

`docker compose up` starts two Tomcat 9 nodes on Java 8 that keep HTTP sessions in Redis, through the [Redisson](https://redisson.org/) session manager. A proxy on port 8080 sends each request to the next node in turn, with no sticky sessions, so a form's second step usually runs on the other node.

Each node deploys the app twice:

| Copy | Request processor |
|---|---|
| [`/before/`](http://localhost:8080/before/) | Struts' default `RequestProcessor` |
| [`/after/`](http://localhost:8080/after/) | `example.upload.SessionFileRequestProcessor` |

The app is a two-step form: upload a file, then review and confirm it. The header of each page shows the node that served it, the Struts and Commons FileUpload jars, and the request processor.

## Requirements

- Docker with `docker compose`. The build runs in Docker, so you don't need a JDK or Maven.
- Nothing else for the NES trial. The HeroDevs NES registry asks for a user name and password, but any values work for trial versions, so `settings.xml` holds placeholders.

## Run it

```bash
docker compose up --build -d
```

Open <http://localhost:8080/>, which starts on `/before/`. Stop it with `docker compose down`. To use another port, run `PORT=9090 docker compose up --build -d`.

To run the NES for Apache Struts 1.1 trial instead, rebuild with the `nes-trial` profile:

```bash
STRUTS=nes-trial docker compose up --build -d
```

The page header then shows `struts-1.1.0-struts-1.1.1-trial.jar`.

## Things to try

On `/before/`:

1. Upload a file. The review page shows it, held on the form as `CommonsMultipartRequestHandler$CommonsFormFile`. The response status is 500, though, because the session couldn't be saved:

   ```bash
   docker compose exec node1 sh -c 'grep -h NotSerializable /usr/local/tomcat/logs/localhost.*.log'
   ```

   ```text
   java.lang.IllegalArgumentException: java.io.NotSerializableException: org.apache.struts.upload.CommonsMultipartRequestHandler$CommonsFormFile
   ```

   The log line is on whichever node served the upload.
2. Confirm. The title is gone, and the "file" is the empty file input the confirm step sent. Each node reads the session from Redis, so this happens even when both steps run on the same node.

On `/after/`:

1. Upload a file. It's held on the form as `example.upload.StoredFile`.
2. Confirm without choosing a replacement. The confirmation page says which node took the upload and which confirmed it, with the same SHA-256 as on the review page.
3. Upload again, and choose a replacement on the review page. The replacement is what gets confirmed.
4. Upload a file over 5 MB. The page says it's too large.

## Check it from a script

`scripts/check.sh` uploads on one node, confirms on the other, and compares SHA-256 hashes. It needs bash and curl. Run it once per Struts build to see that the trial behaves like Apache Struts:

```bash
STRUTS=nes-trial docker compose up --build -d
scripts/check.sh
```

```text
Struts: struts-1.1.0-struts-1.1.1-trial.jar

PASS  10 KB, empty file input            upload on node1 (HTTP 200), confirm on node2 (HTTP 200), held as StoredFile
PASS  1 MB, no file part                 upload on node2 (HTTP 200), confirm on node1 (HTTP 200), held as StoredFile
PASS  1 MB, replaced with 10 KB          upload on node1 (HTTP 200), confirm on node2 (HTTP 200), held as StoredFile
PASS  DynaActionForm, 10 KB              upload on node1 (HTTP 200), confirm on node2 (HTTP 200), held as StoredFile
PASS  6 MB, over maxFileSize             the upload page says the file is too large

LOST  Before: 10 KB, empty file input    upload on node1 (HTTP 500), confirm on node2 (HTTP 200), confirmed a 0-byte file instead
(LOST is the expected result for the before copy.)
```

The output is the same on Apache Struts 1.1, apart from the jar name.

## How it works

The reusable part is in [`src/main/java/example/upload/`](src/main/java/example/upload/):

| Class | What it does |
|---|---|
| [`StoredFile`](src/main/java/example/upload/StoredFile.java) | A `FormFile` that holds the file name, content type and bytes, and is `Serializable` |
| [`SessionFiles`](src/main/java/example/upload/SessionFiles.java) | The logic that runs before and after Struts populates the form |
| [`SessionFileRequestProcessor`](src/main/java/example/upload/SessionFileRequestProcessor.java) | Calls `SessionFiles` around `processPopulate` |
| [`TilesSessionFileRequestProcessor`](src/main/java/example/upload/TilesSessionFileRequestProcessor.java) | The same, for applications that use the Tiles plug-in |

For a mapping with `scope="session"`, the processor runs in this order:

1. It remembers the `StoredFile`s the form already holds.
2. Struts resets and populates the form as usual.
3. Each `FormFile` property that now holds a new upload gets a `StoredFile` copy, and the upload's temporary file is deleted. When the request brought no new file for a property, the stored file is put back. That covers an empty file input, a request with no file part, and a `reset()` that cleared the property.
4. The form is stored in the session again.

The last step matters for some session managers. Struts stores a new session-scoped form before it copies the request into it. A manager that saves an attribute only when `setAttribute` is called would keep that empty form. Redisson's `updateMode="DEFAULT"` works that way: without step 4, the confirm step lost both the title and the file. This example uses `updateMode="AFTER_REQUEST"`, which saves every attribute when the request ends, so step 4 isn't needed here, but it does no harm.

Forms in request scope are left alone, because their upload is used in the same request.

## Use it in your application

1. Copy the classes in `example/upload/` into your application, in your own package. You need `TilesSessionFileRequestProcessor` only with Tiles.
2. Set the processor on the `<controller>` element. Each Struts module has its own:

   ```xml
   <controller processorClass="com.example.upload.SessionFileRequestProcessor"/>
   ```

   - **With the Tiles plug-in,** use `TilesSessionFileRequestProcessor`. The plug-in refuses any other `RequestProcessor` subclass, and Struts doesn't start: `TilesPlugin : Specified RequestProcessor not compatible with TilesRequestProcessor`.
   - **With a custom request processor already,** add the `processPopulate` override to it instead.
3. Leave your actions as they are. `getAttachment()` returns a `StoredFile` on every request after the upload, and its `getFileData()` and `getInputStream()` work as before.

To drop a stored file, set the property to `null` in the action. The processor only puts back what the form held when the request started.

Some things to know before you use it:

- **The file is in memory.** The whole file sits in the session, on the node and in the session store. Keep `maxFileSize` to what a session can carry. For large files, write them to shared storage and keep a reference on the form.
- **Give session-scoped forms a `serialVersionUID`.** Without one, Java computes it from the class, so a rebuild can stop sessions from loading.
- **Clear sessions when you change Struts versions.** In our runs, this example's `UploadForm` crossed between Apache Struts 1.1, the trial and NES for Apache Struts 1.1.4 in both directions. A `DynaActionForm` didn't: the other node failed with `InvalidClassException` on `org.apache.struts.action.DynaActionForm`, because the two jars give it different serialVersionUIDs. Here, `docker compose exec redis redis-cli FLUSHALL` clears every session.
- **Limits stop population.** Over `maxFileSize`, Struts doesn't copy the request into the form, so a too-large replacement leaves the stored file in place. The example's actions check for this and say so instead of confirming the old file.

## Switching to NES for Apache Struts 1.1

**The trial.** The `nes-trial` profile changes the Struts version to `1.1.0-struts-1.1.1-trial` and adds the HeroDevs NES registry. It also adds `commons-fileupload` 1.5, because the trial's POM marks that dependency optional. Without it, nothing brings in Commons FileUpload, and every multipart request fails:

```text
java.lang.ClassNotFoundException: org.apache.commons.fileupload.FileUploadException
```

Commons FileUpload 1.5 brings Commons IO through its own POM. With that dependency in place, every check above gives the same result on the trial as on Apache Struts 1.1. The trial's upload handling has the same settings as Apache Struts 1.1.

Both POMs also declare `javax.sql:jdbc-stdext`, which isn't published, so `pom.xml` excludes it. Java 8 includes `javax.sql`.

**The full release.** NES for Apache Struts 1.1.4 needs an NES access token, and its POM brings NES Commons FileUpload itself. We ran this example's flow against it: the processor works the same, including on `DynaActionForm` and with Tiles. The release adds upload limits from its CVE fixes, and they change what some requests do:

| Request | Apache Struts 1.1 and the trial | NES for Apache Struts 1.1.4 |
|---|---|---|
| A file over `maxFileSize` | The form isn't populated, and the page says the file is too large | HTTP 500, `FileSizeLimitExceededException` |
| A text field over 4 KB | Accepted | The form isn't populated, and the page says a text field is too long. Raise the limit with `<controller maxStringLen="...">` |
| A file name over about 415 characters | Accepted | HTTP 500, "Header section has more than 512 bytes". Raise the limit with `<controller maxHeaderSize="...">` |

A 500 after a too-large replacement leaves the stored file in place, so the user can still confirm the first upload.

## More information

- [NES for Apache Struts 1.1 Trial Setup](https://docs.herodevs.com/apache-struts/1-1/struts-trial)
- [NES for Apache Struts 1.x File Upload](https://docs.herodevs.com/apache-struts/additional-info/file-upload): every upload limit, its default, and what happens when a request exceeds it

## License

The application code is licensed under the [Apache License 2.0](LICENSE).
