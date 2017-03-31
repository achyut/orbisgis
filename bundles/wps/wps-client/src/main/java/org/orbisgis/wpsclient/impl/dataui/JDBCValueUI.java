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

package org.orbisgis.wpsclient.impl.dataui;

import net.miginfocom.swing.MigLayout;
import net.opengis.wps._2_0.DescriptionType;
import net.opengis.wps._2_0.InputDescriptionType;
import net.opengis.wps._2_0.OutputDescriptionType;
import org.orbisgis.commons.progress.SwingWorkerPM;
import org.orbisgis.sif.common.ContainerItem;
import org.orbisgis.wpsclient.impl.WpsClientImpl;
import org.orbisgis.wpsclient.api.dataui.DataUI;
import org.orbisgis.wpsclient.impl.utils.ToolBoxIcon;
import org.orbisgis.wpsclient.impl.utils.WaitLayerUI;
import org.orbisgis.wpsservice.model.*;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.LayerUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.EventHandler;
import java.beans.PropertyChangeEvent;
import java.math.BigInteger;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * DataUI implementation for JDBCValue.
 * This class generate an interactive UI dedicated to the configuration of a JDBCTableFieldValue.
 * The interface generated will be used in the ProcessEditor.
 *
 * @author Sylvain PALOMINOS
 * @author Erwan Bocher
 **/

public class JDBCValueUI implements DataUI {

    /** Size constants **/
    private static final int JLIST_VERTICAL_MAX_ROW_COUNT = 10;
    private static final int JLIST_HORIZONTAL_MAX_ROW_COUNT = 3;
    private static final int JLIST_MIN_ROW_COUNT = 1;

    /** Constant used to pass object as client property throw JComponents **/
    private static final String DATA_MAP_PROPERTY = "DATA_MAP_PROPERTY";
    private static final String URI_PROPERTY = "URI_PROPERTY";
    private static final String VALUE_PROPERTY = "VALUE_PROPERTY";
    private static final String IS_OPTIONAL_PROPERTY = "IS_OPTIONAL_PROPERTY";
    private static final String INITIAL_DELAY_PROPERTY = "INITIAL_DELAY_PROPERTY";
    private static final String TOOLTIP_TEXT_PROPERTY = "TOOLTIP_TEXT_PROPERTY";
    private static final String LAYERUI_PROPERTY = "LAYERUI_PROPERTY";
    private static final String ORIENTATION_PROPERTY = "ORIENTATION_PROPERTY";
    private static final String MAX_JLIST_ROW_COUNT = "MAX_JLIST_ROW_COUNT";
    private static final String DEFAULT_ELEMENT_PROPERTY = "DEFAULT_ELEMENT_PROPERTY";
    /** I18N object */
    private static final I18n I18N = I18nFactory.getI18n(JDBCValueUI.class);

    /** WpsClient using the generated UI. */
    private WpsClientImpl wpsClient;

    @Override
    public void setWpsClient(WpsClientImpl wpsClient){
        this.wpsClient = wpsClient;
    }

