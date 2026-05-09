package com.cyh.Dto;

import lombok.Data;

@Data
public class Geocode {
        private String formattedAddress;
        private String country;
        private String province;
        private String citycode;
        private String city;
        private String district;
        private String adcode;
        private String location;
        private String level;
}
