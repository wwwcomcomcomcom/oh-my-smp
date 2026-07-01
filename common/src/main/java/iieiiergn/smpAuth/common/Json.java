package iieiiergn.smpAuth.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** Shared Gson instance so every module serializes the wire DTOs identically. */
public final class Json {

    public static final Gson GSON = new GsonBuilder().create();

    private Json() {
    }
}
