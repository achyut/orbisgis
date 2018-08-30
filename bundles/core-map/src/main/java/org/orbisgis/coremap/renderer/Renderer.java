/**
 * OrbisGIS is a java GIS application dedicated to research in GIScience.
 * OrbisGIS is developed by the GIS group of the DECIDE team of the 
 * Lab-STICC CNRS laboratory, see <http://www.lab-sticc.fr/>.
 *
 * The GIS group of the DECIDE team is located at :
 *
 * Laboratoire Lab-STICC – CNRS UMR 6285
 * Equipe DECIDE
 * UNIVERSITÉ DE BRETAGNE-SUD
 * Institut Universitaire de Technologie de Vannes
 * 8, Rue Montaigne - BP 561 56017 Vannes Cedex
 * 
 * OrbisGIS is distributed under GPL 3 license.
 *
 * Copyright (C) 2007-2014 CNRS (IRSTV FR CNRS 2488)
 * Copyright (C) 2015-2017 CNRS (Lab-STICC UMR CNRS 6285)
 *
 * This file is part of OrbisGIS.
 *
 * OrbisGIS is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * OrbisGIS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * OrbisGIS. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.orbisgis.coremap.renderer;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.h2gis.utilities.SpatialResultSet;
import org.h2gis.utilities.SpatialResultSetMetaData;
import org.locationtech.jts.geom.GeometryFactory;
import org.orbisgis.commons.progress.NullProgressMonitor;
import org.orbisgis.commons.progress.ProgressMonitor;
import org.orbisgis.corejdbc.ReadRowSet;
import org.orbisgis.coremap.layerModel.ILayer;
import org.orbisgis.coremap.layerModel.LayerException;
import org.orbisgis.coremap.map.MapTransform;
import org.orbisgis.coremap.renderer.se.AreaSymbolizer;
import org.orbisgis.coremap.renderer.se.Rule;
import org.orbisgis.coremap.renderer.se.Style;
import org.orbisgis.coremap.renderer.se.Symbolizer;
import org.orbisgis.coremap.renderer.se.VectorSymbolizer;
import org.orbisgis.coremap.renderer.se.fill.SolidFill;
import org.orbisgis.coremap.renderer.se.parameter.ParameterException;
import org.orbisgis.coremap.renderer.se.stroke.PenStroke;
import org.orbisgis.coremap.renderer.se.visitors.FeaturesVisitor;
import org.orbisgis.coremap.stream.GeoStream;
import org.orbisgis.coremap.ui.editors.map.tool.Rectangle2DDouble;
import org.slf4j.*;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

/**
 * Renderer contains all the logic of the Symbology Encoding process based on java
 * Graphics2D. This is an abstract class and subclasses provided effectives methods
 * according to the rendering target (e.g. bitmap image, SVG, pdf, etc.)
 *
 * @author Maxence Laurent
 */
public abstract class Renderer {

        private static final Logger LOGGER = LoggerFactory.getLogger(Renderer.class);
        private static final I18n I18N = I18nFactory.getI18n(Renderer.class);
        private ResultSetProviderFactory rsProvider = null;

        /**
         * Change the way this renderer gather the table content of a layer.
         * @param rsProvider result set provider instance.
         */
        public void setRsProvider(ResultSetProviderFactory rsProvider) {
            this.rsProvider = rsProvider;
        }

        /**
        * This method shall returns a graphics2D for each symbolizers in the list.
        * This is useful to make the diff bw pdf purpose and image purpose Is
        * called just before a new layer is drawn
        *
        * @param symbs
        * @param g2
        * @param mt
        */
        protected abstract void initGraphics2D(List<Symbolizer> symbs, Graphics2D g2,
            MapTransform mt);

        /**
         * Gets the {@code Graphics2D} instance that is associated to the {@code
         * Symbolizer s}.
         * @param s
         * @return
         */
        protected abstract Graphics2D getGraphics2D(Symbolizer s);

        protected abstract void releaseGraphics2D(Graphics2D g2);

        /**
         * Is called once the layer has been rendered
         * @param g2 the graphics the layer has to be drawn on
         */
        protected abstract void disposeLayer(Graphics2D g2);

            

        /**
         * Draws the content of the Vector Layer
         *
         * @param g2
         *            Object to draw to
         * @param mt
         * @param layer
         *            Source of information
         * @param pm
         *            Progress monitor to report the status of the drawing
         * @return the number of rendered objects
         * @throws java.sql.SQLException
         */
        public int drawVector(Graphics2D g2, MapTransform mt, ILayer layer,
                ProgressMonitor pm) throws SQLException {
                Envelope extent = mt.getAdjustedExtent();
                int layerCount = 0;
                List<Style> styles = layer.getStyles();
                for(Style style : styles){
                        layerCount +=drawStyle(style, g2, mt, layer, pm, extent);
                }
                return layerCount;
        }        

