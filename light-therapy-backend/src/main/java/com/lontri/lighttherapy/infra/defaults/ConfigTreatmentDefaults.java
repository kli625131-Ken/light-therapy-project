package com.lontri.lighttherapy.infra.defaults;

import com.lontri.lighttherapy.executor.port.TreatmentDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConfigTreatmentDefaults implements TreatmentDefaults {

    @Value("${treatment.default.dim:50}")
    private int dim;

    @Value("${treatment.default.cctK:4000}")
    private int cctK;

    @Value("${treatment.default.skydim:50}")
    private int skydim;

    @Value("${treatment.conversion.lux-to-percent-coefficient:1.0}")
    private double luxToPercentCoefficient;

    @Override
    public int defaultDim() {
        return dim;
    }

    @Override
    public int defaultSkydim() {
        return skydim;
    }
    @Override
    public int defaultSumdim() {
        return dim;
    }
    @Override
    public int defaultCctK() {
        return cctK;
    }
    @Override
    public int defaultSkycctK() {
        return cctK;
    }

    @Override
    public double luxToPercentCoefficient() {
        return luxToPercentCoefficient;
    }
}
