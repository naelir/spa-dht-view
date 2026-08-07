package com.naelir.spadhtview;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Entry {

    @JsonProperty("n")
    public String name;

    @JsonProperty("h")
    public String hash;

    @JsonProperty("fc")
    public int fileCount;

    /** Unix epoch milliseconds */
    @JsonProperty("se")
    public long foundTime;
    
    @JsonProperty("sz")
    public long size;

    @JsonProperty("g")
    public String genre;

    @JsonProperty("p")
    public Integer peers;
    
    public Entry() {}
}