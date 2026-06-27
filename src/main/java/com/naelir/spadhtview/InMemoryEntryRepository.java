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
    public List<Entry> findAll(int page, int pageSize) {
        int skip = Math.max(0, (page - 1) * pageSize);
        return store.stream()
                .sorted(Comparator.comparingLong((Entry e) -> e.foundTime).reversed())
                .skip(skip)
                .limit(pageSize)
                .toList();
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public Entry findByHash(String hash) {
        if (hash == null) return null;
        return store.stream()
                .filter(e -> hash.equals(e.hash))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Entry> findByName(String pattern) {
        Pattern regex = Pattern.compile(Pattern.quote(pattern), Pattern.CASE_INSENSITIVE);
        return store.stream()
                .filter(e -> e.name != null && regex.matcher(e.name).find())
                .sorted(Comparator.comparingLong((Entry e) -> e.foundTime).reversed())
                .toList();
    }

    @Override
    public synchronized Entry insert(Entry entry) {
        // duplicate hash – skip silently
        if (findByHash(entry.hash) != null) {
            return entry;
        }
        store.add(copy(entry));
        return entry;
    }

    @Override
    public synchronized boolean update(Entry entry) {
        for (int i = 0; i < store.size(); i++) {
            if (entry.hash != null && entry.hash.equals(store.get(i).hash)) {
                store.set(i, copy(entry));
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private static Entry copy(Entry src) {
        Entry dst = new Entry();
        dst.name      = src.name;
        dst.hash      = src.hash;
        dst.fileCount = src.fileCount;
        dst.foundTime = src.foundTime;
        dst.nfo       = src.nfo;
        return dst;
    }
}