package it.gov.pagopa.reporting.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Fdr3Response {
    private Fdr3Metadata metadata;
    private int count;
    private List<Fdr3Data> data;
}
