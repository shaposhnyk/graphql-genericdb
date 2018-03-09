package com.shaposhnyk.graphql.genericdb

import graphql.language.FieldDefinition
import graphql.language.ListType
import graphql.language.ObjectTypeDefinition
import graphql.language.TypeName
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.TypeDefinitionRegistry
import graphql.schema.idl.TypeRuntimeWiring.newTypeWiring
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean


@SpringBootApplication
class Application {

    @Bean
    fun init() = CommandLineRunner {
        println("Hello Boot")
    }

    @Bean
    fun schema(): GraphQLSchema {
        val queryType = ObjectTypeDefinition("Query")
        queryType.fieldDefinitions.add(FieldDefinition("applications", ListType(TypeName("application"))))

        val appType = ObjectTypeDefinition("application")
        appType.fieldDefinitions.add(FieldDefinition("id", TypeName("String")))
        appType.fieldDefinitions.add(FieldDefinition("name", TypeName("String")))

        val typeRegistry = TypeDefinitionRegistry()
        typeRegistry.add(queryType)
        typeRegistry.add(appType)

        val wiring = RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Query")
                        .dataFetcher("applications") { _ -> listOf("appFirst", "appSecond", "appGUI") }
                )
                .type(newTypeWiring("application")
                        .dataFetcher("id") { env -> (env.getSource() as String).toUpperCase() }
                        .dataFetcher("name") { env -> env.getSource() as String }
                )
                .build()

        return SchemaGenerator().makeExecutableSchema(typeRegistry, wiring)
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}

