package com.danielflower.apprunner.web.v1;

import com.danielflower.apprunner.runners.LeinRunner;
import com.danielflower.apprunner.runners.MavenRunner;
import com.danielflower.apprunner.runners.NodeRunner;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Api(value = "System")
@Path("/system")
public class SystemResource {
    public static final Logger log = LoggerFactory.getLogger(SystemResource.class);

    private static final Runner[] sampleProjects = new Runner[] {
        new Runner("maven", "Java uber jars built with maven", MavenRunner.startCommands),
        new Runner("lein", "Clojure uber jars built with leiningen", LeinRunner.startCommands),
        new Runner("nodejs", "NodeJS apps with NPM dependencies", NodeRunner.startCommands),
    };

    @GET
    @Produces("application/json")
    @ApiOperation(value = "Returns information about AppRunner, including information about sample apps")
    public Response systemInfo(@Context UriInfo uri) throws IOException {
        JSONObject result = new JSONObject();
        JSONArray apps = new JSONArray();
        result.put("samples", apps);
        for (Runner proj : sampleProjects) {
            JSONObject sample = new JSONObject();
            sample.put("name", proj.name);
            sample.put("description", proj.description);
            sample.put("url", uri.getRequestUri().resolve("system/samples/" + proj.zipName()));
            sample.put("runCommands", new JSONArray(proj.commands));
            apps.put(sample);
        }
        return Response.ok(result.toString()).build();
    }

    @GET
    @Path("/samples/{name}")
    @Produces("application/zip")
    @ApiOperation("Returns a ZIP file containing a sample app")
    public Response samples(@ApiParam(required = true, allowableValues = "maven.zip, lein.zip, nodejs.zip") @PathParam("name") String name) throws IOException {
        List<String> sampleProjectNames = Arrays.stream(sampleProjects).map(Runner::zipName).collect(Collectors.toList());
        if (!sampleProjectNames.contains(name)) {
            return Response.status(404).entity("Invalid sample app name. Valid names: " + sampleProjectNames).build();
        }

        try (InputStream zipStream = getClass().getResourceAsStream("/sample-apps/" + name)) {
            return Response.ok(IOUtils.toByteArray(zipStream))
                .header("Content-Disposition", "attachment; filename=\"" + name + "\"")
                .build();
        }
    }

    private static class Runner {
        public final String name;
        public final String description;
        public final String[] commands;

        private Runner(String name, String description, String[] commands) {
            this.name = name;
            this.description = description;
            this.commands = commands;
        }
        public String zipName() {
            return name + ".zip";
        }
    }
}