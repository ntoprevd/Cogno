package com.ntoprevd.cogno.data.db;

import androidx.annotation.Nullable;
import androidx.room.TypeConverter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Converters {

    @TypeConverter
    @Nullable
    public static String fromStringList(@Nullable List<String> value) {
        if (value == null) {
            return null;
        }
        return String.join("\n", value);
    }

    @TypeConverter
    @Nullable
    public static List<String> toStringList(@Nullable String value) {
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(value.split("\\n", -1));
    }
}