        /**
         * 
         * @param style
         * @param g2
         * @param mt
         * @param layer
         * @param pm
         * @param extent
         * @return
         * @throws SQLException 
         */
        private int drawStyle(Style style, Graphics2D g2,MapTransform mt, ILayer layer,
                              ProgressMonitor pm, Envelope extent) throws SQLException {
            int layerCount = 0;            
            //If the extend of the datasource is lower that a pixel then draw a 
            //single polygon to display a pixel
            Rectangle2DDouble layerEnv = mt.toPixel(layer.getEnvelope());
            if ((layerEnv.getHeight() <= mt.getMaxPixelDisplay())
                    && (layerEnv.getWidth() <= mt.getMaxPixelDisplay())) {
                AreaSymbolizer areaSymbolizer = new AreaSymbolizer();
                areaSymbolizer.setFill(new SolidFill(Color.BLACK));
                PenStroke ps = new PenStroke();
                ps.setFill(new SolidFill(Color.BLACK));
                areaSymbolizer.setStroke(ps);
                GeometryFactory gf = new GeometryFactory();
                try {
                    LinkedList<Symbolizer> symbs = new LinkedList<Symbolizer>();
                    symbs.add(areaSymbolizer);
                    initGraphics2D(symbs, g2, mt);
                    drawFeature(areaSymbolizer, gf.toGeometry(layer.getEnvelope()), null, 0, extent, false, mt);
                    disposeLayer(g2);
                } catch (ParameterException | IOException ex) {
                    printEx(ex, layer, g2);
                }
            }
            else{
            LinkedList<Symbolizer> symbs = new LinkedList<Symbolizer>();
            ResultSetProviderFactory layerDataFactory = rsProvider;
            if(layerDataFactory == null) {
                if(layer.getDataManager() != null && layer.getDataManager().getDataSource() != null) {
                    layerDataFactory = new DefaultResultSetProviderFactory();
                } else {
                    throw new SQLException("There is neither a ResultSetProviderFactory instance nor available DataSource in the vectorial layer");
                }
            }
            try {
                // i.e. TextSymbolizer are always drawn above all other layer !! Should now be handle with symbolizer level
                // Standard rules (with filter or no filter but not with elsefilter)
                LinkedList<Rule> rList = new LinkedList<Rule>();
                // Rule with ElseFilter
                LinkedList<Rule> fRList = new LinkedList<Rule>();
                // fetch symbolizers and rules
                style.getSymbolizers(mt, symbs, rList, fRList);
                // Create new dataSource with only feature in current extent
                Set<Long> selectedRows = layer.getSelection();
                // And now, features will be rendered
                // Get a graphics for each symbolizer
                initGraphics2D(symbs, g2, mt);
                ProgressMonitor rulesProgress = pm.startTask(rList.size());
                for (Rule r : rList) { 
                    FeaturesVisitor fv  = new FeaturesVisitor();
                    fv.visitSymbolizerNode(r);
                    Set<String> fields = fv.getResult();
                    try(ResultSetProviderFactory.ResultSetProvider resultSetProvider = layerDataFactory.getResultSetProvider(layer, rulesProgress)) {
                        try(SpatialResultSet rs = resultSetProvider.execute(rulesProgress, extent, fields)) {
                            //Workaround because H2 linked table doesn't contains PK or _ROWID_
                            String pkName = resultSetProvider.getPkName();
                            int pkColumn = -1;
                            if(pkName != null && !pkName.isEmpty()) {
                                pkColumn = rs.findColumn(resultSetProvider.getPkName());
                            }
                            //End workaround
                            int fieldID = rs.getMetaData().unwrap(SpatialResultSetMetaData.class).getFirstGeometryFieldIndex();
                            ProgressMonitor rowSetProgress;
                            // Read row count for progress monitor
                            if(rs instanceof ReadRowSet) {
                                rowSetProgress = rulesProgress.startTask("Drawing " + layer.getName() + " (Rule " + r.getName() + ")", ((ReadRowSet) rs).getRowCount());
                            } else {
                                rowSetProgress = rulesProgress.startTask("Drawing " + layer.getName() + " (Rule " + r.getName() + ")", 1);
                            }
                            while (rs.next()) {
                                if (rulesProgress.isCancelled()) {
                                    break;
                                }
                                Geometry theGeom = null;
                                // If there is only one geometry, it is fetched now, otherwise, it up to symbolizers
                                // to retrieve the correct geometry (through the Geometry attribute)
                                if (fieldID >= 0) {
                                    theGeom = rs.getGeometry(fieldID);
                                }
                                // Do not display the geometry when the envelope
                                //doesn't intersect the current mapcontext area.
                                if (theGeom == null || theGeom.getEnvelopeInternal().intersects(extent)) {
                                    //Workaround because H2 linked table doesn't contains PK or _ROWID_
                                    long row = -1;
                                    if(pkColumn != -1){
                                        row = rs.getLong(pkColumn);
                                    }
                                    //End workaround
                                    boolean selected = selectedRows.contains(row);
                                    List<Symbolizer> sl = r.getCompositeSymbolizer().getSymbolizerList();
                                    for (Symbolizer s : sl) {
                                         drawFeature(s, theGeom, rs, row,extent, selected, mt);
                                    }
                                }
                                rowSetProgress.endTask();
                            }
                        }
                    } catch (SQLException ex) {
                        if(!rulesProgress.isCancelled()) {
                            printEx(ex, layer, g2);
                        }
                    
                }
                    rulesProgress.endTask();
                }
                disposeLayer(g2);
            } catch (ParameterException | IOException ex) {
                printEx(ex, layer, g2);
            }
            }
            return layerCount;
        }

