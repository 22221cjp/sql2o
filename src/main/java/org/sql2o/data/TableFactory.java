package org.sql2o.data;

import org.sql2o.Sql2oException;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 11/19/11
 * Time: 9:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class TableFactory {
    
    public static Table createTable(ResultSet rs){
        Table table = new Table();
        try {
            applyMetadata(table, rs.getMetaData());

        } catch (SQLException e) {
            throw new Sql2oException("Error while reading metadata from database", e);
        }

        try {
            while(rs.next()){
                Row row = new Row(table);
                table.rows().add(row);
                for (Column column : table.columns()){
                    row.addValue(column.getIndex(), rs.getObject(column.getIndex() + 1));
                }
            }
        } catch (SQLException e) {
            throw new Sql2oException("Error while filling Table with data from database", e);
        }
        
        return table;
    }
    
    private static void applyMetadata(Table table, ResultSetMetaData metadata) throws SQLException {
        
        table.setName( metadata.getTableName(1) );

        for (int colIdx = 1; colIdx <= metadata.getColumnCount(); colIdx++){
            String colName = metadata.getColumnName(colIdx);
            String colType = metadata.getColumnTypeName(colIdx);
            table.columns().add(new Column(colName, colIdx - 1, colType));
            table.getColumnNameToIdxMap().put(colName, colIdx - 1);
        }
    }
}
