package org.acme.configAdmin.interfaces;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.acme.configAdmin.service.ConfigAdminService;

import static java.util.Objects.requireNonNull;

@Path("/")
public class MainPage {

    @Inject
    ConfigAdminService configAdminService;

    @Location("index")
    Template pageTemplate;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        return this.pageTemplate.instance();
    }
}
