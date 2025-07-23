package it.gov.pagopa.reporting.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Data model representing a single entry from the FDR3 API response.
 */
@NoArgsConstructor
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Fdr3Data {

    /**
     * Unique identifier of the flow.
     */
    private String fdr;

    /**
     * Identifier of the PSP.
     */
    private String pspId;

    /**
     * Revision number of the flow.
     */
    private int revision;

    /**
     * Publication timestamp of the flow in ISO-8601 format.
     */
    private String published;

}
