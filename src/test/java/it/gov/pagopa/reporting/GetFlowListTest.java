package it.gov.pagopa.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;

import it.gov.pagopa.reporting.model.Fdr3Metadata;
import it.gov.pagopa.reporting.model.Fdr3Response;
import it.gov.pagopa.reporting.service.FlowsService;

@ExtendWith(MockitoExtension.class)
class GetFlowListTest {

    @Spy
    GetFlowList function;

    @Mock
    ExecutionContext context;

    @Mock
    FlowsService flowsService;

    @Test
    void runOK_withFlowDate() throws Exception {

        // general var
        Logger logger = Logger.getLogger("testlogging");
        Fdr3Response fdr3Resp = Fdr3Response.builder()
            .metadata(Fdr3Metadata.builder().pageNumber(0).pageSize(0).totPage(0).build())
            .count(0)
            .data(new ArrayList<>())
            .build();
        String organizationId =  "90000000000";

        // precondition
        when(context.getLogger()).thenReturn(logger);
        doReturn(flowsService).when(function).getFlowsServiceInstance(logger);
        doReturn(fdr3Resp).when(flowsService).fetchFdr3List(organizationId, "2022-01-01");

        final HttpResponseMessage.Builder builder = mock(HttpResponseMessage.Builder.class);
        HttpRequestMessage<Optional<String>> request = mock(HttpRequestMessage.class);

        doReturn(builder).when(request).createResponseBuilder(any(HttpStatus.class));
        doReturn(builder).when(builder).header(anyString(), anyString());
        doReturn(builder).when(builder).body(any());  

        HttpResponseMessage responseMock = mock(HttpResponseMessage.class);
        Map<String,String> qp = new HashMap<>();
        qp.put("flowDate", "2022-01-01");
        when(request.getQueryParameters()).thenReturn(qp);
        doReturn(HttpStatus.OK).when(responseMock).getStatus();
        doReturn(responseMock).when(builder).build();

        // test
        HttpResponseMessage response = function.run(request, organizationId, context);

        // Asserts
        assertEquals(HttpStatus.OK, response.getStatus());
    }

    @Test
    void runOK_noFlowDate() throws Exception {

        // general var
        Logger logger = Logger.getLogger("testlogging");
        Fdr3Response fdr3Resp = Fdr3Response.builder()
            .metadata(Fdr3Metadata.builder().pageNumber(0).pageSize(0).totPage(0).build())
            .count(0)
            .data(new ArrayList<>())
            .build();
        String organizationId =  "90000000000";

        // precondition
        when(context.getLogger()).thenReturn(logger);
        doReturn(flowsService).when(function).getFlowsServiceInstance(logger);
        doReturn(fdr3Resp).when(flowsService).fetchFdr3List(organizationId, null);

        final HttpResponseMessage.Builder builder = mock(HttpResponseMessage.Builder.class);
        HttpRequestMessage<Optional<String>> request = mock(HttpRequestMessage.class);

        doReturn(builder).when(request).createResponseBuilder(any(HttpStatus.class));
        doReturn(builder).when(builder).header(anyString(), anyString());
        doReturn(builder).when(builder).body(any());

        HttpResponseMessage responseMock = mock(HttpResponseMessage.class);
        doReturn(HttpStatus.OK).when(responseMock).getStatus();
        doReturn(responseMock).when(builder).build();

        // test
        HttpResponseMessage response = function.run(request, organizationId, context);

        // Asserts
        assertEquals(HttpStatus.OK, response.getStatus());
    }


    @Test
    void runKO() throws Exception {

        // general var
        Logger logger = Logger.getLogger("testlogging");
        String organizationId =  "90000000000";

        // precondition
        when(context.getLogger()).thenReturn(logger);
        doReturn(flowsService).when(function).getFlowsServiceInstance(logger);
        doThrow(InvalidKeyException.class).when(flowsService).fetchFdr3List(organizationId, "2022-01-01");

        final HttpResponseMessage.Builder builder = mock(HttpResponseMessage.Builder.class);
        HttpRequestMessage<Optional<String>> request = mock(HttpRequestMessage.class);

        doReturn(builder).when(request).createResponseBuilder(any(HttpStatus.class));
        doReturn(builder).when(builder).header(anyString(), anyString());
        doReturn(builder).when(builder).body(any());

        HttpResponseMessage responseMock = mock(HttpResponseMessage.class);
        doReturn(HttpStatus.BAD_REQUEST).when(responseMock).getStatus();
        doReturn(responseMock).when(builder).build();

        // test
        HttpResponseMessage response = function.run(request, organizationId, context);

        // Asserts
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
    }

}
