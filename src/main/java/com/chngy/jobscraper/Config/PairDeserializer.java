package com.chngy.jobscraper.Config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.Iterator;

/**
 * Reads the form the ObjectMapper already WRITES for a Pair: a one-entry object such as {"3":5}, with the key "null"
 * for a missing low value ({"null":null} for an empty pair). Without this the seen file could be written but never read
 * back, so dedupe silently saw nothing as already seen.
 */
public class PairDeserializer extends StdDeserializer<Pair<Integer, Integer>> {

    public PairDeserializer() {
        super(Pair.class);
    }

    @Override
    public Pair<Integer, Integer> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        if (node == null || !node.isObject() || node.size() == 0) {
            return Pair.of(null, null);
        }
        Iterator<String> names = node.fieldNames();
        String name = names.next();
        JsonNode value = node.get(name);
        Integer left = "null".equals(name) ? null : Integer.valueOf(name);
        Integer right = value == null || value.isNull() ? null : value.asInt();
        return Pair.of(left, right);
    }
}
