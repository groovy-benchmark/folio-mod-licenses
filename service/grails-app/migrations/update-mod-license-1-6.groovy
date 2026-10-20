databaseChangeLog = {
  changeSet(author: "Steve Osguthorpe", id: "2019-05-08-ERM-6") {
    addColumn(tableName: "custom_property") {
      column(name: "note", type: "TEXT")
    }
  }
}
