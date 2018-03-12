package com.shaposhnyk.graphql.genericdb

import org.springframework.ldap.core.AttributesMapper
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.query.ContainerCriteria
import org.springframework.ldap.query.LdapQuery
import org.springframework.ldap.query.LdapQueryBuilder
import javax.naming.directory.Attributes


class LdapFetcher(private val template: LdapTemplate,
                  private val query: LdapQuery = LdapQueryBuilder.query()) : SimpleFetcherBuilder {

    private val mapper = AttributesMapper<Attributes> { attrs -> attrs }

    fun ofObjectClass(objectClass: String) = filter("objectclass", objectClass)

    override fun filter(filterName: String, value: String): LdapFetcher {
        if (query is ContainerCriteria) {
            return LdapFetcher(template, query.and(filterName).`is`(value))
        } else if (query is LdapQueryBuilder) {
            return LdapFetcher(template, query.where(filterName).`is`(value))
        }
        return LdapFetcher(template, LdapQueryBuilder.query().where(filterName).`is`(value))
    }

    override fun attributes(fields: Collection<String>): LdapFetcher {
        val arr = fields.toTypedArray()
        val q = (query as? LdapQueryBuilder ?: LdapQueryBuilder.query())
                .attributes(*arr)
        return LdapFetcher(template, q)
    }

    override fun offsetAndLimit(offset: Int, limit: Int): LdapFetcher {
        if (query is LdapQueryBuilder) {
            return LdapFetcher(template, query.countLimit(limit))
        }
        return this;
    }

    override fun fetch(): Collection<Attributes> {
        return template.search(query, mapper)
    }

    companion object {
        fun with(ldap: LdapTemplate) = LdapFetcher(ldap, LdapQueryBuilder.query())
    }
}

