package com.saf.verification;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio simulado para obtener predios por cédula/RUC (datos quemados).
 */
public class PrediosService {

    @WebMethod(operationName = "getPredios")
    public GetPrediosResponse getPredios(@WebParam(name = "request") GetPrediosRequest request) {

        // Datos quemados para simulación
        List<Predio> predios = new ArrayList<>();

        if ("CEDULA".equals(request.getIdentifierType()) && "0102030405".equals(request.getIdentifierValue())) {
            Predio predio = new Predio();
            predio.setPredioId("12345");
            predio.setPredioCodigo("PRE-000123");
            predio.setAreaM2(12500.5);
            predio.setSRID(32717);
            predio.setGeometryWKT("MULTIPOLYGON(((786669 9959898,786694 9959918,...)))");
            predio.setGeometryGeoJSON("{\"type\":\"MultiPolygon\", \"coordinates\":[[[...]]]}");
            predios.add(predio);
        }

        GetPrediosResponse response = new GetPrediosResponse();
        response.setRequestStatus(new RequestStatus("0", "OK"));
        response.setPredios(predios);

        return response;
    }
}