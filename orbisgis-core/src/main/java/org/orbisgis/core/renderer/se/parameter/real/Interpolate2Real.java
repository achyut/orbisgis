package org.orbisgis.core.renderer.se.parameter.real;

import javax.xml.bind.JAXBElement;
import org.gdms.data.feature.Feature;
import org.orbisgis.core.renderer.persistance.se.InterpolateType;
import org.orbisgis.core.renderer.persistance.se.InterpolationPointType;
import org.orbisgis.core.renderer.persistance.se.ModeType;
import org.orbisgis.core.renderer.se.parameter.Interpolate;
import org.orbisgis.core.renderer.se.parameter.InterpolationPoint;
import org.orbisgis.core.renderer.se.parameter.ParameterException;
import org.orbisgis.core.renderer.se.parameter.SeParameterFactory;

public final class Interpolate2Real extends Interpolate<RealParameter, RealLiteral> implements RealParameter {


	private Double min;
	private Double max;

    public Interpolate2Real(RealLiteral fallback, Double min, Double max) {
        super(fallback);
		this.setMinValue(min);
		this.setMaxValue(max);
    }

    public Interpolate2Real(JAXBElement<InterpolateType> expr) {
		super();
        InterpolateType t = expr.getValue();

        this.fallbackValue = new RealLiteral(t.getFallbackValue());
        this.setLookupValue(SeParameterFactory.createRealParameter(t.getLookupValue()));

        if (t.getMode() == ModeType.COSINE) {
            this.setInterpolationMode(InterpolationMode.COSINE);
        } else if (t.getMode() == ModeType.CUBIC) {
            this.setInterpolationMode(InterpolationMode.CUBIC);
        } else {
            this.setInterpolationMode(InterpolationMode.LINEAR);
        }

        for (InterpolationPointType ipt : t.getInterpolationPoint()){
            InterpolationPoint<RealParameter> ip = new InterpolationPoint<RealParameter>();

            ip.setData(ipt.getData());
            ip.setValue(SeParameterFactory.createRealParameter(ipt.getValue()));

            this.addInterpolationPoint(ip);
        }

    }

    @Override
    public double getValue(Feature feat) throws ParameterException {

		double value = this.lookupValue.getValue(feat);

		if (i_points.get(0).getData() > value){
			return i_points.get(0).getValue().getValue(feat);
		}

		if (i_points.get(i_points.size()-1).getData() < value){
			return i_points.get(i_points.size()-1).getValue().getValue(feat);
		}



		int k = getFirstIP(value);

		InterpolationPoint<RealParameter> ip1 = i_points.get(k);
		InterpolationPoint<RealParameter> ip2 = i_points.get(k+1);

		switch(this.mode){
		case CUBIC:
		case COSINE:
			return cosineInterpolation(ip1.getData(), ip2.getData(), value,
			ip1.getValue().getValue(feat), ip2.getValue().getValue(feat));
		case LINEAR:
			return linearInterpolation(ip1.getData(), ip2.getData(), value,
			ip1.getValue().getValue(feat), ip2.getValue().getValue(feat));

		}
        return 0.0; // TODO compute interpolation
    }

	@Override
	public void setFallbackValue(RealLiteral l){
		super.setFallbackValue(l);
		l.setMaxValue(max);
		l.setMinValue(min);
	}

	@Override
	public void addInterpolationPoint(InterpolationPoint<RealParameter> point){
		RealParameter value = point.getValue();
		value.setMaxValue(max);
		value.setMinValue(min);
		super.addInterpolationPoint(point);
	}

	@Override
	public void setMinValue(Double min) {
		this.min = min;
		this.fallbackValue.setMaxValue(min);
		for (InterpolationPoint<RealParameter> ip : this.i_points){
			RealParameter value = ip.getValue();
			value.setMinValue(min);
		}
	}

	@Override
	public void setMaxValue(Double max) {
		this.max = max;
		this.fallbackValue.setMaxValue(max);
		for (InterpolationPoint<RealParameter> ip : this.i_points){
			RealParameter value = ip.getValue();
			value.setMaxValue(max);
		}
	}

	public String toString(){
		return "NA";
	}
}
