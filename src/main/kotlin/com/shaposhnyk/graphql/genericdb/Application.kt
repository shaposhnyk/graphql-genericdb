package com.shaposhnyk.graphql.genericdb

import graphql.Scalars
import graphql.schema.*
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.ldap.core.LdapTemplate
import java.util.stream.Collectors
import javax.naming.directory.Attributes


@SpringBootApplication
class Application {

    @Bean
    fun init(ldapTemplate: LdapTemplate) = CommandLineRunner {
        println("Hello Boot")
        println("people " + LdapFetcher.with(ldapTemplate)
                .ofObjectClass("people")
                .fetch()
                .stream()
                .map { it.get("cn").toString() }
                .collect(Collectors.toList()))
    }

    @Bean
    fun schema(ldapTemplate: LdapTemplate): GraphQLSchema {
        val queryBuilder = GraphQLObjectType.newObject().name("Query")
                .field(rootList("application") { listOf("appFirst", "appSecond", "appGUI") })
                .field(rootList("people", "person") {
                    LdapFetcher.with(ldapTemplate)
                            .ofObjectClass("person")
                            .fetch()
                })

        val appBuilder: GraphQLType = GraphQLObjectType.newObject().name("application")
                .field(staticField("id", String::toUpperCase))
                .field(staticField("name"))
                .build()

        val groupBuilder: GraphQLType = GraphQLObjectType.newObject().name("group")
                .field(ldapField("id", "dn") { it.toUpperCase() })
                .field(ldapField("name", "cn"))
                .build()

        val personBuilder: GraphQLType = GraphQLObjectType.newObject().name("person")
                .field(ldapField("id", "dn"))
                .field(ldapField("login", "sn"))
                .field(ldapField("name", "cn"))
                .field(ldapList("group") { attrs ->
                    val login = attrs.get("sn").get() as String
                    LdapFetcher.with(ldapTemplate)
                            .ofObjectClass("groupOfUniqueNames")
                            .filter("uniqueMember", "uid=${login},ou=people,dc=shaposhnyk,dc=com")
                            .fetch()
                }
                ).build()

        return GraphQLSchema.newSchema()
                .query(queryBuilder)
                .build(mutableSetOf(appBuilder, personBuilder, groupBuilder))
    }

    /*
     * Helper methods
     */

    private fun ldapList(entitySingular: String, extractor: (Attributes) -> Any) = GraphQLFieldDefinition.newFieldDefinition()
            .name("${entitySingular}s")
            .type(GraphQLList.list(GraphQLTypeReference(entitySingular)))
            .dataFetcher { env -> extractor(env.getSource() as Attributes) }
            .build()

    private fun ldapField(extName: String, intName: String) = ldapField(extName, intName) { it }

    private fun ldapField(extName: String, intName: String, extractor: (String) -> String) = GraphQLFieldDefinition.newFieldDefinition()
            .name(extName)
            .type(Scalars.GraphQLString)
            .dataFetcher { env -> extractor((env.getSource() as Attributes).get(intName).get() as String) }
            .build()

    private fun staticField(extName: String) = staticField(extName) { it }

    private fun staticField(extName: String, extractor: (String) -> String) = GraphQLFieldDefinition.newFieldDefinition()
            .name(extName)
            .type(Scalars.GraphQLString)
            .dataFetcher { env -> extractor(env.getSource() as String) }

    private fun rootList(entityPlural: String, entitySingular: String, fetcher: () -> Any): GraphQLFieldDefinition {
        return GraphQLFieldDefinition.newFieldDefinition()
                .name(entityPlural)
                .type(GraphQLList.list(GraphQLTypeReference(entitySingular)))
                .dataFetcher { env -> fetcher() }
                .build()
    }

    private fun rootList(entity: String, fetcher: () -> Any)
            = rootList("${entity}s", entity, fetcher)
}

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}

