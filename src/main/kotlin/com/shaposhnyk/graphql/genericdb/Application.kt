package com.shaposhnyk.graphql.genericdb

import graphql.Scalars
import graphql.schema.*
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.ldap.core.DirContextAdapter
import org.springframework.ldap.core.LdapTemplate
import java.util.stream.Collectors


@SpringBootApplication
class Application {

    @Bean
    fun init(ldapTemplate: LdapTemplate) = CommandLineRunner {
        println("Hello Boot")
        println("people " + LdapFetcher.with(ldapTemplate)
                .ofObjectClass("people")
                .fetch()
                .stream()
                .map { it.getStringAttribute("cn") }
                .collect(Collectors.toList()))
    }

    @Bean
    fun schema(ldapTemplate: LdapTemplate): GraphQLSchema {
        val queryBuilder = GraphQLObjectType.newObject().name("Query")
                .field(rootList("application") { listOf("appFirst", "appSecond", "appGUI") })
                .field(ldapList("people", "person", "person") { env ->
                    val fields = selectedFields(env)
                    log.info("Selecting fields on people: {}", fields)
                    LdapFetcher.with(ldapTemplate)
                            .attributes(fields)
                            .offsetAndLimit(env.arguments["offset"] as Int, env.arguments["limit"] as Int)
                            .ofObjectClass("person")
                            .fetch()
                }).description("Unified Enterprise Data API")
                .build()

        val appBuilder: GraphQLType = GraphQLObjectType.newObject().name("application")
                .field(staticField("id", String::toUpperCase))
                .field(staticField("name"))
                .description("CMDB Application")
                .build()

        val groupBuilder: GraphQLType = GraphQLObjectType.newObject().name("group")
                .field(ldapField("id", "dn") { it.toUpperCase() })
                .field(ldapField("name", "cn"))
                .description("LDAP Group")
                .build()

        val personBuilder: GraphQLType = GraphQLObjectType.newObject().name("person")
                .field(ldapDn("id") { it })
                .field(ldapField("login", "sn"))
                .field(ldapField("name", "cn"))
                .field(ldapList("groups", "group", listOf("sn", "groupOfUniqueNames")) { env ->
                    val login = (env.getSource() as DirContextAdapter).getStringAttribute("sn")
                    val fields = selectedFields(env)
                    log.info("Selecting fields on group: {}", fields)
                    LdapFetcher.with(ldapTemplate)
                            .attributes(fields)
                            .offsetAndLimit(env.arguments["offset"] as Int, env.arguments["limit"] as Int)
                            .ofObjectClass("groupOfUniqueNames")
                            .filter("uniqueMember", "uid=${login},ou=people,dc=shaposhnyk,dc=com")
                            .fetch()
                }
                ).description("LDAP Person")
                .build()

        return GraphQLSchema.newSchema()
                .query(queryBuilder)
                .build(mutableSetOf(appBuilder, personBuilder, groupBuilder))
    }

    /*
     * Helper methods
     */

    /**
     * Extract internalField names by external fieldNames
     * @param env - data fetching environment
     */
    private fun selectedFields(env: DataFetchingEnvironment): Collection<String> {
        return env.selectionSet.get().keys
                .map { k ->
                    ((env.fieldDefinition.type as GraphQLList)
                            .wrappedType as GraphQLObjectType)
                            .getFieldDefinition(k)
                }
                .filter { it is MappingFieldDefinition }
                .flatMap { (it as MappingFieldDefinition).intNames }
    }

    private fun ldapList(entityPlural: String, entitySingular: String, intName: String,
                         fetcher: (DataFetchingEnvironment) -> Any) = ldapList(
            entityPlural, entitySingular,
            listOf(intName), fetcher)

    private fun ldapList(entityPlural: String, entitySingular: String, intNames: List<String>,
                         fetcher: (DataFetchingEnvironment) -> Any): GraphQLFieldDefinition {
        return MappingFieldDefinition.ofMulti(intNames, GraphQLFieldDefinition.newFieldDefinition()
                .name(entityPlural)
                .type(GraphQLList.list(GraphQLTypeReference(entitySingular)))
                .dataFetcher(fetcher)
                .argument(GraphQLArgument.newArgument()
                        .name("limit").type(Scalars.GraphQLInt)
                        .defaultValue(100))
                .argument(GraphQLArgument.newArgument()
                        .name("offset").type(Scalars.GraphQLInt)
                        .defaultValue(0))
                .description("Paginated list of ${entityPlural}")
                .build())
    }

    private fun ldapDn(extName: String, extractor: (String) -> String) = MappingFieldDefinition.of("dn",
            GraphQLFieldDefinition.newFieldDefinition()
                    .name(extName)
                    .type(Scalars.GraphQLString)
                    .dataFetcher { env -> extractor((env.getSource() as DirContextAdapter).dn.toString()) }
                    .build())

    private fun ldapField(extName: String, intName: String) = ldapField(extName, intName) { it }

    private fun ldapField(extName: String, intName: String, extractor: (String) -> String) = MappingFieldDefinition.of(intName,
            GraphQLFieldDefinition.newFieldDefinition()
                    .name(extName)
                    .type(Scalars.GraphQLString)
                    .dataFetcher { env -> extractor((env.getSource() as DirContextAdapter).getStringAttribute(intName)) }
                    .build())

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
                .description("Non-paginated list of ${entityPlural}")
                .build()
    }

    private fun rootList(entity: String, fetcher: () -> Any)
            = rootList("${entity}s", entity, fetcher)

    companion object {
        val log = LoggerFactory.getLogger(this::class.java)
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}

