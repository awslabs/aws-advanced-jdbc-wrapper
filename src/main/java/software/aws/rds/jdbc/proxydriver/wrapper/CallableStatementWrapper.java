/*
 * AWS JDBC Proxy Driver
 * Copyright Amazon.com Inc. or affiliates.
 * See the LICENSE file in the project root for more information.
 */

package software.aws.rds.jdbc.proxydriver.wrapper;

import org.checkerframework.checker.nullness.qual.NonNull;
import software.aws.rds.jdbc.proxydriver.ConnectionPluginManager;
import software.aws.rds.jdbc.proxydriver.util.WrapperUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

public class CallableStatementWrapper implements CallableStatement {

    protected CallableStatement statement;
    protected ConnectionPluginManager pluginManager;

    public CallableStatementWrapper(@NonNull CallableStatement statement,
                                    @NonNull ConnectionPluginManager pluginManager) {
        this.statement = statement;
        this.pluginManager = pluginManager;
    }

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.registerOutParameter",
                () -> this.statement.registerOutParameter(parameterIndex, sqlType),
                parameterIndex, sqlType);
    }

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.registerOutParameter",
                () -> this.statement.registerOutParameter(parameterIndex, sqlType, scale),
                parameterIndex, sqlType, scale);
    }

    @Override
    public boolean wasNull() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                boolean.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.wasNull",
                () -> this.statement.wasNull());
    }

    @Override
    public String getString(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                String.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getString",
                () -> this.statement.getString(parameterIndex),
                parameterIndex);
    }

    @Override
    public boolean getBoolean(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                boolean.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getBoolean",
                () -> this.statement.getBoolean(parameterIndex),
                parameterIndex);
    }

    @Override
    public byte getByte(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                byte.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getByte",
                () -> this.statement.getByte(parameterIndex),
                parameterIndex);
    }

    @Override
    public short getShort(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                short.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getShort",
                () -> this.statement.getShort(parameterIndex),
                parameterIndex);
    }

    @Override
    public int getInt(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                int.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getInt",
                () -> this.statement.getInt(parameterIndex),
                parameterIndex);
    }

    @Override
    public long getLong(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                long.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getLong",
                () -> this.statement.getLong(parameterIndex),
                parameterIndex);
    }

    @Override
    public float getFloat(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                float.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getFloat",
                () -> this.statement.getFloat(parameterIndex),
                parameterIndex);
    }

    @Override
    public double getDouble(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                double.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getDouble",
                () -> this.statement.getDouble(parameterIndex),
                parameterIndex);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                BigDecimal.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getBigDecimal",
                () -> this.statement.getBigDecimal(parameterIndex, scale),
                parameterIndex, scale);
    }

    @Override
    public byte[] getBytes(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                byte[].class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getBytes",
                () -> this.statement.getBytes(parameterIndex),
                parameterIndex);
    }

    @Override
    public Date getDate(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Date.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getDate",
                () -> this.statement.getDate(parameterIndex),
                parameterIndex);
    }

    @Override
    public Time getTime(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Time.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getTime",
                () -> this.statement.getTime(parameterIndex),
                parameterIndex);
    }

    @Override
    public Timestamp getTimestamp(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Timestamp.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getTimestamp",
                () -> this.statement.getTimestamp(parameterIndex),
                parameterIndex);
    }

    @Override
    public Object getObject(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Object.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getObject",
                () -> this.statement.getObject(parameterIndex),
                parameterIndex);
    }

    @Override
    public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                BigDecimal.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getBigDecimal",
                () -> this.statement.getBigDecimal(parameterIndex),
                parameterIndex);
    }

    @Override
    public Object getObject(int parameterIndex, Map<String, Class<?>> map) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Object.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getObject",
                () -> this.statement.getObject(parameterIndex, map),
                parameterIndex, map);
    }

    @Override
    public Ref getRef(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Ref.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getRef",
                () -> this.statement.getRef(parameterIndex),
                parameterIndex);
    }

    @Override
    public Blob getBlob(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Blob.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getBlob",
                () -> this.statement.getBlob(parameterIndex),
                parameterIndex);
    }

    @Override
    public Clob getClob(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Clob.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getClob",
                () -> this.statement.getClob(parameterIndex),
                parameterIndex);
    }

    @Override
    public Array getArray(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Array.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getArray",
                () -> this.statement.getArray(parameterIndex),
                parameterIndex);
    }

    @Override
    public Date getDate(int parameterIndex, Calendar cal) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Date.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getDate",
                () -> this.statement.getDate(parameterIndex, cal),
                parameterIndex, cal);
    }

    @Override
    public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Time.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getTime",
                () -> this.statement.getTime(parameterIndex, cal),
                parameterIndex, cal);
    }

    @Override
    public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Timestamp.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getTimestamp",
                () -> this.statement.getTimestamp(parameterIndex, cal),
                parameterIndex, cal);
    }

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType, String typeName)
            throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.registerOutParameter",
                () -> this.statement.registerOutParameter(parameterIndex, sqlType, typeName),
                parameterIndex, sqlType, typeName);
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.registerOutParameter",
                () -> this.statement.registerOutParameter(parameterName, sqlType),
                parameterName, sqlType);
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType, int scale)
            throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.registerOutParameter",
                () -> this.statement.registerOutParameter(parameterName, sqlType, scale),
                parameterName, sqlType, scale);
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType, String typeName)
            throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.registerOutParameter",
                () -> this.statement.registerOutParameter(parameterName, sqlType, typeName),
                parameterName, sqlType, typeName);
    }

    @Override
    public URL getURL(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                URL.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getURL",
                () -> this.statement.getURL(parameterIndex),
                parameterIndex);
    }

    @Override
    public void setURL(String parameterName, URL val) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setURL",
                () -> this.statement.setURL(parameterName, val),
                parameterName, val);
    }

    @Override
    public void setNull(String parameterName, int sqlType) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setNull",
                () -> this.statement.setNull(parameterName, sqlType),
                parameterName, sqlType);
    }

    @Override
    public void setBoolean(String parameterName, boolean x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setBoolean",
                () -> this.statement.setBoolean(parameterName, x),
                parameterName, x);
    }

    @Override
    public void setByte(String parameterName, byte x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setByte",
                () -> this.statement.setByte(parameterName, x),
                parameterName, x);
    }

    @Override
    public void setShort(String parameterName, short x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setShort",
                () -> this.statement.setShort(parameterName, x),
                parameterName, x);
    }

    @Override
    public void setInt(String parameterName, int x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setInt",
                () -> this.statement.setInt(parameterName, x),
                parameterName, x);
    }

    @Override
    public void setLong(String parameterName, long x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setLong",
                () -> this.statement.setLong(parameterName, x),
                parameterName, x);
    }

    @Override
    public void setFloat(String parameterName, float x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setFloat",
                () -> this.statement.setFloat(parameterName, x),
                parameterName, x);
    }

    @Override
    public void setDouble(String parameterName, double x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setDouble",
                () -> this.statement.setDouble(parameterName, x),
                parameterName, x);
    }

    @Override
    public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setBigDecimal",
                () -> this.statement.setBigDecimal(parameterName, x),
                parameterName, x);
    }

    @Override
    public void setString(String parameterName, String x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setString",
                () -> this.statement.setString(parameterName, x),
                parameterName, x);
    }

    @Override
    public void setBytes(String parameterName, byte[] x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setBytes",
                () -> this.statement.setBytes(parameterName, x),
                parameterName, x);
    }

    @Override
    public void setDate(String parameterName, Date x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setDate",
                () -> this.statement.setDate(parameterName, x),
                parameterName, x);
    }

    @Override
    public void setTime(String parameterName, Time x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setTime",
                () -> this.statement.setTime(parameterName, x),
                parameterName, x);
    }

    @Override
    public void setTimestamp(String parameterName, Timestamp x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setTimestamp",
                () -> this.statement.setTimestamp(parameterName, x),
                parameterName, x);
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x, int length) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setAsciiStream",
                () -> this.statement.setAsciiStream(parameterName, x, length),
                parameterName, x, length);
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x, int length) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setBinaryStream",
                () -> this.statement.setBinaryStream(parameterName, x, length),
                parameterName, x, length);
    }

    @Override
    public void setObject(String parameterName, Object x, int targetSqlType, int scale)
            throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setObject",
                () -> this.statement.setObject(parameterName, x, targetSqlType, scale),
                parameterName, x, targetSqlType, scale);
    }

    @Override
    public void setObject(String parameterName, Object x, int targetSqlType) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setObject",
                () -> this.statement.setObject(parameterName, x, targetSqlType),
                parameterName, x, targetSqlType);
    }

    @Override
    public void setObject(String parameterName, Object x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setObject",
                () -> this.statement.setObject(parameterName, x),
                parameterName, x);
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader, int length)
            throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setCharacterStream",
                () -> this.statement.setCharacterStream(parameterName, reader, length),
                parameterName, reader, length);
    }

    @Override
    public void setDate(String parameterName, Date x, Calendar cal) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setDate",
                () -> this.statement.setDate(parameterName, x, cal),
                parameterName, x, cal);
    }

    @Override
    public void setTime(String parameterName, Time x, Calendar cal) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setTime",
                () -> this.statement.setTime(parameterName, x, cal),
                parameterName, x, cal);
    }

    @Override
    public void setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setTimestamp",
                () -> this.statement.setTimestamp(parameterName, x, cal),
                parameterName, x, cal);
    }

    @Override
    public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setNull",
                () -> this.statement.setNull(parameterName, sqlType, typeName),
                parameterName, sqlType, typeName);
    }

    @Override
    public String getString(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                String.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getString",
                () -> this.statement.getString(parameterName),
                parameterName);
    }

    @Override
    public boolean getBoolean(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                boolean.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getBoolean",
                () -> this.statement.getBoolean(parameterName),
                parameterName);
    }

    @Override
    public byte getByte(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                byte.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getByte",
                () -> this.statement.getByte(parameterName),
                parameterName);
    }

    @Override
    public short getShort(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                short.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getShort",
                () -> this.statement.getShort(parameterName),
                parameterName);
    }

    @Override
    public int getInt(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                int.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getInt",
                () -> this.statement.getInt(parameterName),
                parameterName);
    }

    @Override
    public long getLong(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                long.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getLong",
                () -> this.statement.getLong(parameterName),
                parameterName);
    }

    @Override
    public float getFloat(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                float.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getFloat",
                () -> this.statement.getFloat(parameterName),
                parameterName);
    }

    @Override
    public double getDouble(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                double.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getDouble",
                () -> this.statement.getDouble(parameterName),
                parameterName);
    }

    @Override
    public byte[] getBytes(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                byte[].class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getBytes",
                () -> this.statement.getBytes(parameterName),
                parameterName);
    }

    @Override
    public Date getDate(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Date.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getDate",
                () -> this.statement.getDate(parameterName),
                parameterName);
    }

    @Override
    public Time getTime(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Time.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getTime",
                () -> this.statement.getTime(parameterName),
                parameterName);
    }

    @Override
    public Timestamp getTimestamp(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Timestamp.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getTimestamp",
                () -> this.statement.getTimestamp(parameterName),
                parameterName);
    }

    @Override
    public Object getObject(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Object.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getObject",
                () -> this.statement.getObject(parameterName),
                parameterName);
    }

    @Override
    public BigDecimal getBigDecimal(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                BigDecimal.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getBigDecimal",
                () -> this.statement.getBigDecimal(parameterName),
                parameterName);
    }

    @Override
    public Object getObject(String parameterName, Map<String, Class<?>> map) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Object.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getObject",
                () -> this.statement.getObject(parameterName, map),
                parameterName, map);
    }

    @Override
    public Ref getRef(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Ref.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getRef",
                () -> this.statement.getRef(parameterName),
                parameterName);
    }

    @Override
    public Blob getBlob(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Blob.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getBlob",
                () -> this.statement.getBlob(parameterName),
                parameterName);
    }

    @Override
    public Clob getClob(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Clob.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getClob",
                () -> this.statement.getClob(parameterName),
                parameterName);
    }

    @Override
    public Array getArray(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Array.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getArray",
                () -> this.statement.getArray(parameterName),
                parameterName);
    }

    @Override
    public Date getDate(String parameterName, Calendar cal) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Date.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getDate",
                () -> this.statement.getDate(parameterName, cal),
                parameterName, cal);
    }

    @Override
    public Time getTime(String parameterName, Calendar cal) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Time.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getTime",
                () -> this.statement.getTime(parameterName, cal),
                parameterName, cal);
    }

    @Override
    public Timestamp getTimestamp(String parameterName, Calendar cal) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Timestamp.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getTimestamp",
                () -> this.statement.getTimestamp(parameterName, cal),
                parameterName, cal);
    }

    @Override
    public URL getURL(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                URL.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getURL",
                () -> this.statement.getURL(parameterName),
                parameterName);
    }

    @Override
    public RowId getRowId(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                RowId.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getRowId",
                () -> this.statement.getRowId(parameterIndex),
                parameterIndex);
    }

    @Override
    public RowId getRowId(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                RowId.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getRowId",
                () -> this.statement.getRowId(parameterName),
                parameterName);
    }

    @Override
    public void setRowId(String parameterName, RowId x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setRowId",
                () -> this.statement.setRowId(parameterName, x),
                parameterName, x);
    }

    @Override
    public void setNString(String parameterName, String value) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setNString",
                () -> this.statement.setNString(parameterName, value),
                parameterName, value);
    }

    @Override
    public void setNCharacterStream(String parameterName, Reader value, long length)
            throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setNCharacterStream",
                () -> this.statement.setNCharacterStream(parameterName, value, length),
                parameterName, value, length);
    }

    @Override
    public void setNClob(String parameterName, NClob value) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setNClob",
                () -> this.statement.setNClob(parameterName, value),
                parameterName, value);
    }

    @Override
    public void setClob(String parameterName, Reader reader, long length) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setClob",
                () -> this.statement.setClob(parameterName, reader, length),
                parameterName, reader, length);
    }

    @Override
    public void setBlob(String parameterName, InputStream inputStream, long length)
            throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setBlob",
                () -> this.statement.setBlob(parameterName, inputStream, length),
                parameterName, inputStream, length);
    }

    @Override
    public void setNClob(String parameterName, Reader reader, long length) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setNClob",
                () -> this.statement.setNClob(parameterName, reader, length),
                parameterName, reader, length);
    }

    @Override
    public NClob getNClob(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                NClob.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getNClob",
                () -> this.statement.getNClob(parameterIndex),
                parameterIndex);
    }

    @Override
    public NClob getNClob(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                NClob.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getNClob",
                () -> this.statement.getNClob(parameterName),
                parameterName);
    }

    @Override
    public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
        //noinspection SpellCheckingInspection
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setSQLXML",
                () -> this.statement.setSQLXML(parameterName, xmlObject),
                parameterName, xmlObject);
    }

    @Override
    public SQLXML getSQLXML(int parameterIndex) throws SQLException {
        //noinspection SpellCheckingInspection
        return WrapperUtils.executeWithPlugins(
                SQLXML.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getSQLXML",
                () -> this.statement.getSQLXML(parameterIndex),
                parameterIndex);
    }

    @Override
    public SQLXML getSQLXML(String parameterName) throws SQLException {
        //noinspection SpellCheckingInspection
        return WrapperUtils.executeWithPlugins(
                SQLXML.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getSQLXML",
                () -> this.statement.getSQLXML(parameterName),
                parameterName);
    }

    @Override
    public String getNString(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                String.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getNString",
                () -> this.statement.getNString(parameterIndex),
                parameterIndex);
    }

    @Override
    public String getNString(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                String.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getNString",
                () -> this.statement.getNString(parameterName),
                parameterName);
    }

    @Override
    public Reader getNCharacterStream(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Reader.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getNCharacterStream",
                () -> this.statement.getNCharacterStream(parameterIndex),
                parameterIndex);
    }

    @Override
    public Reader getNCharacterStream(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Reader.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getNCharacterStream",
                () -> this.statement.getNCharacterStream(parameterName),
                parameterName);
    }

    @Override
    public Reader getCharacterStream(int parameterIndex) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Reader.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getCharacterStream",
                () -> this.statement.getCharacterStream(parameterIndex),
                parameterIndex);
    }

    @Override
    public Reader getCharacterStream(String parameterName) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Reader.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getCharacterStream",
                () -> this.statement.getCharacterStream(parameterName),
                parameterName);
    }

    @Override
    public void setBlob(String parameterName, Blob x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setBlob",
                () -> this.statement.setBlob(parameterName, x),
                parameterName, x);
    }

    @Override
    public void setClob(String parameterName, Clob x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setClob",
                () -> this.statement.setClob(parameterName, x),
                parameterName, x);
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x, long length) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setAsciiStream",
                () -> this.statement.setAsciiStream(parameterName, x, length),
                parameterName, x, length);
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x, long length)
            throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setBinaryStream",
                () -> this.statement.setBinaryStream(parameterName, x, length),
                parameterName, x, length);
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader, long length)
            throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setCharacterStream",
                () -> this.statement.setCharacterStream(parameterName, reader, length),
                parameterName, reader, length);
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setAsciiStream",
                () -> this.statement.setAsciiStream(parameterName, x),
                parameterName, x);
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setBinaryStream",
                () -> this.statement.setBinaryStream(parameterName, x),
                parameterName, x);
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setCharacterStream",
                () -> this.statement.setCharacterStream(parameterName, reader),
                parameterName, reader);
    }

    @Override
    public void setNCharacterStream(String parameterName, Reader value) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setNCharacterStream",
                () -> this.statement.setNCharacterStream(parameterName, value),
                parameterName, value);
    }

    @Override
    public void setClob(String parameterName, Reader reader) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setClob",
                () -> this.statement.setClob(parameterName, reader),
                parameterName, reader);
    }

    @Override
    public void setBlob(String parameterName, InputStream inputStream) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setBlob",
                () -> this.statement.setBlob(parameterName, inputStream),
                parameterName, inputStream);
    }

    @Override
    public void setNClob(String parameterName, Reader reader) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setNClob",
                () -> this.statement.setNClob(parameterName, reader),
                parameterName, reader);
    }

    @Override
    public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                type,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getObject",
                () -> this.statement.getObject(parameterIndex, type),
                parameterIndex, type);
    }

    @Override
    public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                type,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getObject",
                () -> this.statement.getObject(parameterName, type),
                parameterName, type);
    }

    @Override
    public void setObject(String parameterName, Object x, SQLType targetSqlType, int scaleOrLength)
            throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setObject",
                () -> this.statement.setObject(parameterName, x, targetSqlType, scaleOrLength),
                parameterName, x, targetSqlType, scaleOrLength);
    }

    @Override
    public void setObject(String parameterName, Object x, SQLType targetSqlType) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setObject",
                () -> this.statement.setObject(parameterName, x, targetSqlType),
                parameterName, x, targetSqlType);
    }

    @Override
    public void registerOutParameter(int parameterIndex, SQLType sqlType) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.registerOutParameter",
                () -> this.statement.registerOutParameter(parameterIndex, sqlType),
                parameterIndex, sqlType);
    }

    @Override
    public void registerOutParameter(int parameterIndex, SQLType sqlType, int scale)
            throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.registerOutParameter",
                () -> this.statement.registerOutParameter(parameterIndex, sqlType, scale),
                parameterIndex, sqlType, scale);
    }

    @Override
    public void registerOutParameter(int parameterIndex, SQLType sqlType, String typeName)
            throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.registerOutParameter",
                () -> this.statement.registerOutParameter(parameterIndex, sqlType, typeName),
                parameterIndex, sqlType, typeName);
    }

    @Override
    public void registerOutParameter(String parameterName, SQLType sqlType) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.registerOutParameter",
                () -> this.statement.registerOutParameter(parameterName, sqlType),
                parameterName, sqlType);
    }

    @Override
    public void registerOutParameter(String parameterName, SQLType sqlType, int scale)
            throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.registerOutParameter",
                () -> this.statement.registerOutParameter(parameterName, sqlType, scale),
                parameterName, sqlType, scale);
    }

    @Override
    public void registerOutParameter(String parameterName, SQLType sqlType, String typeName)
            throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.registerOutParameter",
                () -> this.statement.registerOutParameter(parameterName, sqlType, typeName),
                parameterName, sqlType, typeName);
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                ResultSet.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.executeQuery",
                () -> this.statement.executeQuery());
    }

    @Override
    public int executeUpdate() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                int.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.executeUpdate",
                () -> this.statement.executeUpdate());
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setNull",
                () -> this.statement.setNull(parameterIndex, sqlType),
                parameterIndex, sqlType);
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setBoolean",
                () -> this.statement.setBoolean(parameterIndex, x),
                parameterIndex, x);
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setByte",
                () -> this.statement.setByte(parameterIndex, x),
                parameterIndex, x);
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setShort",
                () -> this.statement.setShort(parameterIndex, x),
                parameterIndex, x);
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setInt",
                () -> this.statement.setInt(parameterIndex, x),
                parameterIndex, x);
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setLong",
                () -> this.statement.setLong(parameterIndex, x),
                parameterIndex, x);
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setFloat",
                () -> this.statement.setFloat(parameterIndex, x),
                parameterIndex, x);
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setDouble",
                () -> this.statement.setDouble(parameterIndex, x),
                parameterIndex, x);
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setBigDecimal",
                () -> this.statement.setBigDecimal(parameterIndex, x),
                parameterIndex, x);
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setString",
                () -> this.statement.setString(parameterIndex, x),
                parameterIndex, x);
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setBytes",
                () -> this.statement.setBytes(parameterIndex, x),
                parameterIndex, x);
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setDate",
                () -> this.statement.setDate(parameterIndex, x),
                parameterIndex, x);
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setTime",
                () -> this.statement.setTime(parameterIndex, x),
                parameterIndex, x);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setTimestamp",
                () -> this.statement.setTimestamp(parameterIndex, x),
                parameterIndex, x);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setAsciiStream",
                () -> this.statement.setAsciiStream(parameterIndex, x, length),
                parameterIndex, x, length);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setUnicodeStream",
                () -> this.statement.setUnicodeStream(parameterIndex, x, length),
                parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setBinaryStream",
                () -> this.statement.setBinaryStream(parameterIndex, x, length),
                parameterIndex, x, length);
    }

    @Override
    public void clearParameters() throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.clearParameters",
                () -> this.statement.clearParameters());
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setObject",
                () -> this.statement.setObject(parameterIndex, x, targetSqlType),
                parameterIndex, x, targetSqlType);
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setObject",
                () -> this.statement.setObject(parameterIndex, x),
                parameterIndex, x);
    }

    @Override
    public boolean execute() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                boolean.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.execute",
                () -> this.statement.execute());
    }

    @Override
    public void addBatch() throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.addBatch",
                () -> this.statement.addBatch());
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length)
            throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setCharacterStream",
                () -> this.statement.setCharacterStream(parameterIndex, reader, length),
                parameterIndex, reader, length);
    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setRef",
                () -> this.statement.setRef(parameterIndex, x),
                parameterIndex, x);
    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setBlob",
                () -> this.statement.setBlob(parameterIndex, x),
                parameterIndex, x);
    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setClob",
                () -> this.statement.setClob(parameterIndex, x),
                parameterIndex, x);
    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setArray",
                () -> this.statement.setArray(parameterIndex, x),
                parameterIndex, x);
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                ResultSetMetaData.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getMetaData",
                () -> this.statement.getMetaData());
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setDate",
                () -> this.statement.setDate(parameterIndex, x, cal),
                parameterIndex, x, cal);
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setTime",
                () -> this.statement.setTime(parameterIndex, x, cal),
                parameterIndex, x, cal);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setTimestamp",
                () -> this.statement.setTimestamp(parameterIndex, x, cal),
                parameterIndex, x, cal);
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setNull",
                () -> this.statement.setNull(parameterIndex, sqlType, typeName),
                parameterIndex, sqlType, typeName);
    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setURL",
                () -> this.statement.setURL(parameterIndex, x),
                parameterIndex, x);
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                ParameterMetaData.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getParameterMetaData",
                () -> this.statement.getParameterMetaData());
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setRowId",
                () -> this.statement.setRowId(parameterIndex, x),
                parameterIndex, x);
    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setNString",
                () -> this.statement.setNString(parameterIndex, value),
                parameterIndex, value);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length)
            throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setNCharacterStream",
                () -> this.statement.setNCharacterStream(parameterIndex, value, length),
                parameterIndex, value, length);
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setNClob",
                () -> this.statement.setNClob(parameterIndex, value),
                parameterIndex, value);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setClob",
                () -> this.statement.setClob(parameterIndex, reader, length),
                parameterIndex, reader, length);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length)
            throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setBlob",
                () -> this.statement.setBlob(parameterIndex, inputStream, length),
                parameterIndex, inputStream, length);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setNClob",
                () -> this.statement.setNClob(parameterIndex, reader, length),
                parameterIndex, reader, length);
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        //noinspection SpellCheckingInspection
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setSQLXML",
                () -> this.statement.setSQLXML(parameterIndex, xmlObject),
                parameterIndex, xmlObject);
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength)
            throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setObject",
                () -> this.statement.setObject(parameterIndex, x, targetSqlType, scaleOrLength),
                parameterIndex, x, targetSqlType, scaleOrLength);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setAsciiStream",
                () -> this.statement.setAsciiStream(parameterIndex, x, length),
                parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setBinaryStream",
                () -> this.statement.setBinaryStream(parameterIndex, x, length),
                parameterIndex, x, length);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length)
            throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setCharacterStream",
                () -> this.statement.setCharacterStream(parameterIndex, reader, length),
                parameterIndex, reader, length);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setAsciiStream",
                () -> this.statement.setAsciiStream(parameterIndex, x),
                parameterIndex, x);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setBinaryStream",
                () -> this.statement.setBinaryStream(parameterIndex, x),
                parameterIndex, x);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setCharacterStream",
                () -> this.statement.setCharacterStream(parameterIndex, reader),
                parameterIndex, reader);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setNCharacterStream",
                () -> this.statement.setNCharacterStream(parameterIndex, value),
                parameterIndex, value);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setClob",
                () -> this.statement.setClob(parameterIndex, reader),
                parameterIndex, reader);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setBlob",
                () -> this.statement.setBlob(parameterIndex, inputStream),
                parameterIndex, inputStream);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setNClob",
                () -> this.statement.setNClob(parameterIndex, reader),
                parameterIndex, reader);
    }

    @Override
    public void setObject(int parameterIndex, Object x, SQLType targetSqlType, int scaleOrLength)
            throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setObject",
                () -> this.statement.setObject(parameterIndex, x, targetSqlType, scaleOrLength),
                parameterIndex, x, targetSqlType, scaleOrLength);
    }

    @Override
    public void setObject(int parameterIndex, Object x, SQLType targetSqlType) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setObject",
                () -> this.statement.setObject(parameterIndex, x, targetSqlType),
                parameterIndex, x, targetSqlType);
    }

    @Override
    public long executeLargeUpdate() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                long.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.executeLargeUpdate",
                () -> this.statement.executeLargeUpdate());
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                ResultSet.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.executeQuery",
                () -> this.statement.executeQuery(sql),
                sql);
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                int.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.executeUpdate",
                () -> this.statement.executeUpdate(sql),
                sql);
    }

    @Override
    public void close() throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.close",
                () -> this.statement.close());
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                int.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getMaxFieldSize",
                () -> this.statement.getMaxFieldSize());
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setMaxFieldSize",
                () -> this.statement.setMaxFieldSize(max),
                max);
    }

    @Override
    public int getMaxRows() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                int.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getMaxRows",
                () -> this.statement.getMaxRows());
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setMaxRows",
                () -> this.statement.setMaxRows(max),
                max);
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setEscapeProcessing",
                () -> this.statement.setEscapeProcessing(enable),
                enable);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                int.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getQueryTimeout",
                () -> this.statement.getQueryTimeout());
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setQueryTimeout",
                () -> this.statement.setQueryTimeout(seconds),
                seconds);
    }

    @Override
    public void cancel() throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.cancel",
                () -> this.statement.cancel());
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                SQLWarning.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getWarnings",
                () -> this.statement.getWarnings());
    }

    @Override
    public void clearWarnings() throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.clearWarnings",
                () -> this.statement.clearWarnings());
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setCursorName",
                () -> this.statement.setCursorName(name),
                name);
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                boolean.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.execute",
                () -> this.statement.execute(sql),
                sql);
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                ResultSet.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getResultSet",
                () -> this.statement.getResultSet());
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                int.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getUpdateCount",
                () -> this.statement.getUpdateCount());
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                boolean.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getMoreResults",
                () -> this.statement.getMoreResults());
    }

    @SuppressWarnings("MagicConstant")
    @Override
    public int getFetchDirection() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                int.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getFetchDirection",
                () -> this.statement.getFetchDirection());
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setFetchDirection",
                () -> this.statement.setFetchDirection(direction),
                direction);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                int.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getFetchSize",
                () -> this.statement.getFetchSize());
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setFetchSize",
                () -> this.statement.setFetchSize(rows),
                rows);
    }

    @SuppressWarnings("MagicConstant")
    @Override
    public int getResultSetConcurrency() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                int.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getResultSetConcurrency",
                () -> this.statement.getResultSetConcurrency());
    }

    @SuppressWarnings("MagicConstant")
    @Override
    public int getResultSetType() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                int.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getResultSetType",
                () -> this.statement.getResultSetType());
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.addBatch",
                () -> this.statement.addBatch(sql),
                sql);
    }

    @Override
    public void clearBatch() throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.clearBatch",
                () -> this.statement.clearBatch());
    }

    @Override
    public int[] executeBatch() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                int[].class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.executeBatch",
                () -> this.statement.executeBatch());
    }

    @Override
    public Connection getConnection() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                Connection.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getConnection",
                () -> this.statement.getConnection());
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                boolean.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getMoreResults",
                () -> this.statement.getMoreResults(current),
                current);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                ResultSet.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getGeneratedKeys",
                () -> this.statement.getGeneratedKeys());
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                int.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.executeUpdate",
                () -> this.statement.executeUpdate(sql, autoGeneratedKeys),
                sql, autoGeneratedKeys);
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                int.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.executeUpdate",
                () -> this.statement.executeUpdate(sql, columnIndexes),
                sql, columnIndexes);
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                int.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.executeUpdate",
                () -> this.statement.executeUpdate(sql, columnNames),
                sql, columnNames);
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                boolean.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.execute",
                () -> this.statement.execute(sql, autoGeneratedKeys),
                sql, autoGeneratedKeys);
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                boolean.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.execute",
                () -> this.statement.execute(sql, columnIndexes),
                sql, columnIndexes);
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return WrapperUtils.executeWithPlugins(
                boolean.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.execute",
                () -> this.statement.execute(sql, columnNames),
                sql, columnNames);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                int.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.getResultSetHoldability",
                () -> this.statement.getResultSetHoldability());
    }

    @Override
    public boolean isClosed() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                boolean.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.isClosed",
                () -> this.statement.isClosed());
    }

    @Override
    public boolean isPoolable() throws SQLException {
        //noinspection SpellCheckingInspection
        return WrapperUtils.executeWithPlugins(
                boolean.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.isPoolable",
                () -> this.statement.isPoolable());
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.setPoolable",
                () -> this.statement.setPoolable(poolable),
                poolable);
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        WrapperUtils.runWithPlugins(
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.closeOnCompletion",
                () -> this.statement.closeOnCompletion());
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return WrapperUtils.executeWithPlugins(
                boolean.class,
                SQLException.class,
                this.pluginManager,
                this.statement,
                "CallableStatement.isCloseOnCompletion",
                () -> this.statement.isCloseOnCompletion());
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return this.statement.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return this.statement.isWrapperFor(iface);
    }
}
