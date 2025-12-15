package com.starter.springboot.entity;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import javax.annotation.processing.Generated;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(PasswordHistory.class)
public abstract class PasswordHistory_ extends com.starter.springboot.entity.BaseAuditedEntity_ {

	public static volatile SingularAttribute<PasswordHistory, String> password;
	public static volatile SingularAttribute<PasswordHistory, Long> id;
	public static volatile SingularAttribute<PasswordHistory, User> user;

	public static final String PASSWORD = "password";
	public static final String ID = "id";
	public static final String USER = "user";

}

