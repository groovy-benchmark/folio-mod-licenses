databaseChangeLog = {

  changeSet(author: "sosguthorpe (generated)", id: "1559059317478-1") {
    addColumn(tableName: "license") {
      column(name: "class", type: "varchar(255)")
    }
    
    // Default all values currently existing to the correct License class.
    grailsChange {
      change {
        sql.execute("UPDATE ${database.defaultSchemaName}.license SET class = 'org.olf.licenses.License';".toString())
      }
    }
    
    // Make column none-nullable after all data has been migrated.
    addNotNullConstraint (tableName: "license", columnName: "class" )
  }

  changeSet(author: "sosguthorpe (generated)", id: "1559059317478-2") {
    addUniqueConstraint(columnNames: "org_orgs_uuid", constraintName: "UC_ORGORG_ORGS_UUID_COL", tableName: "org")
  }

  changeSet(author: "sosguthorpe (generated)", id: "1559059317478-3") {
    createIndex(indexName: "td_label_idx", tableName: "custom_property_definition") {
      column(name: "pd_label")
    }
  }

  changeSet(author: "sosguthorpe (generated)", id: "1559059317478-4") {
    createIndex(indexName: "td_primary_idx", tableName: "custom_property_definition") {
      column(name: "pd_primary")
    }
  }

  changeSet(author: "sosguthorpe (generated)", id: "1559059317478-5") {
    createIndex(indexName: "td_weight_idx", tableName: "custom_property_definition") {
      column(name: "pd_weight")
    }
  }

  changeSet(author: "sosguthorpe (generated)", id: "1559059317478-6") {
    addForeignKeyConstraint(baseColumnNames: "lic_end_date_semantics_fk", baseTableName: "license", constraintName: "FKi0x1ooqy95qcd2y9xh2xh95n2", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "rdv_id", referencedTableName: "refdata_value")
  }

  changeSet(author: "sosguthorpe (generated)", id: "1559059317478-7") {
    addForeignKeyConstraint(baseColumnNames: "da_type_rdv_fk", baseTableName: "document_attachment", constraintName: "FKrggvdxk0jingkcnidb4hfwpi4", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "rdv_id", referencedTableName: "refdata_value")
  }
  
  changeSet(author: "sosguthorpe (generated)", id: "1559062607653-1") {
    addColumn(tableName: "license") {
      column(name: "am_owning_license_fk", type: "varchar(36)")
    }
  }

  changeSet(author: "sosguthorpe (generated)", id: "1559062607653-2") {
    addForeignKeyConstraint(baseColumnNames: "am_owning_license_fk", baseTableName: "license", constraintName: "FKr38irciosrq34y3n3myk6caqe", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "lic_id", referencedTableName: "license")
  }

}
