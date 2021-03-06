/*
 * Copyright 1999-2017 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.druid.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.sql.XAConnection;
import javax.transaction.xa.XAException;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleResultSet;
import oracle.jdbc.OracleStatement;
import oracle.jdbc.internal.OraclePreparedStatement;
import oracle.jdbc.xa.client.OracleXAConnection;
import oracle.sql.ROWID;

import com.alibaba.druid.pool.DruidPooledConnection;
import com.alibaba.druid.support.logging.Log;
import com.alibaba.druid.support.logging.LogFactory;

public class OracleUtils {

    private final static Log LOG = LogFactory.getLog(OracleUtils.class);

    public static XAConnection OracleXAConnection(Connection oracleConnection) throws XAException {
        return new OracleXAConnection(oracleConnection);
    }

    public static int getRowPrefetch(PreparedStatement stmt) throws SQLException {
        OracleStatement oracleStmt = stmt.unwrap(OracleStatement.class);
        
        if (oracleStmt == null) {
            return -1;
        }
        
        return oracleStmt.getRowPrefetch();
    }

    public static void setRowPrefetch(PreparedStatement stmt, int value) throws SQLException {
        OracleStatement oracleStmt = stmt.unwrap(OracleStatement.class);
        if (oracleStmt != null) {
            oracleStmt.setRowPrefetch(value);
        }
    }

    public static void enterImplicitCache(PreparedStatement stmt) throws SQLException {
        oracle.jdbc.internal.OraclePreparedStatement oracleStmt = unwrapInternal(stmt);
        if (oracleStmt != null) {
            oracleStmt.enterImplicitCache();
        }
    }

    public static void exitImplicitCacheToClose(PreparedStatement stmt) throws SQLException {
        oracle.jdbc.internal.OraclePreparedStatement oracleStmt = unwrapInternal(stmt);
        if (oracleStmt != null) {
            oracleStmt.exitImplicitCacheToClose();
        }
    }

    public static void exitImplicitCacheToActive(PreparedStatement stmt) throws SQLException {
        oracle.jdbc.internal.OraclePreparedStatement oracleStmt = unwrapInternal(stmt);
        if (oracleStmt != null) {
            oracleStmt.exitImplicitCacheToActive();
        }
    }

    public static OraclePreparedStatement unwrapInternal(PreparedStatement stmt) throws SQLException {
        if (stmt instanceof OraclePreparedStatement) {
            return (OraclePreparedStatement) stmt;
        }

        OraclePreparedStatement unwrapped = stmt.unwrap(OraclePreparedStatement.class);

        if (unwrapped == null) {
            LOG.error("can not unwrap statement : " + stmt.getClass());
        }

        return unwrapped;
    }

    public static short getVersionNumber(DruidPooledConnection conn) throws SQLException {
        oracle.jdbc.internal.OracleConnection oracleConn = (oracle.jdbc.internal.OracleConnection) unwrap(conn);
        return oracleConn.getVersionNumber();
    }

    public static void setDefaultRowPrefetch(Connection conn, int value) throws SQLException {
        OracleConnection oracleConn = unwrap(conn);
        oracleConn.setDefaultRowPrefetch(value);
    }

    public static int getDefaultRowPrefetch(Connection conn, int value) throws SQLException {
        OracleConnection oracleConn = unwrap(conn);
        return oracleConn.getDefaultRowPrefetch();
    }

    public static boolean getImplicitCachingEnabled(Connection conn) throws SQLException {
        OracleConnection oracleConn = unwrap(conn);
        return oracleConn.getImplicitCachingEnabled();
    }

    public static int getStatementCacheSize(Connection conn) throws SQLException {
        OracleConnection oracleConn = unwrap(conn);
        return oracleConn.getStatementCacheSize();
    }

    public static void purgeImplicitCache(Connection conn) throws SQLException {
        OracleConnection oracleConn = unwrap(conn);
        oracleConn.purgeImplicitCache();
    }

    public static void setImplicitCachingEnabled(Connection conn, boolean cache) throws SQLException {
        OracleConnection oracleConn = unwrap(conn);
        oracleConn.setImplicitCachingEnabled(cache);
    }

    public static void setStatementCacheSize(Connection conn, int size) throws SQLException {
        OracleConnection oracleConn = unwrap(conn);
        oracleConn.setStatementCacheSize(size);
    }

    public static int pingDatabase(Connection conn) throws SQLException {
        OracleConnection oracleConn = unwrap(conn);
        return oracleConn.pingDatabase();
    }

    public static void openProxySession(Connection conn, int type, java.util.Properties prop) throws SQLException {
        OracleConnection oracleConn = unwrap(conn);
        oracleConn.openProxySession(type, prop);
    }

    public static int getDefaultExecuteBatch(Connection conn) throws SQLException {
        OracleConnection oracleConn = unwrap(conn);
        return oracleConn.getDefaultExecuteBatch();
    }

    public static OracleConnection unwrap(Connection conn) throws SQLException {
        if (conn instanceof OracleConnection) {
            return (OracleConnection) conn;
        }

        return conn.unwrap(OracleConnection.class);
    }

    public static ROWID getROWID(ResultSet rs, int columnIndex) throws SQLException {
        OracleResultSet oracleResultSet = rs.unwrap(OracleResultSet.class);
        return oracleResultSet.getROWID(columnIndex);
    }

    private static Set<String> builtinFunctions;

    public static boolean isBuiltinFunction(String function) {
        if (function == null) {
            return false;
        }

        String function_lower = function.toLowerCase();

        Set<String> functions = builtinFunctions;

        if (functions == null) {
            functions = new HashSet<String>();
            Utils.loadFromFile("META-INF/druid/parser/oracle/builtin_functions", functions);
            builtinFunctions = functions;
        }

        return functions.contains(function_lower);
    }

    private static Set<String> builtinTables;

    public static boolean isBuiltinTable(String table) {
        if (table == null) {
            return false;
        }

        String table_lower = table.toLowerCase();

        Set<String> tables = builtinTables;

        if (tables == null) {
            tables = new HashSet<String>();
            Utils.loadFromFile("META-INF/druid/parser/oracle/builtin_tables", tables);
            builtinTables = tables;
        }

        return tables.contains(table_lower);
    }
}
