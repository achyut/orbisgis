package org.orbisgis.core.renderer.se.parameter.real;

import org.gdms.data.DataSource;
import org.orbisgis.core.renderer.se.parameter.ParameterException;
import org.orbisgis.core.renderer.se.parameter.SeParameter;

public interface RealParameter extends SeParameter {

    /*
     * TODO Is (DataSource, featureId) the right way to access a feature ?
     */
    public abstract double getValue(DataSource ds, long fid) throws ParameterException;
}