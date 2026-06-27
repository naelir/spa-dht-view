package com.naelir.spadhtview;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MongoEntryRepository implements EntryRepository {

    private final MongoCollection<Document> collection;

    public MongoEntryRepository(String connectionString, String dbName) {
        MongoClient client = MongoClients.create(connectionString);
        MongoDatabase database = client.getDatabase(dbName);
        this.collection = database.getCollection("entries");
        ensureIndexes();
    }

    /**
     * Creates the required indexes if they do not already exist.
     *
     * <ul>
     *   <li>Unique index on {@code hash}     – enforces deduplication and drives findByHash.</li>
     *   <li>Descending index on {@code foundTime} – speeds up the sort in {@link #findAll}.</li>
     * </ul>
     */
    private void ensureIndexes() {
        collection.createIndex(Indexes.ascending("hash"), new IndexOptions().unique(true).sparse(true));
        collection.createIndex(Indexes.descending("foundTime"));
        // speeds up prefix queries; arbitrary contains-searches are a full-scan in any DB
        collection.createIndex(Indexes.ascending("name"));
    }

    // ...existing code...

    @Override
    public List<Entry> findByName(String pattern) {
        // Case-insensitive regex – equivalent to SQL LIKE '%pattern%'
        Pattern regex = Pattern.compile(Pattern.quote(pattern), Pattern.CASE_INSENSITIVE);
        List<Entry> results = new ArrayList<>();
        collection.find(Filters.regex("name", regex))
                .sort(new Document("foundTime", -1))
                .forEach(doc -> results.add(fromDocument(doc)));
        return results;
    }

    @Override
    public List<Entry> findAll(int page, int pageSize) {
        int skip = Math.max(0, (page - 1) * pageSize);
        List<Entry> results = new ArrayList<>();
        collection.find()
                .sort(new Document("foundTime", -1))
                .skip(skip)
                .limit(pageSize)
                .forEach(doc -> results.add(fromDocument(doc)));
        return results;
    }

    @Override
    public long count() {
        return collection.countDocuments();
    }

    @Override
    public Entry insert(Entry entry) {
        try {
            collection.insertOne(toDocument(entry));
        } catch (MongoWriteException e) {
            if (e.getCode() == 11000) {
                return entry;   // duplicate hash – skip silently
            }
            throw e;
        }
        return entry;
    }

    @Override
    public boolean update(Entry entry) {
        Bson eq = Filters.eq("hash", entry.hash);
        Document document = toDocument(entry);
        ReplaceOptions upsert = new ReplaceOptions().upsert(false);
        long modified = collection.replaceOne(eq, document, upsert).getModifiedCount();
        return modified > 0;
    }

    @Override
    public Entry findByHash(String hash) {
        Document doc = collection.find(Filters.eq("hash", hash)).first();
        return doc != null ? fromDocument(doc) : null;
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private static Document toDocument(Entry e) {
        return new Document("hash", e.hash)
                .append("name", e.name)
                .append("fileCount", e.fileCount)
                .append("foundTime", e.foundTime)
                .append("nfo", e.nfo);
    }

    private static Entry fromDocument(Document doc) {
        Entry e = new Entry();
        e.name      = doc.getString("name");
        e.hash      = doc.getString("hash");
        Integer fc  = doc.getInteger("fileCount");
        e.fileCount = fc != null ? fc : 0;
        e.foundTime = toLong(doc.get("foundTime"));
        Boolean nfo = doc.getBoolean("nfo");
        e.nfo       = nfo != null && nfo;
        return e;
    }

    /** MongoDB can store numbers as Integer or Long depending on the value. */
    private static long toLong(Object val) {
        if (val instanceof Long l) return l;
        if (val instanceof Number n) return n.longValue();
        return 0L;
    }
}