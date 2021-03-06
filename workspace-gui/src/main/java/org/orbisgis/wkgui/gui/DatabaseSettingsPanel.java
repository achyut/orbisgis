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
package org.orbisgis.wkgui.gui;

import java.awt.Window;
import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.xml.bind.DatatypeConverter;

import net.miginfocom.swing.MigLayout;
import org.h2gis.utilities.JDBCUrlParser;
import org.orbisgis.frameworkapi.CoreWorkspace;
import org.orbisgis.sif.common.MenuCommonFunctions;
import org.orbisgis.sif.components.CustomButton;
import org.orbisgis.wkgui.icons.WKIcon;
import org.osgi.service.jdbc.DataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

/**
 * This class is used to manage the database connections used by OrbisGIS.
 * 
 * @author Erwan Bocher
 * @author Nicolas Fortin
 */
public class DatabaseSettingsPanel extends JDialog {

    private static final String DB_PROPERTIES_FILE = "db_connexions.properties";
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseSettingsPanel.class);
    protected static final I18n I18N = I18nFactory.getI18n(DatabaseSettingsPanel.class);
    private Properties dbProperties = new Properties();
    private JTextField connectionName;
    
    private String urlValue;
    private JTextField dbHost;
    private JTextField dbPort;
    private JTextField dbName;
    private JTextField userValue;
    private JCheckBox requirePassword;
    private JComboBox<Object> connectionsComboBox;
    private JComboBox<DB_TYPES> dbTypes;
    boolean canceled = false;
    private CoreWorkspace defaultCoreWorkspace;
    public static String DEFAULT_H2_PORT="8082";
    public static String DEFAULT_MESSAGE_H2=I18N.tr("Not required");
    private static final String URL_STARTS = "jdbc:";
    
    
    public enum DB_TYPES { H2GIS_EMBEDDED, H2GIS_SERVER, POSTGIS; }

    public DatabaseSettingsPanel(CoreWorkspace defaultCoreWorkspace) {
        super();
        this.defaultCoreWorkspace = defaultCoreWorkspace;
        init();
    }

    public DatabaseSettingsPanel(Window owner, CoreWorkspace defaultCoreWorkspace) {
        super(owner);
        this.defaultCoreWorkspace = defaultCoreWorkspace;
        init();
    }

    /**
     * Create the panel
     */
    private void init() {
        loadDBProperties();
        Object[] dbKeys = dbProperties.keySet().toArray();
        JPanel mainPanel = new JPanel(new MigLayout());
        JLabel cbLabel = new JLabel(I18N.tr("Saved connections"));
        connectionsComboBox = new JComboBox<Object>(dbKeys);
        connectionsComboBox.addActionListener(EventHandler.create(ActionListener.class, this, "onUserSelectionChange"));
        connectionsComboBox.setSelectedIndex(-1);
        mainPanel.add(cbLabel);
        mainPanel.add(connectionsComboBox, "width 200!");       
        CustomButton removeBt = new CustomButton(WKIcon.getIcon("remove"));
        removeBt.setToolTipText(I18N.tr("Remove the connection parameters"));
        removeBt.addActionListener(EventHandler.create(ActionListener.class, this, "onRemove")); 
        CustomButton refreshBt = new CustomButton(WKIcon.getIcon("refresh"));
        refreshBt.setToolTipText(I18N.tr("Refresh the parameters"));
        refreshBt.addActionListener(EventHandler.create(ActionListener.class, this, "onUserSelectionChange")); 
        mainPanel.add(refreshBt);
        mainPanel.add(removeBt, "wrap");
        
        JLabel labelName = new JLabel(I18N.tr("Connection name"));
        connectionName = new JTextField();
        mainPanel.add(labelName);
        mainPanel.add(connectionName, "width 200!");
        
        CustomButton saveBt = new CustomButton(WKIcon.getIcon("save"));
        saveBt.setToolTipText(I18N.tr("Save the connection parameters"));
        saveBt.addActionListener(EventHandler.create(ActionListener.class, this, "onSave"));
        
        mainPanel.add(saveBt, "wrap");
        
        
        JLabel labeldbType = new JLabel(I18N.tr("Database"));
        dbTypes = new JComboBox<DB_TYPES>(DB_TYPES.values());
        dbTypes.addActionListener(EventHandler.create(ActionListener.class, this, "onDBTypeChange"));
        
        mainPanel.add(labeldbType);
        mainPanel.add(dbTypes, "span, grow, wrap");
        
        JLabel labelHost = new JLabel(I18N.tr("Host"));
        dbHost = new JTextField();
        mainPanel.add(labelHost);
        mainPanel.add(dbHost, "span, grow, wrap");
        
        JLabel labelPort = new JLabel(I18N.tr("Port"));
        dbPort = new JTextField();
        mainPanel.add(labelPort);
        mainPanel.add(dbPort, "span, grow, wrap");
        
        JLabel labeldbName = new JLabel(I18N.tr("Database name"));
        dbName = new JTextField();
        mainPanel.add(labeldbName);
        mainPanel.add(dbName, "span, grow, wrap");
        
        
        JLabel userLabel = new JLabel(I18N.tr("User name"));
        userValue = new JTextField();
        mainPanel.add(userLabel);
        mainPanel.add(userValue, "span 1, grow, wrap");
        JLabel pswLabel = new JLabel(I18N.tr("Require password"));
        requirePassword = new JCheckBox();
        mainPanel.add(pswLabel);
        mainPanel.add(requirePassword, "span 1, grow, wrap");
        
        
        JButton okBt = new JButton(I18N.tr("&Ok"));
        MenuCommonFunctions.setMnemonic(okBt);
        okBt.addActionListener(EventHandler.create(ActionListener.class, this, "onOk"));
        okBt.setDefaultCapable(true);
        mainPanel.add(okBt, "span 3");
        JButton cancelBt = new JButton(I18N.tr("&Cancel"));
        MenuCommonFunctions.setMnemonic(cancelBt);
        cancelBt.addActionListener(EventHandler.create(ActionListener.class, this, "onCancel"));
        cancelBt.setDefaultCapable(true);
        mainPanel.add(cancelBt, "span 3");
        getContentPane().add(mainPanel);
        setTitle(I18N.tr("Database parameters"));
        onUserSelectionChange();
        pack();
        setResizable(false);
    }

    /**
     * Click on the cancel button
     */
    public void onCancel() {
        canceled = true;
        setVisible(false);
    }

    /**
     * @return True if the user cancel
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Click on the Ok button
     */
    public void onOk() {
        if (checkParameters()) {
            urlValue = buildJDBCUrl();
            saveProperties();    
            setVisible(false);
        }
    }

    /**
     * Check if the parameters are well filled.
     */
    private boolean checkParameters() {
        boolean isParametersOk =true;
        if (connectionName.getText().isEmpty()) {
            JOptionPane.showMessageDialog(rootPane, I18N.tr("Please specify a connexion name."));
            isParametersOk=false;
        } else if (dbName.getText().isEmpty()) {
            JOptionPane.showMessageDialog(rootPane, I18N.tr("The name of the database cannot be null."));
            isParametersOk=false;
        } else if (userValue.getText().isEmpty()) {
            JOptionPane.showMessageDialog(rootPane, I18N.tr("The user name cannot be null."));
            isParametersOk=false;
        }
        //Check the DB type
        return isParametersOk;

    }

    private static List<String> decodeStrings(String encodedStrings) {
        StringTokenizer tk = new StringTokenizer(encodedStrings, "|");
        List<String> strings = new ArrayList<>(tk.countTokens());
        while(tk.hasMoreTokens()) {
            String var = tk.nextToken() ;
            strings.add(new String(DatatypeConverter.parseBase64Binary(var)));
        }
        return strings;
    }

    private static String encodeStrings(String... vars) {
        StringBuilder sb = new StringBuilder();
        for(String var : vars) {
            if(sb.length() != 0) {
                sb.append("|");
            }
            sb.append(DatatypeConverter.printBase64Binary(var.getBytes()));
        }
        return sb.toString();
    }

    /**
     * Click on the Ok button.
     */
    public void onSave() {
        if (checkParameters()) {
            String nameValue = connectionName.getText();
            if (!dbProperties.containsKey(nameValue)) {                
                // Encode attributes in Base64 in order to be able to use separator char without worries
                urlValue = buildJDBCUrl();
                dbProperties.setProperty(nameValue,  encodeStrings(urlValue,userValue.getText(),
                        Boolean.toString(requirePassword.isSelected())));
                connectionsComboBox.addItem(nameValue);
                connectionsComboBox.setSelectedItem(nameValue);
                saveProperties();
                onUserSelectionChange();
            }
        }
    }
    
    /**
     * Create the JDBC url from swing components
     * 
     * @return 
     */
    private String buildJDBCUrl(){        
        StringBuilder sb =  new StringBuilder("jdbc:");        
        DB_TYPES dbType = (DB_TYPES) dbTypes.getSelectedItem();
        switch (dbType) {
            case H2GIS_EMBEDDED:
                sb.append("h2:").append(dbName.getText());
                break;
            case H2GIS_SERVER:
                sb.append("h2:tcp://").append(dbHost.getText());
                if(dbPort.getText()!=null){
                    sb.append(":").append(dbPort.getText());
                }
                sb.append("/").append(dbName.getText());
                break;
            case POSTGIS:
                sb.append("postgresql://").append(dbHost.getText());
                if(dbPort.getText()!=null){
                    sb.append(":").append(dbPort.getText());
                }
                sb.append("/").append(dbName.getText());
                break;
            default:
                break;
        }
        return sb.toString();
        
    }
    
    

    /**
     * Click on the Ok button.
     */
    public void onRemove() {
        String valueConnection = connectionName.getText();
        if(dbProperties.containsKey(valueConnection)){
            dbProperties.remove(valueConnection);
            connectionsComboBox.removeItem(valueConnection);
            saveProperties();
            onUserSelectionChange();
        }
    }    
   

    /**
     * Load the connection properties file.
     */
    private void loadDBProperties() {
        try {
            File propertiesFile = new File(defaultCoreWorkspace.getApplicationFolder() + File.separator + DB_PROPERTIES_FILE);
            if (propertiesFile.exists()) {
                dbProperties.load(new FileInputStream(propertiesFile));
            }
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Save the connection properties file.
     */
    public void saveProperties() {
        try {
            dbProperties.store(new FileOutputStream(defaultCoreWorkspace.getApplicationFolder() + File.separator + DB_PROPERTIES_FILE),
                    I18N.tr("Saved with the OrbisGIS database panel"));
        } catch (IOException ex) {
            LOGGER.error(ex.getLocalizedMessage(), ex);
        }

    }

    /**
     * Change the populate the components.
     */
    public void onUserSelectionChange() {
        boolean isCmbEmpty = connectionsComboBox.getItemCount() == 0;
        if (!isCmbEmpty && connectionsComboBox.getSelectedItem() != null) {
            String value = connectionsComboBox.getSelectedItem().toString();
            String data = dbProperties.getProperty(value);
            connectionName.setText(value);
            List<String> config = decodeStrings(data);
            if (config.size() == 3) {
                urlValue = config.get(0);
                Properties jdcProperties = JDBCUrlParser.parse(urlValue);
                dbName.setText(jdcProperties.getProperty(DataSourceFactory.JDBC_DATABASE_NAME));
                String dbTypeName = parseDbType(urlValue);
                if (dbTypeName.equalsIgnoreCase("h2")) {
                    String netProt = jdcProperties.getProperty(DataSourceFactory.JDBC_NETWORK_PROTOCOL);
                    if (netProt != null) {
                        dbTypes.setSelectedItem(DatabaseSettingsPanel.DB_TYPES.H2GIS_SERVER);
                        dbHost.setText(jdcProperties.getProperty(DataSourceFactory.JDBC_SERVER_NAME));
                        String portNum = jdcProperties.getProperty(DataSourceFactory.JDBC_PORT_NUMBER);
                        dbPort.setText(portNum != null ? portNum : DatabaseSettingsPanel.DEFAULT_H2_PORT);
                    } else {
                        dbTypes.setSelectedItem(DatabaseSettingsPanel.DB_TYPES.H2GIS_EMBEDDED);
                        dbHost.setText(DEFAULT_MESSAGE_H2);
                        dbPort.setText(DEFAULT_MESSAGE_H2);
                    }
                } else if (dbTypeName.equalsIgnoreCase("postgresql")) {
                    dbTypes.setSelectedItem(DatabaseSettingsPanel.DB_TYPES.POSTGIS);
                    dbHost.setText(jdcProperties.getProperty(DataSourceFactory.JDBC_SERVER_NAME));
                    dbPort.setText(jdcProperties.getProperty(DataSourceFactory.JDBC_PORT_NUMBER));
                }

                userValue.setText(config.get(1));
                requirePassword.setSelected(Boolean.parseBoolean(config.get(2)));
            }
        }
    }
    
    /**
     * If the dbtype change some components must be disable
     */
    public void onDBTypeChange(){
        DB_TYPES dbType = (DB_TYPES) dbTypes.getSelectedItem();
        if(dbType.equals(DB_TYPES.H2GIS_EMBEDDED)){
            dbHost.setEnabled(false);
            dbPort.setEnabled(false);
        }
        else{            
            dbHost.setEnabled(true);
            dbPort.setEnabled(true);
            if (dbHost.getText() == null) {
                dbHost.setText("Required");
            }
            if (dbPort.getText() == null) {
                dbPort.setText("Required");
            }
        }
        
    }
    
    /**
     * @return Password field
     */
    public boolean hasPassword() {
        return requirePassword.isSelected();
    }
    /**
     * @return URI field
     */
    public String getJdbcURI() {
        return urlValue;
    }

    /**
     * @return User field
     */
    public String getUser() {
        return userValue.getText();
    }

    
    /**
     * Set a new database user name.
     * @param dataBaseUser User identifier
     */
    public void setUser(String dataBaseUser) {
        userValue.setText(dataBaseUser);
    }
    
    /**
     * @return Database name
     */
    public String getDatabaseName() {
        return dbName.getText();
    }

    /**
     * Set this connection require password.
     * @param hasPassword True if this connection require a password.
     */
    public void setHasPassword(Boolean hasPassword) {
        requirePassword.setSelected(hasPassword);
    }

    /**
     * Set the connection identifier
     * @param connectionName Connection identifier
     */
    public void setConnectionName(String connectionName) {
        this.connectionName.setText(connectionName);
    }
    
    /**
     * Set the name of the database
     * @param dbName 
     */
    public  void setDBName(String dbName) {
        this.dbName.setText(dbName); 
    }
    
    /**
     * Select the DBType name
     * @param dbType  
     */
    public void setDBType(DB_TYPES dbType) {
        dbTypes.setSelectedItem(dbType);
    }
    
    /**
     * Set the host value
     * @param hostValue 
     */
    public void setHost(String hostValue) {
        dbHost.setText(hostValue);
    }
    
    /**
     * Set the port number
     * @param portValue 
     */
    public void setPort(String portValue) {
        dbPort.setText(portValue);
    }     
    
    /**
     * Return the database type name base on the JDBC url
     * @param jdbcUrl
     * @return 
     */
    public static String parseDbType(String jdbcUrl) {
        if (!jdbcUrl.startsWith(URL_STARTS)) {
            throw new IllegalArgumentException("JDBC Url must start with " + URL_STARTS);
        }
        String driverAndURI = jdbcUrl.substring(URL_STARTS.length());
        String driver = driverAndURI.substring(0, driverAndURI.indexOf(':'));
        if (driver != null) {
            return driver;
        }
        throw new IllegalArgumentException("JDBC Url must start with " + URL_STARTS);
    }
    


}