        /**
         * Draw the feature
         * @param s
         * @param geom
         * @param rs
         * @param rowIdentifier
         * @param extent
         * @param selected
         * @param mt
         * @throws ParameterException
         * @throws IOException
         * @throws SQLException 
         */
        private void drawFeature(Symbolizer s, Geometry geom, ResultSet rs,
                        long rowIdentifier, Envelope extent, boolean selected,
                        MapTransform mt) throws ParameterException,
                        IOException, SQLException {
                Geometry theGeom = geom;
                boolean somethingReached = false;
                if(theGeom == null){
                        //We try to retrieve a geometry. If we fail, an
                        //exception will be thrown by the call to draw,
                        //and a message will be shown to the user...
                        VectorSymbolizer vs = (VectorSymbolizer)s;
                        theGeom = vs.getGeometry(rs, rowIdentifier);
                        if(theGeom != null && theGeom.getEnvelopeInternal().intersects(extent)){
                                somethingReached = true;
                        }
                }
                if(somethingReached || theGeom != null){
                        Graphics2D g2S;
                        g2S = getGraphics2D(s);
                        s.draw(g2S, rs, rowIdentifier, selected, mt, theGeom);
                        releaseGraphics2D(g2S);
                }
        }

        private static void printEx(Exception ex, ILayer layer, Graphics2D g2) {
                LOGGER.warn("Could not draw " +layer.getName(), ex);
//                g2.setColor(Color.red);
//                g2.drawString(ex.toString(), EXECP_POS, EXECP_POS);
        }

        public void draw(Graphics2D g2dMap, int width, int height,
                Envelope extent, ILayer layer, ProgressMonitor pm) {
                MapTransform mt = new MapTransform();
                mt.resizeImage(width, height);
                mt.setExtent(extent);

                this.draw(mt, g2dMap, width, height, layer, pm);
        }

        /**
         * Draws the content of the layer in the specified graphics
         *
         * @param mt
         *            Drawing parameters
         * @param layer
         *            Source of information
         * @param pm
         *            Progress monitor to report the status of the drawing
         */
        public void draw(MapTransform mt,
                ILayer layer, ProgressMonitor pm) {

                BufferedImage image = mt.getImage();
                Graphics2D g2 = image.createGraphics();

                this.draw(mt, g2, image.getWidth(), image.getHeight(), layer, pm);
        }

        /**
         * Draws the content of the layer in the specified graphics
         *
         * @param g2
         *            Object to draw to
         * @param width
         *            Width of the generated image
         * @param height
         *            Height of the generated image
         * @param lay
         *            Source of information
         * @param progressMonitor
         *            Progress monitor to report the status of the drawing
         */
        public void draw(MapTransform mt, Graphics2D g2, int width, int height,
                ILayer lay, ProgressMonitor progressMonitor) {

                g2.setRenderingHints(mt.getRenderingHints());

                Envelope extent = mt.getAdjustedExtent();


                ILayer[] layers;

                //ArrayList<Symbolizer> overlay = new ArrayList<Symbolizer>();

                if (lay.acceptsChilds()) {
                        layers = lay.getLayersRecursively();
                } else {
                        layers = new ILayer[]{lay};
                }

                // long total1 = System.currentTimeMillis();
                int numLayers = layers.length;
                ProgressMonitor pm;
                if (progressMonitor == null) {
                    pm = new NullProgressMonitor();
                } else {
                    pm = progressMonitor.startTask(numLayers);
                }
                for (int i = numLayers - 1; i >= 0; i--) {
                        if (pm.isCancelled()) {
                                break;
                        } else {
                                ILayer layer = layers[i];
                                if (layer.isVisible() && extent.intersects(layer.getEnvelope())) {
                                        try {
                                                if (layer.isStream()) {
                                                    drawStreamLayer(g2, layer, width, height, extent, pm);
                                                } else if(layer.isVectorial()) {
                                                    drawVector(g2, mt, layer, pm);
                                                }
                                                // TODO
                                                // if (layer.isRaster()) {
                                                // this.drawRaster(g2, mt, layer,width,height, pm, perm);
                                        } catch (SQLException | LayerException e) {
                                                LOGGER.error(I18N.tr("Layer {0} not drawn",layer.getName()), e);
                                        }
                                }
                        }
                        pm.endTask();
                }
        }

