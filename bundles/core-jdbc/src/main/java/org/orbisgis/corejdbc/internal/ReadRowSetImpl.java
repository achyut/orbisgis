/*
 * OrbisGIS is a GIS application dedicated to scientific spatial simulation.
 * This cross-platform GIS is developed at French IRSTV institute and is able to
 * manipulate and create vector and raster spatial information.
 *
 * OrbisGIS is distributed under GPL 3 license. It is produced by the "Atelier SIG"
 * team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.
 *
 * Copyright (C) 2007-2014 IRSTV (FR CNRS 2488)
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
package org.orbisgis.corejdbc.internal;

import com.vividsolutions.jts.geom.Geometry;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.log4j.Logger;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SFSUtilities;
import org.h2gis.utilities.SpatialResultSetMetaData;
import org.h2gis.utilities.TableLocation;
import org.orbisgis.corejdbc.ReadRowSet;
import org.orbisgis.corejdbc.MetaData;
import org.orbisgis.corejdbc.common.IntegerUnion;
import org.orbisgis.progress.NullProgressMonitor;
import org.orbisgis.progress.ProgressMonitor;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import javax.sql.DataSource;
import javax.sql.rowset.JdbcRowSet;
import javax.sql.rowset.RowSetWarning;
import javax.sql.rowset.serial.SerialRef;
import java.beans.EventHandler;
import java.beans.PropertyChangeListener;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RowSet implementation that can be only linked with a table (or view).
 *
 * @author Nicolas Fortin
 */
public class ReadRowSetImpl extends AbstractRowSet implements JdbcRowSet, DataSource, SpatialResultSetMetaData, ReadRowSet {
    private static final int WAITING_FOR_RESULTSET = 5;
    private static final Logger LOGGER = Logger.getLogger(ReadRowSetImpl.class);
    private static final I18n I18N = I18nFactory.getI18n(ReadRowSetImpl.class, Locale.getDefault(), I18nFactory.FALLBACK);
    private TableLocation location;
    private final DataSource dataSource;
    private Object[] currentRow;
    private long rowId = 0;
    /** If the table has been updated or never read, rowCount is set to -1 (unknown) */
    private long cachedRowCount = -1;
    private int cachedColumnCount = -1;
    private BidiMap<String, Integer> cachedColumnNames;
    private boolean wasNull = true;
    /** Used to managed table without primary key (ResultSet are kept {@link ResultSetHolder#RESULT_SET_TIMEOUT} */
    protected final ResultSetHolder resultSetHolder;
    /** If the table contains a unique non null index then this variable contain the map between the row id [1-n] to the primary key value */
    private BidiMap<Integer, Long> rowPk;
    private String pk_name = "";
    private String select_fields = "*";
    private int firstGeometryIndex = -1;
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock readLock = rwl.writeLock(); // Read here is exclusive
    private int fetchSize = 15;
    private Map<Long, Object[]> cache = new LRUMap<>(fetchSize + 1);
    private int fetchDirection = FETCH_UNKNOWN;
    // When close is called, in how many ms the result set is really closed
    private int closeDelay = 0;
    private IntegerUnion rowFilter;
    private ListIterator<Integer> rowFilterIterator;
    private String orderBy = "";
    private boolean isH2;


    /**
     * Constructor, row set based on primary key, significant faster on large table
     * @param dataSource Connection properties
     * @throws IllegalArgumentException If one of the argument is incorrect, that lead to a SQL exception
     */
    public ReadRowSetImpl(DataSource dataSource) {
        this.dataSource = dataSource;
        resultSetHolder = new ResultSetHolder(fetchSize, this);
    }

    @Override
    public void setFilter(Collection<Integer> rowIdSet) {
        if(rowIdSet != null) {
            rowFilter = new IntegerUnion(rowIdSet);
            rowFilterIterator = rowFilter.listIterator();
            if(!rowFilter.isEmpty()) {
                rowId = rowFilter.first() - 1;
            } else {
                rowId = 0;
            }
            currentRow = null;
        } else {
            rowFilter = null;
            rowFilterIterator = null;
        }
    }

    @Override
    public long getFilteredRowCount() throws SQLException {
        return rowFilter == null ? getRowCount() : rowFilter.size();
    }

    @Override
    public Lock getReadLock() {
        return readLock;
    }

