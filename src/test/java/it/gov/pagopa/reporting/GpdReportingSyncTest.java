package it.gov.pagopa.reporting;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpStatus;
import it.gov.pagopa.reporting.model.ReportedIUVEventModel;
import it.gov.pagopa.reporting.service.FlowsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GpdReportingSyncTest {

    @Spy
    GpdReportingSync function;

    @Mock
    ExecutionContext context;



    @Test
    void noMessage() {
        Logger logger = Logger.getLogger("testlogging");
        when(context.getLogger()).thenReturn(logger);

        List<ReportedIUVEventModel> messages = new ArrayList<>();

        function.run(messages, null, context);

        Mockito.verify(function, never()).gpdReport(any(), any(), any(), any());
    }

    @Test
    void oneMessage() {
        Logger logger = Logger.getLogger("testlogging");
        when(context.getLogger()).thenReturn(logger);

        List<ReportedIUVEventModel> messages = new ArrayList<>();
        ReportedIUVEventModel msg = new ReportedIUVEventModel();
        messages.add(msg);

        function.run(messages, null, context);

        Mockito.verify(function, times(1)).gpdReport(any(), any(), any(), any());
    }
}