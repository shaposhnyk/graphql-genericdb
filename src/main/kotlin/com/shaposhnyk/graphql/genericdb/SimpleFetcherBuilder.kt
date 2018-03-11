package com.shaposhnyk.graphql.genericdb

interface SimpleFetcherBuilder {
    fun filter(filterName: String, value: String): SimpleFetcherBuilder

    fun attributes(fields: Collection<String>): SimpleFetcherBuilder

    fun fetch(): Collection<Any>
}