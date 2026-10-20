package org.olf.licenses

import grails.validation.Validateable
import org.springframework.web.multipart.MultipartFile

class LicenseFileCommand implements Validateable {
  MultipartFile licenseFile

  static constraints = {
    licenseFile validator : { val, obj ->
      if ( val == null ) {
        return false
      }

      if ( val.empty ) {
        return false
      }

      ['pdf'].any { extension ->
        val.originalFilename?.toLowerCase()?.endsWith(extension)
      }
    }
  }

}

