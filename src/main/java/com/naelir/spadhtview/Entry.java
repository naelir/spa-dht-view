package com.naelir.spadhtview;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Entry {

    @JsonProperty("name")
    public String name;

    @JsonProperty("hash")
    public String hash;

    @JsonProperty("fileCount")
    public int fileCount;

    /** Unix epoch milliseconds */
    @JsonProperty("foundTime")
    public long foundTime;
    
    @JsonProperty("nfo")
    public boolean nfo;

    public Entry() {}
}