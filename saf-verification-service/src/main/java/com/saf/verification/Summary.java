package com.saf.verification;

public class Summary {
    private int totalPredios;
    private int totalPrediosConInterseccion;
    private String timestamp;

    public int getTotalPredios() {
        return totalPredios;
    }

    public void setTotalPredios(int totalPredios) {
        this.totalPredios = totalPredios;
    }

    public int getTotalPrediosConInterseccion() {
        return totalPrediosConInterseccion;
    }

    public void setTotalPrediosConInterseccion(int totalPrediosConInterseccion) {
        this.totalPrediosConInterseccion = totalPrediosConInterseccion;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}