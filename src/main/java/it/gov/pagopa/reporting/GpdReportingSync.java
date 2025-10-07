package it.gov.pagopa.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.Cardinality;
import com.microsoft.azure.functions.annotation.EventHubTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import it.gov.pagopa.reporting.model.ReportedIUVEventModel;

import java.time.LocalDateTime;
import java.util.List;
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
     *
     * @return
     */
    @FunctionName("GpdReportingSync")
    public void run(
            @EventHubTrigger(
                    name = "GpdReportingSyncTrigger",
                    eventHubName = "", // blank because the value is included in the connection string
                    consumerGroup = "gpd-reporting-sync",
                    connection = "FDR_EVENTHUB_CONN_STRING",
                    cardinality = Cardinality.MANY)
            List<String> items,
            final ExecutionContext context) {

        Logger logger = context.getLogger();
        logger.log(Level.FINE, () -> "[GpdReportingSync] function executed at: " + LocalDateTime.now());

        ObjectMapper objectMapper = new ObjectMapper();


        for (String event : items) {
            ReportedIUVEventModel reportedIUVEventModel;
            try {
                reportedIUVEventModel = objectMapper.readValue(event, ReportedIUVEventModel.class);
            } catch (Exception e) {
                logger.log(Level.WARNING, () -> "[GpdReportingSync] The message was ignored because it wasn't parsed correctly. msg=[" + event + "]");
                continue;
            }

            // In FDR-1, transferId is not mandatory if there is only one transfer;
            // If ID_TRANSFER is null override with 1.
            if (reportedIUVEventModel.getIdTransfer() == null) {
                reportedIUVEventModel.setIdTransfer(1L);
            }

            gpdReport(logger, reportedIUVEventModel.getDomainId(), reportedIUVEventModel.getIuv(), String.valueOf(reportedIUVEventModel.getIdTransfer()));
        }

    }

    public void gpdReport(Logger logger, String organizationId, String iuv, String transferId) {
        try {
            HttpResponse<String> response = Unirest.post(gpdBasePath + "/organizations/" + organizationId + "/paymentoptions/" + iuv + "/transfers/" + transferId + "/report")
                    .header("accept", "application/json")
                    .header("ocp-apim-subscription-key", gpdSubeKey)
                    .asString();
            if (response.getStatus() != 200) {
                logger.log(Level.SEVERE, () -> "[GpdReportingSync] GPD client failed with status " + response.getStatus() + " body:" + response.getBody());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, () -> "[GpdReportingSync] GPD client Exception: " + e.getLocalizedMessage());
        }
    }


}
