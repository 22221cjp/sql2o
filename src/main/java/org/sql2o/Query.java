package org.sql2o;

import org.joda.time.DateTime;
import org.sql2o.converters.Convert;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;
import org.sql2o.data.Table;
import org.sql2o.data.TableFactory;
import org.sql2o.reflection.Pojo;
import org.sql2o.reflection.PojoMetadata;
import org.sql2o.tools.NamedParameterStatement;

import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 5/18/11
 * Time: 8:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class Query {

    public Query(Connection connection, String queryText, String name) {
        this.connection = connection;
        this.name = name;

        try{
            statement = new NamedParameterStatement(connection.getJdbcConnection(), queryText);
        }
        catch(Exception ex){
            throw new RuntimeException(ex);
        }

        this.setColumnMappings(connection.getSql2o().getDefaultColumnMappings());
        this.caseSensitive = connection.getSql2o().isDefaultCaseSensitive();
        this.methodsMap = new HashMap<String, Method>();
    }

    private Connection connection;

    private Map<String, String> caseSensitiveColumnMappings;
    private Map<String, String> columnMappings;
    private Map<String, Method> methodsMap;

    private NamedParameterStatement statement;

    private boolean caseSensitive;
    
    private final String name;

    public Query addParameter(String name, Object value){
        try{
            statement.setObject(name, value);
        }
        catch(SQLException ex){
            throw new RuntimeException(ex);
        }
        return this;
    }
    
    public Query addParameter(String name, int value){
        try{
            statement.setInt(name, value);
        }
        catch (SQLException ex){
            throw new Sql2oException(ex);
        }
        return this;
    }

    public Query addParameter(String name, Integer value){
        try{
            if (value == null){
                statement.setNull(name, Types.INTEGER);
            }else{
                statement.setInt(name, value);
            }
        }
        catch(SQLException ex){
            throw new RuntimeException(ex);
        }
        return this;
    }

    public Query addParameter(String name, long value){
        try{
            statement.setLong(name, value);
        }
        catch(SQLException ex){
            throw new RuntimeException(ex);
        }
        return this;
    }
    
    public Query addParameter(String name, Long value){
        try{
            if (value == null){
                statement.setNull(name, Types.INTEGER);
            } else {
                statement.setLong(name, value);
            }
        }
        catch (SQLException ex){
            throw new Sql2oException(ex);
        }
        return this;
    }

    public Query addParameter(String name, String value){
        try{
            if (value == null){
                statement.setNull(name, Types.VARCHAR);
            }else{
                statement.setString(name, value);
            }
        }
        catch(Exception ex){
            throw new RuntimeException(ex);
        }
        return this;
    }

    public Query addParameter(String name, Timestamp value){
        try{
            if (value == null){
                statement.setNull(name, Types.TIMESTAMP);
            } else {
                statement.setTimestamp(name, value);
            }
        }
        catch(Exception ex){
            throw new RuntimeException(ex);
        }
        return this;
    }

    public Query addParameter(String name, Date value){
        try{
            if (value == null){
                statement.setNull(name, Types.DATE);
            } else {
                statement.setDate(name, value);
            }
        }
        catch (Exception ex){
            throw new RuntimeException(ex);
        }

        return this;
    }

    public Query addParameter(String name, java.util.Date value){
        Date sqlDate = value == null ? null : new Date(value.getTime());
        return addParameter(name, sqlDate);
    }

    public Query addParameter(String name, Time value){
        try {
            if (value == null){
                statement.setNull(name, Types.TIME);
            } else {
                statement.setTime(name,value);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public Query addParameter(String name, DateTime value){
        return addParameter(name, value.toDate());
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public Query setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        return this;
    }
    
    public Connection getConnection(){
        return this.connection;
    }

    public String getName() {
        return name;
    }

    //    public List[] executeAndFetchMultiple(Class ... returnTypes){
//        List<List> listOfLists = new ArrayList<List>();
//
//        try {
//            boolean hasResult = statement.getStatement().execute();
//
//
//            for (Class clazz : returnTypes){
//                List objList = new ArrayList();
//                PojoMetadata metadata = new PojoMetadata(clazz, this.isCaseSensitive(), this.getColumnMappings());
//
//                ResultSet rs = statement.getStatement().getResultSet();
//                ResultSetMetaData meta = rs.getMetaData();
//
//                while (rs.next()){
//                    Pojo pojo = new Pojo(metadata, this.isCaseSensitive());
//
//                    for (int colIdx = 1; colIdx <= meta.getColumnCount(); colIdx++){
//                        String colName = meta.getColumnName(colIdx);
//                        pojo.setProperty(colName, rs.getObject(colIdx));
//                    }
//
//                    objList.add(pojo.getObject());
//                }
//
//                rs.close();
//
//                listOfLists.add(objList);
//
//                hasResult = statement.getStatement().getMoreResults();
//            }
//
//        } catch (SQLException e) {
//            throw new Sql2oException("Database error", e);
//        }
//        finally {
//            closeConnectionIfNecessary();
//        }
//
//        return listOfLists.toArray(new ArrayList[listOfLists.size()]);
//    }

    public <T> List<T> executeAndFetch(Class returnType){
        List list = new ArrayList();
        PojoMetadata metadata = new PojoMetadata(returnType, this.isCaseSensitive(), this.getColumnMappings());
        try{
            //java.util.Date st = new java.util.Date();
            long start = System.currentTimeMillis();
            ResultSet rs = statement.executeQuery();
            long afterExecQuery = System.currentTimeMillis();

            ResultSetMetaData meta = rs.getMetaData();

            while(rs.next()){

                Pojo pojo = new Pojo(metadata, this.isCaseSensitive());

                //Object obj = returnType.newInstance();
                for(int colIdx = 1; colIdx <= meta.getColumnCount(); colIdx++){
                    String colName = meta.getColumnName(colIdx);
                    pojo.setProperty(colName, rs.getObject(colIdx));
                }

                list.add(pojo.getObject());
            }


            rs.close();
            long afterClose = System.currentTimeMillis();

            System.out.println(String.format("total: %d ms, execution: %d ms, reading and parsing: %d ms; executed [%s]", afterClose - start, afterExecQuery-start, afterClose - afterExecQuery, this.getName() == null ? "No name" : this.getName()));
        }
        catch(SQLException ex){
            throw new Sql2oException("Database error", ex);
        }
        finally {
            closeConnectionIfNecessary();
        }

        return list;
    }

    public <T> T executeAndFetchFirst(Class returnType){
        List l = this.executeAndFetch(returnType);
        if (l.size() == 0){
            return null;
        }
        else{
            return (T)l.get(0);
        }
    }
    
    public Table executeAndFetchTable(){
        ResultSet rs;
        long start = System.currentTimeMillis();
        try {
            rs = statement.executeQuery();
            long afterExecute = System.currentTimeMillis();
            Table table = TableFactory.createTable(rs, this.isCaseSensitive());
            long afterClose = System.currentTimeMillis();
            
            System.out.println(String.format("total: %d ms, execution: %d ms, reading and parsing: %d ms; executed fetch table [%s]", afterClose - start, afterExecute-start, afterClose - afterExecute, this.getName() == null ? "No name" : this.getName()));
            
            return table;
        } catch (SQLException e) {
            throw new Sql2oException("Error while executing query", e);
        } finally {
            closeConnectionIfNecessary();
        }
    }

    public Connection executeUpdate(){
        long start = System.currentTimeMillis();
        try{
            this.connection.setResult(statement.executeUpdate());
            this.connection.setKeys(statement.getStatement().getGeneratedKeys());
            connection.setCanGetKeys(true);
        }
        catch(SQLException ex){
            this.connection.rollback();
            throw new RuntimeException(ex);
        }
        finally {
            closeConnectionIfNecessary();
        }

        long end = System.currentTimeMillis();
        System.out.println(String.format("total: %d ms; executed update [%s] ", end - start, this.getName() == null ? "No name" : this.getName()));

        return this.connection;
    }

    public Object executeScalar(){
        long start = System.currentTimeMillis();
        try {
            ResultSet rs = this.statement.executeQuery();
            if (rs.next()){
                Object o = rs.getObject(1);
                long end = System.currentTimeMillis();
                System.out.println(String.format("total: %d ms; executed scalar [%s] ",end - start, this.getName() == null ? "No name" : this.getName()));
                return o;
            }
            else{
                return null;
            }

        }
        catch (SQLException e) {
            this.connection.rollback();
            throw new Sql2oException("Database error occurred while running executeScalar", e);
        }
        finally{
            closeConnectionIfNecessary();
        }
        
    }
    
    public <V> V executeScalar(Class returnType){
        Object value = executeScalar();
        Converter converter = null;
        try {
            converter = Convert.getConverter(returnType);
            return (V)converter.convert(value);
        } catch (ConverterException e) {
            throw new Sql2oException("Error occured while converting value from database to type " + returnType.toString(), e);
        }

    }

    public <T> List<T> executeScalarList(){
        long start = System.currentTimeMillis();
        List<T> list = new ArrayList<T>();
        try{
            ResultSet rs = this.statement.executeQuery();
            while(rs.next()){
                list.add((T)rs.getObject(1));
            }

            long end = System.currentTimeMillis();
            System.out.println(String.format("total: %d ms; executed scalar list [%s] ", end - start, this.getName() == null ? "No name" : this.getName()));

            return list;
        }
        catch(SQLException ex){
            this.connection.rollback();
            throw new Sql2oException("Error occurred while executing scalar list", ex);
        }
        finally{
            closeConnectionIfNecessary();
        }
    }

    /************** batch stuff *******************/

    public Query addToBatch(){
        try {
            statement.addBatch();
        } catch (SQLException e) {
            throw new Sql2oException("Error while adding statement to batch", e);
        }

        return this;
    }

    public Connection executeBatch() throws Sql2oException {
        long start = System.currentTimeMillis();
        try {
            statement.executeBatch();
        }
        catch (Throwable e) {
            this.connection.rollback();
            throw new Sql2oException("Error while executing batch operation", e);
        }
        finally {
            closeConnectionIfNecessary();
        }

        long end = System.currentTimeMillis();
        System.out.println(String.format("total: %d ms; executed batch [%s]", end - start, this.getName() == null ? "No name" : this.getName()));

        return this.connection;
    }

    /*********** column mapping ****************/

    public Map<String, String> getColumnMappings() {
        if (this.isCaseSensitive()){
            return this.caseSensitiveColumnMappings;
        }
        else{
            return this.columnMappings;
        }
    }
    
    void setColumnMappings(Map<String, String> mappings){

        this.caseSensitiveColumnMappings = new HashMap<String, String>();
        this.columnMappings = new HashMap<String, String>();
        
        for (Map.Entry<String,String> entry : mappings.entrySet()){
            this.caseSensitiveColumnMappings.put(entry.getKey(), entry.getValue());
            this.columnMappings.put(entry.getKey().toLowerCase(), entry.getValue().toLowerCase());
        }
    }

    public Query addColumnMapping(String columnName, String propertyName){
        this.caseSensitiveColumnMappings.put(columnName, propertyName);
        this.columnMappings.put(columnName.toLowerCase(), propertyName.toLowerCase());

        return this;
    }

    /************** private stuff ***************/
    private void closeConnectionIfNecessary(){
        try{
            if (!this.connection.getJdbcConnection().isClosed() && this.connection.getJdbcConnection().getAutoCommit() && statement != null){
                statement.close();
                this.connection.getJdbcConnection().close();
            }
        }
        catch (Exception ex){
            throw new RuntimeException("Error while attempting to close connection", ex);
        }
    }

//    private void onErrorCleanup(){
//        try {
//            if (this.connection.getJdbcConnection().isClosed()){
//                this.connection.getJdbcConnection().close();
//            }
//        } catch (SQLException ex) {
//            throw new RuntimeException("Error while attempting to close connection", ex);
//        }
//    }

}
