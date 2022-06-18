package io.quarkus.rest.client.reactive.multipart;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartFilename;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class MultipartFilenameTest {

    @TestHTTPResource
    URI baseUri;

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class, FormData.class, Client.class, ClientForm.class));

    @Test
    void shouldPassOriginalFileName() throws IOException {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        File file = File.createTempFile("MultipartTest", ".txt");
        file.deleteOnExit();

        ClientForm form = new ClientForm();
        form.file = file;
        assertThat(client.postMultipart(form)).isEqualTo(file.getName());
    }

    @Test
    void shouldUseFileNameFromAnnotation() throws IOException {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        File file = File.createTempFile("MultipartTest", ".txt");
        file.deleteOnExit();

        ClientForm2 form = new ClientForm2();
        form.file = file;
        assertThat(client.postMultipartWithPartFilename(form)).isEqualTo(ClientForm2.FILE_NAME);
    }

    @Path("/multipart")
    @ApplicationScoped
    public static class Resource {
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        public String upload(@MultipartForm FormData form) {
            return form.myFile.fileName();
        }
    }

    public static class FormData {
        @FormParam("myFile")
        public FileUpload myFile;

    }

    @Path("/multipart")
    public interface Client {
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipart(@MultipartForm ClientForm clientForm);

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        String postMultipartWithPartFilename(@MultipartForm ClientForm2 clientForm);
    }

    public static class ClientForm {
        @FormParam("myFile")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public File file;
    }

    public static class ClientForm2 {
        public static final String FILE_NAME = "clientFile";

        @FormParam("myFile")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        @PartFilename(FILE_NAME)
        public File file;
    }
}
