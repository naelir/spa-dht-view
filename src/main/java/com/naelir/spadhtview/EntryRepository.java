package com.naelir.spadhtview;

import java.util.List;
import java.util.Map;

public interface EntryRepository {
    Map<String, Object> getLast();
    List<Entry> getPopular();
    long count();
    List<Entry> findByName(String pattern);
    Entry insert(Entry entry);
    boolean remove(String hash);
}