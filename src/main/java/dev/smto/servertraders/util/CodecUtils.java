package dev.smto.servertraders.util;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.smto.servertraders.ServerTraders;

import java.util.NoSuchElementException;

public class CodecUtils {
    public static <T> String toJsonString(T input, Codec<T> codec) throws NoSuchElementException {
        return codec.encodeStart(JsonOps.INSTANCE, input).resultOrPartial(ServerTraders.LOGGER::error).orElseThrow().toString();
    }

    public static <T> T fromJsonString(String input, Codec<T> codec) throws NoSuchElementException {
        return codec.parse(JsonOps.INSTANCE, JsonParser.parseString(input)).resultOrPartial(ServerTraders.LOGGER::error).orElseThrow();
    }
}
