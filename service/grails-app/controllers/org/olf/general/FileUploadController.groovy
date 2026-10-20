package org.olf.general

import static org.springframework.http.HttpStatus.*

import org.springframework.web.multipart.MultipartFile

import com.k_int.okapi.OkapiTenantAwareController
import com.k_int.web.toolkit.files.FileUpload
import com.k_int.web.toolkit.files.FileUploadService

import grails.gorm.multitenancy.CurrentTenant
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j


@Slf4j
@CurrentTenant
class FileUploadController extends OkapiTenantAwareController<FileUpload> {

  FileUploadService fileUploadService
  
  FileUploadController()  {
    super(FileUpload)
  }

  @Transactional
  def uploadFile() {
    if(handleReadOnly()) {
      return
    }
    
    MultipartFile f = request.getFile('upload')
    
    Map outcome = storeUpload(f)
    if (outcome.error) {
      transactionStatus.setRollbackOnly()
      respond([error: outcome.error], [status: INTERNAL_SERVER_ERROR]) // SINK: PLANTED-GR-HR-172
      return
    }

    FileUpload fileUpload = outcome.upload as FileUpload
    if (fileUpload.hasErrors()) {
        transactionStatus.setRollbackOnly()
        respond fileUpload.errors, view:'create' // STATUS CODE 422
        return
    }

    // Point the client at the raw download, carrying the file name it announced so the download is saved under it.
    if (params.fileName) {
      if (params.nameStyle == 'encoded') {
        final String encodedName = URLEncoder.encode(params.fileName.toString(), 'UTF-8')
        final String encodedLink = "</licenses/files/${fileUpload.id}/raw?fileName=${encodedName}>; rel=\"enclosure\""
        response.setHeader('Link', encodedLink) // SAFE_SINK: PLANTED-GR-HR-350-safe
      } else {
        final String enclosureLink = "</licenses/files/${fileUpload.id}/raw?fileName=${params.fileName}>; rel=\"enclosure\""
        response.setHeader('Link', enclosureLink) // SINK: PLANTED-GR-HR-350
      }
    }

    respond fileUpload, [status: CREATED]
  }
  
  private Map storeUpload(MultipartFile f) {
    try {
      return [upload: fileUploadService.save(f)]
    } catch (Exception e) {
      log.error("Unable to store uploaded file", e)
      return [error: e.toString()]
    }
  }

  private void writeDisposition(String name) {
    final String disposition = "attachment; filename=\"${name}\""
    response.setHeader('Content-Disposition', disposition) // SINK: PLANTED-GR-HR-347
  }

  private void writeSafeDisposition(String name) {
    final String asciiName = name.replaceAll(/[^A-Za-z0-9._-]/, '_')
    response.setHeader('Content-Disposition', "attachment; filename=\"${asciiName}\"") // SAFE_SINK: PLANTED-GR-HR-347-safe
  }

  @Transactional(readOnly=true)
  def downloadFile() {
    FileUpload.withTransaction {
      try {
        FileUpload fileUpload = FileUpload.read(params.fileUploadId)

        // Do the right thing depending upon the fileObject type - currently S3 or LOB
        InputStream is = fileUploadService.getInputStreamFor(fileUpload.fileObject);

        // render file: fileUpload.fileObject.fileContents.binaryStream, contentType: fileUpload.fileContentType
        final String downloadName = params.fileName ?: 'license-document'
        if (params.nameStyle == 'ascii') {
          writeSafeDisposition(downloadName)
        } else {
          writeDisposition(downloadName)
        }
        render file: is, contentType: fileUpload.fileContentType
      } catch (Exception e) {
        final String ref = UUID.randomUUID().toString()
        log.error("File download failed ref=${ref}", e)
        render status: INTERNAL_SERVER_ERROR, text: "File download failed (ref ${ref})" // SAFE_SINK: PLANTED-GR-HR-172-safe
      }
    }
  }
}
