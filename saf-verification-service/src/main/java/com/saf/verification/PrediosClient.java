package com.saf.verification;

import javax.xml.namespace.QName;
import javax.xml.soap.*;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente para consumir el servicio externo de predios de Agrocalidad.
 */
public class PrediosClient {

    private String serviceUrl;
    private String usuario;
    private String clave;
    private static final String NAMESPACE = "http://predios.agrocalidad.gob.ec/service";
    private static final String SERVICE_NAME = "PrediosService";

    public PrediosClient(String serviceUrl, String usuario, String clave) {
        this.serviceUrl = serviceUrl;
        this.usuario = usuario;
        this.clave = clave;
    }

    /**
     * Consulta operador por cédula en el servicio externo de Agrocalidad.
     */
    public GetPrediosResponse getPredios(String identifierType, String identifierValue) {
        try {
            // Extraer la URL base del WSDL (sin ?wsdl)
            String endpointUrl = serviceUrl.replace("?wsdl", "").replace("?WSDL", "");
            
            // Crear el mensaje SOAP para consultarPorCedula
            MessageFactory messageFactory = MessageFactory.newInstance();
            SOAPMessage soapMessage = messageFactory.createMessage();
            SOAPPart soapPart = soapMessage.getSOAPPart();
            SOAPEnvelope envelope = soapPart.getEnvelope();
            SOAPBody body = envelope.getBody();

            // Crear elemento consultarPorCedula CON namespace
            QName consultarPorCedulaQName = new QName(NAMESPACE, "consultarPorCedula", "ser");
            SOAPElement consultarPorCedula = body.addChildElement(consultarPorCedulaQName);
            
            // Agregar request DENTRO del namespace del servicio (heredado)
            SOAPElement request = consultarPorCedula.addChildElement("request");
            
            SOAPElement usuarioElement = request.addChildElement("usuario");
            usuarioElement.addTextNode(this.usuario);
            
            SOAPElement claveElement = request.addChildElement("clave");
            claveElement.addTextNode(this.clave);
            
            SOAPElement cedula = request.addChildElement("cedula");
            cedula.addTextNode(identifierValue);

            soapMessage.saveChanges();
            
            // Log del request para debugging
            System.out.println("=== REQUEST ENVIADO ===");
            java.io.ByteArrayOutputStream outReq = new java.io.ByteArrayOutputStream();
            soapMessage.writeTo(outReq);
            System.out.println(outReq.toString());
            System.out.println("=== FIN REQUEST ===");

            // Crear conexión HTTP directa sin usar WSDL
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection connection = soapConnectionFactory.createConnection();
            
            // Invocar servicio con URL directa
            SOAPMessage response = connection.call(soapMessage, endpointUrl);
            connection.close();
            
            // Parsear respuesta
            return parseResponse(response);

        } catch (Exception e) {
            System.err.println("ERROR en PrediosClient.getPredios: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error llamando servicio de predios: " + e.getMessage(), e);
        }
    }

