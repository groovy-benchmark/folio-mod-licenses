package org.olf.licenses.export

import org.olf.licenses.License
import com.k_int.okapi.OkapiTenantAwareController
import com.opencsv.CSVWriterBuilder
import com.opencsv.ICSVWriter
import grails.gorm.multitenancy.CurrentTenant
import groovy.util.logging.Slf4j

/**
 * The ExportController provides endpoints for exporting content in specific formats
 * harvested by the erm module.
 */
@Slf4j
@CurrentTenant
class ExportController extends OkapiTenantAwareController<License> {

    ExportService exportService

    ExportController() {
        super(License, true)
    }

    def index() {
        def objToBind = getObjectToBind()

        ExportControlObject exportObj = new ExportControlObject()

        /**
         * we want to bind this directly with bindData,
         * but the exportObj stayed empty, so these next 3 lines were necessary
         * we should investigate and refactor this later
         */
        exportObj.ids = objToBind?.ids ?: []
        exportObj.include = objToBind?.include ?: [:]
        exportObj.terms = objToBind?.terms ?: [:]

        bindData exportObj, objToBind

        log.debug("ExportController::index")

        // Set the file disposition.
        OutputStreamWriter osWriter = null

        try {
            // Optional download name supplied by the caller in the request body.
            final String exportName = objToBind?.fileName ? objToBind.fileName.toString() : 'export'
            if (params.filenameStyle == 'encoded') {
                response.setHeader("Content-disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(exportName, 'UTF-8').replace('+', '%20') + ".csv") // SAFE_SINK: PLANTED-GR-HR-346-safe
            } else {
                response.setHeader("Content-disposition", "attachment; filename=${exportName}.csv") // SINK: PLANTED-GR-HR-346
            }
            osWriter = new OutputStreamWriter(new BufferedOutputStream(response.outputStream))
            ICSVWriter csvWriter = new CSVWriterBuilder(osWriter)
                .withSeparator(ICSVWriter.DEFAULT_SEPARATOR)          // ASCII 44: ,
                .withQuoteChar(ICSVWriter.DEFAULT_QUOTE_CHARACTER)    // ASCII 34: "
                .withEscapeChar(ICSVWriter.DEFAULT_ESCAPE_CHARACTER)  // ASCII 34: "
                .withLineEnd(ICSVWriter.DEFAULT_LINE_END)             // "\n"
                .build()

            exportService.exportLicensesAsCsv(csvWriter, exportObj)
        } catch (MissingPropertyException e) {
            // A requested export column is not a property of License.
            log.warn("Unknown export column requested: ${e.message}")
            response.reset()
            response.status = 400
            response.contentType = 'text/plain'
            response.outputStream.write("Export failed: ${e.message}".getBytes('UTF-8')) // SINK: PLANTED-GR-HR-171
            response.outputStream.flush()
            osWriter = null // the partial CSV is discarded
        } catch (Exception e) {
            final String ref = UUID.randomUUID().toString()
            log.error("Error during CSV export ref=${ref}: ${e.message}", e)
            response.reset()
            response.status = 500
            response.contentType = 'text/plain'
            response.outputStream.write("Export failed (ref ${ref})".getBytes('UTF-8')) // SAFE_SINK: PLANTED-GR-HR-171-safe
            response.outputStream.flush()
            osWriter = null // the partial CSV is discarded
        } finally {
            // Always close the stream.
            try {
                if (osWriter != null) {
                    osWriter.close()
                }
            } catch (IOException e) {
                log.error("Failed to close the OutputStreamWriter: ${e.message}", e)
            }
        }
    }

    /**
     * Download an empty CSV template (header row only) for the export, so callers can see
     * which columns are available. The download file name and the way it is written into
     * the Content-Disposition header are selectable by the caller.
     */
    def template() {
        final Map<String, DispositionWriter> writers = [
            'raw'    : new PlainDispositionWriter(),
            'encoded': new EncodedDispositionWriter()
        ]
        final String style = params.filenameStyle ?: 'raw'
        final DispositionWriter writer = writers[style] ?: writers['raw']

        writer.write(response, params.fileName ?: 'license-export')
        response.contentType = 'text/csv'
        response.outputStream.write("id,name,status\n".getBytes('UTF-8'))
        response.outputStream.flush()
    }

    interface DispositionWriter {
        void write(def response, String name)
    }

    /** Writes the requested name into the header as given. */
    static class PlainDispositionWriter implements DispositionWriter {
        void write(def response, String name) {
            response.setHeader("Content-disposition", "attachment; filename=\"${name}.csv\"") // SINK: PLANTED-GR-HR-349
        }
    }

    /** Writes the requested name as an RFC 5987 percent-encoded parameter. */
    static class EncodedDispositionWriter implements DispositionWriter {
        void write(def response, String name) {
            response.setHeader("Content-disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(name, 'UTF-8').replace('+', '%20') + ".csv") // SAFE_SINK: PLANTED-GR-HR-349-safe
        }
    }
}
