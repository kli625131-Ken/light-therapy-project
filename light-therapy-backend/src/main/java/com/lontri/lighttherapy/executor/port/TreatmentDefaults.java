package com.lontri.lighttherapy.executor.port;

public interface TreatmentDefaults {
    int defaultDim(); // 50
    int defaultSumdim(); // 50
    int defaultSkydim(); // 50
    int defaultCctK();              // 4000
    int defaultSkycctK();              // 4000

    double luxToPercentCoefficient(); // Conversion coefficient from lux to percentage
}