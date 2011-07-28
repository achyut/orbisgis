/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.orbisgis.core.renderer.se.graphic;


import com.sun.media.jai.widget.DisplayJAI;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.media.jai.RenderableGraphics;

import javax.swing.JFrame;

import junit.framework.TestCase;
import org.orbisgis.core.ConsoleErrorManager;
import org.orbisgis.core.ConsoleOutputManager;
import org.orbisgis.core.Services;
import org.orbisgis.core.errorManager.ErrorManager;
import org.orbisgis.core.map.MapTransform;
import org.orbisgis.core.renderer.se.FeatureTypeStyle;
import org.orbisgis.core.renderer.se.PointSymbolizer;
import org.orbisgis.core.renderer.se.SeExceptions.InvalidStyle;
import org.orbisgis.core.renderer.se.common.Uom;
import org.orbisgis.core.renderer.se.parameter.ParameterException;
import org.orbisgis.core.ui.plugins.views.output.OutputManager;

/**
 *
 * @author maxence
 */
public class GraphicCollectionTest extends TestCase {

    private FeatureTypeStyle fts;

    public GraphicCollectionTest(String testName) throws IOException {
        super(testName);
    }

    protected ConsoleErrorManager failErrorManager;
    protected ConsoleOutputManager failOutput;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        failErrorManager = new ConsoleErrorManager();
        Services.registerService(ErrorManager.class, "", failErrorManager);
        failOutput = new ConsoleOutputManager();
        Services.registerService(OutputManager.class, "", failOutput);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGraphic() throws IOException, ParameterException, InvalidStyle {
        JFrame frame = new JFrame();
        frame.setTitle("Test GraphicCollection");

        // Get the JFrame’s ContentPane.
        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new BorderLayout());

        // Create an instance of DisplayJAI.
        DisplayJAI dj = new DisplayJAI();

        System.out.println(dj.getColorModel());

        fts = new FeatureTypeStyle(null, "src/test/resources/org/orbisgis/core/renderer/se/graphics.se");
        PointSymbolizer ps = (PointSymbolizer) fts.getRules().get(0).getCompositeSymbolizer().getSymbolizerList().get(0);
        GraphicCollection collec = ps.getGraphicCollection();


		MapTransform mt = new MapTransform();
        double width = Uom.toPixel(270, Uom.MM, mt.getDpi(), null, null);
        double height = Uom.toPixel(160, Uom.MM, mt.getDpi(), null, null);


        //Rectangle2D.Double dim = new Rectangle2D.Double(-width/2, -height/2, width, height);
        BufferedImage img = new BufferedImage((int)width, (int)height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D rg = img.createGraphics();
        rg.setRenderingHints(mt.getRenderingHints());

        collec.draw(rg, null, -1, false, mt, AffineTransform.getTranslateInstance(width/2, height/2));

        rg.setStroke(new BasicStroke(1));
        rg.setPaint(Color.BLACK);

        rg.drawLine(0, (int)height/2, (int)width, (int)height/2);
        rg.drawLine((int)width/2, 0, (int)width/2, (int)height);

        dj.setBounds(0, 0, (int)width, (int)height);
        //dj.setBounds((int)rg.getMinX(), (int)rg.getMinY(), (int)rg.getWidth(), (int)rg.getHeight());

        //RenderedImage r = rg.createRendering(mt.getCurrentRenderContext());

        dj.set(img, 0,0);

        File file = new File("/tmp/graphics.png");
        ImageIO.write(img, "png", file);

        // Add to the JFrame’s ContentPane an instance of JScrollPane
        // containing the DisplayJAI instance.
        //contentPane.add(new JScrollPane(dj), BorderLayout.CENTER);
        contentPane.add(dj, BorderLayout.CENTER);

        // Set the closing operation so the application is finished.
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize((int)width, (int)height+24); // adjust the frame size.
        frame.setVisible(true); // show the frame.

        System.out.print("");
        
        try {
            Thread.sleep(20000);
        } catch (InterruptedException ex) {
            Logger.getLogger(GraphicCollectionTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
}