package com.starter.springboot.domain;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(Authority.class)
public abstract class Authority_ extends com.starter.springboot.domain.BaseAuditedEntity_ {

	public static volatile SingularAttribute<Authority, String> name;
	public static volatile SingularAttribute<Authority, String> description;
	public static volatile SingularAttribute<Authority, Long> id;

	public static final String NAME = "name";
	public static final String DESCRIPTION = "description";
	public static final String ID = "id";

}

