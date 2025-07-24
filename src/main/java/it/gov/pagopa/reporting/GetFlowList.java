package it.gov.pagopa.reporting;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import it.gov.pagopa.reporting.model.Fdr3Response;
import it.gov.pagopa.reporting.model.Flow;
import it.gov.pagopa.reporting.service.FlowsService;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Azure Functions with Azure Http trigger.
 */
public class GetFlowList {

    private String storageConnectionString = System.getenv("FLOW_SA_CONNECTION_STRING");

    private String flowsTable = System.getenv("FLOWS_TABLE");

    private String containerBlob = System.getenv("FLOWS_XML_BLOB");

    /**
     * This function will be invoked by an incoming HTTP request
     * @return
     */
    @FunctionName("GetFlowList")
    public HttpResponseMessage run (
            @HttpTrigger(name = "GetFlowListTrigger",
                    methods = {HttpMethod.GET},
                    route = "organizations/{organizationId}/reportings",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName("organizationId") String organizationId,
            final ExecutionContext context) {

        Logger logger = context.getLogger();

        logger.log(Level.INFO, () -> "[GetFlowList] RetrieveFlows function executed at: " + LocalDateTime.now());

        FlowsService flowsService = getFlowsServiceInstance(logger);

        try {
            String flowDate = request.getQueryParameters().getOrDefault("flowDate", null);
            Fdr3Response fdr3Response = flowsService.fetchFdr3List(organizationId, flowDate);
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            List<Flow> responseList = fdr3Response.getData().stream()
                .map(d -> {
                    OffsetDateTime offsetFlowDate = OffsetDateTime.parse(d.getFlowDate());
                    String formattedDate = offsetFlowDate.format(outputFormatter);
                    return new Flow(d.getFdr(), formattedDate);
                })
            .sorted(Comparator.comparing(Flow::getFlowDate).reversed())
            .toList();

            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(responseList)
                    .build();

        } catch (Exception e) {
            logger.log(Level.SEVERE, () -> "[GetFlowList] GetFlowList error: " + e.getLocalizedMessage());

            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/json")
                    .build();
        }
    }

    protected FlowsService getFlowsServiceInstance(Logger logger) {
        return new FlowsService(this.storageConnectionString, this.flowsTable, this.containerBlob, logger);
    }

}
