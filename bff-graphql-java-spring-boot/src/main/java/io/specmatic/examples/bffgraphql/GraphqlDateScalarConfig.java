package io.specmatic.examples.bffgraphql;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Configuration
public class GraphqlDateScalarConfig {
  @Bean
  RuntimeWiringConfigurer dateScalarConfigurer() {
    GraphQLScalarType dateScalar = GraphQLScalarType.newScalar()
        .name("Date")
        .description("ISO-8601 local date")
        .coercing(new Coercing<LocalDate, String>() {
          @Override
          public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
            try {
              if (dataFetcherResult instanceof LocalDate date) {
                return date.toString();
              }
              if (dataFetcherResult instanceof String value) {
                return LocalDate.parse(value).toString();
              }
            } catch (DateTimeParseException exception) {
              throw new CoercingSerializeException("Expected ISO-8601 date string", exception);
            }
            throw new CoercingSerializeException("Expected LocalDate or ISO-8601 date string");
          }

          @Override
          public LocalDate parseValue(Object input) throws CoercingParseValueException {
            try {
              if (input instanceof String value) {
                return LocalDate.parse(value);
              }
            } catch (DateTimeParseException exception) {
              throw new CoercingParseValueException("Expected ISO-8601 date string", exception);
            }
            throw new CoercingParseValueException("Expected ISO-8601 date string");
          }

          @Override
          public LocalDate parseLiteral(Object input) throws CoercingParseLiteralException {
            try {
              if (input instanceof StringValue value) {
                return LocalDate.parse(value.getValue());
              }
            } catch (DateTimeParseException exception) {
              throw new CoercingParseLiteralException("Expected ISO-8601 date string literal", exception);
            }
            throw new CoercingParseLiteralException("Expected ISO-8601 date string literal");
          }
        })
        .build();

    return wiringBuilder -> wiringBuilder.scalar(dateScalar);
  }
}