    @Override
    public JComponent createUI(DescriptionType inputOrOutput, Map<URI, Object> dataMap, Orientation orientation) {
        JPanel panel = new JPanel(new MigLayout("fill, ins 0, gap 0"));
        JDBCValue jdbcValue = null;
        //Retrieve the JDBCTableFieldValue and if it is optional
        boolean isOptional = false;
        if(inputOrOutput instanceof InputDescriptionType){
            jdbcValue = (JDBCValue)((InputDescriptionType)inputOrOutput).getDataDescription().getValue();
            if(((InputDescriptionType)inputOrOutput).getMinOccurs().equals(new BigInteger("0"))){
                isOptional = true;
            }
        }
        else if(inputOrOutput instanceof OutputDescriptionType){
            return null;
        }

        if(jdbcValue == null){
            return panel;
        }
        //Build and set the JList containing all the field values
        JList<ContainerItem<Object>> list = new JList<>();
        DefaultListModel<ContainerItem<Object>> model = new DefaultListModel<>();
        list.setModel(model);
        if(jdbcValue.isMultiSelection()) {
            list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }
        else {
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }
        int maxRow;
        if(orientation.equals(Orientation.VERTICAL)){
            maxRow = JLIST_VERTICAL_MAX_ROW_COUNT;
        }
        else{
            maxRow = JLIST_HORIZONTAL_MAX_ROW_COUNT;
        }
        list.putClientProperty(MAX_JLIST_ROW_COUNT, maxRow);
        list.setVisibleRowCount(JLIST_MIN_ROW_COUNT);
        list.setLayoutOrientation(JList.VERTICAL);
        ContainerItem<Object> defaultElement = new ContainerItem<Object>("Select a value", I18N.tr("Select a value"));
        list.putClientProperty(DEFAULT_ELEMENT_PROPERTY, defaultElement);
        URI uri = URI.create(inputOrOutput.getIdentifier().getValue());
        list.putClientProperty(URI_PROPERTY, uri);
        list.putClientProperty(VALUE_PROPERTY, jdbcValue);
        list.putClientProperty(DATA_MAP_PROPERTY, dataMap);
        list.putClientProperty(IS_OPTIONAL_PROPERTY, isOptional);
        list.putClientProperty(ORIENTATION_PROPERTY, orientation);
        list.addMouseListener(EventHandler.create(MouseListener.class, this, "refreshList", "source", "mouseEntered"));
        list.addMouseListener(EventHandler.create(MouseListener.class, this, "onComboBoxExited", "source", "mouseExited"));
        list.addListSelectionListener(EventHandler.create(ListSelectionListener.class, this, "onListSelection", "source"));
        list.setToolTipText(inputOrOutput.getAbstract().get(0).getValue());

        if(!isOptional && dataMap.containsKey(uri)) {
            Object obj = dataMap.get(uri);
            if(obj instanceof String[]){
                String[] fields = (String[]) obj;
                int[] indexes = new int[fields.length];
                int i=0;
                for(String field : fields){
                    model.add(0, new ContainerItem<Object>(field, field));
                    indexes[i] = i;
                    i++;
                }
                list.setSelectedIndices(indexes);
            }
            else {
                model.add(0, new ContainerItem<>(dataMap.get(uri), dataMap.get(uri).toString()));
                list.setSelectedIndex(0);
            }
        }
        else{
            model.addElement(defaultElement);
        }
        if(model.getSize()>maxRow){
            list.setVisibleRowCount(maxRow);
        }
        else{
            list.setVisibleRowCount(model.getSize());
        }

        //Adds a WaitLayerUI which will be displayed when the toolbox is loading the data
        JScrollPane listScroller = new JScrollPane(list);
        WaitLayerUI layerUI = new WaitLayerUI();
        JLayer<JComponent> layer = new JLayer<>(listScroller, layerUI);
        panel.add(layer, "growx, wrap");
        list.putClientProperty(LAYERUI_PROPERTY, layerUI);

        return panel;
    }

    @Override
    public Map<URI, Object> getDefaultValue(DescriptionType inputOrOutput) {
        Map<URI, Object> map = new HashMap<>();
        JDBCValue jdbcValue = null;
        boolean isOptional = false;
        if(inputOrOutput instanceof InputDescriptionType){
            jdbcValue = (JDBCValue)((InputDescriptionType)inputOrOutput).getDataDescription().getValue();
            isOptional = ((InputDescriptionType)inputOrOutput).getMinOccurs().equals(new BigInteger("0"));
        }
        else if(inputOrOutput instanceof OutputDescriptionType){
            jdbcValue = (JDBCValue)((OutputDescriptionType)inputOrOutput).getDataDescription().getValue();
        }
        if(jdbcValue.getDefaultValues() != null) {
            map.put(URI.create(inputOrOutput.getIdentifier().getValue()), jdbcValue.getDefaultValues());
        }
        return map;
    }

    @Override
    public ImageIcon getIconFromData(DescriptionType inputOrOutput) {
        return ToolBoxIcon.getIcon(ToolBoxIcon.JDBC_VALUE);
    }

    /**
     * When the jList is exited, reset the tooltipText delay.
     * @param source JComboBox.
     */
    public void onComboBoxExited(Object source){
        //Retrieve the client properties
        JList<String> list = (JList)source;
        Object tooltipText = list.getClientProperty(TOOLTIP_TEXT_PROPERTY);
        if(tooltipText != null) {
            list.setToolTipText((String)tooltipText);
        }
        Object delay = list.getClientProperty(INITIAL_DELAY_PROPERTY);
        if(delay != null){
            ToolTipManager.sharedInstance().setInitialDelay((int)delay);
        }
    }

