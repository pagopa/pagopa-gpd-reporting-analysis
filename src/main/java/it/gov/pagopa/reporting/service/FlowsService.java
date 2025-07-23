package it.gov.pagopa.reporting.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.table.CloudTable;
import com.microsoft.azure.storage.table.TableQuery;
import it.gov.pagopa.reporting.entity.FlowEntity;
import it.gov.pagopa.reporting.model.Fdr3Data;
import it.gov.pagopa.reporting.model.Fdr3Response;
import it.gov.pagopa.reporting.model.Flow;
import it.gov.pagopa.reporting.util.AzuriteStorageUtil;
import it.gov.pagopa.reporting.util.FlowConverter;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class FlowsService {

    private String storageConnectionString;
    private String flowsTable;
    private String containerBlob;
    private Logger logger;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String Fdr3ApiKey = System.getenv("FDR3_APIM_SUBSCRIPTION_KEY");
    private final String Fdr1ApiKey = System.getenv("FDR1_APIM_SUBSCRIPTION_KEY");
    private final String fdr3BaseUrl = System.getenv("FDR3_BASE_URL");
    private final String fdr1BaseUrl = System.getenv("FDR1_BASE_URL");

    private final int flowListDepth = Integer.parseInt(System.getenv("FDR3_FLOW_LIST_DEPTH"));
    private final String listEl4Page = System.getenv("FDR3_LIST_ELEMENTS_FOR_PAGE");

    public FlowsService(String storageConnectionString, String flowsTable, String containerBlob, Logger logger) {

        this.storageConnectionString = storageConnectionString;
        this.flowsTable = flowsTable;
        this.containerBlob = containerBlob;
        this.logger = logger;
    }

    public List<Flow> getByOrganization(String organizationId, String flowDate) throws URISyntaxException, InvalidKeyException, StorageException, RuntimeException {
        logger.log(Level.INFO, () -> String.format("[FlowsService] START get by organization: %s", organizationId));

        // try to create table
        AzuriteStorageUtil azuriteStorageUtil = new AzuriteStorageUtil(storageConnectionString, flowsTable, null);
        azuriteStorageUtil.createTable();

        CloudTable table = CloudStorageAccount.parse(storageConnectionString).createCloudTableClient()
                .getTableReference(this.flowsTable);

        String queryWhereClause = TableQuery.generateFilterCondition("PartitionKey", TableQuery.QueryComparisons.EQUAL, organizationId);
        /*
        * The saved flow date field is a string data, so it cannot be filtered as a numeric or temporal data. Also, in Azure Table storage it does not
        * exists the LIKE clause, so a workaround is made using ASCII character evaluation in 'ge' and 'le' operators. In particular, adding 'T0' string permits
        * to evaluate all dates that are greater or equals than HH=00 of passed date, and adding T3 string permits to evaluate all dates that are lower or equals than
        * HH=23 (because character '2' is lower than character '3' and over hour '23' is not a valid date)
        */
        if (flowDate != null) {
            String flowDateLowerLimitClause = TableQuery.generateFilterCondition("FlowDate", TableQuery.QueryComparisons.GREATER_THAN_OR_EQUAL, flowDate + "T0");
            String flowDateUpperLimitClause = TableQuery.generateFilterCondition("FlowDate", TableQuery.QueryComparisons.LESS_THAN_OR_EQUAL, flowDate + "T3");
            String flowDateIntervalLimitClause = TableQuery.combineFilters(flowDateLowerLimitClause, "and", flowDateUpperLimitClause);
            queryWhereClause = TableQuery.combineFilters(queryWhereClause, "and", flowDateIntervalLimitClause);
        }

        TableQuery<FlowEntity> query = TableQuery.from(FlowEntity.class).where(queryWhereClause);

        Iterable<FlowEntity> result = table.execute(query);

        List<FlowEntity> flowList = new ArrayList<>();
        result.forEach(flowList::add);

        Converter<FlowEntity, Flow> converter = new FlowConverter();
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.createTypeMap(FlowEntity.class, Flow.class).setConverter(converter);

        return flowList.stream().map(flow -> modelMapper.map(flow, Flow.class)).collect(Collectors.toList());
    }

    public String getByFlow(String organizationId, String flowId, String flowDate) throws Exception {
        logger.log(Level.INFO, () -> String.format("[FlowsService] START get by flow: %s - %s - %s", organizationId, flowId, flowDate));

        // try to create blob container
        AzuriteStorageUtil azuriteStorageUtil = new AzuriteStorageUtil(storageConnectionString, null, containerBlob);
        azuriteStorageUtil.createTable();

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(this.storageConnectionString).buildClient();

        BlobContainerClient flowsContainerClient = blobServiceClient.getBlobContainerClient(this.containerBlob);

        // dataOra##idPa##idflow.xml
        BlobClient blobClient = flowsContainerClient.getBlobClient(
                flowDate + "##" + organizationId + "##" + flowId + ".xml"
        );

        //creating an object of output stream to receive the file's content from azure blob.
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blobClient.download(outputStream);

        return outputStream.toString();
    }

    /**
     * Retrieves the list of reporting flows by invoking the APIs exposed by FDR3.
     * <p>
     * If a flowDate is provided, results are filtered to include only entries whose date-time 
     * is inside the day specified in flowDate. To achieve this, the method constructs an upper bound
     * and lower bound for the date-time based on flowDate, the FDR3 API accepts only a lower bound,
     * for that reason the method filters the results by the upper bound after the API call.
     *
     * @param organizationId the unique identifier of the organization to retrieve flows for
     * @param flowDate       optional ISO-8601 date (yyyy-MM-dd) used to filter flows; if null,
     *                       retrieves flows up to a depth defined by configuration
     * @return a {@link Fdr3Response} with the list of matching flows
     * @throws Exception if an HTTP or parsing error occurs while calling the FDR3 endpoint
     */
    public Fdr3Response fetchFdr3List(String organizationId, String flowDate) throws Exception {

        logger.log(Level.INFO, () -> String.format("[FlowsService][fetchFdr3List] START get flow list from FDR3: %s", organizationId));

        // build upper and lower boud in case specific flowDate has been specified
        final OffsetDateTime filterUpperBound = (flowDate != null)
                ? OffsetDateTime.parse(flowDate + "T23:59:59Z")
                : null;

        final String fdrFlowDate = (flowDate != null)
                ? flowDate + "T00:00:00Z"
                : OffsetDateTime.now(ZoneOffset.UTC)
                    .minusMonths(flowListDepth)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        String url = String.format(
            "%s/organizations/%s/fdrs?page=1&size=%s&flowDate=%s",
            fdr3BaseUrl,
            organizationId,
            listEl4Page,
            fdrFlowDate
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Ocp-Apim-Subscription-Key", Fdr3ApiKey)
                .GET()
                .build();

        logger.log(Level.INFO, () -> String.format("[FlowsService][fetchFdr3List] calling FDR3, url: %s", url));
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Error while calling FDR3 to retrieve flow list: " + response.body());
        }

        Fdr3Response fullResponse = objectMapper.readValue(response.body(), Fdr3Response.class);

        if (filterUpperBound != null) {
            
            logger.log(Level.INFO, () -> String.format("[FlowsService][fetchFdr3List] filtering elements, lowerbound [%s] upperbound [%s]", 
                flowDate, filterUpperBound.toString()));

            List<Fdr3Data> filteredData = fullResponse.getData().stream()
                    .filter(d -> {
                        OffsetDateTime publishedDate = OffsetDateTime.parse(d.getPublished());
                        return publishedDate.isBefore(filterUpperBound.plusSeconds(1)); // <= 23:59:59
                    })
                    .toList();

            return Fdr3Response.builder()
                    .metadata(fullResponse.getMetadata())
                    .count(filteredData.size())
                    .data(filteredData)
                    .build();
        }

        return fullResponse;
    }

    public String fetchFdr1Flow(String organizationId, String fdr) throws Exception {

        logger.log(Level.INFO, () -> String.format("[FlowsService][fetchFdr1Flow] START get flow from FDR3, organizationId: %s flowId: %s", organizationId, fdr));

        String url = String.format(
            "%s/internal/organizations/%s/fdrs/%s",
            fdr1BaseUrl,
            organizationId,
            fdr
        );

        logger.log(Level.INFO, () -> String.format("[FlowsService][fetchFdr1Flow] calling FDR1, url: %s", url));
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Ocp-Apim-Subscription-Key", Fdr1ApiKey + ";product=fdr_internal")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("[FlowsService][fetchFdr1Flow] Error while calling FDR1 internal to retrieve flow details: " + response.body());
        }

        // get compressed base64 xml rendicontazione
        logger.log(Level.INFO, "[FlowsService][fetchFdr1Flow] get compressed base64 xml");
        String compressedXmlBase64 = objectMapper.readTree(response.body()).get("xmlRendicontazione").asText();
        byte[] gzipBytes = Base64.getDecoder().decode(compressedXmlBase64);
        
        // decompress GZIP
        logger.log(Level.INFO, "[FlowsService][fetchFdr1Flow] decompress xml file");
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(gzipBytes));
            InputStreamReader reader = new InputStreamReader(gis, StandardCharsets.UTF_8);
            BufferedReader in = new BufferedReader(reader)) {

            StringBuilder xmlBuilder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                xmlBuilder.append(line).append("\n");
            }

            return xmlBuilder.toString().trim(); // XML already formatted
        }
    }
}
