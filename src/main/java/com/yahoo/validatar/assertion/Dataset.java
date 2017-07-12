package com.yahoo.validatar.assertion;

import com.yahoo.validatar.common.Column;
import com.yahoo.validatar.common.Result;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple wrapper for a columnar dataset. Stores a {@link Map} of String column names to {@link Column} column values.
 *
 * Provides methods to do Cartesian Products on datasets. See {@link #cartesianProduct(Map)}.
 */
@AllArgsConstructor @Getter
public class Dataset {
    private final Map<String, Column> columns;

    private static Dataset copy(String namespace, Dataset dataset) {
        Map<String, Column> source = dataset.getColumns();
        // Copy the source columns and namespace the column names.
        Map<String, Column> result;
        result = source.entrySet().stream()
                                  .collect(HashMap::new,
                                          (m, e) -> m.put(Result.getName(namespace, e.getKey()), e.getValue().copy()),
                                          HashMap::putAll);
        return new Dataset(result);
    }

    private static Dataset crossProduct(int row, String nameA, Dataset a, String nameB, Dataset b) {
        return null;
    }

    private static Dataset crossProduct(String nameA, Dataset a, String nameB, Dataset b) {
        // Identity
        if (a == null) {
            return copy(nameB, b);
        }
        // The length of all columns in dataset a are the same. Same for b.
        return null;
    }

    /**
     * Performs a Cartesian Product of all the datasets.
     *
     * @param datasets A {@link Map} of dataset names to a {@link Map} of column names to {@link Column} of column values.
     * @return A {@link Map} of column names to {@link Column} columns
     */
    public static Dataset cartesianProduct(Map<String, Dataset> datasets) {
        // Product is not associative if you treat the datasets as actual sets of ordered tuples but generally
        // A  X (B X C) ~ (A X B) X C ~ A X B X C since there is a bijection from a, (b, c) -> (a, b), c.
        // For us, it is also commutative since A X B and B X A is the same dataset

        // Warning! This can be slow if there are lot of datasets or a lot of rows. This is the most generic and easy
        // to maintain way of doing it.
        Dataset result = null;
        return null;
    }

    public static Dataset join(Dataset dataset, Column rows) {
        return null;
    }
}
