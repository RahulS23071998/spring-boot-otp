package com.starter.springboot.entity;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.util.Date;
import javax.annotation.processing.Generated;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(User.class)
public abstract class User_ extends com.starter.springboot.entity.BaseAuditedEntity_ {

	public static volatile SingularAttribute<User, Date> lastPasswordResetDate;
	public static volatile SingularAttribute<User, String> googleId;
	public static volatile SingularAttribute<User, String> lastName;
	public static volatile SingularAttribute<User, Boolean> isOtpRequired;
	public static volatile SingularAttribute<User, Role> role;
	public static volatile SingularAttribute<User, Boolean> enabled;
	public static volatile SingularAttribute<User, String> firstName;
	public static volatile SingularAttribute<User, Boolean> emailVerified;
	public static volatile SingularAttribute<User, String> password;
	public static volatile SingularAttribute<User, Authority> authority;
	public static volatile SingularAttribute<User, Long> id;
	public static volatile SingularAttribute<User, AuthType> authType;
	public static volatile SingularAttribute<User, String> email;
	public static volatile SingularAttribute<User, String> username;
	public static volatile SingularAttribute<User, UserStatus> status;

	public static final String LAST_PASSWORD_RESET_DATE = "lastPasswordResetDate";
	public static final String GOOGLE_ID = "googleId";
	public static final String LAST_NAME = "lastName";
	public static final String IS_OTP_REQUIRED = "isOtpRequired";
	public static final String ROLE = "role";
	public static final String ENABLED = "enabled";
	public static final String FIRST_NAME = "firstName";
	public static final String EMAIL_VERIFIED = "emailVerified";
	public static final String PASSWORD = "password";
	public static final String AUTHORITY = "authority";
	public static final String ID = "id";
	public static final String AUTH_TYPE = "authType";
	public static final String EMAIL = "email";
	public static final String USERNAME = "username";
	public static final String STATUS = "status";

}

