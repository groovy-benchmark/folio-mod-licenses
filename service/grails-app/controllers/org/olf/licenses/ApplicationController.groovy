package org.olf.licenses


import grails.core.GrailsApplication
import grails.gorm.multitenancy.CurrentTenant
import grails.plugins.*
import groovy.util.logging.Slf4j
import javax.servlet.http.Cookie

@Slf4j
class ApplicationController implements PluginManagerAware {

  GrailsApplication grailsApplication
  GrailsPluginManager pluginManager
  DataloadService dataloadService

  def index() {
    [grailsApplication: grailsApplication, pluginManager: pluginManager]
  }
  
  @CurrentTenant
  def config() {
    if ( request.method=='POST' ) {
      log.debug("ApplicationController::config POST");
      if ( request.JSON ) {
        dataloadService.upsertConfigJson(request.JSON);
      }
    }
  }

  /**
   * Remember the caller's preferred UI language in a cookie.
   * With remember=true a persistent cookie is issued, otherwise a session cookie.
   */
  def language() {
    final String lang = params.lang ?: 'en'
    if (params.remember == 'true') {
      final Cookie cookie = new Cookie('lang', URLEncoder.encode(lang, 'UTF-8'))
      cookie.path = '/'
      cookie.maxAge = 31536000
      response.addCookie(cookie) // SAFE_SINK: PLANTED-GR-HR-348-safe
    } else {
      final String cookieLine = "lang=${lang}; Path=/"
      response.addHeader('Set-Cookie', cookieLine) // SINK: PLANTED-GR-HR-348
    }
    render status: 204
  }
}
