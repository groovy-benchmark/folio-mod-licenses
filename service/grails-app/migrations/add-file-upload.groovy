databaseChangeLog = {

  changeSet(author: "sosguthorpe (generated)", id: "1560252507752-1") {
    createTable(tableName: "file_upload") {
      column(name: "fu_id", type: "VARCHAR(36)") {
        constraints(nullable: "false")
      }

      column(name: "version", type: "BIGINT") {
        constraints(nullable: "false")
      }

      column(name: "fu_filesize", type: "BIGINT") {
        constraints(nullable: "false")
      }

      column(name: "fu_last_mod", type: "TIMESTAMP WITHOUT TIME ZONE")

      column(name: "file_content_type", type: "VARCHAR(255)")

      column(name: "fu_owner", type: "VARCHAR(36)")

      column(name: "fu_filename", type: "VARCHAR(255)") {
        constraints(nullable: "false")
      }

      column(name: "fu_bytes", type: "BYTEA")
    }
  }

  changeSet(author: "sosguthorpe (generated)", id: "1560252507752-2") {
    createTable(tableName: "single_file_attachment") {
      column(name: "id", type: "VARCHAR(36)") {
        constraints(nullable: "false")
      }

      column(name: "version", type: "BIGINT") {
        constraints(nullable: "false")
      }
    }
  }

  changeSet(author: "sosguthorpe (generated)", id: "1560252507752-3") {
    addPrimaryKey(columnNames: "fu_id", constraintName: "file_uploadPK", tableName: "file_upload")
  }

  changeSet(author: "sosguthorpe (generated)", id: "1560252507752-4") {
    addPrimaryKey(columnNames: "id", constraintName: "single_file_attachmentPK", tableName: "single_file_attachment")
  }

  changeSet(author: "sosguthorpe (generated)", id: "1560252507752-5") {
    addForeignKeyConstraint(baseColumnNames: "fu_owner", baseTableName: "file_upload", constraintName: "FK2kjo91mrq9mt35oo8a94c0p5o", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "single_file_attachment")
  }

  changeSet(author: "sosguthorpe (generated)", id: "1560252507752-6") {
    dropColumn(columnName: "da_version", tableName: "document_attachment")
  }
  
}