package com.example.beautyhub.data; // Gantikan dengan nama pakej anda yang betul

import com.example.beautyhub.models.Variant;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VariantsTypeAdapter extends TypeAdapter<Map<String, Variant>> {

    private final Gson gson = new Gson();

    @Override
    public void write(JsonWriter out, Map<String, Variant> value) throws IOException {
        // Logik untuk menulis ke JSON (tidak begitu penting untuk pembacaan)
        if (value == null) {
            out.nullValue();
            return;
        }
        out.beginArray();
        for (Variant variant : value.values()) {
            gson.toJson(variant, Variant.class, out);
        }
        out.endArray();
    }

    @Override
    public Map<String, Variant> read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        Map<String, Variant> variantsMap = new HashMap<>();

        // Periksa jika data adalah array `[`
        if (in.peek() == JsonToken.BEGIN_ARRAY) {
            in.beginArray();
            Type listType = new TypeToken<ArrayList<Variant>>(){}.getType();
            List<Variant> variantList = gson.fromJson(in, listType);
            if (variantList != null) {
                for (Variant variant : variantList) {
                    if (variant.getId() != null && !variant.getId().isEmpty()) {
                        variantsMap.put(variant.getId(), variant);
                    }
                }
            }
            // in.endArray() akan diuruskan oleh `gson.fromJson` di atas
        } else {
            // Jika data sudah dalam format Map `{`, baca seperti biasa
            in.beginObject();
            while (in.hasNext()) {
                String key = in.nextName();
                Variant variant = gson.fromJson(in, Variant.class);
                variantsMap.put(key, variant);
            }
            in.endObject();
        }

        return variantsMap;
    }
}
