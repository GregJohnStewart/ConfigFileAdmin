package org.acme.configAdmin.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.acme.configAdmin.model.ConfigTracker;
import org.acme.configAdmin.service.ConfigAdminService;
import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Path("/config")
public class ConfigEndpoints {

    @Inject
    ConfigAdminService configAdminService;

    @Location("configForm")
    Template configFormTemplate;

    @GET
    @Path("form/{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getConfig(@PathParam("id") UUID id) {
        return this.configFormTemplate.data("configId", id);
    }


    @PATCH
    @Path("form/{id}/values")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance updateValues(
            @PathParam("id") UUID id,
            ObjectNode requestBody
    ) throws ConfigurationException, IOException {
        this.configAdminService.processUpdateRequest(id, requestBody);
        return this.configFormTemplate
                .data("configId", id)
                .data("alert", null);
    }

    @PATCH
    @Path("form/{id}/content")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance updateContent(
            @PathParam("id") UUID id,
            ObjectNode requestBody
    ) throws IOException {
        this.configAdminService.overwriteFile(id, requestBody.get("fileContent").asText());
        return this.configFormTemplate
                .data("configId", id)
                .data("alert", null);
    }
}
