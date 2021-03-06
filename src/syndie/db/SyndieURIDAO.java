package syndie.db;

import java.net.URISyntaxException;
import java.sql.*;
import java.util.*;

import syndie.data.SyndieURI;
import syndie.util.StringUtil;

import net.i2p.util.Log;

/**
 *  Data Access Object
 */
class SyndieURIDAO {
    private final Log _log;
    private final DBClient _client;

    public SyndieURIDAO(DBClient client) {
        _client = client;
        _log = client.ctx().logManager().getLog(SyndieURIDAO.class);
    }
    
    private static final String KEY_TYPE = "__TYPE";
    
    private static final String SQL_FETCH = "SELECT attribKey, attribValString, attribValLong, attribValBool, attribValStrings FROM uriAttribute WHERE uriId = ?";

    public SyndieURI fetch(long uriId) {
        if (uriId < 0) return null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Map attribs = new TreeMap();
        String type = null;
        try {
            stmt = _client.con().prepareStatement(SQL_FETCH);
            stmt.setLong(1, uriId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                String key = rs.getString(1);
                String valStr = rs.getString(2);
                if (!rs.wasNull()) {
                    if (KEY_TYPE.equals(key))
                        type = valStr;
                    else
                        attribs.put(key, valStr);
                } else {
                    long valLong = rs.getLong(3);
                    if (!rs.wasNull()) {
                        attribs.put(key, Long.valueOf(valLong));
                    } else {
                        boolean valBool = rs.getBoolean(4);
                        if (!rs.wasNull()) {
                            attribs.put(key, Boolean.valueOf(valBool));
                        } else {
                            String valStrings = rs.getString(5);
                            if (!rs.wasNull()) {
                                String vals[] = StringUtil.split('\n', valStrings); //valStrings.split("\n");
                                attribs.put(key, vals);
                            } else {
                                // all null
                            }
                        }
                    }
                }
            }
        } catch (SQLException se) {
            _log.error("error fetching the uri [" + uriId + "]", se);
            return null;
        } catch (RuntimeException re) {
            _log.error("internal error fetching the uri [" + uriId + "]", re);
            return null;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException se) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException se) {}
        }
        //if (_log.shouldLog(Log.DEBUG))
        //    _log.debug("URI found for " + uriId + ": " + type + ":" + attribs);
        if (type == null) return null;
        return new SyndieURI(type, attribs);
    }
    
    private static final String SQL_NEXTID = "SELECT NEXT VALUE FOR uriIdSequence FROM information_schema.system_sequences WHERE SEQUENCE_NAME = 'URIIDSEQUENCE'";

    private long nextId() {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = _client.con().prepareStatement(SQL_NEXTID);
            rs = stmt.executeQuery();
            if (rs.next()) {
                long rv = rs.getLong(1);
                if (rs.wasNull())
                    return -1;
                else
                    return rv;
            } else {
                return -1;
            }
        } catch (SQLException se) {
            if (_log.shouldLog(Log.ERROR))
                _log.error("Error retrieving the next uri ID", se);
            return -1;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException se) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException se) {}
        }
    }
    
    
    private static final String SQL_INSERT = "INSERT INTO uriAttribute (attribKey, attribValString, attribValLong, attribValBool, attribValStrings, uriId, isDescriptive) VALUES (?, ?, ?, ?, ?, ?, ?)";

    public long add(SyndieURI uri) {
        long id = nextId();
        if (id < 0)
            return id;
        PreparedStatement stmt = null;
        try {
            stmt = _client.con().prepareStatement(SQL_INSERT);
            
            String type = uri.getType();
            insertAttrib(stmt, KEY_TYPE, type, null, null, null, id, false);
            if (_log.shouldLog(Log.DEBUG))
                _log.debug("URI " + id + " added with type " + type);
            Map attributes = uri.getAttributes();
            for (Iterator iter = attributes.entrySet().iterator(); iter.hasNext(); ) {
                Map.Entry entry = (Map.Entry)iter.next();
                String key = (String)entry.getKey();
                Object val = entry.getValue();
                if (val == null)
                    continue;
                if (val.getClass().isArray()) {
                    String vals[] = (String[])val;
                    insertAttrib(stmt, key, null, null, null, vals, id, false);
                } else if (val instanceof Long) {
                    insertAttrib(stmt, key, null, (Long)val, null, null, id, false);
                } else if (val instanceof Boolean) {
                    insertAttrib(stmt, key, null, null, (Boolean)val, null, id, false);
                } else {
                    insertAttrib(stmt, key, val.toString(), null, null, null, id, false);
                }
                if (_log.shouldLog(Log.DEBUG))
                    _log.debug("URI attribute " + key + " added to " + id);
            }
            return id;
        } catch (SQLException se) {
            if (_log.shouldLog(Log.ERROR))
                _log.error("Error adding the uri", se);
            return -1;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException se) {}
        }
    }

    private void insertAttrib(PreparedStatement stmt, String key, String valString, Long valLong, Boolean valBool, String valStrings[], long id, boolean isDescriptive) throws SQLException {
        //"INSERT INTO uriAttribute
        // (attribKey, attribValString, attribValLong, attribValBool, attribValStrings, uriId, isDescriptive)
        // VALUES (?, ?, ?, ?, ?, ?, ?)";
        stmt.setString(1, key);
        if (valString != null)
            stmt.setString(2, valString);
        else
            stmt.setNull(2, Types.VARCHAR);
        if (valLong != null)
            stmt.setLong(3, valLong.longValue());
        else
            stmt.setNull(3, Types.BIGINT);
        if (valBool != null)
            stmt.setBoolean(4, valBool.booleanValue());
        else
            stmt.setNull(4, Types.BOOLEAN);
        if (valStrings != null) {
            StringBuilder buf = new StringBuilder(64);
            for (int i = 0; i < valStrings.length; i++)
                buf.append(valStrings[i]).append('\n');
            stmt.setString(5, buf.toString());
        } else {
            stmt.setNull(5, Types.VARCHAR);
        }
        stmt.setLong(6, id);
        stmt.setBoolean(7, isDescriptive);
        int rows = stmt.executeUpdate();
        if (rows != 1)
            throw new SQLException("Insert added "+rows+" rows");
    }
}
