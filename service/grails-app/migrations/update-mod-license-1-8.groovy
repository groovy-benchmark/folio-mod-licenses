final Map<String, List<String>> table_cols = [
  'license': ['lic_start_date', 'lic_end_date']
]
databaseChangeLog = {
  changeSet(author: "sosguthorpe", id: "1563877044-1") {
    
    table_cols.each { String table, List<String> cols ->
      // Add backup columns for all the originals.
      
      addColumn(tableName: table) {
        cols.each { String col ->
          column (name: "${col}_old", type: "TIMESTAMP")
        }
      }
      
      // Copy the data
      cols.each { String col ->
        grailsChange {
          change {
            // Set the dates
            sql.execute("UPDATE ${database.defaultSchemaName}.${table} SET ${col}_old = ${col};".toString())
          }
        }
        
        // Change the column type
        modifyDataType (tableName: table, columnName: "${col}", newDataType: "DATE")
        
        // Success drop the column
      }
    }
  }
  
  // Second changeset so as to only drop when the preceding one has succeeded
  changeSet(author: "sosguthorpe", id: "1563877044-2") {
    table_cols.each { String table, List<String> cols ->
      cols.each { String col ->
        // Tidy up by dropping all the backup columns.
        dropColumn(tableName: table, columnName: "${col}_old")
      }
    }
  }
}