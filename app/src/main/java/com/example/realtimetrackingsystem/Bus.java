package com.example.realtimetrackingsystem;

import java.util.List;
import java.util.Map;

public class Bus {
    private String busNumber;

    private String StartTime;
    private String EndTime;
    private String busname;
    private String driverMobileNumber;
    private String source;
    private String destination;
    private List<Map<String, Object>> stations;

    public String getbusnumber() {
        return busNumber;
    }
    public String getsource() {
        return source;
    }

    public String getdestination() {
        return destination;
    }
    public String getStartTime() {
        return StartTime;
    }
    public String getEndTime() {
        return EndTime;
    }
    public String getbusName() {
        return busname;
    }   public String getDriverNumber() {
        return driverMobileNumber;
    }
    public List<Map<String, Object>> getStations() {
        return stations;
    }

}