    /**
     * Update the JList according to if JDBCColumn parent.
     * @param source the source JList.
     */
    public void refreshList(Object source){
        //Instantiate a worker which will do the list update in a separeted swing thread
        FieldValueWorker worker = new FieldValueWorker((JList)source);
        ExecutorService executorService = wpsClient.getExecutorService();
        if(executorService != null){
            executorService.execute(worker);
        }
        else{
            worker.execute();
        }
    }

    public void onListSelection(Object source){
        JList list = (JList)source;
        URI uri = (URI)list.getClientProperty(URI_PROPERTY);
        HashMap<URI, Object> dataMap = (HashMap<URI, Object>)list.getClientProperty(DATA_MAP_PROPERTY);
        Boolean isOptional = (Boolean)list.getClientProperty(IS_OPTIONAL_PROPERTY);
        List<String> listValues = new ArrayList<>();

        if(list.getSelectedIndices().length == 0){
            if(isOptional) {
                dataMap.put(uri, null);
            }
            return;
        }
        else {
            for (int i : list.getSelectedIndices()) {
                listValues.add(list.getModel().getElementAt(i).toString().replaceAll("'", "''"));
            }
        }
        String str = "";
        for(String s : listValues){
            if(! str.isEmpty()){
                str+="\t";
            }
            str+=s;
        }
        dataMap.put(uri, str);
    }

    /**
     * SwingWorker doing the list update in a separated Swing Thread
     */
    private class FieldValueWorker extends SwingWorkerPM{
        private JList<ContainerItem<Object>> list;

        public FieldValueWorker(JList<ContainerItem<Object>> list){
            this.list = list;
        }

