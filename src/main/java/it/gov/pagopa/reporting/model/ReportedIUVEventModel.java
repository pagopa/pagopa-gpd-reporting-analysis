package it.gov.pagopa.reporting.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportedIUVEventModel {

    @JsonProperty("IUV")
    private String iuv;

    @JsonProperty("IUR")
    private String iur;

    @JsonProperty("IMPORTO")
    private BigDecimal amount;

    @JsonProperty("COD_ESITO")
    private Integer outcomeCode;

    @JsonProperty("DATA_ESITO_SINGOLO_PAGAMENTO")
    private String singlePaymentOutcomeDate;

    @JsonProperty("IDSP")
    private String idsp;

    @JsonProperty("ID_FLUSSO")
    private String flowId;

    @JsonProperty("DATA_ORA_FLUSSO")
    private String flowDateTime;

    @JsonProperty("ID_DOMINIO")
    private String domainId;

    @JsonProperty("PSP")
    private String psp;

    @JsonProperty("INT_PSP")
    private String intPsp;

    @JsonProperty("UNIQUE_ID")
    private String uniqueId;

    @JsonProperty("INSERTED_TIMESTAMP")
    private String insertedTimestamp;

    @JsonProperty("ID_TRANSFER")
    private Long idTransfer;
}