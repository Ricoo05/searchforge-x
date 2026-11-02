package dev.searchforge.storage;

import dev.searchforge.analyzer.Analyzer;
import dev.searchforge.index.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Version 1: documents plus sorted delta/varint positional postings, SHA-256 trailer. */
public final class SegmentCodec {
  private SegmentCodec() {}

  private static final int MAGIC = 0x53465831;

  public static byte[] encode(IndexSnapshot index) throws IOException {
    var bytes = new ByteArrayOutputStream();
    var out = new DataOutputStream(bytes);
    out.writeInt(MAGIC);
    out.writeLong(index.generation());
    writeVar(out, index.stopWords().size());
    for (String word : new TreeSet<>(index.stopWords())) writeText(out, word);
    writeVar(out, index.size());
    for (Document d : index.documents().values()) {
      writeVar(out, d.id());
      writeText(out, d.text());
      writeVar(out, d.metadata().size());
      for (var entry : new TreeMap<>(d.metadata()).entrySet()) {
        writeText(out, entry.getKey());
        writeText(out, entry.getValue());
      }
    }
    writeVar(out, index.terms().size());
    for (String term : new TreeSet<>(index.terms())) {
      writeText(out, term);
      var postings = index.postings(term);
      writeVar(out, postings.size());
      long lastId = 0;
      for (var entry : postings.entrySet()) {
        writeVar(out, entry.getKey() - lastId);
        lastId = entry.getKey();
        writeVar(out, entry.getValue().size());
        int lastPosition = 0;
        for (int position : entry.getValue()) {
          writeVar(out, position - lastPosition);
          lastPosition = position;
        }
      }
    }
    byte[] body = bytes.toByteArray();
    out.write(digest(body));
    return bytes.toByteArray();
  }

  public static IndexSnapshot decode(byte[] bytes, Analyzer analyzer) throws IOException {
    if (bytes.length < 44) throw new IOException("Truncated segment");
    byte[] body = Arrays.copyOf(bytes, bytes.length - 32);
    if (!MessageDigest.isEqual(digest(body), Arrays.copyOfRange(bytes, body.length, bytes.length)))
      throw new IOException("Segment checksum mismatch");
    try (var in = new DataInputStream(new ByteArrayInputStream(body))) {
      if (in.readInt() != MAGIC) throw new IOException("Unsupported segment format");
      long generation = in.readLong();
      if (generation < 1) throw new IOException("Invalid generation");
      int stopCount = count(in);
      var stopWords = new HashSet<String>();
      for (int i = 0; i < stopCount; i++)
        if (!stopWords.add(readText(in))) throw new IOException("Duplicate stop word");
      if (!stopWords.equals(analyzer.stopWords()))
        throw new IOException("Analyzer configuration mismatch");
      int count = count(in);
      var documents = new ArrayList<Document>();
      for (int i = 0; i < count; i++) {
        long id = readVar(in);
        String text = readText(in);
        int fields = count(in);
        var metadata = new HashMap<String, String>();
        for (int j = 0; j < fields; j++) {
          if (metadata.putIfAbsent(readText(in), readText(in)) != null)
            throw new IOException("Duplicate metadata key");
        }
        documents.add(new Document(id, text, metadata));
      }
      // Version 1 intentionally rebuilds and cross-checks persisted postings on recovery.
      // This detects analyzer/configuration mismatch and semantic corruption, at startup cost.
      var snapshot = new IndexSnapshot(documents, analyzer, generation);
      int terms = count(in);
      if (terms != snapshot.terms().size()) throw new IOException("Dictionary count mismatch");
      var seenTerms = new HashSet<String>();
      for (int i = 0; i < terms; i++) {
        String term = readText(in);
        if (!seenTerms.add(term)) throw new IOException("Duplicate term");
        if (!snapshot.terms().contains(term)) throw new IOException("Unexpected dictionary term");
        int df = count(in);
        var decoded = new TreeMap<Long, List<Integer>>();
        long id = 0;
        for (int j = 0; j < df; j++) {
          long delta = readVar(in);
          if (delta == 0) throw new IOException("Non-increasing document ID");
          id = Math.addExact(id, delta);
          int tf = count(in), position = 0;
          var positions = new ArrayList<Integer>();
          for (int k = 0; k < tf; k++) {
            position = Math.addExact(position, Math.toIntExact(readVar(in)));
            positions.add(position);
          }
          decoded.put(id, positions);
        }
        if (!decoded.equals(snapshot.postings(term)))
          throw new IOException("Persisted postings/analyzer mismatch");
      }
      if (in.available() != 0) throw new IOException("Trailing segment data");
      return snapshot;
    } catch (IllegalArgumentException | ArithmeticException ex) {
      throw new IOException("Invalid segment content", ex);
    }
  }

  private static int count(DataInputStream in) throws IOException {
    long value = readVar(in);
    if (value > in.available()) throw new IOException("Count exceeds remaining bytes");
    return Math.toIntExact(value);
  }

  private static void writeText(DataOutput out, String text) throws IOException {
    byte[] data = text.getBytes(StandardCharsets.UTF_8);
    writeVar(out, data.length);
    out.write(data);
  }

  private static String readText(DataInputStream in) throws IOException {
    byte[] bytes = in.readNBytes(count(in));
    var decoder = StandardCharsets.UTF_8.newDecoder();
    return decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString();
  }

  public static void writeVar(DataOutput out, long value) throws IOException {
    if (value < 0) throw new IllegalArgumentException("Negative unsigned varint");
    do {
      int bits = (int) (value & 127);
      value >>>= 7;
      out.writeByte(bits | (value == 0 ? 0 : 128));
    } while (value != 0);
  }

  public static long readVar(DataInput in) throws IOException {
    long value = 0;
    for (int shift = 0; shift <= 56; shift += 7) {
      int next = in.readUnsignedByte();
      value |= (long) (next & 127) << shift;
      if ((next & 128) == 0) {
        if (shift > 0 && next == 0) throw new IOException("Noncanonical varint");
        return value;
      }
    }
    throw new IOException("Overflowing varint");
  }

  public static byte[] digest(byte[] bytes) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(bytes);
    } catch (NoSuchAlgorithmException ex) {
      throw new AssertionError(ex);
    }
  }
}
