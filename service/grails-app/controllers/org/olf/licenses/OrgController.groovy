package org.olf.licenses

import org.olf.DependentModuleProxyService
import org.olf.general.Org
import org.springframework.http.HttpStatus

import groovyx.net.http.HttpException
import grails.validation.ValidationException

import com.k_int.okapi.OkapiTenantAwareController

import grails.gorm.multitenancy.CurrentTenant
import groovy.util.logging.Slf4j

@Slf4j
@CurrentTenant
class OrgController extends OkapiTenantAwareController<Org>  {

  DependentModuleProxyService dependentModuleProxyService
  
  OrgController() {
    super(Org)
  }
  
  public find(String id) {
    Org org
    try {
      org = dependentModuleProxyService.coordinateOrg(id)
    } catch (HttpException e) {
      final String ref = UUID.randomUUID().toString()
      log.error("Vendor lookup failed ref=${ref}", e)
      render status: HttpStatus.BAD_GATEWAY.value(), text: "Vendor lookup unavailable (ref ${ref})" // SAFE_SINK: PLANTED-GR-HR-174-safe
      return
    } catch (Exception e) {
      log.error("Org lookup failed for ${id}", e)
      final def detail = (e instanceof ValidationException) ? ((ValidationException) e).errors?.allErrors*.toString() : e.message
      respond([error: 'Org lookup failed', detail: detail], [status: HttpStatus.INTERNAL_SERVER_ERROR]) // SINK: PLANTED-GR-HR-174
      return
    }

    respond org
  }
}

