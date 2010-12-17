package org.orbisgis.core.renderer.se.fill;

import java.awt.Graphics2D;
import java.awt.Paint;

import java.awt.Shape;
import java.awt.TexturePaint;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.RenderableGraphics;
import javax.xml.bind.JAXBElement;
import org.gdms.data.SpatialDataSourceDecorator;

import org.orbisgis.core.renderer.persistance.se.GraphicFillType;
import org.orbisgis.core.renderer.persistance.se.ObjectFactory;
import org.orbisgis.core.renderer.persistance.se.TileGapType;

import org.gdms.data.feature.Feature;
import org.orbisgis.core.map.MapTransform;
import org.orbisgis.core.renderer.se.SeExceptions.InvalidStyle;
import org.orbisgis.core.renderer.se.UomNode;
import org.orbisgis.core.renderer.se.common.Uom;
import org.orbisgis.core.renderer.se.graphic.GraphicCollection;
import org.orbisgis.core.renderer.se.parameter.ParameterException;
import org.orbisgis.core.renderer.se.parameter.SeParameterFactory;
import org.orbisgis.core.renderer.se.parameter.real.RealParameter;
import org.orbisgis.core.renderer.se.parameter.real.RealParameterContext;

public final class GraphicFill extends Fill implements UomNode {

    private GraphicCollection graphic;
    private Uom uom;
    private RealParameter gapX;
    private RealParameter gapY;

    public GraphicFill() {
        this.setGapX(null);
        this.setGapY(null);
    }

    public GraphicFill(GraphicFillType gft) throws InvalidStyle {
        if (gft.getGraphic() != null) {
            this.setGraphic(new GraphicCollection(gft.getGraphic(), this));
        }

        if (gft.getTileGap() != null) {
            TileGapType gap = gft.getTileGap();
            if (gap.getX() != null) {
                this.setGapX(SeParameterFactory.createRealParameter(gap.getX()));
            }
            if (gap.getY() != null) {
                this.setGapY(SeParameterFactory.createRealParameter(gap.getY()));
            }
        }

        if (gft.getUnitOfMeasure() != null) {
            this.setUom(Uom.fromOgcURN(gft.getUnitOfMeasure()));
        }
    }

    GraphicFill(JAXBElement<GraphicFillType> f) throws InvalidStyle {
        this(f.getValue());
    }

    public void setGraphic(GraphicCollection graphic) {
        this.graphic = graphic;
        graphic.setParent(this);
    }

    public GraphicCollection getGraphic() {
        return graphic;
    }

	@Override
    public void setUom(Uom uom) {
        this.uom = uom;
    }

	@Override
	public Uom getOwnUom(){
		return uom;
	}

    @Override
    public Uom getUom() {
        if (uom == null) {
            return parent.getUom();
        } else {
            return uom;
        }
    }

    public void setGapX(RealParameter gap) {
        gapX = gap;
		if (gap != null){
			gap.setContext(RealParameterContext.realContext);
		}
    }

    public void setGapY(RealParameter gap) {
        gapY = gap;
		if (gap != null){
			gap.setContext(RealParameterContext.realContext);
		}
    }

    public RealParameter getGapX() {
        return gapX;
    }

    public RealParameter getGapY() {
        return gapY;
    }

    /**
     * see Fill
     */
    @Override
    public void draw(Graphics2D g2, SpatialDataSourceDecorator sds, long fid, Shape shp, boolean selected, MapTransform mt) throws ParameterException, IOException {
        Paint stipple = this.getPaint(fid, sds, selected, mt);

        // TODO handle selected ! 
        if (stipple != null) {
            g2.setPaint(stipple);
            g2.fill(shp);
        }
    }

    /**
     * Create a new TexturePaint according to this GraphicFill
     * 
     * @param ds DataSource
     * @param fid feature id
     * @return a TexturePain ready to be used
     * @throws ParameterException
     * @throws IOException
     */
	@Override
    public Paint getPaint(long fid, SpatialDataSourceDecorator sds, boolean selected, MapTransform mt) throws ParameterException {
        RenderableGraphics img;
		try {
			img = graphic.getGraphic(sds, fid, selected, mt);
		} catch (IOException ex) {
			Logger.getLogger(GraphicFill.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}

        if (img != null) {
            double gX = 0.0;
            double gY = 0.0;

            if (gapX != null) {
                gX = gapX.getValue(sds, fid);
                if (gX < 0.0) {
                    gX = 0.0;
                }
            }

            if (gapY != null) {
                gY = gapY.getValue(sds, fid);
                if (gY < 0.0) {
                    gY = 0.0;
                }
            }

            gX = Uom.toPixel(gX, getUom(), mt.getDpi(), mt.getScaleDenominator(), (double)img.getWidth());
            gY = Uom.toPixel(gY, getUom(), mt.getDpi(), mt.getScaleDenominator(), (double)img.getHeight());

            BufferedImage i = new BufferedImage((int) (img.getWidth() + gX), (int) (img.getHeight() + gY), BufferedImage.TYPE_INT_ARGB);
            Graphics2D tile = i.createGraphics();

            tile.drawRenderedImage(img.createRendering(mt.getCurrentRenderContext()), AffineTransform.getTranslateInstance(-img.getMinX() + gX / 2.0, -img.getMinY() + gY / 2.0));

            return new TexturePaint(i, new Rectangle2D.Double(0, 0, i.getWidth(), i.getHeight()));
        } else {
            return null;
        }
    }


    @Override
    public boolean dependsOnFeature() {
        if (gapX != null && this.gapX.dependsOnFeature())
            return true;
        if (gapY != null && this.gapY.dependsOnFeature())
            return true;
        if (graphic != null && this.graphic.dependsOnFeature())
            return true;

        return false;
    }

    @Override
    public GraphicFillType getJAXBType() {
        GraphicFillType f = new GraphicFillType();

        if (uom != null) {
            f.setUnitOfMeasure(uom.toURN());
        }

        if (graphic != null) {
            f.setGraphic(graphic.getJAXBElement());
        }

        if (gapX != null && gapY != null) {
            TileGapType tile = new TileGapType();
            if (gapX != null) {
                tile.setX(gapX.getJAXBParameterValueType());
            }
            if (gapY != null) {
                tile.setY(gapY.getJAXBParameterValueType());
            }
            f.setTileGap(tile);
        }

        return f;
    }

    @Override
    public JAXBElement<GraphicFillType> getJAXBElement() {
        ObjectFactory of = new ObjectFactory();
        return of.createGraphicFill(this.getJAXBType());
    }
}