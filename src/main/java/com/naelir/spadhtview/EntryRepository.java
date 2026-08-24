package com.naelir.spadhtview;

import java.util.List;

public interface EntryRepository {
    List<Entry> getLast();
    long count();
    List<Entry> findByName(String pattern);
    Entry insert(Entry entry);
    boolean remove(String hash);
}