        @Override
        protected Object doInBackground() throws Exception {
            WaitLayerUI layerUI = (WaitLayerUI)list.getClientProperty(LAYERUI_PROPERTY);
            JDBCValue jdbcValue = (JDBCValue)list.getClientProperty(VALUE_PROPERTY);
            ContainerItem<Object> defaultElement = (ContainerItem<Object>)list.getClientProperty(DEFAULT_ELEMENT_PROPERTY);
            this.setTaskName(I18N.tr("Updating the column values"));
            Orientation orientation = (Orientation)list.getClientProperty(ORIENTATION_PROPERTY);
            int maxRow = (int) list.getClientProperty(MAX_JLIST_ROW_COUNT);
            URI uri = (URI) list.getClientProperty(URI_PROPERTY);
            Map<URI, Object> dataMap = (Map) list.getClientProperty(DATA_MAP_PROPERTY);
            boolean isOptional = (boolean)list.getClientProperty(IS_OPTIONAL_PROPERTY);
            DefaultListModel<ContainerItem<Object>> model = (DefaultListModel<ContainerItem<Object>>)list.getModel();
            //If the JDBCColumn related to the jdbcValue has been modified, reload the JDBCColumn values
            if(jdbcValue.isJDBCColumnModified()) {
                jdbcValue.setJDBCColumnModified(false);
                String tableName = null;
                String fieldName = null;
                String uriValue = jdbcValue.getJDBCColumnIdentifier().toString();
                if(uriValue.contains("$")){
                    String[] split = uriValue.split("\\$");
                    if(split.length == 4) {
                        tableName = split[1]+"."+split[2];
                        fieldName = split[3];
                    }
                    else if(split.length == 3){
                        tableName = split[1];
                        fieldName = split[2];
                    }
                    else{
                        return null;
                    }
                }
                else if (dataMap.get(jdbcValue.getJDBCTableIdentifier()) != null){
                    tableName = dataMap.get(jdbcValue.getJDBCTableIdentifier()).toString();
                    fieldName = dataMap.get(jdbcValue.getJDBCColumnIdentifier()).toString();
                }
                else if(jdbcValue.getJDBCTableIdentifier().toString().contains("$")){
                    String[] split = jdbcValue.getJDBCTableIdentifier().toString().split("\\$");
                    if(split.length == 3){
                        tableName = split[1]+"."+split[2];
                    }
                    else if(split.length == 2){
                        tableName = split[1];
                    }
                    Object fieldNameObject = dataMap.get(jdbcValue.getJDBCColumnIdentifier());
                    if(fieldNameObject != null) {
                        fieldName = fieldNameObject.toString();
                    }
                }
                if(tableName != null && fieldName != null) {
                    layerUI.start();
                    //First retrieve the good field name with the good case.
                    List<String> fieldList = wpsClient.getTableFieldList(tableName,
                            new ArrayList<DataType>(), new ArrayList<DataType>());
                    for(String field : fieldList){
                        if(field.equalsIgnoreCase(fieldName)){
                            fieldName = field;
                        }
                    }
                    //Retrieve the rowSet reading the table from the wpsService.
                    model.removeAllElements();
                    List<String> listFields = wpsClient.getFieldValueList(tableName, fieldName);
                    Collections.sort(listFields);
                    for (String field : listFields) {
                        model.addElement(new ContainerItem<Object>(field, field));
                    }
                    int maxRowCount;
                    if(orientation.equals(Orientation.VERTICAL)){
                        maxRowCount = JLIST_VERTICAL_MAX_ROW_COUNT;
                    }
                    else{
                        maxRowCount = JLIST_HORIZONTAL_MAX_ROW_COUNT;
                    }
                    if(listFields.size() < maxRowCount){
                        list.setVisibleRowCount(listFields.size());
                    }
                    else {
                        list.setVisibleRowCount(maxRowCount);
                    }
                    if (!isOptional && list.getModel().getSize() > 0) {
                        list.setSelectedIndex(0);
                        if(dataMap.containsKey(uri) && dataMap.get(uri) != null){
                            List<Integer> indexList = new ArrayList<>();
                            String[] elements = dataMap.get(uri).toString().split("\\t");
                            for (String element : elements) {
                                for (int i = 0; i < model.getSize(); i++) {
                                    if (model.getElementAt(i).getLabel().toUpperCase().equals(element.toUpperCase())) {
                                        indexList.add(i);
                                    }
                                }
                            }
                            int[] indexes = new int[indexList.size()];
                            for(int i=0; i<indexList.size(); i++){
                                indexes[i] = indexList.get(i);
                            }
                            list.setSelectedIndices(indexes);
                        }
                    }
                    layerUI.stop();
                }
            }

            //If the jList doesn't contains any values or contains only the default item,
            // it mean that the JDBCColumn hasn't been well selected.
            //So show a tooltip text to warn the user.
            if( list.getModel().getSize() == 0 ||
                    (list.getModel().getSize() == 1 && (list.getModel().getElementAt(0).equals(defaultElement))) ) {
                list.putClientProperty(INITIAL_DELAY_PROPERTY, ToolTipManager.sharedInstance().getInitialDelay());
                list.putClientProperty(TOOLTIP_TEXT_PROPERTY, list.getToolTipText());
                ToolTipManager.sharedInstance().setInitialDelay(0);
                ToolTipManager.sharedInstance().setDismissDelay(2500);
                String fieldValueStr = jdbcValue.getJDBCColumnIdentifier().toString();
                if(fieldValueStr.contains("$")){
                    String[] split = fieldValueStr.split("\\$");
                    if(split.length == 3){
                        fieldValueStr = split[1]+"."+split[2];
                    }
                    else if(split.length == 4){
                        fieldValueStr = split[1]+"."+split[2]+"."+split[3];
                    }
                    list.setToolTipText(I18N.tr("First configure the JDBCTableField {0}.", fieldValueStr));
                }
                else {
                    list.setToolTipText(I18N.tr("First configure the JDBCTableField {0}",
                            fieldValueStr.substring(fieldValueStr.lastIndexOf(":") + 1)));
                }
                if(model.getSize() == 0) {
                    model.addElement(defaultElement);
                }
                ToolTipManager.sharedInstance().mouseMoved(
                        new MouseEvent(list,MouseEvent.MOUSE_MOVED,System.currentTimeMillis(),0,0,0,0,false));
            }
            if(model.getSize()>maxRow){
                list.setVisibleRowCount(maxRow);
            }
            else{
                list.setVisibleRowCount(model.getSize());
            }
            list.revalidate();
            list.repaint();
            return null;
        }
    }
}