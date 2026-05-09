package com.cyh.Dto;

import lombok.Data;

import java.util.List;

@Data
public class GeocodeResponse {
    private String status;
    private String info;
    private String infocode;
    private String count;
    private List<Geocode> geocodes;
}
