package com.shaposhnyk.graphql.genericdb

import graphql.schema.DataFetcher
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLOutputType

/**
 * Mapping field definition. A field which map internalField to a GraphQLField
 */
class MappingFieldDefinition(val intNames: Iterable<String>,
                             name: String?,
                             description: String?,
                             type: GraphQLOutputType?,
                             dataFetcher: DataFetcher<*>?,
                             arguments: MutableList<GraphQLArgument>?,
                             deprecationReason: String?) : GraphQLFieldDefinition(
        name,
        description,
        type,
        dataFetcher,
        arguments,
        deprecationReason) {

    companion object {
        fun of(intName: String, f: GraphQLFieldDefinition)
                = MappingFieldDefinition(listOf(intName),
                f.name,
                f.description,
                f.type,
                f.dataFetcher,
                f.arguments,
                f.deprecationReason)

        fun ofMulti(intNames: List<String>, f: GraphQLFieldDefinition)
                = MappingFieldDefinition(intNames,
                f.name,
                f.description,
                f.type,
                f.dataFetcher,
                f.arguments,
                f.deprecationReason)
    }
}