    private GetPrediosResponse parseResponse(SOAPMessage soapResponse) {
        try {
            // Imprimir respuesta para debugging
            System.out.println("=== RESPUESTA DEL SERVICIO EXTERNO ===");
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            soapResponse.writeTo(out);
            System.out.println(out.toString());
            System.out.println("=== FIN RESPUESTA ===");
            
            GetPrediosResponse response = new GetPrediosResponse();
            List<Predio> predios = new ArrayList<>();

            SOAPBody body = soapResponse.getSOAPBody();
            
            // Validar que el body no esté vacío
            if (body == null) {
                System.err.println("ERROR: SOAP Body es null");
                response.setPredios(predios);
                return response;
            }
            
            // Buscar elementos operador en la respuesta
            org.w3c.dom.NodeList operadorNodes = body.getElementsByTagNameNS("*", "operador");
            System.out.println("Número de operadores encontrados: " + operadorNodes.getLength());
            
            for (int i = 0; i < operadorNodes.getLength(); i++) {
                org.w3c.dom.Node operadorNode = operadorNodes.item(i);
                if (operadorNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    Predio predio = new Predio();
                    
                    try {
                        org.w3c.dom.NodeList children = operadorNode.getChildNodes();
                        for (int j = 0; j < children.getLength(); j++) {
                            org.w3c.dom.Node child = children.item(j);
                            if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                                String localName = child.getLocalName();
                                String textContent = child.getTextContent();
                                
                                try {
                                    switch (localName) {
                                        case "cedula":
                                            if (textContent != null && !textContent.trim().isEmpty()) {
                                                predio.setIdentifier(textContent.trim());
                                            }
                                            break;
                                        case "razonSocial":
                                            if (textContent != null && !textContent.trim().isEmpty()) {
                                                predio.setOwnerName(textContent.trim());
                                            }
                                            break;
                                        case "area":
                                            // Parsear el nodo area que contiene subnodos
                                            parseAreaNode(child, predio);
                                            break;
                                    }
                                } catch (Exception fieldEx) {
                                    System.err.println("ERROR parseando campo " + localName + ": " + fieldEx.getMessage());
                                    // Continuar con el siguiente campo
                                }
                            }
                        }
                    } catch (Exception predioEx) {
                        System.err.println("ERROR parseando predio " + (i+1) + ": " + predioEx.getMessage());
                        predioEx.printStackTrace();
                        // Continuar con el siguiente predio
                        continue;
                    }
                    
                    if (predio.getIdentifier() != null) {
                        predios.add(predio);
                    }
                }
            }

            response.setPredios(predios);
            return response;

        } catch (Exception e) {
            throw new RuntimeException("Error parseando respuesta del servicio de predios", e);
        }
    }
    
    private void parseAreaNode(org.w3c.dom.Node areaNode, Predio predio) {
        if (areaNode == null) {
            System.err.println("WARN: Nodo area es null");
            return;
        }
        
        try {
            org.w3c.dom.NodeList areaChildren = areaNode.getChildNodes();
            for (int k = 0; k < areaChildren.getLength(); k++) {
                org.w3c.dom.Node areaChild = areaChildren.item(k);
                if (areaChild.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    String fieldName = areaChild.getLocalName();
                    String fieldValue = areaChild.getTextContent();
                    
                    if (fieldValue == null || fieldValue.trim().isEmpty()) {
                        continue; // Saltar campos vacíos
                    }
                    
                    try {
                        switch (fieldName) {
                            case "codigoArea":
                                predio.setPredioId(fieldValue.trim());
                                break;
                            case "coordenadasArea":
                                // Las coordenadas vienen en formato WKB (Well-Known Binary) como string hexadecimal
                                // Necesitamos convertir a WKT para usarlo en queries PostGIS
                                String wkbHex = fieldValue.trim();
                                if (isValidWKBHex(wkbHex)) {
                                    String wkt = convertWKBToWKT(wkbHex);
                                    if (wkt != null) {
                                        predio.setGeometryWKT(wkt);
                                        System.out.println("WKB convertido a WKT exitosamente");
                                    } else {
                                        System.err.println("WARN: Error al convertir WKB a WKT");
                                    }
                                } else {
                                    System.err.println("WARN: WKB inválido, ignorando coordenadas: " + wkbHex.substring(0, Math.min(20, wkbHex.length())));
                                }
                                break;
                            case "superficieArea":
                                try {
                                    double superficie = Double.parseDouble(fieldValue.trim());
                                    if (superficie >= 0) {
                                        predio.setArea(superficie);
                                    } else {
                                        System.err.println("WARN: Superficie negativa ignorada: " + superficie);
                                        predio.setArea(0.0);
                                    }
                                } catch (NumberFormatException e) {
                                    System.err.println("ERROR parseando superficieArea: " + fieldValue + " - " + e.getMessage());
                                    predio.setArea(0.0);
                                }
                                break;
                        }
                    } catch (Exception fieldEx) {
                        System.err.println("ERROR parseando campo area." + fieldName + ": " + fieldEx.getMessage());
                        // Continuar con el siguiente campo
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("ERROR general parseando nodo area: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Valida que un string sea un WKB hexadecimal válido
     * WKB debe ser una cadena hexadecimal (solo 0-9, A-F) de longitud par
     */
    private boolean isValidWKBHex(String wkbHex) {
        if (wkbHex == null || wkbHex.isEmpty()) {
            return false;
        }
        
        // Verificar longitud par (cada byte = 2 caracteres hex)
        if (wkbHex.length() % 2 != 0) {
            System.err.println("ERROR WKB: longitud impar " + wkbHex.length());
            return false;
        }
        
        // WKB mínimo: byte order (2) + geometry type (8) = 10 caracteres
        if (wkbHex.length() < 10) {
            System.err.println("ERROR WKB: muy corto " + wkbHex.length());
            return false;
        }
        
        // Verificar que solo contenga caracteres hexadecimales
        boolean isHex = wkbHex.matches("^[0-9A-Fa-f]+$");
        if (!isHex) {
            System.err.println("ERROR WKB: contiene caracteres no hexadecimales");
        }
        return isHex;
    }
    
    /**
     * Convierte un WKB hexadecimal a WKT usando la librería JTS
     */
    private String convertWKBToWKT(String wkbHex) {
        try {
            // Convertir string hexadecimal a bytes
            byte[] wkbBytes = hexStringToByteArray(wkbHex);
            
            // Usar JTS WKBReader para parsear el WKB
            org.locationtech.jts.io.WKBReader wkbReader = new org.locationtech.jts.io.WKBReader();
            org.locationtech.jts.geom.Geometry geometry = wkbReader.read(wkbBytes);
            
            // Convertir a WKT
            org.locationtech.jts.io.WKTWriter wktWriter = new org.locationtech.jts.io.WKTWriter();
            return wktWriter.write(geometry);
        } catch (Exception e) {
            System.err.println("ERROR al convertir WKB a WKT: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Convierte un string hexadecimal a array de bytes
     */
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}