        private void drawStreamLayer(Graphics2D g2, ILayer layer, int width, int height, Envelope extent, ProgressMonitor pm) {
                try {
                        layer.open();
                        GeoStream geoStream = layer.getStream();
                        Image img = geoStream.getMap(width, height, extent, pm);
                        g2.drawImage(img, 0, 0, null);
                } catch (LayerException | IOException e) {
                        LOGGER.error(
                                I18N.tr("Cannot get Stream image"), e);
                }
        }

        /**
         * Draws the content of the layer in the specified image.
         *
         * @param img
         *            Image to draw the data
         * @param extent
         *            Extent of the data to draw in the image
         * @param layer
         *            Layer to get the information
         * @param pm
         *            Progress monitor to report the status of the drawing
         */
        public void draw(BufferedImage img, Envelope extent, ILayer layer,
                ProgressMonitor pm) {
                MapTransform mt = new MapTransform();
                mt.setExtent(extent);
                mt.setImage(img);
                draw(mt, layer, pm);
        }
     /**
     * A workarround to draw a rasterlayer This method wil be updated with the
     * RasterSymbolizer
     *
     * @param g2
     * @param mt
     * @param layer
     * @param width
     * @param height
     * @param pm
     */
    private void drawRaster(Graphics2D g2, MapTransform mt, ILayer layer, int width, int height, ProgressMonitor pm) throws SQLException {
        //TODO Raster with h2
        throw new UnsupportedOperationException("Not supported yet.");
        /*

        GraphicsConfiguration configuration = null;
        boolean isHeadLess = GraphicsEnvironment.isHeadless();
        if (!isHeadLess) {
            configuration = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        }
        DataSource ds = layer.getTableReference();
        long rowCount = ds.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            GeoRaster geoRaster = ds.getRaster(i);
            Envelope layerEnvelope = geoRaster.getMetadata().getEnvelope();
            BufferedImage layerImage;
            if (isHeadLess) {
                layerImage = new BufferedImage(width, height,
                        BufferedImage.TYPE_INT_ARGB);
            } else {
                layerImage = configuration.createCompatibleImage(width,
                        height, BufferedImage.TYPE_INT_ARGB);
            }

            // part or all of the GeoRaster is visible
            Rectangle2DDouble layerPixelEnvelope = mt.toPixel(layerEnvelope);
            Graphics2D gLayer = layerImage.createGraphics();

            
            try {
                ColorModel cm = geoRaster.getDefaultColorModel();
                Image dataImage = geoRaster.getImage(cm);
                gLayer.drawImage(dataImage, (int) layerPixelEnvelope.getMinX(),
                        (int) layerPixelEnvelope.getMinY(),
                        (int) layerPixelEnvelope.getWidth() + 1,
                        (int) layerPixelEnvelope.getHeight() + 1, null);

            } catch (IOException ex) {
                layerImage = createEmptyImage(width, height);
            }

            g2.drawImage(layerImage, 0, 0, null);
        }
         */
    }
    
    /**
     * A simple method to display an empty image
     *
     * @param width
     * @param height
     * @return
     */
    private BufferedImage createEmptyImage(int width, int height) {
        final String noImage = "Image Unavailable";

        if (width == 0 || height == 0) {
            return null;
        }
        BufferedImage bufferedImage =
                new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.setBackground(Color.WHITE);

        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.BLACK);

        // Create our font
        Font font = new Font("SansSerif", Font.PLAIN, 18);
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics();

        int length = metrics.stringWidth(noImage);
        while (length + 6 >= width) {
            font = font.deriveFont((float) (font.getSize2D() * 0.9)); // Scale our font
            graphics.setFont(font);
            metrics = graphics.getFontMetrics();
            length = metrics.stringWidth(noImage);
        }

        int lineHeight = metrics.getHeight();

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawString(noImage, (width - length) / 2, (height + lineHeight) / 2);

        return bufferedImage;
    }
}
