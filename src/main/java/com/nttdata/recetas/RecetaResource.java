package com.nttdata.recetas;

import com.nttdata.recetas.model.ConfigResponse;
import com.nttdata.recetas.model.FirmaRequest;
import com.nttdata.recetas.model.FirmaResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * API de recetas. Los tres secretos deben llegar desde HashiCorp Vault como
 * propiedades: el nombre de cada @ConfigProperty tiene que coincidir EXACTAMENTE
 * con la key del secreto  secret/clinica/ms-recetas.
 */
@Path("/api/recetas")
public class RecetaResource {

    private static final String MASCARA = "****";
    private static final int VISIBLES_ENMASCARADO = 4;

    @ConfigProperty(name = "farmacia.api-key")
    private String apiKey;

    @ConfigProperty(name = "farmacia.url")
    private String farmaciaUrl;

    @ConfigProperty(name = "firma.clave")
    private String firmaClave;

    private final FirmaService firmaService;

    public RecetaResource(FirmaService firmaService) {
        this.firmaService = firmaService;
    }

    @GET
    @Path("/config")
    @Produces(MediaType.APPLICATION_JSON)
    public ConfigResponse config() {
        return new ConfigResponse(
                farmaciaUrl,
                enmascarar(apiKey),
                firmaClave != null && !firmaClave.isBlank()
        );
    }

    @POST
    @Path("/firmar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response firmar(FirmaRequest request) {
        if (request == null || request.codigoReceta() == null || request.codigoReceta().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        String firma = firmaService.firmar(request.codigoReceta(), firmaClave);
        FirmaResponse respuesta = new FirmaResponse(
                request.codigoReceta(),
                request.paciente(),
                firma
        );
        return Response.ok(respuesta).build();
    }

    private String enmascarar(String valor) {
        if (valor == null || valor.length() <= VISIBLES_ENMASCARADO) {
            return MASCARA;
        }
        return MASCARA + valor.substring(valor.length() - VISIBLES_ENMASCARADO);
    }
}
