package com.naelir.spadhtview;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * Thread-safe in-memory implementation of {@link EntryRepository}.
 *
 * <p>Entries are keyed by {@code hash}. Reads never block (CopyOnWriteArrayList).
 * Mutations are {@code synchronized} to guarantee atomicity.
 */
public class InMemoryEntryRepository implements EntryRepository {

    private final CopyOnWriteArrayList<Entry> store = new CopyOnWriteArrayList<>();



    @Override
    public List<Entry> getLast() {
        return store.stream()
                .sorted(Comparator.comparingLong((Entry e) -> e.foundTime).reversed())
                .limit(50)
                .toList();
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public List<Entry> findByName(String pattern) {
        if (pattern == null || pattern.length() < 3) return List.of();
        String[] parts = pattern.split(" ", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(".*");
            sb.append(Pattern.quote(parts[i]));
        }
        Pattern regex = Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
        return store.stream()
                .filter(e -> e.name != null && regex.matcher(e.name).find())
                .sorted(Comparator.comparingLong((Entry e) -> e.foundTime).reversed())
                .limit(200)
                .toList();
    }

    @Override
    public synchronized Entry insert(Entry entry) {
        store.add(copy(entry));
        return entry;
    }

    @Override
    public synchronized boolean remove(String hash) {
        return store.removeIf(e -> hash != null && hash.equals(e.hash));
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private static Entry copy(Entry src) {
        Entry dst = new Entry();
        dst.name      = src.name;
        dst.genre      = src.genre;
        dst.hash      = src.hash;
        dst.fileCount = src.fileCount;
        dst.foundTime = src.foundTime;
        dst.size       = src.size;
        return dst;
    }
}