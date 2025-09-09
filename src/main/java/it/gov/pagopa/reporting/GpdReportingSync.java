package it.gov.pagopa.reporting;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import it.gov.pagopa.reporting.model.ReportedIUVEventModel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Azure Functions with Azure Http trigger.
 */
public class GpdReportingSync {

    private String gpdBasePath = System.getenv("GPD_BASE_PATH");
    private String gpdSubeKey = System.getenv("GPD_SUBKEY");


    /**
     * This function will be invoked by an incoming HTTP request
     * @return
     */
    @FunctionName("GpdReportingSync")
    public void run (
            @EventHubTrigger(
                    name = "BizEvent",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "FDR_EVENTHUB_CONN_STRING",
                    cardinality = Cardinality.MANY)
            List<ReportedIUVEventModel> items,
            @BindingName(value = "PropertiesArray") Map<String, Object>[] properties,
            final ExecutionContext context) {

        Logger logger = context.getLogger();
        logger.log(Level.FINE, () -> "[GpdReportingSync] function executed at: " + LocalDateTime.now());

        for (ReportedIUVEventModel event: items) {
            gpdReport(logger, event.getDomainId(), event.getIuv(), String.valueOf(event.getIdTransfer()));
        }

    }

    public void gpdReport(Logger logger, String organizationId, String iuv, String transferId) {
        try {
            HttpResponse<String> response = Unirest.post(gpdBasePath + "/organizations/" + organizationId + "/paymentoptions/" + iuv + "/transfers/" + transferId + "/report")
                    .header("accept", "application/json")
                    .header("ocp-apim-subscription-key", gpdSubeKey)
                    .asString();
            logger.log(Level.INFO, () -> response.getStatus() +" "+ response.getBody()); // TODO delete this line
        } catch (Exception e) {
            logger.log(Level.SEVERE, () -> "[GpdReportingSync] GPD client error: " + e.getLocalizedMessage());
        }
    }


}
