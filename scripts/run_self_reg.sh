#!/bin/bash

echo Start mod-licenses in self-register mode

java -jar build/libs/mod-licenses-1.0.jar -Xmx1G \
--grails.server.host=10.0.2.2 \
--selfRegister=on \
--dataSource.username=folio_admin \
--dataSource.password=folio_admin \
--dataSource.url=jdbc:postgresql://localhost:5432/okapi_modules

