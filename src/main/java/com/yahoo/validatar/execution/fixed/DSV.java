package com.yahoo.validatar.execution.fixed;

import com.yahoo.validatar.common.Helpable;
import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.Result;
import com.yahoo.validatar.common.TypeSystem;
import com.yahoo.validatar.common.TypeSystem.Type;
import com.yahoo.validatar.common.TypedObject;
import com.yahoo.validatar.execution.Engine;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class DSV implements Engine {
    public static final String ENGINE_NAME = "csv";

    public static final String CSV_DELIMITER = "csv-delimiter";
    public static final String METADATA_DELIMITER_KEY = "delimiter";

    public static final String DEFAULT_TYPE = Type.STRING.name();
    public static final String DEFAULT_DELIMITER = ",";

    private String defaultDelimiter;

    private final OptionParser parser = new OptionParser() {
        {
            accepts(CSV_DELIMITER, "The delimiter to use while parsing fields within a record. Defaults to ',' or CSV")
                    .withRequiredArg()
                    .describedAs("The field delimiter")
                    .defaultsTo(DEFAULT_DELIMITER);
            allowsUnrecognizedOptions();
        }
    };

    @Override
    public boolean setup(String[] arguments) {
        OptionSet options = parser.parse(arguments);
        defaultDelimiter = (String) options.valueOf(CSV_DELIMITER);
        return true;
    }

    @Override
    public void execute(Query query) {
        String queryName = query.name;
        String queryValue = query.value.trim();
        log.info("Running {}", queryName);

        Map<String, String> metadata = query.getMetadata();

        String delimiter = Query.getKey(metadata, METADATA_DELIMITER_KEY).orElse(defaultDelimiter);
        char character = delimiter.charAt(0);
        log.info("Using delimiter as a character: {}", character);

        boolean isFile = isPath(queryValue);

        log.info("Running or loading data from \n{}", queryValue);

        try (InputStream stream = isFile ? new FileInputStream(queryValue) : new ByteArrayInputStream(getBytes(queryValue));
             Reader reader = new InputStreamReader(stream)) {

            CSVFormat format = CSVFormat.RFC4180.withDelimiter(character).withFirstRecordAsHeader().withIgnoreSurroundingSpaces();
            CSVParser parser = new CSVParser(reader, format);

            Map<String, Integer> headerIndices = parser.getHeaderMap();
            Map<String, Type> typeMap = getTypeMapping(headerIndices, metadata);
            Result result = query.createResults();
            parser.iterator().forEachRemaining(r -> addRow(r, result, typeMap));
        } catch (Exception e) {
            log.error("Error while parsing data for {} with {}", queryName, queryValue);
            log.error("Error", e);
            query.setFailure(e.toString());
        }
    }

    private static void addRow(CSVRecord record, Result result, Map<String, Type> header) {
        if (!record.isConsistent()) {
            log.warn("Record does not have the same number of fields as the header mapping. Skipping record: {}", record);
            return;
        }
        for (Map.Entry<String, Type> field : header.entrySet()) {
            String fieldName = field.getKey();
            TypedObject fieldValue = getTyped(field.getValue(), record.get(fieldName));
            result.addColumnRow(fieldName, fieldValue);
        }
    }

    private static TypedObject getTyped(Type type, String field) {
        // Can't be null
        if (type != Type.STRING && field.isEmpty()) {
            log.warn("Found an empty value for a non-string field of type {}. Nulled it. Asserts that use this may fail.", type);
            return null;
        }
        return TypeSystem.cast(type, new TypedObject(field, Type.STRING));
    }

    /**
     * Gets a mapping of the {@link Type} for the columns in the data.
     *
     * @param headerIndices The {@link Map} of header field names to their positions.
     * @param metadata The metadata of the query viewed as a {@link Map}.
     * @return The {@link Map} of column names to their types.
     */
    static Map<String, Type> getTypeMapping(Map<String, Integer> headerIndices, Map<String, String> metadata) {
        if (headerIndices == null) {
            log.error("No header row found. The first row in your data needs to be a header row.");
            throw new RuntimeException("Header row not found for data. First row in data needs to be a header");
        }

        Map<String, Type> typeMap = new HashMap<>();
        for (String column : headerIndices.keySet()) {
            String typeMapping = Query.getKey(metadata, column).orElse(DEFAULT_TYPE);
            try {
                Type type = Type.valueOf(typeMapping);
                typeMap.put(column, type);
            } catch (IllegalArgumentException iae) {
                log.error("Unable to find type {}. Using STRING instead. Valid values are {}", typeMapping, Type.values());
                typeMap.put(column, Type.STRING);
            }
        }
        return typeMap;
    }

    private static byte[] getBytes(String string) {
        String unescaped = StringEscapeUtils.unescapeJava(string);
        return unescaped.getBytes(StandardCharsets.UTF_8);
    }

    private static boolean isPath(String string) {
        File file = new File(string);
        return file.exists() && file.isFile();
    }

    @Override
    public String getName() {
        return ENGINE_NAME;
    }

    @Override
    public void printHelp() {
        Helpable.printHelp("CSV Engine options", parser);
        System.out.println("This Engine lets you load delimited text data from files or to specify it directly as a query.");
        System.out.println("It follows the RFC 4180 CSV specification: https://tools.ietf.org/html/rfc4180\n");
        System.out.println("Your data MUST contain a header row naming your columns.");
        System.out.println("The types of all fields will be inferred as STRINGS. However, you can provide mappings ");
        System.out.println("for each column name by adding entries to the metadata section of the query, where");
        System.out.println("the key is the name of your column and the value is the type of the column.");
        System.out.println("The values can be BOOLEAN, STRING, LONG, DECIMAL, DOUBLE, and TIMESTAMP.");
        System.out.println("DECIMAL is used for really large numbers that cannot fit inside a long (2^63). TIMESTAMP is");
        System.out.println("used to interpret a whole number as a timestamp field - millis from epoch. Use to load dates.");
        System.out.println("This engine primarily exists to let you easily load expected data in as a dataset. You can");
        System.out.println("then use the data by joining it with some other data and performing asserts on the joined");
        System.out.println("dataset.");
    }

}
