package org.olf.licenses

import grails.gorm.multitenancy.CurrentTenant
import groovy.util.logging.Slf4j

import org.olf.licenses.LicenseLink

import com.k_int.okapi.OkapiTenantAwareController

import grails.converters.JSON


/**
 * Control access to subscription agreements.
 * A subscription agreement (SA) is the connection between a set of resources (Which could be packages or individual titles) and a license. 
 * SAs have start dates, end dates and renewal dates. This controller exposes functions for interacting with the list of SAs
 */
@CurrentTenant
class LicenseLinkController extends OkapiTenantAwareController<LicenseLink>  {

  LicenseLinkController() {
    super(LicenseLink)
  }
}

