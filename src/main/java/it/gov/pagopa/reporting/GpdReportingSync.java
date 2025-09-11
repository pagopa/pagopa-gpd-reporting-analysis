package it.gov.pagopa.reporting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
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
     * @return
     */
    @FunctionName("GpdReportingSync")
    public void run (
            @EventHubTrigger(
                    name = "GpdReportingSyncTrigger",
                    eventHubName = "", // blank because the value is included in the connection string
                    consumerGroup = "gpd-reporting-test",
                    connection = "FDR_EVENTHUB_CONN_STRING",
                    cardinality = Cardinality.MANY)
            List<String> items,
            final ExecutionContext context) throws JsonProcessingException {

        Logger logger = context.getLogger();
        logger.log(Level.FINE, () -> "[GpdReportingSync] function executed at: " + LocalDateTime.now());

        ObjectMapper objectMapper = new ObjectMapper();


        for (String event: items) {
            ReportedIUVEventModel reportedIUVEventModel = objectMapper.readValue(event, ReportedIUVEventModel.class);
            gpdReport(logger, reportedIUVEventModel.getDomainId(), reportedIUVEventModel.getIuv(), String.valueOf(reportedIUVEventModel.getIdTransfer()));
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