    private void cachePrimaryKey(ProgressMonitor pm) throws SQLException {
        ProgressMonitor cachePm = pm.startTask(getRowCount());
        if(rowPk == null) {
            rowPk = new DualHashBidiMap<>();
        } else {
            rowPk.clear();
        }
        try(Connection connection = dataSource.getConnection();
            Statement st = connection.createStatement()) {
            PropertyChangeListener listener = EventHandler.create(PropertyChangeListener.class, st, "cancel");
            pm.addPropertyChangeListener(ProgressMonitor.PROP_CANCEL, listener);
            st.setFetchSize(fetchSize);
            connection.setAutoCommit(false); // Use postgre cursor
            try(ResultSet rs = st.executeQuery("SELECT "+pk_name+" FROM "+location+" "+ orderBy)) {
                // Cache the primary key values
                int pkRowId = 0;
                while (rs.next()) {
                    pkRowId++;
                    rowPk.put(pkRowId, rs.getLong(1));
                    cachePm.endTask();
                }
            } finally {
                pm.removePropertyChangeListener(listener);
            }
        } catch (SQLException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private void setWasNull(boolean wasNull) {
        this.wasNull = wasNull;
    }

    protected void checkColumnIndex(int columnIndex) throws SQLException {
        if(columnIndex < 1 || columnIndex > cachedColumnCount) {
            throw new SQLException(new IndexOutOfBoundsException("Column index "+columnIndex+" out of bound[1-"+cachedColumnCount+"]"));
        }
    }

    /**
     * Check the current RowId, throw an exception if the cursor is at wrong position.
     * @throws SQLException
     */
    protected void checkCurrentRow() throws SQLException {
        if(rowId < 1 || rowId > getRowCount()) {
            throw new SQLException("Not in a valid row "+rowId+"/"+getRowCount());
        }
        if(currentRow == null) {
            refreshRowCache();
            if(currentRow == null) {
                throw new SQLException("Not in a valid row "+rowId+"/"+getRowCount());
            }
        }
    }

    private void cacheColumnNames() throws SQLException {
        cachedColumnNames = new DualHashBidiMap<>();
        try(Resource res = resultSetHolder.getResource()) {
            ResultSetMetaData meta = res.getResultSet().getMetaData();
            for (int idColumn = 1; idColumn <= meta.getColumnCount(); idColumn++) {
                cachedColumnNames.put(meta.getColumnName(idColumn), idColumn);
            }
        }
    }

    /**
     * Clear local cache of rows
     */
    protected void clearRowCache() {
        currentRow = null;
    }

    /**
     * Read the content of the DB near the current row id
     */
    protected void refreshRowCache() throws SQLException {
        if(!cache.containsKey(rowId) && rowId > 0 && rowId <= getRowCount()) {
            try(Resource res = resultSetHolder.getResource()) {
                ResultSet rs = res.getResultSet();
                final int columnCount = getColumnCount();
                if(cachedColumnNames == null) {
                    cacheColumnNames();
                }
                // Do not use pk if not available or if using indeterminate fetch without filtering
                if(rowPk == null) {
                    boolean validRow = false;
                    if(rs.getType() == ResultSet.TYPE_FORWARD_ONLY) {
                        if(rowId < rs.getRow()) {
                            // If the result set is Forward only, we have to re-execute the request in order to read the row
                            resultSetHolder.close();
                            res.close();
                            try(Resource res2 = resultSetHolder.getResource()) {
                                rs = res2.getResultSet();
                            }
                        }
                        while (rs.getRow() < rowId) {
                            validRow = rs.next();
                        }
                    } else {
                        validRow = rs.absolute((int)rowId);
                    }
                    if(validRow) {
                        Object[] row = new Object[columnCount];
                        for(int idColumn=1; idColumn <= columnCount; idColumn++) {
                            row[idColumn-1] = rs.getObject(idColumn);
                        }
                        cache.put(rowId, row);
                    }
                } else {
                    List<Long> pkValues = new ArrayList<>(fetchSize);
                    pkValues.add(rowPk.get((int) rowId));
                    // Fetch next QUERY_PK_FETCH_ROWS rows
                    if(rowFilter == null) {
                        int fetchedRows = 0;
                        for(long fetchRowId = rowId + 1; fetchRowId <= getRowCount(); fetchRowId++) {
                            if(fetchedRows++ < fetchSize) {
                                Long key = rowPk.get((int)fetchRowId);
                                if(key != null) {
                                    pkValues.add(key);
                                }
                            } else {
                                break;
                            }
                        }
                    } else {
                        int fetchedRows = 0;
                        Iterator<Integer> it = rowFilter.listIterator((int)rowId);
                        while(it.hasNext()) {
                            if(fetchedRows++ < fetchSize) {
                                pkValues.add(rowPk.get(it.next()));
                            } else {
                                break;
                            }
                        }
                    }
                    StringBuilder whereClause = new StringBuilder();
                    if(!pkValues.isEmpty()) {
                        if (whereClause.length() > 0) {
                            whereClause.append(" OR ");
                        }
                        whereClause.append(pk_name);
                        whereClause.append(" IN (");
                        for(int paramId = 0; paramId < pkValues.size(); paramId++) {
                            if(paramId > 0) {
                                whereClause.append(",");
                            }
                            whereClause.append("?");
                        }
                        whereClause.append(")");
                    }
                    String command;
                    // if command doesn't contain the key then add the key
                    if(cachedColumnNames == null) {
                        cacheColumnNames();
                    }
                    boolean ignoreFirstColumn = !cachedColumnNames.containsKey(pk_name);
                    if(whereClause.length()>0) {
                        if (ignoreFirstColumn) {
                            command = "SELECT " + pk_name + "," + select_fields + " FROM " + getTable() + " WHERE " + whereClause.toString() + " " + orderBy;
                        } else {
                            command = "SELECT " + select_fields + " FROM " + getTable() + " WHERE " + whereClause.toString() + " " + orderBy;
                        }
                        // Acquire row values by using primary key
                        try (Connection connection = dataSource.getConnection()) {
                            connection.setAutoCommit(false);
                             try(PreparedStatement st = connection.prepareStatement(command)) {
                                 int parameterIndex = 1;
                                 if(isH2 || !pk_name.equals(MetaData.POSTGRE_ROW_IDENTIFIER)) {
                                     for (long pk : pkValues) {
                                         st.setLong(parameterIndex++, pk);
                                     }
                                 } else {
                                     for (long pk : pkValues) {
                                         Ref pkRef = new Tid(pk);
                                         st.setRef(parameterIndex++, pkRef);
                                     }
                                 }
                                 try (ResultSet lineRs = st.executeQuery()) {
                                     int offset = ignoreFirstColumn ? 1 : 0;
                                     while (lineRs.next()) {
                                         Object[] row = new Object[columnCount];
                                         for (int idColumn = 1 + offset; idColumn <= columnCount + offset; idColumn++) {
                                             row[idColumn - 1 - offset] = lineRs.getObject(idColumn);
                                         }
                                         cache.put(rowPk.getKey(lineRs.getLong(pk_name)).longValue(), row);
                                     }
                                 }
                             }
                        }
                    }
                }
            }
        }
        currentRow = cache.get(rowId);
    }

    /**
     * Reestablish connection if necessary
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {
        Connection connection = dataSource.getConnection();
        if(getQueryTimeout() > 0) {
            try(Statement st = connection.createStatement()) {
                st.execute("SET QUERY_TIMEOUT "+getQueryTimeout());
            }
        }
        return connection;
    }

    @Override
    public int getTransactionIsolation() {
        try(Connection connection = getConnection()) {
            return connection.getMetaData().getDefaultTransactionIsolation();
        } catch (SQLException ex) {
            return 0;
        }
    }

    @Override
    public String getCommand() {
        return String.format("SELECT "+select_fields+" FROM %s "+orderBy, location);
    }

    @Override
    public void setCommand(String s) throws SQLException {
        // Extract catalog,schema and table name
        final Pattern selectFieldPattern = Pattern.compile("^select(.+?)from", Pattern.CASE_INSENSITIVE);
        final Pattern commandPattern = Pattern.compile("from\\s+((([\"`][^\"`]+[\"`])|(\\w+))\\.){0,2}(([\"`][^\"`]+[\"`])|(\\w+))", Pattern.CASE_INSENSITIVE);
        final Pattern commandPatternTable = Pattern.compile("^from\\s+", Pattern.CASE_INSENSITIVE);
        final Pattern orderByPattern = Pattern.compile("order\\sby\\s+(\\w+)$", Pattern.CASE_INSENSITIVE);
        String table = "";
        Matcher matcher = commandPattern.matcher(s);
        Matcher selectFieldMatcher = selectFieldPattern.matcher(s);
        Matcher orderByMatcher = orderByPattern.matcher(s);
        if(selectFieldMatcher.find()) {
            select_fields = selectFieldMatcher.group(1);
        }
        if (matcher.find()) {
            Matcher tableMatcher = commandPatternTable.matcher(matcher.group());
            if(tableMatcher.find()) {
                table = matcher.group().substring(tableMatcher.group().length());
            }
        }
        if(orderByMatcher.find()) {
            orderBy = orderByMatcher.group();
        }
        if(table.isEmpty()) {
            throw new SQLException("Command does not contain a table name, should be like this \"select * from tablename\"");
        }
        this.location = TableLocation.parse(table);
        try(Connection connection = dataSource.getConnection()) {
            this.pk_name = MetaData.getPkName(connection, location.toString(), true);
        }
    }

    @Override
    public String getTable() {
        return location.toString();
    }

    @Override
    public void initialize(String tableIdentifier, String pk_name, ProgressMonitor pm) throws SQLException {
        initialize(TableLocation.parse(tableIdentifier), pk_name, pm);
    }

    /**
     * Initialize this row set. This method cache primary key.
     * @param location Table location
     * @param pk_name Primary key name {@link org.orbisgis.corejdbc.MetaData#getPkName(java.sql.Connection, String, boolean)}
     * @param pm Progress monitor Progression of primary key caching
     */
    public void initialize(TableLocation location,String pk_name, ProgressMonitor pm) throws SQLException {
        this.location = location;
        this.pk_name = pk_name;
        execute(pm);
    }

    @Override
    public void execute(ProgressMonitor pm) throws SQLException {
        try(Connection connection = dataSource.getConnection()) {
            isH2 = JDBCUtilities.isH2DataBase(connection.getMetaData());
        }
        if(!pk_name.isEmpty()) {
            cachePrimaryKey(pm);
            // Always use PK to fetch rows
            resultSetHolder.setCommand(getCommand() + " LIMIT 0");
        } else {
            resultSetHolder.setCommand(getCommand());
            PropertyChangeListener listener = EventHandler.create(PropertyChangeListener.class, resultSetHolder, "cancel");
            pm.addPropertyChangeListener(ProgressMonitor.PROP_CANCEL, listener);
            try {
                resultSetHolder.getResource(); // Long query
            } finally {
                pm.removePropertyChangeListener(listener);
            }
        }
    }

    @Override
    public void execute() throws SQLException {
        if(resultSetHolder.getCommand() != null) {
            throw new SQLException("This row set is already executed");
        }
        if(location == null) {
            throw new SQLException("You must execute RowSet.setCommand(String sql) first");
        }
        initialize(location, pk_name, new NullProgressMonitor());
    }

    @Override
    public boolean next() throws SQLException {
        return moveCursorTo(rowId + 1);
    }

    @Override
    public void close() throws SQLException {
        clearRowCache();
        try {
            resultSetHolder.delayedClose(closeDelay);
        } catch (Exception ex) {
            throw new SQLException(ex);
        }
    }

    @Override
    public void setCloseDelay(int milliseconds) {
        closeDelay = milliseconds;
    }

    public long getRowCount() throws SQLException {
        if(cachedRowCount == -1) {
            try (Connection connection = getConnection();
                 Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + location)) {
                if (rs.next()) {
                    cachedRowCount = rs.getLong(1);
                }
            }
        }
        return cachedRowCount;
    }

    @Override
    public boolean wasNull() throws SQLException {
        return wasNull;
    }


    @Override
    public SQLWarning getWarnings() throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getWarnings();
        }
    }

    @Override
    public void clearWarnings() throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            res.getResultSet().clearWarnings();
        }
    }

    @Override
    public String getCursorName() throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getCursorName();
        }
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return this;
    }

    @Override
    public Object getObject(int i) throws SQLException {
        checkColumnIndex(i);
        checkCurrentRow();
        Object cell = currentRow[i-1];
        setWasNull(cell == null);
        return cell;
    }

    @Override
    public int findColumn(String label) throws SQLException {
        Integer columnId = cachedColumnNames.get(label.toUpperCase());
        if(columnId == null) {
            throw new SQLException("Column "+label+" does not exists");
        }
        return columnId;
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        return rowId == 0;
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        return rowId > getRowCount();
    }

    @Override
    public boolean isFirst() throws SQLException {
        return rowId == 1;
    }

    @Override
    public boolean isLast() throws SQLException {
        return rowId == getRowCount();
    }

    @Override
    public void beforeFirst() throws SQLException {
        moveCursorTo(0);
    }

    @Override
    public void afterLast() throws SQLException {
        moveCursorTo((int)(getRowCount() + 1));
    }

    @Override
    public boolean first() throws SQLException {
        if(rowFilter == null || rowFilter.isEmpty()) {
            return moveCursorTo(1);
        } else {
            return moveCursorTo(rowFilter.first());
        }
    }

    @Override
    public boolean last() throws SQLException {
        if(rowFilter == null || rowFilter.isEmpty()) {
            return moveCursorTo((int)getRowCount());
        } else {
            return moveCursorTo(rowFilter.last());
        }
    }

    @Override
    public int getRow() throws SQLException {
        return (int)rowId;
    }

    @Override
    public boolean absolute(int i) throws SQLException {
        return moveCursorTo(i);
    }

    private boolean moveCursorTo(long i) throws SQLException {
        i = Math.max(0, i);
        i = Math.min(getRowCount() + 1, i);
        if(rowFilter != null && !rowFilter.isEmpty()) {
            i = Math.max(rowFilter.first() - 1, i);
            i = Math.min(rowFilter.last() + 1, i);
        }
        long oldRowId = rowId;
        if(rowFilterIterator != null) {
            if(i < oldRowId) {
                // back until expected rowid
                while(rowId > i) {
                    if(rowFilterIterator.hasPrevious()) {
                        rowId = rowFilterIterator.previous();
                    } else {
                        if(!rowFilter.isEmpty()) {
                            rowId = rowFilter.first();
                        } else {
                            rowId = 1;
                        }
                        if(rowId > i) {
                            // Before first
                            rowId--;
                        }
                        break;
                    }
                }
            } else if(i > oldRowId) {
                while(rowId < i) {
                    if(rowFilterIterator.hasNext()) {
                        rowId = rowFilterIterator.next();
                    } else {
                        if(!rowFilter.isEmpty()) {
                            rowId = rowFilter.last() + 1;
                        } else {
                            rowId = getRowCount() + 1;
                        }
                        if(rowId < i) {
                            // After last
                            rowId++;
                        }
                        break;
                    }
                }
            }
        } else {
            rowId = i;
        }
        boolean validRow = !(rowId == 0 || rowId > getRowCount() || (rowFilter != null && !rowFilter.isEmpty() && (rowId < rowFilter.first() || rowId > rowFilter.last())));
        if(validRow) {
            refreshRowCache();
        } else {
            currentRow = null;
        }
        if(rowId != oldRowId) {
            notifyCursorMoved();
        }
        return validRow;
    }

    @Override
    public boolean relative(int i) throws SQLException {
        return moveCursorTo((int)(rowId + i));
    }

    @Override
    public boolean previous() throws SQLException {
        return moveCursorTo(rowId - 1);
    }

    @Override
    public void setFetchDirection(int i) throws SQLException {
        fetchDirection = i;
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return fetchDirection;
    }

    @Override
    public void setFetchSize(int i) throws SQLException {
        fetchSize = i;
        LRUMap<Long, Object[]> lruMap = new LRUMap<>(fetchSize + 1);
        lruMap.putAll(cache);
        cache = lruMap;
    }

    @Override
    public int getFetchSize() throws SQLException {
        return fetchSize;
    }

    @Override
    public int getType() throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getType();
        }
    }

    @Override
    public int getConcurrency() throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getConcurrency();
        }
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().rowUpdated();
        }
    }

    @Override
    public boolean rowInserted() throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().rowInserted();
        }
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().rowDeleted();
        }
    }

    @Override
    public void refreshRow() throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            moveCursorTo(rowId);
            currentRow = null;
            cache.clear();
            if(res.getResultSet().getRow() > 0 && !res.getResultSet().isAfterLast()) {
                res.getResultSet().refreshRow();
            }
        } catch (SQLException ex) {
            LOGGER.warn(ex.getLocalizedMessage(), ex);
        }
    }

    @Override
    public Statement getStatement() throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getStatement();
        }
    }


    @Override
    public RowId getRowId(int i) throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getRowId(i);
        }
    }

    @Override
    public RowId getRowId(String s) throws SQLException {
        return getRowId(findColumn(s));
    }

    @Override
    public int getHoldability() throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getHoldability();
        }
    }

    @Override
    public boolean isClosed() throws SQLException {
        return false; // Never closed
    }

    // DataSource methods

    @Override
    public Connection getConnection(String s, String s2) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLogWriter(PrintWriter printWriter) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLoginTimeout(int i) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new UnsupportedOperationException();
    }

    // ResultSetMetaData functions


    @Override
    public String getCatalogName(int i) throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getMetaData().getCatalogName(i);
        }
    }

    @Override
    public int getColumnCount() throws SQLException {
        if(cachedColumnCount == -1) {
            try(Resource res = resultSetHolder.getResource()) {
                cachedColumnCount = res.getResultSet().getMetaData().getColumnCount();
                return cachedColumnCount;
            }
        }
        return cachedColumnCount;
    }

    @Override
    public boolean isAutoIncrement(int i) throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getMetaData().isAutoIncrement(i);
        }
    }

    @Override
    public boolean isCaseSensitive(int i) throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getMetaData().isCaseSensitive(i);
        }
    }

    @Override
    public boolean isSearchable(int i) throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getMetaData().isSearchable(i);
        }
    }

    @Override
    public boolean isCurrency(int i) throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getMetaData().isCurrency(i);
        }
    }

    @Override
    public int isNullable(int i) throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getMetaData().isNullable(i);
        }
    }

    @Override
    public boolean isSigned(int i) throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getMetaData().isSigned(i);
        }
    }

    @Override
    public int getColumnDisplaySize(int i) throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getMetaData().getColumnDisplaySize(i);
        }
    }

    @Override
    public String getColumnLabel(int i) throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getMetaData().getColumnLabel(i);
        }
    }

    @Override
    public String getColumnName(int i) throws SQLException {
        if(cachedColumnNames == null) {
            cacheColumnNames();
        }
        return cachedColumnNames.getKey(i);
    }

    @Override
    public String getSchemaName(int i) throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getMetaData().getSchemaName(i);
        }
    }

    @Override
    public int getPrecision(int i) throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getMetaData().getPrecision(i);
        }
    }

    @Override
    public int getScale(int i) throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getMetaData().getScale(i);
        }
    }

    @Override
    public String getTableName(int i) throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getMetaData().getTableName(i);
        }
    }

    @Override
    public int getColumnType(int i) throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getMetaData().getColumnType(i);
        }
    }

    @Override
    public String getColumnTypeName(int i) throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getMetaData().getColumnTypeName(i);
        }
    }

    @Override
    public boolean isReadOnly(int i) throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getMetaData().isReadOnly(i);
        }
    }

    @Override
    public boolean isWritable(int i) throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getMetaData().isWritable(i);
        }
    }

    @Override
    public boolean isDefinitelyWritable(int i) throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getMetaData().isDefinitelyWritable(i);
        }
    }

    @Override
    public String getColumnClassName(int i) throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            return res.getResultSet().getMetaData().getColumnClassName(i);
        }
    }

    @Override
    public void updateNull(int i) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateBoolean(int i, boolean b) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateByte(int i, byte b) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateShort(int i, short i2) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateInt(int i, int i2) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateLong(int i, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateFloat(int i, float v) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateDouble(int i, double v) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateBigDecimal(int i, BigDecimal bigDecimal) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateString(int i, String s) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateBytes(int i, byte[] bytes) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateDate(int i, Date date) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateTime(int i, Time time) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateTimestamp(int i, Timestamp timestamp) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateAsciiStream(int i, InputStream inputStream, int i2) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateBinaryStream(int i, InputStream inputStream, int i2) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateCharacterStream(int i, Reader reader, int i2) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateObject(int i, Object o, int i2) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateObject(int i, Object o) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateNull(String s) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateBoolean(String s, boolean b) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateByte(String s, byte b) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateShort(String s, short i) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateInt(String s, int i) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateLong(String s, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateFloat(String s, float v) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateDouble(String s, double v) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateBigDecimal(String s, BigDecimal bigDecimal) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateString(String s, String s2) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateBytes(String s, byte[] bytes) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateDate(String s, Date date) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateTime(String s, Time time) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateTimestamp(String s, Timestamp timestamp) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateAsciiStream(String s, InputStream inputStream, int i) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateBinaryStream(String s, InputStream inputStream, int i) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateCharacterStream(String s, Reader reader, int i) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateObject(String s, Object o, int i) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateObject(String s, Object o) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void insertRow() throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateRow() throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void deleteRow() throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateRef(int i, Ref ref) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateRef(String s, Ref ref) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateBlob(int i, Blob blob) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateBlob(String s, Blob blob) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateClob(int i, Clob clob) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateClob(String s, Clob clob) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateArray(int i, Array array) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateArray(String s, Array array) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateRowId(int i, RowId rowId) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateRowId(String s, RowId rowId) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateNString(int i, String s) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateNString(String s, String s2) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateNClob(int i, NClob nClob) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateNClob(String s, NClob nClob) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateSQLXML(int i, SQLXML sqlxml) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateSQLXML(String s, SQLXML sqlxml) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateNCharacterStream(int i, Reader reader, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateNCharacterStream(String s, Reader reader, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateAsciiStream(int i, InputStream inputStream, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateBinaryStream(int i, InputStream inputStream, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateCharacterStream(int i, Reader reader, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateAsciiStream(String s, InputStream inputStream, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateBinaryStream(String s, InputStream inputStream, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateCharacterStream(String s, Reader reader, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateBlob(int i, InputStream inputStream, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateBlob(String s, InputStream inputStream, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateClob(int i, Reader reader, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateClob(String s, Reader reader, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateNClob(int i, Reader reader, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateNClob(String s, Reader reader, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateNCharacterStream(int i, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateNCharacterStream(String s, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateAsciiStream(int i, InputStream inputStream) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateBinaryStream(int i, InputStream inputStream) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateCharacterStream(int i, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateAsciiStream(String s, InputStream inputStream) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateBinaryStream(String s, InputStream inputStream) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateCharacterStream(String s, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateBlob(int i, InputStream inputStream) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateBlob(String s, InputStream inputStream) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateClob(int i, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateClob(String s, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateNClob(int i, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateNClob(String s, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public RowSetWarning getRowSetWarnings() throws SQLException {
        throw new SQLFeatureNotSupportedException("getRowSetWarnings not supported");
    }

    @Override
    public void commit() throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void rollback() throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void rollback(Savepoint s) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void setMatchColumn(int columnIdx) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void setMatchColumn(int[] columnIdxes) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void setMatchColumn(String columnName) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void setMatchColumn(String[] columnNames) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public int[] getMatchColumnIndexes() throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public String[] getMatchColumnNames() throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void unsetMatchColumn(int columnIdx) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void unsetMatchColumn(int[] columnIdxes) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void unsetMatchColumn(String columnName) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void unsetMatchColumn(String[] columnName) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public Geometry getGeometry() throws SQLException {
        if(firstGeometryIndex == -1) {
            try(Connection connection = dataSource.getConnection()) {
                List<String> geoFields = SFSUtilities.getGeometryFields(connection, location);
                if(!geoFields.isEmpty()) {
                    firstGeometryIndex = JDBCUtilities.getFieldIndex(getMetaData(), geoFields.get(0));
                } else {
                    throw new SQLException("No geometry column found");
                }
            }
        }
        return getGeometry(firstGeometryIndex);
    }

    @Override
    public void updateGeometry(int columnIndex, Geometry geometry) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public void updateGeometry(String columnLabel, Geometry geometry) throws SQLException {
        throw new SQLFeatureNotSupportedException("Read only RowSet");
    }

    @Override
    public String getPkName() {
        return pk_name;
    }

    @Override
    public int getRowId(Object primaryKeyRowValue) {
        if(!pk_name.isEmpty()) {
            return rowPk.getKey(primaryKeyRowValue).intValue();
        } else {
            throw new IllegalStateException("The RowSet has not been initialised");
        }
    }

    @Override
    public long getRowPK(int rowNumber) {
        if(!pk_name.isEmpty()) {
            return rowPk.get(rowNumber);
        } else {
            throw new IllegalStateException("The RowSet has not been initialised");
        }
    }

    @Override
    public int getGeometryType(int i) throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            SpatialResultSetMetaData meta = res.getResultSet().getMetaData().unwrap(SpatialResultSetMetaData.class);
            return meta.getGeometryType(i);
        }
    }

    @Override
    public int getGeometryType() throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            SpatialResultSetMetaData meta = res.getResultSet().getMetaData().unwrap(SpatialResultSetMetaData.class);
            return meta.getGeometryType();
        }
    }

    @Override
    public int getFirstGeometryFieldIndex() throws SQLException {
        try(Resource res = resultSetHolder.getResource()) {
            SpatialResultSetMetaData meta = res.getResultSet().getMetaData().unwrap(SpatialResultSetMetaData.class);
            return meta.getFirstGeometryFieldIndex();
        }
    }

    /**
     * This thread guaranty that the connection,ResultSet is released when no longer used.
     */
    private static class ResultSetHolder implements Runnable,AutoCloseable {
        private static final int SLEEP_TIME = 1000;
        private static final int RESULT_SET_TIMEOUT = 60000;
        private final int fetchSize;
        public enum STATUS { NEVER_STARTED, STARTED , READY, CLOSING, CLOSED, EXCEPTION}
        private Exception ex;
        private ResultSet resultSet;
        private DataSource dataSource;
        private String command;
        private STATUS status = STATUS.NEVER_STARTED;
        private long lastUsage = System.currentTimeMillis();
        private static final Logger LOGGER = Logger.getLogger(ResultSetHolder.class);
        private int openCount = 0;
        private Statement cancelStatement;

        private ResultSetHolder(int fetchSize, DataSource dataSource) {
            this.fetchSize = fetchSize;
            this.dataSource = dataSource;
        }

        /**
         * @param command SQL command to execute
         */
        public void setCommand(String command) {
            this.command = command;
        }

        /**
         * @return SQL Command, may be null if not set
         */
        public String getCommand() {
            return command;
        }

        @Override
        public void run() {
            lastUsage = System.currentTimeMillis();
            status = STATUS.STARTED;
            try (Connection connection = dataSource.getConnection()) {
                boolean isH2 = JDBCUtilities.isH2DataBase(connection.getMetaData());
                try (
                        Statement st = connection.createStatement(isH2 ? ResultSet.TYPE_SCROLL_SENSITIVE : ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY)) {
                    cancelStatement = st;
                    st.setFetchSize(fetchSize);
                    if (!isH2) {
                        // Memory optimisation for PostGre
                        connection.setAutoCommit(false);
                    }
                    // PostGreSQL use cursor only if auto commit is false
                    try (ResultSet activeResultSet = st.executeQuery(command)) {
                        resultSet = activeResultSet;
                        status = STATUS.READY;
                        while (lastUsage + RESULT_SET_TIMEOUT > System.currentTimeMillis() || openCount != 0) {
                            Thread.sleep(SLEEP_TIME);
                        }
                    }
                }
            } catch (Exception ex) {
                LOGGER.error(ex.getLocalizedMessage(), ex);
                this.ex = ex;
                status = STATUS.EXCEPTION;
            } finally {
                if (status != STATUS.EXCEPTION) {
                    status = STATUS.CLOSED;
                }
            }
        }

        @Override
        public void close() throws SQLException {
            lastUsage = 0;
            openCount = 0;
            status = STATUS.CLOSING;
        }

        public void delayedClose(int milliSec) {
            lastUsage = System.currentTimeMillis() - RESULT_SET_TIMEOUT + milliSec;
            openCount = 0;
        }

        /**
         * {@link java.sql.Statement#cancel()}
         * @throws SQLException
         */
        public void cancel() throws SQLException {
            Statement cancelObj = cancelStatement;
            if(cancelObj != null && !cancelObj.isClosed()) {
                cancelObj.cancel();
            }
        }

        /**
         * @return ResultSet status
         */
        public STATUS getStatus() {
            return status;
        }

        public Resource getResource() throws SQLException {
            // Wait execution of request
            while(getStatus() != STATUS.READY) {
                // Reactivate result set if necessary
                if(getStatus() == ResultSetHolder.STATUS.CLOSED || getStatus() == ResultSetHolder.STATUS.NEVER_STARTED) {
                    Thread resultSetThread = new Thread(this, "ResultSet of "+command);
                    resultSetThread.start();
                }
                if(status == STATUS.EXCEPTION) {
                    if(ex instanceof SQLException) {
                        throw (SQLException)ex;
                    } else {
                        throw new SQLException(ex);
                    }
                }
                try {
                    Thread.sleep(WAITING_FOR_RESULTSET);
                } catch (InterruptedException e) {
                    throw new SQLException(e);
                }
            }
            lastUsage = System.currentTimeMillis();
            openCount++;
            return new Resource(this, resultSet);
        }

        /**
         * Even if the timer should close the result set, the connection is not closed
         */
        public void onResourceClosed() {
            openCount = Math.max(0, openCount-1);
        }
    }

    /**
     * This class is created each time the result set is necessary, close this object to release the result set.
     */
    private static class Resource implements AutoCloseable {
        private final ResultSet resultSet;
        private final ResultSetHolder holder;

        private Resource(ResultSetHolder holder, ResultSet resultSet) {
            this.holder = holder;
            this.resultSet = resultSet;
        }

        @Override
        public void close() {
            holder.onResourceClosed();
        }

        private ResultSet getResultSet() {
            return resultSet;
        }
    }

    private static class Tid implements Ref {
        private long value;

        private Tid(long value) {
            this.value = value;
        }

        @Override
        public String getBaseTypeName() throws SQLException {
            return "tid";
        }

        @Override
        public Object getObject(Map<String, Class<?>> map) throws SQLException {
            return value;
        }

        @Override
        public Object getObject() throws SQLException {
            return value;
        }

        @Override
        public void setObject(Object value) throws SQLException {
            throw new UnsupportedOperationException();
        }
    }
}
