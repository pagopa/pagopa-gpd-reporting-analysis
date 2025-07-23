package it.gov.pagopa.reporting.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Fdr3Metadata {
    private int pageSize;
    private int pageNumber;
    private int totPage;
}
