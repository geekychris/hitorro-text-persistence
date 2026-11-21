package com.hitorro.basetext.indexer;

import com.hitorro.obj.core.GenericAnalyzer;
import com.hitorro.util.io.ResetableStringReader;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins {@link IndexerUtil#getTokensAsJson} (the JSON the {@code analyze} command returns) to what
 * hitorro-text-persistence 3.0.2 did on Jackson 2. The golden file was produced by running the
 * 3.0.2 classes with the same inputs (generator: hitorro-j3 tools/Jackson2TextPersistenceGolden.java.txt).
 * On 3.0.2 the method always fails with Lucene's IllegalStateException (it never calls
 * {@code TokenStream.reset()}); the migration keeps that, it does not fix it.
 */
class Jackson2TokensCompatTest {

  static final Map<String, String> out = new LinkedHashMap<>();
  static void p(String k, Object v){ out.put(k, String.valueOf(v).replace("\\","\\\\").replace("\n","\\n").replace("\r","\\r")); }
  static String err(Throwable t){ String n = t.getClass().getName(); return "!" + (n.startsWith("com.fasterxml.jackson") || n.startsWith("tools.jackson") ? "jackson" : t.getClass().getSimpleName()); }
  static Map<String, String> compute() throws Exception {
    out.clear();
    ObjectMapper om = com.hitorro.util.json.JsonMappers.json();
    String[] analyzers = {"STANDARD", "STANDARD,CASE", "CASE,STOP", "CASE,PORTERSTEM", "WHITESPACE,CASE", "CASE,LENGTH", "nonsense"};
    String[] texts = {"", "Hello World", "The quick brown foxes JUMPED over 2 lazy dogs!", "caf\u00e9 \u00dcber \uD83D\uDE00 \"quoted\" back\\slash",
        "e-mail: a.b@example.com http://x.y/z?q=1 $12.50 3.14", "tab\there\nnew line"};
    for (int a = 0; a < analyzers.length; a++) {
      for (int t = 0; t < texts.length; t++) {
        for (boolean asArray : new boolean[]{true, false}) {
          String k = "tokens." + a + "." + t + "." + asArray;
          try {
            GenericAnalyzer ga = new GenericAnalyzer(analyzers[a], null, GenericAnalyzer.Mode.Query);
            JsonNode n = IndexerUtil.getTokensAsJson(ga, new ResetableStringReader(texts[t]), asArray);
            p(k, n + " " + n.getNodeType() + " " + om.writeValueAsString(n));
          } catch (Throwable e) {
            p(k, err(e));
          }
        }
      }
    }
    return out;
  }

    @Test
    void tokensAsOnJackson2() throws Exception {
        Map<String, String> golden = new LinkedHashMap<>();
        try (InputStream in = getClass().getResourceAsStream("jackson2-tokens.txt")) {
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (line.isEmpty()) continue;
                int eq = line.indexOf('=');
                golden.put(line.substring(0, eq), line.substring(eq + 1));
            }
        }
        Map<String, String> got = compute();
        for (Map.Entry<String, String> e : golden.entrySet()) {
            assertEquals(e.getValue(), got.get(e.getKey()), e.getKey());
        }
        assertEquals(golden.keySet(), got.keySet());
    }
}
